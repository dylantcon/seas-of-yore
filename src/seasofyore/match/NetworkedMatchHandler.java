package seasofyore.match;

import seasofyore.core.Player;
import seasofyore.ui.TerminalPanel;

/**
 * Base class for every match played across a wire. What all networked
 * flavours share, regardless of transport:
 * <ul>
 *   <li>no curtain -- the remote opponent cannot see this screen anyway;</li>
 *   <li>no pausing -- the other side's match cannot be frozen from here
 *       (the pause key instead offers forfeit/disconnect);</li>
 *   <li>chat -- the terminal surfaces its chat line;</li>
 *   <li>exactly one local player -- the person at this screen.</li>
 * </ul>
 * Subclasses supply the transport: how to connect, how to send a turn, how
 * to carry chat. The remote player will be represented locally by a player
 * implementation that replays moves received from the wire, slotting into
 * the same phase system AI and human players use.
 *
 * @author dylan
 */
public abstract class NetworkedMatchHandler extends AbstractMatchHandler
{
  /**
   * Never: a remote opponent cannot peek at this screen.
   *
   * @return false, always
   */
  @Override
  public final boolean showsCurtain()
  {
    return false;
  }

  /**
   * Never: the remote side's match cannot be frozen from here.
   *
   * @return false, always
   */
  @Override
  public final boolean supportsPause()
  {
    return false;
  }

  /**
   * Always: chat is the point of having a terminal in a networked match.
   *
   * @return true, always
   */
  @Override
  public final boolean supportsChat()
  {
    return true;
  }

  /**
   * Establishes the connection for this match. Servers listen, clients
   * dial; both return once the opening handshake has settled who commands
   * which civilization.
   */
  public abstract void connect();

  /**
   * Tears the connection down deliberately.
   */
  public abstract void disconnect();

  /**
   * Sends a chat line to the remote player.
   *
   * @param message the text the local player typed
   */
  public abstract void sendChat( String message );

  /**
   * Delivers a chat line that arrived from the remote player into the
   * terminal, styled as a whisper across the water.
   *
   * @param sender  the remote player's name
   * @param message the chat text
   */
  protected final void receiveChat( String sender, String message )
  {
    controller.getTerminal().logMessage(
        TerminalPanel.CYAN + TerminalPanel.BOLD + "[" + sender + "]"
        + TerminalPanel.RESET + " " + TerminalPanel.CYAN + message
        + TerminalPanel.RESET );
  }

  /**
   * Forfeits by tearing down the connection and abandoning to the title.
   * Subclasses that can notify the remote side first should override,
   * notify, then delegate to this.
   */
  @Override
  public void forfeit()
  {
    disconnect();
    controller.abandonToTitle();
  }

  /**
   * The person at this screen. Settled during the connection handshake.
   *
   * @return the local player
   */
  @Override
  public abstract Player getLocalPlayer();

  /**
   * Whether this screen holds the active turn, per the synchronized match
   * state both ends maintain.
   *
   * @return true if the local player may act
   */
  @Override
  public abstract boolean isLocalTurn();
}
