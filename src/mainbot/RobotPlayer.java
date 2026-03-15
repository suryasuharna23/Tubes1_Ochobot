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
        TowerLogic.attackNearbyEnemies(rc);
    }
    

    // Soldier Code
    public static void runSoldier(RobotController rc) throws GameActionException{
        MapLocation myLoc = rc.getLocation();
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        MapLocation paintTarget = SplasherLogic.findUnpaintedArea(rc, nearbyTiles);
        MapLocation targetRuin = SoldierLogic.findBestRuin(rc, nearbyTiles);

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
            SoldierLogic.exploreForRuins(rc, nearbyTiles);
        }

        BotUtils.paintCurrentTile(rc);
    }

    // Mopper Code
    public static void runMopper(RobotController rc) throws GameActionException {
        if (rc.getPaint() < LOW_PAINT_THRESHOLD){
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

        MopperLogic.patrolAroundTower(rc, myTower);
    }

    // Splasher Code
    public static void runSplasher(RobotController rc) throws GameActionException {
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
        BotUtils.paintCurrentTile(rc);
    }

}