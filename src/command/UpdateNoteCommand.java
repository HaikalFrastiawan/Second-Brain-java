package command;

import model.Catatan;
import repository.CatatanRepository;
import view.MainFrame;

public class UpdateNoteCommand extends NoteCommand {

    private Catatan catatanLama;
    private Catatan catatanBaru;

    public UpdateNoteCommand(Catatan catatanLama, Catatan catatanBaru, CatatanRepository repo, MainFrame mainFrame) {
        super(null, repo, mainFrame); // Catatan utama tidak digunakan langsung
        this.catatanLama = catatanLama;
        this.catatanBaru = catatanBaru;
    }

    @Override
    public boolean execute() {
        try {
            return repo.update(catatanBaru.getId(), catatanBaru.getJudul(), catatanBaru.getKonten(), catatanBaru.getKategori());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean undo() {
        try {
            // Kembalikan ke state lama
            return repo.update(catatanLama.getId(), catatanLama.getJudul(), catatanLama.getKonten(), catatanLama.getKategori());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}