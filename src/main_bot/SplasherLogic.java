package main_bot;

import battlecode.common.*;

public class SplasherLogic {
	public static MapLocation findRuinNeedingPaint(RobotController rc, MapInfo[] tiles) throws GameActionException {
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
	
	public static MapInfo findBestPaintTarget(RobotController rc, MapLocation ruinLoc) throws GameActionException {
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
	
	public static MapLocation findUnpaintedArea(RobotController rc, MapInfo[] tiles) {
		MapLocation best = null;
		int bestScore = Integer.MIN_VALUE;
		MapLocation myLoc = rc.getLocation();
		
		for (int i = 0; i < tiles.length; i++) {
			if ((tiles[i].getPaint() == PaintType.EMPTY || tiles[i].getPaint().isEnemy()) && !tiles[i].hasRuin() && tiles[i].isPassable()) {
				MapLocation tLoc = tiles[i].getMapLocation();
				int score = tiles[i].getPaint().isEnemy() ? 120 : 60;
				int d = myLoc.distanceSquaredTo(tLoc);

				// imp bytecode O(n): enemy paint > empty, penalti jarak biar ttp efisien.
				score -= d;
				if (rc.canAttack(tLoc)) {
					score += 20;
				}

				if (score > bestScore) {
					bestScore = score;
					best = tLoc;
				}
			}
		}
		return best;
	}
}
