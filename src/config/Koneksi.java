package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Koneksi {

    private static Connection mysqlconfig;

    public static Connection configDB() throws SQLException {
        try {
            if (mysqlconfig == null || mysqlconfig.isClosed()) {
                String url = "jdbc:mysql://localhost:3306/secondbrainjava";
                String user = "root";
                String pass = "";

                // 1. Bagian Class.forName() dihapus karena sudah otomatis di JDBC modern
                mysqlconfig = DriverManager.getConnection(url, user, pass);
            }
        } catch (SQLException e) {
            // 2. Mengubah 'Exception' umum menjadi 'SQLException' yang lebih spesifik
            System.err.println("Koneksi gagal: " + e.getMessage());
            throw e; // Meneruskan error ke pemanggil agar backend tahu jika DB mati
        }
        return mysqlconfig;
    }
}
