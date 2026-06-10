package seasofyore.match;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import javax.swing.SwingUtilities;

/**
 * The LAN transport: newline-delimited UTF-8 lines over a plain TCP
 * socket. One side listened, the other dialed (see MatchConnector); by
 * the time this object exists the socket is connected and all that
 * remains is moving lines and surfacing the pipe's death on the EDT.
 *
 * @author dylan
 */
public final class TcpMatchTransport implements MatchTransport
{
  private final Socket socket;
  private final BufferedReader in;
  private final PrintWriter out;

  private Listener listener;
  private volatile boolean closed = false;

  /**
   * Wraps a connected socket and starts the reader thread.
   *
   * @param socket the connected socket
   * @throws IOException if the streams cannot be opened
   */
  public TcpMatchTransport( Socket socket ) throws IOException
  {
    this.socket = socket;
    socket.setTcpNoDelay( true ); // turn lines should not wait for Nagle

    this.in = new BufferedReader( new InputStreamReader(
        socket.getInputStream(), StandardCharsets.UTF_8 ) );
    this.out = new PrintWriter( new OutputStreamWriter(
        socket.getOutputStream(), StandardCharsets.UTF_8 ), true );

    Thread reader = new Thread( this::readLoop, "lan-match-reader" );
    reader.setDaemon( true );
    reader.start();
  }

  @Override
  public void setListener( Listener listener )
  {
    this.listener = listener;
  }

  /**
   * The reader thread: hands each line to the listener on the EDT until
   * the socket dies.
   */
  private void readLoop()
  {
    String reason = "the connection was closed";
    try
    {
      String line;
      while ( ( line = in.readLine() ) != null )
      {
        final String delivered = line;
        SwingUtilities.invokeLater( () ->
        {
          if ( listener != null )
            listener.onLine( delivered );
        });
      }
    }
    catch ( IOException ex )
    {
      reason = closed ? reason : "the connection was lost: " + ex.getMessage();
    }

    boolean announce = !closed;
    closed = true;
    closeQuietly();

    if ( announce )
    {
      final String finalReason = reason;
      SwingUtilities.invokeLater( () ->
      {
        if ( listener != null )
          listener.onClosed( finalReason );
      });
    }
  }

  @Override
  public void sendLine( String line ) throws IOException
  {
    if ( closed )
      throw new IOException( "transport is closed" );
    out.println( line );
    if ( out.checkError() )
      throw new IOException( "the connection has failed" );
  }

  @Override
  public void close()
  {
    closed = true;
    closeQuietly();
  }

  private void closeQuietly()
  {
    try
    {
      socket.close();
    }
    catch ( IOException ex )
    {
      // best effort
    }
  }
}
