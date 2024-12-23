package seasofyore;

import seasofyore.ui.SidebarPanel;
import seasofyore.ui.QuadrantPanel;
import seasofyore.ui.DraggableShip;
import seasofyore.core.Direction;
import seasofyore.core.Civilization;
import seasofyore.core.ShipType;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Represents the ship placement phase in the Seas of Yore game.
 * This phase allows players to place their ships on their quadrant, configure 
 * their positions, and prepare for the battle phase.
 * 
 * @author dylan connolly
 * 
 *
 */
public class ShipPlacementPhase extends AbstractGamePhase
{
  /**
   * The currently held drag-able ship for placement.
   */
  private DraggableShip draggableShip;
  
  /**
   * The sidebar panel providing the ship selection menu.
   */  
  private SidebarPanel menu;
  
  /**
   * Instruction displayed when no ship is held.
   */
  private final String noShipIns = "No ship held. Click the anchor button to open the"
                              + " menu, then click the ship you'd like to pick up.";
  
  /**
   * Instruction displayed when the fleet is fully placed.
   */
  private final String doneIns = "Fleet placed! End turn by clicking the flag "
                              + "button, which is now illuminated.";
  
  /**
   * Instruction displayed when attempting to place ships on the wrong quadrant.
   */  
  private final String badQIns = "You can only place ships on your own quadrant!";
  
  /**
   * Introduction message for the ship placement phase.
   */  
  private final String intro = "Use the ship menu to pick up ships. To rotate your"
                              + " placement, use the scroll wheel.";
  
  /**
   * Initializes the phase state and UI when entered.
   * Enables interaction on the current player's quadrant and disables interaction 
   * on the enemy's quadrant. Logs introductory instructions to the terminal.
   */
  @Override
  protected void onEnter() // onEnter assumes state and UI represent new player
  {
    draggableShip = null;
    menu = controller.getSidebarPanel();
    controller.applyNewSidebar();
    controller.addToolbar();
    
    controller.getBoardPanel().getFriendlyPanel().enableCellInteraction();
    controller.getBoardPanel().getEnemyPanel().disableCellInteraction();
    
    Civilization c = controller.getCurrentPlayerCivilization();
    controller.logToTerminal( c + ", place your ships on the board." );
    controller.logToTerminal( intro );
  }
  
  /**
   * Handles a cell click event during the ship placement phase.
   * Checks whether a ship can be placed and updates the board and UI accordingly.
   *
   * @param x             the x-coordinate of the clicked cell
   * @param y             the y-coordinate of the clicked cell
   * @param quadrantPanel the QuadrantPanel where the click occurred
   */
  @Override
  public void handleCellClick( int x, int y, QuadrantPanel quadrantPanel )
  {
    if ( !quadrantPanel.isFriendly() && draggableShip != null )
    {
      controller.logToTerminal( badQIns );
      return;
    }
    
    if ( draggableShip == null )
    {
      controller.logToTerminal( noShipIns );
      return;
    }
    
    if ( !controller.getBoard().hasCurrentFinishedSetup() )
    {
      Direction direction = draggableShip.getDirection();
      ShipType held = menu.getSelectedShip();

      if ( !quadrantPanel.placeShip( held, x, y, direction ) )
        controller.logToTerminal( "Can't place " + held + ", try elsewhere." );
      
      else
      {
        controller.logToTerminal( held + " placed successfully!" );
        menu.setSlotEnabled( held, false );
        observePlacementState();
      }
    }
  }

  /**
   * Handles mouse movement within a QuadrantPanel during this phase.
   * Updates the position of the draggable ship based on the movement.
   *
   * @param dragPt the current drag point
   * @param src    the QuadrantPanel where the movement occurred
   */
  @Override
  public void handleQuadrantMovement( Point dragPt, QuadrantPanel src )
  {
    if ( draggableShip != null )
    {
      int[] cellWH = src.getCellDimensions();
      draggableShip.updatePosition( dragPt, cellWH[0], cellWH[1] );
      controller.getDragLayerPanel().repaint();
    }
  }
  
  /**
   * Handles user input during the ship placement phase.
   * Supports ship rotation using the scroll wheel and picking up ships from the sidebar.
   *
   * @param e the MouseEvent representing the user input
   */
  @Override
  public void handleInput( MouseEvent e )
  {
    Component source = e.getComponent(); // the source component (e.g., cellPanel or etc.)
    JPanel dragPanel = controller.getDragLayerPanel();
    QuadrantPanel activeQP = controller.getCurrentQuadrantPanel();

    // map event to dragPanel's coordinate space for consistent rendering
    Point globalPt = SwingUtilities.convertPoint( source, e.getPoint(), dragPanel );
    
    if ( e.getID() == MouseEvent.MOUSE_MOVED && draggableShip != null ) 
    {
      int[] cellWH = activeQP.getCellDimensions(); 
      draggableShip.updatePosition( globalPt, cellWH[0], cellWH[1] );
      // repaint the drag layer to reflect changes
      controller.getDragLayerPanel().repaint();
    }

    if ( e.getID() == MouseEvent.MOUSE_WHEEL && draggableShip != null ) 
    {
      int rotation = ( (MouseWheelEvent) e ).getWheelRotation();
      if ( rotation < 0 ) 
        draggableShip.rotateCounterClockwise();
      else if ( rotation > 0 ) 
        draggableShip.rotateClockwise();
      controller.getDragLayerPanel().repaint();
    }

    if ( e.getID() == MouseEvent.MOUSE_CLICKED ) 
    { 
      // Handle picking up a ship from the sidebar ( source sidebarPanel )
      Point menuPt = getSlotContainedPoint( e.getPoint() );
      ShipType clkShip = menu.getShipAtPoint( menuPt );
      
      if ( clkShip != null && menu.isSlotEnabled( clkShip ) ) 
      {
        draggableShip = instantiateDraggableShip( clkShip );
        menu.setSelectedShip( clkShip );
        controller.logToTerminal( "You've selected the " + clkShip );
        // TODO: update position upon instantiating preview ship ( currently
        //          looks shitty and appears at 0,0 within gamePanel )
      }
      else if ( clkShip == null )
        controller.logToTerminal( noShipIns );
    }
  }
  /**
   * Updates the phase state, transitioning to the battle phase if placement is complete.
   */
  @Override
  public void update()
  {
    if ( controller.getBoard().isPlacementFinal() )
      controller.setPhase( new BattlePhase() );
  }
  
  /**
   * Renders the draggable ship's visuals during the phase.
   *
   * @param g the Graphics object used for rendering
   */
  @Override
  public void render( Graphics g )
  {
    if ( draggableShip != null )
    {
      int[] cellWH = controller.getCurrentQuadrantPanel().getCellDimensions();
      draggableShip.draw( g, cellWH[0], cellWH[1] );
    }
  }

  /**
   * Cleans up the phase state when it ends.
   * Resets the sidebar and disables quadrant interaction.
   */
  @Override
  public void cleanup()
  {
    draggableShip = null;
    menu.nullSelectedShip();
    controller.getBoardPanel().getFriendlyPanel().disableCellInteraction();
    controller.getTerminalPanel().setTurnButtonEnabled( false );
    
    if ( controller.getCurrentPlayer().hasPlacedAllShips() )
      controller.removeToolbar();
    
    if ( controller.getBoard().isPlacementFinal() )
      menu.allSlotsEnabled( false );
  }

  /**
   * Converts a point to the coordinate space of the ship slot container.
   *
   * @param p the point in the original coordinate space
   * @return the converted point in the ship slot container's space
   */
  private Point getSlotContainedPoint( Point p )
  {
    return SwingUtilities.convertPoint( getSidebarPanel(), p,
                                        menu.getSlotContainer() );
  }

  /**
   * Gets the main game panel from the GameController.
   *
   * @return the main game panel
   */
  private SidebarPanel getSidebarPanel()
  {
    return controller.getSidebarPanel();
  }

  /**
   * Instantiates a new DraggableShip for the specified ship type.
   * Defaults the ship's direction to EAST.
   *
   * @param type the type of the ship to instantiate
   * @return the created DraggableShip
   */
  private DraggableShip instantiateDraggableShip( ShipType type )
  {
    return new DraggableShip( controller.getCurrentQuadrantPanel(), type, 
                              Direction.EAST );
  }
  
  /**
   * Handles all ships placed, nullifies any prior state for the current ship. 
   * 
   * If the current player has placed all of their ships, the slots in the 
   * sidebar are disabled, a prompt is sent to the terminal, and the end turn
   * button is enabled to allow interaction.
   */
  private void observePlacementState()
  {
    menu.nullSelectedShip();
    draggableShip = null;
    
    if ( controller.getBoard().hasCurrentFinishedSetup() )
    {
      menu.allSlotsEnabled( false );
      controller.logToTerminal( doneIns );
      controller.logToTerminal( BattlePhase.NTPROMPT );
      controller.getTerminalPanel().setTurnButtonEnabled( true );
    }
    controller.repaint();
  }
}
