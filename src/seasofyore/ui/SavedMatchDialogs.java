/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package seasofyore.ui;

import seasofyore.core.SavedMatch;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * The file-dialog plumbing for saving and recovering matches, shared by the
 * pause menu (save) and the title screen (load) so neither owns chooser
 * configuration, extension handling, or error reporting.
 *
 * @author dylan
 */
public final class SavedMatchDialogs
{
  /**
   * Not instantiable; a utility for the two callers.
   */
  private SavedMatchDialogs() {}

  /**
   * The chooser filter description for *.yore saves.
   */
  private static final String FILTER_DESC =
      "Seas of Yore saves (*." + SavedMatch.FILE_EXTENSION + ")";

  /**
   * Prompts for a destination and writes the match. Appends the .yore
   * extension when the user omits it, and reports failures in a dialog.
   *
   * @param parent the component to anchor dialogs to
   * @param match  the match to bottle
   * @return the file written, or null if the user cancelled or saving failed
   */
  public static File saveViaDialog( Component parent, SavedMatch match )
  {
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle( "Save thy voyage" );
    chooser.setFileFilter( new FileNameExtensionFilter(
        FILTER_DESC, SavedMatch.FILE_EXTENSION ) );

    if ( chooser.showSaveDialog( parent ) != JFileChooser.APPROVE_OPTION )
      return null;

    File file = chooser.getSelectedFile();
    if ( !file.getName().toLowerCase().endsWith( "." + SavedMatch.FILE_EXTENSION ) )
      file = new File( file.getParentFile(),
                       file.getName() + "." + SavedMatch.FILE_EXTENSION );

    try
    {
      SavedMatch.save( file, match );
      return file;
    }
    catch ( IOException ex )
    {
      JOptionPane.showMessageDialog( parent,
          "The log could not be written: " + ex.getMessage(),
          "Save failed", JOptionPane.ERROR_MESSAGE );
      return null;
    }
  }

  /**
   * Prompts for a saved file and reads the match back, reporting failures
   * in a dialog.
   *
   * @param parent the component to anchor dialogs to
   * @return the restored match, or null if the user cancelled or it failed
   */
  public static SavedMatch loadViaDialog( Component parent )
  {
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle( "Recover a voyage" );
    chooser.setFileFilter( new FileNameExtensionFilter(
        FILTER_DESC, SavedMatch.FILE_EXTENSION ) );

    if ( chooser.showOpenDialog( parent ) != JFileChooser.APPROVE_OPTION )
      return null;

    try
    {
      return SavedMatch.load( chooser.getSelectedFile() );
    }
    catch ( IOException | ClassNotFoundException ex )
    {
      JOptionPane.showMessageDialog( parent,
          "That log could not be read: " + ex.getMessage(),
          "Load failed", JOptionPane.ERROR_MESSAGE );
      return null;
    }
  }
}
