package seasofyore.core;

/** 
 * Represents the cardinal directions (NORTH, EAST, SOUTH, WEST) used in the game.
 * @author dylan
 * 
 * Each direction includes x and y offsets for movement, as well as utility methods
 * for navigating and converting between directions and offsets.
 */
public enum Direction 
{ 
  /**
   * North direction, moving upward with offsets (0, -1).
   */
  NORTH( 0, -1 ),
  
  /**
   * East direction, moving rightward with offsets (1, 0).
   */
  EAST( 1, 0 ),
  
  /**
   * South direction, moving downward with offsets (0, 1).
   */
  SOUTH( 0, 1 ),
  
  /**
   * West direction, moving leftward with offsets (-1, 0).
   */
  WEST( -1, 0 );
  
  /**
   * The horizontal offset for the direction.
   */
  private final int xOffset;
  
  /**
   * The vertical offset for the direction.
   */
  private final int yOffset;
  
  /**
   * Pre-computed array of all direction values.
   */  
  private static final Direction[] VALUES = values();
  
  /**
   * Public constant for the x-index.
   */  
  public static final int XIND = 0;
  
  /**
   * Public constant for the y-index.
   */
  public static final int YIND = 1;
  
  
  /**
   * Constructs a Direction with the specified x and y offsets.
   *
   * @param xO the horizontal offset
   * @param yO the vertical offset
   */
  Direction( int xO, int yO )
  {
    this.xOffset = xO;
    this.yOffset = yO;
  }
  
  /**
   * Gets the horizontal offset for this direction.
   *
   * @return the x offset
   */
  public int getXOffset()
  {
    return this.xOffset;
  }
  
  /**
   * Gets the vertical offset for this direction.
   *
   * @return the y offset
   */
  public int getYOffset()
  {
    return this.yOffset;
  }
  
  /**
   * Gets the next direction in clockwise order.
   *
   * @return the next direction
   */
  public Direction next()
  {
    return VALUES[ ( this.ordinal() + 1 ) % VALUES.length ];
  }
  

  /**
   * Gets the previous direction in counterclockwise order.
   *
   * @return the previous direction
   */
  public Direction previous()
  {
    return VALUES[ ( this.ordinal() - 1 + VALUES.length ) % VALUES.length ];
  }
  
  /**
   * Gets the x and y offsets for this direction as an array.
   *
   * @return an array containing the x and y offsets
   */
  public int[] getOffsets()
  {
    return new int[] { xOffset, yOffset };
  }
  
  /**
   * Gets the row and column offsets for this direction as an array.
   * Row corresponds to the y offset, and column corresponds to the x offset.
   *
   * @return an array containing the row and column offsets
   */
  public int[] getRowColOffsets()
  {
    return new int[] { yOffset, xOffset };
  }
  
  /**
   * Gets the Direction corresponding to the specified x and y offsets.
   *
   * @param dx the horizontal offset
   * @param dy the vertical offset
   * @return the Direction matching the specified offsets
   * @throws IllegalArgumentException if no direction matches the offsets
   */
  public static Direction fromOffsets( int dx, int dy )
  {
    for ( Direction dir : VALUES )
      if ( dir.xOffset == dx && dir.yOffset == dy )
        return dir;
    throw new IllegalArgumentException( "No direction for offsets: "
                                        + dx + ", " + dy );
  }
  
  public static Direction getRandom()
  {
    int randomIndex = (int)( Math.random() * VALUES.length );
    return VALUES[ randomIndex ];
  }
}
