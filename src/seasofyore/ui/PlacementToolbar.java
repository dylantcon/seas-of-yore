/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore.ui;

import seasofyore.GameController;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

/**
 * The ship-placement toolbar: the floating pair of buttons offered to a
 * human during setup, for scattering the fleet at random or sweeping the
 * board clean to start over. Extracted from the controller so the toolbar
 * owns its own construction and actions.
 *
 * @author dylan
 */
public final class PlacementToolbar extends JPanel
{
  /**
   * Path to the random placement button image.
   */
  private static final String RANDOM = "/images/random.png";

  /**
   * Path to the reset placement button image (trash icon).
   */
  private static final String GARBAGE = "/images/garbage.png";

  /**
   * Constructs the toolbar bound to a game.
   *
   * @param gc the controller managing the game
   */
  public PlacementToolbar( GameController gc )
  {
    super( new GridLayout( 4, 1 ) );
    setOpaque( false );

    add( Box.createVerticalGlue() );

    CustomButton randomizeButton = new CustomButton(
        new ImageIcon( getClass().getResource( RANDOM ) ) );
    randomizeButton.addActionListener( ( ActionEvent e ) ->
    {
      gc.getCurrentPlayer().randomVesselPlacement();
      gc.getSidebarPanel().allSlotsEnabled( false );
      gc.getTerminal().setTurnButtonEnabled( true );
      gc.repaint();
    });
    randomizeButton.setToolTipText("Random ships");
    add( randomizeButton );

    CustomButton garbageButton = new CustomButton(
        new ImageIcon( getClass().getResource( GARBAGE ) ) );
    garbageButton.addActionListener( ( ActionEvent e ) ->
    {
      gc.getCurrentPlayer().reset();
      gc.getCurrentPhase().cleanup();
      gc.getSidebarPanel().resetAllSlots();
      gc.getBoardPanel().getFriendlyPanel().enableCellInteraction();
      gc.repaint();
    });
    garbageButton.setToolTipText("Clear all");
    add( garbageButton );

    add( Box.createVerticalGlue() );
  }
}
