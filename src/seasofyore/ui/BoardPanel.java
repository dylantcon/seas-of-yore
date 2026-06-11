/**
 * The UI package, which contains all files related to the front-end of the game.
 */
package seasofyore.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Represents the main game board panel, managing the display of friendly and
 * enemy QuadrantPanels, as well as background animations and layout.
 * <p>
 * The water behind the quadrants is a {@link PixelWaterAnimation}: a small
 * set of pre-rendered frames stretch-blitted at a deliberately low rate.
 * Its predecessor -- two full-panel animated GIFs -- repainted the board at
 * the GIFs' own frame rate and re-scaled every frame in software, which
 * under CheerpJ kept the single browser thread busy enough to starve a
 * relay connection. Here the panel owns the clock instead: one Swing timer,
 * one frame counter, two blits per paint.
 *
 * @author dylan connolly
 */
public class BoardPanel extends JPanel
{
  /**
   * The panel representing the player's own quadrant.
   */
  private QuadrantPanel friendlyPane;
  /**
   * The panel representing the enemy's quadrant.
   */
  private QuadrantPanel enemyPane;
  /**
   * The light water loop painted behind the friendly (lower) half.
   */
  private final PixelWaterAnimation lightWater;
  /**
   * The dark water loop painted behind the enemy (upper) half.
   */
  private final PixelWaterAnimation darkWater;

  /**
   * Advances the water loop. Unlike the old GIFs' observer-driven repaints,
   * this timer makes the animation's cost explicit and tunable; it runs only
   * while the panel is in a displayable hierarchy (see addNotify).
   */
  private final Timer waterTimer;

  /**
   * The current frame of the water loop.
   */
  private int waterFrame = 0;

  /**
   * Indicates whether the panels have already been added to the layout.
   */
  private boolean panelsAdded = false;

  /**
   * When true, both quadrants render their fleets (used when spectating an
   * AI-vs-AI match, where neither side is hidden from the watching human).
   */
  private boolean revealBoth = false;
  
  /**
   * Default padding used in the panel layout (GridBagConstraints).
   */
  private final static int DEF_PAD = 20;
  
  /**
   * Constructs a new BoardPanel with the specified friendly and enemy QuadrantPanels.
   *
   * @param friend the QuadrantPanel representing the friendly player's quadrant
   * @param enemy  the QuadrantPanel representing the enemy player's quadrant
   */
  public BoardPanel( QuadrantPanel friend, QuadrantPanel enemy )
  {
    this.friendlyPane = friend;
    this.enemyPane = enemy;

    // the frames are pre-rendered once per session and shared between boards
    lightWater = PixelWaterAnimation.lightWater();
    darkWater = PixelWaterAnimation.darkWater();

    waterTimer = new Timer( PixelWaterAnimation.SUGGESTED_FRAME_MS, e ->
    {
      waterFrame++;
      // a hidden board (e.g. behind another card) skips even the repaint
      if ( isShowing() )
        repaint();
    });

    // use GridBagLayout for increased flexibility
    this.setLayout( new GridBagLayout() );
    this.addAllPanels();
  }

  /**
   * Starts the water clock when the panel joins a displayable hierarchy.
   */
  @Override
  public void addNotify()
  {
    super.addNotify();
    waterTimer.start();
  }

  /**
   * Stops the water clock when the panel leaves the hierarchy, so discarded
   * boards do not animate (or leak) forever.
   */
  @Override
  public void removeNotify()
  {
    waterTimer.stop();
    super.removeNotify();
  }
  
  /**
   * Adds all panels (friendly and enemy) to the layout.
   * This is a top-level method coordinating the panel addition process.
   */
  private void addAllPanels()
  {
    if ( panelsAdded )
      return;
    
    this.removeAll();
    friendlyPane.setFriendly( true );
    // in spectator games both fleets are shown; otherwise the enemy is hidden
    enemyPane.setFriendly( revealBoth );
    
    friendlyPane.updateChildPanelBounds();
    enemyPane.updateChildPanelBounds();
    
    addEnemyPanel();
    addFriendlyPanel();
    
    panelsAdded = true;
    this.revalidate();
    this.repaint();
  }
  
  /**
   * Adds the enemy QuadrantPanel to the layout.
   */
  private void addEnemyPanel()
  {
    GridBagConstraints gbc = createBaseConstraints();
    gbc.gridy = 0;
    this.add( enemyPane, gbc );
  }
  
  /**
   * Adds the friendly QuadrantPanel to the layout.
   */
  private void addFriendlyPanel()
  {
    GridBagConstraints gbc = createBaseConstraints();
    gbc.gridy = 1;
    this.add( friendlyPane, gbc );
  }
  
  /**
   * Creates and configures a base set of GridBagConstraints for layout.
   *
   * @return a GridBagConstraints object with common settings
   */
  private GridBagConstraints createBaseConstraints()
  {
    int availW = this.getWidth();
    int availH = this.getHeight();

    int quadH = availH / 2;
    int idealW = quadH;

    int hPadding = ( availW - idealW ) / 2;
    hPadding = Math.max( hPadding, 0 );

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets( DEF_PAD, hPadding, DEF_PAD, hPadding );
    gbc.gridx = 0;
    gbc.weightx = 1.0;
    gbc.weighty = 0.5;
    gbc.fill = GridBagConstraints.BOTH;

    return gbc;
  }
    
  /**
   * Overrides the setBounds method to ensure proper layout updates
   * when the panel size changes.
   *
   * @param x      the new x-coordinate
   * @param y      the new y-coordinate
   * @param width  the new width of the panel
   * @param height the new height of the panel
   */
  @Override
  public void setBounds( int x, int y, int width, int height )
  {
    super.setBounds( x, y, width, height );
    this.panelsAdded = false;
    this.addAllPanels();
  }
  
  /**
   * Sets whether both quadrants should render their fleets (spectator mode).
   * Triggers a layout refresh so the change takes effect immediately.
   *
   * @param reveal true to reveal both fleets; false to hide the enemy's
   */
  public void setRevealBoth( boolean reveal )
  {
    this.revealBoth = reveal;
    this.panelsAdded = false;
    this.addAllPanels();
  }

  /**
   * Swaps the friendly and enemy QuadrantPanels.
   * This is typically used to switch perspectives between players.
   */
  public void swapPanels()
  {
    QuadrantPanel newFriendly = this.enemyPane;
    QuadrantPanel newEnemy = this.friendlyPane;
    
    this.friendlyPane = newFriendly;
    this.enemyPane = newEnemy;
    
    panelsAdded = false;
    this.addAllPanels();
  }
  
  /**
   * Gets the QuadrantPanel representing the friendly player's quadrant.
   *
   * @return the friendly player's QuadrantPanel
   */
  public QuadrantPanel getFriendlyPanel()
  {
    return this.friendlyPane;
  }
  
  /**
   * Gets the QuadrantPanel representing the enemy's quadrant.
   *
   * @return the enemy player's QuadrantPanel
   */
  public QuadrantPanel getEnemyPanel()
  {
    return this.enemyPane;
  }
  
  /**
   * Paints the component: one stretch-blit of pixel water per half (dark
   * seas for the enemy above, light for the friendly fleet below) and the
   * dividing line. No ImageObserver is involved, so nothing here can
   * schedule repaints behind the timer's back.
   *
   * @param g the Graphics object used for drawing
   */
  @Override
  protected void paintComponent( Graphics g )
  {
    super.paintComponent( g );

    int w = getWidth();
    int h = getHeight();

    darkWater.paintFrame( g, waterFrame, 0, 0, w, h / 2 );
    lightWater.paintFrame( g, waterFrame, 0, h / 2, w, h - h / 2 );

    // draw dividing line between upper and lower areas
    g.setColor( Color.BLACK );
    int middleY = h / 2;
    g.drawLine( 0, middleY, w, middleY ); // horizontal line
  }
}