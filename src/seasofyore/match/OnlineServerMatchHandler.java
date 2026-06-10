package seasofyore.match;

/**
 * The hosting side of a relay match: this screen opened the room whose
 * code the opponent typed in, and commands the Britons.
 *
 * @author dylan
 */
public final class OnlineServerMatchHandler extends OnlineMatchHandler
{
  /**
   * Wraps the paired relay connection.
   *
   * @param transport  the paired transport from MatchConnector.hostOnline
   * @param localName  the local commander's name
   * @param remoteName the remote commander's name
   */
  public OnlineServerMatchHandler( MatchTransport transport,
                                   String localName, String remoteName )
  {
    super( transport, true, localName, remoteName );
  }
}
