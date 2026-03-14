package mainbot;

import battlecode.common.*;

public class MopperLogic {
	public static MapInfo findEnemyPaintNearBase(RobotController rc, MapInfo[] tiles, MapLocation towerLoc){
		MapInfo best = null;
		int maxPrio = -1;

		for (int i=0; i<tiles.length; i++){
			if (tiles[i].getPaint().isEnemy()){
				int prio = 100;
				MapLocation tLoc = tiles[i].getMapLocation();

				if (towerLoc != null){
					int dTower = tLoc.distanceSquaredTo(towerLoc);
					if (dTower <= RobotPlayer.PATROL_RADIUS_SQ){
						prio += (RobotPlayer.PATROL_RADIUS_SQ-dTower)*10;
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

	public static void patrolAroundTower(RobotController rc, MapLocation towerLoc) throws GameActionException{
		if (!rc.isMovementReady()) return;

		if (towerLoc!=null){
			int d = rc.getLocation().distanceSquaredTo(towerLoc);
			if (d>RobotPlayer.PATROL_RADIUS_SQ){
				BotUtils.moveTowards(rc, towerLoc);
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
			Direction d = BotUtils.DIRECTIONS[RobotPlayer.rng.nextInt(8)];
			if (rc.canMove(d)) rc.move(d);
		}
	}

	public static Direction findBestMopSwingDirection(RobotController rc, RobotInfo[] enemies) throws GameActionException{
		if (enemies.length == 0) return null;
		Direction best = null;
		int maxC = 0;
		
		for (int i = 0; i < BotUtils.DIRECTIONS.length; i++) {
			if (!rc.canMopSwing(BotUtils.DIRECTIONS[i])) continue;
			int c = 0;
			for (int j = 0; j < enemies.length; j++) {
				Direction toE = rc.getLocation().directionTo(enemies[j].location);
				if (BotUtils.DIRECTIONS[i] == toE || BotUtils.DIRECTIONS[i].rotateLeft() == toE || BotUtils.DIRECTIONS[i].rotateRight() == toE) {
					c++;
				}
			}
			if (c > maxC) {
				maxC = c;
				best = BotUtils.DIRECTIONS[i];
			}
		}
		return best;       
	}

	public static void retreatForPaint(RobotController rc) throws GameActionException {
		if (!rc.isMovementReady()) return;
		RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
		MapLocation tLoc = BotUtils.findNearestAllyTower(rc, allies);
		
		if (tLoc != null) {
			BotUtils.moveTowards(rc, tLoc);
		} else {
			Direction d = BotUtils.DIRECTIONS[RobotPlayer.rng.nextInt(8)];
			if (rc.canMove(d)) rc.move(d);
		}
	}
}
