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

  /**
   * Notifies the strategy that an enemy ship has been sunk. This is the
   * targeting equivalent of an opponent announcing "you sank my Galleon!" --
   * it is legitimate, observable information (not board peeking), and it lets a
   * strategy stop hunting a finished ship and shrink the set of ship lengths it
   * still expects to find.
   *
   * <p>The default implementation does nothing, so simple strategies that do
   * not learn (e.g. {@link RandomGuessStrategy}) need not override it.</p>
   *
   * @param sunkType the type (and therefore length) of the ship that was sunk
   * @param x        the x-coordinate of the killing shot
   * @param y        the y-coordinate of the killing shot
   */
  default void notifyShipSunk( ShipType sunkType, int x, int y )
  {
    // no-op by default; learning strategies override this
  }
}
