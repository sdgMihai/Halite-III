import hlt.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class MyBot {
    /**
     * Returneaza cea mai buna directie in care sa mearga nava - adancime 1
     * @param ship nava pentru care se aplica greedy
     * @param gameMap harta jocului
     * @return pozitia pe care urmeaza sa mearga
     */
    public static Position Greedy (final Ship ship, final GameMap gameMap) {
        Position initial = ship.position;

        Position[]  positions =
                {
                    new Position(initial.x - 1, initial.y), // sus
                    new Position(initial.x, initial.y + 1), // dreapta
                    new Position(initial.x + 1, initial.y), // jos
                    new Position(initial.x, initial.y - 1)  // stanga
                };

        Arrays.sort(positions, new Comparator<Position>() {
            @Override
            public int compare(Position o1, Position o2) {
                return gameMap.at(o1).halite - gameMap.at(o2).halite;
            }
        });

        for (int i = 0; i < positions.length; ++i) {
            if (!gameMap.at(positions[i]).isOccupied()) {
                return positions[i];
            }
        }

        return initial;
    }

    /**
     * Functie care returneaza comenzi in legatura cu jocul in general
     * @param game referinta la joc
     * @return comanda
     */
    public static Command DecisionGame (final Game game) {

        // TODO daca playerul are mai mult de 3k halite (ales arbitrar) si runda e sub 200
        // TODO sa se faca o nava
        if (game.me.halite > 2000 && game.turnNumber < Constants.MAX_TURNS / 2) {
            return Command.spawnShip();
        }
        return null;
    }

    /**
     * Primeste o sursa si o destinatie si intoarce directia in care ar trebui sa mearga ca sa
     * ajunga destinatie
     * @param source sursa
     * @param destination destinatie
     * @return directia
     */
    public static Direction PositionToDirection (final Position source,
                                                 final Position destination) {
        if (source.x - destination.x == 0 && source.y - destination.y == 0) {
            return Direction.STILL;
        }

        if (source.x - destination.x == 0) {
            if (source.y - destination.y > 0) {
                return Direction.EAST;
            } else {
                return Direction.WEST;
            }
        }

        if (source.x - destination.x > 0 && source.y - destination.y == 0) {
            return Direction.NORTH;
        } else {
            return Direction.SOUTH;
        }
    }

    /**
     * Functie care returneaza comenzi in legatura cu o singura nava
     * @param ship nava
     * @param game jocul
     * @return comanda
     */
    public static Command DecisionShip (final Ship ship, final Game game, FileWriter fileWriter)
            throws IOException {
        GameMap gameMap = game.gameMap;
        Position closestDropoff = game.me.shipyard.position;

        int distance = game.gameMap.calculateDistance(ship.position, game.me.shipyard.position);
        for (Dropoff dropoff : game.me.dropoffs.values()) {
            int auxDistance = gameMap.calculateDistance(ship.position, dropoff.position);
            if (auxDistance < distance) {
                distance = auxDistance;
                closestDropoff = dropoff.position;
            }
        }

        // TODO daca conditia e adevarata sa se creeze un drop-off + conditia daca o nava a ajuns
        // TODO la mai mult de ~7 pozitii departare
        if (game.me.halite > 8000 && game.turnNumber < Constants.MAX_TURNS * 3 / 4 &&
                gameMap.calculateDistance(ship.position, closestDropoff) > 7 &&
                game.me.dropoffs.size() < 3) {
            gameMap.at(ship).markUnsafe(null);
            return Command.transformShipIntoDropoffSite(ship.id);
        }

        // TODO daca are mai mult de 4/5 halite - sa mearga la dropoff
        if (ship.halite > (Constants.MAX_HALITE * 3 / 5)) {
            //gameMap.at(pos).markUnsafe(ship);
            gameMap.at(ship).markUnsafe(null);
            return ship.move(PositionToDirection(ship.position, closestDropoff));
        }

        // TODO daca pozitia actuala nu mai are destule resurse, sa mearga in alta pozitie
        // TODO altfel ramane pe pozitia actuala
        if (gameMap.at(ship).halite < Constants.MAX_HALITE / 10) {
            Position pos = Greedy(ship, gameMap);

            // TODO daca pozitia actuala are mai putine resurse decat cea mai buna alta
            // TODO pozitie sa ramana aici (ca pe else)
            if (gameMap.at(ship).halite >= gameMap.at(pos).halite) {
                return ship.move(Direction.STILL);
            } else {
                gameMap.at(pos).markUnsafe(ship);
                gameMap.at(ship).markUnsafe(null);
                return ship.move(PositionToDirection(ship.position, pos));
            }
        } else {
            return ship.move(Direction.STILL);
        }
    }

    public static void main(final String[] args) throws IOException {
        final long rngSeed;
        if (args.length > 1) {
            rngSeed = Integer.parseInt(args[1]);
        } else {
            rngSeed = System.nanoTime();
        }
        final Random rng = new Random(rngSeed);

        Game game = new Game();
        // At this point "game" variable is populated with initial map data.
        // This is a good place to do computationally expensive start-up pre-processing.
        // As soon as you call "ready" function below, the 2 second per turn timer will start.
        game.ready("MyJavaBot");

        Log.log("Successfully created bot! My Player ID is " + game.myId +
                ". Bot rng seed is " + rngSeed + ".");
        for (;;) {
            game.updateFrame();
            FileWriter fileWriter = new FileWriter("log.out", true);
            final Player me = game.me;
            final GameMap gameMap = game.gameMap;

            final ArrayList<Command> commandQueue = new ArrayList<>();

            // TODO schimbat cu logica noua
            for (final Ship ship : me.ships.values()) {
                commandQueue.add(DecisionShip(ship, game, fileWriter));
            }

            if (
                game.turnNumber < Constants.MAX_TURNS / 2 &&
                me.halite >= Constants.SHIP_COST * 2 &&
                !gameMap.at(me.shipyard).isOccupied())
            {
                commandQueue.add(me.shipyard.spawn());
            }
            fileWriter.close();
            game.endTurn(commandQueue);
        }

    }
}
