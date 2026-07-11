package repository;

import config.Koneksi;
import java.sql.*;   
import java.util.ArrayList;
import java.util.List;
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

    public boolean simpan(String judul, String konten, String kategori) throws SQLException {
        // CURDATE() untuk mendapatkan tanggal hari ini secara otomatis
        String sql = "INSERT INTO catatan (judul, konten, kategori, tanggal_dibuat, koordinat_x, koordinat_y) VALUES (?, ?, ?, CURDATE(), ?, ?)";
        try (Connection conn = Koneksi.configDB();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setString(1, judul);
            pst.setString(2, konten);
            pst.setString(3, kategori != null ? kategori.trim() : "");
            
            // Generate koordinat acak sebagai fallback posisi mekar
            int randX = (int) (Math.random() * 300.0) + 30;
            int randY = (int) (Math.random() * 400.0) + 50;
            pst.setInt(4, randX);
            pst.setInt(5, randY);
            
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
}