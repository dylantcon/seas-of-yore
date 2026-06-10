/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore;

import seasofyore.ui.QuadrantPanel;
import seasofyore.ui.FallingStoneAnimation;
import seasofyore.ui.TerminalPanel;
import seasofyore.core.Ship;
import java.awt.Graphics;
import java.awt.Point;

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
  public static final String HEARYE = TerminalPanel.GOLD + TerminalPanel.BOLD
      + "*** HEAR YE, HEAR YE! THOU HAST ENTERED THE BATTLE PHASE... ***"
      + TerminalPanel.RESET;
  /**
   * Instructions for firing at the enemy's quadrant.
   */
  public static final String FIREINS = TerminalPanel.GREY + TerminalPanel.ITALIC
      + "If thou wisheth to fire at thine enemy, thou must use the mouse to "
      + "click a cell." + TerminalPanel.RESET;
  /**
   * Message indicating the player has already fired on a cell.
   */
  public static final String FIREDUPE = TerminalPanel.GREY + TerminalPanel.ITALIC
      + "Alas, thou hast already besieged this cell." + TerminalPanel.RESET;
  /**
   * Message indicating a successful hit.
   */
  public static final String FIREHIT = TerminalPanel.RED + TerminalPanel.BOLD
      + "Huzzah, milord! A direct HIT!" + TerminalPanel.RESET;

  /**
   * Message indicating a missed shot.
   */
  public static final String FIREMISS = TerminalPanel.BLUE
      + "Zounds! A decisive MISS..." + TerminalPanel.RESET;

  /**
   * Victory announcement message.
   */
  public static final String VICTORY = TerminalPanel.GOLD + TerminalPanel.BOLD
      + "*** VICTORY, Thou hast vanquished thy foe! ***" + TerminalPanel.RESET;
  /**
   * Prompt to pass the turn to the next player.
   */
  public static final String NTPROMPT = TerminalPanel.GREEN + TerminalPanel.ITALIC
      + "-= Click your flag to pass to next player =-" + TerminalPanel.RESET;

  /**
   * Announcement that a new turn has begun and the player may fire.
   */
  public static final String TURNPROMPT = TerminalPanel.GREEN
      + "'Tis thy turn! Choose a cell in thine enemy's waters."
      + TerminalPanel.RESET;
  
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

    controller.announceBattleStart(); // fanfare, first battle turn only
    controller.logToTerminal( TURNPROMPT );
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

    if ( !controller.useStoneAnimations() )
    {
      // the player turned the show off: the shot lands instantly
      resolveShot( quadrantPanel, x, y );
      return;
    }

    // get position of the cell relative to 'effects' layer dragLayerPanel
    Point global = targeted.getGlobalCellPosition( x, y );
    faller = new FallingStoneAnimation( global, controller.getDragLayerPanel() );
    // animation instantiated, start animation thread ( on EDT )
    faller.startAnimation( () ->
    {
      faller = null; // nullify falling animation instance
      controller.getDragLayerPanel().repaint();
      resolveShot( quadrantPanel, x, y );
    });
  }

  /**
   * Resolves a landed shot: updates the board, logs the outcome, and either
   * ends the game or hands the player the flag to pass the turn.
   *
   * @param quadrantPanel the panel fired upon
   * @param x             the x-coordinate of the struck cell
   * @param y             the y-coordinate of the struck cell
   */
  private void resolveShot( QuadrantPanel quadrantPanel, int x, int y )
  {
    boolean hit = quadrantPanel.fireAtCell( x, y );
    String message = hit ? getHitIdentifier( x, y ) : FIREMISS;
    // log resultant message in terminal
    controller.logToTerminal( message );

    if ( controller.getNextPlayer().hasLost() )
    {
      controller.logToTerminal( VICTORY );
      controller.showWinScreen( controller.getCurrentPlayerCivilization() );
      return;
    }
    controller.logToTerminal( NTPROMPT );
    controller.getNextQuadrantPanel().disableCellInteraction();
    controller.getTerminalPanel().setTurnButtonEnabled( true );
  }

  /**
   * Freezes the stone mid-fall while the game is paused.
   */
  @Override
  public void pause()
  {
    if ( faller != null )
      faller.pause();
  }

  /**
   * Lets a frozen stone keep falling.
   */
  @Override
  public void resume()
  {
    if ( faller != null )
      faller.resume();
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
    if ( faller != null )
    {
      faller.stop(); // never let an in-flight shot resolve after the phase dies
      faller = null;
    }

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
      ? TerminalPanel.RED + TerminalPanel.BOLD + "Thou hast SUNK the enemy's "
        + hitShip.getShipType() + "!" + TerminalPanel.RESET
      : TerminalPanel.RED + "Thou hast HIT the enemy's "
        + hitShip.getShipType() + "!" + TerminalPanel.RESET;
    }
    return FIREMISS;
  }
}

