/**
 * The core package, which contains all files associated with the back-end game
 * logic (players, board, quadrants, cells, ships, etc.)
 */
package seasofyore.core;

import seasofyore.ui.QuadrantPanel;

/**
 * Represents the game board in the Seas of Yore game, managing players,
 * their quadrants, and game-play phases.
 * @author dylan connolly
 * 
 */
public class Board 
{
  
  /**
   * The Britons player (Player 1) who started the Hundred Years' War.
   */
  private final Player britons;
  /**
   * The Franks player (Player 2).
   */ 
  private final Player franks;
  /**
   * The player whose turn it currently is.
   */
  private Player currentPlayer;
  
  /**
   * Indicates whether the game is in the setup phase.
   */
  private boolean setupPhase;
  
  
  /**
   * The quadrant representing the Britons' territory.
   */  
  private final PlayerQuadrant bQuad;
  /**
   * The quadrant representing the Franks' territory.
   */  
  private final PlayerQuadrant fQuad;
  
  
  /**
   * Constructs a new Board object, initializing the quadrants, players, and game state.
   * <ul>
   *     <li>The Britons (Player 1) and Franks (Player 2) are initialized with their respective quadrants.</li>
   *     <li>The game starts in the setup phase with the Britons as the current player.</li>
   * </ul>
   */
  public Board()
  {
    bQuad = new PlayerQuadrant();
    fQuad = new PlayerQuadrant();
    
    britons = new Player( Civilization.BRITONS, bQuad, fQuad );
    franks = new Player( Civilization.FRANKS, fQuad, bQuad );
    
    currentPlayer = britons;
    setupPhase = true;
  }
  
  /**
   * Places a ship for the specified player during the setup phase.
   *
   * @param player  the player placing the ship
   * @param ship    the ship to place
   * @param heading the heading of the ship
   * @return true if the ship was placed successfully; false otherwise
   */
  public boolean placeShip( Player player, Ship ship, ShipHeading heading )
  {
    if ( !setupPhase || player != currentPlayer )
      return false;
    
    boolean success = player.placeVessel( ship, heading );
    if ( success && player.hasPlacedAllShips() )
      switchTurns();
    
    return success;
  }

  /**
   * Fires at a specific cell in the enemy's quadrant.
   *
   * @param attacker the attacking player
   * @param x        the x-coordinate of the target cell
   * @param y        the y-coordinate of the target cell
   * @return true if the target was hit; false otherwise
   * @throws IllegalStateException if called during the setup phase
   */
  public boolean fireAtCell( Player attacker, int x, int y )
  {
    if ( setupPhase )
      throw new IllegalStateException( "Cannot fire during the setup phase." );
    
    Player defender = ( attacker == britons ) ? franks : britons;
    boolean hit = defender.getFriendlyQuad().cellIsShip( x, y );
    
    attacker.fireAtEnemyCell( x, y );
    return hit;
  }
  
  /**
   * Switches turns between the two players. If the setup phase ends,
   * the game transitions to the battle phase.
   */
  public void switchTurns()
  {
    if ( isPlacementFinal() && setupPhase )
      setupPhase = false;
    currentPlayer = ( currentPlayer == britons ? franks : britons );
  }
  
  /**
   * Checks if the game is over.
   *
   * @return true if one of the players has lost; false otherwise
   */
  public boolean isGameOver()
  {
    return ( britons.hasLost() || franks.hasLost() );
  }
  
 /**
   * Checks if the game is in the setup phase.
   *
   * @return true if in the setup phase; false otherwise
   */
  public boolean isSetupPhase()
  {
    return setupPhase;
  }
  
  /**
   * Checks if the current player has finished placing their ships.
   *
   * @return true if the current player has placed all ships; false otherwise
   */
  public boolean hasCurrentFinishedSetup()
  {
    return currentPlayer.hasPlacedAllShips();
  }
  
  /**
   * Checks if the next player has finished placing their ships.
   *
   * @return true if the next player has placed all ships; false otherwise
   */
  public boolean hasNextFinishedSetup()
  {
    return getNextPlayer().hasPlacedAllShips();
  }
  
  /**
   * Checks if ship placement is final, meaning all ships for both players
   * have been placed.
   *
   * @return true if placement is final; false otherwise
   */
  public boolean isPlacementFinal()
  {
    return britons.hasPlacedAllShips() && franks.hasPlacedAllShips();
  }
  
  /**
   * Gets the player whose turn it currently is.
   *
   * @return the current player
   */
  public Player getCurrentPlayer()
  {
    return this.currentPlayer;
  }
   
  /**
   * Gets the next player whose turn it will be.
   *
   * @return the next player
   */
  public Player getNextPlayer()
  {
    return ( britons == currentPlayer ? franks : britons );
  }
  
  /**
   * Gets the Britons player.
   *
   * @return the Britons player
   */
  public Player getBritons()
  {
    return this.britons;
  }
  
  /**
   * Gets the Franks player.
   *
   * @return the Franks player
   */
  public Player getFranks()
  {
    return this.franks;
  }

  /**
   * Creates and returns the QuadrantPanel for the next player.
   *
   * @return the next player's QuadrantPanel
   */
  public QuadrantPanel getNextQuadrantPanel()
  {
    return new QuadrantPanel( getNextPlayer(), getNextQuadrant(), false );
  }
  
  /**
   * Creates and returns the QuadrantPanel for the current player.
   *
   * @return the current player's QuadrantPanel
   */
  public QuadrantPanel getCurrentQuadrantPanel()
  {
    return new QuadrantPanel( getCurrentPlayer(), getCurrentQuadrant(), true );
  }
  
  /**
   * Gets the quadrant representing the next player's territory.
   *
   * @return the next player's PlayerQuadrant
   */
  public PlayerQuadrant getNextQuadrant()
  {
    return this.getNextPlayer().getFriendlyQuad();
  }
  
   /**
   * Gets the quadrant representing the current player's territory.
   *
   * @return the current player's PlayerQuadrant
   */
  public PlayerQuadrant getCurrentQuadrant()
  {
    return this.getCurrentPlayer().getFriendlyQuad();
  }
}
