package seasofyore.match;

import seasofyore.GameController;
import seasofyore.core.Player;

/**
 * The seam between the game and the question "where are the players?". A
 * match handler owns every decision that depends on whether the people in a
 * match share one screen, sit at opposite ends of a LAN, or meet across the
 * internet -- so the GameController and the phase system can stay blissfully
 * ignorant of the answer.
 * <p>
 * The controller consults its handler at each point where locality matters:
 * whether a turn handoff needs the secrecy curtain, whether the match can be
 * frozen by a pause menu, whether a chat line belongs in the terminal, and
 * which player the person at <em>this</em> screen is rooting for (the
 * "local player", which decides victory vs. defeat staging). Implementations
 * range from the fully working {@link OfflineMatchHandler} to the networked
 * skeletons that will carry LAN and online play.
 *
 * @author dylan
 */
public interface MatchHandler
{
  /**
   * Binds the handler to a running game. Called once per game start, after
   * the board exists; networked handlers use this to settle who is local.
   *
   * @param controller the controller managing the match
   */
  void beginMatch( GameController controller );

  /**
   * Whether turn handoffs must hide the board behind the curtain. Only true
   * when two humans share one physical screen.
   *
   * @return true if the secrecy curtain is required between turns
   */
  boolean showsCurtain();

  /**
   * Whether the match can be frozen. A solo or hot-seat game pauses freely;
   * a networked match cannot stop the other side's clock.
   *
   * @return true if pausing (and saving) is supported
   */
  boolean supportsPause();

  /**
   * Whether a text chat line belongs in the terminal. Only meaningful when
   * an opponent is somewhere else.
   *
   * @return true if chat is supported
   */
  boolean supportsChat();

  /**
   * The player the person at this screen commands, or null when that is not
   * a single player (hot-seat: both; spectating: neither). This is what
   * decides whether a finished game stages a victory or a defeat.
   *
   * @return the local player, or null if there is not exactly one
   */
  Player getLocalPlayer();

  /**
   * Whether the person at this screen may act right now. Always true
   * offline; in a networked match, false while the remote side acts.
   *
   * @return true if local input should be honoured this turn
   */
  boolean isLocalTurn();

  /**
   * Notifies the handler that the active turn is handing off. Networked
   * handlers forward the turn across the wire here.
   */
  void onTurnEnded();

  /**
   * Concedes the match on behalf of the local player.
   */
  void forfeit();

  /**
   * Releases any resources the handler holds (sockets, threads). Offline
   * handlers have nothing to release.
   */
  void shutdown();
}
