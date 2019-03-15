
import hlt.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.*;

public class MyBot {

    private static final Game game = new Game();
    private static final Random rnd = new Random();
    private static final List<Pair<String, ArrayList<Long>>> unaveragedTime = new ArrayList<>();
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
        long start = System.currentTimeMillis();
        List<MapCell> cells = Arrays.asList(game.gameMap.cells).stream()
                .flatMap((f) -> Arrays.asList(f).stream())
                .collect(Collectors.toList());

        Collections.sort(cells, (a, b) -> b.halite - a.halite);

        final int top = (int) Math.sqrt(game.gameMap.height * game.gameMap.width) / 16;

        vips.clear();
        vips.addAll(cells.stream().limit(cells.size() * top / 100).map((f) -> f.position).collect(Collectors.toList()));
        unaveragedTime.get(1).getSecond().add(System.currentTimeMillis() - start);
    }

    private static boolean shouldAvoidBuildingShips(final int shipLimit) {
        long start = System.currentTimeMillis();
        final int edgeSize = (int) Math.sqrt(game.gameMap.height * game.gameMap.width);
        final int vipProximityRange = edgeSize / 48;

        if (vipProximityRange <= 0) {
            return false;
        }

        if (game.me.halite > Constants.SHIP_COST + Constants.DROPOFF_COST) {
            return false;
        }

        if (game.me.ships.size() < shipLimit / 3) {
            return false;
        }

        boolean ceva =  game.me.ships.values().stream().anyMatch((ship) -> {
            Pair<Position, Direction> closestVip = nearestTargets(ship, vips).get(0);

            if (game.gameMap.calculateDistance(ship.position, closestVip.getFirst()) > vipProximityRange) {
                return false;
            }

            return canUseAsDropOff(closestVip.getFirst());
        });
        unaveragedTime.get(2).getSecond().add(System.currentTimeMillis() - start);
        return ceva;
    }

    private static boolean canUseAsDropOff(final Position p) {
        long start = System.currentTimeMillis();
        final int edgeSize = (int) Math.sqrt(game.gameMap.height * game.gameMap.width);

        final int dropOffProximityRange = edgeSize / 4;
        for (Dropoff df : game.me.dropoffs.values()) {
            if (game.gameMap.calculateDistance(p, df.position) < dropOffProximityRange) {
                unaveragedTime.get(3).getSecond().add(System.currentTimeMillis() - start);
                return false;
            }
        }

        unaveragedTime.get(3).getSecond().add(System.currentTimeMillis() - start);
        return true;
    }

    private static boolean shouldTurnIntoDropOff(final Ship ship) {
        long start = System.currentTimeMillis();
        final int edgeSize = (int) Math.sqrt(game.gameMap.height * game.gameMap.width);
        final int maxDrops = edgeSize / 16;

        if (game.me.dropoffs.size() >= maxDrops) {
            return false;
        }

        if (!canUseAsDropOff(ship.position)) {
            return false;
        }

        if (Objects.equals(ship.position, game.me.shipyard.position)) {
            return false;
        }

        if (!vips.stream().anyMatch((v) -> Objects.equals(v, ship.position))) {
            return false;
        }

        final int shipRange = edgeSize / 2;
        final long nearbyShips = game.me.ships.values().stream()
                .filter((s)
                        -> game.gameMap.calculateDistance(s.position, ship.position) < shipRange)
                .count();
        unaveragedTime.get(4).getSecond().add(System.currentTimeMillis() - start);
        return nearbyShips > 2;
    }

    private static List<Pair<Position, Direction>> nearestTargets(final Ship ship, final List<Position> targets) {
        long start = System.currentTimeMillis();
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
        unaveragedTime.get(5).getSecond().add(System.currentTimeMillis() - start);
        return nearest;
    }

    private static List<Pair<Position, Direction>> nearestDrops(final Ship ship) {
        long start = System.currentTimeMillis();
        List<Position> drops = new ArrayList<>();
        drops.add(game.me.shipyard.position);
        drops.addAll(game.me.dropoffs.values().stream().map((d) -> d.position).collect(Collectors.toList()));

        List<Pair<Position, Direction>> ceva = nearestTargets(ship, drops);
        unaveragedTime.get(6).getSecond().add(System.currentTimeMillis() - start);
        return ceva;
    }

    private static List<Direction> findDirections(final Ship ship) {
        long start = System.currentTimeMillis();
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

        List<Direction> ceva = positions.stream().map(directions::get).distinct().collect(Collectors.toList());
        unaveragedTime.get(7).getSecond().add(System.currentTimeMillis() - start);
        return ceva;
    }

    private static boolean canCrash(final Ship ship, final Direction direction) {
        long start = System.currentTimeMillis();
        Position newLocation = ship.position.directionalOffset(direction);

        for (Direction dir : Direction.ALL_CARDINALS) {
            final Position p = newLocation.directionalOffset(dir);

            if (game.gameMap.at(p).isOccupied() && !game.gameMap.at(p).ship.owner.equals(game.myId)) {

                return true;
            }
        }

        unaveragedTime.get(8).getSecond().add(System.currentTimeMillis() - start);
        return game.gameMap.at(newLocation).isOccupied();
    }

    public static void main(final String[] args) throws IOException {
        final long rngSeed;
        if (args.length > 1) {
            rngSeed = Integer.parseInt(args[1]);
        } else {
            rngSeed = 42;
        }
        rnd.setSeed(rngSeed);

        // one array for each method - 8
        unaveragedTime.add(new Pair<>("Start", new ArrayList<>()));
        unaveragedTime.add(new Pair<>("scanVipPositions", new ArrayList<>()));
        unaveragedTime.add(new Pair<>("shouldAvoidBuildingShips", new ArrayList<>()));
        unaveragedTime.add(new Pair<>("canUseAsDropOff", new ArrayList<>()));
        unaveragedTime.add(new Pair<>("shouldTurnIntoDropOff", new ArrayList<>()));
        unaveragedTime.add(new Pair<>("nearestTargets", new ArrayList<>()));
        unaveragedTime.add(new Pair<>("nearestDrops", new ArrayList<>()));
        unaveragedTime.add(new Pair<>("findDirections", new ArrayList<>()));
        unaveragedTime.add(new Pair<>("canCrash", new ArrayList<>()));

        game.ready("QuantumGreedy");

        final int maxShips = (int) Math.sqrt(game.gameMap.height * game.gameMap.width) / 2;

        Log.log("Successfully created bot! My Player ID is " + game.myId
                + ". Bot rng seed is " + rngSeed + ".");

        for (;;) {
            game.updateFrame();
            FileWriter fileWriter;
            try {
                fileWriter = new FileWriter("log.out", true);
            } catch (Exception e) {
                return;
            }

            for (Pair<String, ArrayList<Long>> pair : unaveragedTime) {
                while (!pair.getSecond().isEmpty()) {
                    pair.getSecond().remove(0);
                }
            }

            final Player me = game.me;
            final GameMap gameMap = game.gameMap;
            final ArrayList<Command> commandQueue = new ArrayList<>();
            scanVipPositions();

            boolean willSpawn = false;
            if (me.ships.size() < maxShips
                    && me.halite >= Constants.SHIP_COST
                    && !gameMap.at(me.shipyard).isOccupied()
                    && !shouldAvoidBuildingShips(maxShips)) {
                me.halite -= Constants.SHIP_COST;
                commandQueue.add(me.shipyard.spawn());
                willSpawn = true;
            }

            for (final Ship ship : me.ships.values()) {

                // Bucata 1
                if (shouldTurnIntoDropOff(ship)
                        && me.halite > Constants.DROPOFF_COST) {
                    me.halite -= Constants.DROPOFF_COST;
                    commandQueue.add(ship.makeDropoff());
                    continue;
                }

                Direction goodDir = null;
                // Bucata 2
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
            ArrayList<Pair <String, Double>> averageTime = new ArrayList<>();
            ArrayList<Pair <String, Double>> averagePercentageTime = new ArrayList<>();
            double allTime = 0.0;
            for (Pair<String, ArrayList<Long>> pair : unaveragedTime) {
                Long sum = 0L;
                for (Long elem : pair.getSecond()) {
                    sum += elem;
                }
                if (pair.getSecond().size() == 0) {
                    averageTime.add(new Pair<>(pair.getFirst(), 0.0));
                    continue;
                }
                allTime += sum.doubleValue() / (double) pair.getSecond().size();
                averageTime.add(new Pair<>(pair.getFirst(), sum.doubleValue() / (double) pair.getSecond().size()));
            }

            for (Pair<String, Double> pair : averageTime) {
                averagePercentageTime.add(new Pair<>
                        (pair.getFirst(), (pair.getSecond() * 100) / allTime));
            }

            fileWriter.write("Methods percentage time for round: " + game.turnNumber + "\n");
            for (Pair<String, Double> pair : averageTime) {
                fileWriter.write(pair.getFirst() + ": " + pair.getSecond() + " ms\n");
            }
            fileWriter.write("\n");
            fileWriter.close();
            game.endTurn(commandQueue);
        }
    }
}
