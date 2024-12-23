/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore;

import seasofyore.core.Ship;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;

/** 
 * Represents the battle phase of the Seas of Yore game.
 * This phase allows players to attack their opponent's quadrant, handles the 
 * logic for firing at cells, and determines the outcome of the game
 * (e.g., victory or continuation).
 * @author dylan
 */
public class BattlePhase extends AbstractGamePhase
{
  
  /**
   * Announcement message when entering the battle phase.
   */
  public static final String HEARYE = "*** HEAR YE, HEAR YE! THOU HAST ENTERED "
                                  + "THE BATTLE PHASE... ***";
  /**
   * Instructions for firing at the enemy's quadrant.
   */
  public static final String FIREINS = "If thou wisheth to fire at thine enemy, "
                                  + "thou must use the mouse to click a cell.";
  /**
   * Message indicating the player has already fired on a cell.
   */
  public static final String FIREDUPE = "Alas, thou hast already besieged this "
                                     + "cell.";
  /**
   * Message indicating a successful hit.
   */
  public static final String FIREHIT = "Huzzah, milord! A direct HIT!";
  
  /**
   * Message indicating a missed shot.
   */
  public static final String FIREMISS = "Zounds! A decisive MISS...";
  
  /**
   * Victory announcement message.
   */
  public static final String VICTORY = "*** VICTORY, Thou hast vanquished thy "
                                     + "foe! ***";
  /**
   * Prompt to pass the turn to the next player.
   */
  public static final String NTPROMPT = "-= Click your flag to pass to next player =-";
  
  /**
   * The QuadrantPanel representing the opponent's quadrant.
   */
  protected QuadrantPanel targeted;
  
  /**
   * The animation for a falling stone effect when firing at a cell.
   */
  protected FallingStoneAnimation faller;
  
  /**
   * Initializes the battle phase when it is entered.
   * Disables interaction with the current player's quadrant and enables
   * interaction with the opponent's quadrant.
   */ 
  @Override
  protected void onEnter()
  {
    faller = null;
    controller.getCurrentQuadrantPanel().disableCellInteraction();
    controller.getNextQuadrantPanel().enableCellInteraction();
    targeted = controller.getNextQuadrantPanel();
  }

  /**
   * Handles a cell click event during the battle phase.
   * Executes the firing logic, manages animations, logs the outcome, and checks for victory.
   *
   * @param x             the x-coordinate of the clicked cell
   * @param y             the y-coordinate of the clicked cell
   * @param quadrantPanel the QuadrantPanel where the click occurred
   */
  @Override
  public void handleCellClick( int x, int y, QuadrantPanel quadrantPanel )
  {
    if ( !targeted.canFireOn( x, y ) )
    {
      controller.logToTerminal( FIREDUPE );
      return;
    }
    // get position of the cell relative to 'effects' layer dragLayerPanel
    Point global = targeted.getGlobalCellPosition( x, y );
    faller = new FallingStoneAnimation( global, controller.getDragLayerPanel() );
    // animation instantiated, start animation thread ( on EDT )
    faller.startAnimation( () ->
    {
      boolean hit = quadrantPanel.fireAtCell( x, y );
      String message = hit ? getHitIdentifier( x, y ) : FIREMISS;
      // log resultant message in terminal
      controller.logToTerminal( message );
      faller = null; // nullify falling animation instance
      controller.getDragLayerPanel().repaint();
      
      if ( controller.getNextPlayer().hasLost() )
      {
        controller.logToTerminal( VICTORY );
        String winner = controller.getCurrentPlayerCivilization().toString();
        controller.showWinScreen( winner );
        return;
      }
      controller.logToTerminal( NTPROMPT );
      controller.getNextQuadrantPanel().disableCellInteraction();
      controller.getTerminalPanel().setTurnButtonEnabled( true );
    });
  }
 
  /**
   * Renders any visuals specific to the battle phase, such as the falling stone animation.
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
   * Cleans up the battle phase when it ends.
   * Removes all components from the drag layer, disables interactions, and 
   * repaints the drag layer.
   */
  @Override
  public void cleanup()
  {
    controller.getDragLayerPanel().removeAll();
    controller.getDragLayerPanel().repaint();
    
    controller.getCurrentQuadrantPanel().disableCellInteraction();
    controller.getNextQuadrantPanel().disableCellInteraction();
  }
  
  /**
   * Gets a message identifying the result of hitting a cell.
   * Indicates whether a ship was hit or sunk.
   *
   * @param x the x-coordinate of the hit cell
   * @param y the y-coordinate of the hit cell
   * @return a message describing the hit or sunk status of the target
   */
  protected String getHitIdentifier( int x, int y )
  {
    Ship hitShip = targeted.getOwner().getShipAt( x, y );
    if ( hitShip != null )
    {
      boolean isSunk = hitShip.isSunk();
      return isSunk
      ? "Thou hast SUNK the enemy's " + hitShip.getShipType() + "!"
      : "Thou hast HIT the enemy's " + hitShip.getShipType() + "!";
    }
    return FIREMISS;
  }
}

