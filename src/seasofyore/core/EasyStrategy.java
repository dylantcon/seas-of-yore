package seasofyore.core;

import java.util.ArrayList;
import java.util.List;

/**
 * The Easy AI. It performs the same predictive strikes as every other
 * difficulty once it has wounded a ship (inherited from
 * {@link AbstractTargetingStrategy}), but explores open water with the simplest
 * sound heuristic: a <em>checkerboard</em> (parity) pattern.
 *
 * <h2>Why a checkerboard?</h2>
 * The smallest ship is length 2, so it must cover one "black" and one "white"
 * cell of a checkerboard. That means firing only at, say, the black squares is
 * guaranteed to eventually touch every ship while spending half as many
 * exploratory shots. It is the classic beginner-but-not-foolish opening, and a
 * good baseline against which the smarter tiers improve.
 *
 * @author dylan
 */
public class EasyStrategy extends AbstractTargetingStrategy
{
  /**
   * Finishes a wounded ship the crude way: a random poke at a cell next to a
   * hit, with no notion of the ship's axis.
   *
   * @param quad the enemy quadrant
   * @return a random adjacent targetable cell, or null if nothing is wounded
   */
  @Override
  protected int[] selectTargetShot( PlayerQuadrant quad )
  {
    return randomAdjacentTarget( quad );
  }

  /**
   * Explores by firing at a random untouched cell of the checkerboard parity,
   * falling back to any targetable cell only once the parity is exhausted.
   *
   * @param quad the enemy quadrant to explore
   * @return coordinates as [x, y], or null if no targetable cell remains
   */
  @Override
  protected int[] selectHuntTarget( PlayerQuadrant quad )
  {
    List<int[]> parityCells = new ArrayList<>();
    List<int[]> anyCells = new ArrayList<>();

    for ( int x = 0; x < SIZE; x++ )
    {
      for ( int y = 0; y < SIZE; y++ )
      {
        if ( !quad.cellIsTargetable( x, y ) )
          continue;

        anyCells.add( new int[] { x, y } );
        if ( ( x + y ) % 2 == 0 )
          parityCells.add( new int[] { x, y } );
      }
    }

    List<int[]> pool = !parityCells.isEmpty() ? parityCells : anyCells;
    if ( pool.isEmpty() )
      return null;

    return pool.get( random.nextInt( pool.size() ) );
  }
}
