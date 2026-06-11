/**
 * The UI package, which contains all files related to the front-end of the game.
 */
package seasofyore.ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * A pre-rendered, palette-mapped "pixel water" loop: the battle screen's
 * replacement for the old animated water GIFs.
 * <p>
 * The GIFs were quietly expensive in three compounding ways. An animated GIF
 * painted through an {@link java.awt.image.ImageObserver} re-triggers a
 * repaint at the GIF's own, uncontrollable frame rate; each of those paints
 * re-scaled the decoded frame to the panel in software; and because the 200
 * cell panels and every ship sprite above the water are translucent, each
 * repaint recomposited all of them too. Under CheerpJ -- where Swing painting
 * and the network relay's events share the browser's one thread -- that
 * steady paint load could starve the relay long enough to drop the match.
 * <p>
 * This class moves all of that cost to construction time so each later frame
 * is a single image blit:
 * <ul>
 *   <li><b>Procedural, but offline.</b> The water surface is the interference
 *       pattern of three sine waves (the same trick ChoppySeasPanel plays in
 *       real time), but here it is sampled once into a small set of frames
 *       and never computed again.</li>
 *   <li><b>Tiny frames, chunky pixels.</b> Frames are rendered at
 *       {@value #TEX_W}x{@value #TEX_H} and stretched to the panel with
 *       nearest-neighbour interpolation, so one stored pixel becomes a fat
 *       square on screen. The retro look and the efficiency are the same
 *       decision: fewer source pixels to store, scale, and composite.</li>
 *   <li><b>A seamless loop.</b> Every wave crosses the frame an integer
 *       number of times in space and advances an integer number of cycles
 *       over the loop, so frame {@value #FRAME_COUNT} IS frame 0 -- the
 *       animation can run forever from {@value #FRAME_COUNT} cached images,
 *       exactly like a GIF, minus the decoding.</li>
 *   <li><b>One geometry, two palettes.</b> The light (friendly) and dark
 *       (enemy) waters share the same precomputed band indices and differ
 *       only in the colour table they are mapped through, halving the
 *       startup trigonometry.</li>
 * </ul>
 * The {@link #SUGGESTED_FRAME_MS} cadence (~6 fps) is deliberate: water
 * shimmer reads perfectly well at single-digit frame rates, and every frame
 * NOT drawn is EDT time handed back to the relay connection.
 *
 * @author dylan
 */
public final class PixelWaterAnimation
{
  /**
   * Frames in one seamless loop. More frames means smoother (and more
   * memory); twelve at ~6 fps gives a two-second swell that never visibly
   * "snaps" back to its start.
   */
  public static final int FRAME_COUNT = 12;

  /**
   * The timer delay (milliseconds) the board should drive this loop with:
   * 160 ms is about 6 fps, a fraction of what the GIFs forced.
   */
  public static final int SUGGESTED_FRAME_MS = 160;

  /**
   * Stored frame width in texels. The frame is stretched to the panel at
   * paint time, so this -- not the window size -- fixes the rendering cost.
   */
  private static final int TEX_W = 160;

  /**
   * Stored frame height in texels. Sized about 2:1 against the width to
   * roughly match a board half, keeping the fat pixels near-square.
   */
  private static final int TEX_H = 80;

  /**
   * The wave recipe: each row is one sine sheet crossing the frame as
   * { cyclesAcrossWidth, cyclesAcrossHeight, cyclesPerLoop }. All entries
   * are integers so the pattern tiles in space and loops in time. Two
   * counter-moving swells plus a faster half-strength ripple (see WEIGHTS)
   * is the same "choppiness" recipe as the menu seas.
   */
  private static final int[][] WAVES =
  {
    { 2,  1,  1 },
    { 4, -2, -1 },
    { 7,  3,  2 }
  };

  /**
   * The relative strength of each wave in {@link #WAVES}.
   */
  private static final double[] WEIGHTS = { 1.0, 1.0, 0.5 };

  /**
   * Height thresholds carving the wave field into colour bands. Heights run
   * roughly -2.5..2.5; everything above the last threshold becomes the rare,
   * moving sparkle band that sells the surface as water.
   */
  private static final double[] BANDS = { -1.1, -0.2, 0.8, 1.9 };

  /**
   * The friendly (lower) half's palette, trough to sparkle: the menu seas'
   * brighter blues, so both screens read as the same ocean.
   */
  private static final int[] LIGHT_PALETTE =
  {
    0x1C3548, 0x244C66, 0x2C5E7E, 0x3A7396, 0x9FC4DC
  };

  /**
   * The enemy (upper) half's palette, trough to sparkle: the same sea by
   * night, matching the old dark-water GIF's role.
   */
  private static final int[] DARK_PALETTE =
  {
    0x0A1724, 0x102438, 0x16304A, 0x1F3D5C, 0x2E4E6E
  };

  /**
   * The shared band-index maps, one byte per texel per frame, built lazily
   * by the first variant constructed and reused by the second.
   */
  private static byte[][] indexMaps;

  /**
   * The lazily built shared variants. The board is rebuilt for every match,
   * so the frames are cached at class level rather than per panel.
   */
  private static PixelWaterAnimation light;
  private static PixelWaterAnimation dark;

  /**
   * This variant's pre-rendered frames.
   */
  private final BufferedImage[] frames;

  /**
   * Builds (or returns) the friendly-water variant.
   *
   * @return the shared light-palette animation
   */
  public static PixelWaterAnimation lightWater()
  {
    if ( light == null )
      light = new PixelWaterAnimation( LIGHT_PALETTE );
    return light;
  }

  /**
   * Builds (or returns) the enemy-water variant.
   *
   * @return the shared dark-palette animation
   */
  public static PixelWaterAnimation darkWater()
  {
    if ( dark == null )
      dark = new PixelWaterAnimation( DARK_PALETTE );
    return dark;
  }

  /**
   * Renders every frame of one palette variant from the shared index maps.
   *
   * @param palette the colour table, one entry per band
   */
  private PixelWaterAnimation( int[] palette )
  {
    if ( indexMaps == null )
      indexMaps = buildIndexMaps();

    frames = new BufferedImage[ FRAME_COUNT ];
    int[] rgb = new int[ TEX_W * TEX_H ];

    for ( int f = 0; f < FRAME_COUNT; f++ )
    {
      byte[] map = indexMaps[f];
      for ( int i = 0; i < rgb.length; i++ )
        rgb[i] = palette[ map[i] ];

      frames[f] = new BufferedImage( TEX_W, TEX_H, BufferedImage.TYPE_INT_RGB );
      frames[f].setRGB( 0, 0, TEX_W, TEX_H, rgb, 0, TEX_W );
    }
  }

  /**
   * Samples the wave field for every frame and quantizes each texel's height
   * into a palette band. This is the only trigonometry the animation ever
   * does, and it runs once per session.
   *
   * @return one band-index map per frame
   */
  private static byte[][] buildIndexMaps()
  {
    byte[][] maps = new byte[ FRAME_COUNT ][ TEX_W * TEX_H ];

    for ( int f = 0; f < FRAME_COUNT; f++ )
    {
      for ( int y = 0; y < TEX_H; y++ )
      {
        for ( int x = 0; x < TEX_W; x++ )
        {
          double height = 0;
          for ( int wv = 0; wv < WAVES.length; wv++ )
          {
            // 2*pi * (integer cycles across each axis and around the loop)
            // => the sheet meets itself at every frame edge and at frame 0
            double theta = 2 * Math.PI
                         * ( WAVES[wv][0] * x / (double) TEX_W
                           + WAVES[wv][1] * y / (double) TEX_H
                           + WAVES[wv][2] * f / (double) FRAME_COUNT );
            height += WEIGHTS[wv] * Math.sin( theta );
          }

          maps[f][ y * TEX_W + x ] = bandOf( height );
        }
      }
    }
    return maps;
  }

  /**
   * Quantizes a wave height into its palette band.
   *
   * @param height the sampled surface height
   * @return the band index, 0 (trough) through BANDS.length (sparkle)
   */
  private static byte bandOf( double height )
  {
    byte band = 0;
    while ( band < BANDS.length && height > BANDS[band] )
      band++;
    return band;
  }

  /**
   * Stretch-blits one frame of the loop -- the entire per-paint cost of the
   * water. Nearest-neighbour interpolation is what keeps the stored texels
   * as crisp squares instead of smearing them; it is also the cheapest
   * scaling mode a renderer can offer.
   *
   * @param g     the graphics context to paint into
   * @param frame the loop frame to show (any int; wrapped modulo the loop)
   * @param x     the destination rectangle's x
   * @param y     the destination rectangle's y
   * @param w     the destination rectangle's width
   * @param h     the destination rectangle's height
   */
  public void paintFrame( Graphics g, int frame, int x, int y, int w, int h )
  {
    Graphics2D g2 = (Graphics2D) g;
    g2.setRenderingHint( RenderingHints.KEY_INTERPOLATION,
                         RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR );
    g2.drawImage( frames[ Math.floorMod( frame, FRAME_COUNT ) ],
                  x, y, w, h, null );
  }

  /**
   * Exposes one frame as a plain image, for static uses -- e.g. the
   * missing-sprite placeholder, which used to be the dark water GIF.
   *
   * @param frame the loop frame (any int; wrapped modulo the loop)
   * @return the frame image at texture resolution
   */
  public Image getFrame( int frame )
  {
    return frames[ Math.floorMod( frame, FRAME_COUNT ) ];
  }
}
