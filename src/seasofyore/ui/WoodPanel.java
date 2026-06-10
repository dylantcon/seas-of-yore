/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore.ui;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

/**
 * A panel whose surface is the ship-deck wood texture, stretched to fill its
 * bounds -- the same plank look the ship sidebar wears. An optional shade
 * darkens the wood with a translucent black wash, for surfaces that should
 * read as backdrop (like the turn curtain) rather than furniture.
 *
 * @author dylan
 */
public class WoodPanel extends JPanel
{
  /**
   * Path to the shared wood texture resource.
   */
  private static final String WOODPATH = "/images/wood.png";

  /**
   * The wood texture image.
   */
  private final ImageIcon wood;

  /**
   * Alpha (0-255) of the darkening wash painted over the wood; 0 = bare.
   */
  private final int shadeAlpha;

  /**
   * Constructs a bare wooden panel.
   */
  public WoodPanel()
  {
    this( 0 );
  }

  /**
   * Constructs a wooden panel with a darkening wash.
   *
   * @param shadeAlpha the wash alpha, 0 (bare wood) to 255 (black)
   */
  public WoodPanel( int shadeAlpha )
  {
    this.wood = new ImageIcon( getClass().getResource( WOODPATH ) );
    this.shadeAlpha = Math.max( 0, Math.min( 255, shadeAlpha ) );
    setOpaque( true );
  }

  /**
   * Paints the stretched wood texture and, if configured, its dark wash.
   *
   * @param g the graphics context
   */
  @Override
  protected void paintComponent( Graphics g )
  {
    super.paintComponent( g );
    g.drawImage( wood.getImage(), 0, 0, getWidth(), getHeight(), this );

    if ( shadeAlpha > 0 )
    {
      g.setColor( new Color( 0, 0, 0, shadeAlpha ) );
      g.fillRect( 0, 0, getWidth(), getHeight() );
    }
  }
}
