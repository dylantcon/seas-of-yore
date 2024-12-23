package seasofyore;

import seasofyore.core.Player;
import seasofyore.core.Direction;
import seasofyore.core.ShipType;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.Map;
import java.util.HashMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLayeredPane;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;

/**
 * Represents the sidebar panel in the Seas of Yore game.
 * The sidebar includes ship slots for selection, a toggle button,
 * and dynamic layout adjustments.
 * @author dylan connolly
 * 
 * 
 */
public class SidebarPanel extends JLayeredPane
{
  /**
   * The container for displaying ship slots.
   */  
  private final JPanel slotContainer;
  
  /**
   * The toggle button to open and close the sidebar.
   */
  private final CustomButton toggleButton;
  
  /**
   * A map of ship types to their corresponding slot labels.
   */
  private final Map< ShipType, JLabel > shipSlots;
  
  /**
   * The currently selected ship type in the sidebar.
   */
  private ShipType selectedShip;
  
  /**
   * The active QuadrantPanel associated with this sidebar.
   */  
  private QuadrantPanel activeQuadrant;
  
  /**
   * Path to the wood texture image used as the background of the slot container.
   */
  private static final String WOODPATH = "/images/wood.png";
  
  /**
   * Path to the navy icon image used for the toggle button.
   */
  private static final String NAVYPATH = "/images/navyicon.png";
  
  /**
   * The wood texture image.
   */
  private final ImageIcon wood;
  
  /**
   * The navy icon image.
   */
  private final ImageIcon navyicon;
  
  /**
   * Indicates whether the sidebar is currently open.
   */
  private boolean isOpen = false;
  
  /**
   * The width of the toggle button.
   */
  public static final int TOGGLE_BUTTON_WIDTH = 40;
  
   /**
   * The height of the toggle button.
   */
  private static final int TOGGLE_BUTTON_HEIGHT = 60;
  
  /**
   * The number of pixels moved per step during sidebar animation.
   */
  private static final int ANIMATION_STEP = 10;
  
  /**
   * The delay in milliseconds between animation steps.
   */
  private static final int ANIMATION_DELAY = 10;
  
  /**
   * The ratio of the sidebar width relative to the parent panel's width.
   */
  private static final float SIDEBAR_RATIO = 0.20f;
  
  
  /**
   * Constructs a new SidebarPanel, initializing ship slots, toggle button,
   * and layout adjustments.
   */
  public SidebarPanel()
  {
    this.shipSlots = new HashMap< >();
    this.selectedShip = null;
    this.activeQuadrant = null;
    this.wood = new ImageIcon( getClass().getResource( WOODPATH ) );
    this.navyicon = new ImageIcon( getClass().getResource( NAVYPATH ) );
    
    setLayout( null ); // allow custom positioning (no layout mgr)
    setOpaque( false );
    
    slotContainer = createSlotContainer();
    add( slotContainer, JLayeredPane.DEFAULT_LAYER );
   
    // add toggle button
    toggleButton = createToggleButton();
    add( toggleButton, JLayeredPane.PALETTE_LAYER );
    
    resetAllSlots();
    
    // add component listener for resizing
    addComponentListener( new ComponentAdapter()
    {
      @Override
      public void componentResized( ComponentEvent e )
      {
        adjustLayout();
      }
    });
  }

  /**
   * Creates the slot container for ship slots with a wood-textured background.
   *
   * @return the slot container panel
   */
  private JPanel createSlotContainer()
  {
    JPanel panel = new JPanel()
    {
      @Override
      public void paintComponent( Graphics g )
      {
        super.paintComponent( g );
        g.drawImage
        ( 
          wood.getImage(),
          0, 
          0, 
          getWidth(), 
          getHeight(),
          this
        );
      }
    };      
    panel.setLayout( null );
    panel.setOpaque( true );
    return panel;
  }

  /**
   * Gets the slot container panel.
   *
   * @return the slot container panel
   */
  public JPanel getSlotContainer()
  {
    return this.slotContainer;
  }
  
  /**
   * Creates the toggle button for opening and closing the sidebar.
   *
   * @return the toggle button
   */
  private CustomButton createToggleButton()
  {
    CustomButton button = new CustomButton( navyicon );
    button.setFocusPainted( false );
    button.setBounds( 0, 0, TOGGLE_BUTTON_WIDTH, TOGGLE_BUTTON_HEIGHT );
    button.addActionListener( e -> toggleSidebar() );
    return button;
  }
  
 /**
   * Adjusts the layout of the sidebar, including the position of ship slots
   * and the toggle button, based on the current size of the parent panel.
   */
  private void adjustLayout()
  {
    int sbW = (int) ( getParent().getWidth() * SIDEBAR_RATIO );
    int sbH = this.getHeight();
    
    // adjust slot container position dynamically
    slotContainer.setBounds
    (
      TOGGLE_BUTTON_WIDTH,
      0,
      sbW - TOGGLE_BUTTON_WIDTH,
      sbH
    );
    // adjust toggle button position dynamically
    toggleButton.setBounds
    ( 
      0,
      sbH / 2 - TOGGLE_BUTTON_HEIGHT / 2,
      TOGGLE_BUTTON_WIDTH,
      TOGGLE_BUTTON_HEIGHT
    );
    resizeShipSlots( sbW, sbH );
  }

  /**
   * Resizes the ship slots dynamically based on the sidebar's dimensions.
   *
   * @param sbW the width of the sidebar
   * @param sbH the height of the sidebar
   */
  private void resizeShipSlots( int sbW, int sbH )
  {
    // adjust ship slots dynamically
    int sH = sbH / ShipType.values().length;
    int sW = sbW - TOGGLE_BUTTON_WIDTH - 20;
    int yPos = 10; // start padding from top
    for ( Map.Entry< ShipType, JLabel > entry : shipSlots.entrySet() )
    {
      JLabel slot = entry.getValue();
      slot.setBounds( 10, yPos, sW, sH - 10 );
      yPos += sH;
    }
    this.repaint();
  }
  
  /**
   * Toggles the sidebar open or closed with a sliding animation.
   */
  private void toggleSidebar()
  {
    isOpen = !isOpen;
    int targetX = isOpen ? getUndockedX() : getDockedX();
    
    Timer t = new Timer( ANIMATION_DELAY, ( ActionEvent e ) -> 
    {
      int x = getX();
      if ( isOpen && x > targetX || !isOpen && x < targetX )
        setLocation( x + ( isOpen ? -ANIMATION_STEP : ANIMATION_STEP ), getY() );
      else
      {
        setLocation( targetX, getY() );
        ( (Timer) e.getSource() ).stop();
      }
    });
    t.start();
    repaint();
  }
  
  /**
   * Forces the sidebar to close if it is currently open.
   */
  public void forceCloseSidebar()
  {
    if ( !isOpen )
      return;
    toggleSidebar();
  }
  
  /**
   * Sets the active QuadrantPanel for the sidebar and populates ship slots.
   *
   * @param quadrant the active QuadrantPanel
   */
  public void setActiveQuadrant( QuadrantPanel quadrant )
  {
    if ( quadrant != null )
    {
      this.activeQuadrant = quadrant;
      populateShipSlots();
    }
  }

  /**
   * Populates the ship slots in the sidebar based on the active QuadrantPanel.
   */
  private void populateShipSlots()
  {
    if ( activeQuadrant == null )
      return;
    
    slotContainer.removeAll();
    Player owner = activeQuadrant.getOwner();
    shipSlots.clear();
    
    for ( ShipType type : ShipType.getAscendingList() )
    {
      JLabel shipSlot = new JLabel();
      ImageIcon shipSprite = activeQuadrant.getCachedShipImage
      (
        owner.getCiv(),
        type,
        Direction.EAST
      );
      Image scaledImage = shipSprite.getImage().getScaledInstance
      (
        100,
        30,
        Image.SCALE_SMOOTH
      );
      shipSlot.setIcon( new ImageIcon( scaledImage ) );
      shipSlot.setBorder( getBorder( type ) );
      shipSlots.put( type, shipSlot );
      slotContainer.add( shipSlot );
    }
    slotContainer.revalidate();
    slotContainer.repaint();
  }
  
  /**
   * Creates a TitledBorder for a ship slot.
   *
   * @param t the ship type for the border
   * @return a TitledBorder for the ship slot
   */
  private TitledBorder getBorder( ShipType t )
  {
    TitledBorder b; 
    b = new TitledBorder( String.format( "%s (%d)", t, t.getLength() ) );
    b.setTitleColor( Color.WHITE );
    return b;
  }
  
  /**
   * Creates a TitledBorder for a selected ship slot.
   *
   * @param t the ship type for the border
   * @return a TitledBorder for the selected ship slot
   */
  private TitledBorder getSelectedBorder( ShipType t )
  {
    TitledBorder s = getBorder( t );
    s.setTitleColor( Color.RED );
    return s;
  }
 
  /**
   * Gets the x-coordinate of the sidebar when docked.
   *
   * @return the x-coordinate of the docked sidebar
   */
  public int getDockedX()
  {
    return getParent().getWidth() - TOGGLE_BUTTON_WIDTH;
  }
  
  /**
   * Gets the x-coordinate of the sidebar when undocked.
   *
   * @return the x-coordinate of the undocked sidebar
   */
  public int getUndockedX()
  {
    return getDockedX() - slotContainer.getWidth();
  }

  /**
   * Checks if a point is within the bounds of a specific ship slot.
   *
   * @param type  the ship type of the slot
   * @param point the point to check
   * @return true if the point is within the slot's bounds; false otherwise
   */
  private boolean isPointInSlot( ShipType type, Point point )
  {
    JLabel slot = shipSlots.get( type );
    if ( slot != null )
    {
      Rectangle bounds = slot.getBounds();
      return bounds.contains( point );
    }
    return false;
  }
  
  /**
   * Gets the ship type corresponding to a point in the sidebar.
   *
   * @param point the point to check
   * @return the ship type at the point, or null if no ship is found
   */
  public ShipType getShipAtPoint( Point point )
  {
    for ( ShipType type : shipSlots.keySet() )
      if ( isPointInSlot( type, point ) )
        return type;
    return null;
  }
  
  /**
   * Gets the JLabel representing the slot for a specific ship type.
   *
   * @param type the ship type
   * @return the JLabel for the ship slot
   */
  public JLabel getShipSlot( ShipType type )
  {
    return shipSlots.get( type );
  }
  
  /**
   * Gets a map of all ship types to their corresponding slot labels.
   *
   * @return a map of all ship slots
   */
  public Map< ShipType, JLabel > getAllShipSlots()
  {
    return new HashMap< >( shipSlots );
  }
  
  /**
   * Enables or disables a specific ship slot.
   *
   * @param type the ship type of the slot
   * @param bool true to enable the slot; false to disable it
   */
  public void setSlotEnabled( ShipType type, boolean bool )
  {
    JLabel slot = shipSlots.get( type );
    if ( slot != null )
    {
      slot.setEnabled( bool );
      slot.setOpaque( bool );
      repaint();
    }
  }
  
  /**
   * Checks if a specific ship slot is enabled.
   *
   * @param type the ship type of the slot
   * @return true if the slot is enabled; false otherwise
   */
  public boolean isSlotEnabled( ShipType type )
  {
    JLabel slot = shipSlots.get( type );
    return slot != null && slot.isEnabled();
  }
  
  /**
   * Gets the currently selected ship type.
   *
   * @return the selected ship type
   */
  public ShipType getSelectedShip()
  {
    return selectedShip;
  }

  /**
   * Sets the currently selected ship type and updates the slot border.
   *
   * @param type the ship type to select
   */
  public void setSelectedShip( ShipType type )
  {
    if ( type == null )
      return;
    
    if ( selectedShip != null )
    {
      JLabel prevSlot = shipSlots.get( selectedShip );
      if ( prevSlot != null )
        prevSlot.setBorder( getBorder( selectedShip ) );
    }
    selectedShip = type;
    if ( selectedShip != null )
    {
      JLabel currentSlot = shipSlots.get( selectedShip );
      if ( currentSlot != null )
        currentSlot.setBorder( getSelectedBorder( type ) );
    }
  }
  
  /**
   * Deselects the currently selected ship.
   */
  public void nullSelectedShip()
  {
    this.selectedShip = null;
  }
  
  /**
   * Resets all ship slots, enabling them and setting their borders to default.
   */
  public final void resetAllSlots()
  {
    for ( Map.Entry< ShipType, JLabel > entry : shipSlots.entrySet() )
    {
      JLabel slot = entry.getValue();
      slot.setEnabled( true );
      slot.setBorder( getBorder( entry.getKey() ) );
    }
    selectedShip = null;
  }
  /**
   * Enables or disables all ship slots and resets their borders.
   *
   * @param b true to enable all slots; false to disable them
   */
  public void allSlotsEnabled( boolean b )
  {
    for ( Map.Entry< ShipType, JLabel > entry : shipSlots.entrySet() )
    {
      JLabel slot = entry.getValue();
      slot.setEnabled( b );
      slot.setOpaque( b );
      slot.setBorder( getBorder( entry.getKey() ) );
    }
    selectedShip = null;
  }
  /**
   * Sets the bounds of the sidebar and adjusts the layout.
   *
   * @param x      the new x-coordinate
   * @param y      the new y-coordinate
   * @param width  the new width
   * @param height the new height
   */
  @Override  
  public void setBounds( int x, int y, int width, int height )
  {
    super.setBounds( x, y, width, height );
    adjustLayout();
  }
  
  class CustomButton extends JButton 
  { // made this because it looks a bit nicer.
    private final ImageIcon icon;
    
    public CustomButton( Icon i )
    {
      super();
      icon = (ImageIcon) i;

      // preserve original look & feel
      setOpaque( false );
      setContentAreaFilled( false );
      setFocusPainted( false );
      setBorderPainted( true );
    }

    @Override
    protected void paintComponent( Graphics g ) 
    {
        Graphics2D g2 = ( Graphics2D ) g.create();

        // enable anti-aliasing for smooth edges
        g2.setRenderingHint
        ( 
          RenderingHints.KEY_ANTIALIASING, 
          RenderingHints.VALUE_ANTIALIAS_ON 
        );

        // draw the background color
        g2.drawImage
        ( 
          icon.getImage(), 
          0, 
          0, 
          this.getWidth(), 
          this.getHeight(), 
          this 
        );
        g2.dispose();
    }  
  }
}