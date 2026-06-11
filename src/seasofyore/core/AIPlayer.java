/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore.core;

/**
 * Represents an AI-controlled player in the game.
 * AI players make autonomous decisions based on predefined strategies.
 * 
 * @author dylan
 */
public class AIPlayer extends Player
{
  /**
   * The strategy this AI player uses for decision-making.
   */
  private AIStrategy strategy;
  
  /**
   * Constructs a new AIPlayer with the specified civilization, quadrants,
   * and default strategy.
   * 
   * @param civ
   * @param fQ
   * @param eQ 
   */
  public AIPlayer( Civilization civ, PlayerQuadrant fQ, PlayerQuadrant eQ )
  {
    super( civ, fQ, eQ );
    // default to random guess, change later
    this.strategy = new RandomGuessStrategy();
  }
  
  /**
   * Constructs a new AIPlayer with the specified civilization, quadrants,
   * and strategy.
   * 
   * @param civ     the civilization of this AI player
   * @param fQ      the friendly quadrant relative to this AI player
   * @param eQ      the enemy quadrant relative to this AI player
   * @param strat   the AI strategy to use
   */
  public AIPlayer( Civilization civ, PlayerQuadrant fQ, PlayerQuadrant eQ, AIStrategy strat )
  {
    super( civ, fQ, eQ );
    this.strategy = strat;
  }
  
   
  /**
   * AI players make autonomous decisions.
   * 
   * @return true, as AI players perform game related tasks without UI interaction
   */
  @Override
  public boolean isAutonomous()
  {
    return true;
  }
  
  /**
   * Sets the strategy for this AI player.
   * 
   * @param strategy the new strategy to use
   */
  public void setStrategy( AIStrategy strategy )
  {
    this.strategy = strategy;
  }
  
  /**
   * Gets the current strategy for this AI player.
   * 
   * @return the current strategy
   */
  public AIStrategy getStrategy()
  {
    return this.strategy;
  }
  
  /**
   * Calculates the next attack based on the AI's strategy.
   * 
   * @return coordinates of the target as [x, y]
   */
  @Override
  public int[] calculateNextAttack()
  {
    return strategy.calculateFiringCoordinates(getEnemyQuad());
  }
  
  /**
   * Processes the result of an attack, updating the AI's strategy accordingly.
   * 
   * @param x   the x-coordinate of the attack
   * @param y   the y-coordinate of the attack
   * @param hit true if the attack hit a ship; false otherwise
   */
  @Override
  public void processAttackResult(int x, int y, boolean hit)
  {
    strategy.processHitResult(x, y, hit);
  }

  /**
   * Forwards a sunk-ship notification to this AI's targeting strategy so it can
   * stop hunting the dead ship and update its model of the remaining fleet.
   *
   * @param sunkType the type (and length) of the ship sunk
   * @param x        the x-coordinate of the killing shot
   * @param y        the y-coordinate of the killing shot
   */
  @Override
  public void notifyEnemyShipSunk( ShipType sunkType, int x, int y )
  {
    strategy.notifyShipSunk( sunkType, x, y );
  }
  
  /**
   * Implements AI ship placement using the strategy.
   * Overrides the random placement with more intelligent strategy-based placement.
   */
  @Override
  @SuppressWarnings("empty-statement")
  public void randomVesselPlacement()
  {
    Ship[] ships = Ship.getListInstance();
    reset();
    friendlyQuad.eraseCells();
    
    for (int i = 0; i < FLEET_SIZE; i++)
    {
      ShipHeading heading = strategy.calculateShipPlacement(ships[i], getFriendlyQuad());
      if (!placeVessel(ships[i], heading))
      {
        // If strategy-based placement fails, fall back to random
        while (!placeVessel(ships[i], ShipHeading.getRandomInstance()));
      }
    }
  }
  
}
