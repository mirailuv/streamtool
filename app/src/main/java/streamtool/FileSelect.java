package streamtool;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JPanel;

public class FileSelect extends JPanel {

    final JFileChooser fc = new JFileChooser();

    public File select() {
        int val = fc.showOpenDialog(FileSelect.this);
        if (val == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile();
        } else return null;
    }
    
}
