package seasofyore.match;

import seasofyore.core.Player;

/**
 * The joining side of an online match: asks the relay or matchmaking
 * service for an open session (or a friend's session code), joins it, and
 * then relays turns and chat. Skeleton: every transport method states its
 * intent and throws until the online service exists.
 *
 * @author dylan
 */
public final class OnlineClientMatchHandler extends OnlineMatchHandler
{
  @Override
  public void connect()
  {
    throw new UnsupportedOperationException(
      "Online joining not yet implemented: will join a session by code or matchmaking" );
  }

  @Override
  public void disconnect()
  {
    throw new UnsupportedOperationException(
      "Online joining not yet implemented" );
  }

  @Override
  public void sendChat( String message )
  {
    throw new UnsupportedOperationException(
      "Online chat not yet implemented" );
  }

  @Override
  public Player getLocalPlayer()
  {
    throw new UnsupportedOperationException(
      "Online joining not yet implemented" );
  }

  @Override
  public boolean isLocalTurn()
  {
    throw new UnsupportedOperationException(
      "Online joining not yet implemented" );
  }
}
