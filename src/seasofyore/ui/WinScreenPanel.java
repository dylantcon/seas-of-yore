/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore.ui;

import seasofyore.core.Civilization;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

/**
 * The end-of-game screen, player-goal aware. Instead of a static image it
 * stages a mood-set {@link ChoppySeasPanel} -- calm golden seas, olive
 * branches, and the victor's flag for a win; a storm and the fallen player's
 * burning flag for a loss -- with the title and buttons floating in front.
 * The featured flag paints between the seas and the foreground content, so
 * the near waves lap over its base while the buttons stay on top.
 *
 * @author dylan
 */
public class WinScreenPanel extends JLayeredPane
{
  /**
   * Insets for the headline rows.
   */
  private static final Insets HEADER_P = new Insets( 24, 0, 6, 0 );

  /**
   * Insets for the button rows.
   */
  private static final Insets STD_P = new Insets( 10, 20, 10, 20 );

  /**
   * The shared menu palette, echoed here so the ending reads as part of the
   * same world as the title screens.
   */
  private static final Color INK = new Color( 18, 10, 28 );
  private static final Color GOLD = new Color( 232, 201, 124 );
  private static final Color PARCHMENT = new Color( 229, 213, 175 );
  private static final Color SEA_BLUE = new Color( 24, 58, 94 );
  private static final Color BLOOD_RED = new Color( 122, 24, 24 );

  /**
   * The defeat headline plate: ash-grey text on a dark, dried-blood ground.
   */
  private static final Color ASH = new Color( 200, 196, 188 );
  private static final Color DRIED_BLOOD = new Color( 52, 14, 14 );

  /**
   * Constructs the end screen for one outcome.
   *
   * @param featured     the civilization whose flag the scene features: the
   *                     celebrated winner, or -- in defeat -- the fallen player
   * @param featuredName the featured commander's spoken name (a human's
   *                     titled name, or an AI's tavern nickname)
   * @param defeat       true to stage the storm and burning flag; false for
   *                     the victory celebration
   * @param rtn          listener for the return-to-menu button
   * @param quit         listener for the quit button
   */
  public WinScreenPanel( Civilization featured, String featuredName,
                         boolean defeat, ActionListener rtn, ActionListener quit )
  {
    ChoppySeasPanel seas = new ChoppySeasPanel();
    if ( defeat )
      seas.showDefeat( featured );
    else
      seas.showVictory( featured );

    JPanel content = new JPanel( new GridBagLayout() );
    content.setOpaque( false );

    String headline = defeat ? "DEFEAT..." : "VICTORY!";
    String story = defeat
                 ? featuredName + "'s fleet rests beneath the waves..."
                 : featuredName + " rules the seas! The " + featured
                   + " banner flies high!";

    JLabel headlineLabel = makeChipLabel( headline,
        new Font( "Serif", Font.BOLD | Font.ITALIC, 54 ),
        defeat ? ASH : GOLD, defeat ? DRIED_BLOOD : INK );
    JLabel storyLabel = makeChipLabel( story,
        new Font( "Serif", Font.ITALIC, 18 ),
        defeat ? ASH : INK, defeat ? new Color( 30, 26, 24 ) : PARCHMENT );

    JButton returnButton = makeButton( "Return to Menu", Color.WHITE, SEA_BLUE );
    JButton quitButton = makeButton( "Quit to Desktop", PARCHMENT, BLOOD_RED );
    returnButton.addActionListener( rtn );
    quitButton.addActionListener( quit );

    // headline pinned high, buttons pinned low: the stretchy middle row is
    // deliberately empty so the flag scene shows through between them
    gblAdd( content, headlineLabel, 0, 0.0, HEADER_P );
    gblAdd( content, storyLabel, 1, 0.0, HEADER_P );
    gblAdd( content, new JLabel(), 2, 1.0, STD_P );
    gblAdd( content, returnButton, 3, 0.0, STD_P );
    gblAdd( content, quitButton, 4, 0.0,
            new Insets( 10, 20, 36, 20 ) );

    add( seas, JLayeredPane.DEFAULT_LAYER );
    add( content, JLayeredPane.PALETTE_LAYER );
  }

  /**
   * Sizes every child to fill this pane; JLayeredPane has no layout manager.
   */
  @Override
  public void doLayout()
  {
    for ( Component child : getComponents() )
      child.setBounds( 0, 0, getWidth(), getHeight() );
  }

  /**
   * Adds one row of the single centered column, with an optional vertical
   * stretch weight (used by the empty middle row that frames the flag).
   *
   * @param panel the GridBagLayout panel to add to
   * @param c     the component forming this row
   * @param gY    the row index
   * @param wY    the vertical weight (0 packs the row; 1 makes it stretch)
   * @param i     the insets around the row
   */
  private void gblAdd( JPanel panel, Component c, int gY, double wY, Insets i )
  {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = gY;
    gbc.weighty = wY;
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.insets = i;
    panel.add( c, gbc );
  }

  /**
   * Builds an opaque chip label, the same legibility device the menus use
   * over the animated seas.
   *
   * @param text the label text
   * @param font the font
   * @param fg   the text color
   * @param bg   the plate color
   * @return the styled label
   */
  private JLabel makeChipLabel( String text, Font font, Color fg, Color bg )
  {
    JLabel label = new JLabel( text );
    label.setFont( font );
    label.setForeground( fg );
    label.setBackground( bg );
    label.setOpaque( true );
    label.setBorder( BorderFactory.createEmptyBorder( 8, 24, 8, 24 ) );
    return label;
  }

  /**
   * Builds a menu-styled button with the shared fixed footprint.
   *
   * @param text the button text
   * @param fg   the text color
   * @param bg   the face color
   * @return the styled button
   */
  private JButton makeButton( String text, Color fg, Color bg )
  {
    JButton button = new JButton( text );
    button.setPreferredSize( new Dimension( 280, 60 ) );
    button.setFont( new Font( "Serif", Font.BOLD, 20 ) );
    button.setForeground( fg );
    button.setBackground( bg );
    button.setFocusable( false );
    return button;
  }
}
