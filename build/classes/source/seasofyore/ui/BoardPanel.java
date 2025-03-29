/**
 * The UI package, which contains all files related to the front-end of the game.
 */
package seasofyore.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

/**
 * Represents the main game board panel, managing the display of friendly and 
 * enemy QuadrantPanels, as well as background animations and layout.
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
   * The light water animation displayed in the background.
   */
  protected final ImageIcon waterAnimation;
  /**
   * The dark water animation displayed in the background.
   */
  private final ImageIcon waterAnimationDark;
  
  /**
   * Path to the light water animation resource.
   */
  private final String waterGif = "/images/waterlight.gif";
  
   /**
   * Path to the dark water animation resource.
   */
  private final String waterDarkGif = "/images/waterdark.gif";
  
  /**
   * The scaled version of the light water animation image.
   */
  protected Image scaledWaterLight;
  
  /**
   * The scaled version of the dark water animation image.
   */
  protected Image scaledWaterDark;
  
  /**
   * Indicates whether the panels have already been added to the layout.
   */
  private boolean panelsAdded = false;
  
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
    
    // load water animation
    waterAnimation = new ImageIcon( getClass().getResource( waterGif ) );
    waterAnimationDark = new ImageIcon( getClass().getResource( waterDarkGif ) );
    
    // use GridBagLayout for increased flexibility
    this.setLayout( new GridBagLayout() );
    this.addAllPanels();
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
    enemyPane.setFriendly( false );
    
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
   * Paints the component, including background water animations and a dividing line.
   *
   * @param g the Graphics object used for drawing
   */
  @Override
  protected void paintComponent( Graphics g )
  {
    super.paintComponent( g );

    scaledWaterLight = waterAnimation.getImage();
    scaledWaterDark = waterAnimationDark.getImage();
    
    int w = getWidth();
    int h = getHeight();

    g.drawImage( scaledWaterDark, 0, 0, w, h / 2, this );
    g.drawImage( scaledWaterLight, 0, h / 2, w, h, this );
   
    // draw dividing line between upper and lower areas
    g.setColor( Color.BLACK );
    int middleY = getHeight() / 2;
    g.drawLine( 0, middleY, getWidth(), middleY ); // horizontal line    
  }
}