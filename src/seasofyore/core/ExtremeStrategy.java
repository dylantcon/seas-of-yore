package seasofyore.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Extreme AI. It is built to beat {@link HardStrategy} by sharpening all
 * three things a battleship player does -- exploring, finishing, and hiding:
 *
 * <h2>1. Hunting: the true probability map</h2>
 * Like Hard, it slides every surviving hull over the board and votes on the
 * cells each legal placement covers -- but <em>unweighted</em>. Hard scales
 * each placement's vote by hull length, which distorts the estimate: a
 * length-5 placement already casts five votes, so weighting it again
 * over-values long-ship regions. An unweighted count is the true expected
 * occupancy of each cell under a uniform placement model, so Extreme's first
 * miss-free shots land where a ship is genuinely most likely to be.
 *
 * <h2>2. Finishing: joint Monte Carlo consistency</h2>
 * Hard's target mode counts single-ship placements that could explain at
 * least one wound. Extreme instead samples entire <em>fleets</em>: complete,
 * non-overlapping placements of every surviving ship that together explain
 * every active wound and avoid every miss and sunk deck. Counting cell
 * occupancy across thousands of such consistent worlds answers the question
 * Hard only approximates -- "given everything I know, which cell is most
 * likely a deck?" -- including cross-ship effects (two wound clusters cannot
 * be the same hull; ships cannot overlap) that per-ship counting ignores.
 * If sampling cannot find enough consistent worlds (deeply tangled wounds),
 * it falls back to line-following so it never stalls.
 *
 * <h2>3. Hiding: anti-heatmap placement</h2>
 * A heatmap hunter probes the cells the most placements pass through --
 * the broad center -- first, and reaches low-count edge water last. Extreme
 * exploits that: it scores every legal placement of its own fleet by the
 * static hunt-heat of the cells it would occupy and moors among the coldest
 * options (choosing randomly within the coldest tier so it stays
 * unpredictable). Against any placement-count hunter this systematically
 * buys extra turns before its ships are found.
 *
 * @author dylan
 */
public class ExtremeStrategy extends AbstractTargetingStrategy
{
  /**
   * Monte Carlo worlds attempted per finishing shot. Each sample is a full
   * fleet placement, so a few thousand keeps per-shot noise well below the
   * gaps between candidate cells while staying far under a frame's budget.
   */
  private static final int SAMPLES = 2400;

  /**
   * The minimum number of accepted (fully consistent) worlds required to
   * trust the Monte Carlo estimate. Below this the wounds are too tangled
   * for sampling and the strategy falls back to line-following.
   */
  private static final int MIN_ACCEPTED = 40;

  /**
   * How many of the coldest candidate placements each ship chooses among
   * when mooring. A pool this size keeps the fleet in genuinely cold water
   * while leaving enough variety that the layout is never twice the same.
   */
  private static final int COLD_POOL = 12;

  /**
   * The static hunt-heat of an empty board (placement counts for the full
   * fleet), shared by every instance: it is a property of the rules, not of
   * any one game, and defensive placement only needs relative temperatures.
   */
  private static double[][] staticHeat;

  /**
   * Moors the fleet in the water a placement-count hunter searches last.
   * Every legal heading is scored by the summed static hunt-heat of the
   * cells it would cover, and the ship picks randomly among the coldest few.
   * Only this player's own quadrant is consulted -- no peeking.
   *
   * @param ship     the ship to place
   * @param quadrant this player's own quadrant
   * @return the chosen heading, or a random one if nothing legal was found
   */
  @Override
  public ShipHeading calculateShipPlacement( Ship ship, PlayerQuadrant quadrant )
  {
    ensureStaticHeat();

    List<ShipHeading> candidates = new ArrayList<>();
    List<Double> heats = new ArrayList<>();

    for ( Direction d : Direction.values() )
    {
      for ( int x = 0; x < SIZE; x++ )
      {
        for ( int y = 0; y < SIZE; y++ )
        {
          ShipHeading heading = new ShipHeading( x, y, d );
          if ( !quadrant.validHeading( ship, heading ) )
            continue;

          double heat = 0.0;
          for ( int[] cell : heading.getOccupiedCells( ship.getShipLength() ) )
            heat += staticHeat[cell[0]][cell[1]];

          candidates.add( heading );
          heats.add( heat );
        }
      }
    }

    if ( candidates.isEmpty() )
      return ShipHeading.getRandomInstance(); // caller retries randomly anyway

    // selection sort just the coldest COLD_POOL candidates to the front --
    // the lists are at most ~360 long, so simplicity beats cleverness here
    int pool = Math.min( COLD_POOL, candidates.size() );
    for ( int i = 0; i < pool; i++ )
    {
      int coldest = i;
      for ( int j = i + 1; j < candidates.size(); j++ )
        if ( heats.get( j ) < heats.get( coldest ) )
          coldest = j;
      Collections.swap( candidates, i, coldest );
      Collections.swap( heats, i, coldest );
    }

    return candidates.get( random.nextInt( pool ) );
  }

  /**
   * Finishes wounded ships by joint Monte Carlo: samples whole surviving
   * fleets consistent with every observation and fires at the cell that was
   * a deck in the most sampled worlds.
   *
   * @param quad the enemy quadrant
   * @return the best finishing shot as [x, y], or null if nothing is wounded
   */
  @Override
  protected int[] selectTargetShot( PlayerQuadrant quad )
  {
    List<int[]> activeHits = collectActiveHits( quad );
    if ( activeHits.isEmpty() )
      return null;

    int[] shot = monteCarloShot( quad, activeHits );

    // tangled wounds can starve the sampler of consistent worlds; never stall
    return ( shot != null ) ? shot : lineFollowingTarget( quad );
  }

  /**
   * Explores with an exact, unweighted placement-count map: each legal
   * placement of each surviving hull votes once on every cell it covers,
   * yielding the true expected occupancy per cell. (Joint sampling adds
   * nothing here -- with no wounds to explain, per-ship counting and fleet
   * sampling agree almost everywhere -- so the exact map is both faster and
   * noise-free.)
   *
   * @param quad the enemy quadrant to explore
   * @return coordinates as [x, y], or null if no targetable cell remains
   */
  @Override
  protected int[] selectHuntTarget( PlayerQuadrant quad )
  {
    double[][] heat = new double[SIZE][SIZE];

    for ( int length : remainingLengths )
    {
      accumulateUnweighted( quad, heat, length, 1, 0 ); // horizontal
      accumulateUnweighted( quad, heat, length, 0, 1 ); // vertical
    }

    return hottestTargetable( quad, heat );
  }

  /**
   * Adds one vote per covered cell for every placement of a hull that lies
   * entirely on targetable water.
   *
   * @param quad the enemy quadrant
   * @param heat the map being accumulated into
   * @param length the hull length being slid
   * @param dx the x-step of the orientation
   * @param dy the y-step of the orientation
   */
  private void accumulateUnweighted( PlayerQuadrant quad, double[][] heat,
                                     int length, int dx, int dy )
  {
    int maxX = ( dx == 1 ) ? SIZE - length : SIZE - 1;
    int maxY = ( dy == 1 ) ? SIZE - length : SIZE - 1;

    for ( int x = 0; x <= maxX; x++ )
    {
      for ( int y = 0; y <= maxY; y++ )
      {
        boolean fits = true;
        for ( int i = 0; i < length && fits; i++ )
          fits = quad.cellIsTargetable( x + i * dx, y + i * dy );

        if ( !fits )
          continue;

        for ( int i = 0; i < length; i++ )
          heat[x + i * dx][y + i * dy] += 1.0;
      }
    }
  }

  /**
   * Runs the joint Monte Carlo estimate: across SAMPLES attempted worlds,
   * counts how often each still-targetable cell holds a deck of a surviving
   * ship, and returns the most frequent one.
   *
   * @param quad       the enemy quadrant
   * @param activeHits the wounded-but-unsunk cells every world must explain
   * @return the hottest cell as [x, y], or null if too few worlds were found
   */
  private int[] monteCarloShot( PlayerQuadrant quad, List<int[]> activeHits )
  {
    voteQuad = quad; // gates the per-world occupancy votes while sampling

    // cells no surviving ship may cross: misses and decks of sunk ships
    boolean[] blocked = new boolean[SIZE * SIZE];
    for ( int x = 0; x < SIZE; x++ )
      for ( int y = 0; y < SIZE; y++ )
        if ( quad.cellIsMiss( x, y )
          || ( quad.cellIsHit( x, y ) && sunkCells.contains( key( x, y ) ) ) )
          blocked[key( x, y )] = true;

    double[][] counts = new double[SIZE][SIZE];
    int accepted = 0;

    for ( int s = 0; s < SAMPLES; s++ )
      if ( sampleWorld( blocked, activeHits, counts ) )
        accepted++;

    if ( accepted < MIN_ACCEPTED )
      return null;

    return hottestTargetable( quad, counts );
  }

  /**
   * Attempts to build one consistent world: a complete, non-overlapping
   * placement of every surviving hull that covers every active hit and
   * avoids every blocked cell. On success, votes for each placed deck cell
   * that is still an open (unfired) shot.
   *
   * <p>Hit-covering ships are placed first, each chosen uniformly among the
   * placements of a random surviving hull through a random unexplained
   * wound; the rest of the fleet then fills open water. A world that paints
   * itself into a corner simply fails -- the next sample starts fresh.</p>
   *
   * @param blocked    cells no ship may cross
   * @param activeHits the wounds the world must explain
   * @param counts     the occupancy tally to vote into on success
   * @return true if a fully consistent world was placed and tallied
   */
  private boolean sampleWorld( boolean[] blocked, List<int[]> activeHits,
                               double[][] counts )
  {
    boolean[] occupied = new boolean[SIZE * SIZE];
    List<Integer> lengths = new ArrayList<>( remainingLengths );
    Collections.shuffle( lengths, random );

    Set<Integer> uncovered = new HashSet<>();
    for ( int[] hit : activeHits )
      uncovered.add( key( hit[0], hit[1] ) );

    List<int[]> placedRuns = new ArrayList<>(); // {x, y, dx, dy, length}

    // phase 1: explain every wound
    while ( !uncovered.isEmpty() )
    {
      if ( lengths.isEmpty() )
        return false; // wounds remain but the fleet is spent: inconsistent

      int target = randomElement( uncovered );
      int tx = target % SIZE;
      int ty = target / SIZE;

      // gather every placement of every remaining hull through this wound
      List<int[]> options = new ArrayList<>();
      for ( int li = 0; li < lengths.size(); li++ )
      {
        int length = lengths.get( li );
        collectRunsThrough( tx, ty, length, li, blocked, occupied, options );
      }

      if ( options.isEmpty() )
        return false; // nothing can explain this wound in this world

      int[] run = options.get( random.nextInt( options.size() ) );
      placeRun( run, occupied, placedRuns );
      lengths.remove( run[5] ); // run[5] is the index into lengths

      // this hull may have explained several wounds at once
      for ( int i = 0; i < run[4]; i++ )
        uncovered.remove( key( run[0] + i * run[2], run[1] + i * run[3] ) );
    }

    // phase 2: scatter the rest of the fleet over open water
    for ( int length : lengths )
    {
      List<int[]> options = new ArrayList<>();
      collectOpenRuns( length, blocked, occupied, options );
      if ( options.isEmpty() )
        return false; // no room left for this hull: inconsistent world

      placeRun( options.get( random.nextInt( options.size() ) ),
                occupied, placedRuns );
    }

    // success: every placed deck on an unfired cell is a candidate shot
    for ( int[] run : placedRuns )
    {
      for ( int i = 0; i < run[4]; i++ )
      {
        int cx = run[0] + i * run[2];
        int cy = run[1] + i * run[3];
        if ( !uncoveredVote( cx, cy ) )
          continue;
        counts[cx][cy] += 1.0;
      }
    }
    return true;
  }

  /**
   * Whether a placed deck cell should receive an occupancy vote: only cells
   * we could actually still fire at matter, so known hits are skipped.
   *
   * @param x the x-coordinate
   * @param y the y-coordinate
   * @return true if the cell is a legitimate future shot
   */
  private boolean uncoveredVote( int x, int y )
  {
    // lastQuadFor votes: a deck on an already-hit cell is old news
    return voteQuad == null || voteQuad.cellIsTargetable( x, y );
  }

  /**
   * Collects every run of the given length that passes through (tx, ty)
   * without crossing a blocked or occupied cell, in both orientations.
   * Each result is encoded as {x, y, dx, dy, length, lengthIndex}.
   *
   * @param tx          the x-coordinate the run must cover
   * @param ty          the y-coordinate the run must cover
   * @param length      the hull length
   * @param lengthIndex the index of this hull in the surviving-lengths list
   * @param blocked     cells no ship may cross
   * @param occupied    cells already taken by this world's placed hulls
   * @param out         the list collecting valid runs
   */
  private void collectRunsThrough( int tx, int ty, int length, int lengthIndex,
                                   boolean[] blocked, boolean[] occupied,
                                   List<int[]> out )
  {
    for ( int orient = 0; orient < 2; orient++ )
    {
      int dx = ( orient == 0 ) ? 1 : 0;
      int dy = 1 - dx;

      // slide the run so each of its cells in turn sits on the target
      for ( int offset = 0; offset < length; offset++ )
      {
        int sx = tx - offset * dx;
        int sy = ty - offset * dy;
        if ( runIsClear( sx, sy, dx, dy, length, blocked, occupied ) )
          out.add( new int[] { sx, sy, dx, dy, length, lengthIndex } );
      }
    }
  }

  /**
   * Collects every run of the given length lying entirely on clear water.
   *
   * @param length   the hull length
   * @param blocked  cells no ship may cross
   * @param occupied cells already taken by this world's placed hulls
   * @param out      the list collecting valid runs
   */
  private void collectOpenRuns( int length, boolean[] blocked,
                                boolean[] occupied, List<int[]> out )
  {
    for ( int orient = 0; orient < 2; orient++ )
    {
      int dx = ( orient == 0 ) ? 1 : 0;
      int dy = 1 - dx;
      int maxX = ( dx == 1 ) ? SIZE - length : SIZE - 1;
      int maxY = ( dy == 1 ) ? SIZE - length : SIZE - 1;

      for ( int x = 0; x <= maxX; x++ )
        for ( int y = 0; y <= maxY; y++ )
          if ( runIsClear( x, y, dx, dy, length, blocked, occupied ) )
            out.add( new int[] { x, y, dx, dy, length, -1 } );
    }
  }

  /**
   * Whether a run stays in bounds and crosses no blocked or occupied cell.
   *
   * @param x        the starting x-coordinate
   * @param y        the starting y-coordinate
   * @param dx       the x-step
   * @param dy       the y-step
   * @param length   the hull length
   * @param blocked  cells no ship may cross
   * @param occupied cells already taken in this world
   * @return true if every covered cell is clear
   */
  private boolean runIsClear( int x, int y, int dx, int dy, int length,
                              boolean[] blocked, boolean[] occupied )
  {
    for ( int i = 0; i < length; i++ )
    {
      int cx = x + i * dx;
      int cy = y + i * dy;
      if ( !PlayerQuadrant.cellInBounds( cx, cy ) )
        return false;
      int k = key( cx, cy );
      if ( blocked[k] || occupied[k] )
        return false;
    }
    return true;
  }

  /**
   * Marks a run's cells occupied and records it for the success tally.
   *
   * @param run        the encoded run {x, y, dx, dy, length, ...}
   * @param occupied   the occupancy mask to mark
   * @param placedRuns the record of runs placed in this world
   */
  private void placeRun( int[] run, boolean[] occupied, List<int[]> placedRuns )
  {
    for ( int i = 0; i < run[4]; i++ )
      occupied[key( run[0] + i * run[2], run[1] + i * run[3] )] = true;
    placedRuns.add( run );
  }

  /**
   * Picks a uniformly random element of a set of cell keys.
   *
   * @param keys the candidate keys
   * @return one key, chosen uniformly
   */
  private int randomElement( Set<Integer> keys )
  {
    int pick = random.nextInt( keys.size() );
    for ( int k : keys )
      if ( pick-- == 0 )
        return k;
    throw new IllegalStateException( "unreachable: set was not empty" );
  }

  /**
   * Returns the targetable cell with the highest positive score, choosing
   * randomly among ties, or null if no targetable cell scored at all.
   *
   * @param quad the enemy quadrant
   * @param heat the score grid to read
   * @return the hottest targetable cell as [x, y], or null
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

    return hottest.isEmpty() ? null
                             : hottest.get( random.nextInt( hottest.size() ) );
  }

  /**
   * The quadrant whose fired-state gates occupancy votes, captured at shot
   * selection so the per-world tally can skip cells that are already hits.
   */
  private PlayerQuadrant voteQuad;

  /**
   * Lazily builds the shared static heat map: the unweighted placement count
   * of the full standard fleet over an empty board. Synchronisation is
   * unnecessary -- the computation is deterministic, so a benign race just
   * recomputes the same grid.
   */
  private void ensureStaticHeat()
  {
    if ( staticHeat != null )
      return;

    double[][] heat = new double[SIZE][SIZE];
    PlayerQuadrant empty = new PlayerQuadrant();

    for ( ShipType type : ShipType.getAscendingList() )
    {
      accumulateUnweighted( empty, heat, type.getLength(), 1, 0 );
      accumulateUnweighted( empty, heat, type.getLength(), 0, 1 );
    }
    staticHeat = heat;
  }
}
