/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore;

import seasofyore.ui.FallingStoneAnimation;
import seasofyore.ui.TerminalPanel;
import java.awt.Graphics;
import java.awt.Point;

/**
 * The remote opponent's turn, as seen from this screen: a passive phase.
 * All input is disabled and nothing here decides anything -- the enemy's
 * shots arrive over the wire, the match handler resolves them against the
 * local fleet, and the handler advances the turn when GENDTURN arrives.
 * The phase's one active duty is theatrical: when the handler asks, it
 * flies the incoming stone over the local waters so the enemy's volley
 * looks like a volley.
 *
 * @author dylan
 */
public class RemoteTurnPhase extends AbstractGamePhase
{
  /**
   * The incoming stone in flight, if the show is on.
   */
  private FallingStoneAnimation faller;

  /**
   * Locks the screen down and announces the wait.
   */
  @Override
  protected void onEnter()
  {
    faller = null;
    controller.getCurrentQuadrantPanel().disableCellInteraction();
    controller.getNextQuadrantPanel().disableCellInteraction();
    controller.getTerminalPanel().setTurnButtonEnabled( false );

    controller.announceBattleStart(); // fanfare, first battle turn only
    controller.logToTerminal( TerminalPanel.GREY + TerminalPanel.ITALIC
        + controller.getCurrentPlayer().getTitledName()
        + " takes aim from across the water..." + TerminalPanel.RESET );
  }

  /**
   * Flies an enemy stone onto the local quadrant, resolving the shot when
   * it lands. Called by the networked match handler when a GSHOT arrives
   * and the stone show is enabled.
   *
   * @param x        the target x-coordinate on the local quadrant
   * @param y        the target y-coordinate on the local quadrant
   * @param onLanded the shot resolution to run at touchdown
   */
  public void animateIncoming( int x, int y, Runnable onLanded )
  {
    // during the enemy's turn, the local fleet is the "next" player's
    Point global = controller.getNextQuadrantPanel().getGlobalCellPosition( x, y );
    faller = new FallingStoneAnimation( global, controller.getDragLayerPanel() );
    faller.startAnimation( () ->
    {
      faller = null;
      controller.getDragLayerPanel().repaint();
      onLanded.run();
    });
  }

  /**
   * Renders the incoming stone.
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
   * Stops a stone mid-flight if the phase is torn down (e.g. by defeat).
   */
  @Override
  public void cleanup()
  {
    if ( faller != null )
    {
      faller.stop();
      faller = null;
    }
  }
}
