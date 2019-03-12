import hlt.Command;
import hlt.Player;

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
    public Player player;

    DP(Player player){
        this.player = player;
    }

    void update(Ship ship, Command command) {
        lastAction.put(ship, command);
    }
}
