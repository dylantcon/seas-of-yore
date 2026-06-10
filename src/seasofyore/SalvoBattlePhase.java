package seasofyore;

import seasofyore.ui.QuadrantPanel;
import seasofyore.ui.FallingStoneAnimation;
import java.awt.Graphics;
import java.awt.Point;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Represents the Salvo battle phase in the Seas of Yore game. In this phase, 
 * players can take multiple shots in a single turn, based on the number of 
 * their remaining ships. The class handles the logic for firing multiple shots,
 * managing animations, and determining victory conditions.
 * 
 * @author dylan
 */
public class SalvoBattlePhase extends BattlePhase
{
  /**
   * Queue of falling stone animations to play during the turn.
   */
  Queue< FallingStoneAnimation > animationQueue; // stores animations tbP
  
  /**
   * Queue of grid points representing the cells being targeted for shots.
   */
  Queue< Point > shotGridPointQueue;
  
  /**
   * The number of shots the player can take during their turn.
   */
  private int shotsRemaining;
  
  /**
   * The QuadrantPanel representing the target for Salvo shots.
   */
  private QuadrantPanel salvoTarget;
  
  
  /**
   * Initializes the Salvo battle phase when it is entered.
   * Disables interaction with the current player's quadrant and enables interaction
   * with the opponent's quadrant. Initializes shot and animation queues and logs 
   * the entry message.
   */  
  @Override
  protected void onEnter()
  {
    controller.getCurrentQuadrantPanel().disableCellInteraction();
    salvoTarget = targeted = controller.getNextQuadrantPanel();
    animationQueue = new LinkedList< >();
    shotGridPointQueue = new LinkedList< >();
    controller.announceBattleStart(); // fanfare, first battle turn only
    faller = null;

    // A restored game may resume mid-turn: shots already resolved this turn
    // come off the volley, and a fully spent one leaves only the flag.
    // Without this, each save-and-reload bought a whole fresh salvo.
    if ( computeShotsRemaining() == 0 )
    {
      salvoTarget.disableCellInteraction();
      controller.logToTerminal( "Thy salvo is spent." );
      controller.logToTerminal( NTPROMPT );
      controller.getTerminalPanel().setTurnButtonEnabled( true );
      return;
    }

    salvoTarget.enableCellInteraction();
    controller.logToTerminal( formatSalvoEntry() );
  }

  /**
   * The shots left in this turn's volley: one per surviving ship, less any
   * already resolved this turn (nonzero only when resuming a restored game
   * or re-entering after a pause mid-volley).
   *
   * @return the number of shots still owed to the volley
   */
  private int computeShotsRemaining()
  {
    int owed = controller.getCurrentPlayer().getRemainingShips()
             - controller.getBoard().getShotsFiredThisTurn();
    shotsRemaining = Math.max( 0, owed );
    return shotsRemaining;
  }
 
  /**
   * Handles a cell click event during the Salvo battle phase.
   * Locks the clicked cell for SALVO, queues the shot and its animation, and 
   * decrements the remaining shots.
   *
   * @param x             the x-coordinate of the clicked cell
   * @param y             the y-coordinate of the clicked cell
   * @param quadrantPanel the QuadrantPanel where the click occurred
   */
  @Override
  public void handleCellClick( int x, int y, QuadrantPanel quadrantPanel )
  { 
    // Check if they still have ammunition
    if ( shotsRemaining == 0 )
    {
      controller.logToTerminal( "No more shots this turn!" );
      return;
    }
    
    // check if targetable relative to subjected QuadrantPanel
    if ( !salvoTarget.canFireOn( x, y ) )
    {
      controller.logToTerminal( FIREDUPE );
      return;
    }
    
    // Iterate through previously selected targets, if any exist
    for ( Point p : shotGridPointQueue )
    {
      // Check if attempted cell is already targeted
      if ( p.x == x && p.y == y )
      {
        controller.logToTerminal( "Cell (" + x + "," + y + ") is already marked" );
        return;
      }
    }
    
    // lock in clicked cell as red. will be fixed as turn ends
    salvoTarget.lockCellForSALVO( x, y );
    shotGridPointQueue.add( new Point( x, y ) );
    shotsRemaining--;

    if ( controller.useStoneAnimations() )
    {
      // get global position of the cell in the dragLayerPanel
      Point global = salvoTarget.getGlobalCellPosition( x, y );
      animationQueue.add(
          new FallingStoneAnimation( global, controller.getDragLayerPanel() ) );
    }

    if ( shotsRemaining == 0 )
      playNextAnimation();
  }

  /**
   * Resolves the queued salvo, shot by shot. With stone animations on, each
   * shot rides its stone and the next launches when it lands; with them off,
   * the whole volley resolves instantly. Either way the turn finishes
   * through {@link #finishSalvo()} once the queue runs dry.
   */
  private void playNextAnimation()
  {
    if ( shotGridPointQueue.isEmpty() )
    {
      finishSalvo();
      return;
    }

    if ( !controller.useStoneAnimations() )
    {
      resolveNextShot( this::playNextAnimation );
      return;
    }

    faller = animationQueue.poll();
    faller.startAnimation( () ->
    {
      faller = null; // nullify falling animation instance
      controller.getDragLayerPanel().repaint();
      resolveNextShot( this::playNextAnimation );
    });
  }

  /**
   * Resolves the next queued shot through the match handler -- which may
   * answer immediately (offline) or once the verdict crosses the water
   * (networked) -- then continues the volley. A killing blow ends the
   * turn on the spot.
   *
   * @param onResolved how the volley continues after this shot
   */
  private void resolveNextShot( Runnable onResolved )
  {
    controller.getBoard().recordShotFired(); // survives a mid-turn save

    Point p = shotGridPointQueue.poll();
    salvoTarget.unlockCellForSalvo( p.x, p.y );

    controller.getMatchHandler().resolveOutgoingShot( p.x, p.y,
        ( hit, sunkType, defenderDefeated ) ->
    {
      controller.logToTerminal( buildShotMessage( hit, sunkType ) );

      if ( defenderDefeated )
      {
        controller.logToTerminal( VICTORY );
        controller.showWinScreen( controller.getCurrentPlayerCivilization() );
        return;
      }
      onResolved.run();
    });
  }

  /**
   * Ends the salvo turn and hands the player the flag (defeat is caught
   * per-shot, on the killing blow itself).
   */
  private void finishSalvo()
  {
    controller.getNextQuadrantPanel().disableCellInteraction();
    controller.logToTerminal( "SALVO turn complete!" );
    controller.logToTerminal( NTPROMPT );
    controller.getTerminalPanel().setTurnButtonEnabled( true );
  }
  
  /**
   * Renders any visuals specific to the Salvo battle phase.
   * Draws the falling stone animation if one is active.
   *
   * @param g the Graphics object used for rendering
   */
  @Override
  public void render( Graphics g )
  {
    if ( faller != null )
      faller.draw( g );
  }
  
  /**
   * Cleans up the Salvo battle phase when it ends.
   * Clears the animation queue, resets the number of shots remaining,
   * and invokes the parent class cleanup.
   */
  @Override
  public void cleanup()
  {
    super.cleanup();
    animationQueue.clear();
    shotsRemaining = 0;
  }
  
  /**
   * Formats the message displayed when entering the Salvo battle phase,
   * indicating the number of shots available for the turn. The count itself
   * is settled by computeShotsRemaining() during onEnter.
   *
   * @return the formatted entry message
   */
  private String formatSalvoEntry()
  {
    return String.format( " {SALVO}: You have %d shots this turn.",
                          shotsRemaining );
  }
}
