package seasofyore.match;

/**
 * The hosting side of a LAN match: this screen listened, the opponent
 * dialed in, and the host commands the Britons and fires first.
 *
 * @author dylan
 */
public final class LANServerMatchHandler extends LANMatchHandler
{
  /**
   * Wraps the accepted connection.
   *
   * @param transport  the connected transport from MatchConnector.hostLan
   * @param localName  the local commander's name
   * @param remoteName the remote commander's name
   */
  public LANServerMatchHandler( MatchTransport transport,
                                String localName, String remoteName )
  {
    super( transport, true, localName, remoteName );
  }
}
