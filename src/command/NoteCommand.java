package command;

import model.Catatan;
import repository.CatatanRepository;
import view.MainFrame;

/**
 * Kelas dasar abstrak untuk semua command yang beroperasi pada Catatan.
 * Menyediakan referensi ke repository dan frame utama.
 */
public abstract class NoteCommand implements Command {
    protected MainFrame mainFrame;
    protected CatatanRepository repo;
    protected Catatan catatan; // Catatan yang menjadi subjek command

    public NoteCommand(Catatan catatan, CatatanRepository repo, MainFrame mainFrame) {
        this.catatan = catatan;
        this.repo = repo;
        this.mainFrame = mainFrame;
    }
}