/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore.core;

/**
 *
 * @author dylan
 */
public class PlayerFactory 
{
  /**
   * Creates a human player.
   * 
   * @param civ The civilization the human player will control
   * @param fQ  The player's friendly quadrant
   * @param eQ  The player's enemy quadrant
   * @return a new HumanPlayer instance
   */
  public static Player createHumanPlayer( Civilization civ, PlayerQuadrant fQ, PlayerQuadrant eQ )
  {
    return new HumanPlayer( civ, fQ, eQ );
  }

  /**
   * Creates a player of the given type -- human or any AI tier. This is the
   * single entry point used by arbitrary game configuration.
   *
   * @param type the kind of player to create
   * @param civ  the civilization the player will control
   * @param fQ   the player's friendly quadrant
   * @param eQ   the player's enemy quadrant
   * @return a HumanPlayer or a suitably configured AIPlayer
   */
  public static Player createPlayer( PlayerType type, Civilization civ,
                                     PlayerQuadrant fQ, PlayerQuadrant eQ )
  {
    if ( type == null || !type.isAI() )
      return createHumanPlayer( civ, fQ, eQ );
    return createAIPlayer( civ, fQ, eQ, type.getDifficulty() );
  }
  
  public static Player createAIPlayer( Civilization civ, PlayerQuadrant fQ, 
  /**********************************/ PlayerQuadrant eQ, AIDifficulty difficulty )
  {
    AIStrategy strategy;
    
    switch ( difficulty )
    {
      case EASY:
        strategy = new EasyStrategy();
        break;
      case MEDIUM:
        strategy = new MediumStrategy();
        break;
      case HARD:
        strategy = new HardStrategy();
        break;
      default:
        strategy = new RandomGuessStrategy();
        break;
    }
    
    return new AIPlayer( civ, fQ, eQ, strategy );
  }
  
  public enum AIDifficulty
  {
    EASY, MEDIUM, HARD
  }
}
