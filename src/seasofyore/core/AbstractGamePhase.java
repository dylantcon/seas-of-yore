/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore;

import seasofyore.ui.QuadrantPanel;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 * An abstract base class for game phases in the Seas of Yore game.
 * Provides default implementations for the {@link GamePhase} interface methods
 * and allows subclasses to override specific behaviors as needed.
 * This class simplifies the creation of new game phases by handling common 
 * setup logic.
 * 
 * @author dylan
 */
public abstract class AbstractGamePhase implements GamePhase
{
  /**
   * The GameController managing the game state and UI for this phase.
   * This is set when the phase is entered.
   */
  protected GameController controller;
  
  
  /**
   * Called when the phase starts.
   * Stores the GameController and calls the {@link #onEnter()} method
   * for additional setup logic in subclasses.
   *
   * @param controller the GameController managing the game
   */
  @Override
  public void enterPhase( GameController controller )
  {
    this.controller = controller;
    onEnter();
  }

  /**
   * Handles a cell click event during this phase.
   * This default implementation does nothing and can be overridden by subclasses.
   *
   * @param x              the x-coordinate of the clicked cell
   * @param y              the y-coordinate of the clicked cell
   * @param quadrantPanel  the QuadrantPanel where the click occurred
   */
  @Override
  public void handleCellClick( int x, int y, QuadrantPanel quadrantPanel )
  {
    
  }
  
  /**
   * Handles mouse movement events within a QuadrantPanel during this phase.
   * This default implementation does nothing and can be overridden by subclasses.
   *
   * @param dragLayerPoint the point in the drag layer corresponding to the movement
   * @param src            the QuadrantPanel where the movement occurred
   */
  @Override
  public void handleQuadrantMovement( Point dragLayerPoint, QuadrantPanel src )
  {
    
  }
  
  /**
   * Handles user input events during this phase. Not all phases need this method.
   * This default implementation does nothing and can be overridden by subclasses.
   *
   * @param e the MouseEvent representing the user input
   */
  @Override
  public void handleInput( MouseEvent e )
  {
    // default: do nothing at all
  }
  
  /**
   * Updates the state of the phase.
   * This default implementation does nothing and can be overridden by subclasses.
   */
  @Override
  public void update()
  {
    // default: do nothing at all
  }
  
  /**
   * Renders any custom visuals for the phase.
   * This default implementation does nothing and can be overridden by subclasses.
   *
   * @param g the Graphics object used for rendering
   */
  @Override
  public void render( Graphics g )
  {
    // default: do nothing at all
  }
  
  /**
   * Called when the phase is entered.
   * This method is intended to be implemented by subclasses to define
   * phase-specific setup logic.
   */
  protected abstract void onEnter();
}
