/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore.core;

/**
 *
 * @author dylan
 */
public class HumanPlayer extends Player
{
  /**
   * Constructs a new HumanPlayer with the specified civilization and quadrants.
   * 
   * @param civ the civilization of this human player
   * @param fQ the friendly quadrant relative to this human player
   * @param eQ the enemy quadrant relative to this human player
   */
  public HumanPlayer( Civilization civ, PlayerQuadrant fQ, PlayerQuadrant eQ )
  {
    super( civ, fQ, eQ );
  }
  
  /**
   * Human players are not autonomous and require UI interaction.
   * 
   * @return false, as human players make decisions through the UI
   */
  @Override
  public boolean isAutonomous()
  {
    return false;
  }
  
  /**
   * Human player select attack coordinates through UI interaction.
   * This method is not used for human players but needs implementation.
   * 
   * @return null, as attack coordinates are determined through UI
   */
  @Override
  public int[] calculateNextAttack()
  {
    // human players select attack coordinates through UI
    return null;
  }
  
  /**
   * Human players don't need to process attack results algorithmically.
   * Results are displayed in the UI.
   * 
   * @param x   the x-coordinate of the attack
   * @param y   the y-coordinate of the attack
   * @param hit true if the attack hit a ship; false otherwise
   */
  @Override
  public void processAttackResult( int x, int y, boolean hit ) {}
}
