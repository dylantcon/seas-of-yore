
package seasofyore.core;

import java.util.List;

/**
 * Represents the player's quadrant in the Seas of Yore game. Manages the grid 
 * state, ship placement, and interactions within the quadrant.
 * 
 * @author dylan connolly
 */
public class PlayerQuadrant 
{
    /**
     * Constant representing a water cell in the quadrant.
     */
    public static final int WATER_CELL = 0;

    /**
     * Constant representing a ship cell in the quadrant.
     */
    public static final int SHIP_CELL = 1;

    /**
     * Constant representing a hit cell in the quadrant.
     */
    public static final int HIT_CELL = -1;

    /**
     * Constant representing a miss cell in the quadrant.
     */
    public static final int MISS_CELL = 2;

    /**
     * The size of the grid (10x10).
     */
    public static final int GRID_SIZE = 10;

    /**
     * Represents a null cell, which should not exist in the grid.
     */
    public static final int NULL_CELL = -2;

    /**
     * Index representing the x-coordinate in a cell.
     */
    public static final int X = 0;

    /**
     * Index representing the y-coordinate in a cell.
     */
    public static final int Y = 1;

    /**
     * The grid representing the quadrant.
     */
    private final int[][] quad;

    /**
     * Constructs a new PlayerQuadrant and initializes all cells as water cells.
     */
    public PlayerQuadrant() 
    {
        this.quad = new int[GRID_SIZE][GRID_SIZE];
        for (int j = 0; j < GRID_SIZE; j++)
        {
            for (int i = 0; i < GRID_SIZE; i++)
            {
                this.quad[j][i] = WATER_CELL;
            }
        }
    }

    /**
     * Validates whether a ship's heading is within bounds and does not overlap with existing ships.
     *
     * @param ship    the ship to validate
     * @param heading the heading of the ship
     * @return true if the heading is valid; false otherwise
     */
    public boolean validHeading(Ship ship, ShipHeading heading) 
    {
        List<int[]> occupied = heading.getOccupiedCells(ship.getShipLength());
        return isRangeInBounds(occupied) && isRangeEmpty(occupied);
    }

    /**
     * Places a ship in the quadrant if the heading is valid.
     *
     * @param ship    the ship to place
     * @param heading the heading of the ship
     * @return true if the ship was successfully placed; false otherwise
     */
    public boolean placeShip(Ship ship, ShipHeading heading) 
    {
        if (!validHeading(ship, heading))
        {
            return false;
        }
        List<int[]> occupied = heading.getOccupiedCells(ship.getShipLength());
        for (int[] cell : occupied)
        {
            setQuadrantAt(cell[X], cell[Y], SHIP_CELL);
        }
        return true;
    }

    /**
     * Checks if a range of cells is within the bounds of the grid.
     *
     * @param cells the list of cells to check
     * @return true if all cells are within bounds; false otherwise
     */
    private boolean isRangeInBounds(List<int[]> cells) 
    {
        for (int[] cell : cells)
        {
            if (!cellInBounds(cell[X], cell[Y]))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a range of cells is empty (contains only water cells).
     *
     * @param cells the list of cells to check
     * @return true if all cells are empty; false otherwise
     */
    private boolean isRangeEmpty(List<int[]> cells) 
    {
        for (int[] cell : cells)
        {
            if (getQuadrantAt(cell[X], cell[Y]) != WATER_CELL)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a specific cell is within the bounds of the grid.
     *
     * @param x the x-coordinate of the cell
     * @param y the y-coordinate of the cell
     * @return true if the cell is within bounds; false otherwise
     */
    public static boolean cellInBounds(int x, int y) 
    {
        return !(x >= GRID_SIZE || y >= GRID_SIZE || x < 0 || y < 0);
    }

    /**
     * Checks if a specific cell contains a ship.
     *
     * @param x the x-coordinate of the cell
     * @param y the y-coordinate of the cell
     * @return true if the cell contains a ship; false otherwise
     */
    public boolean cellIsShip(int x, int y) 
    {
        return (cellInBounds(x, y) && getQuadrantAt(x, y) == SHIP_CELL);
    }

    /**
     * Checks if a specific cell has been hit.
     *
     * @param x the x-coordinate of the cell
     * @param y the y-coordinate of the cell
     * @return true if the cell is hit; false otherwise
     */
    public boolean cellIsHit(int x, int y) 
    {
        return (cellInBounds(x, y) && getQuadrantAt(x, y) == HIT_CELL);
    }

    /**
     * Checks if a specific cell is marked as a miss.
     *
     * @param x the x-coordinate of the cell
     * @param y the y-coordinate of the cell
     * @return true if the cell is a miss; false otherwise
     */
    public boolean cellIsMiss(int x, int y) 
    {
        return (cellInBounds(x, y) && getQuadrantAt(x, y) == MISS_CELL);
    }

    /**
     * Checks if a specific cell has been fired at.
     *
     * @param x the x-coordinate of the cell
     * @param y the y-coordinate of the cell
     * @return true if the cell has been fired at; false otherwise
     */
    public boolean cellIsFired(int x, int y) 
    {
        return (cellIsHit(x, y) || cellIsMiss(x, y));
    }

    /**
     * Checks if a specific cell is target-able.
     *
     * @param x the x-coordinate of the cell
     * @param y the y-coordinate of the cell
     * @return true if the cell is target-able; false otherwise
     */
    public boolean cellIsTargetable(int x, int y) 
    {
        return (cellInBounds(x, y) && !cellIsFired(x, y));
    }

    /**
     * Gets the type of a specific cell.
     *
     * @param x the x-coordinate of the cell
     * @param y the y-coordinate of the cell
     * @return the cell type, or NULL_CELL if out of bounds
     */
    public int getCellType(int x, int y) 
    {
        if (cellInBounds(x, y))
        {
            return getQuadrantAt(x, y);
        }
        return NULL_CELL;
    }

    /**
     * Sets the type of a specific cell.
     *
     * @param x        the x-coordinate of the cell
     * @param y        the y-coordinate of the cell
     * @param cellType the cell type to set
     */
    public void setCellType(int x, int y, int cellType) 
    {
        setQuadrantAt(x, y, cellType);
    }

    /**
     * Fires at a specific cell, marking it as a hit or miss.
     *
     * @param x the x-coordinate of the cell
     * @param y the y-coordinate of the cell
     * @return true if the cell was successfully fired at; false otherwise
     */
    public boolean fireAtCell(int x, int y)
    {
      if ( cellIsTargetable( x, y ) )
      {
        int quadrantEntry = MISS_CELL;
        if ( cellIsShip( x, y ) )
          quadrantEntry = HIT_CELL;
        setQuadrantAt( x, y, quadrantEntry );
        return true;
      }
      return false;
    }

     /**
     * Getter for translation from x-y to row-column.
     *
     * @param x the x-coordinate of the cell
     * @param y the y-coordinate of the cell
     * @return true if the cell was successfully fired at; false otherwise
     */
    private int getQuadrantAt( int x, int y )
    {
      return this.quad[y][x];
    }

    /**
     * Setter for translation from x-y to row-column.
     *
     * @param x the x-coordinate of the cell
     * @param y the y-coordinate of the cell
     * @param newVal the new value being placed in cell
     */
    private void setQuadrantAt( int x, int y, int newVal )
    {
      this.quad[y][x] = newVal;
    }
}
