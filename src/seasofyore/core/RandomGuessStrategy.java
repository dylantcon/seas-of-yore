/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A simple AI strategy that makes random guesses for ship placement and targeting.
 * This serves as the base difficulty level for AI opponents.
 * 
 * @author dylan
 */
public class RandomGuessStrategy implements AIStrategy
{
  private final Random random = new Random();
  
  /**
   * Calculates a random ship placement.
   * 
   * @param ship     the ship to place
   * @param quadrant the quadrant to place the ship in
   * @return a randomly generated ShipHeading
   */
  @Override
  public ShipHeading calculateShipPlacement(Ship ship, PlayerQuadrant quadrant)
  {
    return ShipHeading.getRandomInstance();
  }
  
  /**
   * Calculates random firing coordinates, targeting only cells that haven't
   * been fired upon already.
   * 
   * @param enemyQuadrant the enemy quadrant to target
   * @return random valid coordinates as [x, y]
   */
  @Override
  public int[] calculateFiringCoordinates(PlayerQuadrant enemyQuadrant)
  {
    List<int[]> validTargets = new ArrayList<>();
    
    // Collect all valid (unfired) targets
    for (int x = 0; x < PlayerQuadrant.GRID_SIZE; x++)
    {
      for (int y = 0; y < PlayerQuadrant.GRID_SIZE; y++)
      {
        if (enemyQuadrant.cellIsTargetable(x, y))
        {
          validTargets.add(new int[]{x, y});
        }
      }
    }
    
    if (validTargets.isEmpty())
    {
      return null; // No valid targets left
    }
    
    // Choose a random target from the valid ones
    return validTargets.get(random.nextInt(validTargets.size()));
  }
  
  /**
   * Does nothing with the hit result, as this strategy doesn't learn.
   * 
   * @param x    the x-coordinate of the attack
   * @param y    the y-coordinate of the attack
   * @param isHit true if the attack hit a ship; false otherwise
   */
  @Override
  public void processHitResult(int x, int y, boolean isHit)
  {
    // RandomGuessStrategy doesn't learn from previous results
  }
}