package seasofyore.match;

/**
 * The joining side of a relay match: this screen joined a room by its
 * code, and commands the Franks.
 *
 * @author dylan
 */
public final class OnlineClientMatchHandler extends OnlineMatchHandler
{
  /**
   * Wraps the paired relay connection.
   *
   * @param transport  the paired transport from MatchConnector.joinOnline
   * @param localName  the local commander's name
   * @param remoteName the remote commander's name
   */
  public OnlineClientMatchHandler( MatchTransport transport,
                                   String localName, String remoteName )
  {
    super( transport, false, localName, remoteName );
  }
}
