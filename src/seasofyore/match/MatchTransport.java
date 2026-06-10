package seasofyore.match;

import java.io.IOException;

/**
 * One full-duplex line pipe to the opposing screen. The networked match
 * logic is written entirely against this seam, so the same handler drives
 * a direct LAN socket today and a relay-carried WebSocket (the
 * browser-compatible path) identically -- and a future transport costs no
 * change above this line.
 * <p>
 * Transports are handed to handlers already connected; establishing the
 * connection (listening, dialing, or pairing through the relay) is the
 * {@link MatchConnector}'s job, performed before the game is built.
 * Listener callbacks are delivered on the Swing event thread.
 *
 * @author dylan
 */
public interface MatchTransport
{
  /**
   * Receives transport events on the event dispatch thread.
   */
  interface Listener
  {
    /**
     * One protocol line arrived from the opposing screen.
     *
     * @param line the line, verbatim
     */
    void onLine( String line );

    /**
     * The pipe ended -- deliberately or otherwise. No further callbacks.
     *
     * @param reason a human-readable cause
     */
    void onClosed( String reason );
  }

  /**
   * Sets the event listener.
   *
   * @param listener the consumer of transport events
   */
  void setListener( Listener listener );

  /**
   * Sends one protocol line.
   *
   * @param line the line to send
   * @throws IOException if the pipe has failed
   */
  void sendLine( String line ) throws IOException;

  /**
   * Closes the pipe. Idempotent.
   */
  void close();
}
