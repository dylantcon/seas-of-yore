/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore.ui;

import seasofyore.GameController;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The transparent pause overlay, in the same spirit as the javarominoes
 * pause menu: a dim scrim over the frozen battle with a packed column of
 * options. It adapts to the match's locality:
 * <ul>
 *   <li><b>Offline</b> (pause supported): the game truly freezes -- stones
 *       hang mid-air -- and the menu offers Resume, Save, Return to Title,
 *       and Quit.</li>
 *   <li><b>Networked</b> (pause unsupported): the battle keeps raging
 *       behind the scrim, and the menu offers only Forfeit &amp; Disconnect
 *       or a quick return to it.</li>
 * </ul>
 *
 * @author dylan
 */
public final class PauseMenuPanel extends JPanel
{
  /**
   * The shared menu palette.
   */
  private static final Color INK = new Color( 18, 10, 28 );
  private static final Color GOLD = new Color( 232, 201, 124 );
  private static final Color PARCHMENT = new Color( 229, 213, 175 );
  private static final Color SEA_BLUE = new Color( 24, 58, 94 );
  private static final Color BLOOD_RED = new Color( 122, 24, 24 );

  /**
   * Menu spacing, mirroring the title screens.
   */
  private static final Insets HEADER_P = new Insets( 20, 0, 20, 0 );
  private static final Insets STD_P = new Insets( 10, 20, 10, 20 );

  /**
   * The scrim's darkness over the battle behind.
   */
  private static final Color SCRIM = new Color( 0, 0, 0, 160 );

  /**
   * Constructs the overlay for the given match locality.
   *
   * @param controller the game controller managing the match
   * @param fullPause  true when the match is truly frozen (offline); false
   *                   for the networked forfeit-only variant
   */
  public PauseMenuPanel( GameController controller, boolean fullPause )
  {
    setLayout( new GridBagLayout() );
    setOpaque( false );

    if ( fullPause )
      buildPauseMenu( controller );
    else
      buildForfeitMenu( controller );
  }

  /**
   * Builds the offline variant: the match is frozen and every local option
   * is on the table.
   *
   * @param gc the game controller
   */
  private void buildPauseMenu( GameController gc )
  {
    JLabel title = makeChipLabel( "PAUSED",
        new Font( "Serif", Font.BOLD | Font.ITALIC, 48 ), GOLD, INK );

    JButton resumeButton = makeButton( "Resume", Color.WHITE, SEA_BLUE );
    JButton saveButton = makeButton( "Save Game", INK, PARCHMENT );
    JButton titleButton = makeButton( "Return to Title", INK, PARCHMENT );
    JButton quitButton = makeButton( "Quit to Desktop", PARCHMENT, BLOOD_RED );

    resumeButton.addActionListener( e -> gc.togglePause() );
    saveButton.addActionListener( e -> gc.saveGameViaDialog() );
    titleButton.addActionListener( e -> gc.abandonToTitle() );
    quitButton.addActionListener( e -> System.exit( 0 ) );

    // mid-placement saves would desync the sidebar's slot bookkeeping on
    // load, so the log may only be written once the fleets are settled
    if ( !gc.getBoard().isPlacementFinal() )
    {
      saveButton.setEnabled( false );
      saveButton.setToolTipText( "The log cannot be written 'til both fleets are moored." );
    }

    gblAdd( title, 0, HEADER_P );
    gblAdd( resumeButton, 1, STD_P );
    gblAdd( saveButton, 2, STD_P );
    gblAdd( titleButton, 3, STD_P );
    gblAdd( quitButton, 4, STD_P );
  }

  /**
   * Builds the networked variant: the match cannot freeze, so the only
   * decisions are to keep fighting or to strike the colours.
   *
   * @param gc the game controller
   */
  private void buildForfeitMenu( GameController gc )
  {
    JLabel title = makeChipLabel( "THE BATTLE RAGES ON",
        new Font( "Serif", Font.BOLD | Font.ITALIC, 40 ), GOLD, INK );
    JLabel note = makeChipLabel( "A match across the water cannot be paused.",
        new Font( "Serif", Font.ITALIC, 16 ), INK, PARCHMENT );

    JButton backButton = makeButton( "Back to Battle", Color.WHITE, SEA_BLUE );
    JButton forfeitButton = makeButton( "Forfeit & Disconnect", PARCHMENT, BLOOD_RED );

    backButton.addActionListener( e -> gc.togglePause() );
    forfeitButton.addActionListener( e -> gc.getMatchHandler().forfeit() );

    gblAdd( title, 0, HEADER_P );
    gblAdd( note, 1, HEADER_P );
    gblAdd( backButton, 2, STD_P );
    gblAdd( forfeitButton, 3, STD_P );
  }

  /**
   * Adds one row of the packed, centered column.
   *
   * @param c  the component forming this row
   * @param gY the row index
   * @param i  the insets around the row
   */
  private void gblAdd( java.awt.Component c, int gY, Insets i )
  {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = gY;
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.insets = i;
    add( c, gbc );
  }

  /**
   * Builds an opaque chip label, the menus' legibility device.
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

  /**
   * Dims the frozen (or raging) battle behind the options.
   *
   * @param g the graphics context
   */
  @Override
  protected void paintComponent( Graphics g )
  {
    g.setColor( SCRIM );
    g.fillRect( 0, 0, getWidth(), getHeight() );
    super.paintComponent( g );
  }
}
