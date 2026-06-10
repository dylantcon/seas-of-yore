/**
 * The UI package, which contains all files related to the front-end of the game.
 */
package seasofyore.ui;

import seasofyore.core.Civilization;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * A self-animating background panel that renders a layered, parallax-scrolling
 * "choppy seas" effect. It is meant to sit behind a transparent menu so every
 * screen shares one continuously moving backdrop.
 * <p>
 * The principle is the same as a classic parallax scroller: several wave layers
 * are drawn back-to-front, and each <em>nearer</em> (lower, darker) layer scrolls
 * <em>faster</em> than the ones behind it. The brain reads that speed difference
 * as depth. A single Swing {@link Timer} advances a shared clock and triggers a
 * repaint; nothing else needs to poke the panel.
 * <p>
 * Each wave's surface is the sum of two sine waves of different frequency moving
 * in opposite directions. A single sine is too smooth and regular to look like
 * the sea; adding a faster, counter-moving ripple breaks the symmetry and gives
 * the crests their irregular, "choppy" character.
 * <p>
 * The panel is also <em>mood-aware</em>, so one component serves the menus and
 * both endings of a game:
 * <ul>
 *   <li>{@link #showNormal() NORMAL} -- the twilight menu backdrop;</li>
 *   <li>{@link #showVictory VICTORY} -- calm golden seas, the victor's flag
 *       flying tall with olive branches behind it;</li>
 *   <li>{@link #showDefeat DEFEAT} -- a storm: dark sky, drifting clouds,
 *       sharp-capped waves, and the fallen player's flag scorched and
 *       burning.</li>
 * </ul>
 * The flag and its dressings paint between the sky and the wave layers, so the
 * nearer waves lap over its base -- and any foreground UI (buttons, labels)
 * naturally sits in front of everything.
 *
 * @author dylan
 */
public class ChoppySeasPanel extends JPanel
{
  /**
   * The three renderable moods of the seas.
   */
  public enum SeaMood
  {
    /** The everyday twilight backdrop used behind the menus. */
    NORMAL,
    /** Calm, golden, celebratory: the featured flag flies undamaged. */
    VICTORY,
    /** Stormy and dangerous: the featured flag burns. */
    DEFEAT
  }

  /**
   * Target redraw rate. 60 ticks/second is smooth and cheap for flat fills.
   */
  private static final int REFRESH_HZ = 60;

  /**
   * Milliseconds in a second, for converting the refresh rate to a Timer delay.
   */
  private static final int MS_PER_SEC = 1000;

  /**
   * Horizontal spacing, in pixels, between samples of each wave curve. Smaller
   * is smoother but costs more points per polygon; 6px is imperceptibly jagged.
   */
  private static final int SAMPLE_STEP_PX = 6;

  /**
   * Colour at the very top of the normal sky gradient (deep twilight).
   */
  private static final Color SKY_TOP = new Color( 0x0B1E3B );

  /**
   * Colour at the horizon end of the normal sky gradient (paler, hazier blue).
   */
  private static final Color SKY_HORIZON = new Color( 0x3A5E84 );

  /**
   * Victory sky: a clear day fading to golden haze at the waterline.
   */
  private static final Color SKY_TOP_VICTORY = new Color( 0x6FA0CF );
  private static final Color SKY_HORIZON_VICTORY = new Color( 0xF4DDA5 );

  /**
   * Defeat sky: near-black storm light over a grim grey horizon.
   */
  private static final Color SKY_TOP_DEFEAT = new Color( 0x14161C );
  private static final Color SKY_HORIZON_DEFEAT = new Color( 0x39414B );

  /**
   * The flag's width as a fraction of the panel width when a mood features one.
   */
  private static final float FLAG_W_FRAC = 0.44f;

  /**
   * The flag's vertical center as a fraction of the panel height. Low enough
   * that the rear wave layers overlap its base, planting it "in" the sea.
   */
  private static final float FLAG_CY_FRAC = 0.30f;

  /**
   * Flame anchors on the burning flag, as fractions of the flag rectangle,
   * with a per-flame flicker speed (Hz-ish) so no two flames pulse together.
   * Encoded as { xFrac, yFrac, flickerSpeed }.
   */
  private static final float[][] FLAMES =
  {
    { 0.14f, 0.30f, 5.1f },
    { 0.50f, 0.12f, 6.3f },
    { 0.86f, 0.34f, 4.6f },
    { 0.30f, 0.78f, 5.7f },
    { 0.72f, 0.70f, 6.9f }
  };

  /**
   * Storm cloud clusters for the defeat sky, encoded as
   * { xFrac, yFrac, sizeFrac of width, driftFrac of width per second }.
   */
  private static final float[][] CLOUDS =
  {
    { 0.10f, 0.10f, 0.16f, 0.014f },
    { 0.45f, 0.05f, 0.21f, 0.020f },
    { 0.78f, 0.14f, 0.13f, 0.011f }
  };

  /**
   * Puff offsets composing one cloud cluster, in units of the cluster size:
   * { dxFrac, dyFrac, scale }.
   */
  private static final float[][] CLOUD_PUFFS =
  {
    {  0.00f,  0.00f, 1.00f },
    { -0.70f,  0.18f, 0.72f },
    {  0.65f,  0.22f, 0.68f },
    {  0.05f, -0.28f, 0.62f },
    { -0.35f, -0.16f, 0.55f },
    {  0.38f, -0.12f, 0.58f }
  };

  /**
   * The wave layers, ordered back (index 0, far/slow/pale) to front
   * (last index, near/fast/dark). They are painted in this order so each
   * nearer layer overlaps the ones behind it.
   */
  private final WaveLayer[] layers;

  /**
   * The timer that advances {@link #elapsedSeconds} and requests repaints.
   * Started and stopped with the panel's presence in a displayable hierarchy
   * (see {@link #addNotify()}/{@link #removeNotify()}), so discarded panels
   * -- e.g. a dismissed win screen -- do not animate forever.
   */
  private final Timer redrawTimer;

  /**
   * A shared, monotonically increasing clock (in seconds) that drives every
   * layer's phase. Using one analytic clock -- rather than per-layer pixel
   * offsets -- keeps the waves perfectly smooth and frame-rate independent.
   */
  private float elapsedSeconds = 0f;

  /**
   * The current mood of the seas.
   */
  private SeaMood mood = SeaMood.NORMAL;

  /**
   * The featured civilization's flag, or null when the mood shows no flag.
   */
  private ImageIcon flag;

  /**
   * Constructs the panel in the NORMAL mood. The animation runs whenever the
   * panel is part of a displayable hierarchy.
   */
  public ChoppySeasPanel()
  {
    setOpaque( true );
    layers = buildLayers();

    redrawTimer = new Timer( MS_PER_SEC / REFRESH_HZ, this::onTick );
  }

  /**
   * Switches to the celebratory variation: calm golden seas, with the given
   * civilization's flag flying undamaged before a wreath of olive branches.
   *
   * @param civ the civilization being celebrated
   */
  public void showVictory( Civilization civ )
  {
    mood = SeaMood.VICTORY;
    flag = loadFlag( civ );
    repaint();
  }

  /**
   * Switches to the stormy variation: dark sky, drifting storm clouds,
   * sharp-capped waves, and the given civilization's flag scorched brown and
   * burning in several places.
   *
   * @param civ the civilization whose fleet was lost
   */
  public void showDefeat( Civilization civ )
  {
    mood = SeaMood.DEFEAT;
    flag = loadFlag( civ );
    repaint();
  }

  /**
   * Returns to the everyday twilight backdrop with no flag.
   */
  public void showNormal()
  {
    mood = SeaMood.NORMAL;
    flag = null;
    repaint();
  }

  /**
   * Loads the flag image for a civilization from the bundled resources.
   *
   * @param civ the civilization
   * @return the flag icon
   */
  private ImageIcon loadFlag( Civilization civ )
  {
    return new ImageIcon( getClass().getResource( "/images/" + civ + ".png" ) );
  }

  /**
   * Starts animating when the panel joins a displayable hierarchy.
   */
  @Override
  public void addNotify()
  {
    super.addNotify();
    redrawTimer.start();
  }

  /**
   * Stops animating when the panel leaves the hierarchy, so a discarded
   * panel (and everything it references) can actually be collected.
   */
  @Override
  public void removeNotify()
  {
    redrawTimer.stop();
    super.removeNotify();
  }

  /**
   * Defines the stack of wave layers. Going from back to front, each layer sits
   * lower on the panel, swings with a larger amplitude, has a shorter
   * wavelength, scrolls faster, and is painted a darker blue -- all cues that
   * read as "closer to the viewer".
   *
   * @return the ordered array of wave layers
   */
  private WaveLayer[] buildLayers()
  {
    // NOTE: a layer's surface swings as far as 1.5x its amplitude below its
    // baseline (swell + half-strength ripple), and the DEFEAT mood scales
    // amplitudes by up to 1.5x again. Keep baseFraction + 2.25 * amplitudeFrac
    // under 1.0 or the layer's surface dips below the panel's bottom edge and
    // the layer behind it peeks through at every trough.
    return new WaveLayer[]
    {
      //             base   amp    wavelen  speed  colour
      new WaveLayer( 0.42f, 0.025f, 0.55f,  0.50f, new Color( 0x2C4A63 ) ),
      new WaveLayer( 0.55f, 0.030f, 0.42f,  0.70f, new Color( 0x244056 ) ),
      new WaveLayer( 0.68f, 0.035f, 0.33f,  0.95f, new Color( 0x1C3548 ) ),
      new WaveLayer( 0.80f, 0.045f, 0.26f,  1.25f, new Color( 0x142A3A ) ),
      new WaveLayer( 0.90f, 0.040f, 0.20f,  1.60f, new Color( 0x0E2030 ) )
    };
  }

  /**
   * Timer callback: advance the shared clock by the real tick interval and ask
   * Swing to repaint. Using the timer's own delay (rather than assuming a fixed
   * step) keeps motion consistent even if the timer fires late.
   *
   * @param e the timer event (unused beyond identifying the tick)
   */
  private void onTick( ActionEvent e )
  {
    elapsedSeconds += (float) redrawTimer.getDelay() / MS_PER_SEC;
    repaint();
  }

  /**
   * Paints the scene back to front: sky, then the mood's set dressing (storm
   * clouds, wreath, flag, flames), then every wave layer.
   *
   * @param g the Graphics context supplied by Swing
   */
  @Override
  protected void paintComponent( Graphics g )
  {
    super.paintComponent( g );

    // work on a private copy so our hint/colour changes never leak out
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                         RenderingHints.VALUE_ANTIALIAS_ON );

    int w = getWidth();
    int h = getHeight();

    // sky behind everything: vertical gradient from zenith to horizon
    g2.setPaint( new GradientPaint( 0, 0, skyTop(), 0, h, skyHorizon() ) );
    g2.fillRect( 0, 0, w, h );

    if ( mood == SeaMood.DEFEAT )
      paintStormClouds( g2, w );

    if ( flag != null )
      paintFlag( g2, w, h );

    // back-to-front: each nearer layer paints over the ones behind it
    for ( WaveLayer layer : layers )
      paintWaveLayer( g2, layer, w, h );

    g2.dispose();
  }

  /**
   * Fills a single wave layer as a closed polygon: the choppy surface curve
   * across the top, then straight down to the panel's bottom corners.
   *
   * @param g2    the (configured) graphics context
   * @param layer the layer to draw
   * @param w     the current panel width in pixels
   * @param h     the current panel height in pixels
   */
  private void paintWaveLayer( Graphics2D g2, WaveLayer layer, int w, int h )
  {
    float baseY = layer.baseFraction * h;             // resting waterline
    float amp   = layer.amplitudeFrac * h * ampScale(); // crest/trough swing
    // angular wavenumber k = 2*pi / wavelength; convert the fractional
    // wavelength into pixels first so the look scales with window width
    float k     = (float) ( 2 * Math.PI ) / ( layer.wavelengthFrac * w );
    float phase = layer.speed * speedScale() * elapsedSeconds;

    Polygon poly = new Polygon();

    // sample the surface left to right
    for ( int x = 0; x <= w + SAMPLE_STEP_PX; x += SAMPLE_STEP_PX )
    {
      // primary swell plus a faster, counter-moving ripple => choppy crests
      double swell  = waveShape( k * x + phase );
      double ripple = 0.5 * waveShape( 2.3 * k * x - 1.7 * phase );
      int y = (int) ( baseY - amp * ( swell + ripple ) );
      poly.addPoint( x, y );
    }

    // close the polygon down the right edge, along the bottom, up the left edge
    poly.addPoint( w + SAMPLE_STEP_PX, h );
    poly.addPoint( 0, h );

    g2.setColor( moodTint( layer.color ) );
    g2.fillPolygon( poly );
  }

  /**
   * The waveform sample for the current mood. NORMAL and VICTORY use the pure
   * sine; DEFEAT slews it toward a triangle wave -- the piecewise-linear
   * {@code (2/pi) * asin(sin theta)}, whose zeros and peaks line up with the
   * sine it replaces -- so the rounded swells become sharp, dangerous caps.
   *
   * @param theta the wave argument in radians
   * @return the surface displacement in [-1, 1]
   */
  private double waveShape( double theta )
  {
    double sine = Math.sin( theta );
    double sharpness = ( mood == SeaMood.DEFEAT ) ? 0.85 : 0.0;

    if ( sharpness == 0.0 )
      return sine;

    double triangle = 2.0 / Math.PI * Math.asin( sine );
    return ( 1.0 - sharpness ) * sine + sharpness * triangle;
  }

  /**
   * The sky's zenith colour for the current mood.
   *
   * @return the top gradient colour
   */
  private Color skyTop()
  {
    switch ( mood )
    {
      case VICTORY: return SKY_TOP_VICTORY;
      case DEFEAT:  return SKY_TOP_DEFEAT;
      default:      return SKY_TOP;
    }
  }

  /**
   * The sky's horizon colour for the current mood.
   *
   * @return the bottom gradient colour
   */
  private Color skyHorizon()
  {
    switch ( mood )
    {
      case VICTORY: return SKY_HORIZON_VICTORY;
      case DEFEAT:  return SKY_HORIZON_DEFEAT;
      default:      return SKY_HORIZON;
    }
  }

  /**
   * The amplitude multiplier for the current mood: heavy swell in a storm,
   * gentle in celebration.
   *
   * @return the amplitude scale factor
   */
  private float ampScale()
  {
    switch ( mood )
    {
      case VICTORY: return 0.65f;
      case DEFEAT:  return 1.50f;
      default:      return 1.0f;
    }
  }

  /**
   * The scroll-speed multiplier for the current mood: driven in a storm,
   * lazy in calm water.
   *
   * @return the speed scale factor
   */
  private float speedScale()
  {
    switch ( mood )
    {
      case VICTORY: return 0.75f;
      case DEFEAT:  return 1.60f;
      default:      return 1.0f;
    }
  }

  /**
   * Re-tints a layer colour for the current mood: brightened toward warm
   * daylight for victory, crushed toward black-green storm water for defeat.
   *
   * @param base the layer's normal colour
   * @return the mood-adjusted colour
   */
  private Color moodTint( Color base )
  {
    switch ( mood )
    {
      case VICTORY: return mix( base, new Color( 0x4D88B0 ), 0.35f );
      case DEFEAT:  return mix( base, new Color( 0x0C1410 ), 0.45f );
      default:      return base;
    }
  }

  /**
   * Linearly interpolates between two colours.
   *
   * @param a    the base colour
   * @param b    the target colour
   * @param frac how far to move from a toward b, in [0, 1]
   * @return the mixed colour
   */
  private static Color mix( Color a, Color b, float frac )
  {
    return new Color(
      Math.round( a.getRed()   + ( b.getRed()   - a.getRed()   ) * frac ),
      Math.round( a.getGreen() + ( b.getGreen() - a.getGreen() ) * frac ),
      Math.round( a.getBlue()  + ( b.getBlue()  - a.getBlue()  ) * frac ) );
  }

  /**
   * Paints the drifting storm clouds of the defeat sky. Each cluster of
   * overlapping puffs scrolls slowly rightward and wraps around, with its own
   * size and speed so the sky never repeats exactly.
   *
   * @param g2 the graphics context
   * @param w  the panel width in pixels
   */
  private void paintStormClouds( Graphics2D g2, int w )
  {
    g2.setColor( new Color( 38, 43, 51, 235 ) );

    for ( float[] cloud : CLOUDS )
    {
      float size = cloud[2] * w;
      // drift right and wrap: the modulo runs over (panel + cloud) widths so
      // the cluster fully exits one side before re-entering the other
      float span = w + 2 * size;
      float x = ( cloud[0] * w + cloud[3] * w * elapsedSeconds ) % span - size;
      float y = cloud[1] * getHeight();

      for ( float[] puff : CLOUD_PUFFS )
      {
        float pw = size * puff[2];
        float ph = pw * 0.62f;
        g2.fillOval( Math.round( x + puff[0] * size - pw / 2 ),
                     Math.round( y + puff[1] * size - ph / 2 ),
                     Math.round( pw ), Math.round( ph ) );
      }
    }
  }

  /**
   * Paints the featured flag (and its mood dressing) between the sky and the
   * waves: olive branches behind an undamaged flag for victory, or a
   * scorch-browned, burning flag for defeat.
   *
   * @param g2 the graphics context
   * @param w  the panel width in pixels
   * @param h  the panel height in pixels
   */
  private void paintFlag( Graphics2D g2, int w, int h )
  {
    int imgW = flag.getIconWidth();
    int imgH = flag.getIconHeight();
    if ( imgW <= 0 || imgH <= 0 )
      return;

    int flagW = Math.round( FLAG_W_FRAC * w );
    int flagH = Math.round( (float) flagW * imgH / imgW );
    int flagX = ( w - flagW ) / 2;
    int flagY = Math.round( FLAG_CY_FRAC * h - flagH / 2f );

    if ( mood == SeaMood.VICTORY )
      paintOliveWreath( g2, w / 2, flagY + flagH / 2, flagW );

    g2.drawImage( flag.getImage(), flagX, flagY, flagW, flagH, this );

    if ( mood == SeaMood.DEFEAT )
    {
      // a light brown wash reads as scorched, smoke-stained cloth
      g2.setColor( new Color( 94, 66, 28, 90 ) );
      g2.fillRect( flagX, flagY, flagW, flagH );

      paintFlames( g2, flagX, flagY, flagW, flagH );
    }
  }

  /**
   * Paints the two olive branches that arc around the victorious flag,
   * opening at the top like a laurel wreath. Each branch is a curved stem
   * with elongated leaf ellipses planted along it, every leaf rotated to its
   * point on the curve via an AffineTransform.
   *
   * @param g2    the graphics context
   * @param cx    the wreath center x (the flag's center)
   * @param cy    the wreath center y
   * @param flagW the flag width the wreath is sized against
   */
  private void paintOliveWreath( Graphics2D g2, int cx, int cy, int flagW )
  {
    float radius = 0.62f * flagW;
    paintOliveBranch( g2, cx, cy, radius, false ); // right branch
    paintOliveBranch( g2, cx, cy, radius, true );  // left branch
  }

  /**
   * Paints one olive branch: a stem swept along a circular arc from the
   * wreath's bottom toward one upper side, with alternating inner/outer
   * leaves along its length.
   *
   * @param g2     the graphics context
   * @param cx     the wreath center x
   * @param cy     the wreath center y
   * @param radius the wreath radius
   * @param left   true for the left branch, false for the right
   */
  private void paintOliveBranch( Graphics2D g2, int cx, int cy, float radius,
                                 boolean left )
  {
    // screen angles: 90 degrees is straight down; sweep up one side to just
    // past horizontal, leaving the wreath open at the top
    double from = Math.toRadians( 90 );
    double to = Math.toRadians( left ? 200 : -20 );
    int leaves = 9;

    g2.setStroke( new BasicStroke( Math.max( 2f, radius * 0.035f ),
                                   BasicStroke.CAP_ROUND,
                                   BasicStroke.JOIN_ROUND ) );

    double prevX = 0;
    double prevY = 0;

    for ( int i = 0; i <= leaves; i++ )
    {
      double a = from + ( to - from ) * i / leaves;
      double x = cx + radius * Math.cos( a );
      double y = cy + radius * Math.sin( a );

      // stem segment connecting consecutive arc points
      if ( i > 0 )
      {
        g2.setColor( new Color( 0x55, 0x6B, 0x2F ) );
        g2.drawLine( (int) prevX, (int) prevY, (int) x, (int) y );
      }
      prevX = x;
      prevY = y;

      if ( i == 0 )
        continue; // no leaf on the shared bottom point

      // each leaf is an ellipse rotated to lie along the arc's tangent,
      // tilted alternately to the inside and outside of the stem
      double tangent = a + Math.PI / 2;
      double tilt = ( ( i % 2 == 0 ) ? 1 : -1 ) * Math.toRadians( 35 );
      float leafLen = radius * 0.30f;
      float leafWid = radius * 0.10f;

      AffineTransform old = g2.getTransform();
      g2.translate( x, y );
      g2.rotate( tangent + tilt );
      g2.setColor( ( i % 2 == 0 ) ? new Color( 0x6B, 0x8E, 0x23 )
                                  : new Color( 0x80, 0x80, 0x00 ) );
      g2.fillOval( Math.round( -leafLen / 2 ), Math.round( -leafWid / 2 ),
                   Math.round( leafLen ), Math.round( leafWid ) );
      g2.setTransform( old );
    }
  }

  /**
   * Paints the scattered flames consuming the defeated flag. Each flame sits
   * at a fixed anchor on the cloth and is built from three stacked ellipses
   * (red-orange body, orange mid, yellow core) whose height flickers and
   * whose core sways on its own clock, over a dark scorch mark.
   *
   * @param g2    the graphics context
   * @param flagX the flag rectangle's x
   * @param flagY the flag rectangle's y
   * @param flagW the flag rectangle's width
   * @param flagH the flag rectangle's height
   */
  private void paintFlames( Graphics2D g2, int flagX, int flagY,
                            int flagW, int flagH )
  {
    for ( int i = 0; i < FLAMES.length; i++ )
    {
      float[] f = FLAMES[i];
      float ax = flagX + f[0] * flagW;
      float ay = flagY + f[1] * flagH;

      // every flame breathes on its own frequency, with a phase spread so
      // the fire never pulses in unison
      double flick = 0.72 + 0.28 * Math.sin( elapsedSeconds * f[2] + i * 1.9 );
      double sway = Math.sin( elapsedSeconds * 7.3 + i * 2.1 );

      float fw = flagW * 0.085f;
      float fh = (float) ( fw * 1.9f * flick );

      // scorch mark where the cloth has burned through
      g2.setColor( new Color( 28, 20, 14, 160 ) );
      g2.fillOval( Math.round( ax - fw * 0.85f ), Math.round( ay - fw * 0.30f ),
                   Math.round( fw * 1.7f ), Math.round( fw * 0.6f ) );

      // body, mid, and core of the flame, narrowing and brightening upward
      g2.setColor( new Color( 226, 88, 34, 235 ) );
      g2.fillOval( Math.round( ax - fw / 2 ), Math.round( ay - fh * 0.95f ),
                   Math.round( fw ), Math.round( fh ) );

      g2.setColor( new Color( 244, 140, 38, 235 ) );
      g2.fillOval( Math.round( ax - fw * 0.32f + (float) sway * fw * 0.10f ),
                   Math.round( ay - fh * 0.75f ),
                   Math.round( fw * 0.64f ), Math.round( fh * 0.70f ) );

      g2.setColor( new Color( 255, 214, 92, 240 ) );
      g2.fillOval( Math.round( ax - fw * 0.18f + (float) sway * fw * 0.18f ),
                   Math.round( ay - fh * 0.55f ),
                   Math.round( fw * 0.36f ), Math.round( fh * 0.50f ) );
    }
  }

  /**
   * An immutable description of one parallax wave layer. Bundling the tuning
   * parameters keeps {@link #buildLayers()} readable and makes each layer's
   * "personality" (height, choppiness, speed, colour) easy to adjust.
   */
  private static final class WaveLayer
  {
    /** Resting waterline as a fraction of panel height (0 = top, 1 = bottom). */
    final float baseFraction;
    /** Wave amplitude as a fraction of panel height. */
    final float amplitudeFrac;
    /** Primary wavelength as a fraction of panel width. */
    final float wavelengthFrac;
    /** Horizontal phase speed in radians per second (parallax: bigger = nearer). */
    final float speed;
    /** Fill colour for this layer's water body. */
    final Color color;

    WaveLayer( float baseFraction, float amplitudeFrac, float wavelengthFrac,
               float speed, Color color )
    {
      this.baseFraction   = baseFraction;
      this.amplitudeFrac  = amplitudeFrac;
      this.wavelengthFrac = wavelengthFrac;
      this.speed          = speed;
      this.color          = color;
    }
  }
}
