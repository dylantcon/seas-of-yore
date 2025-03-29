
package seasofyore;

import seasofyore.ui.QuadrantPanel;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 * Represents a phase in the Seas of Yore game.
 * Each phase defines specific behaviors for handling interactions, updates, and 
 * rendering. Examples of phases include ship placement and battle.
 * Implementations of this interface define phase-specific logic for gameplay.
 * 
 * @author dylan connolly
 */
public interface GamePhase 
{
  /**
   * Called when the phase starts.
   * Allows the phase to initialize state and configure the UI.
   * @param controller
   */  
  void enterPhase( GameController controller );
  
  /**
   * Handles a cell click event during this phase.
   *
   * @param x              the x-coordinate of the clicked cell
   * @param y              the y-coordinate of the clicked cell
   * @param quadrantPanel  the QuadrantPanel where the click occurred
   */  
  void handleCellClick( int x, int y, QuadrantPanel quadrantPanel );
  
  
  /**
   * Handles mouse movement events within a QuadrantPanel during this phase.
   *
   * @param dragLayerPoint the point in the drag layer corresponding to the movement
   * @param src            the QuadrantPanel where the movement occurred
   */
  void handleQuadrantMovement( Point dragLayerPoint, QuadrantPanel src );


  /**
   * Handles user input during this phase.
   * @param e 
   */
  void handleInput( MouseEvent e );
  
  /**
   * Updates the state of the phase (e.g., animations, timers).
   */
  void update();

  /**
   * Renders any custom visuals for the phase (optional).
   * @param g
   */
  void render( Graphics g );
  
  /**
   * Cleans up resources or resets configurations when the phase ends.
   * This is a default implementation that does nothing but can be overridden by specific phases.
   */
  default void cleanup()
  {
    
  }
}
