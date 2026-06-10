package seasofyore.core;

/**
 * A headless head-to-head harness for AI strategies. It plays full games of
 * the core rules -- alternating shots between two AI players, with the same
 * hit/sink bookkeeping the UI's AITurnPhase performs -- and reports win
 * rates, average shots-to-victory, and the match odds the per-game edge
 * implies for best-of-3 and best-of-5 series.
 *
 * <p>The first move alternates between the two sides from game to game, so
 * neither strategy banks the (real) first-mover advantage.</p>
 *
 * <p>Usage: {@code java seasofyore.core.StrategyArena [tierA] [tierB] [games]}
 * -- e.g. {@code java seasofyore.core.StrategyArena HARD EXTREME 400}.
 * Defaults: HARD vs EXTREME over 200 games.</p>
 *
 * @author dylan
 */
public final class StrategyArena
{
  /**
   * Not instantiable; this is a command-line tool.
   */
  private StrategyArena() {}

  /**
   * Entry point: parses the two tiers and the game count, runs the series,
   * and prints the report.
   *
   * @param args optional: tierA tierB games
   */
  public static void main( String[] args )
  {
    PlayerFactory.AIDifficulty tierA = parseTier( args, 0, PlayerFactory.AIDifficulty.HARD );
    PlayerFactory.AIDifficulty tierB = parseTier( args, 1, PlayerFactory.AIDifficulty.EXTREME );
    int games = ( args.length > 2 ) ? Integer.parseInt( args[2] ) : 200;

    run( tierA, tierB, games );
  }

  /**
   * Parses one difficulty argument, falling back to a default.
   *
   * @param args the command-line arguments
   * @param idx  the argument index to parse
   * @param def  the default if the argument is absent
   * @return the parsed difficulty
   */
  private static PlayerFactory.AIDifficulty parseTier( String[] args, int idx,
                                                       PlayerFactory.AIDifficulty def )
  {
    if ( args.length <= idx )
      return def;
    return PlayerFactory.AIDifficulty.valueOf( args[idx].toUpperCase() );
  }

  /**
   * Plays the full series and prints the report.
   *
   * @param tierA the first strategy tier
   * @param tierB the second strategy tier
   * @param games how many games to play
   */
  private static void run( PlayerFactory.AIDifficulty tierA,
                           PlayerFactory.AIDifficulty tierB, int games )
  {
    int winsA = 0;
    int winsB = 0;
    long winnerShots = 0;

    long startMs = System.currentTimeMillis();

    for ( int g = 0; g < games; g++ )
    {
      // fresh boards, players, and (stateful) strategies every game
      PlayerQuadrant quadA = new PlayerQuadrant();
      PlayerQuadrant quadB = new PlayerQuadrant();
      Player playerA = PlayerFactory.createAIPlayer( Civilization.BRITONS,
                                                     quadA, quadB, tierA );
      Player playerB = PlayerFactory.createAIPlayer( Civilization.FRANKS,
                                                     quadB, quadA, tierB );
      playerA.randomVesselPlacement();
      playerB.randomVesselPlacement();

      // alternate who fires first so neither tier banks the tempo edge
      Player attacker = ( g % 2 == 0 ) ? playerA : playerB;
      Player defender = ( attacker == playerA ) ? playerB : playerA;

      int turns = 0;
      while ( true )
      {
        if ( ++turns > 2 * PlayerQuadrant.GRID_SIZE * PlayerQuadrant.GRID_SIZE )
          throw new IllegalStateException( "game failed to terminate" );

        if ( takeTurn( attacker, defender ) )
          break; // defender's fleet is gone; attacker wins

        Player swap = attacker;
        attacker = defender;
        defender = swap;
      }

      if ( attacker == playerA )
        winsA++;
      else
        winsB++;
      winnerShots += ( turns + 1 ) / 2; // attacker fired on the odd turns
    }

    long elapsedMs = System.currentTimeMillis() - startMs;
    report( tierA, tierB, games, winsA, winsB, winnerShots, elapsedMs );
  }

  /**
   * Plays one shot, mirroring the bookkeeping AITurnPhase performs in the
   * UI: true-hit detection via getShipAt (independent of fired state),
   * board update, deck sync, strategy feedback, and sink notification.
   *
   * @param attacker the player firing
   * @param defender the player being fired upon
   * @return true if this shot eliminated the defender's fleet
   */
  private static boolean takeTurn( Player attacker, Player defender )
  {
    int[] shot = attacker.calculateNextAttack();
    if ( shot == null )
      return false; // no targetable cell; cannot happen before a loss

    int x = shot[0];
    int y = shot[1];

    boolean hit = ( defender.getShipAt( x, y ) != null );
    defender.getFriendlyQuad().fireAtCell( x, y );
    defender.syncDecksToQuadrantState();

    attacker.processAttackResult( x, y, hit );

    Ship struck = hit ? defender.getShipAt( x, y ) : null;
    if ( struck != null && struck.isSunk() )
      attacker.notifyEnemyShipSunk( struck.getShipType(), x, y );

    return defender.hasLost();
  }

  /**
   * Prints the series result and the implied best-of-3 / best-of-5 odds.
   *
   * @param tierA       the first tier
   * @param tierB       the second tier
   * @param games       games played
   * @param winsA       games won by tierA
   * @param winsB       games won by tierB
   * @param winnerShots total shots fired by winners, summed over all games
   * @param elapsedMs   wall-clock duration of the series
   */
  private static void report( PlayerFactory.AIDifficulty tierA,
                              PlayerFactory.AIDifficulty tierB, int games,
                              int winsA, int winsB, long winnerShots,
                              long elapsedMs )
  {
    double pB = (double) winsB / games;

    System.out.printf( "%s vs %s -- %d games (%.1fs)%n",
                       tierA, tierB, games, elapsedMs / 1000.0 );
    System.out.printf( "  %-8s wins: %4d  (%.1f%%)%n", tierA, winsA,
                       100.0 * winsA / games );
    System.out.printf( "  %-8s wins: %4d  (%.1f%%)%n", tierB, winsB,
                       100.0 * pB );
    System.out.printf( "  mean shots-to-victory: %.1f%n",
                       (double) winnerShots / games );
    System.out.printf( "  implied odds %s takes a Bo3: %.1f%%   Bo5: %.1f%%%n",
                       tierB, 100.0 * bestOf( pB, 2 ), 100.0 * bestOf( pB, 3 ) );
  }

  /**
   * The probability of winning a first-to-n series given a per-game win
   * probability, summed over every series length the win can arrive in.
   *
   * @param p the per-game win probability
   * @param n the games needed to take the series (2 for Bo3, 3 for Bo5)
   * @return the series win probability
   */
  private static double bestOf( double p, int n )
  {
    // win the n-th game after exactly k losses: C(n-1+k, k) p^n (1-p)^k
    double total = 0.0;
    for ( int k = 0; k < n; k++ )
      total += binomial( n - 1 + k, k ) * Math.pow( p, n ) * Math.pow( 1 - p, k );
    return total;
  }

  /**
   * Small exact binomial coefficient.
   *
   * @param n the pool size
   * @param k the choose count
   * @return n choose k
   */
  private static double binomial( int n, int k )
  {
    double result = 1.0;
    for ( int i = 1; i <= k; i++ )
      result = result * ( n - k + i ) / i;
    return result;
  }
}
