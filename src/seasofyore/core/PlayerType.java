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
  HUMAN( "Human", "Mortal Commander",
    "A livin', breathin' commander o' flesh an' blood, sailin' by wit, "
    + "nerve, an' whatever luck the saints will spare. God bless yer soul." , null ),

  /** An AI using the Easy (checkerboard) strategy. */
  AI_EASY( "Easy AI", "Deckhand Davey",
    "A green deckhand what fires wherever the gulls point. Davey pokes at "
    + "the waves like a blindfolded darter. "
    + "Fresh 'o mind, the lad be hardly able to tie a lowly knot."
    + " Davey be a fine first foe fer "
    + "a fresh commander findin' their sea legs.", AIDifficulty.EASY ),

  /** An AI using the Medium (heuristic) strategy. */
  AI_MEDIUM( "Medium AI", "Bosun Bramble",
    "A weather'd bosun with a nose fer blood in the water. "
    + "This salty buccaneer be clever, an' he'll give ye no quarter. "
    + "Bramble wanders 'til he strikes wood, then he's after ye, plank-by-"
    + "plank, 'til yer hull gives way to the cold, foamy sea.", AIDifficulty.MEDIUM ),

  /** An AI using the Hard (probability heatmap) strategy. */
  AI_HARD( "Hard AI", "Admiral Greywake",
    "A grizzled admiral what charts ev'ry wave with grim, unrelentin' arithmetic. "
    + "Fer twenty-five summers, he ponder'd the logs at a maritime academy. "
    + "Greywake reads the sea like scripture, yer very silence tells "
    + "him where yer sailors be.", AIDifficulty.HARD ),

  /** An AI using the Extreme (joint Monte Carlo + stealth placement) strategy. */
  AI_EXTREME( "Extreme AI", "The Drowned King",
    "A legend, say one; a yarn, say another. Some tales be bilge; not this'n. "
    + "Heed my warning: A spectre be in these waters, under countless fathoms o' "
    + "the murky brine. It knowst where thy keels rest afore thou dost. No chart "
    + "explain'd it, nor fleet, outlasted it. Face the Drowned King, an' yer "
    + "name joins the myths at the bottom o' the sea.", AIDifficulty.EXTREME ),

  /**
   * A human at another screen, whose moves arrive over the wire. Never
   * offered in the local battle-setup selectors; networked matchmaking
   * assigns it.
   */
  REMOTE( "Across the Water", "Distant Commander",
    "A mortal commander somewhere across the water, sailin' under their own "
    + "colours. What they're plannin', only the wire knows.", null );

  /**
   * A human-readable label for menus.
   */
  private final String label;

  /**
   * The commander's name, as the harbour-folk tell it. Used wherever the
   * game speaks of an opponent with a voice rather than a setting.
   */
  private final String nickname;

  /**
   * The tavern-tale description of this commander, in proper sea-dog speak.
   */
  private final String lore;

  /**
   * The AI difficulty this type maps to, or null for {@link #HUMAN}.
   */
  private final AIDifficulty difficulty;

  /**
   * Constructs a PlayerType with display texts and (for AI types) the
   * difficulty it maps to.
   *
   * @param label      the menu label
   * @param nickname   the commander's name in the game's voice
   * @param lore       the tavern-tale description
   * @param difficulty the AI difficulty, or null for a human
   */
  PlayerType( String label, String nickname, String lore, AIDifficulty difficulty )
  {
    this.label = label;
    this.nickname = nickname;
    this.lore = lore;
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
   * @return true for any AI tier; false for humans, local or remote
   */
  public boolean isAI()
  {
    return this != HUMAN && this != REMOTE;
  }

  /**
   * Whether this side sits at a different screen.
   *
   * @return true only for {@link #REMOTE}
   */
  public boolean isRemote()
  {
    return this == REMOTE;
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

  /**
   * The commander's name as the harbour-folk tell it.
   *
   * @return the nickname
   */
  public String getNickname()
  {
    return this.nickname;
  }

  /**
   * The tavern-tale description of this commander.
   *
   * @return the lore text
   */
  public String getLore()
  {
    return this.lore;
  }
}
