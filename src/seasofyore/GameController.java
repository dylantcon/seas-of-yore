package seasofyore;

import seasofyore.ui.SidebarPanel;
import seasofyore.ui.BoardPanel;
import seasofyore.ui.CurtainPanel;
import seasofyore.ui.PlacementToolbar;
import seasofyore.ui.QuadrantPanel;
import seasofyore.ui.SavedMatchDialogs;
import seasofyore.ui.TerminalPanel;
import seasofyore.ui.WinScreenPanel;
import seasofyore.ui.PauseMenuPanel;
import seasofyore.core.Player;
import seasofyore.core.Civilization;
import seasofyore.core.MatchConfig;
import seasofyore.core.PlayerQuadrant;
import seasofyore.core.PlayerType;
import seasofyore.core.SavedMatch;
import seasofyore.core.ShipType;
import seasofyore.core.Board;
import seasofyore.match.MatchHandler;
import seasofyore.match.NetworkedMatchHandler;
import seasofyore.match.OfflineMatchHandler;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Point;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
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
   * Runnable to return to the title screen.
   */
  private final Runnable returnToTitle;

  /**
   * Indicates whether the game is in Salvo mode (multi-shot turns).
   */
  private final boolean salvoMode;

  /**
   * Whether attacks ride the falling-stone animation or resolve instantly.
   * A player preference chosen on the battle-setup screen.
   */
  private final boolean stoneAnimationsEnabled;

  /**
   * The locality seam: every decision that depends on where the players
   * physically are (curtain, pause, chat, who is "local") is delegated here.
   * Offline by default; networked games will install a networked handler.
   */
  private final MatchHandler matchHandler;

  /**
   * A board restored from a saved game, consumed by the next startGame()
   * instead of building a fresh one. Null in ordinary games and after the
   * restored game has begun (so "play again" rebuilds from scratch).
   */
  private Board injectedBoard;

  /**
   * The pause overlay currently showing, or null when the game is live.
   */
  private PauseMenuPanel pausePanel;

  /**
   * Whether the game is paused (or showing the networked forfeit overlay).
   */
  private boolean paused = false;

  /**
   * Whether the curtain was mid-animation when the game paused, so resume
   * can restart it.
   */
  private boolean curtainWasMoving = false;

  /**
   * The kind of player controlling the Britons (human or an AI tier).
   */
  private PlayerType britonsType = PlayerType.HUMAN;

  /**
   * The kind of player controlling the Franks (human or an AI tier).
   */
  private PlayerType franksType = PlayerType.HUMAN;
  
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
   * The curtain for hot-seat turn transitions. Owns its own motion, states,
   * and reveal button; the controller only drops it, opens it, and pauses it.
   */
  private CurtainPanel curtain;

  /**
   * The editor panel visible for human players during the ShipPlacementPhase
   */
  private PlacementToolbar toolbarPanel;

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
   * Represents the fixed size of the toolbar buttons
   */
  private static final int TB_BUTTON_DIM = 75;
  
  /**
   * Whether the battle-phase fanfare has been logged yet this game.
   */
  private boolean battleAnnounced = false;

  /**
   * The human commanders' chosen names, applied to freshly built boards
   * (restored boards already carry theirs).
   */
  private final String britonsName;
  private final String franksName;

  /**
   * Constructs a GameController for a fresh match. Everything situational --
   * who commands each civilization, their chosen names, the rules variant,
   * and presentation preferences -- arrives in one configuration value.
   *
   * @param config        the assembled match configuration
   * @param returnToTitle a Runnable to return to the title screen
   */
  public GameController( MatchConfig config, Runnable returnToTitle )
  {
    this.returnToTitle = returnToTitle;
    this.salvoMode = config.isSalvoMode();
    this.stoneAnimationsEnabled = config.useStoneAnimations();
    this.britonsType = config.getBritonsType();
    this.franksType = config.getFranksType();
    this.britonsName = config.getBritonsName();
    this.franksName = config.getFranksName();
    this.matchHandler = new OfflineMatchHandler();

    startGame();
  }

  /**
   * Constructs a GameController that resumes a match restored from disk. The
   * saved board (players, fleets, wounds, names, AI state, whose turn it is)
   * is used as-is for the first game; choosing "play again" afterwards
   * rebuilds a fresh match between the same kinds of players.
   *
   * @param saved         the bottled match
   * @param returnToTitle a Runnable to return to the title screen
   */
  public GameController( SavedMatch saved, Runnable returnToTitle )
  {
    this.returnToTitle = returnToTitle;
    this.salvoMode = saved.isSalvoMode();
    this.stoneAnimationsEnabled = true;
    this.britonsType = saved.getBoard().getBritonsType();
    this.franksType = saved.getBoard().getFranksType();
    this.britonsName = saved.getBoard().getBritons().getName();
    this.franksName = saved.getBoard().getFranks().getName();
    this.matchHandler = new OfflineMatchHandler();
    this.injectedBoard = saved.getBoard();

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
   * Gets the current player's QuadrantPanel, resolved by who owns the panel
   * rather than by which screen slot it occupies. In hot-seat games the
   * bottom (revealed) slot always belongs to the current player, but in solo
   * games the human keeps the bottom slot even during the AI's turn -- so
   * slot position alone cannot identify whose panel is whose.
   *
   * @return the QuadrantPanel instance for the current player
   */
  public QuadrantPanel getCurrentQuadrantPanel()
  {
    return getPanelOwnedBy( current );
  }

  /**
   * Gets the next player's QuadrantPanel, resolved by owner.
   *
   * @return the QuadrantPanel instance for the next player
   */
  public QuadrantPanel getNextQuadrantPanel()
  {
    return getPanelOwnedBy( board.getNextPlayer() );
  }

  /**
   * Finds the QuadrantPanel owned by the given player, regardless of whether
   * it currently sits in the friendly (bottom, revealed) or enemy (top,
   * hidden) slot of the BoardPanel.
   *
   * @param p the player whose panel to find
   * @return the panel owned by that player
   */
  private QuadrantPanel getPanelOwnedBy( Player p )
  {
    QuadrantPanel friendly = boardPanel.getFriendlyPanel();
    return ( friendly.getOwner() == p ) ? friendly : boardPanel.getEnemyPanel();
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
    battleAnnounced = false;
    paused = false;
    pausePanel = null;
    // initialize backend logic: a restored board if one was injected,
    // otherwise a fresh match between the configured player kinds
    if ( injectedBoard != null )
    {
      board = injectedBoard;
      injectedBoard = null; // a later "play again" rebuilds from scratch
    }
    else
    {
      board = new Board( britonsType, franksType );
      board.getBritons().setName( britonsName );
      board.getFranks().setName( franksName );
    }
    board.prepareForPlay();   // settle AI setup so only humans see placement UI
    matchHandler.beginMatch( this );
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
    initializeToolbar();
    
    add( gamePanel, JLayeredPane.DEFAULT_LAYER );
    add( sidebarPanel, JLayeredPane.PALETTE_LAYER );
    add( dragLayerPanel, JLayeredPane.DRAG_LAYER );
    add( curtain, JLayeredPane.POPUP_LAYER );

    initializeMouseListeners();
    initializePauseKeyBinding();
    terminal.setChatAvailable( matchHandler.supportsChat() );
    sidebarPanel.setActiveQuadrant( boardPanel.getFriendlyPanel() );

    this.addComponentListener( new ComponentAdapter()
    {
      @Override
      public void componentResized( ComponentEvent e )
      {
        updateComponentBounds();
        curtain.updateBounds();
        sidebarPanel.forceCloseSidebar();
      }
    });

    this.updateComponentBounds();
    curtain.updateBounds();
    updatePhase(); // updatePhase starts the game
  }
  
  /**
   * Updates the current game phase based on game state.
   */
  private void updatePhase()
  {
    // Get appropriate phase based on game state and next player type
    GamePhase phase;

    if ( board.isPlacementFinal() )
    {
      // Check if current player is AI
      if ( getCurrentPlayer().isAutonomous() )
      {
        phase = PhaseFactory.createAITurnPhase( salvoMode );
      } 
      else 
      {
        phase = PhaseFactory.createBattlePhase( salvoMode );
      }
    } 
    else 
    {
      phase = PhaseFactory.createShipPlacementPhase();
    }

    setPhase( phase );
  }
  
  /**
   * Sets the current game phase.
   *
   * @param phase the new GamePhase to set
   */
  public void setPhase( GamePhase phase )
  {
    if ( currentPhase != null )
      currentPhase.cleanup();
    
    this.currentPhase = phase;
    currentPhase.enterPhase( this );
    repaint();
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
   * Logs a message to the terminal panel, with the current civilization's
   * prefix coloured in its banner's hue: Britons red, Franks blue.
   *
   * @param message the message to log
   */
  public void logToTerminal( String message )
  {
    terminal.logMessage( civColour( current.getCiv() ) + TerminalPanel.BOLD
                       + "[" + current.getCiv() + "]" + TerminalPanel.RESET
                       + " " + message );
  }

  /**
   * The terminal colour code for a civilization's banner.
   *
   * @param civ the civilization
   * @return the ANSI colour its prefix is written in
   */
  private static String civColour( Civilization civ )
  {
    return ( civ == Civilization.BRITONS ) ? TerminalPanel.RED
                                           : TerminalPanel.BLUE;
  }

  /**
   * Logs the battle-phase fanfare exactly once per game, the first time a
   * battle turn actually begins. The battle phases call this from onEnter
   * so the announcement lands wherever setup happens to end, rather than
   * being tied to game start (where placement is rarely final).
   */
  public void announceBattleStart()
  {
    if ( battleAnnounced )
      return;

    battleAnnounced = true;
    logToTerminal( BattlePhase.HEARYE );
    logToTerminal( BattlePhase.FIREINS );
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
        // forwards mouse movement events to AbstractGamePhase subclasses,
        //  primarily for updating the draggable ship silhouette
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
    configureMouseObservation( gamePanel, sharedMouseAdapter );
    configureMouseObservation( sidebarPanel, sharedMouseAdapter );
  }
  
  private void configureMouseObservation( JComponent comp, MouseAdapter adpt )
  {
    comp.addMouseListener( adpt );
    comp.addMouseMotionListener( adpt );
    comp.addMouseWheelListener( adpt );
  }
  
  /**
   * Highly generalized and versatile turn-switching method for the UI,
   * is tightly bound with the phase-specific methods during game-play
   */
  public void switchTurns()
  {
    currentPhase.cleanup();
    terminal.setTurnButtonEnabled( false );
    matchHandler.onTurnEnded();

    // Whether this handoff needs the secrecy curtain is the match handler's
    // call: only two humans sharing one screen have fleets to hide from
    // each other. When the curtain drops, the swap runs the moment it has
    // fully closed; otherwise the handoff is immediate (but still async, so
    // the calling phase finishes unwinding first).
    if ( matchHandler.showsCurtain() )
    {
      sidebarPanel.forceCloseSidebar();
      curtain.drop( this::advanceTurn );
    }
    else
    {
      Timer turnSwitchTimer = new Timer( 0, ( ActionEvent e ) -> advanceTurn() );
      turnSwitchTimer.setRepeats( false );
      turnSwitchTimer.start();
    }
  }

  /**
   * Performs the actual handoff: swaps the board and UI to the next player
   * and enters their phase. Runs behind a fully closed curtain in hot-seat
   * games, immediately otherwise.
   */
  private void advanceTurn()
  {
    swapStateAndUI();
    setPhase( PhaseFactory.createNextTurnPhase( this, salvoMode ) );
  }
  
  /**
   * Displays the end-of-game screen, aware of the local player's goal. The
   * victory/defeat distinction tracks the same line the curtain does: games
   * with two humans (the curtain games) always have a winner at the keyboard,
   * so they -- and spectated matches -- only ever celebrate the winner.
   * Defeat is shown solely in curtainless solo games, when the lone human's
   * fleet is the one beneath the waves; their own flag burns. When networked
   * play distinguishes a local player from a remote one, the same branch
   * decides for the local side.
   *
   * @param winner the civilization that won the game
   */
  public void showWinScreen( Civilization winner )
  {
    Player localPlayer = matchHandler.getLocalPlayer();
    boolean defeat = ( localPlayer != null && localPlayer.getCiv() != winner );
    Civilization featured = defeat ? localPlayer.getCiv() : winner;

    winScreen = new WinScreenPanel
    (
      featured, defeat, ( ActionEvent e ) -> {
        // play again actionPerformed listener
        remove( winScreen );
        this.startGame();
        returnToTitle.run();

    },
      // exit actionPerformed listener via lambda
      ( ActionEvent e ) -> System.exit( 0 )
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
    // Remember whether the player receiving the turn still owes a fleet:
    // board.switchTurns() auto-places for an AI in that position, and the
    // event deserves the same announcement a human's "Fleet placed!" gets.
    Player handedTo = board.getNextPlayer();
    boolean fleetWasPending = handedTo.isAutonomous()
                           && !handedTo.hasPlacedAllShips();

    // Always update the game state
    board.switchTurns();
    current = board.getCurrentPlayer();

    if ( fleetWasPending && handedTo.hasPlacedAllShips() )
      terminal.logMessage( civColour( handedTo.getCiv() ) + TerminalPanel.BOLD
                         + "[" + handedTo.getCiv() + "]" + TerminalPanel.RESET
                         + " " + TerminalPanel.GOLD + "The enemy commander "
                         + "hath deployed their fleet!" + TerminalPanel.RESET );

    // Update the turn indicator
    terminal.updateTurnButtonIcon();

    // Only hot-seat games rearrange the screen: the bottom slot is the
    // revealed one, so it must follow whichever human is acting. With at most
    // one human the arrangement never changes -- the lone human (or, when
    // spectating, the Britons) keeps the bottom slot for the whole game, and
    // the phases resolve panels by owner rather than by slot.
    if ( board.getHumanCount() == 2 )
      boardPanel.swapPanels();
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

    curtain = new CurtainPanel( this );

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

    // when spectating an AI-vs-AI match there is no hidden side, so reveal both
    // fleets to the watching human
    boardPanel.setRevealBoth( board.isSpectating() );
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
  
  private void initializeToolbar()
  {
    toolbarPanel = new PlacementToolbar( this );
  }
  
  public void addToolbar()
  {
    add( toolbarPanel, JLayeredPane.PALETTE_LAYER );
    updateComponentBounds();
    repaint();
  }
  
  public void removeToolbar()
  {
    remove( toolbarPanel );
    updateComponentBounds();
    repaint();
  }
  
  
  /**
   * Updates the bounds of components in the GameController, ensuring proper layout adjustments.
   * Includes updates for the game panel, sidebar, drag layer, terminal, and curtain.
   */
  private void updateComponentBounds()
  {
    int width = this.getWidth();
    int height = this.getHeight();

    if ( curtain != null )
      curtain.updateBounds();

    if ( gamePanel != null )
      gamePanel.setBounds( 0, 0, width, height );
    
    if ( sidebarPanel != null )
      sidebarPanel.setBounds( width - OFFSET, 0, width, height );
    
    if ( dragLayerPanel != null )
      dragLayerPanel.setBounds( gamePanel.getBounds() );
    
    if ( isSetupPhase() && toolbarPanel != null )
    {
      toolbarPanel.setBounds( 0, ( height / 2 ) - TB_BUTTON_DIM, 
        TB_BUTTON_DIM, ( height / 2 ) + TB_BUTTON_DIM );
    }
    
    if ( terminal != null )
      terminal.setBounds( 0, boardPanel.getY(), width, height );
    
    this.revalidate();
    this.repaint();
  }
  
  /**
   * Gets this match's locality handler.
   *
   * @return the match handler
   */
  public MatchHandler getMatchHandler()
  {
    return this.matchHandler;
  }

  /**
   * Gets the terminal panel. Public so match handlers can deliver text
   * (e.g. incoming chat) into the log.
   *
   * @return the terminal panel
   */
  public TerminalPanel getTerminal()
  {
    return this.terminal;
  }

  /**
   * Whether attacks should ride the falling-stone animation. Phases consult
   * this and resolve shots instantly when the player turned the show off.
   *
   * @return true to animate attacks
   */
  public boolean useStoneAnimations()
  {
    return this.stoneAnimationsEnabled;
  }

  /**
   * Routes a chat line typed in the terminal to the network. Quietly ignored
   * if the match is not networked (the field is hidden then anyway).
   *
   * @param message the text the local player typed
   */
  public void sendChatMessage( String message )
  {
    if ( matchHandler instanceof NetworkedMatchHandler )
      ( (NetworkedMatchHandler) matchHandler ).sendChat( message );
  }

  /**
   * Registers the Escape key to open and close the pause overlay, on the
   * WHEN_IN_FOCUSED_WINDOW map so it works no matter which child has focus.
   */
  private void initializePauseKeyBinding()
  {
    getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW )
      .put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), "togglePause" );
    getActionMap().put( "togglePause", new AbstractAction()
    {
      @Override
      public void actionPerformed( ActionEvent e )
      {
        togglePause();
      }
    });
  }

  /**
   * Opens or closes the pause overlay. In offline matches the game truly
   * freezes -- phase timers and any stone mid-flight included; in networked
   * matches nothing can freeze, so the overlay only offers forfeiture while
   * the battle rages on behind it.
   */
  public void togglePause()
  {
    if ( winScreen != null && winScreen.getParent() == this )
      return; // the match is over; the end screen owns the stage

    if ( paused )
      resumeGame();
    else
      pauseGame();
  }

  /**
   * Raises the pause overlay, freezing the match when the handler allows it.
   */
  private void pauseGame()
  {
    boolean freeze = matchHandler.supportsPause();

    if ( freeze )
    {
      if ( currentPhase != null )
        currentPhase.pause();

      curtainWasMoving = curtain.isMoving();
      if ( curtainWasMoving )
        curtain.pauseMotion();
    }

    pausePanel = new PauseMenuPanel( this, freeze );
    pausePanel.setBounds( 0, 0, getWidth(), getHeight() );
    add( pausePanel, JLayeredPane.MODAL_LAYER );
    paused = true;
    revalidate();
    repaint();
  }

  /**
   * Dismisses the pause overlay and thaws whatever pauseGame froze.
   */
  private void resumeGame()
  {
    if ( pausePanel != null )
    {
      remove( pausePanel );
      pausePanel = null;
    }

    if ( matchHandler.supportsPause() )
    {
      if ( currentPhase != null )
        currentPhase.resume();

      if ( curtainWasMoving )
        curtain.resumeMotion();
      curtainWasMoving = false;
    }

    paused = false;
    revalidate();
    repaint();
  }

  /**
   * Abandons the current match and returns to the title screen, clearing any
   * pause state on the way out.
   */
  public void abandonToTitle()
  {
    if ( paused )
      resumeGame();

    matchHandler.shutdown();
    startGame();
    returnToTitle.run();
  }

  /**
   * Prompts for a destination and bottles the match to disk. Offered from
   * the pause menu once both fleets are placed; the dialog plumbing lives
   * in SavedMatchDialogs.
   */
  public void saveGameViaDialog()
  {
    File file = SavedMatchDialogs.saveViaDialog(
        this, new SavedMatch( board, salvoMode ) );

    if ( file != null )
      terminal.logMessage( TerminalPanel.GREEN + "Voyage committed to the log: "
                         + file.getName() + TerminalPanel.RESET );
  }

}