package seasofyore.match;

/**
 * Base for matches carried by the javalab relay: the transport is a
 * relay room rather than a direct socket, which is what lets a CheerpJ
 * build in a browser -- where no socket can be dialed or answered --
 * play anyone, anywhere. All behaviour lives in
 * {@link NetworkedMatchHandler}; this layer names the transport family.
 *
 * @author dylan
 */
public abstract class OnlineMatchHandler extends NetworkedMatchHandler
{
  /**
   * Passes the established, paired relay connection up to the engine.
   *
   * @param transport  the paired relay transport
   * @param host       whether this screen hosts the match
   * @param localName  the local commander's name
   * @param remoteName the remote commander's name
   */
  protected OnlineMatchHandler( MatchTransport transport, boolean host,
                                String localName, String remoteName )
  {
    super( transport, host, localName, remoteName );
  }
}
