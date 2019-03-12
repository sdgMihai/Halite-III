import hlt.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static hlt.Direction.STILL;

public class MyBot {

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

        // TODO conditie facut in dropoff
        if (game.gameMap.height > 64 && game.me.halite > 8000 && game.turnNumber < Constants.MAX_TURNS * 3 / 4 &&
                gameMap.calculateDistance(ship.position, closestDropoff) > 7 &&
                game.me.dropoffs.size() < 3) {
            gameMap.at(ship).markUnsafe(null);
            gameMap.at(ship).structure = ship;

            return Command.transformShipIntoDropoffSite(ship.id);
        }

        // TODO cand se intoarce nu merge pe cel mai scurt drum
        if (ship.goingtoDrop || ship.halite > (Constants.MAX_HALITE * 3 / 4)) {
            Direction dir = MyBotUtils.PositionToDirection(ship.position, gameMap.normalize(closestDropoff));
            ship.goingtoDrop = true;
            if (!gameMap.at(MyBotUtils.DirectionToPosition(ship.position, dir)).isOccupied()) {
                gameMap.at(MyBotUtils.DirectionToPosition(ship.position, dir)).markUnsafe(ship);
                gameMap.at(ship).markUnsafe(null);
                if (gameMap.at(MyBotUtils.DirectionToPosition(ship.position, dir)).hasStructure()) {
                    ship.goingtoDrop = false;
                }
                return ship.move(MyBotUtils.PositionToDirection(ship.position, gameMap.normalize(closestDropoff)));
            } else {
                return ship.move(STILL); // TODO poate trebuie sa faca loc
            }
        }

        // daca pozitia actuala nu mai are destule resurse, sa mearga in alta pozitie
        // altfel ramane pe pozitia actuala
        if (gameMap.at(ship).halite < 50) {
            Position pos = MyBotUtils.Greedy(ship, gameMap);

            // daca pozitia actuala are mai putine resurse decat cea mai buna alta
            // pozitie sa ramana aici (ca pe else)
            if (gameMap.at(ship).halite > gameMap.at(pos).halite) {
                return ship.move(STILL);
            } else {
                gameMap.at(pos).markUnsafe(ship);
                gameMap.at(ship).markUnsafe(null);
                return ship.move(MyBotUtils.PositionToDirection(ship.position, pos));
            }
        } else {
            return ship.move(STILL);
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

            if (game.turnNumber < Constants.MAX_TURNS / 2 &&
                    me.halite >= Constants.SHIP_COST * 2 && !gameMap.at(me.shipyard).isOccupied()) {
                commandQueue.add(me.shipyard.spawn());
            }
            fileWriter.close();
            game.endTurn(commandQueue);
        }

    }
}
