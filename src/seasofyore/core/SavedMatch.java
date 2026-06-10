package seasofyore.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * A complete match, bottled: the board (which transitively carries both
 * players, their quadrants, fleets, wounds, and any AI strategy state) plus
 * the rules it was being played under. Written and read with plain Java
 * serialization -- the whole object graph in one stream -- so saving is one
 * writeObject and loading is one readObject.
 *
 * @author dylan
 */
public final class SavedMatch implements Serializable
{
  /**
   * Serialization version for saved games.
   */
  private static final long serialVersionUID = 1L;

  /**
   * The conventional file extension for a saved match: a ship's log.
   */
  public static final String FILE_EXTENSION = "yore";

  /**
   * The complete board state, including both players.
   */
  private final Board board;

  /**
   * Whether the match was being played under SALVO rules.
   */
  private final boolean salvoMode;

  /**
   * Bottles a match.
   *
   * @param board     the board mid-game
   * @param salvoMode true if the match uses SALVO rules
   */
  public SavedMatch( Board board, boolean salvoMode )
  {
    this.board = board;
    this.salvoMode = salvoMode;
  }

  /**
   * The saved board, ready to hand to a GameController.
   *
   * @return the board
   */
  public Board getBoard()
  {
    return this.board;
  }

  /**
   * Whether the saved match uses SALVO rules.
   *
   * @return true for SALVO; false for Classic
   */
  public boolean isSalvoMode()
  {
    return this.salvoMode;
  }

  /**
   * Writes a match to disk.
   *
   * @param file  the destination file
   * @param match the match to save
   * @throws IOException if the file cannot be written
   */
  public static void save( File file, SavedMatch match ) throws IOException
  {
    try ( ObjectOutputStream out =
          new ObjectOutputStream( new FileOutputStream( file ) ) )
    {
      out.writeObject( match );
    }
  }

  /**
   * Reads a match back from disk.
   *
   * @param file the saved file
   * @return the restored match
   * @throws IOException            if the file cannot be read
   * @throws ClassNotFoundException if the stream is not a saved match
   */
  public static SavedMatch load( File file )
      throws IOException, ClassNotFoundException
  {
    try ( ObjectInputStream in =
          new ObjectInputStream( new FileInputStream( file ) ) )
    {
      return (SavedMatch) in.readObject();
    }
  }
}
