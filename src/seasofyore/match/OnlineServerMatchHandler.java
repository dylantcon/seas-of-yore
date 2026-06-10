package seasofyore.match;

import seasofyore.core.Player;

/**
 * The hosting side of an online match: registers a session with the relay
 * or matchmaking service and waits for an opponent to join it, then relays
 * turns and chat. Skeleton: every transport method states its intent and
 * throws until the online service exists.
 *
 * @author dylan
 */
public final class OnlineServerMatchHandler extends OnlineMatchHandler
{
  @Override
  public void connect()
  {
    throw new UnsupportedOperationException(
      "Online hosting not yet implemented: will register a session and await a joiner" );
  }

  @Override
  public void disconnect()
  {
    throw new UnsupportedOperationException(
      "Online hosting not yet implemented" );
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
      "Online hosting not yet implemented" );
  }

  @Override
  public boolean isLocalTurn()
  {
    throw new UnsupportedOperationException(
      "Online hosting not yet implemented" );
  }
}
