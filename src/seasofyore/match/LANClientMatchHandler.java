package seasofyore.match;

import seasofyore.core.Player;

/**
 * The joining side of a LAN match: dials a host's address on
 * {@link #LAN_PORT}, completes the handshake, and then relays turns and
 * chat. Skeleton: every transport method states its intent and throws
 * until the wire protocol lands.
 *
 * @author dylan
 */
public final class LANClientMatchHandler extends LANMatchHandler
{
  @Override
  public void connect()
  {
    throw new UnsupportedOperationException(
      "LAN joining not yet implemented: will dial the host on port " + LAN_PORT );
  }

  @Override
  public void disconnect()
  {
    throw new UnsupportedOperationException(
      "LAN joining not yet implemented" );
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
      "LAN joining not yet implemented: the client's player is settled in the handshake" );
  }

  @Override
  public boolean isLocalTurn()
  {
    throw new UnsupportedOperationException(
      "LAN joining not yet implemented" );
  }
}
