/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore;

import seasofyore.core.Player;
import seasofyore.core.Ship;
import seasofyore.ui.QuadrantPanel;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import javax.swing.Timer;

/**
 * Represents a game phase where an AI player takes its turn.
 * Handles automated AI actions without requiring the curtain transition
 * or manual user interaction to proceed.
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
   * Indicates whether the AI has completed its turn.
   */
  private boolean turnComplete = false;
  
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
    // AI attacks the human player's quadrant (current player's quadrant)
    QuadrantPanel targetPanel = controller.getCurrentQuadrantPanel();
    
    // Disable interaction during AI turn
    targetPanel.disableCellInteraction();
    controller.getNextQuadrantPanel().disableCellInteraction();
    
    // display message that AI is taking its turn
    controller.logToTerminal( "AI opponent is preparing to attack..." );
    
    Player aiPlayer = controller.getNextPlayer();
    
    // initialize salvos for SALVO mode
    if ( salvoMode )
    {
      salvosRemaining = aiPlayer.getRemainingShips();
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
   * Performs a single AI action (attack).
   */
  private void performAIAction()
  {
    // get the AI player and target panel
    Player aiPlayer = controller.getNextPlayer();
    QuadrantPanel targetPanel = controller.getCurrentQuadrantPanel();
    
    // calculate AI's next attack coordinates
    int[] attackCoords = aiPlayer.calculateNextAttack();
    
    if ( attackCoords != null )
    {
      int x = attackCoords[0];
      int y = attackCoords[1];
      
      // A true HIT means the defender actually has a ship on this cell. We must
      // NOT use the return of fireAtCell(): that only reports the shot was
      // legal (it returns true for misses too), which made every valid shot
      // read as a "hit". getShipAt is independent of fired state, so we can ask
      // it either side of the shot.
      Player defender = controller.getCurrentPlayer();
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
        resultMessage = "AI missed at " + x + "," + y + ".";
      else if ( sunk )
        resultMessage = "AI SANK your " + struck.getShipType() + " at " + x + "," + y + "!";
      else
        resultMessage = "AI hit your ship at " + x + "," + y + "!";
      controller.logToTerminal(resultMessage);
      
      // check if human player has lost
      if ( controller.getCurrentPlayer().hasLost() )
      {
        controller.logToTerminal( "You have been defeated by the AI!" );
        String winner = controller.getNextPlayer().getCiv().toString();
        
        controller.showWinScreen( winner );
        return;
      }
      
      // in salvo mode, continue until all salvos are spent
      if ( salvoMode )
      {
        salvosRemaining--;
        if ( salvosRemaining > 0 )
        {
          // continue with next salvo after a delay
          actionTimer.setInitialDelay( AI_ACTION_DELAY );
          actionTimer.start();
          return;
        }
      }
      
      // end turn after a delay
      turnComplete = true;
      actionTimer = new Timer( AI_ACTION_DELAY, ( ActionEvent e ) -> 
      {
        finishAITurn();
      });
      actionTimer.setRepeats( false );
      actionTimer.start();
    }
    else
      // no valid moves, end turn
      finishAITurn();
  }
  
  /**
   * Finishes the AI turn and switches back to the human player.
   */
  private void finishAITurn()
  {
    // Only a true AI-vs-AI spectator game (no humans at all) advances
    // automatically. ANY game with a human -- including single-player vs AI --
    // hands control back exactly as before: light the turn flag and wait for a
    // click. Gating on the human count avoids touching the single-player flow.
    if ( controller.getBoard().isSpectating() )
    {
      controller.logToTerminal( "AI turn complete." );
      controller.switchTurns();
    }
    else
    {
      controller.logToTerminal( "AI turn complete. Your turn now." );
      controller.logToTerminal( BattlePhase.NTPROMPT );
      controller.getTerminalPanel().setTurnButtonEnabled( true );
    }
  }
  
  /**
   * Cleans up resources when the phase ends.
   */
  @Override
  public void cleanup()
  {
    if ( actionTimer != null && actionTimer.isRunning() )
    {
      actionTimer.stop();
    }
  }
  
  /**
   * Updates the phase state.
   * Not actively used as AI actions are timer-driven.
   */
  @Override
  public void update() {}
  
  /**
   * Renders any custom visuals for the phase.
   * Not needed for AITurnPhase.
   */
  @Override
  public void render( Graphics g ) {}
}
