package seasofyore.match;

/**
 * Base for matches played across a local network. The LAN family will speak
 * a plain socket protocol on a well-known port -- one side hosting
 * ({@link LANServerMatchHandler}), the other dialing a host address
 * ({@link LANClientMatchHandler}) -- with no account, lobby, or relay in
 * between. What distinguishes LAN from online is discovery and trust: peers
 * find each other by address (or broadcast) and connect directly.
 * <p>
 * Not yet implemented: this layer exists so the menus, terminal, pause
 * logic, and phase system can already route around the differences.
 *
 * @author dylan
 */
public abstract class LANMatchHandler extends NetworkedMatchHandler
{
  /**
   * The well-known port the LAN protocol will use.
   */
  protected static final int LAN_PORT = 51066; // the Hundred Years' War, abridged
}
