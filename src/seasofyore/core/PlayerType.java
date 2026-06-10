package seasofyore.core;

import seasofyore.core.PlayerFactory.AIDifficulty;

/**
 * Describes what controls one side of a game: a human at the keyboard, or an AI
 * at one of the three difficulty tiers. This is the single knob the game
 * configuration turns to build any matchup -- human vs human, human vs any AI
 * (on either civilization), or AI vs AI for spectating.
 *
 * @author dylan
 */
public enum PlayerType
{
  /** A human player who acts through the UI. */
  HUMAN( "Human", null ),
  /** An AI using the Easy (checkerboard) strategy. */
  AI_EASY( "Easy AI", AIDifficulty.EASY ),
  /** An AI using the Medium (heuristic) strategy. */
  AI_MEDIUM( "Medium AI", AIDifficulty.MEDIUM ),
  /** An AI using the Hard (probability heatmap) strategy. */
  AI_HARD( "Hard AI", AIDifficulty.HARD ),
  /** An AI using the Extreme (joint Monte Carlo + stealth placement) strategy. */
  AI_EXTREME( "Extreme AI", AIDifficulty.EXTREME );

  /**
   * A human-readable label for menus.
   */
  private final String label;

  /**
   * The AI difficulty this type maps to, or null for {@link #HUMAN}.
   */
  private final AIDifficulty difficulty;

  /**
   * Constructs a PlayerType with a display label and (for AI types) the
   * difficulty it maps to.
   *
   * @param label      the menu label
   * @param difficulty the AI difficulty, or null for a human
   */
  PlayerType( String label, AIDifficulty difficulty )
  {
    this.label = label;
    this.difficulty = difficulty;
  }

  /**
   * Maps an AI difficulty to its corresponding player type, or {@link #HUMAN}
   * when no difficulty is given. Bridges legacy difficulty-based wiring to the
   * unified player-type model.
   *
   * @param difficulty the AI difficulty, or null for a human
   * @return the matching PlayerType
   */
  public static PlayerType fromDifficulty( AIDifficulty difficulty )
  {
    if ( difficulty == null )
      return HUMAN;
    switch ( difficulty )
    {
      case EASY:
        return AI_EASY;
      case MEDIUM:
        return AI_MEDIUM;
      case HARD:
        return AI_HARD;
      case EXTREME:
        return AI_EXTREME;
      default:
        throw new IllegalArgumentException( "Unknown difficulty: " + difficulty );
    }
  }

  /**
   * Whether this side is controlled by an AI.
   *
   * @return true for any AI tier; false for {@link #HUMAN}
   */
  public boolean isAI()
  {
    return this != HUMAN;
  }

  /**
   * The AI difficulty this type maps to.
   *
   * @return the difficulty, or null if this is a human
   */
  public AIDifficulty getDifficulty()
  {
    return this.difficulty;
  }

  /**
   * The human-readable menu label for this type.
   *
   * @return the display label
   */
  public String getLabel()
  {
    return this.label;
  }
}
