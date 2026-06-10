package seasofyore.core;

import java.util.ArrayList;
import java.util.List;

/**
 * The Hard AI. Its exploration is driven by a <em>probability-density
 * heatmap</em>: for every enemy ship still afloat, it slides that hull over
 * every position and orientation on the board, and for each placement that does
 * not collide with a past shot it adds weight to all the cells that placement
 * would cover. The cell that the most surviving ships could still occupy is, by
 * definition, the most likely to be a ship -- so that is where it fires.
 *
 * <h2>Why this is the strong play</h2>
 * The map adapts itself automatically as the game unfolds:
 * <ul>
 *   <li>Misses carve holes that forbid placements, draining the heat from their
 *       neighbourhood.</li>
 *   <li>Sunk ships are removed from {@link #remainingLengths}, so the heatmap
 *       stops looking for hulls that no longer exist. Once the length-2 ship is
 *       gone it only ever considers lengths 3 and up -- exactly the
 *       "predict three or greater" behaviour: the minimum hull length the map
 *       reasons about rises to track the smallest ship that could still be out
 *       there.</li>
 *   <li>Each placement is weighted by its length, biasing the map by the
 *       adjusted mean hull size of the surviving fleet, so larger -- and
 *       easier to stumble into -- ships pull the search toward themselves.</li>
 * </ul>
 *
 * Wounded ships are still finished off by the shared predictive-strike logic in
 * {@link AbstractTargetingStrategy}.
 *
 * @author dylan
 */
public class HardStrategy extends AbstractTargetingStrategy
{
  /**
   * Finishes a wounded ship probability-optimally. It builds a heatmap counting
   * only ship placements <em>consistent with the current wounds</em> -- every
   * surviving hull length, in both orientations, that covers at least one active
   * hit and otherwise lies on open water (passing through hits, but never
   * through misses or sunk cells). The targetable cell covered by the most such
   * placements is the likeliest next deck, so that is where it fires.
   *
   * <p>Because it reasons about ship lengths, it is sharper than naive
   * line-following: e.g. it knows a length-5 hull wounded at one end must extend
   * far enough to fit, and weights the far cells accordingly.</p>
   *
   * @param quad the enemy quadrant
   * @return the best finishing shot as [x, y], or null if nothing is wounded
   */
  @Override
  protected int[] selectTargetShot( PlayerQuadrant quad )
  {
    List<int[]> active = collectActiveHits( quad );
    if ( active.isEmpty() )
      return null;

    double[][] heat = buildTargetHeatmap( quad );
    int[] best = hottestTargetable( quad, heat );

    // if no length-consistent placement exists (rare, e.g. tangled adjacent
    // ships), fall back to line-following so the AI never stalls
    return ( best != null ) ? best : lineFollowingTarget( quad );
  }

  /**
   * Builds the target-mode heatmap: every surviving hull length and orientation
   * whose placement covers at least one active hit and otherwise lies on
   * passable cells (targetable water or existing hits) contributes its
   * length-weighted vote to each targetable cell it would cover.
   *
   * @param quad the enemy quadrant
   * @return a grid of accumulated weights indexed as [x][y]
   */
  private double[][] buildTargetHeatmap( PlayerQuadrant quad )
  {
    double[][] heat = new double[SIZE][SIZE];

    // Target mode is unweighted on purpose: each consistent placement counts
    // once, so a cell's heat is the true number of surviving-ship placements
    // that could explain the wounds through it -- a maximum-likelihood estimate
    // of where the next deck lies. (Length weighting, used when hunting, would
    // only distort that estimate here.)
    for ( int length : remainingLengths )
    {
      accumulateTarget( quad, heat, length, 1.0, true );   // horizontal
      accumulateTarget( quad, heat, length, 1.0, false );  // vertical
    }
    return heat;
  }

  /**
   * Adds the contribution of every hit-consistent placement of a hull of the
   * given length and orientation to the target heatmap.
   *
   * @param quad       the enemy quadrant
   * @param heat       the heatmap being accumulated into
   * @param length     the hull length being placed
   * @param weight     the weight each valid placement contributes per cell
   * @param horizontal true for horizontal placements, false for vertical
   */
  private void accumulateTarget( PlayerQuadrant quad, double[][] heat,
                                 int length, double weight, boolean horizontal )
  {
    int dx = horizontal ? 1 : 0;
    int dy = horizontal ? 0 : 1;
    int maxX = horizontal ? SIZE - length : SIZE - 1;
    int maxY = horizontal ? SIZE - 1 : SIZE - length;

    for ( int x = 0; x <= maxX; x++ )
    {
      for ( int y = 0; y <= maxY; y++ )
      {
        if ( !targetPlacementValid( quad, x, y, dx, dy, length ) )
          continue;

        // vote only on the still-targetable cells of this placement -- the
        // already-hit cells are not future shots
        for ( int i = 0; i < length; i++ )
        {
          int cx = x + i * dx;
          int cy = y + i * dy;
          if ( quad.cellIsTargetable( cx, cy ) )
            heat[cx][cy] += weight;
        }
      }
    }
  }

  /**
   * Whether a hull placed at (x, y) along (dx, dy) is consistent with the
   * current wounds: every covered cell is passable (targetable water or a hit),
   * and at least one covered cell is an active (unsunk) hit.
   *
   * @param quad   the enemy quadrant
   * @param x      the starting x-coordinate
   * @param y      the starting y-coordinate
   * @param dx     the x-step
   * @param dy     the y-step
   * @param length the hull length
   * @return true if the placement could explain a current wound
   */
  private boolean targetPlacementValid( PlayerQuadrant quad, int x, int y,
                                        int dx, int dy, int length )
  {
    boolean coversActiveHit = false;
    for ( int i = 0; i < length; i++ )
    {
      int cx = x + i * dx;
      int cy = y + i * dy;
      boolean targetable = quad.cellIsTargetable( cx, cy );
      boolean active = isActiveHit( quad, cx, cy );
      if ( !targetable && !active )
        return false;   // blocked by a miss, a sunk deck, or the edge
      if ( active )
        coversActiveHit = true;
    }
    return coversActiveHit;
  }

  /**
   * Returns the targetable cell with the highest (positive) heat, choosing
   * randomly among ties, or null if no targetable cell has any heat.
   *
   * @param quad the enemy quadrant
   * @param heat the heatmap to read
   * @return the hottest targetable cell as [x, y], or null if none has heat
   */
  private int[] hottestTargetable( PlayerQuadrant quad, double[][] heat )
  {
    double bestHeat = 0.0;
    List<int[]> hottest = new ArrayList<>();
    for ( int x = 0; x < SIZE; x++ )
    {
      for ( int y = 0; y < SIZE; y++ )
      {
        if ( !quad.cellIsTargetable( x, y ) )
          continue;

        double h = heat[x][y];
        if ( h > bestHeat + 1e-9 )
        {
          bestHeat = h;
          hottest.clear();
          hottest.add( new int[] { x, y } );
        }
        else if ( h > 1e-9 && Math.abs( h - bestHeat ) <= 1e-9 )
        {
          hottest.add( new int[] { x, y } );
        }
      }
    }
    return hottest.isEmpty() ? null : hottest.get( random.nextInt( hottest.size() ) );
  }

  /**
   * Explores by building a fresh probability-density heatmap of where the
   * surviving fleet could still fit and firing at the hottest targetable cell.
   *
   * @param quad the enemy quadrant to explore
   * @return coordinates as [x, y], or null if no targetable cell remains
   */
  @Override
  protected int[] selectHuntTarget( PlayerQuadrant quad )
  {
    double[][] heat = buildHeatmap( quad );

    double bestHeat = -1.0;
    List<int[]> hottest = new ArrayList<>();

    for ( int x = 0; x < SIZE; x++ )
    {
      for ( int y = 0; y < SIZE; y++ )
      {
        if ( !quad.cellIsTargetable( x, y ) )
          continue;

        double h = heat[x][y];
        if ( h > bestHeat + 1e-9 )
        {
          bestHeat = h;
          hottest.clear();
          hottest.add( new int[] { x, y } );
        }
        else if ( Math.abs( h - bestHeat ) <= 1e-9 )
        {
          hottest.add( new int[] { x, y } );
        }
      }
    }

    if ( hottest.isEmpty() )
      return null;

    // random choice among equally-hot cells keeps the AI from being predictable
    return hottest.get( random.nextInt( hottest.size() ) );
  }

  /**
   * Builds the probability-density heatmap. For every surviving ship length and
   * both orientations, every legal placement (one lying entirely on targetable
   * water) adds its length-weighted contribution to each cell it covers.
   *
   * <p>The strategy reads only the publicly observable fired-state of cells via
   * {@link PlayerQuadrant#cellIsTargetable}; it never inspects actual ship
   * positions.</p>
   *
   * @param quad the enemy quadrant
   * @return a grid of accumulated placement weights indexed as [x][y]
   */
  private double[][] buildHeatmap( PlayerQuadrant quad )
  {
    double[][] heat = new double[SIZE][SIZE];
    double mean = meanRemainingLength();

    // remainingLengths keeps duplicates (e.g. the two length-3 hulls), so a
    // length with more surviving ships naturally contributes more placements.
    for ( int length : remainingLengths )
    {
      // weight longer hulls more heavily, scaled by the surviving mean length
      double weight = ( mean > 0 ) ? ( length / mean ) : 1.0;

      accumulate( quad, heat, length, weight, true );   // horizontal
      accumulate( quad, heat, length, weight, false );  // vertical
    }
    return heat;
  }

  /**
   * Adds the contribution of every legal placement of a hull of the given
   * length and orientation to the heatmap.
   *
   * @param quad       the enemy quadrant
   * @param heat       the heatmap being accumulated into
   * @param length     the hull length being placed
   * @param weight     the weight each valid placement contributes per cell
   * @param horizontal true for horizontal placements, false for vertical
   */
  private void accumulate( PlayerQuadrant quad, double[][] heat,
                           int length, double weight, boolean horizontal )
  {
    int dx = horizontal ? 1 : 0;
    int dy = horizontal ? 0 : 1;

    int maxX = horizontal ? SIZE - length : SIZE - 1;
    int maxY = horizontal ? SIZE - 1 : SIZE - length;

    for ( int x = 0; x <= maxX; x++ )
    {
      for ( int y = 0; y <= maxY; y++ )
      {
        if ( placementFits( quad, x, y, dx, dy, length ) )
        {
          for ( int i = 0; i < length; i++ )
            heat[x + i * dx][y + i * dy] += weight;
        }
      }
    }
  }

  /**
   * Determines whether a hull of the given length can legally occupy the run of
   * cells starting at (x, y) -- that is, whether every covered cell is still
   * targetable water (in bounds and not yet fired upon).
   *
   * @param quad   the enemy quadrant
   * @param x      the starting x-coordinate
   * @param y      the starting y-coordinate
   * @param dx     the x-step between consecutive cells
   * @param dy     the y-step between consecutive cells
   * @param length the hull length
   * @return true if the placement fits entirely on targetable water
   */
  private boolean placementFits( PlayerQuadrant quad, int x, int y,
                                 int dx, int dy, int length )
  {
    for ( int i = 0; i < length; i++ )
    {
      if ( !quad.cellIsTargetable( x + i * dx, y + i * dy ) )
        return false;
    }
    return true;
  }
}
