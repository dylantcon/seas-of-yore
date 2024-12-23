package seasofyore;
import seasofyore.core.Player;
import seasofyore.core.PlayerQuadrant;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;

/**
 * Represents an individual cell in the game board grid.
 * Handles rendering, mouse interaction, and cell state management for
 * gameplay.
 * 
 * @author dylan
 */
public class CellPanel extends JPanel
{
  /**
   * The x-coordinate of this cell in the grid.
   */
  private final int x;
  /**
   * The y-coordinate of this cell in the grid.
   */
  private final int y;
 
  /**
   * The quadrant to which this cell belongs.
   */
  private final PlayerQuadrant quadrant;
  
  /**
   * The parent panel containing this cell.
   */
  private final QuadrantPanel quadPanel;
  
   /**
   * Indicates whether this cell can currently interact with mouse events.
   */
  private boolean canInteract;
  
  /**
   * Indicates whether this cell is locked for SALVO mode.
   */
  private boolean salvoLock = false;

  /**
   * The color used to render fog over a cell.
   */
  private static final Color FOG_COLOR = new Color( 0, 0, 0, 127 );
  
  /**
   * Constructs a new CellPanel.
   *
   * @param x  the x-coordinate of the cell in the grid
   * @param y  the y-coordinate of the cell in the grid
   * @param qP the parent QuadrantPanel containing this cell
   */
  public CellPanel( int x, int y, QuadrantPanel qP )
  {
    this.x = x;
    this.y = y;
    this.quadPanel = qP;
    this.quadrant = quadPanel.getOwnerQuadrant();
    this.canInteract = true;
    this.initMouseListener();
    
    this.setOpaque( false );
    this.setBorder( getPassiveBorder() );
  }
  

  /**
   * Initializes mouse listeners for interaction with this cell.
   */
  private void initMouseListener()
  {
    MouseAdapter customAdapter = ( new MouseAdapter()
    {
      @Override
      public void mouseClicked( MouseEvent e )
      {
        if ( canInteract || !quadPanel.isFinalPlacementState() )
          handleClick();
      }
      
      @Override
      public void mouseEntered( MouseEvent e )
      {
        if ( canInteract && !salvoLock )
          setBorder( getFocusBorder() );
      }
      
      @Override
      public void mouseExited( MouseEvent e )
      {
        if ( canInteract && !salvoLock )
          setBorder( getPassiveBorder() );
      }
      
      @Override
      public void mouseMoved( MouseEvent e )
      {
        handleMouseMove( e );
      }
    });
    
    this.addMouseListener( customAdapter );
    this.addMouseMotionListener( customAdapter );
  }
 
  /**
   * Gets the state of the cell as defined by the quadrant.
   *
   * @return the state of the cell
   */
  public int getCellState()
  {
    return quadrant.getCellType( x, y );
  }
  
  /**
   * Gets the owner of this cell.
   *
   * @return the player who owns this cell
   */
  public Player getCellOwner()
  {
    return quadPanel.getOwner();
  }
  
  /**
   * Checks if the owner of this cell is the friendly player.
   *
   * @return true if the cell belongs to the friendly player; false otherwise
   */
  public boolean isCellOwnerFriendly()
  {
    return quadPanel.isFriendly();
  }
  
  /**
   * Gets the x-coordinate of this cell in the grid.
   *
   * @return the x-coordinate
   */
  public int getGridX()
  {
    return this.x;
  }
  
  /**
   * Gets the y-coordinate of this cell in the grid.
   *
   * @return the y-coordinate
   */
  public int getGridY()
  {
    return this.x;
  }
  
  /**
   * Checks if the cell has been hit.
   *
   * @return true if the cell is marked as hit; false otherwise
   */
  public boolean isHit()
  {
    return quadrant.cellIsHit( x, y );
  }
  
  /**
   * Checks if the cell has been missed.
   *
   * @return true if the cell is marked as missed; false otherwise
   */
  public boolean isMissed()
  {
    return quadrant.cellIsMiss( x, y );
  }
  
  /**
   * Checks if the cell can currently be fired upon.
   *
   * @return true if the cell can be fired upon; false otherwise
   */
  public boolean canBeFiredUpon()
  {
    return !isHit() && !isMissed() && canInteract;
  }
  
  /**
   * Sets whether this cell can interact with mouse events.
   *
   * @param interactable true to enable interaction; false to disable it
   */
  public void setInteractable( boolean interactable )
  {
    this.canInteract = interactable;
    this.repaint();
  }
  
  /**
   * Enables interaction with this cell.
   */
  public void enableInteraction()
  {
    setInteractable( true );
  }
  
  /**
   * Disables interaction with this cell.
   * Resets the border to its passive state.
   */
  public void disableInteraction()
  {
    setInteractable( false );
    setBorder( getPassiveBorder() );
  }
  
  /**
   * Handles a mouse click event on this cell.
   */
  private void handleClick()
  {
    quadPanel.handleCellClick( x, y );
    repaint();
  }
  
  /**
   * Handles mouse movement over this cell.
   *
   * @param e the MouseEvent representing the movement
   */
  private void handleMouseMove( MouseEvent e ) 
  {
    quadPanel.handleCellMouseMove( x, y, e );
  }

  /**
   * Gets the unique key for this cell.
   *
   * @return a string key representing the cell's coordinates
   */
  protected String getKey()
  {
    return makeKey( x, y );
  }
  
  /**
   * Creates a unique key for the specified cell coordinates.
   *
   * @param xPos the x-coordinate of the cell
   * @param yPos the y-coordinate of the cell
   * @return a string key representing the coordinates
   */
  public static String makeKey( int xPos, int yPos )
  {
    return String.format( "%d,%d", xPos, yPos );
  }
  
  /**
   * Gets the passive border for the cell.
   *
   * @return the passive border
   */
  private Border getPassiveBorder()
  {
    return BorderFactory.createLineBorder( Color.GRAY, 1 );
  }
  
  /**
   * Gets the border for when the cell is focused.
   *
   * @return the focus border
   */
  private Border getFocusBorder()
  {
    return BorderFactory.createLineBorder( Color.YELLOW, 2 );
  }
  
  /**
   * Locks the cell for SALVO mode, applying the SALVO border.
   */
  private Border getSALVOBorder()
  {
    return BorderFactory.createLineBorder( Color.RED, 2 );
  }
  
  /**
   * Locks the cell for SALVO mode, applying the SALVO border.
   */
  public void lockForSALVO()
  {
    setBorder( getSALVOBorder() );
    salvoLock = true;
  }
  
  /**
   * Unlocks the cell from SALVO mode, resetting to the passive border.
   */
  public void unlockForSALVO()
  {
    setBorder( getPassiveBorder() );
    salvoLock = false;
  }
  
  /**
   * Renders the cell, applying appropriate visuals based on its state (hit, miss, or fog).
   *
   * @param g the Graphics object used for rendering
   */
  @Override
  public void paintComponent( Graphics g )
  {
    super.paintComponent( g );
    
    switch ( quadrant.getCellType( x, y ) )
    {
      case PlayerQuadrant.HIT_CELL -> renderFiredCell( g, Color.RED );
      case PlayerQuadrant.MISS_CELL -> renderFiredCell( g, Color.BLUE );
      default -> renderFog( g );
    }
  }
  
  /**
   * Renders the visual for a fired-upon cell.
   *
   * @param g the Graphics object used for rendering
   * @param c the color representing the fired state
   */
  private void renderFiredCell( Graphics g, Color c )
  {
    g.setColor( new Color( c.getRed(), c.getGreen(), c.getBlue(), 100 ) );
    g.fillRect( 0, 0, this.getWidth(), this.getHeight() );
  }
 
  /**
   * Renders the fog effect over the cell.
   *
   * @param g the Graphics object used for rendering
   */
  private void renderFog( Graphics g )
  {
    g.setColor( FOG_COLOR );
    g.fillRect( 0, 0, this.getWidth(), this.getHeight() );
  }
}
