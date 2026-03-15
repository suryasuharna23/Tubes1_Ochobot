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
    static final int MAX_TOWERS = 25;
    static final int TOWER_UPGRADE_CHIPS_THRESHOLD = 2500;

    // inisiasi trackers
    static int towerBuiltCount = 0;
    static int unitSpawnCount = 0;

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
        Message[] towerMsgs = rc.readMessages(-1);

        if (!rc.isActionReady()){
            TowerLogic.attackNearbyEnemies(rc);
            return;
        }

        MapLocation myLoc = rc.getLocation();
        MapLocation spawnLoc = TowerLogic.findBestSpawnLocation(rc, myLoc);

        if (spawnLoc == null){
            TowerLogic.attackNearbyEnemies(rc);
            return;
        } // jika spawnLoc null, berarti tidak ada lokasi yang cocok untuk spawn, jadi kita skip spawn dan fokus ke attack

        if (unitSpawnCount > 0 && unitSpawnCount % 4 == 0 && rc.canBuildRobot(UnitType.MOPPER, spawnLoc)) {
            rc.buildRobot(UnitType.MOPPER, spawnLoc);
            unitSpawnCount++; // unitSpawnCount mod 4 = spawn Mopper tiap 4 spawn biar lebih seimbang  
        } else if (rc.canBuildRobot(UnitType.SPLASHER, spawnLoc)){
            rc.buildRobot(UnitType.SPLASHER, spawnLoc);
            unitSpawnCount++;
        } else if (unitSpawnCount % 3 == 0 && rc.canBuildRobot(UnitType.SOLDIER, spawnLoc)){
            rc.buildRobot(UnitType.SOLDIER, spawnLoc);
            unitSpawnCount++; // unitSpawnCount mod 3 = spawn Soldier tiap 3 spawn
        }

        if (towerMsgs.length == 0 && (rc.getRoundNum() % 10 == 0)) {
            RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
            for (int i = 0; i < allies.length; i++) {
                if (!allies[i].type.isTowerType() && rc.canSendMessage(allies[i].location, rc.getRoundNum())) {
                    rc.sendMessage(allies[i].location, rc.getRoundNum());
                    break;
                }
            }
        }

        TowerLogic.attackNearbyEnemies(rc);
    }
    

    // Soldier Code
    public static void runSoldier(RobotController rc) throws GameActionException{
        Message[] messages = rc.readMessages(-1);

        if (rc.getPaint() == 0 && rc.getHealth() <= 20 && rc.getRoundNum() > 100) {
            rc.disintegrate(); // jika paint habis dan health rendah, lebih baik mati daripada jadi beban untuk tim, terutama di late game
            return;
        }

        if (rc.isActionReady() && rc.getPaint() < LOW_PAINT_THRESHOLD) {
            RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
            for (int i = 0; i < allies.length; i++) {
                if (allies[i].type.isTowerType() && rc.canTransferPaint(allies[i].location, -40)) {
                    rc.transferPaint(allies[i].location, -40);
                    break;
                }
            }
        } 

        MapLocation myLoc = rc.getLocation();
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        MapLocation paintTarget = SplasherLogic.findUnpaintedArea(rc, nearbyTiles);
        MapLocation targetRuin = SoldierLogic.findBestRuin(rc, nearbyTiles);

        if (rc.getRoundNum() % 12 == 0 && rc.canMarkResourcePattern(myLoc)) {
            rc.markResourcePattern(myLoc); 
        }

        MapInfo myTile = rc.senseMapInfo(myLoc);
        if (myTile.isPassable() && !myTile.hasRuin()) {
            if (myTile.getPaint() == PaintType.EMPTY && myTile.getMark() == PaintType.EMPTY) {
                rc.mark(myLoc, false);
            } else if (myTile.getPaint().isAlly() && myTile.getMark().isAlly()) {
                rc.removeMark(myLoc);
            }
        }

        if (messages.length > 0) {
            rc.setIndicatorString("Soldier recv=" + messages.length);
        }

        if ((rc.getRoundNum() % 10 == 0) && targetRuin != null) {
            RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
            MapLocation nearestTower = BotUtils.findNearestAllyTower(rc, allies);
            if (nearestTower != null && rc.canSendMessage(nearestTower, targetRuin.x * 100 + targetRuin.y)) {
                rc.sendMessage(nearestTower, targetRuin.x * 100 + targetRuin.y);
            } 
        }

        if (paintTarget != null) {
            if (targetRuin != null && myLoc.distanceSquaredTo(targetRuin)<=BUILD_RADIUS_SQ
                && rc.isActionReady() && !rc.canAttack(paintTarget)) {
                SoldierLogic.buildTowerAtRuin(rc, targetRuin);
                return;
            }

            if (rc.canAttack(paintTarget)) {
                rc.attack(paintTarget);
            } else {
                BotUtils.moveTowards(rc, paintTarget);
            }
            BotUtils.paintCurrentTile(rc);
            return;
        }

        if (targetRuin != null){
            int distance = myLoc.distanceSquaredTo(targetRuin);
            if (distance <= BUILD_RADIUS_SQ){
                SoldierLogic.buildTowerAtRuin(rc, targetRuin);
            } else {
                BotUtils.moveTowards(rc, targetRuin);
            }
        } else {
            if (rc.isActionReady()
                && rc.getChips() >= TOWER_UPGRADE_CHIPS_THRESHOLD
                && (rc.getRoundNum() % 6 == 0)) {
                for (int i = 0; i < BotUtils.DIRECTIONS.length; i++) {
                    MapLocation adj = myLoc.add(BotUtils.DIRECTIONS[i]);
                    if (rc.canUpgradeTower(adj)) {
                        rc.upgradeTower(adj);
                        return;
                    }
                }
            }
            SoldierLogic.exploreForRuins(rc, nearbyTiles);
        }

        BotUtils.paintCurrentTile(rc);
    }

    // Mopper Code
    public static void runMopper(RobotController rc) throws GameActionException {
        Message[] messages = rc.readMessages(-1);

        if (rc.isActionReady() && rc.getPaint() > 80) {
            RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
            for (int i = 0; i < allies.length; i++) {
                if (!allies[i].type.isTowerType() && allies[i].paintAmount < 40 && rc.canTransferPaint(allies[i].location, 20)) {
                    rc.transferPaint(allies[i].location, 20);
                    return;
                }
            }
        }

        if (rc.getPaint() < LOW_PAINT_THRESHOLD){
            if (rc.isActionReady()) {
                RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
                for (int i = 0; i < allies.length; i++) {
                    if (allies[i].type.isTowerType() && rc.canTransferPaint(allies[i].location, -40)) {
                        rc.transferPaint(allies[i].location, -40);
                        return;
                    }
                }
            }
            MopperLogic.retreatForPaint(rc); // jika paint rendah, mundur untuk mengisi ulang
            return;
        }

        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(); //
        MapLocation myTower = null;
        if ((rc.getRoundNum() & 1) == 0) {
            RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
            myTower = BotUtils.findNearestAllyTower(rc, allies);
        }
        MapInfo enemyPaint = MopperLogic.findEnemyPaintNearBase(rc, nearbyTiles, myTower);

        if (enemyPaint != null){
            MapLocation eLoc = enemyPaint.getMapLocation(); // eloc = enemy location
            if (rc.canAttack(eLoc)){
                rc.attack(eLoc);
                return;
            } else {
                BotUtils.moveTowards(rc, eLoc);
                return;
            }
        }

        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        Direction swingDir = MopperLogic.findBestMopSwingDirection(rc, enemies); // swingDir = direction untuk nge-mop

        if (swingDir != null && rc.canMopSwing(swingDir)){
            rc.mopSwing(swingDir);
            return;
        }

        if (messages.length > 0) {
            rc.setIndicatorString("Mopper recv=" + messages.length);
        }

        MopperLogic.patrolAroundTower(rc, myTower);
    }

    // Splasher Code
    public static void runSplasher(RobotController rc) throws GameActionException {
        Message[] messages = rc.readMessages(-1);

        if (rc.isActionReady() && rc.getPaint() < LOW_PAINT_THRESHOLD) {
            RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
            for (int i = 0; i < allies.length; i++) {
                if (allies[i].type.isTowerType() && rc.canTransferPaint(allies[i].location, -30)) {
                    rc.transferPaint(allies[i].location, -30);
                    break;
                }
            }
        }

        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();

        MapLocation greedyPaint = SplasherLogic.findUnpaintedArea(rc, nearbyTiles);
        MapLocation targetRuin = null;
        if ((rc.getRoundNum() & 1) == 0 || greedyPaint == null) {
            targetRuin = SplasherLogic.findRuinNeedingPaint(rc, nearbyTiles);
        } // ngecat dulu baru cari ruin yg butuh paint

        if (greedyPaint != null && rc.canAttack(greedyPaint)) {
            rc.attack(greedyPaint);
            if (rc.isMovementReady() && (rc.getRoundNum() & 1) == 0) {
                RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
                MapLocation nearestTower = BotUtils.findNearestAllyTower(rc, allies);
                if (nearestTower != null && rc.getLocation().distanceSquaredTo(nearestTower) <= PATROL_RADIUS_SQ) {
                    Direction outDir = nearestTower.directionTo(rc.getLocation());
                    if (rc.canMove(outDir)) {
                        rc.move(outDir);
                    } else if (rc.canMove(outDir.rotateLeft())) {
                        rc.move(outDir.rotateLeft());
                    } else if (rc.canMove(outDir.rotateRight())) {
                        rc.move(outDir.rotateRight());
                    }
                }
            }
            return;
        }
        
        if (targetRuin != null) {
            if (rc.getLocation().distanceSquaredTo(targetRuin) <= SOLDIER_ATTACK_RADIUS_SQ) {
                boolean shouldScanRuinPattern = rc.isActionReady() && (((rc.getRoundNum() & 1) == 0) || greedyPaint == null);
                if (shouldScanRuinPattern) {
                    MapInfo tTile = SplasherLogic.findBestPaintTarget(rc, targetRuin);
                    if (tTile != null) {
                        rc.attack(tTile.getMapLocation(), tTile.getMark() == PaintType.ALLY_SECONDARY);
                        return;
                    }
                }

                if (greedyPaint != null) {
                    BotUtils.moveTowards(rc, greedyPaint);
                }
            } else {
                BotUtils.moveTowards(rc, targetRuin);
            }
        } else {
            if (greedyPaint != null) {
                BotUtils.moveTowards(rc, greedyPaint);
            } else {
                SoldierLogic.exploreForRuins(rc, nearbyTiles);
            }
        }

        if (messages.length > 0) {
            rc.setIndicatorString("Splasher recv=" + messages.length);
        }

        BotUtils.paintCurrentTile(rc);
    }

}