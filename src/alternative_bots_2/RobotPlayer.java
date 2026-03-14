package alternative_bots_2;

import battlecode.common.*;
import java.util.*;

public class RobotPlayer {
    static final Random rng = new Random(2026);

    static final Direction[] directions = {
        Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
        Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST,
    };

    public static void run(RobotController rc) throws GameActionException {
        while(true) {
            try {
                switch(rc.getType()) {
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
                        runTower(rc);
                        break;                        
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }

    public static void runTower(RobotController rc) throws GameActionException{
        if(!rc.isActionReady()) return;

        int chips = rc.getChips();
        int paint = rc.getPaint();

        if(chips >= 4000 && chips < 5000) {
            rc.setIndicatorString("Chips ditabung untuk upgrade tower level 3");
            return;
        }

        UnitType toBuild = null;
        if(paint >= 300 && chips >= 400) {
            toBuild = UnitType.SPLASHER;
        } else if(paint >= 200 && chips >= 250) {
            toBuild = UnitType.SOLDIER;
        } else if(paint >= 100 && chips >= 300) {
            toBuild = UnitType.MOPPER;
        }

        if(toBuild == null && chips >= 250 && paint >= 150) {
            toBuild = UnitType.SOLDIER;
        }

        if(toBuild != null) {
            for (Direction dir : directions) {
                MapLocation spawnLoc = rc.getLocation().add(dir);
                if(rc.canBuildRobot(toBuild, spawnLoc)) {
                    rc.buildRobot(toBuild, spawnLoc);
                    return;
                }
            }
        }
    }

    public static void runSoldier(RobotController rc) throws GameActionException {
        if(rc.getChips() >= 5000) {
            for(Direction dir : directions) {
                MapLocation adj = rc.getLocation().add(dir);
                if(rc.canUpgradeTower(adj)) {
                    rc.upgradeTower(adj);
                    return;
                }
            }
        }

        MapInfo[] nearby = rc.senseNearbyMapInfos();
        for(MapInfo tile : nearby) {
            if(tile.hasRuin()) {
                MapLocation ruinLoc = tile.getMapLocation();
                if(rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, ruinLoc)) {
                    rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, ruinLoc);
                    return;
                }
            }
        }
        BotUtils.smartMove(rc);
    }

    public static void runMopper(RobotController rc) throws GameActionException {
        if (rc.getPaint() > 20) {
            for(Direction dir : directions) {
                MapLocation adj = rc.getLocation().add(dir);
                if(rc.canTransferPaint(adj, 20)) {
                    rc.transferPaint(adj, 20);
                    return;
                }
            }
        }

        for (Direction dir : directions) {
            if(rc.canMopSwing(dir)) {
                rc.mopSwing(dir);
                return;
            }
        }
        BotUtils.smartMove(rc);
    }

    public static void runSplasher(RobotController rc) throws GameActionException {
        if(rc.isActionReady()) {
            MapInfo[] tiles = rc.senseNearbyMapInfos();
            for(MapInfo tile : tiles) {
                if(tile.getPaint().isEnemy() && rc.canAttack(tile.getMapLocation())) {
                    rc.attack(tile.getMapLocation());
                    return;
                }
            }
        }
        BotUtils.smartMove(rc);
    }
}