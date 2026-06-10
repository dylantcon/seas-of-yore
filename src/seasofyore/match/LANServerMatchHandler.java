package seasofyore.match;

import seasofyore.core.Player;

/**
 * The hosting side of a LAN match: listens on {@link #LAN_PORT}, accepts one
 * opponent, runs the opening handshake (who commands which civilization,
 * classic or SALVO), and then relays turns and chat for the length of the
 * match. Skeleton: every transport method states its intent and throws
 * until the wire protocol lands.
 *
 * @author dylan
 */
public final class LANServerMatchHandler extends LANMatchHandler
{
  @Override
  public void connect()
  {
    throw new UnsupportedOperationException(
      "LAN hosting not yet implemented: will listen on port " + LAN_PORT
      + " and handshake the first opponent that dials in" );
  }

  @Override
  public void disconnect()
  {
    throw new UnsupportedOperationException(
      "LAN hosting not yet implemented" );
  }

  @Override
  public void sendChat( String message )
  {
    throw new UnsupportedOperationException(
      "LAN chat not yet implemented" );
  }

  @Override
  public Player getLocalPlayer()
  {
    throw new UnsupportedOperationException(
      "LAN hosting not yet implemented: the host's player is settled in the handshake" );
  }

  @Override
  public boolean isLocalTurn()
  {
    throw new UnsupportedOperationException(
      "LAN hosting not yet implemented" );
  }
}
