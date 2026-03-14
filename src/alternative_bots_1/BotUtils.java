package alternative_bots_1;

import battlecode.common.*;

public class BotUtils {
	public static void updateEnemyRobots(RobotController rc) throws GameActionException{
		// Sensing methods can be passed in a radius of -1 to automatically 
		// use the largest possible value.
		// Untuk mendeteksi musuh di sekitar dan mengirimkan pesan ke teman jika ada ancaman
		RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		if (enemyRobots.length != 0){
			rc.setIndicatorString("There are nearby enemy robots! Scary! AAAA");
			// Save an array of locations with enemy robots in them for possible future use.
			MapLocation[] enemyLocations = new MapLocation[enemyRobots.length];
			for (int i = 0; i < enemyRobots.length; i++){
				enemyLocations[i] = enemyRobots[i].getLocation();
			}
			RobotInfo[] allyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
			// Occasionally try to tell nearby allies how many enemy robots we see.
			if (rc.getRoundNum() % 20 == 0){
				for (RobotInfo ally : allyRobots){
					if (rc.canSendMessage(ally.location, enemyRobots.length)){
						rc.sendMessage(ally.location, enemyRobots.length);
					}
				}
			}
		}
	}
}
