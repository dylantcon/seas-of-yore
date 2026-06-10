package seasofyore.match;

import seasofyore.core.Player;
import seasofyore.core.Ship;

/**
 * The match handler for everything played on one machine: solo against an
 * AI, two-human hot-seat, and AI-vs-AI spectating. Every decision reduces to
 * counting the humans at this screen:
 * <ul>
 *   <li>the curtain exists only for two humans hiding fleets from each
 *       other;</li>
 *   <li>pausing (and saving) is always safe -- nobody's clock runs
 *       elsewhere;</li>
 *   <li>chat is meaningless when every player shares one room;</li>
 *   <li>the "local player" is the lone human, if there is exactly one.</li>
 * </ul>
 *
 * @author dylan
 */
public final class OfflineMatchHandler extends AbstractMatchHandler
{
  /**
   * The curtain drops only between two humans sharing the screen.
   *
   * @return true in hot-seat games
   */
  @Override
  public boolean showsCurtain()
  {
    return board() != null && board().getHumanCount() == 2;
  }

  /**
   * Offline matches freeze freely.
   *
   * @return true, always
   */
  @Override
  public boolean supportsPause()
  {
    return true;
  }

  /**
   * No one to chat with across a single keyboard.
   *
   * @return false, always
   */
  @Override
  public boolean supportsChat()
  {
    return false;
  }

  /**
   * The lone human, when there is exactly one; null in hot-seat and
   * spectator games, where the end screen simply celebrates the winner.
   *
   * @return the sole human player, or null
   */
  @Override
  public Player getLocalPlayer()
  {
    return ( board() == null ) ? null : board().getSoleHuman();
  }

  /**
   * Everything is local; input is always honoured.
   *
   * @return true, always
   */
  @Override
  public boolean isLocalTurn()
  {
    return true;
  }

  /**
   * Resolves a shot entirely in this JVM: the defender's fleet is right
   * here, so the verdict is computed, the mark applied, and the callback
   * run synchronously.
   */
  @Override
  public void resolveOutgoingShot( int x, int y, ShotOutcome outcome )
  {
    Player defender = controller.getNextPlayer();
    boolean hit = ( defender.getShipAt( x, y ) != null );

    // mark the cell and sync the defender's deck state
    controller.getNextQuadrantPanel().fireAtCell( x, y );

    Ship struck = hit ? defender.getShipAt( x, y ) : null;
    boolean sunk = ( struck != null && struck.isSunk() );

    outcome.onResolved( hit, sunk ? struck.getShipType() : null,
                        defender.hasLost() );
  }

  /**
   * Concedes: with a lone human, the opponent takes the match (and the
   * defeat staging tells the truth about it); otherwise -- hot-seat or
   * spectating -- there is no single loser, so the game simply returns to
   * the title.
   */
  @Override
  public void forfeit()
  {
    Player local = getLocalPlayer();

    if ( local != null )
    {
      Player victor = ( board().getBritons() == local )
                    ? board().getFranks()
                    : board().getBritons();
      controller.showWinScreen( victor.getCiv() );
    }
    else
      controller.abandonToTitle();
  }
}
