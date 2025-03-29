/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Just a simple, admittedly crappy win screen overlay.
 * You can't win them all, I'm proud of other things.
 * 
 * @author dylan
 */
public class WinScreenPanel extends JPanel
{
  private final ImageIcon victory;
  private final static String PATH = "/images/VICTORY.png";
  
  public WinScreenPanel( String winner, ActionListener rtn, ActionListener quit ) 
  {
    setLayout( new BorderLayout() );
    setBackground( new Color( 0, 0, 0, 200 ) ); // transparent black overlay
    victory = new ImageIcon( getClass().getResource( PATH ) );
    
    // victory message
    JLabel victoryLabel = new JLabel( String.format( "VICTORY, %s! Thou hast "
                                      + "vanquished thy foe!", winner ) );
    victoryLabel.setFont( new Font( "Serif", Font.BOLD, 40 ) );
    victoryLabel.setForeground( Color.RED ); // red text to contrast
    victoryLabel.setHorizontalAlignment( SwingConstants.CENTER );
    add( victoryLabel, BorderLayout.CENTER );
    
    // button panel
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout( new FlowLayout() );
    buttonPanel.setOpaque( false );
    
    // play again button
    JButton playAgainButton = new JButton( "Return to menu" );
    playAgainButton.addActionListener( rtn );
    buttonPanel.add( playAgainButton );
    
    // quit button
    JButton quitButton = new JButton( "Quit" );
    quitButton.addActionListener( quit );
    buttonPanel.add( quitButton );
    
    add( buttonPanel, BorderLayout.SOUTH );
  }
  
  @Override
  public void paintComponent( Graphics g )
  {
    super.paintComponent( g );
    g.drawImage( victory.getImage(), 0, 0, getWidth(), getHeight(), this );
  }
}
