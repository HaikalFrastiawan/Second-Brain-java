package view;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.ast.Node;

import model.Catatan;
import repository.CatatanRepository;

public class MainFrame extends JFrame {

    private JTable tabelCatatan;
    private DefaultTableModel tableModel;
    private JTextField txtJudul;
    private JComboBox<String> cbKategori;
    private JTextArea txtKonten; // Untuk mode edit
    private JEditorPane previewKonten; // Untuk mode preview Markdown
    private JTabbedPane tabKonten; // Kontainer untuk editor dan preview
    private JTextField txtCari;

    private RoundedButton btnSimpan;
    private RoundedButton btnHapus;
    private RoundedButton btnClear;

    private GraphPanel graphPanel;
    private CatatanRepository repo;
    private String selectedId = null;

    // Parser dan Renderer untuk Markdown (Flexmark)
    private Parser markdownParser;
    private HtmlRenderer htmlRenderer;

    // --- PALET WARNA CYBERPUNK/DARK HUD ---
    private static final Color BG_MAIN = new Color(0x0B0F19); // Deep Space Black
    private static final Color BG_SURFACE = new Color(0x161B22); // Dark Graphite
    private static final Color BG_INPUT = new Color(0x1F2937);
    private static final Color ACCENT_GREEN = new Color(0x10B981);
    private static final Color ACCENT_RED = new Color(0xEF4444);
    private static final Color ACCENT_PURPLE = new Color(110, 68, 255);
    private static final Color ACCENT_CYAN_FOCUS = new Color(0x00E5FF);
    private static final Color BORDER_MUTED = new Color(0x4B5563);
    private static final Color TEXT_MAIN = new Color(0xF3F4F6);
    private static final Color TEXT_MUTED = Color.LIGHT_GRAY;

    public MainFrame() {
        setTitle("Second Brain App - Mahasiswa Edition");
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_MAIN);

        repo = new CatatanRepository();

        // FITUR BARU: Inisialisasi parser Markdown
        MutableDataSet options = new MutableDataSet();
        markdownParser = Parser.builder(options).build();
        htmlRenderer = HtmlRenderer.builder(options).build();

        graphPanel = new GraphPanel(repo, this); // Pastikan graphPanel dibuat SEBELUM digunakan

        initComponents();
        layoutComponents();
        initEvents();

        // Ambil data awal dari database untuk merender tabel dan rasi bintang
        refreshData();
    }

    private void applyModernStyling(JComponent component, Border defaultBorder, Border focusBorder) {
        component.setBorder(defaultBorder);
        component.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { component.setBorder(focusBorder); }
            @Override public void focusLost(FocusEvent e) { component.setBorder(defaultBorder); }
        });
    }

    private void initComponents() {
        String[] kolom = {"ID", "Tanggal", "Judul Catatan"};
        tableModel = new DefaultTableModel(kolom, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        tabelCatatan = new JTable(tableModel);
        tabelCatatan.setAutoCreateRowSorter(true);

        // --- Kustomisasi Form Input Modern ---
        Border defaultBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_MUTED, 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        );
        Border focusBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_CYAN_FOCUS, 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        );

        txtCari = new JTextField();
        txtCari.setBackground(BG_INPUT);
        txtCari.setForeground(TEXT_MAIN);
        txtCari.setCaretColor(ACCENT_CYAN_FOCUS);
        applyModernStyling(txtCari, defaultBorder, focusBorder);

        txtJudul = new JTextField();
        txtJudul.setBackground(BG_INPUT);
        txtJudul.setForeground(TEXT_MAIN);
        txtJudul.setCaretColor(ACCENT_CYAN_FOCUS);
        applyModernStyling(txtJudul, defaultBorder, focusBorder);

        cbKategori = new JComboBox<>();
        cbKategori.setEditable(true);
        cbKategori.setBackground(BG_INPUT);
        cbKategori.setForeground(TEXT_MAIN);
        cbKategori.setBorder(BorderFactory.createLineBorder(BORDER_MUTED, 1));
        // FlatLaf menangani styling ComboBox dengan baik, jadi kustomisasi minimal

        // FITUR BARU: Mengganti JTextArea dengan Tabbed Pane untuk Edit & Preview
        tabKonten = new JTabbedPane();
        tabKonten.setBackground(BG_MAIN);
        tabKonten.setForeground(TEXT_MUTED);

        txtKonten = new JTextArea();
        txtKonten.setBackground(BG_INPUT);
        txtKonten.setForeground(TEXT_MAIN);
        txtKonten.setCaretColor(ACCENT_CYAN_FOCUS);
        txtKonten.setLineWrap(true);
        txtKonten.setWrapStyleWord(true);
        JScrollPane scrollEdit = new JScrollPane(txtKonten);
        scrollEdit.setBorder(null);

        previewKonten = new JEditorPane();
        previewKonten.setContentType("text/html");
        previewKonten.setEditable(false);
        previewKonten.setBackground(BG_INPUT);
        JScrollPane scrollPreview = new JScrollPane(previewKonten);

        tabKonten.addTab("📝 Edit", scrollEdit);
        tabKonten.addTab("👁️ Preview", scrollPreview);

        // --- Tombol Aksi Rounded & Glowing ---
        btnClear = new RoundedButton("Batal");
        btnClear.setBackground(BORDER_MUTED);

        btnSimpan = new RoundedButton("Simpan Baru");
        btnSimpan.setBackground(ACCENT_GREEN);

        btnHapus = new RoundedButton("Hapus");
        btnHapus.setBackground(ACCENT_RED);

        // --- Kustomisasi Tabel Futuristik ---
        tabelCatatan.setBackground(BG_MAIN);
        tabelCatatan.setForeground(TEXT_MAIN);
        tabelCatatan.setGridColor(BG_SURFACE);
        tabelCatatan.setRowHeight(30);
        tabelCatatan.setSelectionBackground(ACCENT_CYAN_FOCUS.darker());
        tabelCatatan.setSelectionForeground(Color.WHITE);
        tabelCatatan.setShowGrid(true);
        tabelCatatan.setShowVerticalLines(false); // Hilangkan garis vertikal

        tabelCatatan.getTableHeader().setBackground(BG_SURFACE);
        tabelCatatan.getTableHeader().setForeground(TEXT_MAIN);
        tabelCatatan.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabelCatatan.getTableHeader().setBorder(BorderFactory.createLineBorder(BORDER_MUTED));
        
        // Pindahkan pengaturan renderer ke sini agar hanya diatur sekali.
        // Ini memperbaiki ClassCastException dan inefisiensi.
        tabelCatatan.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10)); // Padding sel
                setForeground(TEXT_MAIN);
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? BG_MAIN : BG_SURFACE);
                }
                setText(value != null ? value.toString() : "");
                return this;
            }
        });

    }

    private void layoutComponents() {
        setLayout(new BorderLayout());

        // PANEL KIRI
        JPanel panelKiri = new JPanel(new GridBagLayout());
        panelKiri.setPreferredSize(new Dimension(380, 700)); // Lebarkan sedikit
        panelKiri.setBackground(BG_MAIN);
        panelKiri.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(6, 4, 6, 4);

        // Judul Logs Aplikasi
        JLabel lblApp = new JLabel("📊 Second Brain Logs", JLabel.LEFT);
        lblApp.setFont(new Font("Segoe UI", Font.BOLD, 20));
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
        lblCari.setForeground(TEXT_MUTED);
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
        scrollTable.getViewport().setBackground(BG_MAIN);
        scrollTable.setBorder(BorderFactory.createLineBorder(BORDER_MUTED));
        panelKiri.add(scrollTable, gbc);

        // Label & Input Judul
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 0.2;
        JLabel lblJudul = new JLabel("Judul:");
        lblJudul.setForeground(TEXT_MUTED);
        panelKiri.add(lblJudul, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.8;
        panelKiri.add(txtJudul, gbc);

        // Label & Input Kategori
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.2;
        JLabel lblKategori = new JLabel("Kategori:");
        lblKategori.setForeground(TEXT_MUTED);
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
        

        // Barisan Tombol Aksi di Bawah
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        JPanel panelTombol = new JPanel(new GridLayout(1, 3, 10, 0));
        panelTombol.setOpaque(false); // Buat transparan
        panelTombol.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        panelTombol.add(btnClear);
        panelTombol.add(btnSimpan);
        panelTombol.add(btnHapus);
        panelKiri.add(panelTombol, gbc);

        // Pindahkan penambahan tabKonten ke sini setelah panelTombol
        // agar referensi 'defaultBorder' valid.
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.weighty = 0.8;
        gbc.fill = GridBagConstraints.BOTH;
        panelKiri.add(tabKonten, gbc);

        add(panelKiri, BorderLayout.WEST);
        add(graphPanel, BorderLayout.CENTER);
    }

    private void initEvents() {
        // Event ketika baris tabel dipilih manual oleh user
        tabelCatatan.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = tabelCatatan.getSelectedRow();
                if (selectedRow != -1) {
                    // Konversi view index ke model index jika ada sorting
                    int modelRow = tabelCatatan.convertRowIndexToModel(selectedRow);
                    String id = tableModel.getValueAt(modelRow, 0).toString();
                    
                    // Panggil metode sinkronisasi yang sudah ada
                    setSelectedId(id);
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

        // FITUR BARU: Update preview Markdown saat tab diubah
        tabKonten.addChangeListener(e -> {
            if (tabKonten.getSelectedIndex() == 1) { // Jika tab "Preview" dipilih
                String markdownText = txtKonten.getText();
                Node document = markdownParser.parse(markdownText);
                String html = htmlRenderer.render(document);
                // Tambahkan sedikit CSS agar sesuai tema gelap
                previewKonten.setText("<html><body style='background-color:#1F2937; color:#F3F4F6; font-family:sans-serif; padding:8px;'>" + html + "</body></html>");
            }
        });

        // FITUR BARU: Simpan posisi node saat aplikasi ditutup
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                graphPanel.simpanPosisiNode();
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
            btnSimpan.setBackground(ACCENT_PURPLE); // Berubah UNGU saat mode edit
        }
    }

    public void bersihkanForm() {
        selectedId = null;
        txtJudul.setText("");
        cbKategori.setSelectedItem("");
        txtKonten.setText("");
        tabelCatatan.clearSelection();
        btnSimpan.setText("Simpan Baru");
        btnSimpan.setBackground(ACCENT_GREEN); // Set kembali ke HIJAU saat kosong
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
}
