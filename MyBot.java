
import hlt.*;

import java.util.*;
import java.util.stream.*;

public class MyBot {

    private static final Game game = new Game();
    private static final Random rnd = new Random();

    private static final List<Position> vips = new ArrayList<>();

    private static void scanVipPositions() {
        List<MapCell> cells = Arrays.asList(game.gameMap.cells).stream().flatMap((f) -> Arrays.asList(f).stream()).collect(Collectors.toList());

        Collections.sort(cells, (a, b) -> b.halite - a.halite);

        vips.clear();
        vips.addAll(cells.stream().limit(cells.size() / 100).map((f) -> f.position).collect(Collectors.toList()));
    }

    private static boolean shouldBuildDrop(final Position p) {
        for (Dropoff df : game.me.dropoffs.values()) {
            if (game.gameMap.calculateDistance(p, df.position) < (game.gameMap.height * 3 / 4)) {
                return false;
            }
        }

        return true;
    }

    private static Pair<Position, Direction> nearestTarget(final Ship ship, final List<Position> targets) {
        Direction minDir = null;
        Position minLoc = null;
        int minDist = Integer.MAX_VALUE;
        for (Position p : targets) {
            for (Direction dir : Direction.ALL_CARDINALS) {
                final Position loc = ship.position.directionalOffset(dir);
                final int dist = game.gameMap.calculateDistance(loc, p);
                if (dist < minDist) {
                    minDist = dist;
                    minLoc = loc;
                    minDir = dir;
                }
            }
        }

        return new Pair<>(minLoc, minDir);
    }

    private static Pair<Position, Direction> nearestDrop(final Ship ship) {
        List<Position> drops = new ArrayList<>();
        drops.add(game.me.shipyard.position);
        drops.addAll(game.me.dropoffs.values().stream().map((d) -> d.position).collect(Collectors.toList()));

        return nearestTarget(ship, drops);
    }

    private static List<Direction> findDirections(final Ship ship) {
        final List<Position> positions = new ArrayList<>();
        final Map<Position, Direction> directions = new HashMap<>();

        final Pair<Position, Direction> closestDrop = nearestDrop(ship);
        final Position dropLoc = closestDrop.getFirst();
        final Direction dropDir = closestDrop.getSecond();

        int halTreshold = (100 - (game.turnNumber * 100 / Constants.MAX_TURNS));
        halTreshold = Math.min(halTreshold, 80);
        halTreshold = Math.max(halTreshold, 66);

        if (ship.halite >= halTreshold * Constants.MAX_HALITE / 100) {
            positions.add(dropLoc);
            directions.put(dropLoc, dropDir);

            final List<Position> randPositions = new ArrayList<>();
            for (Direction dir : Direction.ALL_CARDINALS) {
                if (dir.equals(dropDir)) {
                    continue;
                }

                final Position p = ship.position.directionalOffset(dir);
                randPositions.add(p);
                directions.put(p, dir);
            }
            
            Collections.shuffle(randPositions, rnd);
            positions.addAll(randPositions);
        } else {
            final Pair<Position, Direction> closestVip = nearestTarget(ship, vips);
            final Direction vipDir = closestVip.getSecond();

            for (Direction dir : Direction.ALL_CARDINALS) {
                final Position p = ship.position.directionalOffset(dir);
                directions.put(p, dir);
                final int distanceAmp = game.gameMap.calculateDistance(p, dropLoc);
                final int vipAmp = Objects.equals(dir, vipDir) ? 4 : 1;
                final int hall = Integer.max(1, game.gameMap.at(p).halite);
                final int weight = Integer.max(1, hall * distanceAmp * vipAmp);
                for (int i = 0; i < weight; ++i) {
                    positions.add(p);
                }
            }

            Collections.shuffle(positions, rnd);
        }

        return positions.stream().map(directions::get).distinct().collect(Collectors.toList());
    }

    private static boolean canCrash(final Ship ship, final Direction direction) {
        Position newLocation = ship.position.directionalOffset(direction);

        for (Direction dir : Direction.ALL_CARDINALS) {
            final Position p = newLocation.directionalOffset(dir);

            if (game.gameMap.at(p).isOccupied() && !game.gameMap.at(p).ship.owner.equals(game.myId)) {
                return true;
            }
        }

        return game.gameMap.at(newLocation).isOccupied();
    }

    public static void main(final String[] args) {
        final long rngSeed;
        if (args.length > 1) {
            rngSeed = Integer.parseInt(args[1]);
        } else {
            rngSeed = 42;
        }
        rnd.setSeed(rngSeed);

        // At this point "game" variable is populated with initial map data.
        // This is a good place to do computationally expensive start-up pre-processing.
        // As soon as you call "ready" function below, the 2 second per turn timer will start.
        game.ready("QuantumGreedy");

        final int maxShips = Constants.MAX_TURNS * 2 / game.gameMap.height;

        Log.log("Successfully created bot! My Player ID is " + game.myId
                + ". Bot rng seed is " + rngSeed + ".");

        for (;;) {
            game.updateFrame();
            final Player me = game.me;
            final GameMap gameMap = game.gameMap;

            final ArrayList<Command> commandQueue = new ArrayList<>();

            scanVipPositions();

            boolean willSpawn = false;
            if (me.ships.size() < maxShips
                    && me.halite >= Constants.SHIP_COST
                    && !gameMap.at(me.shipyard).isOccupied()) {
                me.halite -= Constants.SHIP_COST;
                commandQueue.add(me.shipyard.spawn());
                willSpawn = true;
            }

            // TODO schimbat cu logica noua
            for (final Ship ship : me.ships.values()) {
                if (shouldBuildDrop(ship.position)
                        && me.halite > Constants.DROPOFF_COST
                        && vips.contains(ship.position)) {
                    me.halite -= Constants.DROPOFF_COST;
                    commandQueue.add(ship.makeDropoff());
                    continue;
                }

                Direction goodDir = null;
                for (Direction dir : findDirections(ship)) {
                    if (!canCrash(ship, dir)) {
                        goodDir = dir;
                        break;
                    }
                }

                if (goodDir == null) {
                    commandQueue.add(ship.stayStill());

                    continue;
                }

                final Position pos = ship.position.directionalOffset(goodDir);
                if (gameMap.at(ship.position).halite > 64 && !ship.isFull()
                        || me.shipyard.position.equals(pos) && willSpawn
                        || ship.halite - gameMap.at(ship).halite / 10 < 0) {
                    commandQueue.add(ship.stayStill());
                } else {
                    commandQueue.add(ship.move(gameMap.naiveNavigate(ship, pos)));
                }
            }

            game.endTurn(commandQueue);
        }
    }
}
