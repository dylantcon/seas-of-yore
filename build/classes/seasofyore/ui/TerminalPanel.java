/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore.ui;

/**
 * Handles IO. Not needed as public.
 * @author dylan
 * 
 * 
 */
import seasofyore.core.Civilization;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.*;
import javax.swing.text.DefaultCaret;
import seasofyore.GameController;

public class TerminalPanel extends JPanel 
{
  private final GameController controller;
  private final JTextArea terminalArea;
  private final JScrollPane scrollPane;
  private final JButton endTurnButton;
  
  private ImageIcon currentIcon;

  public TerminalPanel( ActionListener endTurnListener, GameController game )
  {
    // Set layout and background
    setLayout( null );
    setBackground( Color.BLACK );
    controller = game;

    // initialize terminal area
    terminalArea = new JTextArea();
    terminalArea.setEditable( false );
    terminalArea.setLineWrap( true );
    terminalArea.setWrapStyleWord( true );
    terminalArea.setFont( new Font( "Monospaced", Font.BOLD, 12 ) );
    terminalArea.setForeground( Color.WHITE );
    terminalArea.setBackground( Color.BLACK );

    // enable auto-scrolling
    DefaultCaret caret = ( DefaultCaret ) terminalArea.getCaret();
    caret.setUpdatePolicy( DefaultCaret.ALWAYS_UPDATE );

    // wrap the terminal area in a scroll pane
    scrollPane = new JScrollPane( terminalArea );
    scrollPane.setBorder( BorderFactory.createEmptyBorder() );


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
    add( scrollPane );
  }

  // add a message to the terminal log
  public void logMessage( String message ) 
  {
    terminalArea.append( "$ " + message + "\n" );
    SwingUtilities.invokeLater( () -> 
    {
      terminalArea.setCaretPosition( terminalArea.getDocument().getLength() );
    });
  }

  // enable or disable the end turn button
  public void setTurnButtonEnabled( boolean enabled ) 
  {
    endTurnButton.setEnabled( enabled );
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
  
  public void updateTurnButtonIcon()
  {
    currentIcon = getButtonIcon( controller.getCurrentPlayerCivilization() );
    scaleTurnButtonIcon();
    endTurnButton.setIcon( currentIcon );
  }
  
  @Override
  public Dimension getPreferredSize()
  {
    int maxHeight = 85;
    int buttonWidth = 135;
    int paneWidth = this.getParent().getWidth() - buttonWidth;
    
    endTurnButton.setBounds( 0, 0, buttonWidth, maxHeight );
    scrollPane.setBounds( buttonWidth, 0, paneWidth, maxHeight );
    return new Dimension( this.getParent().getWidth(), maxHeight );
  }
}
