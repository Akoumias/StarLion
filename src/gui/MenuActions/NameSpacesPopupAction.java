/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.MenuActions;

import graphs.Graph;
import gui.GraphInternalFrame;
import gui.InternalFrame;
import gui.ProjectManager;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Collection;
import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import model.RDFNamespace;

/**
 *
 * @author zabetak
 */
public class NameSpacesPopupAction extends AbstractAction implements MenuActionInterface {

    public void actionPerformed(ActionEvent arg0) {
        popUpSelectNamespaces();
    }

    public boolean execute() {
        return popUpSelectNamespaces();
    }

    private boolean popUpSelectNamespaces() {
        String fileName = ProjectManager.getSingleton().getActiveProject().getName();
        Collection<String> projectNamespaces = ProjectManager.getSingleton().getActiveProject().getProjectNamespaces();
        String[] nameSpaces = projectNamespaces.toArray(new String[projectNamespaces.size()]);
        RDFNamespace basicNspace = ProjectManager.getSingleton().getActiveProject().getModel().getNamespace(fileName);
        String basicNSpace = null;
        if (basicNspace != null) {
            basicNSpace = basicNspace.getURI();
        }
        String[] viewableNameSpaces = new String[nameSpaces.length];
        int k = 0;
        //bNameSpacePosition == -1 means that we couldn't figure the basic Namespace so we must show all the namespaces
        int bNameSpacePosition = -1;
        //Filter the basic namespace from the choices because it is loaded either way
        for (int i = 0; i < nameSpaces.length; i++) {
            if (nameSpaces[i] == null) {
                continue;
            }
            if (nameSpaces[i].equals(basicNSpace)) {

                bNameSpacePosition = i;
                continue;
            } else {
                viewableNameSpaces[k] = nameSpaces[i];
                k++;
            }
        }
        //We create another array because we don't want the JList to contain empty information
        String[] finalNameSpaces = new String[k];

        for (int i = 0; i < k; i++) {
            finalNameSpaces[i] = viewableNameSpaces[i];
        }

        /*String[] nameSpacesString = new String[nameSpaces.length];
        for(int i = 0;i < nameSpaces.length;i++){
        nameSpacesString[i] = nameSpaces[i].getURI();
        }*/

        //Populate a list with these names in order to facilitate user's selection
        JList list = new JList(finalNameSpaces);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL);
        if (bNameSpacePosition == -1) {
            list.setSelectedIndex(0);
        }
        list.setVisibleRowCount(-1);
        //list.setSelectedIndex(0);
        //Add list to a scrollpane and this to another panel
        JScrollPane listScroller = new JScrollPane(list);
        listScroller.setPreferredSize(new Dimension(350, 150));
        JPanel p = new JPanel();

        //Add checkbox about external links
        JCheckBox exLinks = new JCheckBox("Create External Links");
        p.add(exLinks);
        p.add(listScroller);
        //Create objects that will appear in OptionPane
        Object[] message = new Object[2];
        //Create Button names
        String[] options = {"Load", "Cancel"};
        //Add to Optionpane all previously created panels
        message[0] = new String("Select the depedencies that you want to load too (use Ctrl to select more than one):");
        message[1] = p;

        int result = JOptionPane.showOptionDialog(
                null, // the parent that the dialog blocks 
                message, // the dialog message array 
                "Load Namespaces", // the title of the dialog window 
                JOptionPane.DEFAULT_OPTION, // option type 
                JOptionPane.INFORMATION_MESSAGE, // message type 
                null, // optional icon, use null to use the default icon 
                options, // options string array, will be made into buttons 
                options[0] // option that should be made into a default button 
                );

        if (result == 0) {
            String[] loadNameSpaces;
            //bNameSpacePosition == -1 means that we couldn't figure the basic Namespace so we must show all the namespaces
            if (bNameSpacePosition != -1) {
                loadNameSpaces = new String[list.getSelectedIndices().length + 1];
            } else {
                loadNameSpaces = new String[list.getSelectedIndices().length];
            }

            int i = 0;
            for (i = 0; i < list.getSelectedIndices().length; i++) {
                loadNameSpaces[i] = viewableNameSpaces[list.getSelectedIndices()[i]];
            }

            if (bNameSpacePosition != -1) {
                loadNameSpaces[i] = nameSpaces[bNameSpacePosition];
            }

            getActiveGraph().setGraphNameSpaces(loadNameSpaces);
//            getActiveGraph().setGraphExternalLinks(exLinks.isSelected());
            return true;
        } else {
            return false;
        }
    }

    private Graph getActiveGraph() {
        InternalFrame IF = ProjectManager.getSingleton().getActiveProject().getActiveFrame();
        if (IF instanceof GraphInternalFrame) {
            return ((GraphInternalFrame) IF).getGraph();
        } else {
            return null;
        }

    }
}
