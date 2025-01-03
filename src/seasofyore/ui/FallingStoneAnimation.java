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
 * Small class that uses event threads to guide the animation
 * of small stones that fall from the top of the screen.
 * 
 * 
 * @author dylan
 */
public class FallingStoneAnimation 
{
    private final Point startPoint; // where the animation starts
    private final Point targetCell; // target cell for the stone
    private final JPanel renderPanel; // panel to repaint during animation
    
    // grey brown RGB code, for stone
    private static final Color STONECOLOR = new Color( 132, 123, 109 );
    private int stoneY; // current Y position of the stone
    private static final int STONE_SIZE = 30; // size of the stone
    private static final int ANIMATION_SPEED = 10; // speed in pixels per frame
    private Timer animationTimer;

    public FallingStoneAnimation( Point targetCell, JPanel renderPanel ) 
    {
        this.startPoint = new Point( targetCell.x, 0 ); // start at the top of the screen
        this.targetCell = targetCell;
        this.renderPanel = renderPanel;
        this.stoneY = 0;
    }
    
    // for use on event dispatch thread ONLY !
    public void startAnimation( Runnable onAnimationComplete ) 
    {
      animationTimer = new Timer( 6, ( ActionEvent e ) -> 
      {
        stoneY += ANIMATION_SPEED; // move stone downward
        // check if the stone has reached the target cell
        if ( stoneY >= targetCell.y ) 
        {
            animationTimer.stop();
            onAnimationComplete.run(); // trigger post-animation logic
        }
        renderPanel.repaint(); // Request a repaint to show the updated position
      });
      animationTimer.start();
    }

    // center cell X position ( doesn't change )
    private int centerX()
    {
      return startPoint.x - STONE_SIZE / 2;
    }
    
    // center cell Y position ( does change )
    private int centerStoneY()
    {
      return stoneY - STONE_SIZE / 2;
    }
    
    public void draw( Graphics g ) 
    {
      int centX = centerX();
      int centStoneY = centerStoneY();
      
      g.setColor( STONECOLOR ); // stone color ( brownish? )
      g.fillOval( centX, centStoneY, STONE_SIZE, STONE_SIZE );
      g.setColor( Color.BLACK );
      g.drawOval( centX, centStoneY, STONE_SIZE, STONE_SIZE );
    }
}

