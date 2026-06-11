/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore.ui;

import javalabrelay.RelayClient;
import seasofyore.match.LANClientMatchHandler;
import seasofyore.match.LANMatchHandler;
import seasofyore.match.LANServerMatchHandler;
import seasofyore.match.MatchConnector;
import seasofyore.match.MatchHandler;
import seasofyore.match.OnlineClientMatchHandler;
import seasofyore.match.OnlineServerMatchHandler;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * The "Across the Water" screen: everything about establishing a
 * networked match, owned by the screen itself rather than by the
 * application shell. It offers the four crossings -- host or join, on
 * the local network (direct TCP) or through the javalab relay (room
 * codes; the path that also works from a CheerpJ browser build) -- runs
 * the blocking connection work off the EDT with a working Cancel, and
 * hands its listener a finished connection plus the right
 * {@link MatchHandler} for it. What happens next (building the game) is
 * the listener's business.
 *
 * @author dylan
 */
public final class MultiplayerPanel extends JPanel
{
  /**
   * Receives the screen's outcomes.
   */
  public interface Listener
  {
    /**
     * A connection stands and a handler wraps it: build the game.
     *
     * @param connection      the established, handshaken connection
     * @param handler         the locality handler for this match
     * @param localName       the local commander's name
     * @param stoneAnimations whether attacks ride the falling-stone
     *                        animation on this screen; purely local
     *                        presentation, so each side chooses its own
     *                        (a CheerpJ build may want it off for speed)
     */
    void onMatchReady( MatchConnector.Connection connection,
                       MatchHandler handler, String localName,
                       boolean stoneAnimations );

    /**
     * The player backed out to the title screen.
     */
    void onBack();
  }

  /**
   * The shared menu palette and spacing, as the other screens wear.
   */
  private static final Color INK = new Color( 18, 10, 28 );
  private static final Color GOLD = new Color( 232, 201, 124 );
  private static final Color PARCHMENT = new Color( 229, 213, 175 );
  private static final Color WOOD_EDGE = new Color( 43, 26, 12 );
  private static final Insets HEADER_P = new Insets( 20, 0, 20, 0 );
  private static final Insets STD_P = new Insets( 10, 20, 10, 20 );
  private static final Insets BACK_P = new Insets( 30, 20, 10, 20 );

  private final Listener listener;

  private final JTextField nameField;
  private final JTextField addressField;
  private final JTextField roomField;
  private final JButton classicButton;
  private final JButton salvoButton;
  private final JButton stoneButton;
  private final JLabel statusLabel;
  private final JButton cancelButton;
  private final List<JButton> actionButtons = new ArrayList<>();

  private boolean salvoSelected = false;
  private boolean stoneAnimationsEnabled = true;
  private volatile Runnable cancelAction;

  /**
   * Builds the screen.
   *
   * @param listener the consumer of this screen's outcomes
   */
  public MultiplayerPanel( Listener listener )
  {
    this.listener = listener;
    setLayout( new GridBagLayout() );
    setOpaque( false );

    JLabel headerLabel = makeChipLabel( "Across the Water",
        new Font( "Serif", Font.BOLD, 40 ), GOLD, INK );

    JPanel deck = new WoodPanel();
    deck.setLayout( new GridBagLayout() );
    deck.setBorder( BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder( WOOD_EDGE, 5 ),
        BorderFactory.createEmptyBorder( 16, 26, 16, 26 ) ) );

    nameField = makeNameField( "Arthur", 10 );

    classicButton = makeDeckToggle( "Classic", 120 );
    salvoButton = makeDeckToggle( "SALVO", 120 );
    classicButton.addActionListener( e -> { salvoSelected = false; refreshModeToggle(); } );
    salvoButton.addActionListener( e -> { salvoSelected = true; refreshModeToggle(); } );

    JPanel rulesRow = flowRow();
    rulesRow.add( makeDeckLabel( "Rules (when hosting):" ) );
    rulesRow.add( classicButton );
    rulesRow.add( salvoButton );

    // stone volleys: whether attacks ride the falling-stone animation.
    // Unlike the rules, this is local-only presentation: each commander
    // chooses for their own screen, so it applies hosting OR joining --
    // the lever a browser (CheerpJ) player pulls for a faster match.
    stoneButton = makeDeckToggle( "", 240 );
    stoneButton.addActionListener( e ->
    {
      stoneAnimationsEnabled = !stoneAnimationsEnabled;
      refreshStoneToggle();
    });

    // LAN: host listens, the other side dials an address
    JButton hostLanButton = makeDeckToggle( "Host on this Network", 220 );
    addressField = makeNameField( "", 9 );
    JButton joinLanButton = makeDeckToggle( "Join", 90 );

    JPanel lanRow = flowRow();
    lanRow.add( hostLanButton );
    lanRow.add( makeDeckLabel( "or address:" ) );
    lanRow.add( addressField );
    lanRow.add( joinLanButton );

    // Relay: host opens a room code, the other side types it in
    JButton hostOnlineButton = makeDeckToggle( "Host via Relay", 220 );
    roomField = makeNameField( "", 5 );
    JButton joinOnlineButton = makeDeckToggle( "Join", 90 );

    JPanel relayRow = flowRow();
    relayRow.add( hostOnlineButton );
    relayRow.add( makeDeckLabel( "or room code:" ) );
    relayRow.add( roomField );
    relayRow.add( joinOnlineButton );

    // status plaque + cancel for the waiting states
    statusLabel = new JLabel();
    statusLabel.setFont( new Font( "Serif", Font.ITALIC, 14 ) );
    statusLabel.setForeground( INK );
    statusLabel.setBackground( PARCHMENT );
    statusLabel.setOpaque( true );
    statusLabel.setBorder( BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder( WOOD_EDGE, 2 ),
        BorderFactory.createEmptyBorder( 8, 12, 8, 12 ) ) );

    cancelButton = makeDeckToggle( "Cancel", 120 );
    cancelButton.setVisible( false );
    cancelButton.addActionListener( e ->
    {
      Runnable cancel = cancelAction;
      if ( cancel != null )
        cancel.run();
    });

    hostLanButton.addActionListener( e -> hostLanMatch() );
    joinLanButton.addActionListener( e -> joinLanMatch() );
    hostOnlineButton.addActionListener( e -> hostOnlineMatch() );
    joinOnlineButton.addActionListener( e -> joinOnlineMatch() );

    actionButtons.add( hostLanButton );
    actionButtons.add( joinLanButton );
    actionButtons.add( hostOnlineButton );
    actionButtons.add( joinOnlineButton );
    for ( JButton button : actionButtons )
      styleToggle( button, true );

    gblAdd( deck, fieldRow( "Thy name, Commander:", nameField ), 0, STD_P );
    gblAdd( deck, rulesRow, 1, STD_P );
    gblAdd( deck, stoneButton, 2, STD_P );
    gblAdd( deck, lanRow, 3, STD_P );
    gblAdd( deck, relayRow, 4, STD_P );
    gblAdd( deck, statusLabel, 5, STD_P );
    gblAdd( deck, cancelButton, 6, STD_P );

    refreshModeToggle();
    refreshStoneToggle();
    setStatus( "Choose thy crossing: a direct line on this network, or a "
             + "room code through the javalab relay." );

    JButton backButton = makeMenuButton( "Back", INK, PARCHMENT );
    backButton.addActionListener( e -> listener.onBack() );

    gblAdd( this, headerLabel, 0, HEADER_P );
    gblAdd( this, deck, 1, STD_P );
    gblAdd( this, backButton, 2, BACK_P );
  }

  // ------------------------------------------------------------------
  // The four crossings
  // ------------------------------------------------------------------

  /**
   * Hosts a LAN match: listen on the well-known port and wait for an
   * opponent to dial in. The Cancel plank closes the listening socket.
   */
  private void hostLanMatch()
  {
    final String name = requireName();
    if ( name == null )
      return;

    final ServerSocket harbor;
    try
    {
      harbor = new ServerSocket( LANMatchHandler.LAN_PORT );
    }
    catch ( IOException ex )
    {
      setStatus( "Could not open the harbour: " + ex.getMessage() );
      return;
    }

    setStatus( "Awaiting an opponent on port " + LANMatchHandler.LAN_PORT
             + ". Thy addresses: " + localAddresses() );
    setBusy( true, () -> closeQuietly( harbor ) );

    final boolean salvo = salvoSelected;
    runOffEdt( () ->
    {
      try
      {
        MatchConnector.Connection conn =
            MatchConnector.hostLan( harbor, name, salvo );
        onEdt( () -> deliver( conn,
            new LANServerMatchHandler( conn.transport, name, conn.remoteName ),
            name ) );
      }
      catch ( IOException ex )
      {
        onEdt( () -> connectFailed( "The wait was abandoned or failed: "
                                  + ex.getMessage() ) );
      }
      finally
      {
        closeQuietly( harbor );
      }
    });
  }

  /**
   * Joins a LAN match at the address in the field.
   */
  private void joinLanMatch()
  {
    final String name = requireName();
    if ( name == null )
      return;

    final String address = addressField.getText().trim();
    if ( address.isEmpty() )
    {
      setStatus( "Type the host's address first." );
      return;
    }

    setStatus( "Dialing " + address + "..." );
    setBusy( true, null );

    runOffEdt( () ->
    {
      try
      {
        MatchConnector.Connection conn = MatchConnector.joinLan( address, name );
        onEdt( () -> deliver( conn,
            new LANClientMatchHandler( conn.transport, name, conn.remoteName ),
            name ) );
      }
      catch ( IOException ex )
      {
        onEdt( () -> connectFailed( "Could not reach the host: "
                                  + ex.getMessage() ) );
      }
    });
  }

  /**
   * Hosts a relay match: open a room and display its code for sharing.
   */
  private void hostOnlineMatch()
  {
    final String name = requireName();
    if ( name == null )
      return;

    final String room = RelayClient.randomRoomCode();
    setStatus( "Room code: <b>" + room + "</b> -- share it with thine "
             + "opponent. Awaiting their arrival through the relay..." );
    setBusy( true, null ); // armed once the relay connects

    final boolean salvo = salvoSelected;
    runOffEdt( () ->
    {
      try
      {
        MatchConnector.Connection conn = MatchConnector.hostOnline(
            room, name, salvo,
            canceller -> onEdt( () -> setBusy( true, canceller ) ) );
        onEdt( () -> deliver( conn,
            new OnlineServerMatchHandler( conn.transport, name, conn.remoteName ),
            name ) );
      }
      catch ( IOException ex )
      {
        onEdt( () -> connectFailed( "The relay crossing failed: "
                                  + ex.getMessage() ) );
      }
    });
  }

  /**
   * Joins a relay match by the room code in the field.
   */
  private void joinOnlineMatch()
  {
    final String name = requireName();
    if ( name == null )
      return;

    final String room = roomField.getText().trim().toUpperCase();
    if ( room.isEmpty() )
    {
      setStatus( "Type the host's room code first." );
      return;
    }

    setStatus( "Seeking room " + room + " through the relay..." );
    setBusy( true, null );

    runOffEdt( () ->
    {
      try
      {
        MatchConnector.Connection conn = MatchConnector.joinOnline( room, name );
        onEdt( () -> deliver( conn,
            new OnlineClientMatchHandler( conn.transport, name, conn.remoteName ),
            name ) );
      }
      catch ( IOException ex )
      {
        onEdt( () -> connectFailed( "The relay crossing failed: "
                                  + ex.getMessage() ) );
      }
    });
  }

  /**
   * Resets the screen and hands the finished connection to the listener.
   */
  private void deliver( MatchConnector.Connection connection,
                        MatchHandler handler, String name )
  {
    setBusy( false, null );
    setStatus( "Connected to " + connection.remoteName + "!" );
    listener.onMatchReady( connection, handler, name, stoneAnimationsEnabled );
  }

  /**
   * Resets the screen after a failed or abandoned connection attempt.
   */
  private void connectFailed( String message )
  {
    setBusy( false, null );
    setStatus( message );
  }

  // ------------------------------------------------------------------
  // Screen state
  // ------------------------------------------------------------------

  /**
   * Updates the status plaque (HTML-wrapped so long lines wrap).
   */
  private void setStatus( String text )
  {
    statusLabel.setText( "<html><div style='width:360px'>" + text + "</div></html>" );
  }

  /**
   * Enters or leaves the busy/waiting state: action planks lock, the
   * Cancel plank appears, and the given abort action arms it.
   */
  private void setBusy( boolean busy, Runnable cancel )
  {
    for ( JButton button : actionButtons )
      button.setEnabled( !busy );
    cancelAction = cancel;
    cancelButton.setVisible( busy && cancel != null );
    revalidate();
    repaint();
  }

  /**
   * Reads and validates the name field.
   *
   * @return the trimmed name, or null (after complaining) if blank
   */
  private String requireName()
  {
    String text = nameField.getText() == null ? "" : nameField.getText().trim();
    if ( !text.isEmpty() )
      return text;

    JOptionPane.showMessageDialog( this,
        "Every commander must sign the muster roll -- a name is required.",
        "Unsigned muster roll", JOptionPane.WARNING_MESSAGE );
    return null;
  }

  /**
   * This machine's plausible IPv4 addresses, for the LAN host to read to
   * their opponent.
   */
  private static String localAddresses()
  {
    StringBuilder list = new StringBuilder();
    try
    {
      Enumeration<NetworkInterface> interfaces =
          NetworkInterface.getNetworkInterfaces();
      while ( interfaces.hasMoreElements() )
      {
        NetworkInterface nic = interfaces.nextElement();
        if ( !nic.isUp() || nic.isLoopback() )
          continue;

        Enumeration<InetAddress> addresses = nic.getInetAddresses();
        while ( addresses.hasMoreElements() )
        {
          InetAddress address = addresses.nextElement();
          if ( address instanceof Inet4Address )
          {
            if ( list.length() > 0 )
              list.append( ", " );
            list.append( address.getHostAddress() );
          }
        }
      }
    }
    catch ( SocketException ex )
    {
      // fall through to the shrug
    }
    return ( list.length() > 0 ) ? list.toString() : "(unknown)";
  }

  /**
   * Runs blocking connection work off the EDT.
   */
  private static void runOffEdt( Runnable work )
  {
    Thread worker = new Thread( work, "match-connector" );
    worker.setDaemon( true );
    worker.start();
  }

  /**
   * Hops back onto the EDT.
   */
  private static void onEdt( Runnable work )
  {
    SwingUtilities.invokeLater( work );
  }

  /**
   * Closes a listening socket without ceremony.
   */
  private static void closeQuietly( ServerSocket socket )
  {
    try
    {
      socket.close();
    }
    catch ( IOException ex )
    {
      // best effort
    }
  }

  // ------------------------------------------------------------------
  // Deck styling (the menus' shared look)
  // ------------------------------------------------------------------

  private void refreshModeToggle()
  {
    styleToggle( classicButton, !salvoSelected );
    styleToggle( salvoButton, salvoSelected );
  }

  private void refreshStoneToggle()
  {
    stoneButton.setText(
        "Stone Volleys: " + ( stoneAnimationsEnabled ? "SHOWN" : "INSTANT" ) );
    styleToggle( stoneButton, stoneAnimationsEnabled );
  }

  private void styleToggle( JButton button, boolean selected )
  {
    button.setBackground( selected ? GOLD : WOOD_EDGE );
    button.setForeground( selected ? INK : PARCHMENT );
  }

  private JButton makeDeckToggle( String text, int width )
  {
    JButton button = new JButton( text );
    button.setPreferredSize( new java.awt.Dimension( width, 36 ) );
    button.setFont( new Font( "Serif", Font.BOLD, 16 ) );
    button.setFocusable( false );
    button.setBorder( BorderFactory.createLineBorder( WOOD_EDGE, 2 ) );
    return button;
  }

  private JLabel makeDeckLabel( String text )
  {
    JLabel label = new JLabel( text );
    label.setFont( new Font( "Serif", Font.BOLD, 18 ) );
    label.setForeground( PARCHMENT );
    return label;
  }

  private JTextField makeNameField( String defaultText, int columns )
  {
    JTextField field = new JTextField( defaultText, columns );
    field.setFont( new Font( "Serif", Font.BOLD, 16 ) );
    field.setForeground( INK );
    field.setBackground( PARCHMENT );
    field.setCaretColor( INK );
    field.setBorder( BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder( WOOD_EDGE, 2 ),
        BorderFactory.createEmptyBorder( 2, 8, 2, 8 ) ) );
    return field;
  }

  private JButton makeMenuButton( String text, Color fg, Color bg )
  {
    JButton button = new JButton( text );
    button.setPreferredSize( new java.awt.Dimension( 280, 60 ) );
    button.setFont( new Font( "Serif", Font.BOLD, 20 ) );
    button.setForeground( fg );
    button.setBackground( bg );
    button.setFocusable( false );
    return button;
  }

  private JLabel makeChipLabel( String text, Font font, Color fg, Color bg )
  {
    JLabel label = new JLabel( text );
    label.setFont( font );
    label.setForeground( fg );
    label.setBackground( bg );
    label.setOpaque( true );
    label.setBorder( BorderFactory.createEmptyBorder( 8, 24, 8, 24 ) );
    return label;
  }

  private JPanel flowRow()
  {
    JPanel row = new JPanel( new FlowLayout( FlowLayout.CENTER, 8, 0 ) );
    row.setOpaque( false );
    return row;
  }

  private JPanel fieldRow( String labelText, Component control )
  {
    JPanel row = flowRow();
    row.add( makeDeckLabel( labelText ) );
    row.add( control );
    return row;
  }

  private void gblAdd( JPanel panel, Component c, int gY, Insets insets )
  {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = gY;
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.insets = insets;
    panel.add( c, gbc );
  }
}
