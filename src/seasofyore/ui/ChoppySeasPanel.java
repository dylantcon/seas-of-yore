/**
 * The UI package, which contains all files related to the front-end of the game.
 */
package seasofyore.ui;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
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
 *
 * @author dylan
 */
public class ChoppySeasPanel extends JPanel
{
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
   * Colour at the very top of the sky gradient (deep twilight).
   */
  private static final Color SKY_TOP = new Color( 0x0B1E3B );

  /**
   * Colour at the horizon end of the sky gradient (paler, hazier blue).
   */
  private static final Color SKY_HORIZON = new Color( 0x3A5E84 );

  /**
   * The wave layers, ordered back (index 0, far/slow/pale) to front
   * (last index, near/fast/dark). They are painted in this order so each
   * nearer layer overlaps the ones behind it.
   */
  private final WaveLayer[] layers;

  /**
   * The timer that advances {@link #elapsedSeconds} and requests repaints.
   */
  private final Timer redrawTimer;

  /**
   * A shared, monotonically increasing clock (in seconds) that drives every
   * layer's phase. Using one analytic clock -- rather than per-layer pixel
   * offsets -- keeps the waves perfectly smooth and frame-rate independent.
   */
  private float elapsedSeconds = 0f;

  /**
   * Constructs the panel and immediately starts its animation timer.
   */
  public ChoppySeasPanel()
  {
    setOpaque( true );
    layers = buildLayers();

    redrawTimer = new Timer( MS_PER_SEC / REFRESH_HZ, this::onTick );
    redrawTimer.start();
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
    return new WaveLayer[]
    {
      //             base   amp    wavelen  speed  colour
      new WaveLayer( 0.42f, 0.025f, 0.55f,  0.50f, new Color( 0x2C4A63 ) ),
      new WaveLayer( 0.55f, 0.030f, 0.42f,  0.70f, new Color( 0x244056 ) ),
      new WaveLayer( 0.68f, 0.035f, 0.33f,  0.95f, new Color( 0x1C3548 ) ),
      new WaveLayer( 0.80f, 0.045f, 0.26f,  1.25f, new Color( 0x142A3A ) ),
      new WaveLayer( 0.92f, 0.055f, 0.20f,  1.60f, new Color( 0x0E2030 ) )
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
   * Paints the sky gradient and then every wave layer, back to front.
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

    // sky behind everything: vertical gradient from twilight to horizon haze
    g2.setPaint( new GradientPaint( 0, 0, SKY_TOP, 0, h, SKY_HORIZON ) );
    g2.fillRect( 0, 0, w, h );

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
    float baseY = layer.baseFraction * h;          // resting waterline
    float amp   = layer.amplitudeFrac * h;         // crest/trough swing
    // angular wavenumber k = 2*pi / wavelength; convert the fractional
    // wavelength into pixels first so the look scales with window width
    float k     = (float) ( 2 * Math.PI ) / ( layer.wavelengthFrac * w );
    float phase = layer.speed * elapsedSeconds;     // how far this layer has scrolled

    Polygon poly = new Polygon();

    // sample the surface left to right
    for ( int x = 0; x <= w + SAMPLE_STEP_PX; x += SAMPLE_STEP_PX )
    {
      // primary swell plus a faster, counter-moving ripple => choppy crests
      double swell  = Math.sin( k * x + phase );
      double ripple = 0.5 * Math.sin( 2.3 * k * x - 1.7 * phase );
      int y = (int) ( baseY - amp * ( swell + ripple ) );
      poly.addPoint( x, y );
    }

    // close the polygon down the right edge, along the bottom, up the left edge
    poly.addPoint( w + SAMPLE_STEP_PX, h );
    poly.addPoint( 0, h );

    g2.setColor( layer.color );
    g2.fillPolygon( poly );
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
