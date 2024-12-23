package seasofyore;

import seasofyore.core.Player;
import seasofyore.core.Civilization;
import seasofyore.core.PlayerQuadrant;
import seasofyore.core.ShipType;
import seasofyore.core.Board;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.Timer;

/**
 * Controls the flow of the Seas of Yore game, managing phases, UI interactions, 
 * and game state. Handles the initialization of panels, game phases, and game-play 
 *  elements such as turn-taking and ship placement.
 * 
 * @author dylan connolly
 */
public class GameController extends JLayeredPane implements QuadrantListener
{
  /**
   * Runnable to return to the title screen. Not configured.
   */
  private final Runnable returnToTitle;

  /**
   * Indicates whether the game is in Salvo mode (multi-shot turns).
   */
  private final boolean salvoMode;

  /**
   * Offset width for the sidebar panel.
   */
  private static final int OFFSET = SidebarPanel.TOGGLE_BUTTON_WIDTH;

  /**
   * The game logic backend.
   */
  private Board board;

  /**
   * The current phase of the game.
   */
  private GamePhase currentPhase;

  /**
   * Panel displayed when a player wins the game.
   */
  private WinScreenPanel winScreen;

  /**
   * The main game panel containing the board and terminal.
   */
  private JPanel gamePanel;

  /**
   * The drag layer panel for handling drag-and-drop operations, and any
   * high-layer effects rendering that might need to be seen.
   */
  private JPanel dragLayerPanel;

  /**
   * The curtain panel for turn transitions.
   */
  private JPanel curtainPanel;

  /**
   * The terminal panel for displaying game messages and housing the
   *  cool flag button!
   */
  private TerminalPanel terminal;

  /**
   * The panel displaying the game board.
   */
  private BoardPanel boardPanel;          // panel for game board

  /**
   * The panel displaying the ship selection sidebar.
   */  
  private SidebarPanel sidebarPanel;

  /**
   * The player whose turn it currently is.
   */
  private Player current;

  /**
   * Timer controlling the curtain animations for turn transitions.
   */  
  private Timer curtainTimer;

  /**
   * Indicates if the curtain is fully closed.
   */
  private boolean curtainClosedState = false;

  /**
   * Indicates if the curtain is in the process of closing.
   */
  private boolean curtainClosingState = false;

  /**
   * The vertical position of the curtain.
   */
  private int curtainY;

  /**
   * The speed of the curtain movement in pixels per frame.
   */
  private final int curtainSpeed = 10; // speed of curtain movement
  
  
  /**
   * Constructs a new GameController and starts the game.
   *
   * @param salvo          true if the game is in Salvo mode; false otherwise
   * @param returnToTitle  a Runnable to return to the title screen
   */
  public GameController( boolean salvo, Runnable returnToTitle )
  {
    this.returnToTitle = returnToTitle;  // keep returnToTitle thread ready
    this.salvoMode = salvo;
    
    startGame();
  }
  
  /**
   * Gets the sidebar panel.
   *
   * @return the SidebarPanel instance
   */  
  public SidebarPanel getSidebarPanel()
  {
    return this.sidebarPanel;
  }
  
  /**
   * Gets the game board backend.
   *
   * @return the Board instance
   */
  public Board getBoard()
  {
    return this.board;
  }
  
  /**
   * Gets the panel displaying the game board.
   *
   * @return the BoardPanel instance
   */
  public BoardPanel getBoardPanel()
  {
    return this.boardPanel;
  }
  
  /**
   * Gets the player whose turn it currently is.
   *
   * @return the current player
   */
  public Player getCurrentPlayer()
  {
    return this.current;
  }
  
  /**
   * Gets the player whose turn is next.
   *
   * @return the next player
   */  
  public Player getNextPlayer()
  {
    return this.board.getNextPlayer();
  }
  
  /**
   * Gets the current player's quadrant.
   *
   * @return the current player's PlayerQuadrant
   */
  public PlayerQuadrant getCurrentQuadrant()
  {
    return current.getFriendlyQuad();
  }
  
  /**
   * Gets the next player's quadrant.
   *
   * @return the next player's PlayerQuadrant
   */
  public PlayerQuadrant getNextQuadrant()
  {
    return current.getEnemyQuad();
  }
  
  /**
   * Gets the current player's QuadrantPanel.
   *
   * @return the QuadrantPanel instance for the current player
   */
  public QuadrantPanel getCurrentQuadrantPanel()
  {
    return boardPanel.getFriendlyPanel();
  }
  
  /**
   * Gets the next player's QuadrantPanel.
   *
   * @return the QuadrantPanel instance for the next player
   */
  public QuadrantPanel getNextQuadrantPanel()
  {
    return boardPanel.getEnemyPanel();
  }
  
  /**
   * Gets the main game panel containing the board and terminal.
   *
   * @return the main game panel
   */
  public JPanel getGamePanel()
  {
    return this.gamePanel;
  }
  
  /**
   * Gets the drag layer panel for drag-and-drop operations.
   *
   * @return the drag layer panel
   */
  public JPanel getDragLayerPanel()
  {
    return this.dragLayerPanel;
  }
  
  /**
   * Gets the terminal panel for displaying game messages.
   *
   * @return the TerminalPanel instance
   */
  protected TerminalPanel getTerminalPanel()
  {
    return this.terminal;
  }
  
  /**
   * Gets the currently held ship from the sidebar.
   *
   * @return the ShipType of the selected ship
   */
  public ShipType getHeldSidebarShip()
  {
    return sidebarPanel.getSelectedShip();
  }
  
  /**
   * Gets the civilization of the current player.
   *
   * @return the Civilization of the current player
   */  
  public Civilization getCurrentPlayerCivilization()
  {
    return this.current.getCiv();
  }

  /**
   * Starts the game by initializing all game elements and phases. 
   * This method cannot be overridden by implementing subclasses.
   */
  public final void startGame()
  {
    this.removeAll();
    currentPhase = null;
    // initialize backend logic
    board = new Board();
    current = board.getCurrentPlayer();
    gamePanel = new JPanel( new BorderLayout() );
    dragLayerPanel = new JPanel( null ) 
    {
      @Override
      protected void paintComponent( Graphics g ) 
      {
        super.paintComponent( g );
        if ( currentPhase != null )
          currentPhase.render( g );
      }
    };
    dragLayerPanel.setOpaque( false );
    
    setLayout( null );  // null layout for foreground
    addGameplayElements();
    
    add( gamePanel, JLayeredPane.DEFAULT_LAYER );
    add( sidebarPanel, JLayeredPane.PALETTE_LAYER );
    add( dragLayerPanel, JLayeredPane.DRAG_LAYER );
    add( curtainPanel, JLayeredPane.POPUP_LAYER );
    
    initializeCurtainTimer();
    initializeMouseListeners();
    sidebarPanel.setActiveQuadrant( boardPanel.getFriendlyPanel() );
    
    this.addComponentListener( new ComponentAdapter()
    {
      @Override
      public void componentResized( ComponentEvent e )
      {
        updateComponentBounds();
        updateCurtainBounds();
        sidebarPanel.forceCloseSidebar();
      }
    });
    
    this.updateComponentBounds();
    this.updateCurtainBounds();
    updatePhase(); // updatePhase starts the game
  }
  
  /**
   * Updates the current game phase based on game state.
   */
  private void updatePhase()
  {
    if ( board.isPlacementFinal() )
    {
      logToTerminal( BattlePhase.HEARYE );
      logToTerminal( BattlePhase.FIREINS );
      setPhase( getBattleMode() );
    }
    
    else
      setPhase( new ShipPlacementPhase() );
  }
  
  /**
   * Sets the current game phase.
   *
   * @param phase the new GamePhase to set
   */
  public void setPhase( GamePhase phase )
  {
    if ( currentPhase != null )
    {
      currentPhase.cleanup();
    }
    this.currentPhase = phase;
    currentPhase.enterPhase( this );
    repaint();
  }
  
  /**
   * Gets the appropriate BattlePhase based on game mode.
   *
   * @return a BattlePhase or SalvoBattlePhase instance
   */
  public GamePhase getBattleMode()
  {
    return ( salvoMode ? new SalvoBattlePhase() : new BattlePhase() );
  }
  
  /**
   * Checks if the game is currently in the setup phase.
   *
   * @return true if in the setup phase; false otherwise
   */
  public boolean isSetupPhase()
  {
    return currentPhase instanceof ShipPlacementPhase;
  }
  
  /**
   * Handles a cell click event in a QuadrantPanel.
   *
   * @param x              the x-coordinate of the clicked cell
   * @param y              the y-coordinate of the clicked cell
   * @param quadrantPanel  the QuadrantPanel where the click occurred
   */
  @Override
  public void onCellClicked( int x, int y, QuadrantPanel quadrantPanel )
  {
    if ( currentPhase != null )
      currentPhase.handleCellClick( x, y, quadrantPanel );
    else
      logToTerminal( "Game phase not set. Click ignored" );
  }
  
  /**
   * Handles mouse movement within a QuadrantPanel.
   *
   * @param dragPt the current drag point
   * @param src    the QuadrantPanel where the movement occurred
   */
  @Override
  public void onMovement( Point dragPt, QuadrantPanel src )
  {
    if ( currentPhase != null )
      currentPhase.handleQuadrantMovement( dragPt, src );
    else
      logToTerminal( "Game phase not set. Movement ignored" );
  }
  
  /**
   * Logs a message to the terminal panel.
   *
   * @param message the message to log
   */
  public void logToTerminal( String message )
  {
    terminal.logMessage( "[" + current.getCiv() + "] " + message );
  }
  
  /**
   * Gets the current game phase.
   *
   * @return the current GamePhase instance
   */
  public GamePhase getCurrentPhase()
  {
    return currentPhase;
  }
  
  /**
   * Fetches and constructs an anonymous inner class that dictates
   * the movement of the curtainPanel, during both opening, closing, 
   * and closed states.
   */
  private void initializeCurtainTimer()
  {
    curtainTimer = new Timer( 5, ( ActionEvent e ) -> 
    {
      if ( curtainClosingState ) 
      {
        curtainY += curtainSpeed;
        if ( curtainY >= getHeight() )
        {
          curtainClosingState = false;
          curtainClosedState = true;
          curtainTimer.stop();
        }
        updateCurtainBounds();
      }
      else if ( !curtainClosedState )
      {
        curtainY -= curtainSpeed;
        if ( curtainY <= -getHeight() )
        {
          curtainY = -getHeight();
          curtainClosedState = false;
          curtainPanel.setVisible( false );
          curtainTimer.stop();
        }
      }
      updateCurtainBounds();
    });
  }
  
  /**
   *   Organization helper. Oversees the construction and
   * delegation of mouseListeners to the components who need
   *   to parse mouse events during any given game phase
   */
  private void initializeMouseListeners()
  {
    MouseAdapter sharedMouseAdapter = new MouseAdapter()
    {
      @Override
      public void mouseClicked( MouseEvent e )
      {
        // construct an adapter to delegate mouse
        // clicked events to AbstractGamePhase subclasses
        if ( currentPhase != null )
          currentPhase.handleInput( e );
      }
      
      @Override
      public void mouseMoved( MouseEvent e )
      {
        if ( currentPhase != null )
          currentPhase.handleInput( e );
      }
      
      @Override
      public void mouseWheelMoved( MouseWheelEvent e )
      {
        // construct an adapter to delegate mouse wheel
        //  events to AbstractGamePhase subclasses
        if ( currentPhase != null )
          currentPhase.handleInput( e );
      }
    };
    gamePanel.addMouseListener( sharedMouseAdapter );
    gamePanel.addMouseMotionListener( sharedMouseAdapter );
    gamePanel.addMouseWheelListener( sharedMouseAdapter );
  }
  
  /**
   * Highly generalized and versatile turn-switching method for the UI,
   * is tightly bound with the phase-specific methods during game-play
   */
  public void switchTurns()
  {
    currentPhase.cleanup();    
    dropCurtain();           // start curtainTimer with curtainClosingState
    
    Timer turnSwitchTimer;
    turnSwitchTimer = new Timer( getCurtainTime(), ( ActionEvent e ) -> 
    {
      swapStateAndUI();
      updatePhase();
      
      terminal.updateTurnButtonIcon();
    });
    turnSwitchTimer.setRepeats( false );
    
    turnSwitchTimer.start();
  }
  
  /**
   * Displays the win screen with the specified winner's name and actions for play again and exit.
   *
   * @param winner the name of the winning player or civilization
   */
  public void showWinScreen( String winner ) 
  {
    winScreen = new WinScreenPanel
    ( 
      winner,
      e -> 
      {
        // play again actionPerformed listener via lambda
        remove( winScreen );
        revalidate();
        repaint();
        startGame();
      },
      // exit actionPerformed listener via lambda
      e -> System.exit( 0 )
    );

    // add the win screen to the modal layer
    winScreen.setBounds( 0, 0, getWidth(), getHeight() );
    add( winScreen, JLayeredPane.MODAL_LAYER );
    revalidate();
    repaint();
  }
  
  /**
   * Swaps the current game state and updates the UI to reflect the player's turn 
   * and board. Updates the current player and swaps the panels in the BoardPanel.
   */
  private void swapStateAndUI()
  {
    board.switchTurns();
    boardPanel.swapPanels();
    current = board.getCurrentPlayer();
  }
  
  /**
   * Updates the sidebar to reflect the active quadrant of the current player.
   */
  public void applyNewSidebar()
  {
    sidebarPanel.setActiveQuadrant( getCurrentQuadrantPanel() );
  }
  
  /**
   * Adds gameplay elements to the main game panel, including the board, sidebar, curtain,
   * and terminal panels. Initializes these elements and configures their layout.
   */
  private void addGameplayElements()
  {
    sidebarPanel = new SidebarPanel();
    this.initializeBoardPanel();
    
    gamePanel.add( boardPanel, BorderLayout.CENTER );
    gamePanel.add( sidebarPanel, BorderLayout.EAST );
    
    curtainPanel = createCurtain();
    gamePanel.add( curtainPanel, BorderLayout.NORTH );
    
    this.initializeTerminalPanel();
  }
  
  /**
   * Initializes the BoardPanel with the current and next QuadrantPanels from the game board.
   * Binds the QuadrantPanels to the GameController and sets up the listeners.
   */
  private void initializeBoardPanel()
  {
    QuadrantPanel currentPanel = board.getCurrentQuadrantPanel();
    QuadrantPanel nextPanel = board.getNextQuadrantPanel();
    
    currentPanel.setGameController( this );
    nextPanel.setGameController( this );
    
    currentPanel.setQuadrantListener( this );
    nextPanel.setQuadrantListener( this );
    
    boardPanel = new BoardPanel( currentPanel, nextPanel );
  }
  
  /**
   * Initializes the terminal panel and configures the turn button.
   * Adds the terminal panel to the main game panel.
   */
  private void initializeTerminalPanel()
  {
    terminal = new TerminalPanel( e -> switchTurns(), this );
    terminal.updateTurnButtonIcon();
    gamePanel.add( terminal, BorderLayout.SOUTH );
  }
  
  /**
  * Creates the curtain panel used for turn transitions.
  * Includes a revealer button to manually open the curtain.
  *
  * @return the curtain panel
  */
  private JPanel createCurtain()
  {
    curtainY = 0;
    JPanel curtain = new JPanel();
    curtain.setOpaque( true );
    curtain.setBackground( new Color( 16, 0, 22 ) );
    curtain.setLayout( null );

    JButton revealer = new JButton( "UNHIDE CURTAIN" );
    revealer.setBounds( getWidth() / 2 - 125, getHeight() / 2 - 25, 250, 50 );
    revealer.addActionListener( e -> openCurtain() );
    revealer.setVisible( true ); // initially hidden
    curtain.add( revealer );

    curtain.addComponentListener( new ComponentAdapter() 
    {
      @Override
      public void componentResized( ComponentEvent e ) 
      {
        revealer.setBounds( getWidth() / 2 - 125, getHeight() / 2 - 25, 250, 50 );
      }
    });
    return curtain;
  }
  
  /**
   * Drops the curtain during turn transitions, closing the sidebar and disabling 
   * the terminal's turn button. Starts the curtain animation timer.
   */
  private void dropCurtain()
  {
    sidebarPanel.forceCloseSidebar();
    curtainClosingState = true;
    curtainClosedState = false;
    curtainY = 0;
    curtainPanel.setVisible( true );
    terminal.setTurnButtonEnabled( false );
    curtainTimer.start();
  }
  
  /**
   * Opens the curtain during turn transitions and starts the curtain 
   * animation timer.
   */
  private void openCurtain()
  {
    curtainClosedState = false;
    curtainTimer.start();
  }
  
  /**
   * Calculates the total time required for the curtain animation.
   *
   * @return the total animation time in milliseconds
   */
  private int getCurtainTime()
  {
    int animationFrames = getHeight() / curtainSpeed; // total frames needed
    int msDelayFrame = 15; // time delay between frames (milliseconds)

    return animationFrames * msDelayFrame; // total animation time in milliseconds
  }
  
  /**
   * Updates the bounds of components in the GameController, ensuring proper layout adjustments.
   * Includes updates for the game panel, sidebar, drag layer, terminal, and curtain.
   */
  private void updateComponentBounds()
  {
    int width = this.getWidth();
    int height = this.getHeight();
    
    if ( curtainPanel != null )
      updateCurtainBounds();

    if ( gamePanel != null )
      gamePanel.setBounds( 0, 0, width, height );
    
    if ( sidebarPanel != null )
      sidebarPanel.setBounds( width - OFFSET, 0, width, height );
    
    if ( dragLayerPanel != null )
      dragLayerPanel.setBounds( gamePanel.getBounds() );
    
    if ( terminal != null )
      terminal.setBounds( 0, boardPanel.getY(), width, height );
    
    this.revalidate();
    this.repaint();
  }
  
  /**
   * Updates the bounds of the curtain panel, ensuring it is correctly positioned 
   * based on its state. Adjusts the position when the curtain is fully closed.
   */
  public void updateCurtainBounds()
  {
    int width = this.getWidth();
    int height = this.getHeight();
    
    if ( curtainClosedState ) // curtain fully closed, enforce
      curtainY = height;
      
    curtainPanel.setBounds( 0, curtainY - height, width, height );
  }
}