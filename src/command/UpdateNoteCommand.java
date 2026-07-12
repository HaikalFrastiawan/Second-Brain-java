package command;

import model.Catatan;
import repository.CatatanRepository;
import view.MainFrame;

public class UpdateNoteCommand extends NoteCommand {
    private Catatan catatanLama;
    private Catatan catatanBaru;

    public UpdateNoteCommand(Catatan catatanLama, Catatan catatanBaru, CatatanRepository repo, MainFrame mainFrame) {
        super(catatanBaru, repo, mainFrame); // Parent class memakai data terbaru
        this.catatanLama = catatanLama;
        this.catatanBaru = catatanBaru;
    }@Override
    public boolean execute() {
        try {
            // REDO: Jalankan update dengan membongkar data dari catatanBaru
            return repo.update(
                this.catatanBaru.getId(), 
                this.catatanBaru.getJudul(), 
                this.catatanBaru.getKonten(), 
                this.catatanBaru.getKategori()
            );
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean undo() {
        try {
            // UNDO: Kembalikan data lama dengan membongkar data dari catatanLama
            return repo.update(
                this.catatanLama.getId(), 
                this.catatanLama.getJudul(), 
                this.catatanLama.getKonten(), 
                this.catatanLama.getKategori()
            );
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}