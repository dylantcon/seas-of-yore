/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package seasofyore;

import java.awt.Point;

/**
 * Lightweight event-forwarding class for use with CellPanel
 * and dragLayerPanel GameController 
 * @author dylan connolly
 */
public interface QuadrantListener 
{
  /**
   * triggered when a cell in the QuadrantPanel is clicked.
   *
   * @param x The x-coordinate of the clicked cell
   * @param y The y-coordinate of the clicked cell
   * @param quadrantPanel The QuadrantPanel where the click occurred
   */
  void onCellClicked( int x, int y, QuadrantPanel quadrantPanel );
  
  void onMovement( Point p, QuadrantPanel src );
}
