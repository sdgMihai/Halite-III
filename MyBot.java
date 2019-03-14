
import hlt.*;

import java.util.*;
import java.util.stream.*;

public class MyBot {

    private static final Game game = new Game();
    private static final Random rnd = new Random();

    private static final List<Position> vips = new ArrayList<>();

    /* based on https://stackoverflow.com/a/11926952 */
    public static <E> E getWeightedRandom(List<Pair<E, Double>> weights, Random random) {
        return weights
                .stream()
                .map(e -> new Pair<>(e.getFirst(), -Math.log(random.nextDouble()) / e.getSecond()))
                .min((e0, e1) -> e0.getSecond().compareTo(e1.getSecond()))
                .orElseThrow(IllegalArgumentException::new).getFirst();
    }

    private static void scanVipPositions() {
        List<MapCell> cells = Arrays.asList(game.gameMap.cells).stream().flatMap((f) -> Arrays.asList(f).stream()).collect(Collectors.toList());

        Collections.sort(cells, (a, b) -> b.halite - a.halite);

        final int top = (int) Math.sqrt(game.gameMap.height * game.gameMap.width) / 16;

        vips.clear();
        vips.addAll(cells.stream().limit(cells.size() * top / 100).map((f) -> f.position).collect(Collectors.toList()));
    }

    private static boolean shouldBuildDrop(final Position p) {
        for (Dropoff df : game.me.dropoffs.values()) {
            if (game.gameMap.calculateDistance(p, df.position) < (game.gameMap.height * 3 / 4)) {
                return false;
            }
        }

        return true;
    }

    private static List<Pair<Position, Direction>> nearestTargets(final Ship ship, final List<Position> targets) {
        List<Pair<Position, Direction>> nearest = new ArrayList<>();

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

                    nearest.clear();
                }
                if (dist == minDist) {
                    nearest.add(new Pair<>(minLoc, minDir));
                }
            }
        }

        return nearest;
    }

    private static List<Pair<Position, Direction>> nearestDrops(final Ship ship) {
        List<Position> drops = new ArrayList<>();
        drops.add(game.me.shipyard.position);
        drops.addAll(game.me.dropoffs.values().stream().map((d) -> d.position).collect(Collectors.toList()));

        return nearestTargets(ship, drops);
    }

    private static List<Direction> findDirections(final Ship ship) {
        final List<Position> positions = new ArrayList<>();
        final Map<Position, Direction> directions = new HashMap<>();

        final List<Pair<Position, Direction>> closestDrops = nearestDrops(ship);

        int halTreshold = (100 - (game.turnNumber * 100 / Constants.MAX_TURNS));
        halTreshold = Math.min(halTreshold, 80);
        halTreshold = Math.max(halTreshold, 66);

        if (ship.halite >= halTreshold * Constants.MAX_HALITE / 100) {
            for (Pair<Position, Direction> drop : closestDrops) {
                final Position dropLoc = drop.getFirst();
                final Direction dropDir = drop.getSecond();

                positions.add(dropLoc);
                directions.put(dropLoc, dropDir);
            }

            Collections.shuffle(positions);

            final List<Position> randPositions = new ArrayList<>();
            for (Direction dir : Direction.ALL_CARDINALS) {
                if (closestDrops.stream().anyMatch((p) -> p.getSecond() == dir)) {
                    continue;
                }

                final Position p = ship.position.directionalOffset(dir);
                randPositions.add(p);
                directions.put(p, dir);
            }

            Collections.shuffle(randPositions, rnd);
            positions.addAll(randPositions);
        } else {
            final List<Pair<Position, Direction>> closestVips = nearestTargets(ship, vips);

            List<Pair<Position, Double>> weights = new ArrayList<>();

            Direction.ALL_CARDINALS.stream().forEachOrdered((dir) -> {
                final Position p = ship.position.directionalOffset(dir);
                directions.put(p, dir);
                final int distanceAmp = game.gameMap.calculateDistance(p, game.me.shipyard.position);
                final int vipAmp = closestVips.stream().anyMatch((v) -> v.getSecond() == dir) ? 4 : 1;
                final int hall = Integer.max(1, game.gameMap.at(p).halite);
                weights.add(new Pair<>(p, Double.max(1.0, 1.0 * hall * distanceAmp * vipAmp)));
            });

            while (weights.size() > 0) {
                final Position pos = getWeightedRandom(weights, rnd);
                positions.add(pos);
                weights.removeIf((p) -> Objects.equals(pos, p.getFirst()));
            }
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

        final int maxShips = (int) Math.sqrt(game.gameMap.height * game.gameMap.width) / 2;

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
