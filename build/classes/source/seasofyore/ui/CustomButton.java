/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore.ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;

/**
 * Custom button class, configured for icons. Looks nicer than ordinary JButtons
 * 
 * @author dylan
 */
public class CustomButton extends JButton 
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