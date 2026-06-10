package seasofyore.match;

import javalabrelay.RelayClient;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The relay transport: match lines carried through a javalab relay room
 * instead of a direct socket. This is the browser-compatible path -- a
 * CheerpJ page cannot dial or listen, but it can hold a WebSocket to the
 * relay, and the relay forwards lines between the paired players
 * verbatim.
 * <p>
 * The object doubles as the {@link RelayClient.PairListener}: the
 * connector hands it to openRoom/joinRoom and then blocks on
 * {@link #awaitPaired} until the opponent arrives (or an error or
 * timeout ends the wait).
 *
 * @author dylan
 */
public final class RelayMatchTransport
    implements MatchTransport, RelayClient.PairListener
{
  private final RelayClient relay;

  private final CountDownLatch pairedLatch = new CountDownLatch( 1 );
  private volatile boolean paired = false;
  private volatile String pairError = null;

  private Listener listener;
  private volatile boolean closed = false;

  /**
   * Wraps a connected relay client (HELLO/WELCOME already done).
   *
   * @param relay the relay session lines will travel through
   */
  public RelayMatchTransport( RelayClient relay )
  {
    this.relay = relay;
  }

  /**
   * Blocks until the room fills, fails, or times out. Call off the EDT
   * (the connector's background thread), since pairing events arrive on
   * the EDT.
   *
   * @param timeoutMs how long to wait
   * @return true once paired; false on error or timeout
   */
  public boolean awaitPaired( long timeoutMs )
  {
    try
    {
      pairedLatch.await( timeoutMs, TimeUnit.MILLISECONDS );
    }
    catch ( InterruptedException ex )
    {
      Thread.currentThread().interrupt();
    }
    return paired;
  }

  /**
   * Why pairing failed, if it did.
   *
   * @return the error text, or null
   */
  public String getPairError()
  {
    return pairError;
  }

  // ------------------------------------------------------------------
  // MatchTransport
  // ------------------------------------------------------------------

  @Override
  public void setListener( Listener listener )
  {
    this.listener = listener;
  }

  @Override
  public void sendLine( String line ) throws IOException
  {
    if ( closed )
      throw new IOException( "transport is closed" );
    relay.sendToPeer( line );
  }

  @Override
  public void close()
  {
    closed = true;
    relay.close();
  }

  // ------------------------------------------------------------------
  // RelayClient.PairListener (events arrive on the app's dispatcher)
  // ------------------------------------------------------------------

  @Override
  public void onWaiting( String room )
  {
    // the room code came from us; nothing to record
  }

  @Override
  public void onPaired()
  {
    paired = true;
    pairedLatch.countDown();
  }

  @Override
  public void onPeerLine( String line )
  {
    if ( listener != null )
      listener.onLine( line );
  }

  @Override
  public void onPeerLeft()
  {
    if ( closed )
      return;
    closed = true;

    if ( !paired )
    {
      // the relay died before anyone joined: unblock the connector
      pairError = "the relay connection ended";
      pairedLatch.countDown();
      return;
    }

    if ( listener != null )
      listener.onClosed( "the enemy departed" );
  }

  @Override
  public void onError( String code, String detail )
  {
    pairError = code + ": " + detail;
    pairedLatch.countDown();
  }
}
