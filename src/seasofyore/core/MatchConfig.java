package seasofyore.core;

/**
 * Everything needed to set up one match, gathered into a single immutable
 * value instead of an ever-growing constructor parameter list: who commands
 * each civilization (and, for humans, what they wish to be called), the
 * rules variant, and presentation preferences. The battle-setup screen
 * builds one of these; the GameController consumes it.
 *
 * @author dylan
 */
public final class MatchConfig
{
  /**
   * The kind of player commanding the Britons.
   */
  private final PlayerType britonsType;

  /**
   * The kind of player commanding the Franks.
   */
  private final PlayerType franksType;

  /**
   * The Britons commander's chosen name (meaningful for humans; null or
   * blank means unnamed).
   */
  private final String britonsName;

  /**
   * The Franks commander's chosen name.
   */
  private final String franksName;

  /**
   * True for SALVO rules; false for Classic.
   */
  private final boolean salvoMode;

  /**
   * True to animate attacks with the falling stone; false for instant shots.
   */
  private final boolean stoneAnimations;

  /**
   * Assembles a match configuration.
   *
   * @param britonsType     who commands the Britons
   * @param franksType      who commands the Franks
   * @param britonsName     the Britons commander's chosen name (may be null)
   * @param franksName      the Franks commander's chosen name (may be null)
   * @param salvoMode       true for SALVO rules
   * @param stoneAnimations true to animate attacks
   */
  public MatchConfig( PlayerType britonsType, PlayerType franksType,
                      String britonsName, String franksName,
                      boolean salvoMode, boolean stoneAnimations )
  {
    this.britonsType = ( britonsType == null ) ? PlayerType.HUMAN : britonsType;
    this.franksType = ( franksType == null ) ? PlayerType.HUMAN : franksType;
    this.britonsName = britonsName;
    this.franksName = franksName;
    this.salvoMode = salvoMode;
    this.stoneAnimations = stoneAnimations;
  }

  /**
   * Who commands the Britons.
   *
   * @return the Britons' player type
   */
  public PlayerType getBritonsType()
  {
    return this.britonsType;
  }

  /**
   * Who commands the Franks.
   *
   * @return the Franks' player type
   */
  public PlayerType getFranksType()
  {
    return this.franksType;
  }

  /**
   * The Britons commander's chosen name.
   *
   * @return the name, or null if none was chosen
   */
  public String getBritonsName()
  {
    return this.britonsName;
  }

  /**
   * The Franks commander's chosen name.
   *
   * @return the name, or null if none was chosen
   */
  public String getFranksName()
  {
    return this.franksName;
  }

  /**
   * Whether the match uses SALVO rules.
   *
   * @return true for SALVO; false for Classic
   */
  public boolean isSalvoMode()
  {
    return this.salvoMode;
  }

  /**
   * Whether attacks ride the falling-stone animation.
   *
   * @return true to animate attacks
   */
  public boolean useStoneAnimations()
  {
    return this.stoneAnimations;
  }
}
