package seasofyore.core;

import java.util.List;

/**
 * Represents a player in the Seas of Yore game, managing their fleet,
 * civilization, and game quadrants.
 * 
 * 
 * @author dylan
 * 
 */
public class Player 
{
  /**
   * The fixed size of the player's fleet.
   */
  public static final int FLEET_SIZE = 5;

  /**
   * The player's fleet of ships.
   */
  private Ship[] fleet;

  /**
   * The locations of the player's ships.
   */
  private ShipHeading[] locations;

  /**
   * The number of ships placed on the player's board.
   */
  private int placedShips = 0;

  /**
   * The civilization the player represents.
   */
  private final Civilization civ;

  /**
   * The player's quadrant representing friendly territory.
   */
  private final PlayerQuadrant friendlyQuad;

  /**
   * The player's quadrant representing enemy territory.
   */
  private final PlayerQuadrant enemyQuad;

  /**
   * Constructs a new Player with the specified civilization and quadrants.
   * 
   * @param thisCiv the civilization of the player
   * @param fQ      the player's friendly quadrant
   * @param eQ      the player's enemy quadrant
   */   
  public Player( Civilization thisCiv, PlayerQuadrant fQ, PlayerQuadrant eQ )
  {
    this.civ = thisCiv;
    this.enemyQuad = eQ;
    this.friendlyQuad = fQ;
    this.fleet = new Ship[FLEET_SIZE];
    this.locations = new ShipHeading[FLEET_SIZE];                
  }
  
  /**
   * Gets the player's friendly quadrant.
   * 
   * @return the friendly quadrant
   */
  public PlayerQuadrant getFriendlyQuad()
  {
    return this.friendlyQuad;
  }
  
  /**
   * Gets the player's enemy quadrant.
   * 
   * @return the enemy quadrant
   */
  public PlayerQuadrant getEnemyQuad()
  {
    return this.enemyQuad;
  }
  
  /**
   * Gets the player's civilization.
   * 
   * @return the calling object's civilization
   */
  public Civilization getCiv()
  {
    return this.civ;
  }
  
  /**
   * Gets the player's fleet of ships.
   * 
   * @return an array of Ships
   */
  public Ship[] getFleet()
  {
    return this.fleet;
  }
  
  /**
   * Gets the locations of the player's ships.
   * 
   * @return an array of ShipHeadings
   */
  public ShipHeading[] getShipLocations()
  {
    return this.locations;
  }

  /**
   * Places a vessel on the player's board.
   * 
   * @param vessel  the ship to place
   * @param heading the heading of the ship
   * @return true if the placement was successful; false otherwise
   */
  public boolean placeVessel( Ship vessel, ShipHeading heading )
  {
    boolean placementSuccessful = friendlyQuad.placeShip( vessel, heading );
    if ( placementSuccessful )
    {
      fleet[ placedShips ] = vessel;
      locations[ placedShips++ ] = heading;
      return true;
    }
    return false;
  }
  
  /**
   * Randomizes the calling object's (Player) ship placement, placing all ships.
   */
  public void randomVesselPlacement()
  {
    Ship[] ships = Ship.getListInstance();      // get a ship array for fleet
    reset();
    friendlyQuad.eraseCells();
    
    // while we haven't placed all our ships, attempt placing current ship with
    //  a randomly generated ship heading. stop when all ships have been placed
    while ( placedShips != 5 )
      placeVessel( ships[placedShips], ShipHeading.getRandomInstance() );
  }
  
  /**
   * Resets all placement related variables to their uninitialized state.
   */
  public void reset()
  {
    fleet = new Ship[ FLEET_SIZE ];             // re-initialize fleet
    locations = new ShipHeading[ FLEET_SIZE ];  // re-initialize locations
    placedShips = 0;                            // set placedShips to 0
    friendlyQuad.eraseCells();                  // reset friendly quadrant
  }
  
  /**
   * Fires at a specific cell in the enemy quadrant.
   * 
   * @param x the x-coordinate of the target cell
   * @param y the y-coordinate of the target cell
   * @return true if cell was target-able and the attack was executed; false otherwise
   */
  public boolean fireAtEnemyCell( int x, int y )
  {
    if ( !enemyQuad.cellIsTargetable( x, y ) )
      return false;
    enemyQuad.fireAtCell( x, y );
    return true;
  }
  
      /**
     * Fires at a specific cell, marking it for a hit or miss, and returns a
     * boolean value representing successfully or unsuccessfully hitting a ship.
     * 
     * @param x the x-coordinate of the cell
     * @param y the y-coordinate of the cell
     * @return true if the cell was successfully fired at AND the cell contained
     * an undamaged ship deck; false otherwise
     */
    public boolean tryEnemyShipDamage( int x, int y )
    {
      // short circuit eval is key here, check to see if cell at specified
      //  quadrant location contained a ship BEFORE changing the state of
      //  the cell. this is quite useful for AI players and populating the
      //  heatmap with state metadata. 
      return getEnemyQuad().cellIsShip( x, y ) 
          && getEnemyQuad().fireAtCell( x, y );
    }
  
 /**
 * Synchronizes the state of the player's ships with their quadrant.
 */
  public void syncDecksToQuadrantState()
  {
    for ( int i = 0; i < placedShips; i++ )
    {
      List< int[] > area = locations[i].getOccupiedCells( fleet[i].getLength() );
      Ship ship = fleet[i];
      
      for ( int j = 0; j < ship.getLength(); j++ )
      {
        int[] cellXY = area.get( j );
        ship.setDeck( j, friendlyQuad.getCellType( cellXY[0], cellXY[1] ) );
      }
    }
  }
  
  /**
   * Checks if the player has lost the game.
   * 
   * @return true if all ships are sunk and all ships are placed; false otherwise
   */
  public boolean hasLost()
  {
    return ( getRemainingShips() == 0 && hasPlacedAllShips() );
  }
  
  /**
   * Checks if all ships have been placed.
   * 
   * @return true if all ships are placed; false otherwise
   */
  public boolean hasPlacedAllShips()
  {
    return placedShips == FLEET_SIZE;
  }

  /**
   * Checks if a specific ship has been placed.
   * 
   * @param ship the ship to check
   * @return true if the ship is placed; false otherwise
   */
  public boolean isShipPlaced( Ship ship ) 
  {
    for ( int i = 0; i < placedShips; i++ ) 
      if ( fleet[i] == ship && locations[i] != null ) 
        return true;
    return false;
  }
  
  /**
   * Gets the number of ships placed on the board.
   * 
   * @return the number of placed ships
   */
  public int getShipsPlaced()
  {
    return this.placedShips;
  }
  
  /**
   * Gets the number of ships remaining to be placed.
   * 
   * @return the number of remaining ships to place
   */
  public int getRemainingShipsToPlace()
  {
    return FLEET_SIZE - placedShips;
  }
  
  /**
   * Gets the number of remaining ships that are not sunk.
   * 
   * @return the number of remaining ships
   */
  public int getRemainingShips()
  {
    int count = 0;
    for ( Ship ship : fleet )
      if ( !ship.isSunk() )
        count++;
    return count;
  }

  /**
   * Gets the ship located at the specified coordinates.
   * 
   * @param x the x-coordinate
   * @param y the y-coordinate
   * @return the ship at the specified coordinates, or null if none exists
   */
  public Ship getShipAt( int x, int y )
  {
    int[] shipCell = new int[] { x, y };
    for ( int i = 0; i < locations.length; i++ )
    {
      List< int[] > cells = locations[i].getOccupiedCells( fleet[i].getLength() );
      for ( int[] cell : cells )
      {
        if ( shipCell[0] == cell[0] && shipCell[1] == cell[1] )
          return fleet[i];
      }
    }
    return null;
  }
}
