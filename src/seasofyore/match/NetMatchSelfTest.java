package seasofyore.match;

import javalabrelay.ReferenceRelayServer;
import javalabrelay.RelayClient;
import javalabrelay.RelayTransports;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The networked-match plumbing test: establishes real matches over both
 * transports -- a direct TCP socket pair, and rooms through an in-process
 * reference relay -- and verifies the pre-game handshake (names and rules
 * crossing correctly) plus game-line exchange in both directions. Runs
 * headlessly: {@code java seasofyore.match.NetMatchSelfTest} with the
 * relay library on the classpath; prints PASS lines and exits nonzero on
 * the first failure.
 *
 * @author dylan
 */
public final class NetMatchSelfTest
{
  /**
   * How long any single expected event may take.
   */
  private static final int WAIT_MS = 10_000;

  /**
   * Queues a transport's events for assertions.
   */
  private static final class RecordingListener implements MatchTransport.Listener
  {
    final BlockingQueue<String> events = new ArrayBlockingQueue<>( 64 );

    @Override
    public void onLine( String line )
    {
      events.add( "LINE " + line );
    }

    @Override
    public void onClosed( String reason )
    {
      events.add( "CLOSED" );
    }
  }

  private NetMatchSelfTest() {}

  /**
   * Runs both transport suites.
   *
   * @param args unused
   * @throws Exception on the first failed expectation
   */
  public static void main( String[] args ) throws Exception
  {
    testLanMatch();
    testRelayMatch();
    System.out.println( "ALL TESTS PASSED" );
    System.exit( 0 ); // EDT and daemon threads linger otherwise
  }

  /**
   * A LAN match on an ephemeral port: handshake, then lines both ways.
   */
  private static void testLanMatch() throws Exception
  {
    final ServerSocket harbor = new ServerSocket( 0 );

    final AtomicReference<MatchConnector.Connection> hostConn = new AtomicReference<>();
    Thread hostThread = new Thread( () ->
    {
      try
      {
        hostConn.set( MatchConnector.hostLan( harbor, "Arthur", true ) );
      }
      catch ( IOException ex )
      {
        // hostConn stays null; the assertion below reports it
      }
    });
    hostThread.start();

    MatchConnector.Connection joiner = MatchConnector.joinLan(
        "127.0.0.1:" + harbor.getLocalPort(), "Charlemagne" );
    hostThread.join( WAIT_MS );

    MatchConnector.Connection host = hostConn.get();
    require( host != null, "LAN: host side connected" );
    require( host.host && !joiner.host, "LAN: exactly one side hosts" );
    require( "Charlemagne".equals( host.remoteName ), "LAN: host learned the joiner's name" );
    require( "Arthur".equals( joiner.remoteName ), "LAN: joiner learned the host's name" );
    require( joiner.salvo, "LAN: joiner learned the host's SALVO rules" );

    exchangeLines( host.transport, joiner.transport, "LAN" );

    host.transport.close();
    joiner.transport.close();
    harbor.close();
  }

  /**
   * A relay match through an in-process reference relay: pairing by room
   * code, handshake, then lines both ways.
   */
  private static void testRelayMatch() throws Exception
  {
    ReferenceRelayServer relay = new ReferenceRelayServer( 0, true );
    relay.runInBackground();
    System.setProperty( RelayTransports.URI_PROPERTY,
                        "ws://127.0.0.1:" + relay.getPort() + "/relay" );

    final String room = RelayClient.randomRoomCode();

    final AtomicReference<MatchConnector.Connection> hostConn = new AtomicReference<>();
    Thread hostThread = new Thread( () ->
    {
      try
      {
        hostConn.set( MatchConnector.hostOnline( room, "Arthur", false, null ) );
      }
      catch ( IOException ex )
      {
        // hostConn stays null; the assertion below reports it
      }
    });
    hostThread.start();

    Thread.sleep( 500 ); // let the host open the room before joining

    MatchConnector.Connection joiner =
        MatchConnector.joinOnline( room, "Charlemagne" );
    hostThread.join( WAIT_MS );

    MatchConnector.Connection host = hostConn.get();
    require( host != null, "Relay: host side connected" );
    require( "Charlemagne".equals( host.remoteName ), "Relay: host learned the joiner's name" );
    require( "Arthur".equals( joiner.remoteName ), "Relay: joiner learned the host's name" );
    require( !joiner.salvo, "Relay: joiner learned the host's Classic rules" );

    exchangeLines( host.transport, joiner.transport, "Relay" );

    host.transport.close();
    joiner.transport.close();
    relay.shutdown();
  }

  /**
   * Sends representative game-protocol lines both directions and asserts
   * verbatim arrival.
   */
  private static void exchangeLines( MatchTransport hostSide,
                                     MatchTransport joinerSide,
                                     String label ) throws Exception
  {
    RecordingListener hostEvents = new RecordingListener();
    RecordingListener joinerEvents = new RecordingListener();
    hostSide.setListener( hostEvents );
    joinerSide.setListener( joinerEvents );

    hostSide.sendLine( "GSHOT 3 4" );
    expect( joinerEvents.events, "LINE GSHOT 3 4",
            label + ": host's shot crossed" );

    joinerSide.sendLine( "GRESULT 3 4 HIT SUNK COG" );
    expect( hostEvents.events, "LINE GRESULT 3 4 HIT SUNK COG",
            label + ": joiner's verdict crossed" );

    joinerSide.sendLine( "GCHAT well struck, friend" );
    expect( hostEvents.events, "LINE GCHAT well struck, friend",
            label + ": chat crossed with spaces intact" );
  }

  /**
   * Waits for the next event on a queue and asserts its value.
   */
  private static void expect( BlockingQueue<String> events, String wanted,
                              String description ) throws Exception
  {
    String got = events.poll( WAIT_MS, TimeUnit.MILLISECONDS );
    if ( !wanted.equals( got ) )
      fail( description + " -- wanted [" + wanted + "] got [" + got + "]" );
    pass( description );
  }

  /**
   * Asserts a condition.
   */
  private static void require( boolean condition, String description )
  {
    if ( !condition )
      fail( description );
    pass( description );
  }

  private static void pass( String description )
  {
    System.out.println( "PASS: " + description );
  }

  private static void fail( String description )
  {
    System.out.println( "FAIL: " + description );
    System.exit( 1 );
  }
}
