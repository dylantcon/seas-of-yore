package seasofyore.match;

import javalabrelay.RelayClient;
import javalabrelay.RelayTransports;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

/**
 * Establishes networked matches before any game exists: it produces a
 * connected, handshaken {@link MatchTransport} plus everything the game
 * needs to construct itself (who hosts, both names, the rules). Every
 * method blocks and must run on a background thread; the multiplayer
 * screen owns that thread and a Cancel button.
 *
 * <h2>The pre-game handshake</h2>
 * <pre>
 *   host  → GHELLO 1 &lt;name&gt;
 *   host  → GRULES CLASSIC|SALVO
 *   joiner→ GHELLO 1 &lt;name&gt;
 * </pre>
 * The host decides the rules; the joiner learns them here, which is why
 * the handshake precedes (and parameterizes) the game's construction.
 *
 * @author dylan
 */
public final class MatchConnector
{
  /**
   * How long any single handshake step may take.
   */
  private static final int HANDSHAKE_TIMEOUT_MS = 20_000;

  /**
   * How long relay pairing may wait for an opponent before giving up.
   */
  private static final int PAIRING_TIMEOUT_MS = 5 * 60_000;

  /**
   * Everything the multiplayer screen needs to build the game.
   */
  public static final class Connection
  {
    /** The connected, handshaken pipe. */
    public final MatchTransport transport;
    /** Whether this screen hosts (Britons, first shot). */
    public final boolean host;
    /** The remote commander's name. */
    public final String remoteName;
    /** Whether the match uses SALVO rules (decided by the host). */
    public final boolean salvo;

    Connection( MatchTransport transport, boolean host,
                String remoteName, boolean salvo )
    {
      this.transport = transport;
      this.host = host;
      this.remoteName = remoteName;
      this.salvo = salvo;
    }
  }

  /**
   * Not instantiable; a toolbox of blocking connectors.
   */
  private MatchConnector() {}

  /**
   * Hosts a LAN match: listens until an opponent dials in, then
   * handshakes as host.
   *
   * @param serverSocket the listening socket (owned by the caller so its
   *                     Cancel button can close it to abort the wait)
   * @param name         the local commander's name
   * @param salvo        the rules this host has chosen
   * @return the established connection
   * @throws IOException if listening or the handshake fails
   */
  public static Connection hostLan( ServerSocket serverSocket, String name,
                                    boolean salvo ) throws IOException
  {
    Socket socket = serverSocket.accept();
    MatchTransport transport = new TcpMatchTransport( socket );
    String remoteName = handshake( transport, true, name, salvo ).remoteName;
    return new Connection( transport, true, remoteName, salvo );
  }

  /**
   * Joins a LAN match by the host's address, optionally with an explicit
   * port ("192.168.1.20" or "192.168.1.20:51066").
   *
   * @param hostAddress the host's hostname or IP, with optional :port
   * @param name        the local commander's name
   * @return the established connection (rules learned from the host)
   * @throws IOException if dialing or the handshake fails
   */
  public static Connection joinLan( String hostAddress, String name )
      throws IOException
  {
    String host = hostAddress;
    int port = LANMatchHandler.LAN_PORT;

    int colon = hostAddress.lastIndexOf( ':' );
    if ( colon > 0 && hostAddress.indexOf( ':' ) == colon ) // not IPv6
    {
      host = hostAddress.substring( 0, colon );
      port = Integer.parseInt( hostAddress.substring( colon + 1 ) );
    }

    Socket socket = new Socket( host, port );
    MatchTransport transport = new TcpMatchTransport( socket );
    HandshakeResult result = handshake( transport, false, name, false );
    return new Connection( transport, false, result.remoteName, result.salvo );
  }

  /**
   * Hosts a relay match: opens the room and waits for someone holding
   * the code, then handshakes as host.
   *
   * @param roomCode the code to open (share it with the opponent)
   * @param name     the local commander's name
   * @param salvo    the rules this host has chosen
   * @return the established connection
   * @throws IOException if the relay, pairing, or handshake fails
   */
  public static Connection hostOnline( String roomCode, String name,
                                       boolean salvo, CancelHook cancel )
      throws IOException
  {
    RelayMatchTransport transport = openRelayRoom( roomCode, true, cancel );
    String remoteName = handshake( transport, true, name, salvo ).remoteName;
    return new Connection( transport, true, remoteName, salvo );
  }

  /**
   * A handle the UI can use to abort a blocking wait (closing the
   * listening socket or relay connection from the Cancel button).
   */
  public interface CancelHook
  {
    /**
     * Registers the action that aborts the wait.
     *
     * @param canceller the abort action
     */
    void register( Runnable canceller );
  }

  /**
   * Joins a relay match by its room code.
   *
   * @param roomCode the code the host shared
   * @param name     the local commander's name
   * @return the established connection (rules learned from the host)
   * @throws IOException if the relay, pairing, or handshake fails
   */
  public static Connection joinOnline( String roomCode, String name )
      throws IOException
  {
    RelayMatchTransport transport = openRelayRoom( roomCode, false, null );
    HandshakeResult result = handshake( transport, false, name, false );
    return new Connection( transport, false, result.remoteName, result.salvo );
  }

  /**
   * Connects to the relay and resolves a room to a paired transport.
   */
  private static RelayMatchTransport openRelayRoom( String roomCode,
                                                    boolean open,
                                                    CancelHook cancel )
      throws IOException
  {
    RelayClient relay = new RelayClient(
        RelayTransports.create( RelayTransports.relayUri(),
                                SwingUtilities::invokeLater ),
        "seas-of-yore" );
    relay.connect();

    if ( cancel != null )
      cancel.register( relay::close );

    RelayMatchTransport transport = new RelayMatchTransport( relay );
    if ( open )
      relay.openRoom( roomCode, transport );
    else
      relay.joinRoom( roomCode, transport );

    if ( !transport.awaitPaired( PAIRING_TIMEOUT_MS ) )
    {
      relay.close();
      String why = ( transport.getPairError() != null )
                 ? transport.getPairError() : "no opponent arrived";
      throw new IOException( why );
    }
    return transport;
  }

  /**
   * What the handshake learned about the other side.
   */
  private static final class HandshakeResult
  {
    final String remoteName;
    final boolean salvo;

    HandshakeResult( String remoteName, boolean salvo )
    {
      this.remoteName = remoteName;
      this.salvo = salvo;
    }
  }

  /**
   * Runs the pre-game handshake over an established transport, using a
   * temporary listener (the match handler takes over afterwards).
   */
  private static HandshakeResult handshake( MatchTransport transport,
                                            boolean host, String name,
                                            boolean salvo ) throws IOException
  {
    final BlockingQueue<String> lines = new ArrayBlockingQueue<>( 8 );
    transport.setListener( new MatchTransport.Listener()
    {
      @Override
      public void onLine( String line )
      {
        lines.add( line );
      }

      @Override
      public void onClosed( String reason )
      {
        lines.add( "GCLOSED " + reason );
      }
    });

    if ( host )
    {
      transport.sendLine( "GHELLO 1 " + name );
      transport.sendLine( "GRULES " + ( salvo ? "SALVO" : "CLASSIC" ) );

      String hello = await( lines, "GHELLO" );
      return new HandshakeResult( nameFromHello( hello ), salvo );
    }

    String hello = await( lines, "GHELLO" );
    String rules = await( lines, "GRULES" );
    transport.sendLine( "GHELLO 1 " + name );

    boolean hostSalvo = rules.endsWith( "SALVO" );
    return new HandshakeResult( nameFromHello( hello ), hostSalvo );
  }

  /**
   * Waits for the next line bearing the expected keyword.
   */
  private static String await( BlockingQueue<String> lines, String keyword )
      throws IOException
  {
    try
    {
      long deadline = System.currentTimeMillis() + HANDSHAKE_TIMEOUT_MS;
      while ( true )
      {
        long remaining = deadline - System.currentTimeMillis();
        String line = lines.poll( Math.max( 1, remaining ),
                                  TimeUnit.MILLISECONDS );
        if ( line == null )
          throw new IOException( "the enemy never completed the handshake" );
        if ( line.startsWith( "GCLOSED" ) )
          throw new IOException( "connection lost during the handshake" );
        if ( line.startsWith( keyword ) )
          return line;
        // unexpected early traffic: ignore and keep waiting
      }
    }
    catch ( InterruptedException ex )
    {
      Thread.currentThread().interrupt();
      throw new IOException( "interrupted during the handshake" );
    }
  }

  /**
   * Extracts the commander's name from a GHELLO line
   * ("GHELLO &lt;version&gt; &lt;name...&gt;").
   */
  private static String nameFromHello( String hello )
  {
    String[] parts = hello.split( " ", 3 );
    return ( parts.length >= 3 && !parts[2].trim().isEmpty() )
         ? parts[2].trim() : "Unknown";
  }
}
