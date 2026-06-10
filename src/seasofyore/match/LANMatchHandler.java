package seasofyore.match;

/**
 * Base for matches played across a local network: the transport is a
 * direct TCP socket -- one side listened on {@link #LAN_PORT}, the other
 * dialed an address -- with no account, relay, or third machine involved.
 * All behaviour lives in {@link NetworkedMatchHandler}; this layer names
 * the transport family and its well-known port.
 *
 * @author dylan
 */
public abstract class LANMatchHandler extends NetworkedMatchHandler
{
  /**
   * The well-known port the LAN protocol uses.
   */
  public static final int LAN_PORT = 51066; // the Hundred Years' War, abridged

  /**
   * Passes the established connection up to the engine.
   *
   * @param transport  the connected TCP transport
   * @param host       whether this screen hosts the match
   * @param localName  the local commander's name
   * @param remoteName the remote commander's name
   */
  protected LANMatchHandler( MatchTransport transport, boolean host,
                             String localName, String remoteName )
  {
    super( transport, host, localName, remoteName );
  }
}
