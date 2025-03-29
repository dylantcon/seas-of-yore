/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package seasofyore.core;

/**
 *
 * @author dylan
 */
public interface AIStrategy 
{
  /**
   * Calculates a ship placement based on the strategy.
   * 
   * @param ship     the ship to place
   * @param quadrant the quadrant to place the ship in
   * @return a ShipHeading representing the chosen position and direction
   */
  ShipHeading calculateShipPlacement( Ship ship, PlayerQuadrant quadrant );
  
  /**
   * Calculates firing coordinates based on the strategy.
   * 
   * @param enemyQuadrant the enemy quadrant to target
   * @return coordinates as [x, y]
   */
  int[] calculateFiringCoordinates( PlayerQuadrant enemyQuadrant );
  
  /**
   * Processes the result of a previous attack to update the strategy.
   * 
   * @param x    the x-coordinate of the attack
   * @param y    the y-coordinate of the attack
   * @param isHit true if the attack hit a ship; false otherwise
   */
  void processHitResult( int x, int y, boolean isHit );
}
