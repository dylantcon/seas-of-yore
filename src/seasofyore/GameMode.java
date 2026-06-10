/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package seasofyore;

/**
 * Enumeration of available game modes in Seas of Yore.
 * Each mode represents a distinct game-play configuration.
 * 
 * @author dylan
 */
public enum GameMode 
{
  CLASSIC( "Classic Game" ),
  SALVO( "Salvo Game" ),
  AI_EASY( "Easy AI Opponent" ),
  AI_MEDIUM( "Medium AI Opponent" ),
  AI_HARD( "Hard AI Opponent" ),
  MULTIPLAYER_LAN( "LAN Multiplayer" ),
  MULTIPLAYER_ONLINE( "Online Multiplayer" );
  
  private final String displayName;
  
  /**
   * Constructs a GameMode enumeration with the specified display name
   * 
   * @param displayName the display name of this GameMode
   */
  GameMode( String displayName )
  {
    this.displayName = displayName;
  }
  
  /**
   * Gets the display name of this GameMode
   * 
   * @return the display name of this GameMode
   */
  public String getDisplayName()
  {
    return displayName;
  }
  
  /**
   * Determines if this game mode is currently implemented.
   * 
   * @return true if the mode is available; false if not yet implemented
   */
  public boolean isImplemented()
  {
    return this != MULTIPLAYER_LAN && this != MULTIPLAYER_ONLINE;
  }
  
  /**
   * Determines if this game mode involves an AI opponent.
   * 
   * @return true if the mode includes an AI; false otherwise
   */
  public boolean isAIMode()
  {
    return this == AI_EASY || this == AI_MEDIUM || this == AI_HARD;
  }
}
