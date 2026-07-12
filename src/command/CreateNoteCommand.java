package command;

import javax.swing.JOptionPane; // 👈 Tambahkan import ini di paling atas
 
import model.Catatan;
import repository.CatatanRepository;
import view.MainFrame;

public class CreateNoteCommand extends NoteCommand {

    public CreateNoteCommand(Catatan catatan, CatatanRepository repo, MainFrame mainFrame) {
        super(catatan, repo, mainFrame);
    }

    @Override
    public boolean execute() {
        try {
            return repo.simpan(this.catatan) != null;
        } catch (Exception e) {
            // 🚨 POP-UP KETIKA SERVER DATABASE MATI ATAU EROR SQL
            JOptionPane.showMessageDialog(
                this.mainFrame, // Menggunakan mainFrame dari parent class
                "Gagal terhubung ke database server!\nPastikan MySQL di XAMPP/Laragon sudah dijalankan.", 
                "Koneksi Eror", 
                JOptionPane.ERROR_MESSAGE
            );
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean undo() {
        try {
            return repo.hapus(this.catatan.getId());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}