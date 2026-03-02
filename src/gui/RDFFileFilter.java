package gui;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 * Provides a filter facility for selecting RDF-like files.
 * @author leonidis
 */
public class RDFFileFilter extends FileFilter{

	@Override
	/**
	 * Checks if this file should be accepted as a valid one
	 */
	public boolean accept(File f) {
		
		if(f.isDirectory())
			return true;
		
		String extension = getExtension(f);
        if (extension != null
                && (extension.equalsIgnoreCase("rdf")
                || extension.equalsIgnoreCase("rdfs")
                || extension.equalsIgnoreCase("ttl")
                || extension.equalsIgnoreCase("owl")))
			return true;
		else
			return false;
	}//end accept

	@Override
	/**
	 * Returns a string description of valid files
	 */
	public String getDescription() {
        String desc = ".rdf/.rdfs/.ttl/.owl files";
		return desc;
	}//end getDescription
	
	/**
	 * Returns the extension of provided file
	 * @param f file to return its extension
	 * @return extension
	 */
    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }//end getExtension
}//end ProjectFileFilter