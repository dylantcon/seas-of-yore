package seasofyore.core;

import java.util.ArrayList;
import java.util.List;

/**
 * The Medium AI -- the "skilled human" tier. It reasons with heuristics a
 * thoughtful person actually uses rather than the formal probability maps of
 * {@link HardStrategy}, and it both hunts and finishes more capably than the
 * crude {@link EasyStrategy}.
 *
 * <h2>Hunt: a small state machine of human heuristics</h2>
 * Layered on the base class's HUNT/TARGET machine, Medium's HUNT state has two
 * sub-states:
 * <ul>
 *   <li><b>SWEEP</b> -- the normal case. Fire on the adaptive parity lattice
 *       {@code (x + y) % L == 0}, where {@code L} is the shortest surviving
 *       ship, and among those cells prefer the ones sitting in the most open
 *       water.</li>
 *   <li><b>CONSTRAINED</b> -- entered automatically late in the game once the
 *       parity lattice is used up. Parity is dropped and any remaining open
 *       cell is fair game, still ranked by openness.</li>
 * </ul>
 *
 * <h2>The heuristics being combined</h2>
 * <ol>
 *   <li><b>Adaptive parity.</b> A ship of length {@code L} must cross the
 *       diagonal lattice {@code (x + y) % L == 0}, so firing only there is still
 *       complete but cheaper. As small ships sink, {@code L} grows and the shots
 *       spread out, no longer wastefully checking for hulls too short to
 *       exist.</li>
 *   <li><b>Density repellant.</b> Open water is preferred; clusters of cells
 *       already fired upon are mildly repellent, which also naturally steers the
 *       AI away from cramped edges and corners toward roomy stretches where a
 *       big ship is likelier to be hiding.</li>
 *   <li><b>Feasibility pruning.</b> A cell that the smallest surviving ship
 *       could not possibly occupy -- a gap too small for any remaining hull --
 *       is never wasted on. This is a yes/no judgement a human makes at a
 *       glance, and it sharpens the late game considerably.</li>
 *   <li><b>Line-following finish.</b> Inherited target behaviour that, once two
 *       hits line up, drives straight down the ship's axis instead of poking
 *       blindly the way Easy does.</li>
 * </ol>
 *
 * Crucially, Medium never <em>counts</em> ship placements or builds a
 * probability field the way {@link HardStrategy} does -- it only asks the
 * yes/no question "could a ship fit here at all?" and otherwise judges cells by
 * parity and local openness. That keeps it clearly a notch below Hard while
 * decisively above Easy.
 *
 * @author dylan
 */
public class MediumStrategy extends AbstractTargetingStrategy
{
  /**
   * The sub-states of Medium's exploration (hunt) behaviour.
   */
  private enum HuntState
  {
    /** Disciplined parity sweep, ranked by openness. */
    SWEEP,
    /** Late-game fallback once the parity lattice is exhausted. */
    CONSTRAINED
  }

  /**
   * The radius (Chebyshev distance) of the neighbourhood used to gauge how open
   * a candidate cell is.
   */
  private static final int OPENNESS_RADIUS = 2;

  /**
   * Finishes a wounded ship by following its axis: once two hits line up, it
   * drives straight down the line rather than poking blindly.
   *
   * @param quad the enemy quadrant
   * @return the best line-following shot as [x, y], or null if nothing wounded
   */
  @Override
  protected int[] selectTargetShot( PlayerQuadrant quad )
  {
    return lineFollowingTarget( quad );
  }

  /**
   * Explores using the hunt state machine: a parity sweep ranked by openness,
   * degrading to an openness-ranked sweep of any open cell once the parity
   * lattice is exhausted.
   *
   * @param quad the enemy quadrant to explore
   * @return coordinates as [x, y], or null if no targetable cell remains
   */
  @Override
  protected int[] selectHuntTarget( PlayerQuadrant quad )
  {
    int minLen = minRemainingLength();
    int stride = Math.max( 2, minLen );

    // SWEEP candidates: parity-lattice cells the smallest ship could still fit
    List<int[]> lattice = new ArrayList<>();
    // CONSTRAINED candidates: any cell the smallest ship could still fit
    List<int[]> feasible = new ArrayList<>();
    // last resort if nothing is feasible (degenerate end of game)
    List<int[]> anyCell = new ArrayList<>();

    for ( int x = 0; x < SIZE; x++ )
    {
      for ( int y = 0; y < SIZE; y++ )
      {
        if ( !quad.cellIsTargetable( x, y ) )
          continue;

        anyCell.add( new int[] { x, y } );
        if ( canFit( quad, x, y, minLen ) )
        {
          feasible.add( new int[] { x, y } );
          if ( ( x + y ) % stride == 0 )
            lattice.add( new int[] { x, y } );
        }
      }
    }

    HuntState state = !lattice.isEmpty() ? HuntState.SWEEP : HuntState.CONSTRAINED;
    List<int[]> pool = ( state == HuntState.SWEEP ) ? lattice
                     : !feasible.isEmpty() ? feasible
                     : anyCell;
    if ( pool.isEmpty() )
      return null;

    return mostOpen( quad, pool );
  }

  /**
   * The yes/no feasibility glance: could a ship of the given length still sit
   * over (x, y) in any orientation, lying entirely on open water? Unlike
   * {@link HardStrategy}, this counts nothing -- it stops at the first fit it
   * finds and never tallies how many ways a ship could go.
   *
   * @param quad   the enemy quadrant
   * @param x      the x-coordinate of the cell
   * @param y      the y-coordinate of the cell
   * @param length the smallest surviving ship length
   * @return true if at least one placement covering the cell is possible
   */
  private boolean canFit( PlayerQuadrant quad, int x, int y, int length )
  {
    return canFitAlong( quad, x, y, 1, 0, length )
        || canFitAlong( quad, x, y, 0, 1, length );
  }

  /**
   * Whether a hull of the given length can cover (x, y) along one axis with
   * every cell on open (targetable) water.
   *
   * @param quad   the enemy quadrant
   * @param x      the x-coordinate of the covered cell
   * @param y      the y-coordinate of the covered cell
   * @param dx     the x-component of the axis
   * @param dy     the y-component of the axis
   * @param length the hull length
   * @return true if some placement along this axis covers (x, y) legally
   */
  private boolean canFitAlong( PlayerQuadrant quad, int x, int y,
                               int dx, int dy, int length )
  {
    // try each placement whose run covers (x, y)
    for ( int offset = 0; offset < length; offset++ )
    {
      int sx = x - offset * dx;
      int sy = y - offset * dy;
      boolean ok = true;
      for ( int i = 0; i < length && ok; i++ )
        ok = quad.cellIsTargetable( sx + i * dx, sy + i * dy );
      if ( ok )
        return true;
    }
    return false;
  }

  /**
   * Selects the most open cell in a pool, breaking ties randomly so the AI
   * stays unpredictable without sacrificing the heuristic.
   *
   * @param quad the enemy quadrant
   * @param pool the candidate cells
   * @return the chosen cell as [x, y]
   */
  private int[] mostOpen( PlayerQuadrant quad, List<int[]> pool )
  {
    int bestScore = Integer.MIN_VALUE;
    List<int[]> tied = new ArrayList<>();
    for ( int[] cell : pool )
    {
      int score = opennessScore( quad, cell[0], cell[1] );
      if ( score > bestScore )
      {
        bestScore = score;
        tied.clear();
        tied.add( cell );
      }
      else if ( score == bestScore )
      {
        tied.add( cell );
      }
    }
    return tied.get( random.nextInt( tied.size() ) );
  }

  /**
   * Scores a cell by how much open water surrounds it: each still-targetable
   * neighbour within {@link #OPENNESS_RADIUS} adds a point, each already-fired
   * neighbour subtracts one. Higher means more open and more attractive.
   *
   * @param quad the enemy quadrant
   * @param x    the x-coordinate
   * @param y    the y-coordinate
   * @return the openness score
   */
  private int opennessScore( PlayerQuadrant quad, int x, int y )
  {
    int score = 0;
    for ( int dx = -OPENNESS_RADIUS; dx <= OPENNESS_RADIUS; dx++ )
    {
      for ( int dy = -OPENNESS_RADIUS; dy <= OPENNESS_RADIUS; dy++ )
      {
        if ( dx == 0 && dy == 0 )
          continue;

        int nx = x + dx;
        int ny = y + dy;
        if ( !PlayerQuadrant.cellInBounds( nx, ny ) )
          continue;

        if ( quad.cellIsTargetable( nx, ny ) )
          score++;
        else if ( quad.cellIsFired( nx, ny ) )
          score--;
      }
    }
    return score;
  }
}
