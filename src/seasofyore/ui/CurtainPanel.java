/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * The hot-seat secrecy curtain: a dark wooden bulkhead that sweeps down to
 * hide the boards during a two-human turn handoff and sweeps back up when
 * the next player is ready. The panel owns its whole performance -- the
 * exponential motion (the falling stone's treatment: each sweep eases away
 * from rest and accelerates), the closed/closing state, the RAISE THE
 * CURTAIN button, and the completion callback that lets the game swap turns
 * the instant the stage is fully hidden.
 * <p>
 * The host component (the game's layered pane) supplies the geometry; the
 * curtain positions itself against the host's current size on every frame
 * via {@link #updateBounds()}.
 *
 * @author dylan
 */
public final class CurtainPanel extends WoodPanel
{
  /**
   * The classic curtain speed in px/frame, reused as the base of the
   * exponential: each frame moves BASE^(frame / TENFOLD) pixels.
   */
  private static final double BASE_SPEED = 10.0;

  /**
   * Frames for the sweep's speed to grow by another factor of the base.
   */
  private static final int TENFOLD_FRAMES = 40;

  /**
   * Milliseconds between animation frames.
   */
  private static final int FRAME_MS = 5;

  /**
   * The darkening wash over the wood, so the button reads clearly.
   */
  private static final int CURTAIN_SHADE = 110;

  /**
   * The component the curtain covers; supplies width and height.
   */
  private final JComponent host;

  /**
   * The timer driving the current sweep.
   */
  private final Timer motionTimer;

  /**
   * Indicates if the curtain is fully closed.
   */
  private boolean closedState = false;

  /**
   * Indicates if the curtain is in the process of closing.
   */
  private boolean closingState = false;

  /**
   * The curtain's vertical sweep position.
   */
  private int curtainY;

  /**
   * Frames elapsed in the current sweep; drives the exponential speed-up.
   */
  private int sweepFrame;

  /**
   * What to do the instant the curtain finishes closing -- the turn swap
   * runs here, safely hidden behind the fully drawn curtain.
   */
  private Runnable onClosed;

  /**
   * Constructs the curtain over the given host component.
   *
   * @param host the component the curtain covers
   */
  public CurtainPanel( JComponent host )
  {
    super( CURTAIN_SHADE );
    this.host = host;
    setLayout( null );

    JButton revealer = new JButton( "RAISE THE CURTAIN" );
    revealer.setFont( new Font( "Serif", Font.BOLD, 20 ) );
    revealer.setForeground( new Color( 18, 10, 28 ) );    // ink on parchment,
    revealer.setBackground( new Color( 229, 213, 175 ) ); // like the Back button
    revealer.setFocusable( false );
    revealer.addActionListener( e -> open() );
    add( revealer );

    addComponentListener( new ComponentAdapter()
    {
      @Override
      public void componentResized( ComponentEvent e )
      {
        revealer.setBounds( getWidth() / 2 - 140, getHeight() / 2 - 30, 280, 60 );
      }
    });

    motionTimer = new Timer( FRAME_MS, this::onFrame );
  }

  /**
   * One animation frame: advance the sweep exponentially in whichever
   * direction is active, firing the closed callback on touchdown.
   *
   * @param e the timer event
   */
  private void onFrame( ActionEvent e )
  {
    sweepFrame++;
    int sweep = (int) Math.ceil(
        Math.pow( BASE_SPEED, sweepFrame / (double) TENFOLD_FRAMES ) );

    if ( closingState )
    {
      curtainY += sweep;
      if ( curtainY >= host.getHeight() )
      {
        closingState = false;
        closedState = true;
        motionTimer.stop();

        if ( onClosed != null )
        {
          Runnable queued = onClosed;
          onClosed = null;
          queued.run(); // swap the state while nobody can see the boards
        }
      }
    }
    else if ( !closedState )
    {
      curtainY -= sweep;
      if ( curtainY <= -host.getHeight() )
      {
        curtainY = -host.getHeight();
        setVisible( false );
        motionTimer.stop();
      }
    }
    updateBounds();
  }

  /**
   * Drops the curtain, queueing work to run the instant it has fully closed.
   *
   * @param onFullyClosed the work to run behind the closed curtain
   */
  public void drop( Runnable onFullyClosed )
  {
    this.onClosed = onFullyClosed;
    closingState = true;
    closedState = false;
    curtainY = 0;
    sweepFrame = 0;
    setVisible( true );
    motionTimer.start();
  }

  /**
   * Raises the curtain with the same accelerating sweep it fell with.
   */
  public void open()
  {
    closedState = false;
    sweepFrame = 0;
    motionTimer.start();
  }

  /**
   * Whether a sweep is currently in motion (used by the pause system).
   *
   * @return true if the curtain is mid-sweep
   */
  public boolean isMoving()
  {
    return motionTimer.isRunning();
  }

  /**
   * Freezes a sweep in place for a game pause.
   */
  public void pauseMotion()
  {
    motionTimer.stop();
  }

  /**
   * Resumes a frozen sweep.
   */
  public void resumeMotion()
  {
    motionTimer.start();
  }

  /**
   * Repositions the curtain against the host's current size, enforcing the
   * fully closed position when applicable. The host calls this on resize;
   * the curtain calls it itself on every animation frame.
   */
  public void updateBounds()
  {
    int width = host.getWidth();
    int height = host.getHeight();

    if ( closedState ) // curtain fully closed, enforce
      curtainY = height;

    setBounds( 0, curtainY - height, width, height );
  }
}
