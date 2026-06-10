/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore;

import seasofyore.core.MatchConfig;
import seasofyore.core.PlayerType;

/**
 * Factory class responsible for creating GameController instances. Every
 * matchup -- the old fixed modes included -- reduces to building the right
 * {@link MatchConfig}, so the factory is now a thin translation layer.
 *
 * @author dylan
 */
public class GameControllerFactory
{

  /**
   * Creates a GameController for an assembled match configuration. This is
   * the entry point for the battle-setup screen.
   *
   * @param config      the match configuration
   * @param returnTitle callback to return to the title screen
   * @return a configured GameController
   */
  public GameController createController( MatchConfig config, Runnable returnTitle )
  {
    return new GameController( config, returnTitle );
  }

  /**
   * Creates a GameController instance configured for one of the legacy fixed
   * game modes, expressed as the equivalent match configuration.
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
        return createController( hotSeat( false ), returnTitle );
      case SALVO:
        return createController( hotSeat( true ), returnTitle );
      case AI_EASY:
        return createController( soloVersus( PlayerType.AI_EASY ), returnTitle );
      case AI_MEDIUM:
        return createController( soloVersus( PlayerType.AI_MEDIUM ), returnTitle );
      case AI_HARD:
        return createController( soloVersus( PlayerType.AI_HARD ), returnTitle );
      case MULTIPLAYER_LAN:
        throw new UnsupportedOperationException( "Multiplayer mode not yet implemented" );
      case MULTIPLAYER_ONLINE:
        throw new UnsupportedOperationException( "Multiplayer mode not yet implemented" );
      default:
        throw new IllegalArgumentException( "Unknown game mode: " + mode );
    }
  }

  /**
   * The configuration for a classic two-human hot-seat match.
   *
   * @param salvo true for SALVO rules
   * @return the match configuration
   */
  private static MatchConfig hotSeat( boolean salvo )
  {
    return new MatchConfig( PlayerType.HUMAN, PlayerType.HUMAN,
                            null, null, salvo, true );
  }

  /**
   * The configuration for the legacy solo wiring: a human commanding the
   * Britons against an AI commanding the Franks.
   *
   * @param ai the AI tier to face
   * @return the match configuration
   */
  private static MatchConfig soloVersus( PlayerType ai )
  {
    return new MatchConfig( PlayerType.HUMAN, ai, null, null, false, true );
  }
}
