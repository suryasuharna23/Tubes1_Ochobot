package mainbot;

import battlecode.common.*;

public class SoldierLogic {
	public static MapLocation findBestRuin(RobotController rc, MapInfo[] tiles) throws GameActionException {
		MapLocation bestRuin = null;
		int bestDist = Integer.MAX_VALUE;
		MapLocation myLoc = rc.getLocation();

		for (int i = 0; i < tiles.length; i++) {
			if (!tiles[i].hasRuin()) continue;

			MapLocation ruinLoc = tiles[i].getMapLocation();

			RobotInfo bot = rc.senseRobotAtLocation(ruinLoc);
			if (bot != null && bot.type.isTowerType()) continue; // skip jika ada tower di lokasi ruin

			int d2 = myLoc.distanceSquaredTo(ruinLoc); // priortas ruin terdekat
			if (d2<bestDist) {
				bestDist=d2;
				bestRuin=ruinLoc;
			}
		}
		return bestRuin;
	}

	public static void buildTowerAtRuin(RobotController rc, MapLocation ruinLoc) throws GameActionException {
		UnitType towerType = RobotPlayer.towerBuiltCount % 2 == 0
			? UnitType.LEVEL_ONE_PAINT_TOWER
			: UnitType.LEVEL_ONE_MONEY_TOWER;

		if (rc.canMarkTowerPattern(towerType, ruinLoc)) {
			rc.markTowerPattern(towerType, ruinLoc);
		}

		MapInfo[] pTiles = rc.senseNearbyMapInfos(ruinLoc, 8);
		for (int i = 0; i < pTiles.length; i++) {
			PaintType mark = pTiles[i].getMark();
			PaintType paint = pTiles[i].getPaint();

			if (mark != PaintType.EMPTY && mark != paint) {
				boolean sec = mark == PaintType.ALLY_SECONDARY;
				if (rc.canAttack(pTiles[i].getMapLocation())) {
					rc.attack(pTiles[i].getMapLocation(), sec);
					return; // cuma bisa sekali nyerang/ngecat
				}
			}
		}

		if (rc.canCompleteTowerPattern(towerType, ruinLoc)) {
			rc.completeTowerPattern(towerType, ruinLoc);
			RobotPlayer.towerBuiltCount++;
		}
	}

	public static void exploreForRuins(RobotController rc, MapInfo[] tiles) throws GameActionException {
		if (!rc.isMovementReady()) return;

		MapLocation bestLoc = null;
		int maxEmpty = 0;

		for (int i = 0; i < BotUtils.DIRECTIONS.length; i++) {
			if (!rc.canMove(BotUtils.DIRECTIONS[i])) continue;

			MapLocation nextLoc = rc.getLocation().add(BotUtils.DIRECTIONS[i]);
			int emptyCount = 0;

			for (int j = 0; j < tiles.length; j++) {
				if (tiles[j].getPaint() == PaintType.EMPTY && tiles[j].getMapLocation().distanceSquaredTo(nextLoc) <= 8) {
					emptyCount++;
				}

				if (emptyCount > maxEmpty) {
					maxEmpty = emptyCount;
					bestLoc = nextLoc;
				}
			}
		}

		if (bestLoc != null && maxEmpty > 0) {
			BotUtils.moveTowards(rc, bestLoc);
		} else {
			Direction dir = BotUtils.DIRECTIONS[RobotPlayer.rng.nextInt(BotUtils.DIRECTIONS.length)]; // random walk jika tidak ada lokasi yang jelas untuk dijelajahi
			if (rc.canMove(dir)) {
				rc.move(dir);
			}
		}
	}
}
