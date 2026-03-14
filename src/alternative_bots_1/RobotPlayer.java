package alternative_bots_1;

import battlecode.common.*;

import java.util.Random;


/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public class RobotPlayer {
    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final Random rng = new Random(6147);

    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    public static void run(RobotController rc) throws GameActionException {
        // Hello world! Standard output is very useful for debugging.
        // Everything you say here will be directly viewable in your terminal when you run a match!
        // Logika robot yang dijalankan dari awal sampai akhir ronde
        System.out.println("I'm alive");

        // You can also use indicators to save debug notes in replays.
        rc.setIndicatorString("Hello world!");

        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            turnCount += 1;  // We have now been alive for one more turn!

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                // The same run() function is called for every robot on your team, even if they are
                // different types. Here, we separate the control depending on the UnitType, so we can
                // use different strategies on different robots. If you wish, you are free to rewrite
                // this into a different control structure!
                switch (rc.getType()){
                    case SOLDIER: runSoldier(rc); break; 
                    case MOPPER: runMopper(rc); break;
                    case SPLASHER: runSplasher(rc); break; // Consider upgrading examplefuncsplayer to use splashers!
                    default: runTower(rc); break;
                    }
                }
            catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println("GameActionException");
                e.printStackTrace();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println("Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }

    /**
     * Run a single turn for towers.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    public static void runTower(RobotController rc) throws GameActionException{
        // Pick a direction to build in.
        // Perintah-perintah dan mengatur perilaku Tower
        Direction buildDir = null;
        for (Direction d : directions) {
            if (rc.canBuildRobot(UnitType.SOLDIER, rc.getLocation().add(d))) {
                buildDir = d;
                break;
            }
        }

        if (buildDir != null){
            MapLocation buildLoc = rc.getLocation().add(buildDir);
            int currentPaint = rc.getPaint();

            if (currentPaint < 100){
                if (rc.canBuildRobot(UnitType.MOPPER, buildLoc)) {
                    rc.buildRobot(UnitType.MOPPER, buildLoc);
                    System.out.println("BUILT A MOPPER");
                }
            }
            else if (currentPaint > 300){
                if (rc.canBuildRobot(UnitType.SPLASHER, buildLoc)) {
                    rc.buildRobot(UnitType.SPLASHER, buildLoc);
                    System.out.println("BUILT A SPLASHER");
                }
            }
            else{
                if (rc.canBuildRobot(UnitType.SOLDIER, buildLoc)) {
                    rc.buildRobot(UnitType.SOLDIER, buildLoc);
                    System.out.println("BUILT A SOLDIER");
                }
            }

            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            if (enemies.length > 0) {
                if (rc.canAttack(enemies[0].getLocation())) {
                    rc.attack(enemies[0].getLocation());
                }
            }
        }

        // Read incoming messages
        Message[] messages = rc.readMessages(-1);
        for (Message m : messages) {
            System.out.println("Tower received message: '#" + m.getSenderID() + " " + m.getBytes());
        }

    }


    /**
     * Run a single turn for a Soldier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    public static void runSoldier(RobotController rc) throws GameActionException{
        // Sense information about all visible nearby tiles.
        // Mengatur perilaku Soldier-Soldier
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        MapLocation myLoc = rc.getLocation();
        MapLocation targetTowerLoc = null;
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
         // enemyLoc sebagai penanda robot musuh atau petak musuh
        for (RobotInfo enemy : enemies) {
            // Cek apakah tipe robot musuh adalah salah satu jenis Tower
            if (enemy.getType() == UnitType.LEVEL_ONE_MONEY_TOWER || 
                enemy.getType() == UnitType.LEVEL_ONE_PAINT_TOWER || 
                enemy.getType() == UnitType.LEVEL_ONE_DEFENSE_TOWER) {
                
                targetTowerLoc = enemy.getLocation();
                break; // Greedy: Serang tower pertama yang terlihat
            }
        }

        if (targetTowerLoc != null) {
            Direction dir = myLoc.directionTo(targetTowerLoc);
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
            
            if (rc.canAttack(targetTowerLoc)) {
                rc.attack(targetTowerLoc);
            }
        } 
        else if (enemies.length > 0) {
            Direction dir = myLoc.directionTo(enemies[0].getLocation());
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
        }
        else {
            for (MapInfo tile : nearbyTiles) {
                if (tile.getPaint().isEnemy()) {
                    Direction dir = myLoc.directionTo(tile.getMapLocation());
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                        break;
                    }
                }
            }
        }

        MapInfo currentTile = rc.senseMapInfo(myLoc);
        if (!currentTile.getPaint().isAlly() && rc.canAttack(myLoc)) {
            rc.attack(myLoc);
        }

        Message[] messages = rc.readMessages(-1);
        
        for (Message m : messages) {
            System.out.println("Robot #" + rc.getID() + " terima pesan dari #" + m.getSenderID() + ": " + m.getBytes());
            if (enemies.length == 0 && targetTowerLoc == null) {
                Direction randomPossibleDir = directions[rng.nextInt(directions.length)];
                if (rc.canMove(randomPossibleDir)) {
                    rc.move(randomPossibleDir);
                    rc.setIndicatorString("Searching detected enemys..!");
                }
            }
        }

    }


    /**
     * Run a single turn for a Mopper.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    public static void runMopper(RobotController rc) throws GameActionException{
        // Move and attack randomly.
        // Mengatur perilaku Mopper
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        MapLocation myLoc = rc.getLocation();
        MapLocation enemyLoc = null;
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
         // enemyLoc sebagai penanda robot musuh atau petak musuh
        if (enemies.length > 0) {
            enemyLoc = enemies[0].getLocation();
            // Direction dirToEnemy = rc.getLocation().directionTo(enemyLoc);
            
            // if (rc.canMove(dirToEnemy)){
            //     rc.move(dirToEnemy);
            // }
        }
        else{
            for (MapInfo tile : nearbyTiles) {
                if (tile.getPaint().isEnemy()) {
                    enemyLoc = tile.getMapLocation();
                    break;
                    // if (rc.canAttack(loc)) {
                    //     rc.attack(loc);
                    //     break;
                    // }
                }
            }
        }

        if (enemyLoc != null) {
            Direction dir = myLoc.directionTo(enemyLoc);
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
        }

        boolean acted = false;
        for (Direction d : directions){
            if (rc.canMopSwing(d)){
                rc.mopSwing(d);
                acted = true;
                break;
            }
        }
        
        if (!acted){
            for (MapInfo tile : nearbyTiles) {
                if (tile.getPaint().isEnemy()) {
                    if (rc.canAttack(tile.getMapLocation())) {
                        rc.attack(tile.getMapLocation());
                        break;
                    }
                }
            }
        }
        
        // We can also move our code into different methods or classes to better organize it!
        // BotUtils.updateEnemyRobots(rc);
    }

    public static void runSplasher(RobotController rc) throws GameActionException{
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        MapLocation myLoc = rc.getLocation();
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        double paintPercentage = (double) rc.getPaint()/rc.getType().paintCapacity;

        if (paintPercentage < 0.30) {
            RobotInfo[] friendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
            MapLocation closestTower = null;

            for (RobotInfo friend : friendlyRobots) {
                UnitType type = friend.getType();
                if (type == UnitType.LEVEL_ONE_PAINT_TOWER || 
                    type == UnitType.LEVEL_ONE_MONEY_TOWER || 
                    type == UnitType.LEVEL_ONE_DEFENSE_TOWER){
                    closestTower = friend.getLocation();
                    break;
                }
            }

            if (closestTower != null) {
                Direction toTower = myLoc.directionTo(closestTower);
                if (myLoc.isAdjacentTo(closestTower) || myLoc.equals(closestTower)) {
                    rc.setIndicatorString("Refilling paint..");
                } 
                else if (rc.canMove(toTower)) {
                    rc.move(toTower);
                }
                return;
            }
        }

         // enemyLoc sebagai penanda robot musuh atau petak musuh
        if (enemies.length > 0) {
            Direction away = enemies[0].getLocation().directionTo(myLoc);
            if (rc.canMove(away)){
                rc.move(away);
            }
        }
        else{
            for (MapInfo tile : nearbyTiles) {
                if (tile.getPaint() == PaintType.EMPTY) {
                    // emptyTile itu not occupied oleh tim manapun
                    Direction emptyTile = myLoc.directionTo(tile.getMapLocation());
                    if (rc.canMove(emptyTile)){
                        rc.move(emptyTile);
                        break;
                    }
                }
            }
        }

        for (MapInfo tile : nearbyTiles) {
                if (tile.getPaint() == PaintType.EMPTY || tile.getPaint().isEnemy()) {
                    if (rc.canAttack(tile.getMapLocation())) {
                        rc.attack(tile.getMapLocation());
                        break;
                    }
                }
            }
    }

}
