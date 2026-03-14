package mainbot;

import battlecode.common.*;

import java.util.Random;

final class BotUtils {
    static final Direction[] DIRECTIONS = {
        Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
        Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST
    };

    private BotUtils() {
    }

    static void moveTowards(RobotController rc, MapLocation target) throws GameActionException {
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

    static void paintCurrentTile(RobotController rc) throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        if (!rc.senseMapInfo(myLoc).getPaint().isAlly() && rc.canAttack(myLoc)) {
            rc.attack(myLoc);
        }
    }

    static MapLocation findNearestAllyTower(RobotController rc, RobotInfo[] allies) {
        MapLocation tLoc = null;
        int minDistance = 999999;
        for (int i = 0; i < allies.length; i++) {
            if (allies[i].type.isTowerType()) {
                int d = rc.getLocation().distanceSquaredTo(allies[i].location);
                if (d < minDistance) {
                    minDistance = d;
                    tLoc = allies[i].location;
                }
            }
        }
        return tLoc;
    }

    static void randomMove(RobotController rc, Random rng) throws GameActionException {
        if (!rc.isMovementReady()) return;
        Direction d = DIRECTIONS[rng.nextInt(DIRECTIONS.length)];
        if (rc.canMove(d)) {
            rc.move(d);
        }
    }
}