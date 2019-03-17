package hlt;

import hlt.*;

import java.util.*;
import java.util.stream.Collectors;

public class Victory {

    public static final Game game = new Game();
    public static final Random rnd = new Random();

    public static final List<Position> vips = new ArrayList<>();

    /**
     * Based on source: https://stackoverflow.com/a/11926952
     *
     * @param <E> data type of the returned element
     * @param weights list of elements and their weights
     * @param random random numbers generator
     * @return a random element from a list of elements of the same type and
     * their weights, where the probability is that of the weights
     */
    public static <E> E getWeightedRandom(List<Pair<E, Double>> weights, Random random) {
        return weights
                .stream()
                .map(e -> new Pair<>(e.getFirst(), -Math.log(random.nextDouble()) / e.getSecond()))
                .min((e0, e1) -> e0.getSecond().compareTo(e1.getSecond()))
                .orElseThrow(IllegalArgumentException::new).getFirst();
    }

    /**
     * Processes a customized top% of all the cells on the game map based on
     * their halite.
     */
    public static void scanVipPositions() {
        // processes all positions on the map into an ArrayList
        List<MapCell> cells = Arrays.asList(game.gameMap.cells).stream()
                .flatMap((f) -> Arrays.asList(f).stream())
                .collect(Collectors.toList());
        // sorts in descending order considering the halite
        Collections.sort(cells, (a, b) -> b.halite - a.halite);
        // the top percentage is determined considering the dimensions of the
        // game map
        final int top = (int) Math.sqrt(game.gameMap.height * game.gameMap.width) / 16;

        vips.clear();
        // vips will contain the top% of cells
        vips.addAll(cells.stream().limit(cells.size() * top / 100).map((f) -> f.position).collect(Collectors.toList()));
    }

    /**
     * Decides whether it is favorable strategically to avoid building a ship,
     * depending on the map size, amount of halite to build a drop off later,
     * number of ships so far throughout the game and a proximity range of the
     * vips.
     *
     * @param shipLimit the maximum limit of ships that can be built
     * @return true if we should avoid building a new ship or false if we
     * shouldn't
     */
    public static boolean shouldAvoidBuildingShips(final int shipLimit) {
        final int edgeSize = (int) Math.sqrt(game.gameMap.height * game.gameMap.width);
        // proximity range for vip positions, considering a map to be small for
        // dimension = 48
        final int vipProximityRange = edgeSize / 48;
        // if the range is small (the map is small), a new ship should be built
        if (vipProximityRange == 0) {
            return false;
        }
        // if the ship has enough halite for both building a ship and later
        // becoming a drop off, a new ship should be built
        if (game.me.halite > Constants.SHIP_COST + Constants.DROPOFF_COST) {
            return false;
        }
        // if the number of ships is below the limit / 3, a new ship should be
        // built
        if (game.me.ships.size() < shipLimit / 3) {
            return false;
        }
        // checks for every ship if there is a situation in which it is more
        // favorable to build a drop off instead of a new ship
        return game.me.ships.values().stream().anyMatch((ship) -> {
            // gets the vip position that is closest to the ship.
            // since all of the vips returned are at the same distance,
            // we can just use the first one in order to determine the said
            // distance
            Pair<Position, Direction> closestVip = nearestTargets(ship, vips).get(0);
            // checks if there is a vip position outside the proximity range
            // if it were within the proximity range, it would have been more
            // favorable to turn into a drop off instead of building a ship
            if (game.gameMap.calculateDistance(ship.position, closestVip.getFirst()) > vipProximityRange) {
                // this ship is too far away from a vip position, so we discard
                // it
                return false;
            }
            // if we can use the closest vip position to build a drop off, we
            // won't build a ship
            return canUseAsDropOff(closestVip.getFirst());
        });
    }

    /**
     * We choose a proximity range depending on the map size and check if the
     * drop offs are dispersed evenly, outside each other's proximity range.
     *
     * @param p position to check if it's suitable to be a drop off
     * @return true if it is, false otherwise
     */
    public static boolean canUseAsDropOff(final Position p) {
        final int edgeSize = (int) Math.sqrt(game.gameMap.height * game.gameMap.width);
        // proximity range for the drop offs, depending on the map size
        final int dropOffProximityRange = edgeSize / 4;
        // we check for every drop off on the map
        for (Dropoff df : game.me.dropoffs.values()) {
            // if the distance drom the current position to it is within the
            // proximity range
            if (game.gameMap.calculateDistance(p, df.position) < dropOffProximityRange) {
                // it is too close to the drop off
                return false;
            }
        }
        // there is no drop off in the proximity range, we can use this position
        // for a new drop off
        return true;
    }

    /**
     *
     * @param ship current ship
     * @return true if the ship should turn into a drop off, false otherwise
     */
    public static boolean shouldTurnIntoDropOff(final Ship ship) {
        final int edgeSize = (int) Math.sqrt(game.gameMap.height * game.gameMap.width);
        // maximum limit of drop offs
        final int maxDrops = edgeSize / 16;
        // won't turn into a drop off if the limit is exceeded
        if (game.me.dropoffs.size() >= maxDrops) {
            return false;
        }
        // checks if the position for the drop off is strategically ok (inside
        // the proximity range)
        if (!canUseAsDropOff(ship.position)) {
            return false;
        }
        // if the position corresponds to the shipyard's, the ship cannot turn
        // into a drop off
        if (Objects.equals(ship.position, game.me.shipyard.position)) {
            return false;
        }
        // checks if there is a vip position on which to turn the ship into a
        // drop off
        if (!vips.stream().anyMatch((v) -> Objects.equals(v, ship.position))) {
            return false;
        }
        // the minimum limit of ships to be around a drop off
        final int shipRange = edgeSize / 2;
        // counts how many ships are around the position suitable for a drop off
        final long nearbyShips = game.me.ships.values().stream()
                .filter((s)
                        -> game.gameMap.calculateDistance(s.position, ship.position) < shipRange)
                .count();
        // if there are more than the minimum limit, we turn the ship into a
        // drop off
        return nearbyShips > 2;
    }

    /**
     * Used for determining either the nearest drop offs or vip positions for a
     * ship.
     *
     * @param ship current ship
     * @param targets list of targets (they can be drop offs or vip positions)
     * @return list of positions and directions towards those positions
     */
    public static List<Pair<Position, Direction>> nearestTargets(final Ship ship, final List<Position> targets) {
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

    /**
     * Processes the nearest drop offs for a given ship using nearestTargets
     * function.
     *
     * @param ship the current ship for which we process the nearest drop offs
     * @return list of positions and directions towards them
     */
    public static List<Pair<Position, Direction>> nearestDropOffs(final Ship ship) {
        List<Position> drops = new ArrayList<>();
        // adds the shipyard as a drop off
        drops.add(game.me.shipyard.position);
        // adds all the drop offs positions to the list
        drops.addAll(game.me.dropoffs.values().stream().map((d) -> d.position).collect(Collectors.toList()));

        return nearestTargets(ship, drops);
    }

    /**
     *
     * @param ship the ship we are currently trying to find a strategic
     * direction to go to
     * @return the directions in which the ship could go, ranked by preference
     */
    public static List<Direction> findDirections(final Ship ship) {
        final List<Position> positions = new ArrayList<>();
        final Map<Position, Direction> directions = new HashMap<>();
        // nearestDropOffs returns a list with the positions and the specific
        // directions to the closest drop offs
        final List<Pair<Position, Direction>> closestDrops = nearestDropOffs(ship);

        // the halite threshold is slowly going down as the number of turns goes
        // on.
        // it can be seen as an inversed game progress percentage
        int haliteThreshold = (100 - (game.turnNumber * 100 / Constants.MAX_TURNS));
        // ensure that the threshold is at most 80%
        haliteThreshold = Math.min(haliteThreshold, 80);
        // and at least 66%
        haliteThreshold = Math.max(haliteThreshold, 66);
        // if the collected halite on a ship is over the haliteThreshold, then
        // we should return to the closest drop off
        if (ship.halite >= haliteThreshold * Constants.MAX_HALITE / 100) {
            for (Pair<Position, Direction> drop : closestDrops) {
                final Position dropLoc = drop.getFirst();
                final Direction dropDir = drop.getSecond();

                positions.add(dropLoc);
                directions.put(dropLoc, dropDir);
            }
            // ensures that the ships don't all use the same path by giving them
            // different directional guidances
            Collections.shuffle(positions);

            // collect the remaining directions as well, since it could be the
            // case that the ones which are better are not necessarily free to
            // use
            final List<Position> randPositions = new ArrayList<>();
            for (Direction dir : Direction.ALL_CARDINALS) {
                // avoid directions which have already been considered
                if (closestDrops.stream().anyMatch((p) -> p.getSecond() == dir)) {
                    continue;
                }

                final Position p = ship.position.directionalOffset(dir);
                randPositions.add(p);
                directions.put(p, dir);
            }

            // shuffle the above mentioned positions in order to avoid using the
            // same routes.
            // note that we are doing this separately in order to ensure that
            // the prefered routes are always the first ones to be considered
            // and use the alternatives only if they are not available
            Collections.shuffle(randPositions, rnd);
            positions.addAll(randPositions);
        } else {
            // in this case, we can continue searching for halite.
            // processes the closest vip positions
            final List<Pair<Position, Direction>> closestVips = nearestTargets(ship, vips);

            List<Pair<Position, Double>> weights = new ArrayList<>();

            // for each possible direction, calculate the position in which the
            // ship would be after going in it, and compute the weight of the
            // location
            for (Direction dir : Direction.ALL_CARDINALS) {
                final Position p = ship.position.directionalOffset(dir);
                directions.put(p, dir);
                // the weight of the position is amplified by the distance from
                // the shipyard, in order to encourage the ships to move away
                // from the shipyard itself
                final int distanceAmp = game.gameMap.calculateDistance(p, game.me.shipyard.position);
                // directions that go towards a vip location are more preferred
                // and as such have an amplifier of 4
                final int vipAmp = closestVips.stream().anyMatch((v) -> v.getSecond() == dir) ? 4 : 1;
                // finally, the base weight of a position is the quantity of
                // halite which can be found in it
                final int hall = Integer.max(1, game.gameMap.at(p).halite);
                // the weight is at least 1.0, and it is halite amount amplified
                // by the distance from the shipyard and the vip direction
                // amplifier
                final double weight = Double.max(1.0, 1.0 * hall * distanceAmp * vipAmp);
                weights.add(new Pair<>(p, weight));
            };
            // sample the positions based on their weights.
            // positions with higher weights are more likely to come before
            // positions with lower weights
            while (weights.size() > 0) {
                // get a random position
                final Position pos = getWeightedRandom(weights, rnd);
                // add it to the resulting list of positions
                positions.add(pos);
                // and then remove it from the set of weights such that it will
                // no longer be sampled in the future
                weights.removeIf((p) -> Objects.equals(pos, p.getFirst()));
            }
            // at this point all of the directions have been extracted based on
            // their weights, and positions.size() == 4
        }

        // since we are issuing moving commands as directions, not as positions
        // we must convert each position into the direction which the ship must
        // take to reach it. as such, we map the direction using the `direction`
        // map, which maps each location with the corresponding direction
        return positions.stream().map((p) -> directions.get(p)).distinct().collect(Collectors.toList());
    }

    /**
     * Decides whether the current ship is about to crash in the next move.
     *
     * @param ship the ship we are currently deciding the direction
     * @param direction the current ship's most strategic direction to go for
     * @return true if it there is a ship on the position we plan to move to,
     * false otherwise
     */
    public static boolean canCrash(final Ship ship, final Direction direction) {
        // the position according to the direction we want the ship to move to
        Position newLocation = ship.position.directionalOffset(direction);
        // checks all 4 positions
        for (Direction dir : Direction.ALL_CARDINALS) {
            final Position p = newLocation.directionalOffset(dir);
            // if there is an opponent ship on that position, they can crash
            if (game.gameMap.at(p).isOccupied() && !game.gameMap.at(p).ship.owner.equals(game.myId)) {
                return true;
            }
        }
        // checks if there is a ship on the position I want to move to
        return game.gameMap.at(newLocation).isOccupied();
    }

}
