/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore.core;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the heading (position and direction) of a ship on the game board.
 * It calculates the occupied cells of the ship based on its position and 
 * direction.
 * 
 * @author dylan connolly
 *
 */
public class ShipHeading 
{
  /**
   * Index representing the x-coordinate.
   */
  public static final int XIND = 0;
  
  /**
   * Index representing the y-coordinate.
   */
  public static final int YIND = 1;
  
  /**
   * The x-coordinate of the rear of the ship.
   */
  private int xPos;
  
  /**
   * The y-coordinate of the rear of the ship.
   */
  private int yPos;
  
  /**
   * The direction the ship is facing.
   */
  private Direction direction;
 
  /**
   * Constructs a new ShipHeading with the specified position and direction.
   *
   * @param x the x-coordinate of the rear of the ship
   * @param y the y-coordinate of the rear of the ship
   * @param d the direction the ship is facing
   */
  public ShipHeading( int x, int y, Direction d )
  {
    this.xPos = x;
    this.yPos = y;
    this.direction = d;
  }
  
  /**
   * Creates a new instance of ShipHeading with the specified position and direction.
   *
   * @param x the x-coordinate of the rear of the ship
   * @param y the y-coordinate of the rear of the ship
   * @param d the direction the ship is facing
   * @return a new instance of ShipHeading
   */
  public static ShipHeading getInstance( int x, int y, Direction d )
  {
    return new ShipHeading( x, y, d );
  }
  
  /**
   * Gets the rear position of the ship as an array of coordinates.
   *
   * @return an array where index 0 is the x-coordinate and index 1 is the y-coordinate
   */
  public int[] getRear()
  {
    return new int[] { xPos, yPos };
  }
  
  /**
   * Gets the direction the ship is facing.
   *
   * @return the direction of the ship
   */
  public Direction getDirection()
  {
    return this.direction;
  }
  
  /**
   * Sets the direction the ship is facing.
   *
   * @param d the new direction of the ship
   */
  public void setDirection( Direction d )
  {
    this.direction = d;
  }
  
  /**
   * Sets the rear position of the ship.
   *
   * @param x the new x-coordinate of the rear
   * @param y the new y-coordinate of the rear
   */
  public void setRear( int x, int y )
  {
    this.xPos = x;
    this.yPos = y;
  }
  
  /**
   * Calculates the cells occupied by the ship based on its length, starting from the rear.
   *
   * @param shipLength the length of the ship
   * @return a list of integer arrays, where each array contains the x and y coordinates of an occupied cell
   */
  public List< int[] > getOccupiedCells( int shipLength )
  {
    List< int[] > cells = new ArrayList< >();
    int dx = direction.getXOffset();
    int dy = direction.getYOffset();
    
    for ( int i = 0; i < shipLength; i++ )
      cells.add( new int[] { xPos + i * dx, yPos + i * dy } );
    
    return cells;
  }
  
  /**
   * Adjusts the rear position of the ship by the specified offsets.
   *
   * @param dx the change in the x-coordinate
   * @param dy the change in the y-coordinate
   */  
  public void adjustRear( int dx, int dy )
  {
    this.xPos += dx;
    this.yPos += dy;
  }
}
