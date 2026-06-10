package seasofyore.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Base class for all learning AI targeting strategies. It fixes the overall
 * decision procedure -- finish a wounded ship if one exists, otherwise explore
 * open water -- and supplies the shared bookkeeping (sunk-ship tracking,
 * surviving fleet lengths) plus a toolbox of reusable scoring helpers. Each
 * difficulty subclass then defines <em>both</em> of the varying steps:
 * <ul>
 *   <li>{@link #selectTargetShot(PlayerQuadrant)} -- the <em>target mode</em>
 *       predictive strike used to finish a wounded ship;</li>
 *   <li>{@link #selectHuntTarget(PlayerQuadrant)} -- the <em>hunt mode</em>
 *       exploration of open water.</li>
 * </ul>
 *
 * <p>This is the Template Method pattern: {@link #calculateFiringCoordinates}
 * is fixed, while the two steps that distinguish the tiers are abstract. The
 * tiers are deliberately differentiated at <em>both</em> steps -- a stronger AI
 * both explores and finishes more cleverly -- so that the gap between them is
 * decisive rather than marginal.</p>
 *
 * <h2>The "no peeking" rule</h2>
 * The {@link PlayerQuadrant} passed to this strategy is the real enemy board, so
 * {@link PlayerQuadrant#cellIsShip} would reveal hidden ship positions. A fair
 * AI must never call it. Every method here reads only the publicly observable
 * fired-state of a cell -- {@link PlayerQuadrant#cellIsHit cellIsHit},
 * {@link PlayerQuadrant#cellIsMiss cellIsMiss} and
 * {@link PlayerQuadrant#cellIsTargetable cellIsTargetable} -- exactly the
 * information a human opponent would have.
 *
 * @author dylan
 */
public abstract class AbstractTargetingStrategy implements AIStrategy
{
  /**
   * The grid dimension, cached for convenience.
   */
  protected static final int SIZE = PlayerQuadrant.GRID_SIZE;

  /**
   * The four orthogonal neighbour offsets, as { dx, dy } pairs. Ships are
   * straight lines, so only orthogonal adjacency matters for targeting.
   */
  protected static final int[][] DIRS = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

  /**
   * Shared source of randomness, used for tie-breaking between equally good
   * candidate cells so the AI is not trivially predictable.
   */
  protected final Random random = new Random();

  /**
   * The lengths of the enemy ships that are still afloat. Initialised from the
   * standard fleet and shrunk as {@link #notifyShipSunk} reports kills. This is
   * what lets a strategy reason about the minimum possible remaining ship
   * length (e.g. once the length-2 ship is gone, no ship shorter than 3 exists).
   */
  protected final List<Integer> remainingLengths;

  /**
   * Cells confirmed to belong to a sunk ship. These are still {@code HIT_CELL}s
   * on the board, but the AI must stop hunting around them, so they are
   * excluded from the set of "active" (wounded-but-unsunk) hits.
   */
  protected final Set<Integer> sunkCells = new HashSet<>();

  /**
   * Constructs the strategy with a full enemy fleet's worth of expected ship
   * lengths.
   */
  protected AbstractTargetingStrategy()
  {
    remainingLengths = new ArrayList<>();
    for ( ShipType type : ShipType.getAscendingList() )
      remainingLengths.add( type.getLength() );
  }

  /**
   * Default AI ship placement is random. Subclasses may override for smarter
   * placement, but the difficulty tiers here are differentiated by their
   * targeting, not their placement.
   *
   * @param ship     the ship to place
   * @param quadrant the quadrant to place the ship in
   * @return a random ShipHeading
   */
  @Override
  public ShipHeading calculateShipPlacement( Ship ship, PlayerQuadrant quadrant )
  {
    return ShipHeading.getRandomInstance();
  }

  /**
   * The Template Method. Chooses the next cell to fire upon by first attempting
   * a predictive strike against a wounded ship (target mode); if no ship is
   * currently wounded, it falls back to the subclass-defined exploration
   * (hunt mode).
   *
   * @param enemyQuadrant the enemy quadrant to target
   * @return coordinates as [x, y], or null if no targetable cell remains
   */
  @Override
  public final int[] calculateFiringCoordinates( PlayerQuadrant enemyQuadrant )
  {
    // remember the board so that sink bookkeeping -- which arrives in a
    // separate notifyShipSunk call with no quadrant of its own -- can inspect
    // the same hit/miss state we just fired against.
    this.lastQuad = enemyQuadrant;

    // finish a wounded ship if one exists (target mode); otherwise explore
    int[] predictive = selectTargetShot( enemyQuadrant );
    if ( predictive != null )
      return predictive;

    return selectHuntTarget( enemyQuadrant );
  }

  /**
   * Hook for subclasses to record per-shot information if they wish. The base
   * class derives everything it needs directly from the board's fired-state, so
   * this default implementation does nothing.
   *
   * @param x     the x-coordinate of the attack
   * @param y     the y-coordinate of the attack
   * @param isHit true if the attack hit a ship; false otherwise
   */
  @Override
  public void processHitResult( int x, int y, boolean isHit )
  {
    // base class reads hit/miss state straight from the board; nothing to store
  }

  /**
   * Records a sunk ship: removes its length from the expected fleet and marks
   * its cells as resolved so the AI stops hunting around them.
   *
   * @param sunkType the type (and length) of the ship sunk
   * @param x        the x-coordinate of the killing shot
   * @param y        the y-coordinate of the killing shot
   */
  @Override
  public void notifyShipSunk( ShipType sunkType, int x, int y )
  {
    remainingLengths.remove( (Integer) sunkType.getLength() );
    markSunkCells( x, y, sunkType.getLength() );
  }

  /**
   * Chooses a predictive strike to finish off a wounded ship (target mode), or
   * returns null when no enemy ship is currently wounded (the signal to fall
   * back to hunting). Each tier supplies its own finishing skill, typically by
   * delegating to one of the shared helpers below.
   *
   * @param quad the enemy quadrant
   * @return the chosen finishing shot as [x, y], or null if nothing is wounded
   */
  protected abstract int[] selectTargetShot( PlayerQuadrant quad );

  /**
   * Chooses an exploration target when no enemy ship is currently wounded. One
   * of the two steps that distinguish the difficulty tiers.
   *
   * @param quad the enemy quadrant to explore
   * @return coordinates as [x, y], or null if no targetable cell remains
   */
  protected abstract int[] selectHuntTarget( PlayerQuadrant quad );

  /**
   * The length of the shortest enemy ship still afloat. As small ships are
   * sunk this value rises, which lets the smarter tiers space their
   * exploratory shots further apart -- there is no point checking for a
   * length-2 ship once both length-2 cells must be covered by a longer hull.
   *
   * @return the minimum remaining ship length, or 1 if the fleet is empty
   */
  protected int minRemainingLength()
  {
    int min = Integer.MAX_VALUE;
    for ( int len : remainingLengths )
      min = Math.min( min, len );
    return ( min == Integer.MAX_VALUE ) ? 1 : min;
  }

  /**
   * The mean length of the enemy ships still afloat. Used by the heatmap tier
   * to bias placement weighting toward the ship sizes that actually remain.
   *
   * @return the mean remaining ship length, or 0 if the fleet is empty
   */
  protected double meanRemainingLength()
  {
    if ( remainingLengths.isEmpty() )
      return 0.0;
    int sum = 0;
    for ( int len : remainingLengths )
      sum += len;
    return (double) sum / remainingLengths.size();
  }

  // ----------------------------------------------------------------------
  // Shared target-mode (predictive strike) helpers -- tiers pick one
  // ----------------------------------------------------------------------

  /**
   * The naive finish (used by the Easy tier): fire at a random targetable cell
   * orthogonally adjacent to any wounded cell. It pokes blindly around hits
   * with no sense of a ship's axis, so it wastes shots a smarter tier would
   * save by driving straight down a discovered line.
   *
   * @param quad the enemy quadrant
   * @return a random adjacent targetable cell, or null if nothing is wounded
   */
  protected int[] randomAdjacentTarget( PlayerQuadrant quad )
  {
    List<int[]> activeHits = collectActiveHits( quad );
    if ( activeHits.isEmpty() )
      return null;

    List<int[]> neighbours = new ArrayList<>();
    Set<Integer> seen = new HashSet<>();
    for ( int[] hit : activeHits )
    {
      for ( int[] dir : DIRS )
      {
        int nx = hit[0] + dir[0];
        int ny = hit[1] + dir[1];
        if ( quad.cellIsTargetable( nx, ny ) && seen.add( key( nx, ny ) ) )
          neighbours.add( new int[] { nx, ny } );
      }
    }

    if ( neighbours.isEmpty() )
      return null;
    return neighbours.get( random.nextInt( neighbours.size() ) );
  }

  /**
   * The line-following finish (used by the Medium tier): like the naive finish,
   * but it recognises when two or more wounded cells line up and drives the
   * shot along that axis to finish the ship efficiently.
   *
   * <p>Scoring favours continuing an established line: if a hit has another
   * active hit directly behind it in some direction, the cell ahead in that
   * direction is far more likely to be the same ship than a fresh perpendicular
   * guess, so it is weighted heavily.</p>
   *
   * @param quad the enemy quadrant
   * @return the best predictive target as [x, y], or null if nothing is wounded
   */
  protected int[] lineFollowingTarget( PlayerQuadrant quad )
  {
    List<int[]> activeHits = collectActiveHits( quad );
    if ( activeHits.isEmpty() )
      return null;

    int[] best = null;
    int bestScore = Integer.MIN_VALUE;

    for ( int[] hit : activeHits )
    {
      for ( int[] dir : DIRS )
      {
        int nx = hit[0] + dir[0];
        int ny = hit[1] + dir[1];

        if ( !quad.cellIsTargetable( nx, ny ) )
          continue;

        // base score for any neighbour of a wounded cell
        int score = 1;

        // if the cell behind this hit is also an active hit, we have two
        // collinear hits and are extending a known line -- much stronger.
        int bx = hit[0] - dir[0];
        int by = hit[1] - dir[1];
        if ( isActiveHit( quad, bx, by ) )
          score += 10;

        // random jitter breaks ties without changing ranking tiers
        score = score * 2 + random.nextInt( 2 );

        if ( score > bestScore )
        {
          bestScore = score;
          best = new int[] { nx, ny };
        }
      }
    }

    return best;
  }

  /**
   * Collects every cell that is a hit but does not belong to an already-sunk
   * ship -- i.e. the decks of ships that are wounded but still afloat.
   *
   * @param quad the enemy quadrant
   * @return a list of active hit cells as [x, y]
   */
  protected List<int[]> collectActiveHits( PlayerQuadrant quad )
  {
    List<int[]> active = new ArrayList<>();
    for ( int x = 0; x < SIZE; x++ )
      for ( int y = 0; y < SIZE; y++ )
        if ( isActiveHit( quad, x, y ) )
          active.add( new int[] { x, y } );
    return active;
  }

  /**
   * Determines whether a cell is a hit belonging to a ship that is still
   * afloat (wounded but not sunk).
   *
   * @param quad the enemy quadrant
   * @param x    the x-coordinate
   * @param y    the y-coordinate
   * @return true if the cell is an active (unsunk) hit
   */
  protected boolean isActiveHit( PlayerQuadrant quad, int x, int y )
  {
    return quad.cellIsHit( x, y ) && !sunkCells.contains( key( x, y ) );
  }

  /**
   * Marks the cells of a just-sunk ship as resolved. Starting from the killing
   * shot, it walks to the far end of the contiguous run of hits along whichever
   * axis is long enough to contain the ship, then claims {@code length} cells
   * forward. This is robust for ships placed with at least one cell of spacing,
   * which is the overwhelmingly common case.
   *
   * @param x      the x-coordinate of the killing shot
   * @param y      the y-coordinate of the killing shot
   * @param length the length of the sunk ship
   */
  protected void markSunkCells( int x, int y, int length )
  {
    // pick the axis (horizontal or vertical) whose contiguous hit run is long
    // enough to hold this ship
    int[] axis = ( hitRunLength( x, y, 1, 0 ) >= length )
               ? new int[] { 1, 0 }
               : new int[] { 0, 1 };

    // walk backwards to the start of the run
    int sx = x;
    int sy = y;
    while ( isHitCell( sx - axis[0], sy - axis[1] ) )
    {
      sx -= axis[0];
      sy -= axis[1];
    }

    // claim `length` cells forward from the run start
    for ( int i = 0; i < length; i++ )
      sunkCells.add( key( sx + i * axis[0], sy + i * axis[1] ) );
  }

  /**
   * Counts the length of the contiguous run of hit cells through (x, y) along a
   * given axis, including the cell itself.
   *
   * @param x  the x-coordinate of the seed cell
   * @param y  the y-coordinate of the seed cell
   * @param dx the x-component of the axis direction
   * @param dy the y-component of the axis direction
   * @return the number of contiguous hit cells along the axis
   */
  protected int hitRunLength( int x, int y, int dx, int dy )
  {
    int count = 1;
    for ( int s = -1; s <= 1; s += 2 )
    {
      int cx = x + s * dx;
      int cy = y + s * dy;
      while ( isHitCell( cx, cy ) )
      {
        count++;
        cx += s * dx;
        cy += s * dy;
      }
    }
    return count;
  }

  /**
   * Whether a cell is in bounds and currently a hit (sunk or not), according to
   * the most recently fired-against board. Used by the sink-run walkers, which
   * run inside {@link #notifyShipSunk} and so rely on {@link #lastQuad}.
   *
   * @param x the x-coordinate
   * @param y the y-coordinate
   * @return true if (x, y) is a hit cell
   */
  private boolean isHitCell( int x, int y )
  {
    return lastQuad != null && lastQuad.cellIsHit( x, y );
  }

  /**
   * The most recently targeted quadrant, captured during firing so that sink
   * bookkeeping (triggered in a separate call) can inspect the board.
   */
  private PlayerQuadrant lastQuad;

  /**
   * Encodes a cell coordinate into a single integer key for set membership.
   *
   * @param x the x-coordinate
   * @param y the y-coordinate
   * @return a unique integer key for the cell
   */
  protected static int key( int x, int y )
  {
    return y * SIZE + x;
  }
}
