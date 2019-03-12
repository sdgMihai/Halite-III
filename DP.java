import hlt.Command;
import hlt.Player;
import hlt.Ship;

import java.util.HashMap;
import java.util.Map;

/**
 * DP
 * <p>
 * 12-Mar-19
 *
 * @author Gheoace Mihai
 */

public class DP {
    public Map<Ship, Command> lastAction = new HashMap<>();
    public Map<Ship, Integer> counts = new HashMap<>();  // counts still clk
    public Player player;

    DP(Player player){
        this.player = player;
    }

    void update(Ship ship, Command command) {
        if (command.command.charAt(command.command.length() - 1) == 'o') {
            return;
        } else if (command.command.charAt(0) == 'c') {  // ship -> dropoff
            lastAction.remove(ship);
            counts.remove(ship);
        } else {
            lastAction.put(ship, command);
            counts.put(ship, counts.getOrDefault(ship, 0) + 1);
        }
    }

    boolean isnTMoving(Ship ship) {
        if (counts.getOrDefault(ship, 0) >= 4) {
            return true;
        }
        return false;
    }


}
