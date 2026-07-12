package repository;

import java.sql.Connection;
import java.sql.PreparedStatement;   
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import config.Koneksi;
import model.Catatan;

public class CatatanRepository {

    public List<Catatan> getAllCatatan() {
        List<Catatan> list = new ArrayList<>();
        String sql = "SELECT id_catatan, tanggal_dibuat, judul, konten, kategori, koordinat_x, koordinat_y FROM catatan ORDER BY tanggal_dibuat DESC";
        
        // Menggunakan try-with-resources agar koneksi otomatis ditutup setelah dipakai
        try (Connection conn = Koneksi.configDB();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            
            while (rs.next()) {
                list.add(new Catatan(
                    rs.getString("id_catatan"),
                    rs.getString("judul"),
                    rs.getString("konten"), // Urutan parameter disesuaikan dengan konstruktor model Catatan
                    rs.getString("kategori"),
                    rs.getString("tanggal_dibuat"),
                    rs.getInt("koordinat_x"),
                    rs.getInt("koordinat_y")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<String> getDistinctKategori() {
        List<String> kategoriList = new ArrayList<>();
        String sql = "SELECT DISTINCT kategori FROM catatan WHERE kategori IS NOT NULL AND kategori != '' ORDER BY kategori ASC";
        try (Connection conn = Koneksi.configDB();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            
            while (rs.next()) {
                kategoriList.add(rs.getString("kategori"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return kategoriList;
    }

    public Catatan getCatatanById(String id) {
        String sql = "SELECT id_catatan, judul, kategori, konten, tanggal_dibuat, koordinat_x, koordinat_y FROM catatan WHERE id_catatan = ?";
        try (Connection conn = Koneksi.configDB();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setString(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return new Catatan(
                        rs.getString("id_catatan"),
                        rs.getString("judul"),
                        rs.getString("konten"), // Urutan disesuaikan
                        rs.getString("kategori"),
                        rs.getString("tanggal_dibuat"),
                        rs.getInt("koordinat_x"),
                        rs.getInt("koordinat_y")
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Menyimpan objek Catatan lengkap. Penting untuk command pattern.
     * Mengembalikan objek yang sama jika berhasil.
     */
   /**
     * Menyimpan objek Catatan lengkap. Penting untuk command pattern.
     * Mengembalikan objek yang sama jika berhasil.
     */
    public Catatan simpan(Catatan catatan) throws SQLException {
        String sql = "INSERT INTO catatan (judul, konten, kategori, koordinat_x, koordinat_y, tanggal_dibuat) VALUES (?, ?, ?, ?, ?, CURDATE())";
        // try-with-resources tetap dipasang, tapi letakkan throws SQLException di deklarasi method
        try (Connection conn = Koneksi.configDB();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setString(1, catatan.getJudul());
            pst.setString(2, catatan.getKonten());
            pst.setString(3, catatan.getKategori() != null ? catatan.getKategori().trim() : "");
            pst.setInt(4, catatan.getKoordinatX());
            pst.setInt(5, catatan.getKoordinatY());
            
            pst.executeUpdate();
            return catatan; 
        }
    }

    public boolean simpan(String judul, String konten, String kategori, double x, double y) throws SQLException {
        // CURDATE() untuk mendapatkan tanggal hari ini secara otomatis
        String sql = "INSERT INTO catatan (judul, konten, kategori, tanggal_dibuat, koordinat_x, koordinat_y) VALUES (?, ?, ?, CURDATE(), ?, ?)";
        try (Connection conn = Koneksi.configDB();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setString(1, judul);
            pst.setString(2, konten);
            pst.setString(3, kategori != null ? kategori.trim() : "");
            pst.setInt(4, (int) x);
            pst.setInt(5, (int) y);
            
            return pst.executeUpdate() > 0;
        }
    }



    public boolean hapus(String id) throws SQLException {
        String sql = "DELETE FROM catatan WHERE id_catatan = ?";
        try (Connection conn = Koneksi.configDB();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setString(1, id);
            return pst.executeUpdate() > 0;
        }
    }

    public boolean update(String id, String judul, String konten, String kategori) throws SQLException {
        String sql = "UPDATE catatan SET judul=?, konten=?, kategori=? WHERE id_catatan=?";
        try (Connection conn = Koneksi.configDB();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setString(1, judul);
            pst.setString(2, konten);
            pst.setString(3, kategori != null ? kategori.trim() : "");
            pst.setString(4, id);
            
            return pst.executeUpdate() > 0;
        }
    }

    public boolean updateKoordinat(String id, double x, double y) throws SQLException {
        String sql = "UPDATE catatan SET koordinat_x=?, koordinat_y=? WHERE id_catatan=?";
        try (Connection conn = Koneksi.configDB();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setInt(1, (int) x);
            pst.setInt(2, (int) y);
            pst.setString(3, id);
            
            return pst.executeUpdate() > 0;
        }
    }
}