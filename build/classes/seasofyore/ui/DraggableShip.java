/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore.ui;

import seasofyore.core.Ship;
import seasofyore.core.ShipHeading;
import seasofyore.core.Direction;
import seasofyore.core.PlayerQuadrant;
import seasofyore.core.ShipType;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import javax.swing.ImageIcon;

/**
 * Represents a ship that can be dragged and placed on the game board during 
 * the ship placement phase. This class manages the ship's position, direction,
 * sprite, and validation logic for placement.
 * 
 * @author dylan connolly
 * 
 */
public class DraggableShip 
{
  /**
   * The type of the ship being dragged.
   */
  private ShipType type;
  
 /**
  * The direction the ship is currently facing.
  */
  private Direction direction;
  
  /**
   * The x-coordinate of the mouse pointer.
   */
  private int mouseX; 

  /**
   * The y-coordinate of the mouse pointer.
   */
  private int mouseY;
  
  /**
   * The x-coordinate of the ship's position in the grid.
   */
  private int gridX;
  
  /**
   * The y-coordinate of the ship's position in the grid.
   */
  private int gridY;
  
  /**
   * The sprite representing the ship.
   */
  private ImageIcon sprite;
  
  /**
   * The QuadrantPanel associated with this draggable ship.
   */
  private final QuadrantPanel qp;

  /**
   * The color indicating a valid ship placement.
   */
  private static final Color VALID_COL = new Color( 0, 255, 0, 127 );
  
  /**
   * The color indicating an invalid ship placement.
   */
  private static final Color INVALID_COL = new Color( 255, 0, 0, 127 );
  
  
  /**
   * Constructs a new DraggableShip.
   *
   * @param qp   the QuadrantPanel associated with this ship
   * @param type the type of the ship
   * @param dir  the initial direction of the ship
   */
  public DraggableShip( QuadrantPanel qp, ShipType type, Direction dir )
  {
    this.type = type;
    this.direction = dir;
    this.qp = qp;
  }
  
  
  /**
   * Updates the ship's position based on a global point and cell dimensions.
   *
   * @param global the global point representing the mouse position
   * @param cW     the width of a cell
   * @param cH     the height of a cell
   */
  public void updatePosition( Point global, int cW, int cH )
  {
    int[] gridPosition = qp.getGridFromGlobal( global );
    
    this.mouseX = global.x;
    this.mouseY = global.y;
    this.gridX = gridPosition[0];
    this.gridY = gridPosition[1];
  }
  
  /**
   * Gets the current direction of the ship.
   *
   * @return the current direction
   */
  public Direction getDirection()
  {
    return this.direction;
  }
  

  /**
   * Sets the direction of the ship.
   *
   * @param dir the new direction
   */
  public void setDirection( Direction dir )
  {
    this.direction = dir;
  }   
  
  /**
   * Rotates the ship clockwise.
   */
  public void rotateClockwise()
  {
    direction = direction.next();
    this.updateSprite();
  }

  /**
   * Rotates the ship counterclockwise.
   */
  public void rotateCounterClockwise()
  {
    direction = direction.previous();
    this.updateSprite();
  }
  
  /**
   * Sets the type of the ship.
   *
   * @param type the new ship type
   */
  public void setType( ShipType type )
  {
    this.type = type;
  }
  
  /**
   * Gets the type of the ship.
   *
   * @return the current ship type
   */
  public ShipType getType()
  {
    return this.type;
  }
  
  /**
   * Gets the heading (position and direction) of the ship.
   *
   * @return the ship's heading
   */
  public ShipHeading getHeading()
  {
    return new ShipHeading( gridX, gridY, direction );
  }

  /**
   * Creates a new Ship object of the current type.
   *
   * @return a new Ship instance
   */
  public Ship getShip()
  {
    return new Ship( type );
  }
  
  /**
   * Gets the length of the ship based on its type.
   *
   * @return the length of the ship
   */
  public int getShipLength()
  {
    return Ship.getAShipLength( type );
  }
  
  /**
   * Validates whether the ship can be placed at its current position.
   *
   * @return true if the ship placement is valid; false otherwise
   */
  private boolean validateLocation()
  {
    PlayerQuadrant q = qp.getOwnerQuadrant();
    return q.validHeading( getShip(), getHeading() );
  }
 
  /**
   * Updates the sprite image for the ship based on its type and direction.
   */
  private void updateSprite()
  {
    String spritePath = String.format( "/images/%s-%s-%s.png", qp.getOwnerCiv(), 
                                       type, direction );
    
    sprite = new ImageIcon( getClass().getResource( spritePath ) );
    if ( sprite == null ) 

      System.err.println( "Failed to load sprite: " + spritePath );
  }
  

  /**
   * Calculates the rendering position of the ship based on the mouse offset and 
   * grid coordinates.
   *
   * @param cellW the width of a cell
   * @param cellH the height of a cell
   * @return an array containing the x and y coordinates for rendering
   */
  private int[] getRenderXY( int cellW, int cellH )
  {
    int offsetX = ( mouseX - ( gridX * cellW ) ) - ( cellW / 2 );
    int offsetY = ( mouseY - ( gridY * cellH ) ) - ( cellH / 2 );

    int[] dim = getShipBounds( cellW, cellH );
    return new int[] { dim[0] + offsetX, dim[1] + offsetY };
  }
  

  /**
   * Gets the rendering bounds of the ship based on its heading and dimensions.
   *
   * @param cellW the width of a cell
   * @param cellH the height of a cell
   * @return an array containing x, y, width, and height of the ship's bounds
   */
  private int[] getShipBounds( int cellW, int cellH )
  {
    return qp.calculateShipBounds( getHeading(), cellW, cellH, getShipLength() );
  }
  
  /**
   * Draws the ship on the specified graphics context, including its sprite and 
   * placement validity overlay. In our case, this is on dragLayerPanel.
   *
   * @param g          the graphics context
   * @param cellWidth  the width of a cell
   * @param cellHeight the height of a cell
   */
  public void draw( Graphics g, int cellWidth, int cellHeight )
  {
    updateSprite();
    
    int[] dim = getShipBounds( cellWidth, cellHeight );
    int[] renderXY = getRenderXY( cellWidth, cellHeight );

    boolean isValid = validateLocation();
    Color placementColor = isValid ? VALID_COL : INVALID_COL;
    
    g.setColor( placementColor );
    g.fillRect( renderXY[0], renderXY[1], dim[2], dim[3] );
    
    g.drawImage( sprite.getImage(), renderXY[0], renderXY[1], dim[2], dim[3], null );
  }
}
