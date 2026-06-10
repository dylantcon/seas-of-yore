/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Small class that uses Swing timers to animate a catapult stone plummeting
 * onto a target cell. The stone accelerates under gravity while a shadow
 * grows beneath it on the target cell; on touchdown a brief splash ring
 * ripples outward before the completion callback fires.
 *
 * @author dylan
 */
public class FallingStoneAnimation
{
    private final Point targetCell;   // target cell center for the stone
    private final JPanel renderPanel; // panel to repaint during animation

    // grey brown RGB code, for stone
    private static final Color STONECOLOR = new Color( 132, 123, 109 );
    // translucent black, for the shadow cast on the target cell
    private static final Color SHADOWCOLOR = new Color( 0, 0, 0, 70 );

    private static final int STONE_SIZE = 30;       // size of the stone
    private static final int FRAME_MS = 6;          // delay between frames

    // The fall is exponential: each frame the stone moves BASE_SPEED raised
    // to (fallFrame / TENFOLD_FRAMES) pixels. BASE_SPEED is the classic
    // constant stone speed, reused as the base of the exponent, so the stone
    // lifts off at a crawl, crosses its old cruising speed mid-fall, and
    // slams into the target still gaining speed.
    private static final double BASE_SPEED = 10.0;  // px/frame, exponent base
    private static final int TENFOLD_FRAMES = 30;   // frames per BASE_SPEED-fold gain

    private static final int SPLASH_FRAMES = 10;    // length of the splash
    private static final int SPLASH_GROWTH = 3;     // ring radius gain, px/frame

    private double stoneY;      // current Y position of the stone
    private int fallFrame;      // frames spent falling so far
    private int splashFrame;    // 0 while falling; counts splash frames after
    private Timer animationTimer;

    public FallingStoneAnimation( Point targetCell, JPanel renderPanel )
    {
        this.targetCell = targetCell;
        this.renderPanel = renderPanel;
        this.stoneY = -STONE_SIZE;        // start just above the visible panel
        this.fallFrame = 0;
        this.splashFrame = 0;
    }

    // for use on event dispatch thread ONLY !
    public void startAnimation( Runnable onAnimationComplete )
    {
      animationTimer = new Timer( FRAME_MS, ( ActionEvent e ) ->
      {
        if ( !isSplashing() )
        {
          fallFrame++;
          // exponential speed-up: BASE_SPEED^(fallFrame / TENFOLD_FRAMES)
          stoneY += Math.pow( BASE_SPEED, fallFrame / (double) TENFOLD_FRAMES );
          if ( stoneY >= targetCell.y )
          {
            stoneY = targetCell.y;
            splashFrame = 1;   // touchdown: begin the splash
          }
        }
        else if ( ++splashFrame > SPLASH_FRAMES )
        {
          animationTimer.stop();
          onAnimationComplete.run(); // trigger post-animation logic
        }
        renderPanel.repaint(); // Request a repaint to show the updated position
      });
      animationTimer.start();
    }

    /**
     * Cancels the animation without firing the completion callback. Lets a
     * phase that is torn down mid-flight (e.g. by a win screen) make sure no
     * orphaned timer resolves a shot after the phase is gone.
     */
    public void stop()
    {
      if ( animationTimer != null && animationTimer.isRunning() )
        animationTimer.stop();
    }

    // whether the stone has landed and the splash ring is still expanding
    private boolean isSplashing()
    {
      return splashFrame > 0;
    }

    // center cell X position ( doesn't change )
    private int centerX()
    {
      return targetCell.x - STONE_SIZE / 2;
    }

    // center cell Y position ( does change )
    private int centerStoneY()
    {
      return (int) stoneY - STONE_SIZE / 2;
    }

    public void draw( Graphics g )
    {
      if ( isSplashing() )
      {
        drawSplash( g );
        return;
      }
      drawShadow( g );
      drawStone( g );
    }

    // the stone in flight
    private void drawStone( Graphics g )
    {
      int centX = centerX();
      int centStoneY = centerStoneY();

      g.setColor( STONECOLOR ); // stone color ( brownish? )
      g.fillOval( centX, centStoneY, STONE_SIZE, STONE_SIZE );
      g.setColor( Color.BLACK );
      g.drawOval( centX, centStoneY, STONE_SIZE, STONE_SIZE );
    }

    // a shadow on the target cell that grows as the stone closes in,
    // selling the sense of height without any extra geometry
    private void drawShadow( Graphics g )
    {
      double progress = ( targetCell.y <= 0 )
                      ? 1.0
                      : Math.max( 0.0, Math.min( 1.0, stoneY / targetCell.y ) );

      int w = (int) ( STONE_SIZE * ( 0.4 + 0.6 * progress ) );
      int h = w / 2; // squashed ellipse reads as a shadow on water

      g.setColor( SHADOWCOLOR );
      g.fillOval( targetCell.x - w / 2, targetCell.y - h / 2, w, h );
    }

    // an expanding, fading ring where the stone struck the water
    private void drawSplash( Graphics g )
    {
      int radius = STONE_SIZE / 2 + splashFrame * SPLASH_GROWTH;
      int alpha = Math.max( 0, 220 - ( 220 / SPLASH_FRAMES ) * splashFrame );

      g.setColor( new Color( 255, 255, 255, alpha ) );
      // squashed like the shadow, so the ripple lies flat on the water
      g.drawOval( targetCell.x - radius, targetCell.y - radius / 2,
                  radius * 2, radius );
    }
}
