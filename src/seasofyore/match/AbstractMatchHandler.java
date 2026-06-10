package seasofyore.match;

import seasofyore.GameController;
import seasofyore.core.Board;

/**
 * Base class for match handlers: stores the controller binding and provides
 * the conveniences every locality flavour needs, plus do-nothing defaults
 * for the lifecycle notifications most handlers ignore.
 *
 * @author dylan
 */
public abstract class AbstractMatchHandler implements MatchHandler
{
  /**
   * The controller managing the match this handler serves.
   */
  protected GameController controller;

  /**
   * Binds this handler to a running game.
   *
   * @param controller the controller managing the match
   */
  @Override
  public void beginMatch( GameController controller )
  {
    this.controller = controller;
  }

  /**
   * Convenience accessor for the bound game's board.
   *
   * @return the board, or null before beginMatch
   */
  protected Board board()
  {
    return ( controller == null ) ? null : controller.getBoard();
  }

  /**
   * Default: nothing to forward when a turn ends.
   */
  @Override
  public void onTurnEnded()
  {
    // local handoffs need no plumbing
  }

  /**
   * Default: nothing to release.
   */
  @Override
  public void shutdown()
  {
    // no sockets, no threads, nothing held
  }
}
