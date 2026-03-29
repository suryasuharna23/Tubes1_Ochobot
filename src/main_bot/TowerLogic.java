package main_bot;

import battlecode.common.*;

public class TowerLogic {
	public static MapLocation findBestSpawnLocation(RobotController rc, MapLocation towerLoc) throws GameActionException {
		MapLocation best = null;
		for (int i = 0; i < BotUtils.DIRECTIONS.length; i++) {
			MapLocation loc = towerLoc.add(BotUtils.DIRECTIONS[i]);
			if (rc.canBuildRobot(UnitType.SOLDIER, loc) && rc.canSenseLocation(loc)) {
				if (!rc.senseMapInfo(loc).getPaint().isEnemy()) {
					return loc;
				}
				if (best==null) {
					best=loc;
				}
			}
		}
		return best;
	}

	public static void attackNearbyEnemies(RobotController rc) throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		for (int i = 0; i < enemies.length; i++) {
			if (rc.canAttack(enemies[i].location)) {
				rc.attack(enemies[i].location);
				break;
			}
		}
	}
}
