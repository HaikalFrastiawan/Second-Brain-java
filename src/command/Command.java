package command;

/**
 * Interface dasar untuk Command Pattern. Setiap aksi yang bisa di-undo/redo
 * akan mengimplementasikan interface ini.
 */
public interface Command {
    boolean execute();
    boolean undo();
}