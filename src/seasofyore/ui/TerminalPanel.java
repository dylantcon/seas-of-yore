/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore.ui;

import seasofyore.core.Civilization;
import seasofyore.core.PlayerType;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import seasofyore.GameController;

/**
 * The ship's log: a styled terminal strip along the bottom of the game. It
 * renders messages through a {@link JTextPane} with support for a practical
 * subset of ANSI SGR escape codes (reset, bold, italic, and the 8 + 8 colour
 * codes), so any caller can colour its words the way a real terminal would:
 * {@code log( TerminalPanel.RED + "A HIT!" + TerminalPanel.RESET )}.
 * <p>
 * Above the log scrolls a status line naming the commander whose turn it is
 * -- humans by honorific, AI tiers by their tavern nickname and difficulty --
 * and, in networked matches, a chat field surfaces beneath it so words can be
 * exchanged across the water.
 *
 * @author dylan
 */
public class TerminalPanel extends JPanel
{
  // ------------------------------------------------------------------
  // ANSI SGR escape codes callers can embed in messages
  // ------------------------------------------------------------------

  /** The ANSI escape character (0x1B) that opens every SGR sequence. */
  private static final char ESC = (char) 27;

  /** Resets all styling to the terminal's defaults. */
  public static final String RESET = ESC + "[0m";
  /** Bold weight. */
  public static final String BOLD = ESC + "[1m";
  /** Italic slant. */
  public static final String ITALIC = ESC + "[3m";
  /** Muted grey -- asides and procedure text. */
  public static final String GREY = ESC + "[90m";
  /** Battle red -- hits, blood, the Britons' banner. */
  public static final String RED = ESC + "[31m";
  /** Sea green -- prompts that invite action. */
  public static final String GREEN = ESC + "[32m";
  /** Illuminated gold -- fanfares and victories. */
  public static final String GOLD = ESC + "[33m";
  /** Deep blue -- misses, open water, the Franks' banner. */
  public static final String BLUE = ESC + "[34m";
  /** Royal magenta. */
  public static final String MAGENTA = ESC + "[35m";
  /** Cold cyan -- whispers and chat. */
  public static final String CYAN = ESC + "[36m";
  /** Bright white -- emphasis without ceremony. */
  public static final String WHITE = ESC + "[37m";

  /**
   * The terminal's parchment-on-ink palette for the 30-37 colour codes, in
   * ANSI order: black, red, green, yellow, blue, magenta, cyan, white. The
   * 90-97 "bright" codes map to lightened versions of the same entries.
   */
  private static final Color[] PALETTE =
  {
    new Color( 0x5A5246 ), // black -> weathered grey-brown (still legible)
    new Color( 0xD9534F ), // red
    new Color( 0x7FB069 ), // green
    new Color( 0xE8C97C ), // yellow -> illuminated gold
    new Color( 0x6FA0CF ), // blue
    new Color( 0xB07FB0 ), // magenta
    new Color( 0x6FC3C3 ), // cyan
    new Color( 0xEDE7D6 )  // white -> bright parchment
  };

  /** The terminal's background: near-black ink with a violet cast. */
  private static final Color TERM_BG = new Color( 13, 10, 18 );

  /** The terminal's default text colour: aged parchment. */
  private static final Color TERM_FG = new Color( 216, 205, 180 );

  /** The prompt glyph colour: a dimmed gold. */
  private static final Color PROMPT_FG = new Color( 158, 134, 82 );

  /** Fixed pixel sizes for the strip's interior layout. */
  private static final int PANEL_H = 100;
  private static final int BUTTON_W = 135;
  private static final int STATUS_H = 14;
  private static final int CHAT_H = 24;

  /** The one font every part of the terminal strip speaks in. */
  private static final Font TERM_FONT = new Font( "Monospaced", Font.BOLD, 10 );

  private final GameController controller;
  private final JTextPane terminalPane;
  private final JScrollPane scrollPane;
  private final JButton endTurnButton;
  private final javax.swing.JLabel statusLabel;
  private final JTextField chatField;

  private boolean chatAvailable = false;

  private ImageIcon currentIcon;

  public TerminalPanel( ActionListener endTurnListener, GameController game )
  {
    // Set layout and background
    setLayout( null );
    setBackground( TERM_BG );
    controller = game;

    // the log itself: a styled pane so ANSI colour runs render faithfully
    terminalPane = new JTextPane();
    terminalPane.setEditable( false );
    terminalPane.setFont( TERM_FONT );
    terminalPane.setForeground( TERM_FG );
    terminalPane.setBackground( TERM_BG );
    terminalPane.setCaretColor( TERM_FG );

    // enable auto-scrolling
    DefaultCaret caret = ( DefaultCaret ) terminalPane.getCaret();
    caret.setUpdatePolicy( DefaultCaret.ALWAYS_UPDATE );

    // wrap the terminal pane in a scroll pane
    scrollPane = new JScrollPane( terminalPane );
    scrollPane.setBorder( BorderFactory.createEmptyBorder() );

    // the status line: who commands the current turn
    statusLabel = new javax.swing.JLabel();
    statusLabel.setFont( TERM_FONT );
    statusLabel.setForeground( PALETTE[3] );
    statusLabel.setBackground( new Color( 24, 18, 32 ) );
    statusLabel.setOpaque( true );
    statusLabel.setBorder( BorderFactory.createEmptyBorder( 0, 8, 0, 8 ) );

    // the chat line: hidden until a match handler that supports chat appears
    chatField = new JTextField();
    chatField.setFont( TERM_FONT );
    chatField.setForeground( PALETTE[6] );
    chatField.setBackground( new Color( 20, 16, 28 ) );
    chatField.setCaretColor( PALETTE[6] );
    chatField.setBorder( BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder( 1, 0, 0, 0, new Color( 60, 50, 80 ) ),
        BorderFactory.createEmptyBorder( 2, 8, 2, 8 ) ) );
    chatField.setVisible( false );
    chatField.addActionListener( e ->
    {
      String text = chatField.getText().trim();
      if ( !text.isEmpty() )
        controller.sendChatMessage( text );
      chatField.setText( "" );
    });

    // initialize end turn button
    endTurnButton = new JButton();
    endTurnButton.setEnabled( false ); // initially disabled
    endTurnButton.addActionListener( endTurnListener );
    endTurnButton.setFocusPainted( false );

    endTurnButton.addComponentListener( new ComponentAdapter()
    {
      @Override
      public void componentResized( ComponentEvent e )
      {
        updateTurnButtonIcon();
      }
    });

    add( endTurnButton );
    add( statusLabel );
    add( scrollPane );
    add( chatField );
  }

  /**
   * Adds a message to the terminal log. The message may contain ANSI SGR
   * escape sequences; they are interpreted, not displayed.
   *
   * @param message the (possibly colour-coded) message
   */
  public void logMessage( String message )
  {
    StyledDocument doc = terminalPane.getStyledDocument();

    // prompt glyph in dim gold, then the styled message body
    SimpleAttributeSet promptAttrs = baseAttributes();
    StyleConstants.setForeground( promptAttrs, PROMPT_FG );
    insertQuietly( doc, "> ", promptAttrs );

    appendAnsi( doc, message );
    insertQuietly( doc, "\n", baseAttributes() );
  }

  /**
   * Walks a message left to right, splitting it into runs at each ANSI
   * escape sequence. Escape sequences mutate the current attribute set; the
   * text between them is inserted with a snapshot of those attributes.
   *
   * @param doc     the document to append into
   * @param message the raw message text
   */
  private void appendAnsi( StyledDocument doc, String message )
  {
    SimpleAttributeSet attrs = baseAttributes();
    int i = 0;

    while ( i < message.length() )
    {
      if ( message.charAt( i ) == ESC
        && i + 1 < message.length() && message.charAt( i + 1 ) == '[' )
      {
        int end = message.indexOf( 'm', i + 2 );
        if ( end > 0 )
        {
          applySgrCodes( message.substring( i + 2, end ), attrs );
          i = end + 1;
          continue;
        }
      }

      int next = message.indexOf( ESC, i + 1 );
      if ( next < 0 )
        next = message.length();

      // snapshot the attributes: later escape codes must not restyle
      // text that was inserted before them
      insertQuietly( doc, message.substring( i, next ),
                     new SimpleAttributeSet( attrs ) );
      i = next;
    }
  }

  /**
   * Applies one escape sequence's semicolon-separated SGR codes to the
   * running attribute set. Unknown codes are ignored, as a terminal would.
   *
   * @param codes the text between "ESC[" and "m"
   * @param attrs the attribute set to mutate
   */
  private void applySgrCodes( String codes, SimpleAttributeSet attrs )
  {
    for ( String token : codes.split( ";" ) )
    {
      int code;
      try
      {
        code = token.isEmpty() ? 0 : Integer.parseInt( token.trim() );
      }
      catch ( NumberFormatException ex )
      {
        continue; // malformed code: skip it, keep the rest
      }

      if ( code == 0 )
      {
        attrs.removeAttributes( attrs );
        attrs.addAttributes( baseAttributes() );
      }
      else if ( code == 1 )
        StyleConstants.setBold( attrs, true );
      else if ( code == 3 )
        StyleConstants.setItalic( attrs, true );
      else if ( code >= 30 && code <= 37 )
        StyleConstants.setForeground( attrs, PALETTE[code - 30] );
      else if ( code >= 90 && code <= 97 )
        StyleConstants.setForeground( attrs, PALETTE[code - 90].brighter() );
    }
  }

  /**
   * The terminal's default text attributes: bold parchment monospace.
   *
   * @return a fresh attribute set
   */
  private SimpleAttributeSet baseAttributes()
  {
    SimpleAttributeSet attrs = new SimpleAttributeSet();
    StyleConstants.setForeground( attrs, TERM_FG );
    StyleConstants.setBold( attrs, true );
    return attrs;
  }

  /**
   * Inserts text at the end of the document, swallowing the checked
   * BadLocationException that cannot occur when appending at the length.
   *
   * @param doc   the document
   * @param text  the text to append
   * @param attrs the attributes to style it with
   */
  private void insertQuietly( StyledDocument doc, String text,
                              SimpleAttributeSet attrs )
  {
    try
    {
      doc.insertString( doc.getLength(), text, attrs );
    }
    catch ( BadLocationException ex )
    {
      // appending at doc.getLength() is always a valid location
    }
  }

  // enable or disable the end turn button
  public void setTurnButtonEnabled( boolean enabled )
  {
    endTurnButton.setEnabled( enabled );
  }

  /**
   * Shows or hides the chat line. The controller calls this once the match
   * handler is known: chat exists only across a network.
   *
   * @param available true to surface the chat field
   */
  public void setChatAvailable( boolean available )
  {
    chatAvailable = available;
    chatField.setVisible( available );
    revalidate();
    repaint();
  }

  private ImageIcon getButtonIcon( Civilization c )
  {
    if ( c == Civilization.BRITONS )
      return new ImageIcon( getClass().getResource( "/images/BRITONS.png" ) );
    return new ImageIcon( getClass().getResource( "/images/FRANKS.png" ) );
  }

  private void scaleTurnButtonIcon()
  {
    if ( currentIcon == null )
      currentIcon = getButtonIcon( controller.getCurrentPlayerCivilization() );

    Image orig = currentIcon.getImage();

    // scale the image to fit within the button bounds
    int butW = Math.max( endTurnButton.getWidth(), 1 );
    int butH = Math.max( endTurnButton.getHeight(), 1 );
    Image scaledImage = orig.getScaledInstance( butW, butH, Image.SCALE_FAST );

    // set the scaled image as the button's icon
    currentIcon = ( new ImageIcon( scaledImage ) );
    endTurnButton.setIcon( currentIcon );
  }

  /**
   * Refreshes the turn flag's icon and the status line naming whose turn it
   * is: humans by honorific, AI commanders by nickname and tier.
   */
  public void updateTurnButtonIcon()
  {
    currentIcon = getButtonIcon( controller.getCurrentPlayerCivilization() );
    scaleTurnButtonIcon();
    endTurnButton.setIcon( currentIcon );
    refreshStatusLine();
  }

  /**
   * Rebuilds the status line from the current player's civilization and the
   * commander steering it: AI tiers by their tavern nickname and difficulty,
   * humans by the titled name they signed the muster roll with.
   */
  private void refreshStatusLine()
  {
    Civilization civ = controller.getCurrentPlayerCivilization();
    PlayerType type = controller.getBoard().getPlayerType( civ );

    String commander = type.isAI()
                     ? type.getNickname() + "  [" + type.getLabel() + "]"
                     : controller.getCurrentPlayer().getTitledName();

    statusLabel.setText( civ + "  <>  " + commander );
  }

  @Override
  public Dimension getPreferredSize()
  {
    int paneW = this.getParent().getWidth() - BUTTON_W;
    int chatH = chatAvailable ? CHAT_H : 0;

    endTurnButton.setBounds( 0, 0, BUTTON_W, PANEL_H );
    statusLabel.setBounds( BUTTON_W, 0, paneW, STATUS_H );
    scrollPane.setBounds( BUTTON_W, STATUS_H, paneW,
                          PANEL_H - STATUS_H - chatH );
    chatField.setBounds( BUTTON_W, PANEL_H - chatH, paneW, chatH );

    return new Dimension( this.getParent().getWidth(), PANEL_H );
  }
}
