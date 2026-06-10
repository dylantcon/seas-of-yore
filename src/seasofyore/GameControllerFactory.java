/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore;

import seasofyore.core.PlayerFactory;
import seasofyore.core.PlayerType;

/**
 * Factory class responsible for creating GameController instances based on game mode.
 * This enables lazy initialization and resource management.
 * 
 * @author dylan
 */
public class GameControllerFactory
{
  
  /**
   * Creates a GameController instance configured for the specified game mode.
   * 
   * @param mode the game mode to create a controller for
   * @param returnTitle callback to return to the title screen
   * @return a properly configured GameController instance
   */
  public GameController createController( GameMode mode, Runnable returnTitle )
  {
    switch ( mode )
    {
      case CLASSIC:
        return new GameController( false, returnTitle );
      case SALVO:
        return new GameController( true, returnTitle );
      case AI_EASY:
        return createAIController( PlayerFactory.AIDifficulty.EASY, false, returnTitle );
      case AI_MEDIUM:
        return createAIController( PlayerFactory.AIDifficulty.MEDIUM, false, returnTitle );
      case AI_HARD:
        return createAIController( PlayerFactory.AIDifficulty.HARD, false, returnTitle );
      case MULTIPLAYER_LAN:
        throw new UnsupportedOperationException( "Multiplayer mode not yet implemented" );
      case MULTIPLAYER_ONLINE:
        throw new UnsupportedOperationException( "Multiplayer mode not yet implemented" );
      default:
        throw new IllegalArgumentException( "Unknown game mode: " + mode );
    }
  }
  
  /**
   * Creates a GameController with an AI opponent.
   * 
   * @param d the AI difficulty level
   * @param rT callback to return to the title screen
   * @return a GameController configured for AI play
   */
  private GameController createAIController( PlayerFactory.AIDifficulty d, boolean s, Runnable rT )
  {
    return new GameController( s, rT, true, d );
  }

  /**
   * Creates a GameController for an arbitrary matchup: any combination of human
   * and AI tiers on the two civilizations, in either Classic or Salvo mode.
   * This is the entry point for the custom-battle configuration screen.
   *
   * @param britonsType the kind of player controlling the Britons
   * @param franksType  the kind of player controlling the Franks
   * @param salvo       true for Salvo mode; false for Classic
   * @param returnTitle callback to return to the title screen
   * @return a configured GameController
   */
  public GameController createCustomController( PlayerType britonsType, PlayerType franksType,
                                               boolean salvo, Runnable returnTitle )
  {
    return createCustomController( britonsType, franksType, salvo, true, returnTitle );
  }

  /**
   * Creates a GameController for an arbitrary matchup with presentation
   * preferences: any combination of human and AI tiers, Classic or Salvo
   * rules, and the falling-stone attack animation shown or skipped.
   *
   * @param britonsType     the kind of player controlling the Britons
   * @param franksType      the kind of player controlling the Franks
   * @param salvo           true for Salvo mode; false for Classic
   * @param stoneAnimations true to animate attacks with the falling stone
   * @param returnTitle     callback to return to the title screen
   * @return a configured GameController
   */
  public GameController createCustomController( PlayerType britonsType, PlayerType franksType,
                                               boolean salvo, boolean stoneAnimations,
                                               Runnable returnTitle )
  {
    return new GameController( salvo, returnTitle, britonsType, franksType,
                               stoneAnimations );
  }
}
