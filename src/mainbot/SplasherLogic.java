package mainbot;

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
}
