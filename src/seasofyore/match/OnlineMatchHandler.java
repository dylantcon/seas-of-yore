package seasofyore.match;

/**
 * Base for matches played across the open internet. Unlike the LAN family,
 * online play cannot assume direct reachability: this layer is where a
 * relay or matchmaking service, session identity, and reconnection-after-
 * drop policies will live, shared by the hosting
 * ({@link OnlineServerMatchHandler}) and joining
 * ({@link OnlineClientMatchHandler}) sides.
 * <p>
 * Not yet implemented: this layer exists so the menus, terminal, pause
 * logic, and phase system can already route around the differences.
 *
 * @author dylan
 */
public abstract class OnlineMatchHandler extends NetworkedMatchHandler
{
}
