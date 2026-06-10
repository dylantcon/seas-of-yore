package seasofyore.match;

/**
 * The joining side of a LAN match: this screen dialed the host's
 * address, and commands the Franks.
 *
 * @author dylan
 */
public final class LANClientMatchHandler extends LANMatchHandler
{
  /**
   * Wraps the dialed connection.
   *
   * @param transport  the connected transport from MatchConnector.joinLan
   * @param localName  the local commander's name
   * @param remoteName the remote commander's name
   */
  public LANClientMatchHandler( MatchTransport transport,
                                String localName, String remoteName )
  {
    super( transport, false, localName, remoteName );
  }
}
