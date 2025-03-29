
package seasofyore.core;

/**
 * Represents the different types of ships available in the Seas of Yore game.
 * Each ship type has a specific length and description.
 */
public enum ShipType 
{
  /**
   * A small, fast ship with a length of 2.
   */
  CRAYER( 2, "A small, fast ship" ),

  /**
   * A versatile mid-sized ship with a length of 3.
   */
  HOY( 3, "A versatile mid-sized ship" ),

  /**
   * A powerful rowed vessel with a length of 3.
   */
  GALLEY( 3, "A powerful rowed vessel" ),

  /**
   * A large, sturdy merchant ship with a length of 4.
   */
  COG( 4, "A large, sturdy merchant ship" ),

  /**
   * A heavily armed warship with a length of 5.
   */
  GALLEON( 5, "A heavily armed warship" );

  /**
   * Cached array of all ship types, used for iteration.
   */
  private static final ShipType[] VALUES = values();

  /**
   * The length of the ship type.
   */
  private final int length;

  /**
   * A description of the ship type.
   */
  private final String description;

  /**
   * Constructs a new ShipType with the specified length and description.
   * 
   * @param length      the length of the ship type
   * @param description the description of the ship type
   */
  ShipType( int length, String description ) 
  {
    this.length = length;
    this.description = description;
  }

  /**
   * Gets the length of the ship type.
   * 
   * @return the length of the ship type
   */
  public int getLength() 
  {
    return this.length;
  }

  /**
   * Gets the description of the ship type.
   * 
   * @return the description of the ship type
   */
  public String getDescription() 
  {
      return this.description;
  }

  /**
   * Gets the next ship type in the enumeration order.
   * 
   * @return the next ship type
   */
  public ShipType next() 
  {
    return VALUES[ ( this.ordinal() + 1 ) % VALUES.length ];
  }

  /**
   * Gets the previous ship type in the enumeration order.
   * 
   * @return the previous ship type
   */
  public ShipType previous() 
  {
    return VALUES[ ( this.ordinal() - 1 + VALUES.length ) % VALUES.length ];
  }

  /**
   * Gets an array of all ship types in ascending order.
   * 
   * @return an array of all ship types
   */
  public static ShipType[] getAscendingList() 
  {
    return VALUES.clone();
  }
}