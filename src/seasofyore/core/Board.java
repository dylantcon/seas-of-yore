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
public class Board implements java.io.Serializable
{
  /**
   * Serialization version for saved games.
   */
  private static final long serialVersionUID = 1L;

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
   * The kind of player controlling the Britons.
   */
  private final PlayerType britonsType;

  /**
   * The kind of player controlling the Franks.
   */
  private final PlayerType franksType;


  /**
   * Constructs a new Board object with two human players.
   */
  public Board()
  {
    this( PlayerType.HUMAN, PlayerType.HUMAN );
  }

  /**
   * Legacy constructor: Britons are always human, Franks are an AI of the given
   * difficulty (or human when {@code withAI} is false). Retained so existing
   * single-AI wiring keeps working; delegates to the canonical constructor.
   *
   * @param withAI       true to make the Franks an AI
   * @param aiDifficulty the Franks' AI difficulty (ignored if withAI is false)
   */
  public Board( boolean withAI, PlayerFactory.AIDifficulty aiDifficulty )
  {
    this( PlayerType.HUMAN,
          withAI ? PlayerType.fromDifficulty( aiDifficulty ) : PlayerType.HUMAN );
  }

  /**
   * Canonical constructor: builds a board for any matchup by specifying what
   * controls each civilization. Either side may be a human or any AI tier,
   * enabling human-vs-human, human-vs-AI (on either civilization), and AI-vs-AI
   * spectator games.
   *
   * @param britonsType the kind of player controlling the Britons
   * @param franksType  the kind of player controlling the Franks
   */
  public Board( PlayerType britonsType, PlayerType franksType )
  {
    this.britonsType = ( britonsType == null ) ? PlayerType.HUMAN : britonsType;
    this.franksType = ( franksType == null ) ? PlayerType.HUMAN : franksType;

    bQuad = new PlayerQuadrant();
    fQuad = new PlayerQuadrant();

    britons = PlayerFactory.createPlayer( this.britonsType, Civilization.BRITONS, bQuad, fQuad );
    franks = PlayerFactory.createPlayer( this.franksType, Civilization.FRANKS, fQuad, bQuad );

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
    
    // if new current player is AI and in setup phase, place ships automatically
    if ( currentPlayer.isAutonomous() && !currentPlayer.hasPlacedAllShips() && setupPhase )
    {
      // AI places ships automatically
      currentPlayer.randomVesselPlacement();
      // and immediately switches back to human player
      switchTurns();
    }
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
   * Gets the kind of player controlling the Britons.
   *
   * @return the Britons' player type
   */
  public PlayerType getBritonsType()
  {
    return this.britonsType;
  }

  /**
   * Gets the kind of player controlling the Franks.
   *
   * @return the Franks' player type
   */
  public PlayerType getFranksType()
  {
    return this.franksType;
  }

  /**
   * Gets the kind of player controlling the given civilization.
   *
   * @param civ the civilization to look up
   * @return the PlayerType commanding that civilization
   */
  public PlayerType getPlayerType( Civilization civ )
  {
    return ( civ == Civilization.BRITONS ) ? britonsType : franksType;
  }

  /**
   * Counts how many of the two players are human. Zero means an AI-vs-AI
   * spectator game; one means a standard solo game on either civilization; two
   * means a hot-seat human-vs-human game.
   *
   * @return the number of human players (0, 1, or 2)
   */
  public int getHumanCount()
  {
    int count = 0;
    if ( !britons.isAutonomous() )
      count++;
    if ( !franks.isAutonomous() )
      count++;
    return count;
  }

  /**
   * Whether this is an AI-vs-AI game the human only spectates.
   *
   * @return true if neither player is human
   */
  public boolean isSpectating()
  {
    return getHumanCount() == 0;
  }

  /**
   * Returns the single human player, or null if there is not exactly one. Used
   * to anchor the on-screen perspective in solo games.
   *
   * @return the lone human player, or null
   */
  public Player getSoleHuman()
  {
    if ( getHumanCount() != 1 )
      return null;
    return britons.isAutonomous() ? franks : britons;
  }

  /**
   * Auto-places the fleet of every AI player that has not yet placed. Lets the
   * controller settle all AI boards up front so the placement UI only ever has
   * to involve humans.
   */
  public void autoPlaceAIShips()
  {
    if ( britons.isAutonomous() && !britons.hasPlacedAllShips() )
      britons.randomVesselPlacement();
    if ( franks.isAutonomous() && !franks.hasPlacedAllShips() )
      franks.randomVesselPlacement();
  }

  /**
   * Prepares the board for play, intervening only when the nominal first player
   * (the Britons) is an AI. In that case it settles every AI fleet up front and
   * then either ends setup outright (an AI-vs-AI spectator game) or hands setup
   * to the lone human so the placement UI is only ever shown to a person. When
   * the Britons are human, the existing lazy setup flow already does the right
   * thing, so this returns without changing anything.
   */
  public void prepareForPlay()
  {
    if ( !currentPlayer.isAutonomous() )
      return;

    autoPlaceAIShips();

    if ( isPlacementFinal() )
      setupPhase = false;            // AI vs AI: nothing left to place
    else
      currentPlayer = getNextPlayer(); // hand setup to the lone human
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
