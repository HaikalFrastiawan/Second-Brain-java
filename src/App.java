import javax.swing.SwingUtilities;
import com.formdev.flatlaf.FlatDarkLaf;

import view.MainFrame;

public class App {
    public static void main(String[] args) {
        // Inisialisasi Tema FlatLaf Dark sebelum membuat frame utama
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}   