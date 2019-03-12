import hlt.Direction;
import hlt.GameMap;
import hlt.Position;
import hlt.Ship;

import java.util.Arrays;
import java.util.Comparator;

import static hlt.Direction.NORTH;

public final class MyBotUtils {
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
        Arrays.sort(positions,
                (Position o1, Position o2) -> gameMap.at(o1).halite - gameMap.at(o2).halite);
        for (int i = 0; i < positions.length; ++i) {
            if (!gameMap.at(positions[i]).isOccupied()) {
                return positions[i];
            }
        }

        return initial;
    }

    /**
     * Primeste o sursa si o destinatie si intoarce directia in care ar trebui sa mearga ca sa
     * ajunga destinatie pentru harta jocului (y, x)
     * @param source sursa
     * @param destination destinatie
     * @return directia
     */
    public static Direction PositionToDirection (final Position source,
                                                 final Position destination) {
        if (source.x == destination.x && source.y == destination.y) {
            return Direction.STILL;
        }

        if (source.x == destination.x) {
            if (source.y > destination.y) {
                return NORTH;
            } else {
                return Direction.SOUTH;
            }
        }

        if (source.x < destination.x && source.y == destination.y) {
            return Direction.EAST;
        } else {
            return Direction.WEST;
        }
    }

    /**
     * Intoarce pozitia pe harta (x, y) unde o sa fie destinatia
     * @param source locul de und se pleaca
     * @param direction directia in care se pleaca
     * @return pozitia pe care o sa fie
     */
    public static Position DirectionToPosition (final Position source,
                                                final Direction direction) {
        switch(direction) {
            case NORTH:
                return new Position(source.x, source.y - 1);
            case SOUTH:
                return new Position(source.x, source.y + 1);
            case EAST:
                return new Position(source.x + 1, source.y);
            case WEST:
                return new Position(source.x - 1, source.y);
            case STILL:
                return source;
            default:
                return null;
        }
    }
}
