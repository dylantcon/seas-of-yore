/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore;

import seasofyore.ui.TerminalPanel;

/**
 * The lull between a networked player finishing their placement and the
 * enemy finishing theirs. Both screens place simultaneously, so whoever
 * is done first waits here -- input disabled, message posted -- until the
 * match handler receives the enemy's GREADY and begins the battle.
 *
 * @author dylan
 */
public class WaitingForEnemyPhase extends AbstractGamePhase
{
  /**
   * Locks the screen down and announces the wait.
   */
  @Override
  protected void onEnter()
  {
    controller.getCurrentQuadrantPanel().disableCellInteraction();
    controller.getNextQuadrantPanel().disableCellInteraction();
    controller.getTerminalPanel().setTurnButtonEnabled( false );

    controller.logToTerminal( TerminalPanel.GREY + TerminalPanel.ITALIC
        + "Thy fleet stands ready. Awaiting the enemy's deployment..."
        + TerminalPanel.RESET );
  }
}
