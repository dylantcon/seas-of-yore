package seasofyore.core;

import seasofyore.core.PlayerQuadrant;
import seasofyore.core.ShipType;


/**
 * Represents a ship in the Seas of Yore game. Each ship has a specific type
 * and a number of decks that track its health.
 * 
 * @author dylan
 * 
 */
public class Ship 
{
    /**
     * The health state of each deck of the ship.
     */
    private int[] decks;

    /**
     * The type of the ship.
     */
    private final ShipType type;

    /**
     * Constructs a new Ship of the specified type.
     *
     * @param s the type of the ship
     */
    public Ship(ShipType s) 
    {
        this.type = s;
        this.initShip();
    }

    /**
     * Gets the length of a ship based on its type.
     *
     * @param s the type of the ship
     * @return the length of the ship
     */
    public static int getAShipLength(ShipType s) 
    {
        int len = -1;
        switch (s) 
        {
            case CRAYER -> len = 2;
            case HOY -> len = 3;
            case GALLEY -> len = 3;
            case COG -> len = 4;
            case GALLEON -> len = 5;
        }
        return len;
    }

    /**
     * Gets the length of the ship (number of decks).
     *
     * @return the number of decks
     */
    public int getShipLength() 
    {
        return decks.length;
    }

    /**
     * An alias for getShipLength().
     *
     * @return the number of decks
     */
    public int getLength() 
    {
        return getShipLength();
    }

    /**
     * Gets the type of the ship.
     *
     * @return the type of the ship
     */
    public ShipType getShipType() 
    {
        return this.type;
    }

    /**
     * Sets the value of a specific deck.
     *
     * @param index the index of the deck
     * @param value the value to set for the deck
     */
    public void setDeck(int index, int value) 
    {
        if (index >= 0 && index < this.getLength())
        {
            decks[index] = value;
        }
    }

    /**
     * Initializes the ship with decks based on its type.
     */
    private void initShip() 
    {
        this.decks = new int[getAShipLength(type)];
        for (int i = 0; i < decks.length; i++)
        {
            decks[i] = 1;
        }
    }

    /**
     * Creates a new instance of a ship with the specified type.
     *
     * @param t the type of the ship
     * @return a new Ship instance
     */
    public static Ship getInstance(ShipType t) 
    {
        return new Ship(t);
    }

    /**
     * Destroys a specific deck of the ship.
     *
     * @param deck the index of the deck to destroy
     * @return true if the deck was successfully destroyed; false otherwise
     */
    public boolean destroyDeck(int deck) 
    {
        if (deck < 0 || deck >= decks.length)
        {
            return false;
        }

        if (decks[deck] == -1)
        {
            return false;
        }

        decks[deck] = -1;
        return true;
    }

    /**
     * Checks if the ship is completely sunk.
     *
     * @return true if the ship is sunk; false otherwise
     */
    public boolean isSunk() 
    {
        for (int deck : decks)
        {
            if (deck == PlayerQuadrant.SHIP_CELL)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the ship is damaged (any deck destroyed).
     *
     * @return true if the ship is damaged; false otherwise
     */
    public boolean isDamaged() 
    {
        int sum = 0;
        for (int n : decks)
        {
            sum += n;
        }
        return (sum <= 0);
    }
}