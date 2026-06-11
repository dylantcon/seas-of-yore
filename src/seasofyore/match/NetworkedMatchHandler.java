package seasofyore.match;

import seasofyore.GameController;
import seasofyore.GamePhase;
import seasofyore.RemoteTurnPhase;
import seasofyore.WaitingForEnemyPhase;
import seasofyore.core.Civilization;
import seasofyore.core.Player;
import seasofyore.core.RemotePlayer;
import seasofyore.core.Ship;
import seasofyore.core.ShipType;
import seasofyore.ui.QuadrantPanel;
import seasofyore.ui.TerminalPanel;
import java.io.IOException;
import javax.swing.JOptionPane;

/**
 * The engine of every networked match, written once against
 * {@link MatchTransport} so a direct LAN socket and a relay-carried
 * WebSocket behave identically. The four leaf classes (LAN/Online x
 * server/client) exist to name the taxonomy; everything they share is
 * here.
 *
 * <h2>The wire game protocol</h2>
 * Each side is authoritative over its own fleet: ship positions never
 * cross the water, only declarations and verdicts do. After the
 * connector's handshake (GHELLO/GRULES, performed before the game is
 * built), the lines are:
 * <pre>
 *   GREADY                                      my fleet is placed
 *   GSHOT &lt;x&gt; &lt;y&gt;                               I fire at your waters
 *   GRESULT &lt;x&gt; &lt;y&gt; HIT|MISS [SUNK &lt;type&gt;] [DEFEATED]   my verdict on your shot
 *   GENDTURN                                    my turn is over; yours begins
 *   GCHAT &lt;text&gt;                                a word across the water
 *   GFORFEIT                                    I strike my colours
 * </pre>
 * The host commands the Britons and fires first; both boards mirror the
 * turn state by exchanging GENDTURN.
 *
 * <h2>What all networked flavours share</h2>
 * No curtain (the opponent cannot see this screen), no pausing (the
 * remote clock cannot be frozen; the pause key offers forfeit instead),
 * chat in the terminal, and exactly one local player -- which is what
 * stages victory against defeat on the end screen.
 *
 * @author dylan
 */
public abstract class NetworkedMatchHandler extends AbstractMatchHandler
    implements MatchTransport.Listener
{
  /**
   * The pipe to the opposing screen.
   */
  protected final MatchTransport transport;

  /**
   * Whether this screen hosts (commands the Britons and fires first).
   */
  protected final boolean host;

  /**
   * The local commander's chosen name.
   */
  protected final String localName;

  /**
   * The remote commander's name, learned in the connector's handshake.
   */
  protected final String remoteName;

  /**
   * The shot awaiting the remote side's GRESULT, if any.
   */
  private ShotOutcome pendingOutcome;

  /**
   * Fleet-ready declarations from each side of the water.
   */
  private boolean localReady = false;
  private boolean remoteReady = false;

  /**
   * True while applying a remote GENDTURN, so the resulting local turn
   * switch does not echo GENDTURN straight back.
   */
  private boolean remoteAdvancing = false;

  /**
   * Set once the match has a winner, so a subsequent connection close is
   * an anticlimax rather than an error dialog.
   */
  private boolean concluded = false;

  /**
   * Builds the engine around an established connection.
   *
   * @param transport  the connected pipe to the opposing screen
   * @param host       whether this screen hosts the match
   * @param localName  the local commander's name
   * @param remoteName the remote commander's name (from the handshake)
   */
  protected NetworkedMatchHandler( MatchTransport transport, boolean host,
                                   String localName, String remoteName )
  {
    this.transport = transport;
    this.host = host;
    this.localName = localName;
    this.remoteName = remoteName;
    transport.setListener( this );
  }

  /**
   * Binds to the running game: names the remote stand-in and starts
   * placement on the LOCAL player at this end -- both screens place
   * simultaneously, regardless of nominal turn order.
   */
  @Override
  public void beginMatch( GameController controller )
  {
    super.beginMatch( controller );

    remotePlayer().setName( remoteName );
    board().forceCurrentPlayer( localCiv() );
  }

  /**
   * The civilization this screen commands.
   *
   * @return Britons for the host, Franks for the joiner
   */
  protected final Civilization localCiv()
  {
    return host ? Civilization.BRITONS : Civilization.FRANKS;
  }

  /**
   * The civilization across the water.
   *
   * @return the opponent's civilization
   */
  protected final Civilization remoteCiv()
  {
    return host ? Civilization.FRANKS : Civilization.BRITONS;
  }

  /**
   * The local player object.
   */
  @Override
  public final Player getLocalPlayer()
  {
    return ( localCiv() == Civilization.BRITONS ) ? board().getBritons()
                                                  : board().getFranks();
  }

  /**
   * The remote player's local stand-in.
   */
  protected final RemotePlayer remotePlayer()
  {
    Player remote = ( remoteCiv() == Civilization.BRITONS )
                  ? board().getBritons() : board().getFranks();
    return (RemotePlayer) remote;
  }

  /**
   * Never: a remote opponent cannot peek at this screen.
   */
  @Override
  public final boolean showsCurtain()
  {
    return false;
  }

  /**
   * Never: the remote side's match cannot be frozen from here.
   */
  @Override
  public final boolean supportsPause()
  {
    return false;
  }

  /**
   * Always: chat is the point of having a terminal in a networked match.
   */
  @Override
  public final boolean supportsChat()
  {
    return true;
  }

  /**
   * Whether this screen holds the active turn.
   */
  @Override
  public final boolean isLocalTurn()
  {
    return board().getCurrentPlayer() == getLocalPlayer();
  }

  /**
   * The two networked turn-handoff special cases. During setup, the flag
   * click means "my fleet is placed": declare GREADY and either begin the
   * battle (if the enemy already declared) or wait -- taking over the
   * transition entirely. In battle, relay the handoff with GENDTURN
   * (unless this switch IS the application of the enemy's GENDTURN) and
   * let the controller advance both boards in step.
   */
  @Override
  public boolean interceptTurnEnd()
  {
    if ( !localReady )
    {
      localReady = true;
      sendQuietly( "GREADY" );

      if ( remoteReady )
        startBattle();
      else
        controller.setPhase( new WaitingForEnemyPhase() );
      return true;
    }

    if ( !remoteAdvancing )
      sendQuietly( "GENDTURN" );
    return false;
  }

  /**
   * Sends the local shot across the water; the verdict arrives later as
   * GRESULT and completes the outcome callback. Exactly one shot may be
   * in the air: a second one would overwrite the pending verdict slot
   * and desynchronize the boards, so it is a programming error in the
   * calling phase (which must lock firing until the verdict lands).
   */
  @Override
  public void resolveOutgoingShot( int x, int y, ShotOutcome outcome )
  {
    if ( pendingOutcome != null )
      throw new IllegalStateException(
          "A shot is already awaiting its GRESULT; the firing phase "
          + "must lock the panel until the verdict arrives." );

    pendingOutcome = outcome;
    sendQuietly( "GSHOT " + x + " " + y );
  }

  /**
   * Sends a chat line and echoes it locally in the terminal.
   *
   * @param message the text the local player typed
   */
  public void sendChat( String message )
  {
    sendQuietly( "GCHAT " + message );
    controller.getTerminal().logMessage(
        TerminalPanel.CYAN + TerminalPanel.BOLD + "[" + localName + "]"
        + TerminalPanel.RESET + " " + TerminalPanel.CYAN + message
        + TerminalPanel.RESET );
  }

  /**
   * Strikes the colours: tells the enemy, closes the pipe, returns to
   * the title.
   */
  @Override
  public void forfeit()
  {
    concluded = true;
    sendQuietly( "GFORFEIT" );
    transport.close();
    controller.abandonToTitle();
  }

  /**
   * Releases the connection.
   */
  @Override
  public void shutdown()
  {
    concluded = true;
    transport.close();
  }

  // ------------------------------------------------------------------
  // MatchTransport.Listener (on the EDT)
  // ------------------------------------------------------------------

  @Override
  public void onLine( String line )
  {
    String keyword = firstWord( line );
    String rest = restAfterFirstWord( line );

    switch ( keyword )
    {
      case "GSHOT":    handleIncomingShot( rest );  break;
      case "GRESULT":  handleShotResult( rest );    break;
      case "GENDTURN": handleRemoteTurnEnd();       break;
      case "GREADY":   handleRemoteReady();         break;
      case "GCHAT":    handleChat( rest );          break;
      case "GFORFEIT": handleForfeit();             break;
      default:
        break; // unknown chatter tolerated for forward compatibility
    }
  }

  @Override
  public void onClosed( String reason )
  {
    if ( concluded )
      return;
    concluded = true;

    JOptionPane.showMessageDialog( controller,
        "The line to the enemy went dead: " + reason,
        "Connection lost", JOptionPane.WARNING_MESSAGE );
    controller.abandonToTitle();
  }

  // ------------------------------------------------------------------
  // Incoming protocol handling
  // ------------------------------------------------------------------

  /**
   * The enemy fires at this screen's waters. The verdict is computed
   * here -- only this side knows its own fleet -- marked, answered, and
   * shown (with the falling stone, when the show is on).
   */
  private void handleIncomingShot( String rest )
  {
    String[] parts = rest.split( " " );
    final int x = Integer.parseInt( parts[0] );
    final int y = Integer.parseInt( parts[1] );

    Runnable resolve = () -> resolveIncomingShot( x, y );

    GamePhase phase = controller.getCurrentPhase();
    if ( controller.useStoneAnimations() && phase instanceof RemoteTurnPhase )
      ( (RemoteTurnPhase) phase ).animateIncoming( x, y, resolve );
    else
      resolve.run();
  }

  /**
   * Applies an enemy shot to the local fleet and answers with the
   * verdict.
   */
  private void resolveIncomingShot( int x, int y )
  {
    Player local = getLocalPlayer();
    // during the enemy's turn the local fleet is the "next" player's
    QuadrantPanel localPanel = controller.getNextQuadrantPanel();

    boolean hit = ( local.getShipAt( x, y ) != null );
    localPanel.fireAtCell( x, y );
    controller.getBoard().recordShotFired();

    Ship struck = hit ? local.getShipAt( x, y ) : null;
    boolean sunk = ( struck != null && struck.isSunk() );
    boolean defeated = local.hasLost();

    StringBuilder verdict = new StringBuilder( "GRESULT " ).append( x )
        .append( ' ' ).append( y ).append( hit ? " HIT" : " MISS" );
    if ( sunk )
      verdict.append( " SUNK " ).append( struck.getShipType() );
    if ( defeated )
      verdict.append( " DEFEATED" );
    sendQuietly( verdict.toString() );

    String name = remotePlayer().getTitledName();
    if ( !hit )
      controller.logToTerminal( TerminalPanel.BLUE + name
          + "'s stone splashes harmlessly at " + x + "," + y + "."
          + TerminalPanel.RESET );
    else if ( sunk )
      controller.logToTerminal( TerminalPanel.RED + TerminalPanel.BOLD + name
          + " hath SUNK thy " + struck.getShipType() + "!"
          + TerminalPanel.RESET );
    else
      controller.logToTerminal( TerminalPanel.RED + name
          + " strikes thy " + struck.getShipType() + " at " + x + "," + y
          + "!" + TerminalPanel.RESET );

    if ( defeated )
    {
      concluded = true;
      controller.showWinScreen( remoteCiv() );
    }
  }

  /**
   * The enemy's verdict on the local player's shot: mark the enemy
   * quadrant and complete the firing phase's outcome callback.
   */
  private void handleShotResult( String rest )
  {
    String[] parts = rest.split( " " );
    int x = Integer.parseInt( parts[0] );
    int y = Integer.parseInt( parts[1] );
    boolean hit = "HIT".equals( parts[2] );

    ShipType sunkType = null;
    boolean defeated = false;
    for ( int i = 3; i < parts.length; i++ )
    {
      if ( "SUNK".equals( parts[i] ) && i + 1 < parts.length )
        sunkType = ShipType.valueOf( parts[++i] );
      if ( "DEFEATED".equals( parts[i] ) )
        defeated = true;
    }

    // mark what we learned on the remote quadrant (their panel hides
    // ships anyway; hits and misses are all we will ever know)
    remotePlayer().getFriendlyQuad().setCellType( x, y,
        hit ? seasofyore.core.PlayerQuadrant.HIT_CELL
            : seasofyore.core.PlayerQuadrant.MISS_CELL );
    controller.getBoardPanel().repaint();

    if ( defeated )
    {
      remotePlayer().markDefeated();
      concluded = true;
    }

    if ( pendingOutcome != null )
    {
      ShotOutcome outcome = pendingOutcome;
      pendingOutcome = null;
      outcome.onResolved( hit, sunkType, defeated );
    }
  }

  /**
   * The enemy's turn ended: advance the local board to match, without
   * echoing GENDTURN back.
   */
  private void handleRemoteTurnEnd()
  {
    remoteAdvancing = true;
    try
    {
      controller.switchTurns();
    }
    finally
    {
      remoteAdvancing = false;
    }
  }

  /**
   * The enemy's fleet is placed. If ours is too, the battle begins.
   */
  private void handleRemoteReady()
  {
    remoteReady = true;
    remotePlayer().markFleetReady();

    if ( localReady )
      startBattle();
    else
      controller.getTerminal().logMessage( TerminalPanel.GOLD
          + remotePlayer().getTitledName()
          + "'s fleet stands ready across the water." + TerminalPanel.RESET );
  }

  /**
   * Chat from across the water.
   */
  private void handleChat( String text )
  {
    controller.getTerminal().logMessage(
        TerminalPanel.CYAN + TerminalPanel.BOLD + "[" + remoteName + "]"
        + TerminalPanel.RESET + " " + TerminalPanel.CYAN + text
        + TerminalPanel.RESET );
  }

  /**
   * The enemy struck their colours: the local player wins.
   */
  private void handleForfeit()
  {
    concluded = true;
    controller.getTerminal().logMessage( TerminalPanel.GOLD + TerminalPanel.BOLD
        + remotePlayer().getTitledName() + " hath struck their colours!"
        + TerminalPanel.RESET );
    controller.showWinScreen( localCiv() );
  }

  /**
   * Both fleets stand placed: battle begins on the host's civilization,
   * simultaneously at both ends.
   */
  private void startBattle()
  {
    controller.beginBattleAs( Civilization.BRITONS );
  }

  /**
   * Sends a line, treating transport failure as a closed connection
   * (the reader side will surface it properly).
   */
  protected final void sendQuietly( String line )
  {
    try
    {
      transport.sendLine( line );
    }
    catch ( IOException ex )
    {
      // the read side notices and reports the dead pipe
    }
  }

  /**
   * The first space-delimited word of a line (or all of it).
   */
  private static String firstWord( String line )
  {
    int space = line.indexOf( ' ' );
    return ( space < 0 ) ? line : line.substring( 0, space );
  }

  /**
   * Everything after the first word, or the empty string.
   */
  private static String restAfterFirstWord( String line )
  {
    int space = line.indexOf( ' ' );
    return ( space < 0 ) ? "" : line.substring( space + 1 );
  }
}
