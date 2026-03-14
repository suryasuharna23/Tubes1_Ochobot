package alternative_bots_2;

import battlecode.common.*;
import java.util.Random;

public class BotUtils {
	private static final Random rng = new Random(2026);

	private static final Direction[] directions = {
		Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
		Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST,
	};

	public static void smartMove(RobotController rc) throws GameActionException {
		if(!rc.isMovementReady()) return;

		for (Direction dir : directions) {
			MapLocation next = rc.getLocation().add(dir);
			if(rc.canMove(dir) && !rc.senseMapInfo(next).getPaint().isAlly()) {
				rc.move(dir);

				if (rc.canAttack(rc.getLocation())) {
					rc.attack(rc.getLocation());
				}
				return;
			}

			Direction d = directions[rng.nextInt(8)];
			if(rc.canMove(d)) rc.move(d);
		}
	}
}
