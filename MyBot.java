import hlt.*;

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
        return positions[0];
    }

    /**
     * Functie care returneaza comenzi in legatura cu jocul in general
     * @param game referinta la joc
     * @return comanda
     */
    public static Command DecisionGame (final Game game) {

        // TODO daca playerul are mai mult de 3k halite (ales arbitrar) si runda e sub 200
        // TODO sa se faca o nava
        if (game.me.halite > 3000 && game.turnNumber < Constants.MAX_TURNS / 2) {
            return null;
        }

        // TODO daca conditia e adevarata sa se creeze un drop-off + conditia daca o nava a ajuns
        // TODo la mai mult de ~8 pozitii departare
        if (game.me.halite > 8000 && game.turnNumber < Constants.MAX_TURNS * 3 / 4) {
            return null;
        }

        return null;
    }

    /**
     * Functie care returneaza comenzi in legatura cu o singura nava
     * @param ship nava
     * @param gameMap harta jocului
     * @return comanda
     */
    public static Command DecisionShip (final Ship ship, final GameMap gameMap) {

        // TODO daca are mai mult de 750 halite - sa mearga la dropoff
        if (ship.halite > (Constants.MAX_HALITE * 3 / 4)) {

            return null;
        }

        // TODO daca pozitia actuala nu mai are destule resurse sa mearga in alta pozitie
        // TODO altfel ramane pe pozitia actuala
        if (gameMap.cells[ship.position.x][ship.position.y].halite < (Constants.MAX_HALITE / 4)) {

            // TODO daca pozitia actuala are mai putine resurse decat cea mai buna alta
            // TODO pozitie sa ramana aici (ca pe else)
            int result = 0;
            if (gameMap.cells[ship.position.x][ship.position.y].halite > result) {

                return null;
            }

            return null;
        } else {

            return null;
        }
    }

    public static void main(final String[] args) {
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
            final Player me = game.me;
            final GameMap gameMap = game.gameMap;

            final ArrayList<Command> commandQueue = new ArrayList<>();

            Command command = DecisionGame(game); //

            // TODO schimbat cu logica noua
            for (final Ship ship : me.ships.values()) {
                if (gameMap.at(ship).halite < Constants.MAX_HALITE / 10 || ship.isFull()) {
                    final Direction randomDirection = Direction.ALL_CARDINALS.get(rng.nextInt(4));
                    commandQueue.add(ship.move(randomDirection));
                } else {
                    commandQueue.add(ship.stayStill());
                }
            }

            if (
                game.turnNumber <= 200 &&
                me.halite >= Constants.SHIP_COST &&
                !gameMap.at(me.shipyard).isOccupied())
            {
                commandQueue.add(me.shipyard.spawn());
            }

            game.endTurn(commandQueue);
        }
    }
}
