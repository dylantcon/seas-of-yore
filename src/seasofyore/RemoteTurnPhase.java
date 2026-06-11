/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore;

import seasofyore.ui.FallingStoneAnimation;
import seasofyore.ui.TerminalPanel;
import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

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
   * Every incoming stone still in flight. A single field would lose its
   * grip on an earlier stone when a second arrived, leaving an animation
   * cleanup() could not stop -- and a stone resolving after the turn has
   * switched marks the wrong quadrant and spends the new turn's volley.
   */
  private final List< FallingStoneAnimation > fallers = new ArrayList< >();

  /**
   * Locks the screen down and announces the wait.
   */
  @Override
  protected void onEnter()
  {
    fallers.clear();
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
    FallingStoneAnimation stone =
        new FallingStoneAnimation( global, controller.getDragLayerPanel() );
    fallers.add( stone );
    stone.startAnimation( () ->
    {
      fallers.remove( stone );
      controller.getDragLayerPanel().repaint();
      onLanded.run();
    });
  }

  /**
   * Renders every incoming stone still in flight.
   *
   * @param g the Graphics object used for rendering
   */
  @Override
  public void render( Graphics g )
  {
    for ( FallingStoneAnimation stone : fallers )
      stone.draw( g );
  }

  /**
   * Stops every stone mid-flight if the phase is torn down (by GENDTURN
   * or defeat): a stone that outlived the phase would resolve against
   * the next turn's board state.
   */
  @Override
  public void cleanup()
  {
    for ( FallingStoneAnimation stone : fallers )
      stone.stop();
    fallers.clear();
  }
}
