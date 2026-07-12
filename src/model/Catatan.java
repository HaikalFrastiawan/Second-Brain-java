package model;

// File model data, urutan constructor harus sinkron dengan urutan di Repository
public class Catatan {
    private String id;
    private String judul;
    private String konten;
    private String kategori;
    private String tanggal;
    private int koordinatX;
    private int koordinatY;

    // Constructor: pastikan urutan konten -> kategori (SINKRONKAN dengan Repository.java)
    public Catatan(String id, String judul, String konten, String kategori, String tanggal, int koordinatX, int koordinatY) {
        this.id = id;
        this.judul = judul;
        this.konten = konten;
        this.kategori = kategori;
        this.tanggal = tanggal;
        this.koordinatX = koordinatX;
        this.koordinatY = koordinatY;
    }

    // Getter
    public String getId() { return id; }
    public String getJudul() { return judul; }
    public String getKonten() { return konten; }
    public String getKategori() { return kategori; }
    public String getTanggal() { return tanggal; }
    public int getKoordinatX() { return koordinatX; }
    public int getKoordinatY() { return koordinatY; }
    public void setId(String id) {
        this.id = id;
    }
}