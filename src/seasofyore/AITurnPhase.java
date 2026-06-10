/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore;

import seasofyore.core.Player;
import seasofyore.core.PlayerType;
import seasofyore.core.Ship;
import seasofyore.ui.FallingStoneAnimation;
import seasofyore.ui.QuadrantPanel;
import seasofyore.ui.TerminalPanel;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import javax.swing.Timer;

/**
 * Represents a game phase where an AI player takes its turn. Like every
 * phase, it belongs to the CURRENT player: the AI is the current player and
 * attacks the next player's quadrant. The phase paces its shots with timers,
 * drops the same falling-stone animation a human attack gets, and hands the
 * turn back automatically -- no curtain or flag click is ever required to
 * get past an AI's turn.
 *
 * @author dylan
 */
public class AITurnPhase extends AbstractGamePhase
{
  /**
   * The delay between AI actions in milliseconds
   */
  private static final int AI_ACTION_DELAY = 750;

  /**
   * Timer for controlling AI action timing
   */
  private Timer actionTimer;

  /**
   * The falling-stone animation for the AI's shot in flight, mirroring the
   * effect rendered for human attacks in BattlePhase.
   */
  private FallingStoneAnimation faller;

  /**
   * The salvos remaining for the AI in Salvo mode.
   */
  private int salvosRemaining = 0;

  /**
   * Whether the game is in Salvo mode.
   */
  private final boolean salvoMode;

  /**
   * Constructs a new AI turn phase.
   *
   * @param salvoMode true if the game is in Salvo mode; false otherwise
   */
  public AITurnPhase( boolean salvoMode )
  {
    this.salvoMode = salvoMode;
  }

  /**
   * Called when the phase is entered.
   * Sets up the AI turn and initiates the first action.
   */
  @Override
  protected void onEnter()
  {
    faller = null;

    // nobody clicks anything during an AI turn
    controller.getCurrentQuadrantPanel().disableCellInteraction();
    controller.getNextQuadrantPanel().disableCellInteraction();
    controller.getTerminalPanel().setTurnButtonEnabled( false );

    // display message that the AI commander -- by name -- is taking its turn
    controller.announceBattleStart(); // fanfare, first battle turn only
    PlayerType aiType = controller.getBoard()
        .getPlayerType( controller.getCurrentPlayerCivilization() );
    controller.logToTerminal( TerminalPanel.ITALIC + aiType.getNickname()
                            + " surveys thy waters..." + TerminalPanel.RESET );

    // initialize salvos for SALVO mode
    if ( salvoMode )
    {
      salvosRemaining = controller.getCurrentPlayer().getRemainingShips();
      controller.logToTerminal( String.format( "AI has %d shots this turn", salvosRemaining ) );
    }

    // start the AI action sequence after a short delay
    actionTimer = new Timer( AI_ACTION_DELAY, ( ActionEvent e ) ->
    {
      performAIAction();
    });
    actionTimer.setRepeats( false );
    actionTimer.start();
  }

  /**
   * Performs a single AI action: picks a target cell and launches the stone
   * at it. The shot itself resolves when the animation lands.
   */
  private void performAIAction()
  {
    Player aiPlayer = controller.getCurrentPlayer();
    QuadrantPanel targetPanel = controller.getNextQuadrantPanel();

    // calculate AI's next attack coordinates
    int[] attackCoords = aiPlayer.calculateNextAttack();

    if ( attackCoords == null )
    {
      // no valid moves, end turn
      finishAITurn();
      return;
    }

    int x = attackCoords[0];
    int y = attackCoords[1];

    if ( !controller.useStoneAnimations() )
    {
      // the player turned the show off: the AI's shot lands instantly too
      resolveAIShot( targetPanel, x, y );
      return;
    }

    // drop a stone on the target and resolve the shot once it lands, exactly
    // as BattlePhase does for the human's attacks
    Point global = targetPanel.getGlobalCellPosition( x, y );
    faller = new FallingStoneAnimation( global, controller.getDragLayerPanel() );
    faller.startAnimation( () -> resolveAIShot( targetPanel, x, y ) );
  }

  /**
   * Resolves a landed AI shot: updates the board, feeds the result back into
   * the AI's strategy, logs it, and either continues the salvo or schedules
   * the end of the turn.
   *
   * @param targetPanel the defender's QuadrantPanel
   * @param x           the x-coordinate of the struck cell
   * @param y           the y-coordinate of the struck cell
   */
  private void resolveAIShot( QuadrantPanel targetPanel, int x, int y )
  {
    faller = null;
    controller.getDragLayerPanel().repaint();

    Player aiPlayer = controller.getCurrentPlayer();
    Player defender = controller.getNextPlayer();

    // A true HIT means the defender actually has a ship on this cell. We must
    // NOT use the return of fireAtCell(): that only reports the shot was
    // legal (it returns true for misses too), which made every valid shot
    // read as a "hit". getShipAt is independent of fired state, so we can ask
    // it either side of the shot.
    boolean hit = ( defender.getShipAt( x, y ) != null );

    // execute the attack (renders the hit/miss tile and syncs deck state)
    targetPanel.fireAtCell( x, y );

    // process the result
    aiPlayer.processAttackResult( x, y, hit );

    // if that shot sank one of the defender's ships, tell the AI so its
    // strategy can stop hunting it and shrink its expected fleet. fireAtCell
    // has already synced the defender's deck state, so isSunk() is current.
    Ship struck = hit ? defender.getShipAt( x, y ) : null;
    boolean sunk = ( struck != null && struck.isSunk() );
    if ( sunk )
      aiPlayer.notifyEnemyShipSunk( struck.getShipType(), x, y );

    // log the result
    String resultMessage;
    if ( !hit )
      resultMessage = TerminalPanel.BLUE + "AI missed at " + x + "," + y + "."
                    + TerminalPanel.RESET;
    else if ( sunk )
      resultMessage = TerminalPanel.RED + TerminalPanel.BOLD + "AI SANK the "
                    + struck.getShipType() + " at " + x + "," + y + "!"
                    + TerminalPanel.RESET;
    else
      resultMessage = TerminalPanel.RED + "AI hit a ship at " + x + "," + y
                    + "!" + TerminalPanel.RESET;
    controller.logToTerminal( resultMessage );

    // check if the defender has lost
    if ( defender.hasLost() )
    {
      controller.logToTerminal( TerminalPanel.GOLD + TerminalPanel.BOLD
                              + "The " + defender.getCiv()
                              + " fleet is vanquished!" + TerminalPanel.RESET );
      controller.showWinScreen( aiPlayer.getCiv() );
      return;
    }

    // in salvo mode, continue until all salvos are spent
    if ( salvoMode )
    {
      salvosRemaining--;
      if ( salvosRemaining > 0 )
      {
        // continue with the next salvo after a delay
        actionTimer = new Timer( AI_ACTION_DELAY, ( ActionEvent e ) ->
        {
          performAIAction();
        });
        actionTimer.setRepeats( false );
        actionTimer.start();
        return;
      }
    }

    // end turn after a delay
    actionTimer = new Timer( AI_ACTION_DELAY, ( ActionEvent e ) ->
    {
      finishAITurn();
    });
    actionTimer.setRepeats( false );
    actionTimer.start();
  }

  /**
   * Finishes the AI turn and advances the game. The handoff is automatic in
   * every case: to the human in a solo game, or straight to the other AI in
   * a spectated match. switchTurns() itself knows no curtain is needed when
   * an AI is involved.
   */
  private void finishAITurn()
  {
    controller.logToTerminal( "AI turn complete." );
    controller.switchTurns();
  }

  /**
   * Cleans up resources when the phase ends.
   */
  @Override
  public void cleanup()
  {
    if ( actionTimer != null && actionTimer.isRunning() )
      actionTimer.stop();

    if ( faller != null )
    {
      faller.stop();
      faller = null;
    }
  }

  /**
   * Freezes the AI's turn for a game pause. At any instant exactly one
   * driver is pending -- the action timer counting down to the next step,
   * or a stone in flight -- and both freeze reversibly.
   */
  @Override
  public void pause()
  {
    if ( actionTimer != null && actionTimer.isRunning() )
      actionTimer.stop();

    if ( faller != null )
      faller.pause();
  }

  /**
   * Resumes whichever driver was pending when the game paused: a stone in
   * flight keeps falling, otherwise the action timer restarts its delay.
   */
  @Override
  public void resume()
  {
    if ( faller != null )
      faller.resume();
    else if ( actionTimer != null )
      actionTimer.start();
  }

  /**
   * Updates the phase state.
   * Not actively used as AI actions are timer-driven.
   */
  @Override
  public void update() {}

  /**
   * Renders the falling stone for the AI's shot in flight.
   *
   * @param g the Graphics object used for rendering
   */
  @Override
  public void render( Graphics g )
  {
    if ( faller != null )
      faller.draw( g );
  }
}
