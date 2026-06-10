package seasofyore.core;

/**
 * The local stand-in for an opponent at another screen. Their real fleet
 * lives across the water and is never known here -- that is the entire
 * security model of networked battleship: each side is authoritative over
 * its own ships and reveals only shot results. Locally, a RemotePlayer's
 * quadrant accumulates the hit/miss marks our shots earn, and two flags
 * delivered by the wire protocol stand in for the state we cannot see:
 * whether their fleet is placed, and whether it has been destroyed.
 *
 * @author dylan
 */
public class RemotePlayer extends Player
{
  /**
   * Serialization version (networked matches are not saveable, but Player
   * is Serializable and the field is cheap honesty).
   */
  private static final long serialVersionUID = 1L;

  /**
   * Whether the remote side has declared its fleet placed (GREADY).
   */
  private boolean fleetReady = false;

  /**
   * Whether the remote side has declared its fleet destroyed (a GRESULT
   * carrying DEFEATED).
   */
  private boolean defeated = false;

  /**
   * Constructs the stand-in for the player across the water.
   *
   * @param civ the civilization they command
   * @param fQ  their quadrant (as seen from this screen)
   * @param eQ  the local player's quadrant
   */
  public RemotePlayer( Civilization civ, PlayerQuadrant fQ, PlayerQuadrant eQ )
  {
    super( civ, fQ, eQ );
  }

  /**
   * Remote players act through the wire, not through this screen's UI and
   * not through an AI -- so they are neither autonomous nor local.
   *
   * @return false, always
   */
  @Override
  public boolean isAutonomous()
  {
    return false;
  }

  /**
   * @return true, definitionally
   */
  @Override
  public boolean isRemote()
  {
    return true;
  }

  /**
   * Their fleet is "placed" when the wire says so.
   *
   * @return true once GREADY has arrived
   */
  @Override
  public boolean hasPlacedAllShips()
  {
    return fleetReady;
  }

  /**
   * Records the remote side's GREADY declaration.
   */
  public void markFleetReady()
  {
    fleetReady = true;
  }

  /**
   * Their loss is known only by their own announcement.
   *
   * @return true once a DEFEATED result has arrived
   */
  @Override
  public boolean hasLost()
  {
    return defeated;
  }

  /**
   * Records the remote side's announcement of its own destruction.
   */
  public void markDefeated()
  {
    defeated = true;
  }

  /**
   * The local model cannot count an unseen fleet; this only feeds UI
   * counts, never rules decisions (those ride the wire).
   *
   * @return the full fleet size until defeat, then zero
   */
  @Override
  public int getRemainingShips()
  {
    return defeated ? 0 : FLEET_SIZE;
  }

  /**
   * Never places locally: the real fleet is across the water.
   */
  @Override
  public void randomVesselPlacement()
  {
    // their placement happens on their own screen
  }

  /**
   * Remote moves arrive as GSHOT messages, never from this method.
   *
   * @return null, always
   */
  @Override
  public int[] calculateNextAttack()
  {
    return null;
  }

  /**
   * Nothing to learn locally; the remote side runs its own bookkeeping.
   */
  @Override
  public void processAttackResult( int x, int y, boolean hit )
  {
    // no local model of the remote commander's mind
  }
}
