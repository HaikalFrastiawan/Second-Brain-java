package view;

import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import model.Catatan;
import repository.CatatanRepository;

public class MainFrame extends JFrame {

    private JTable tabelCatatan;
    private DefaultTableModel tableModel;
    private JTextField txtJudul;
    private JComboBox<String> cbKategori;
    private JTextArea txtKonten;
    private JTextField txtCari;

    private JButton btnSimpan;
    private JButton btnHapus;
    private JButton btnClear;

    private GraphPanel graphPanel;
    private CatatanRepository repo;
    private String selectedId = null;

    public MainFrame() {
        setTitle("Second Brain App - Mahasiswa Edition");
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(15, 15, 28));

        repo = new CatatanRepository();
        initComponents();
        layoutComponents();
        initEvents();

        // Ambil data awal dari database untuk merender tabel dan rasi bintang
        refreshData();
    }

    private void initComponents() {
                // Inisialisasi & Kustomisasi Tabel Transparan
        String[] kolom = {"ID", "Tanggal", "Judul Catatan"};
        tableModel = new DefaultTableModel(kolom, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        tabelCatatan = new JTable(tableModel);
tabelCatatan.setAutoCreateRowSorter(true);
        // Inisialisasi Komponen Input dengan Tema Dark
        txtCari = new JTextField();
        txtCari.setBackground(new Color(20, 20, 35));
        txtCari.setForeground(Color.WHITE);
        txtCari.setCaretColor(Color.WHITE);
        txtCari.setBorder(BorderFactory.createLineBorder(new Color(40, 40, 65), 1));

        txtJudul = new JTextField();
        txtJudul.setBackground(new Color(20, 20, 35));
        txtJudul.setForeground(Color.WHITE);
        txtJudul.setCaretColor(Color.WHITE);
        txtJudul.setBorder(BorderFactory.createLineBorder(new Color(40, 40, 65), 1));

        cbKategori = new JComboBox<>();
        cbKategori.setEditable(true);
        cbKategori.setBackground(new Color(20, 20, 35));
        cbKategori.setForeground(Color.WHITE);
        // Membuat editor internal ComboBox sewarna
        Component editor = cbKategori.getEditor().getEditorComponent();
        if (editor instanceof JTextField) {
            ((JTextField) editor).setBackground(new Color(20, 20, 35));
            ((JTextField) editor).setForeground(Color.WHITE);
            ((JTextField) editor).setCaretColor(Color.WHITE);
            ((JTextField) editor).setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        }
        cbKategori.setBorder(BorderFactory.createLineBorder(new Color(40, 40, 65), 1));

        txtKonten = new JTextArea();
        txtKonten.setBackground(new Color(15, 15, 30));
        txtKonten.setForeground(Color.WHITE);
        txtKonten.setCaretColor(Color.WHITE);
        txtKonten.setLineWrap(true);
        txtKonten.setWrapStyleWord(true);
        txtKonten.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Tombol-tombol Berwarna Neon / Soft Dark
        btnClear = new JButton("Batal");
        btnClear.setBackground(new Color(50, 50, 50));
        btnClear.setForeground(Color.WHITE);
        btnClear.setFocusPainted(false);
        btnClear.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        btnSimpan = new JButton("Simpan Baru");
        btnSimpan.setBackground(new Color(40, 167, 69)); // Mengembalikan warna HIJAU EMERALD asli
        btnSimpan.setForeground(Color.WHITE);
        btnSimpan.setFocusPainted(false);
        btnSimpan.setFont(new Font("SansSerif", Font.BOLD, 11)); // Mengecilkan font ke 11 agar teks tidak kepotong (...)
        btnSimpan.setBorder(BorderFactory.createEmptyBorder(8, 5, 8, 5)); // Menipiskan padding kanan-kiri

        btnHapus = new JButton("Hapus");
        btnHapus.setBackground(new Color(235, 69, 95)); // Merah kustom hangat
        btnHapus.setForeground(Color.WHITE);
        btnHapus.setFocusPainted(false);
        btnHapus.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        tabelCatatan.setBackground(new Color(20, 20, 35));
        tabelCatatan.setForeground(Color.WHITE);
        tabelCatatan.setGridColor(new Color(30, 30, 50)); // Garis grid dibuat samar
        tabelCatatan.setRowHeight(25); // Baris lebih renggang & elegan
        tabelCatatan.setSelectionBackground(new Color(45, 45, 75)); // Warna sorot redup nyaman di mata
        tabelCatatan.setSelectionForeground(Color.WHITE);
        tabelCatatan.setShowGrid(true);

        // Desain Header Tabel agar tidak putih kaku
        tabelCatatan.getTableHeader().setBackground(new Color(25, 25, 45));
        tabelCatatan.getTableHeader().setForeground(Color.WHITE);
        tabelCatatan.getTableHeader().setBorder(BorderFactory.createLineBorder(new Color(40, 40, 65)));

        graphPanel = new GraphPanel(repo, this);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());

        // PANEL KIRI
        JPanel panelKiri = new JPanel(new GridBagLayout());
        panelKiri.setPreferredSize(new Dimension(350, 700));
        panelKiri.setBackground(new Color(15, 15, 28)); // Warna background utama kiri
        panelKiri.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(6, 4, 6, 4);

        // Judul Logs Aplikasi
        JLabel lblApp = new JLabel("📊 Second Brain Logs", JLabel.LEFT);
        lblApp.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblApp.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new java.awt.Insets(0, 4, 15, 4);
        panelKiri.add(lblApp, gbc);

        // Input Cari
        gbc.insets = new java.awt.Insets(6, 4, 6, 4);
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.2;
        JLabel lblCari = new JLabel("Cari:");
        lblCari.setForeground(Color.LIGHT_GRAY);
        panelKiri.add(lblCari, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.8;
        panelKiri.add(txtCari, gbc);

        // JScrollPane untuk Tabel
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        JScrollPane scrollTable = new JScrollPane(tabelCatatan);
        scrollTable.getViewport().setBackground(new Color(20, 20, 35));
        scrollTable.setBorder(BorderFactory.createLineBorder(new Color(40, 40, 65)));
        panelKiri.add(scrollTable, gbc);

        // Label & Input Judul
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 0.2;
        JLabel lblJudul = new JLabel("Judul:");
        lblJudul.setForeground(Color.LIGHT_GRAY);
        panelKiri.add(lblJudul, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.8;
        panelKiri.add(txtJudul, gbc);

        // Label & Input Kategori
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.2;
        JLabel lblKategori = new JLabel("Kategori:");
        lblKategori.setForeground(Color.LIGHT_GRAY);
        panelKiri.add(lblKategori, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.8;
        panelKiri.add(cbKategori, gbc);

        // Area Konten / Isi Catatan
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.weighty = 0.8;
        gbc.fill = GridBagConstraints.BOTH;
        JPanel panelIsi = new JPanel(new BorderLayout());
        panelIsi.setBackground(new Color(15, 15, 28));
        JLabel lblIsi = new JLabel("Isi:");
        lblIsi.setForeground(Color.LIGHT_GRAY);
        lblIsi.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        panelIsi.add(lblIsi, BorderLayout.NORTH);
        JScrollPane scrollKonten = new JScrollPane(txtKonten);
        scrollKonten.setBorder(BorderFactory.createLineBorder(new Color(40, 40, 65)));
        panelIsi.add(scrollKonten, BorderLayout.CENTER);
        panelKiri.add(panelIsi, gbc);

        // Barisan Tombol Aksi di Bawah
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        JPanel panelTombol = new JPanel(new GridLayout(1, 3, 10, 0));
        panelTombol.setBackground(new Color(15, 15, 28));
        panelTombol.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        panelTombol.add(btnClear);
        panelTombol.add(btnSimpan);
        panelTombol.add(btnHapus);
        panelKiri.add(panelTombol, gbc);

        add(panelKiri, BorderLayout.WEST);
        add(graphPanel, BorderLayout.CENTER);
        add(graphPanel, BorderLayout.CENTER);
    }

    private void initEvents() {
        // Event ketika baris tabel dipilih manual oleh user
        tabelCatatan.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            private boolean isAdjusting = false;

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && tabelCatatan.getSelectedRow() != -1) {
                    int row = tabelCatatan.getSelectedRow();
                    String id = tableModel.getValueAt(row, 0).toString();

                    // Supaya tidak terjadi infinity loop/tabrakan event, set ID langsung tanpa memicu seleksi ulang
                    selectedId = id;
                    tampilkanDataKeForm(id);

                    if (graphPanel != null) {
                        graphPanel.repaint();
                    }
                }
            }
        });

        // Event Tombol Simpan / Perbarui
        btnSimpan.addActionListener(e -> {
            String judul = txtJudul.getText().trim();
            String konten = txtKonten.getText().trim();
            String kategori = cbKategori.getSelectedItem() != null ? cbKategori.getSelectedItem().toString().trim() : "";

            if (judul.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Judul tidak boleh kosong!");
                return;
            }

            try {
                if (selectedId == null) {
                    repo.simpan(judul, konten, kategori);
                } else {
                    repo.update(selectedId, judul, konten, kategori);
                }
                refreshData();
                bersihkanForm();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        // Event Tombol Hapus
        btnHapus.addActionListener(e -> {
            if (selectedId == null) {
                JOptionPane.showMessageDialog(this, "Pilih catatan yang ingin dihapus terlebih dahulu!");
                return;
            }
            int opsi = JOptionPane.showConfirmDialog(this, "Hapus catatan ini?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
            if (opsi == JOptionPane.YES_OPTION) {
                try {
                    repo.hapus(selectedId);
                    refreshData();
                    bersihkanForm();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Event Tombol Clear / Batal
        btnClear.addActionListener(e -> bersihkanForm());

        // Event Pencarian Realtime saat mengetik
        txtCari.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                saring();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                saring();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                saring();
            }

            private void saring() {
                if (graphPanel != null) {
                    graphPanel.setSearchQuery(txtCari.getText());
                }
            }
        });
    }

    // ==========================================
    //   PERBAIKAN UTAMA: SISTEM SINKRONISASI 2 ARAH
    // ==========================================
    public String getSelectedId() {
        return this.selectedId;
    }

    public void setSelectedId(String id) {
        this.selectedId = id;

        if (id != null) {
            // 1. Sinkronkan seleksi warna biru ke baris tabel
            for (int i = 0; i < tabelCatatan.getRowCount(); i++) {
                if (tabelCatatan.getValueAt(i, 0).toString().equals(id)) {
                    // Memicu baris tabel terpilih secara terprogram
                    tabelCatatan.setRowSelectionInterval(i, i);
                    tabelCatatan.scrollRectToVisible(tabelCatatan.getCellRect(i, 0, true));
                    break;
                }
            }
            // 2. Isi data teks ke form input
            tampilkanDataKeForm(id);
        } else {
            // Jika diklik di luar lingkaran node, bersihkan seleksi
            tabelCatatan.clearSelection();
            bersihkanForm();
        }

        // 3. Paksa GraphPanel menggambar ulang efek lingkaran menyala
        if (graphPanel != null) {
            graphPanel.repaint();
        }
    }

    private void tampilkanDataKeForm(String id) {
        if (id == null) {
            bersihkanForm();
            return;
        }
        Catatan c = repo.getCatatanById(id);
        if (c != null) {
            txtJudul.setText(c.getJudul());
            cbKategori.setSelectedItem(c.getKategori());
            txtKonten.setText(c.getKonten());
            btnSimpan.setText("Perbarui");
            btnSimpan.setBackground(new Color(110, 68, 255)); // Berubah UNGU saat mode edit
        }
    }

    public void bersihkanForm() {
        selectedId = null;
        txtJudul.setText("");
        cbKategori.setSelectedItem("");
        txtKonten.setText("");
        tabelCatatan.clearSelection();
        btnSimpan.setText("Simpan Baru");
        btnSimpan.setBackground(new Color(40, 167, 69)); // Set kembali ke HIJAU saat kosong
        if (graphPanel != null) {
            graphPanel.repaint();
        }
    }

    public void refreshData() {
        // Ambil data terbaru dari database
        List<Catatan> list = repo.getAllCatatan();

        // Perbarui isi JTable
        tableModel.setRowCount(0);
        for (Catatan c : list) {
            tableModel.addRow(new Object[]{c.getId(), c.getTanggal(), c.getJudul()});
        }
// 2. Refresh Kategori (Pastikan aman)
        if (cbKategori != null) {
            cbKategori.removeAllItems();
            cbKategori.addItem(""); // Pilihan default kosong
            for (String kat : repo.getDistinctKategori()) {
                if (kat != null && !kat.trim().isEmpty()) {
                    cbKategori.addItem(kat);
                }
            }
        }

        // Perbarui rasi bintang di GraphPanel
        if (graphPanel != null) {
            graphPanel.SinkronkanNodes(list);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}
