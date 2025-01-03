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
    controller.getNextQuadrantPanel().enableCellInteraction();
    salvoTarget = targeted = controller.getNextQuadrantPanel();
    animationQueue = new LinkedList< >();
    shotGridPointQueue = new LinkedList< >();
    controller.logToTerminal( formatSalvoEntry() );
    faller = null;
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
    if ( shotsRemaining == 0 )
    {
      controller.logToTerminal( "No more shots this turn!" );
      return;
    }
    
    if ( !salvoTarget.canFireOn( x, y ) )
    {
      controller.logToTerminal( FIREDUPE );
      return;
    }
    // lock in clicked cell as red. will be fixed as turn ends
    salvoTarget.lockCellForSALVO( x, y );
    shotGridPointQueue.add( new Point( x, y ) );
    shotsRemaining--;
    FallingStoneAnimation stone;
    
    // get global position of the cell in the dragLayerPanel
    Point global = salvoTarget.getGlobalCellPosition( x, y );
    stone = new FallingStoneAnimation( global, controller.getDragLayerPanel() );
    animationQueue.add( stone );
    
    if ( shotsRemaining == 0 )
      playNextAnimation();
  }
  
  /**
   * Plays the next animation in the queue.
   * Resolves the shot once the animation is complete and logs the result.
   * If the opponent has lost, displays the victory screen. Otherwise, continues 
   * to the next animation.
   */
  private void playNextAnimation()
  {
    if ( !animationQueue.isEmpty() )
    {
      faller = animationQueue.poll();
      faller.startAnimation( () ->
      {
        Point p = shotGridPointQueue.poll();
        salvoTarget.unlockCellForSalvo( p.x, p.y );
        salvoTarget.fireAtCell( p.x, p.y );
        // controller.getCurrentPlayer().tryEnemyShipDamage( p.x, p.y );
        String message = getHitIdentifier( p.x, p.y );
        // get hit identifier message, then
        // log resultant message in terminal
        controller.logToTerminal( message );
        faller = null; // nullify falling animation instance
       
        controller.getDragLayerPanel().repaint();
        
        playNextAnimation();
        controller.getDragLayerPanel().repaint();
      });
    }
    else
    {
      controller.getNextQuadrantPanel().disableCellInteraction();
      controller.logToTerminal( "SALVO turn complete!" );
      
      if ( controller.getNextPlayer().hasLost() )
      {
        controller.logToTerminal( VICTORY );
        String winner = controller.getCurrentPlayerCivilization().toString();
        controller.showWinScreen( winner );
        return;
      }  
      controller.logToTerminal( NTPROMPT );
      controller.getTerminalPanel().setTurnButtonEnabled( true );
    }
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
   * indicating the number of shots available for the turn.
   *
   * @return the formatted entry message
   */
  private String formatSalvoEntry()
  {
    shotsRemaining = controller.getCurrentPlayer().getRemainingShips();
    return String.format( " {SALVO}: You have %d shots this turn.", 
                          shotsRemaining );
  }
}
