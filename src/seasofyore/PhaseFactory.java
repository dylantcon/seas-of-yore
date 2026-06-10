/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore;

import seasofyore.core.Player;

/**
 * Factory class responsible for creating and configuring specific game phase instances.
 * This reduces coupling between GameController and specific phase implementations.
 * 
 * @author dylan
 */
public class PhaseFactory 
{
  
  /**
   * Creates a ship placement phase
   * 
   * @return a new ShipPlacementPhase instance
   */
  public static GamePhase createShipPlacementPhase()
  {
    return new ShipPlacementPhase();
  }
  
  /**
   * Creates an appropriate battle phase based on game mode.
   * 
   * @param salvoMode
   * @return 
   */
  public static GamePhase createBattlePhase( boolean salvoMode )
  {
    return salvoMode ? new SalvoBattlePhase() : new BattlePhase();
  }
  
  /**
   * Creates an AI turn phase when an AI player is active.
   * 
   * @param salvoMode
   * @return 
   */
  public static GamePhase createAITurnPhase( boolean salvoMode )
  {
    return new AITurnPhase( salvoMode );
  }
  
  /**
   * Creates the appropriate phase based on game state.
   * 
   * @param g the GameController managing the game
   * @param s true if the game is in Salvo mode; false otherwise
   * @return the appropriate GamePhase for the current game state
   */
  public static GamePhase createPhaseFromGameState( GameController g, boolean s )
  {
    if ( g.getBoard().isPlacementFinal() )
      return createBattlePhase( s );
    else
      return createShipPlacementPhase();
  }
  
  /**
   * Creates the phase for the turn that is about to begin. Called after the
   * board has already switched turns, so the phase is chosen by who the NEW
   * current player is: the phase always belongs to the player acting in it.
   *
   * @param g the GameController managing the game
   * @param s true if the game is in Salvo mode; false otherwise
   * @return the phase in which the new current player acts
   */
  public static GamePhase createNextTurnPhase( GameController g, boolean s )
  {
    Player actor = g.getCurrentPlayer();

    if ( g.getBoard().isPlacementFinal() )
    {
      if ( actor.isRemote() )
        return new RemoteTurnPhase();
      if ( actor.isAutonomous() )
        return createAITurnPhase( s );
      return createBattlePhase( s );
    }
    else
      return createShipPlacementPhase();
  }
  
}
