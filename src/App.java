import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import com.formdev.flatlaf.FlatDarkLaf;
import view.MainFrame;
import config.Koneksi; // 👈 Pastikan import class Koneksi kamu

public class App {
    public static void main(String[] args) {
        // Inisialisasi Tema FlatLaf
        FlatDarkLaf.setup();

        // 🚨 UJI KONEKSI SEBELUM MEMBUKA APLIKASI
        try {
            java.sql.Connection testConn = Koneksi.configDB();
            if (testConn != null) {
                testConn.close(); // Tutup kembali setelah berhasil dites
            }
        } catch (Exception e) {
            // Jika gagal konek, langsung tembak pop-up peringatan keras
            JOptionPane.showMessageDialog(
                null, 
                "Aplikasi tidak dapat dimulai karena gagal terhubung ke Database Server!\n" +
                "Silakan pastikan MySQL di XAMPP sudah Anda aktifkan.", 
                "Database Terputus", 
                JOptionPane.ERROR_MESSAGE
            );
            System.exit(0); // Tutup aplikasi segera agar tidak terjadi error berantai
        }

        // Jika koneksi aman, barulah buka MainFrame utama
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}