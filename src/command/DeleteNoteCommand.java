package command;

import model.Catatan;
import repository.CatatanRepository;
import view.MainFrame;

public class DeleteNoteCommand extends NoteCommand {

    public DeleteNoteCommand(Catatan catatan, CatatanRepository repo, MainFrame mainFrame) {
        super(catatan, repo, mainFrame);
    }

    @Override
    public boolean execute() {
        try {
            return repo.hapus(this.catatan.getId());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean undo() {
        try {
            return repo.simpan(this.catatan) != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}