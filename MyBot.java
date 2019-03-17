
import hlt.*;
//import hlt.Victory;

import java.util.*;
import java.util.stream.Collectors;

public class MyBot {

    public static void main(final String[] args) {
        Victory victory = new Victory();
        final long rngSeed;
        if (args.length > 1) {
            rngSeed = Integer.parseInt(args[1]);
        } else {
            // constant seed number for constant results and a deterministic solution over all
            rngSeed = 42;
        }
        victory.rnd.setSeed(rngSeed);

        // At this point "game" variable is populated with initial map data.
        // This is a good place to do computationally expensive start-up pre-processing.
        // As soon as you call "ready" function below, the 2 second per turn timer will start.
        victory.game.ready("QuantumGreedy");
        // maximum number of ships to be created
        final int maxShips = (int) Math.sqrt(victory.game.gameMap.height * victory.game.gameMap.width) / 2;

        Log.log("Successfully created bot! My Player ID is " + victory.game.myId
                + ". Bot rng seed is " + rngSeed + ".");

        for (;;) {
            victory.game.updateFrame();
            final Player me = victory.game.me;
            final GameMap gameMap = victory.game.gameMap;

            final ArrayList<Command> commandQueue = new ArrayList<>();
            // processes a vip list with the top% positions with halite on the
            // map
            victory.scanVipPositions();
            // variable for knowing if we have queued a ship spawn command in
            // this round
            boolean willSpawn = false;
            // checks whether we hit the maximum limit of ships to build
            if (me.ships.size() < maxShips
                    // and we have enough halite to build a new ship
                    && me.halite >= Constants.SHIP_COST
                    // and the position of the shipyard is not occupied
                    && !gameMap.at(me.shipyard).isOccupied()
                    // and there is no reason to avoid building a ship (for
                    // better scoring)
                    && !victory.shouldAvoidBuildingShips(maxShips)) {
                me.halite -= Constants.SHIP_COST;
                commandQueue.add(me.shipyard.spawn());
                willSpawn = true;
            }

            // attempt to compute the action of each ship
            for (final Ship ship : me.ships.values()) {
                // checks if the ship should be turned into a drop off
                if (victory.shouldTurnIntoDropOff(ship)
                        // and whether we have enough halite for transformation
                        && me.halite > Constants.DROPOFF_COST) {
                    me.halite -= Constants.DROPOFF_COST;
                    commandQueue.add(ship.makeDropoff());
                    continue;
                }

                // the direction which does not lead to a crash, initially nil
                Direction goodDir = null;
                // findDirections returns a "personalized" list, for every ship,
                // of close positions from the vips list
                for (Direction dir : victory.findDirections(ship)) {
                    // avoid going into a direction that could lead to a crash
                    if (!victory.canCrash(ship, dir)) {
                        goodDir = dir;
                        break;
                    }
                }

                // if no good direction was found, stay still
                if (goodDir == null) {
                    commandQueue.add(ship.stayStill());

                    continue;
                }

                final Position pos = ship.position.directionalOffset(goodDir);
                // the ship will stay still if there is halite to collect on
                // this position and it is not full
                if (gameMap.at(ship.position).halite > 64 && !ship.isFull()
                        // or if there will be a new spawned ship and there
                        // could be a collision
                        || me.shipyard.position.equals(pos) && willSpawn
                        // or if it hasn't got enough halite to move
                        || ship.halite - gameMap.at(ship).halite / 10 < 0) {
                    commandQueue.add(ship.stayStill());
                } else {
                    // otherwise move in the `best` direction
                    commandQueue.add(ship.move(gameMap.naiveNavigate(ship, pos)));
                }
            }

            victory.game.endTurn(commandQueue);
        }
    }
}


/*
public static void main(String[] args) {

        final long rngSeed;
        if (args.length > 1) {
            rngSeed = Integer.parseInt(args[1]);
        } else {
            // constant seed number for constant results and a deterministic solution over all
            rngSeed = 42;
        }
        victory.rnd.setSeed(rngSeed);

        Log.log("Successfully created bot! My Player ID is " + victory.game.myId
                + ". Bot rng seed is " + rngSeed + ".");
        if (args.length > 1) {
            rngSeed = Integer.parseInt(args[1]);
        } else {
            // constant seed number for constant results and a deterministic solution over all
            rngSeed = 42;
        }
        victory.rnd.setSeed(rngSeed);
        victory.game.ready("QuantumGreedy");

        for(;;) {
        victory.update();
        }
 */
