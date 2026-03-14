package mainbot;

import battlecode.common.*;

import java.util.Random;

public class RobotPlayer {
    static int turnCount = 0;
    static final Random rng = new Random();

    // inisiasi constants
    static final int SOLDIER_ATTACK_RADIUS_SQ = 9; // SQ = squared
    static final int BUILD_RADIUS_SQ = 2;
    static final double RUIN_BASE_SCORE = 100.0;
    static final int PATROL_RADIUS_SQ = 16;
    static final int LOW_PAINT_THRESHOLD = 30;

    // message protocol Soldier/Mopper ke Tower
    static final int MSG_ENEMY_SPOTTED = 1;
    static final int MSG_RUIN_FOUND    = 2;
    static final int MSG_NEED_BACKUP   = 3;

    // inisiasi trackers
    static int towerBuiltCount = 0;
    static int unitSpawnCount = 0;
    static MapLocation reportedEnemyLoc = null;
    static MapLocation reportedRuinLoc  = null;

    static final Direction[] directions = {
        Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
        Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST
    };

    public static void run(RobotController rc) throws GameActionException{
        while (true){
            turnCount++;
            try {
                switch (rc.getType()){
                    case SOLDIER:
                        runSoldier(rc);
                        break;
                    case MOPPER:
                        runMopper(rc);
                        break;
                    case SPLASHER:
                        runSplasher(rc);
                        break;
                    default:
                        runTower(rc); // kenapa di sini runTower? karena tower tidak perlu di run setiap turn, jadi bisa diskip
                        break;
                }
            } catch (Exception e){
                System.out.println("Error in turn " + turnCount + ": " + e.getMessage());
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }

    // Tower Code
    public static void runTower(RobotController rc) throws GameActionException{
        if (!rc.isActionReady()){
            readTowerMessages(rc);
            attackNearbyEnemies(rc);
            return;
        }

        MapLocation myLoc = rc.getLocation();
        MapLocation spawnLoc = findBestSpawnLocation(rc, myLoc);

        if (spawnLoc == null){
            readTowerMessages(rc);
            attackNearbyEnemies(rc);
            return;
        } // jika spawnLoc null, berarti tidak ada lokasi yang cocok untuk spawn, jadi kita skip spawn dan fokus ke attack dan read message

        if (rc.canBuildRobot(UnitType.SPLASHER, spawnLoc)){
            rc.buildRobot(UnitType.SPLASHER, spawnLoc);
            unitSpawnCount++;
            System.out.println("Tower built SPLASHER at "+spawnLoc+" (Total spawned: "+unitSpawnCount+")");
        } else if (rc.canBuildRobot(UnitType.SOLDIER, spawnLoc)){
            rc.buildRobot(UnitType.SOLDIER, spawnLoc);
            unitSpawnCount++;
            System.out.println("Tower built SOLDIER at "+spawnLoc+" (Total spawned: "+unitSpawnCount+")");
        } else if (unitSpawnCount%5 == 0 && rc.canBuildRobot(UnitType.MOPPER, spawnLoc)){
            rc.buildRobot(UnitType.MOPPER, spawnLoc);
            unitSpawnCount++;
            System.out.println("Tower built MOPPER at "+spawnLoc+" (Total spawned: "+unitSpawnCount+")");
        }
        readTowerMessages(rc);
        attackNearbyEnemies(rc);
    }

    private static MapLocation findBestSpawnLocation(RobotController rc, MapLocation towerLoc) throws GameActionException{
        MapLocation best=null;
        for (int i=0; i<directions.length; i++){
            MapLocation loc = towerLoc.add(directions[i]);
            if (rc.canBuildRobot(UnitType.SOLDIER, loc) && rc.canSenseLocation(loc)){
                if (!rc.senseMapInfo(loc).getPaint().isEnemy()){
                    return loc; // prioritas utama: lokasi tanpa paint musuh
                }
                if (best == null){
                    best = loc; // simpan lokasi pertama yang bisa dibangun sebagai cadangan
                }
            }
        }
        return best;
    }

    private static void readTowerMessages(RobotController rc) throws GameActionException{
        Message[] messages = rc.readMessages(-1);
        for (int i=0; i<messages.length; i++){
            int data = messages[i].getBytes();
            int type = (data >> 16) & 0xFF;
            int x    = (data >>  8) & 0xFF;
            int y    =  data        & 0xFF;
            MapLocation loc = new MapLocation(x, y);

            switch (type) {
                case MSG_ENEMY_SPOTTED:
                    reportedEnemyLoc = loc;
                    if (rc.canAttack(loc)) rc.attack(loc);
                    break;
                case MSG_RUIN_FOUND:
                case MSG_NEED_BACKUP:
                    reportedRuinLoc = loc;
                    break;
            }
        }
    }

    static void broadcastToTower(RobotController rc, int msgType, MapLocation loc) throws GameActionException {
        int data = (msgType << 16) | (loc.x << 8) | loc.y;
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        for (int i = 0; i < allies.length; i++) {
            if (allies[i].type.isTowerType() && rc.canSendMessage(allies[i].location, data)) {
                rc.sendMessage(allies[i].location, data);
                return;
            }
        }
    }

    private static void attackNearbyEnemies(RobotController rc) throws GameActionException{
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        for (int i=0; i<enemies.length; i++){
            if (rc.canAttack(enemies[i].location)){
                rc.attack(enemies[i].location);
                System.out.println("Tower attacked enemy at "+enemies[i].location);
                break;
            }
        }
    }
    

    // Soldier Code
    public static void runSoldier(RobotController rc) throws GameActionException{
        MapLocation myLoc = rc.getLocation();
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(); // untuk sensing tile di sekitar
        MapLocation targetRuin = findBestRuin(rc, nearbyTiles);

        if (targetRuin!=null){
            int distance = myLoc.distanceSquaredTo(targetRuin);
            if (distance <= BUILD_RADIUS_SQ){
                buildTowerAtRuin(rc, targetRuin);
            } else {
                moveTowards(rc, targetRuin);
            }
        } else {
            exploreForRuins(rc, nearbyTiles);
        }
        paintCurrentTile(rc); 
    }

    private static MapLocation findBestRuin(RobotController rc, MapInfo[] tiles) throws GameActionException{
        MapLocation bestRuin=null;
        double maxScore = -1.0;
        MapLocation myLoc = rc.getLocation();

        for (int i=0; i<tiles.length; i++){
            if (!tiles[i].hasRuin()) continue;

            MapLocation ruinLoc = tiles[i].getMapLocation();

            RobotInfo bot = rc.senseRobotAtLocation(ruinLoc);
            if (bot != null && bot.type.isTowerType()) continue; // skip jika ada tower di lokasi ruin

            double distance = Math.sqrt(myLoc.distanceSquaredTo(ruinLoc));
            double score = RUIN_BASE_SCORE / Math.max(0.1, distance); // hindari pembagian dengan nol

            if (score>maxScore){
                maxScore = score;
                bestRuin = ruinLoc;
            }
        }
        return bestRuin;
    }

    private static void buildTowerAtRuin(RobotController rc, MapLocation ruinLoc) throws GameActionException {
        UnitType towerType = towerBuiltCount%2==0 ? UnitType.LEVEL_ONE_PAINT_TOWER : UnitType.LEVEL_ONE_MONEY_TOWER;

        if (rc.canMarkTowerPattern(towerType, ruinLoc)){
            rc.markTowerPattern(towerType, ruinLoc);
        }

        MapInfo[] pTiles = rc.senseNearbyMapInfos(ruinLoc, 8);
        for (int i=0; i<pTiles.length; i++){
            PaintType mark = pTiles[i].getMark();
            PaintType paint = pTiles[i].getPaint();

            if (mark != PaintType.EMPTY && mark != paint){
                boolean sec = (mark == PaintType.ALLY_SECONDARY);
                if (rc.canAttack(pTiles[i].getMapLocation())){
                    rc.attack(pTiles[i].getMapLocation(),sec);
                    return; // cuma bisa sekali nyerang/ngecat
                }
            }
        }

        if (rc.canCompleteTowerPattern(towerType, ruinLoc)){
            rc.completeTowerPattern(towerType, ruinLoc);
            towerBuiltCount++;
            System.out.println("Tower created");
        }
    }

    public static void exploreForRuins(RobotController rc, MapInfo[] tiles) throws GameActionException {
        if (!rc.isMovementReady()) return;

        MapLocation bestLoc = null;
        int maxEmpty = 0;

        for (int i=0; i<directions.length; i++){
            if (rc.canMove(directions[i])) continue;

            MapLocation nextLoc = rc.getLocation().add(directions[i]);
            int emptyCount = 0;

            for (int j=0; j<tiles.length; j++){
                if (tiles[j].getPaint() == PaintType.EMPTY && tiles[j].getMapLocation().distanceSquaredTo(nextLoc) <= 8){
                    emptyCount++;
                }

                if (emptyCount > maxEmpty){
                    maxEmpty = emptyCount;
                    bestLoc = nextLoc;
                }
            }
        }

        if (bestLoc != null && maxEmpty >0){
            moveTowards(rc, bestLoc);
        } else {
            Direction dir = directions[rng.nextInt(directions.length)]; // random walk jika tidak ada lokasi yang jelas untuk dijelajahi
            if (rc.canMove(dir)){
                rc.move(dir);
            }
        }
    }

    // Mopper Code
    public static void runMopper(RobotController rc) throws GameActionException {
        if (rc.getPaint() < LOW_PAINT_THRESHOLD){
            retreatForPaint(rc); // jika paint rendah, mundur untuk mengisi ulang
            return;
        }

        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        MapLocation myTower = findNearestAllyTower(rc, allies);
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(); //
        MapInfo enemyPaint = findEnemyPaintNearBase(rc, nearbyTiles, myTower);

        if (enemyPaint != null){
            MapLocation eLoc = enemyPaint.getMapLocation(); // eloc = enemy location
            if (rc.canAttack(eLoc)){
                rc.attack(eLoc);
                return;
            } else {
                moveTowards(rc, eLoc);
                return;
            }
        }

        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        Direction swingDir = findBestMopSwingDirection(rc, enemies); // swingDir = direction untuk nge-mop

        if (swingDir != null && rc.canMopSwing(swingDir)){
            rc.mopSwing(swingDir);
            return;
        }

        patrolAroundTower(rc, myTower); // untuk menjaga area sekitar tower tetap bersih dari paint musuh
    }

    private static MapLocation findNearestAllyTower(RobotController rc, RobotInfo[] allies){
        MapLocation tLoc = null;
        int minDistance = 999999; // nilai besar untuk inisialisasi
        for (int i = 0; i<allies.length; i++){
            if (allies[i].type.isTowerType()){
                int d = rc.getLocation().distanceSquaredTo(allies[i].location);
                if (d < minDistance){
                    minDistance = d;
                    tLoc = allies[i].location;
                }
            }
        }
        return tLoc;
    }

    private static MapInfo findEnemyPaintNearBase(RobotController rc, MapInfo[] tiles, MapLocation towerLoc){
        MapInfo best = null;
        int maxPrio = -1;

        for (int i=0; i<tiles.length; i++){
            if (tiles[i].getPaint().isEnemy()){
                int prio = 100;
                MapLocation tLoc = tiles[i].getMapLocation();

                if (towerLoc != null){
                    int dTower = tLoc.distanceSquaredTo(towerLoc);
                    if (dTower <= PATROL_RADIUS_SQ){
                        prio += (PATROL_RADIUS_SQ-dTower)*10;
                    }
                }
                prio-=rc.getLocation().distanceSquaredTo(tLoc);

                if (prio>maxPrio && rc.canAttack(tLoc)){
                    maxPrio=prio;
                    best = tiles[i];
                }
            }
        }
        return best;
    }

    private static void patrolAroundTower(RobotController rc, MapLocation towerLoc) throws GameActionException{
        if (!rc.isMovementReady()) return;

        if (towerLoc!=null){
            int d = rc.getLocation().distanceSquaredTo(towerLoc);
            if (d>PATROL_RADIUS_SQ){
                moveTowards(rc, towerLoc);
            } else if (d<=2){
                Direction away = towerLoc.directionTo(rc.getLocation());
                if (rc.canMove(away)) rc.move(away);
            } else {
                Direction pDir = rc.getLocation().directionTo(towerLoc).rotateLeft().rotateLeft();
                if (rc.canMove(pDir)) rc.move(pDir);
                else if (rc.canMove(pDir.rotateLeft())) rc.move(pDir.rotateLeft());
                else if (rc.canMove(pDir.rotateRight())) rc.move(pDir.rotateRight());
            }
        } else {
            Direction d = directions[rng.nextInt(8)];
            if (rc.canMove(d)) rc.move(d);
        }
    }

    private static Direction findBestMopSwingDirection(RobotController rc, RobotInfo[] enemies) throws GameActionException{
        if (enemies.length == 0) return null;
        Direction best = null;
        int maxC = 0;
        
        for (int i = 0; i < directions.length; i++) {
            if (!rc.canMopSwing(directions[i])) continue;
            int c = 0;
            for (int j = 0; j < enemies.length; j++) {
                Direction toE = rc.getLocation().directionTo(enemies[j].location);
                if (directions[i] == toE || directions[i].rotateLeft() == toE || directions[i].rotateRight() == toE) {
                    c++;
                }
            }
            if (c > maxC) {
                maxC = c;
                best = directions[i];
            }
        }
        return best;       
    }

    private static void retreatForPaint(RobotController rc) throws GameActionException {
        if (!rc.isMovementReady()) return;
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        MapLocation tLoc = findNearestAllyTower(rc, allies);
        
        if (tLoc != null) {
            moveTowards(rc, tLoc);
        } else {
            Direction d = directions[rng.nextInt(8)];
            if (rc.canMove(d)) rc.move(d);
        }
    }

    // Splasher Code
    public static void runSplasher(RobotController rc) throws GameActionException {
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        MapLocation targetRuin = findRuinNeedingPaint(rc, nearbyTiles);
        
        if (targetRuin != null) {
            if (rc.getLocation().distanceSquaredTo(targetRuin) <= SOLDIER_ATTACK_RADIUS_SQ) {
                MapInfo tTile = findBestPaintTarget(rc, targetRuin);
                if (tTile != null && rc.canAttack(tTile.getMapLocation())) {
                    rc.attack(tTile.getMapLocation(), tTile.getMark() == PaintType.ALLY_SECONDARY);
                }
            } else {
                moveTowards(rc, targetRuin);
            }
        } else {
            MapLocation unpainted = findUnpaintedArea(rc, nearbyTiles);
            if (unpainted != null) {
                if (rc.canAttack(unpainted)) rc.attack(unpainted);
                else moveTowards(rc, unpainted);
            } else {
                exploreForRuins(rc, nearbyTiles);
            }
        }
        paintCurrentTile(rc);
    }
    
    private static MapLocation findRuinNeedingPaint(RobotController rc, MapInfo[] tiles) throws GameActionException {
        for (int i = 0; i < tiles.length; i++) {
            if (tiles[i].hasRuin()) {
                MapLocation rLoc = tiles[i].getMapLocation();
                RobotInfo bot = rc.senseRobotAtLocation(rLoc);
                if (bot != null && bot.type.isTowerType()) continue;
                
                MapInfo[] pTiles = rc.senseNearbyMapInfos(rLoc, 8);
                for (int j = 0; j < pTiles.length; j++) {
                    if (pTiles[j].getMark() != PaintType.EMPTY && pTiles[j].getMark() != pTiles[j].getPaint()) {
                        return rLoc;
                    }
                }
            }
        }
        return null;
    }
    
    private static MapInfo findBestPaintTarget(RobotController rc, MapLocation ruinLoc) throws GameActionException {
        MapInfo[] pTiles = rc.senseNearbyMapInfos(ruinLoc, 8);
        MapInfo best = null;
        int minD = 999999;
        
        for (int i = 0; i < pTiles.length; i++) {
            if (pTiles[i].getMark() != PaintType.EMPTY && pTiles[i].getMark() != pTiles[i].getPaint()) {
                int d = rc.getLocation().distanceSquaredTo(pTiles[i].getMapLocation());
                if (d < minD && rc.canAttack(pTiles[i].getMapLocation())) {
                    minD = d;
                    best = pTiles[i];
                }
            }
        }
        return best;
    }
    
    private static MapLocation findUnpaintedArea(RobotController rc, MapInfo[] tiles) {
        MapLocation best = null;
        int maxC = 0;
        
        for (int i = 0; i < tiles.length; i++) {
            if (tiles[i].getPaint() == PaintType.EMPTY && !tiles[i].hasRuin() && tiles[i].isPassable()) {
                MapLocation tLoc = tiles[i].getMapLocation();
                if (rc.canAttack(tLoc)) {
                    int c = 0;
                    for (int j = 0; j < tiles.length; j++) {
                        if (tiles[j].getPaint() == PaintType.EMPTY && tiles[j].getMapLocation().distanceSquaredTo(tLoc) <= 2) {
                            c++;
                        }
                    }
                    if (c > maxC) {
                        maxC = c;
                        best = tLoc;
                    }
                }
            }
        }
        return best;
    }

    // Utils function
    private static void moveTowards(RobotController rc, MapLocation target) throws GameActionException {
        if (!rc.isMovementReady()) return;
        
        Direction dir = rc.getLocation().directionTo(target);
        if (rc.canMove(dir)) {
            rc.move(dir);
            return;
        }
        
        if (rc.canMove(dir.rotateLeft())) {
            rc.move(dir.rotateLeft());
        } else if (rc.canMove(dir.rotateRight())) {
            rc.move(dir.rotateRight());
        }
    }
    
        private static void paintCurrentTile(RobotController rc) throws GameActionException {
            MapLocation myLoc = rc.getLocation();
            if (!rc.senseMapInfo(myLoc).getPaint().isAlly() && rc.canAttack(myLoc)) {
                rc.attack(myLoc);
            }
        }
    }