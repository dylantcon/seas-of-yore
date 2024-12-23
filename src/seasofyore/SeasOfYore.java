/*=
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */

/**
 * The main game package, which contains all files associated with controlling 
 * the game state.
 */
package seasofyore;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;

/**
 * Main entry point of the program. Medieval Battleship, using Swing Components.
 * 
 * @author dylan
 */
public class SeasOfYore 
{ 
  private static final String PTH = "/images/TITLE.png";
  private final ImageIcon ART;
  
  private SeasOfYore()
  {
    ART = new ImageIcon( getClass().getResource( PTH ) );
  }
  
  public static void main( String[] args ) 
  { 
    // Initialize main application frame
    SwingUtilities.invokeLater( () -> 
    {
      JFrame frame = new JFrame( "Seas of Yore" );
      frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
      frame.setSize( 800, 800 );

      // cardlayout to manage screens
      CardLayout cardLayout = new CardLayout();
      JPanel mainPanel = new JPanel( cardLayout );

      SeasOfYore gme = new SeasOfYore();
      
      // title screen panel
      JPanel titlePanel = gme.createTitlePanel( cardLayout, mainPanel, gme );
      // GameController Panel, passing in Runnable to return to titlescreen
      GameController classicController = new GameController( false, () -> 
      {
        cardLayout.show( mainPanel, "TitleScreen" );
      });
      
      GameController salvoController = new GameController( true, () ->
      {
        cardLayout.show( mainPanel, "TitleScreen" );
      });

      // add panels to the mainPanel
      mainPanel.add( titlePanel, "TitleScreen" );
      mainPanel.add( classicController, "GameScreen" );
      mainPanel.add( salvoController, "SalvoGameScreen" );

      // show the title screen by default
      cardLayout.show( mainPanel, "TitleScreen" );

      // add mainPanel to the frame
      frame.add( mainPanel );
      frame.setVisible( true );        
    });
  }
  
  private JPanel createTitlePanel( CardLayout cL, JPanel mP, SeasOfYore SoY ) 
  {
    // panel for the title screen
    JPanel titlePanel = new JPanel() 
    {
      @Override
      protected void paintComponent( Graphics g ) 
      {
        super.paintComponent(g);
        if ( ART != null )
        {
          g.drawImage( SoY.ART.getImage(), 0, 0, getWidth(), getHeight(), this );
        }
      }
    };
    titlePanel.setLayout( new BorderLayout() );

    // title Label
    JLabel titleLabel = new JLabel( "Seas of Yore", SwingConstants.CENTER );
    titleLabel.setFont( new Font( "Serif", Font.BOLD + Font.ITALIC, 60 ) );
    titleLabel.setForeground( Color.MAGENTA );

    // buttons Panel
    JButton startButton = new JButton( "Start Classic Game" );
    startButton.addActionListener( e -> cL.show( mP, "GameScreen" ) );

    JButton salvoButton = new JButton( "Start SALVO Game" );
    salvoButton.addActionListener( e -> cL.show( mP, "SalvoGameScreen" ) );

    JButton quitButton = new JButton( "Quit" );
    quitButton.addActionListener( e -> System.exit(0) );

    JPanel buttonPanel = new JPanel( new FlowLayout() );
    buttonPanel.add( startButton );
    buttonPanel.add( salvoButton );
    buttonPanel.add( quitButton );
    buttonPanel.setOpaque( false );

    // Add components to titlePanel
    titlePanel.add( titleLabel, BorderLayout.CENTER );
    titlePanel.add( buttonPanel, BorderLayout.SOUTH );

    return titlePanel;
  }
}

//
//import java.awt.BorderLayout;
//import java.awt.CardLayout;
//import java.awt.Color;
//import java.awt.FlowLayout;
//import java.awt.Font;
//import java.awt.Graphics;
//import javax.swing.ImageIcon;
//import javax.swing.JButton;
//import javax.swing.JFrame;
//import javax.swing.JLabel;
//import javax.swing.JPanel;
//import javax.swing.SwingConstants;
//import javax.swing.SwingUtilities;
//
///**
// * Main entry point of the program. Medieval Battleship, using Swing Components.
// * 
// * @author dylan
// */
//public class SeasOfYore 
//{ 
//  private static final String PTH = "/images/TITLE.png";
//  private final ImageIcon ART;
//  
//  private JPanel titlePanel;
//  private JPanel gamePanel;
//  private JPanel seasOfYore;
//  
//  private int userView;
//  private static final int TITLE_SCREEN = 0;
//  private static final int CLASSIC_SCREEN = 1;
//  private static final int SALVO_SCREEN = 2;
//  
//  private final JFrame frame;
//  
//  private SeasOfYore()
//  {
//    seasOfYore = new JPanel();
//    frame = new JFrame( "Seas of Yore" );
//    
//    frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
//    frame.setSize( 800, 800 );
//    
//    ART = new ImageIcon( getClass().getResource( PTH ) );
//    
//  }
//  
//  private void configureFrame()
//  {
//    seasOfYore 
//    
//  }
//  
//  private void run()
//  {
//    frame.setVisible( true );
//  }
//  
//  public static void main( String[] args ) 
//  { 
//    // Initialize main application frame
//    SwingUtilities.invokeLater( () -> 
//    {
//      SeasOfYore SoY = new SeasOfYore();
//      SoY.run();
//    });
//  }
//}
//



