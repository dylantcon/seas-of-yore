/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore.ui;


import seasofyore.core.Ship;
import seasofyore.core.Player;
import seasofyore.core.ShipHeading;
import seasofyore.core.Direction;
import seasofyore.core.Civilization;
import seasofyore.core.PlayerQuadrant;
import seasofyore.core.ShipType;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.ImageIcon;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import seasofyore.GameController;
import seasofyore.QuadrantListener;

/**
 * Represents a player's quadrant in the game, with layered rendering for ship 
 * sprite &amp; other effects.
 * 
 * @author dylan connolly
 * 
 */
public class QuadrantPanel extends JLayeredPane
{ 
    /**
     * The player who owns this quadrant panel.
     */
    private final Player owner;

    /**
     * The player's corresponding game quadrant.
     */
    private final PlayerQuadrant quadrant;

    /**
     * Indicates if this quadrant panel is friendly (the player's own panel).
     */
    private boolean isFriendly;

    /**
     * Listener for quadrant interactions, such as cell clicks or movements.
     */
    private QuadrantListener listener;

    /**
     * Map of cell keys to CellPanel objects.
     */
    private final TreeMap<String, CellPanel> cellMap;

    /**
     * The grid panel for displaying cells.
     */
    private final JPanel gridPanel;

    /**
     * The overlay panel for rendering ships and effects.
     */
    private final JPanel overlayPanel;

    /**
     * The game controller managing this quadrant panel.
     */
    private GameController gameController;

    /**
     * Default image path for missing resources.
     */
    private final String nullPath = "/images/waterdark.gif";

    /**
     * Cache for ship images based on civilization, ship type, and direction.
     */
    private final Map<String, ImageIcon> imageCache = new HashMap<>();

    /**
     * Constructs a new QuadrantPanel for a specific player and quadrant.
     *
     * @param o the player who owns this quadrant panel
     * @param q the player's game quadrant
     * @param f true if the panel is friendly; false otherwise
     */
    public QuadrantPanel( Player o, PlayerQuadrant q, boolean f )
    {
      this.owner = o;
      this.quadrant = q;
      this.isFriendly = f;
      this.cellMap = new TreeMap< >();

      int gridS = PlayerQuadrant.GRID_SIZE;
      this.setLayout( null );

      gridPanel = new JPanel();
      gridPanel.setLayout( new GridLayout( gridS, gridS ) );
      for ( int y = 0; y < PlayerQuadrant.GRID_SIZE; y++ )
      {
        for ( int x = 0; x < PlayerQuadrant.GRID_SIZE; x++ )
        {
          CellPanel cell = new CellPanel( x, y, this );
          cellMap.put( cell.getKey(), cell );
          gridPanel.add( cell );
        }
      }
      // allow transparency for water animation
      gridPanel.setOpaque( false );
      gridPanel.setBounds( 0, 0, 500, 500 );
      // create upper layer overlay for ships and effects
      overlayPanel = new JPanel()
      {
        @Override
        protected void paintComponent( Graphics g )
        {
          super.paintComponent( g );
          if ( isFriendly )
            drawShips( g );
        }
        @Override
        public boolean isFocusable()
        {
          return false;
        }
      };
      overlayPanel.setOpaque( false );    
      overlayPanel.setLayout( null );
      // add component listener for dynamic resizing
      this.addComponentListener( new ComponentAdapter()
      {
        @Override
        public void componentResized( ComponentEvent e )
        {
          updateChildPanelBounds();
        }
      });
      this.add( gridPanel, JLayeredPane.DEFAULT_LAYER );
      this.add( overlayPanel, JLayeredPane.PALETTE_LAYER );
    }

  /**
   * Updates the bounds of child panels during resizing.
   */
  protected void updateChildPanelBounds()
  {
    int width = this.getWidth();
    int height = this.getHeight();
    
    gridPanel.setBounds( 0, 0, width, height );
    overlayPanel.setBounds( 0, 0, width, height );
    this.revalidate();
    this.repaint();
  }
  
  /**
   * Sets whether this panel is friendly and triggers a repaint.
   *
   * @param friendly true if friendly; false otherwise
   */
  public void setFriendly( boolean friendly )
  {
    this.isFriendly = friendly;
    this.repaint(); // repaint to reflect changes
  }
  
  /**
   * Checks if this panel is friendly.
   *
   * @return true if friendly; false otherwise
   */
  public boolean isFriendly()
  {
    return this.isFriendly;
  }
  
  /**
   * Sets the game controller managing this panel. Dependency injection.
   *
   * @param gameController the game controller
   */
  public void setGameController( GameController gameController )
  {
    this.gameController = gameController;
  }
  
   /**
   * Gets the game controller managing this panel.
   *
   * @return the game controller
   */
  public GameController getGameController()
  {
    return this.gameController;
  }
  
   /**
   * Gets the state of the game from a dependency injection in GameController.
   *
   * @return a Boolean representing the finalization of ship placement
   */
  public boolean isFinalPlacementState()
  {
    return this.gameController.getBoard().isPlacementFinal();
  }
  
  /**
   * Checks if a specific cell can be fired upon.
   *
   * @param x the x-coordinate of the cell
   * @param y the y-coordinate of the cell
   * @return true if the cell can be fired upon; false otherwise
   */
  public boolean canFireOn(int x, int y)
  {
    return this.getCellAt(x, y).canBeFiredUpon();
  }

  /**
   * Enables interaction for all cells in the panel.
   */
  public void enableCellInteraction()
  {
    cellMap.values().forEach(CellPanel::enableInteraction);
  }

  /**
   * Disables interaction for all cells in the panel.
   */
  public void disableCellInteraction()
  {
    cellMap.values().forEach(CellPanel::disableInteraction);
  }

  /**
   * Locks a specific cell for SALVO mode.
   *
   * @param x the x-coordinate of the cell
   * @param y the y-coordinate of the cell
   */
  public void lockCellForSALVO(int x, int y)
  {
    CellPanel cell = getCellAt(x, y);
    if (cell != null)
    {
        cell.lockForSALVO();
        cell.repaint();
    }
  }

  /**
   * Unlocks a specific cell from SALVO mode.
   *
   * @param x the x-coordinate of the cell
   * @param y the y-coordinate of the cell
   */
  public void unlockCellForSalvo(int x, int y)
  {
      CellPanel cell = getCellAt(x, y);
      if (cell != null)
      {
          cell.unlockForSALVO();
          cell.repaint();
      }
  }

  /**
   * Handles a click event on a specific cell.
   *
   * @param x the x-coordinate of the clicked cell
   * @param y the y-coordinate of the clicked cell
   */
  public void handleCellClick( int x, int y )
  {
      if ( listener != null )
      {
          listener.onCellClicked( x, y, this );
      }
      else
      {
          System.err.println("QuadrantListener not set. Click ignored");
      }
  }

  /**
   * Handles mouse movement over a specific cell.
   *
   * @param x the x-coordinate of the cell
   * @param y the y-coordinate of the cell
   * @param e the mouse event triggering the movement
   */
  public void handleCellMouseMove( int x, int y, MouseEvent e )
  {
      if ( listener != null )
      {
          CellPanel moved = getCellAt(x, y);
          JPanel dragLayer = getGameController().getDragLayerPanel();
          Point dragPt = SwingUtilities.convertPoint(moved, e.getPoint(), dragLayer);
          listener.onMovement(dragPt, this);
      }
      else
      {
          System.err.println("QuadrantListener not set. Movement ignored.");
      }
  }

  /**
   * Gets the overlay panel for this quadrant.
   *
   * @return the overlay panel
   */
  public JPanel getQuadrantOverlayPanel()
  {
      return this.overlayPanel;
  }

  /**
   * Gets the owner of this quadrant panel.
   *
   * @return the player who owns this panel
   */
  public Player getOwner()
  {
      return this.owner;
  }

  /**
   * Gets the player's corresponding game quadrant.
   *
   * @return the player's quadrant
   */
  public PlayerQuadrant getOwnerQuadrant()
  {
      return this.quadrant;
  }

  /**
   * Gets the civilization of the quadrant owner.
   *
   * @return the owner's civilization
   */
  public Civilization getOwnerCiv()
  {
      return owner.getCiv();
  }

  /**
   * Gets the CellPanel at the specified coordinates.
   *
   * @param x the x-coordinate of the cell
   * @param y the y-coordinate of the cell
   * @return the CellPanel at the specified coordinates, or null if not found
   */
  public CellPanel getCellAt(int x, int y)
  {
      return cellMap.get(CellPanel.makeKey(x, y));
  }

  /**
   * Converts a global point to grid coordinates.
   *
   * @param global the global point
   * @return an array containing the x and y grid coordinates
   */
  public int[] getGridFromGlobal( Point global )
  {
      Point local = SwingUtilities.convertPoint(
          gameController.getDragLayerPanel(),
          global,
          gridPanel
      );

      int[] cellWH = getCellDimensions();
      int gridX = local.x / cellWH[0];
      int gridY = local.y / cellWH[1];

      return new int[] { gridX, gridY };
  }

  /**
   * Converts grid coordinates to a global position, relative to the
   *  dragLayerPanel in GameController.
   * 
   * @param x the x-coordinate in the grid
   * @param y the y-coordinate in the grid
   * @return the global position of the cell
   */
  public Point getGlobalCellPosition( int x, int y )
  {
      int[] cellWH = getCellDimensions();
      int cellX = x * cellWH[0] + cellWH[0] / 2;
      int cellY = y * cellWH[1] + cellWH[1] / 2;

      JPanel dL = gameController.getDragLayerPanel();
      return SwingUtilities.convertPoint( this, new Point(cellX, cellY), dL );
  }

  /**
   * Gets the CellPanel at a specific point.
   *
   * @param p the point to check
   * @return the CellPanel at the specified point, or null if out of bounds
   */
  public CellPanel getCellAtCoordinates( Point p )
  {
      int[] cellWH = getCellDimensions();

      // Calculate the row and column from the point
      int x = p.x / cellWH[0];
      int y = p.y / cellWH[1];

      // Check if the coordinates are within bounds
      if ( !PlayerQuadrant.cellInBounds( x, y ) )
      {
        return null; // Out of bounds
      }

      return getCellAt( x, y );
  }

  /**
   * Retrieves or loads a cached ship image.
   *
   * @param c the civilization of the ship
   * @param t the type of the ship
   * @param d the direction of the ship
   * @return the cached ship image, or a placeholder if not found
   */
  public ImageIcon getCachedShipImage( Civilization c, ShipType t, Direction d )
  {
      String key = String.format( "%s-%s-%s", c, t, d );
      return imageCache.computeIfAbsent( key, k -> getShipImage( c, t, d ) );
  }

  /**
   * Loads a ship image from the specified path.
   *
   * @param c the civilization of the ship
   * @param t the type of the ship
   * @param d the direction of the ship
   * @return the loaded ship image, or a placeholder if the image is missing
   */
  private ImageIcon getShipImage( Civilization c, ShipType t, Direction d )
  {
    String imgpath = String.format( "/images/%s-%s-%s.png", c, t, d );
    java.net.URL imgURL = getClass().getResource(imgpath);
    if (imgURL == null)
    {
      System.err.println( "Missing sprite: " + imgpath );
      imgURL = getClass().getResource( nullPath );
      return new ImageIcon( imgURL );
    }
    return new ImageIcon( imgURL );
  }

  /**
   * Gets the preferred size for this panel.
   * The size is calculated as the smaller of the parent's width or half its height.
   *
   * @return the preferred size as a Dimension object
   */
  @Override
  public Dimension getPreferredSize()
  {
    int size = Math.min( getParent().getWidth(), getParent().getHeight() / 2 );
    return new Dimension( size, size );
  }

  /**
   * Sets the QuadrantListener for handling cell interactions.
   *
   * @param listener the QuadrantListener to set
   */
  public void setQuadrantListener( QuadrantListener listener )
  {
    this.listener = listener;
  }

  /**
   * Places a ship in the quadrant based on its type, coordinates, and direction.
   *
   * @param t the type of the ship to place
   * @param x the x-coordinate of the starting point
   * @param y the y-coordinate of the starting point
   * @param d the direction of the ship
   * @return true if the ship was placed successfully; false otherwise
   */
  public boolean placeShip( ShipType t, int x, int y, Direction d )
  {
    // TODO: implement non-default directional placement
    ShipHeading heading = new ShipHeading( x, y, d );
    boolean placed = owner.placeVessel( Ship.getInstance(t), heading );
    if ( placed )
    {
      this.updateChildPanelBounds();
    }

    return placed;
  }

  /**
   * Fires at a cell based on a global point.
   *
   * @param p the global point indicating the target cell
   * @return true if the fire hit a ship; false otherwise
   */
  public boolean fireAt( Point p )
  {
      int cellW = ( this.getWidth() / PlayerQuadrant.GRID_SIZE );
      int cellH = ( this.getHeight() / PlayerQuadrant.GRID_SIZE );

      int x = p.x / cellW;
      int y = p.y / cellH;

      boolean hit = quadrant.fireAtCell( x, y );
      this.repaint();
      return hit;
  }

  /**
   * Fires at a specific cell based on grid coordinates.
   *
   * @param x the x-coordinate of the cell
   * @param y the y-coordinate of the cell
   * @return true if the fire hit a ship; false otherwise
   */
  public boolean fireAtCell( int x, int y )
  {
      boolean result = quadrant.fireAtCell( x, y );
      owner.syncDecksToQuadrantState();
      return result;
  }

  /**
   * Draws ships onto the overlay panel if this panel is friendly.
   *
   * @param g the graphics context
   */
  protected void drawShips(Graphics g)
  {
      int cellW = (this.getWidth() / PlayerQuadrant.GRID_SIZE);
      int cellH = (this.getHeight() / PlayerQuadrant.GRID_SIZE);

      for (int i = 0; i < Player.FLEET_SIZE; i++)
      {
          Ship sp = owner.getFleet()[i];
          ShipHeading hg = owner.getShipLocations()[i];

          if (sp != null && hg != null)
          {
              int[] bnds = calculateShipBounds(hg, cellW, cellH, sp.getShipLength());
              ImageIcon shipImg = getCachedShipImage(
                  owner.getCiv(),
                  sp.getShipType(),
                  hg.getDirection()
              );
              g.drawImage(shipImg.getImage(), bnds[0], bnds[1], bnds[2], bnds[3], this);
          }
      }
  }

  /**
   * Gets the dimensions of a single cell in the grid.
   *
   * @return an array containing the width and height of a cell
   */
  public int[] getCellDimensions()
  {
      int cellWidth = overlayPanel.getWidth() / PlayerQuadrant.GRID_SIZE;
      int cellHeight = overlayPanel.getHeight() / PlayerQuadrant.GRID_SIZE;

      return new int[] { cellWidth, cellHeight };
  }

  /**
   * Calculates the rendering bounds for a ship based on its heading and length.
   *
   * @param hg   the ship's heading
   * @param cellW the width of a cell
   * @param cellH the height of a cell
   * @param loa   the length overall of the ship
   * @return an array containing the x, y, width, and height for the ship's bounds
   */
  public int[] calculateShipBounds(ShipHeading hg, int cellW, int cellH, int loa)
  {
      int[] bounds = new int[4]; // imgX, imgY, imgW, imgH
      int[] offsetVector = hg.getDirection().getOffsets();

      for (int i = 0; i < offsetVector.length; i++)
      {
          offsetVector[i] *= loa - 1;
      }

      int[] rear = hg.getRear();
      int[] front = {
          rear[ShipHeading.XIND] + offsetVector[Direction.XIND],
          rear[ShipHeading.YIND] + offsetVector[Direction.YIND]
      };

      switch (hg.getDirection())
      {
          case NORTH ->
          {
              bounds[0] = front[0] * cellW;
              bounds[1] = front[1] * cellH;
              bounds[2] = cellW;
              bounds[3] = cellH * loa;
          }
          case EAST ->
          {
              bounds[0] = rear[0] * cellW;
              bounds[1] = rear[1] * cellH;
              bounds[2] = cellW * loa;
              bounds[3] = cellH;
          }
          case SOUTH ->
          {
              bounds[0] = rear[0] * cellW;
              bounds[1] = rear[1] * cellH;
              bounds[2] = cellW;
              bounds[3] = cellH * loa;
          }
          case WEST ->
          {
              bounds[0] = front[0] * cellW;
              bounds[1] = front[1] * cellH;
              bounds[2] = cellW * loa;
              bounds[3] = cellH;
          }
      }
      return bounds;
  }
}
