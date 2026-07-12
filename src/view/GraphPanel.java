package view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import model.Catatan;
import repository.CatatanRepository;

class Link {
    String sourceId;
    String targetId;
    public Link(String s, String t) { sourceId = s; targetId = t; }
}

public class GraphPanel extends JPanel {

    private List<GraphNode> graphNodes = new ArrayList<>();
    private double zoomFactor = 1.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;
    private Point lastMousePosition;
    private CatatanRepository repo;
    private MainFrame mainFrame;
    private String searchQuery = "";
    private boolean isInitializingAnimation = true;
    private int animationTicks = 0;
    private float pulseTracker = 0.0f;
    private boolean pulseForward = true;
    private JButton btnZoomIn, btnZoomOut, btnResetZoom, btnExportMd;

    // Konstanta Sistem Fisika (Force-Directed Graph)
    private final double REPEL_FORCE = 700.0; // Gaya tolak-menolak antar node agar tidak bertumpuk
    private final double SPRING_FORCE = 0.035; // Gaya tarik pegas elastis satu kategori
    private final double DAMPING = 0.82;       // Redaman hambatan udara agar gerakan stabil
    private final int MAX_LINE_DIST = 350;      // Jarak maksimum untuk menggambar garis koneksi

    // Cache untuk data yang sering diakses di paintComponent
    private Map<String, String> idToCategoryMap = new HashMap<>();
    private Map<String, String> idToTitleMap = new HashMap<>();
    private Map<String, String> titleToIdMap = new HashMap<>(); // Untuk parsing link [[Judul]]
    private Map<String, ArrayList<GraphNode>> categoryGroups = new HashMap<>();
    private List<Link> explicitLinks = new ArrayList<>(); // FITUR BARU: Untuk link [[...]]

    // FITUR BARU: Untuk menu klik kanan
    private JPopupMenu contextMenu;
    private Point2D.Double contextMenuClickPoint;

    // Constructor bawaan (urutan baru)
    public GraphPanel(CatatanRepository repo, MainFrame mainFrame) {
        this.repo = repo;
        this.mainFrame = mainFrame;
        // Inisialisasi cache data saat pertama kali dibuat
        perbaruiCacheData();

        setBackground(new Color(10, 10, 18));
        initControls();
        initMouseListeners();
        initContextMenu(); // Inisialisasi menu klik kanan
        initGraphEngineTimer();
    }

    /**
     * FITUR BARU: Menginisialisasi JPopupMenu untuk klik kanan.
     */
    private void initContextMenu() {
        contextMenu = new JPopupMenu();
        JMenuItem createItem = new JMenuItem("Buat Catatan Baru di Sini");
        createItem.addActionListener(e -> {
            String judul = JOptionPane.showInputDialog(
                mainFrame,
                "Masukkan judul untuk catatan baru:",
                "Buat Catatan Baru",
                JOptionPane.PLAIN_MESSAGE
            );

            if (judul != null && !judul.trim().isEmpty()) {
                try {
                    repo.simpan(judul, "", "", contextMenuClickPoint.getX(), contextMenuClickPoint.getY());
                    mainFrame.refreshData();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(mainFrame, "Gagal menyimpan catatan baru.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        contextMenu.add(createItem);
    }

    private void initControls() {
        // 1. Set layout null khusus di GraphPanel agar tombol bisa melayang di pojok
        setLayout(null);

        Font fontSinyal = new Font("SansSerif", Font.BOLD, 14);
        Color warnaBgTombol = new Color(25, 25, 45, 220); // Semi-transparan dark
        Color warnaGaris = new Color(60, 60, 90);

        // Inisialisasi komponen overlay
        btnZoomIn = new JButton("+");
        btnZoomOut = new JButton("-");
        btnResetZoom = new JButton("🎯");
        btnExportMd = new JButton("📝 Export Weekly Review (.md)");

        JButton[] listTombol = {btnZoomIn, btnZoomOut, btnResetZoom, btnExportMd};
        for (JButton btn : listTombol) {
            btn.setBackground(warnaBgTombol);
            btn.setForeground(Color.WHITE);
            btn.setFont(btn == btnExportMd ? new Font("SansSerif", Font.BOLD, 11) : fontSinyal);
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createLineBorder(warnaGaris, 1));
            add(btn); // Masukkan ke dalam panel grafik
        }

        // 2. Logika Aksi Tombol
        btnZoomIn.addActionListener(e -> {
            zoomFactor = Math.min(3.0, zoomFactor * 1.2);
            repaint();
        });
        btnZoomOut.addActionListener(e -> {
            zoomFactor = Math.max(0.3, zoomFactor / 1.2);
            repaint();
        });
        btnResetZoom.addActionListener(e -> {
            zoomFactor = 1.0;
            offsetX = 0.0;
            offsetY = 0.0;
            repaint();
        });
        btnExportMd.addActionListener(e -> aksiExportMarkdown());

        // 3. Atur posisi koordinat tombol secara dinamis saat window ditarik/di-resize
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = getWidth();
                int h = getHeight();

                // Pojok Kanan Atas (Kontrol Zoom)
                btnZoomIn.setBounds(w - 120, 20, 30, 30);
                btnZoomOut.setBounds(w - 85, 20, 30, 30);
                btnResetZoom.setBounds(w - 50, 20, 30, 30);

                // Pojok Kanan Bawah (Tombol Export MD)
                btnExportMd.setBounds(w - 220, h - 50, 200, 32);
            }
        });
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query.toLowerCase().trim();
        repaint();
    }

    public void adjustZoom(double increment) {
        if (increment > 0) {
            this.zoomFactor = Math.min(3.0, this.zoomFactor * increment);
        } else {
            this.zoomFactor = Math.max(0.3, this.zoomFactor / Math.abs(increment));
        }
        repaint();
    }

    public void resetView() {
        this.zoomFactor = 1.0;
        this.offsetX = 0.0;
        this.offsetY = 0.0;
        repaint();
    }

public void SinkronkanNodes(List<Catatan> catatanList) {
    List<GraphNode> nodesBaru = new ArrayList<>();
    
    // Gunakan fallback ukuran jika panel belum selesai di-render di layar
    int cx = getWidth() > 50 ? getWidth() / 2 : 400;
    int cy = getHeight() > 50 ? getHeight() / 2 : 300;

    for (Catatan c : catatanList) {
        GraphNode existingNode = cariNodeLama(c.getId());
        if (existingNode != null) {
            existingNode.title = c.getJudul();
            existingNode.color = dapatkanWarnaKategori(c.getKategori());
            nodesBaru.add(existingNode);
        } else {
            // FITUR BARU: Gunakan koordinat dari DB jika ada, jika tidak, baru buat posisi acak.
            double nx, ny;
            if (c.getKoordinatX() > 0 && c.getKoordinatY() > 0) {
                nx = c.getKoordinatX();
                ny = c.getKoordinatY();
            } else {
                // Fallback: Posisi acak jika koordinat belum ada di DB
                double angle = Math.random() * 2.0 * Math.PI;
                double radius = 100 + (Math.random() * 200);
                nx = cx + Math.cos(angle) * radius;
                ny = cy + Math.sin(angle) * radius;
            }
            nodesBaru.add(new GraphNode(c.getId(), c.getJudul(), nx, ny, dapatkanWarnaKategori(c.getKategori())));
        }
    }
    this.graphNodes = nodesBaru;
    // Perbarui cache setiap kali node disinkronkan
    perbaruiCacheData();
    repaint();
}

    public void simpanPosisiNode() {
        System.out.println("Menyimpan posisi node...");
        for (GraphNode node : graphNodes) {
            try {
                repo.updateKoordinat(node.id, node.x, node.y);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Memperbarui cache data dari repository untuk menghindari pemanggilan berulang
     * di dalam `paintComponent`.
     */
    private void perbaruiCacheData() {
        idToCategoryMap.clear();
        idToTitleMap.clear();
        titleToIdMap.clear();
        categoryGroups.clear();
        explicitLinks.clear();

        List<Catatan> semuaCatatan = repo.getAllCatatan();
        for (Catatan c : semuaCatatan) {
            String kat = (c.getKategori() == null || c.getKategori().trim().isEmpty()) ? "Uncategorized" : c.getKategori();
            idToCategoryMap.put(c.getId(), kat);
            idToTitleMap.put(c.getId(), c.getJudul().toLowerCase());
            titleToIdMap.put(c.getJudul().toLowerCase(), c.getId());
        }

        // FITUR BARU: Parsing link [[...]] dari konten setiap catatan
        var linkPattern = Pattern.compile("\\[\\[(.*?)\\]\\]");
        for (Catatan c : semuaCatatan) {
            var matcher = linkPattern.matcher(c.getKonten());
            while (matcher.find()) {
                String targetTitle = matcher.group(1).toLowerCase().trim();
                String targetId = titleToIdMap.get(targetTitle);
                if (targetId != null && !targetId.equals(c.getId())) {
                    explicitLinks.add(new Link(c.getId(), targetId));
                }
            }
        }

        // Bangun ulang pengelompokan node berdasarkan kategori dari cache
        // Ini harus dilakukan di sini agar `updatePhysics` memiliki data grup terbaru.
        categoryGroups.clear();
        for (GraphNode node : graphNodes) {
            String cat = idToCategoryMap.getOrDefault(node.id, "Uncategorized");
            categoryGroups.computeIfAbsent(cat, k -> new ArrayList<>()).add(node);
        }
    }

    public void refreshNodes(List<GraphNode> newNodes) {
        this.graphNodes = newNodes;
        this.isInitializingAnimation = true;
        this.animationTicks = 0;
        repaint();
    }

    private void initGraphEngineTimer() {
        Timer timer = new Timer(16, e -> {
            updatePhysics();
            if (pulseForward) {
                pulseTracker += 0.015f;
                if (pulseTracker >= 1.0f) {
                    pulseTracker = 1.0f;
                    pulseForward = false;
                }
            } else {
                pulseTracker -= 0.015f;
                if (pulseTracker <= 0.0f) {
                    pulseTracker = 0.0f;
                    pulseForward = true;
                }
            }
        });
        timer.start();
    }

    public void updatePhysics() {
        int MAX_LINE_DIST = 350;

        if (graphNodes == null || graphNodes.isEmpty()) {
            return;
        }

        // 1. Gaya Tolak-Menolak (Repulsion) antar semua node
        for (int i = 0; i < graphNodes.size(); i++) {
            for (int j = i + 1; j < graphNodes.size(); j++) {
                GraphNode n1 = graphNodes.get(i);
                GraphNode n2 = graphNodes.get(j);

                double dx = n1.x - n2.x;
                double dy = n1.y - n2.y;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist == 0) dist = 1; // Hindari pembagian dengan nol

                // Terapkan gaya tolak jika jarak terlalu dekat
                if (dist < 200) {
                    double force = REPEL_FORCE / (dist * dist);
                    double forceX = (dx / dist) * force;
                    double forceY = (dy / dist) * force;

                    n1.vx += forceX;
                    n1.vy += forceY;
                    n2.vx -= forceX; // Terapkan gaya berlawanan pada node kedua
                    n2.vy -= forceY;
                }
            }
        }

        // 2. Gaya Tarik Pegas (Spring) untuk node dalam kategori yang sama
        for (ArrayList<GraphNode> group : categoryGroups.values()) {
            for (int i = 0; i < group.size(); i++) {
                for (int j = i + 1; j < group.size(); j++) {
                    GraphNode n1 = group.get(i);
                    GraphNode n2 = group.get(j);

                    double dx = n2.x - n1.x;
                    double dy = n2.y - n1.y;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist == 0) dist = 1;

                    double force = SPRING_FORCE * (dist - 150); // 150 adalah panjang ideal pegas
                    n1.vx += (dx / dist) * force;
                    n1.vy += (dy / dist) * force;
                    n2.vx -= (dx / dist) * force;
                    n2.vy -= (dy / dist) * force;
                }
            }
        }

        // 3. Update posisi node berdasarkan akumulasi gaya dan terapkan redaman
        for (GraphNode node : graphNodes) {
            node.x += Math.max(-6, Math.min(6, node.vx));
            node.y += Math.max(-6, Math.min(6, node.vy));
            node.vx *= DAMPING;
            node.vy *= DAMPING;

            // TAMBAHKAN PEMBATAS DINDING INI:
            if (getWidth() > 0 && getHeight() > 0) {
                node.x = Math.max(50, Math.min(getWidth() - 50, node.x));
                node.y = Math.max(50, Math.min(getHeight() - 50, node.y));
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;

        AffineTransform oldXForm = g2d.getTransform();
        g2d.translate(cx, cy);
        g2d.scale(zoomFactor, zoomFactor);
        g2d.translate(-cx + offsetX, -cy + offsetY);

        Graphics2D gGlow = (Graphics2D) g2d.create();
        float[] dist = {0.0f, 0.7f, 1.0f};
        Color[] colors = {new Color(0, 140, 255, 9), new Color(0, 45, 90, 3), new Color(10, 10, 18, 0)};
        RadialGradientPaint rgb = new RadialGradientPaint(cx, cy, 550, dist, colors);
        gGlow.setPaint(rgb);
        gGlow.fillOval(cx - 550, cy - 550, 1100, 1100);
        gGlow.dispose();

        // Gambar garis koneksi antar node
        drawConnections(g2d);
        drawExplicitLinks(g2d); // FITUR BARU

        // Gambar setiap node dan labelnya
        String selectedId = mainFrame.getSelectedId();
        String selectedCategory = (selectedId != null) ? idToCategoryMap.get(selectedId) : null;

        for (GraphNode node : graphNodes) {
            // Cukup panggil metode helper yang sudah bersih dan terstruktur
            drawNode(g2d, node, selectedId, selectedCategory);
            drawNodeLabel(g2d, node, selectedId, selectedCategory);
        }
        g2d.setTransform(oldXForm);
    }

    private void aksiExportMarkdown() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Simpan Weekly Review");
        fileChooser.setSelectedFile(new File("Weekly_Review.md"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            List<Catatan> semuaCatatan = repo.getAllCatatan(); // Ambil data dari repo

            try (FileWriter writer = new FileWriter(file)) {
                writer.write("# 🧠 Weekly Review Summary - Second Brain\n\n");
                writer.write("Dibuat otomatis pada tanggal: " + java.time.LocalDate.now() + "\n\n---\n\n");

                for (Catatan c : semuaCatatan) {
                    writer.write("## 📝 " + c.getJudul() + "\n");
                    writer.write("* **Kategori:** `" + (c.getKategori().isEmpty() ? "Uncategorized" : c.getKategori()) + "`\n");
                    writer.write("* **Tanggal:** " + c.getTanggal() + "\n\n");
                    writer.write("### Isi Catatan:\n" + c.getKonten() + "\n\n");
                    writer.write("---\n\n");
                }

                JOptionPane.showMessageDialog(this, "Berhasil mengeksport file Markdown ke:\n" + file.getAbsolutePath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Gagal mengeksport file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void drawConnections(Graphics2D g2d) {
        String selectedId = mainFrame.getSelectedId();
        String selectedCategory = (selectedId != null) ? idToCategoryMap.get(selectedId) : null;

        if (selectedId == null || selectedCategory == null) {
            return;
        }

        ArrayList<GraphNode> nodesInSelectedGroup = categoryGroups.get(selectedCategory);
        if (nodesInSelectedGroup == null) {
            return;
        }

        g2d.setColor(new Color(0, 215, 255, 175));
        g2d.setStroke(new BasicStroke(1.5f));

        for (int i = 0; i < nodesInSelectedGroup.size(); i++) {
            for (int j = i + 1; j < nodesInSelectedGroup.size(); j++) {
                GraphNode n1 = nodesInSelectedGroup.get(i);
                GraphNode n2 = nodesInSelectedGroup.get(j);
                double d = Math.hypot(n1.x - n2.x, n1.y - n2.y);

                if (d <= MAX_LINE_DIST) {
                    int x1 = (int) n1.x + 4;
                    int y1 = (int) n1.y + 4;
                    int x2 = (int) n2.x + 4;
                    int y2 = (int) n2.y + 4;
                    g2d.drawLine(x1, y1, x2, y2);

                    // Gambar pulsa animasi di garis
                    int pulseX = (int) (x1 + (x2 - x1) * pulseTracker);
                    int pulseY = (int) (y1 + (y2 - y1) * pulseTracker);
                    g2d.setColor(Color.WHITE);
                    g2d.fillOval(pulseX - 2, pulseY - 2, 4, 4);
                    g2d.setColor(new Color(0, 215, 255, 175)); // Kembalikan warna untuk garis berikutnya
                }
            }
        }
    }

    private void drawExplicitLinks(Graphics2D g2d) {
        if (explicitLinks.isEmpty()) return;

        g2d.setColor(new Color(255, 255, 0, 180)); // Warna kuning untuk link eksplisit
        g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{6, 4}, 0)); // Garis putus-putus

        Map<String, GraphNode> nodeMap = new HashMap<>();
        for (GraphNode n : graphNodes) {
            nodeMap.put(n.id, n);
        }

        for (Link link : explicitLinks) {
            GraphNode sourceNode = nodeMap.get(link.sourceId);
            GraphNode targetNode = nodeMap.get(link.targetId);

            if (sourceNode != null && targetNode != null) {
                int x1 = (int) sourceNode.x + 4;
                int y1 = (int) sourceNode.y + 4;
                int x2 = (int) targetNode.x + 4;
                int y2 = (int) targetNode.y + 4;
                g2d.drawLine(x1, y1, x2, y2);
            }
        }
    }

    private void drawNode(Graphics2D g2d, GraphNode node, String selectedId, String selectedCategory) {
        final int BASE_DIAMETER = 8;
        final int MAX_EXTRA_DIAMETER = 10;
        final int GLOW_OFFSET = 12;
        final int NODE_DRAW_OFFSET = 4;

        boolean isSelected = node.id.equals(selectedId);
        String currentCategory = idToCategoryMap.getOrDefault(node.id, "Uncategorized");
        String lowerTitle = idToTitleMap.getOrDefault(node.id, "");
        boolean matchesSearch = searchQuery.isEmpty() || lowerTitle.contains(searchQuery) || currentCategory.toLowerCase().contains(searchQuery);

        int baseGroupSize = categoryGroups.getOrDefault(currentCategory, new ArrayList<>()).size();
        int nodeDiameter = BASE_DIAMETER + Math.min(MAX_EXTRA_DIAMETER, baseGroupSize / 2);
        int glowDiameter = nodeDiameter + GLOW_OFFSET;

        int alphaGlow = 35, alphaCore = 220;

        if (!matchesSearch) {
            alphaGlow = 2; alphaCore = 25;
        } else if (selectedId != null) {
            if (!currentCategory.equals(selectedCategory) && !isSelected) {
                alphaGlow = 3; alphaCore = 40;
            } else if (!isSelected) {
                alphaGlow = 75; alphaCore = 255;
            }
        }

        int nx = (int) node.x;
        int ny = (int) node.y;

        if (isSelected) {
            g2d.setColor(new Color(255, 0, 100, 110)); // Warna glow khusus untuk node terpilih
            g2d.fillOval(nx - (glowDiameter / 2) + NODE_DRAW_OFFSET, ny - (glowDiameter / 2) + NODE_DRAW_OFFSET, glowDiameter, glowDiameter);
            g2d.setColor(Color.WHITE); // Warna inti khusus untuk node terpilih
            g2d.fillOval(nx - (nodeDiameter / 2) + NODE_DRAW_OFFSET, ny - (nodeDiameter / 2) + NODE_DRAW_OFFSET, nodeDiameter, nodeDiameter);
        } else {
            g2d.setColor(new Color(node.color.getRed(), node.color.getGreen(), node.color.getBlue(), alphaGlow));
            g2d.fillOval(nx - (glowDiameter / 2) + NODE_DRAW_OFFSET, ny - (glowDiameter / 2) + NODE_DRAW_OFFSET, glowDiameter, glowDiameter);
            g2d.setColor(new Color(node.color.getRed(), node.color.getGreen(), node.color.getBlue(), alphaCore));
            g2d.fillOval(nx - (nodeDiameter / 2) + NODE_DRAW_OFFSET, ny - (nodeDiameter / 2) + NODE_DRAW_OFFSET, nodeDiameter, nodeDiameter);
        }
    }

    private void drawNodeLabel(Graphics2D g2d, GraphNode node, String selectedId, String selectedCategory) {
        boolean isSelected = node.id.equals(selectedId);
        String currentCategory = idToCategoryMap.getOrDefault(node.id, "Uncategorized");
        String lowerTitle = idToTitleMap.getOrDefault(node.id, "");
        boolean matchesSearch = searchQuery.isEmpty() || lowerTitle.contains(searchQuery) || currentCategory.toLowerCase().contains(searchQuery);

        int alphaText = 165;
        if (!matchesSearch) {
            alphaText = 15;
        } else if (selectedId != null) {
            if (!currentCategory.equals(selectedCategory) && !isSelected) {
                alphaText = 30;
            } else if (!isSelected) {
                alphaText = 225;
            }
        }

        if (isSelected) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        } else {
            g2d.setColor(new Color(195, 195, 215, alphaText));
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));
        }
        
        int nodeDiameter = 8 + Math.min(10, categoryGroups.getOrDefault(currentCategory, new ArrayList<>()).size() / 2);
        g2d.drawString(node.title, (int)node.x - 15, (int)node.y + nodeDiameter + 10);
    }

private GraphNode nodeSedangDiedit = null;

private void initMouseListeners() {
    addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            lastMousePosition = e.getPoint();
            if (e.isPopupTrigger()) {
                showContextMenu(e);
                return;
            }

            // --- FITUR GESER NODE: Cari tahu apakah klik jatuh di atas sebuah Node ---
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;

            java.awt.geom.AffineTransform tx = new java.awt.geom.AffineTransform();
            tx.translate(cx, cy);
            tx.scale(zoomFactor, zoomFactor);
            tx.translate(-cx + offsetX, -cy + offsetY);

            java.awt.geom.Point2D dstTransform = new java.awt.geom.Point2D.Double();
            try {
                tx.inverseTransform(e.getPoint(), dstTransform);
                double clickedX = dstTransform.getX();
                double clickedY = dstTransform.getY();
                double klikToleransi = 18.0;

                nodeSedangDiedit = null; // reset sebelum mencari
                for (GraphNode node : graphNodes) {
                    double dx = clickedX - (node.x + 4);
                    double dy = clickedY - (node.y + 4);
                    if (Math.sqrt(dx * dx + dy * dy) <= klikToleransi) {
                        nodeSedangDiedit = node; // Node berhasil dikunci untuk diseret!
                        break;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showContextMenu(e);
            }
            
            // --- FITUR GESER NODE: Saat lepas klik, simpan koordinat baru ke MySQL ---
           if (nodeSedangDiedit != null) {
                try {
                    // LANGSUNG KIRIM nodeSedangDiedit.id (tanpa di-parse ke int)
                    repo.updateKoordinat(nodeSedangDiedit.id, nodeSedangDiedit.x, nodeSedangDiedit.y);
                    System.out.println("Menyimpan posisi node ID: " + nodeSedangDiedit.id);
                } catch (Exception ex) {
                    System.err.println("Gagal auto-save koordinat ke DB: " + ex.getMessage());
                }
                nodeSedangDiedit = null; // lepas kuncian node
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;

            if (SwingUtilities.isRightMouseButton(e)) return;

            java.awt.geom.AffineTransform tx = new java.awt.geom.AffineTransform();
            tx.translate(cx, cy);
            tx.scale(zoomFactor, zoomFactor);
            tx.translate(-cx + offsetX, -cy + offsetY);

            java.awt.Point srcTransform = e.getPoint();
            java.awt.geom.Point2D dstTransform = new java.awt.geom.Point2D.Double();

            try {
                tx.inverseTransform(srcTransform, dstTransform);

                double clickedX = dstTransform.getX();
                double clickedY = dstTransform.getY();

                GraphNode nodeTerpilih = null;
                double klikToleransi = 18.0;

                for (GraphNode node : graphNodes) {
                    double dx = clickedX - (node.x + 4);
                    double dy = clickedY - (node.y + 4);
                    double distance = Math.sqrt(dx * dx + dy * dy);

                    if (distance <= klikToleransi) {
                        nodeTerpilih = node;
                        break;
                    }
                }

                if (nodeTerpilih != null) {
                    mainFrame.setSelectedId(nodeTerpilih.id);
                } else {
                    mainFrame.setSelectedId(null);
                }

                repaint();

            } catch (java.awt.geom.NoninvertibleTransformException ex) {
                ex.printStackTrace();
            }
        }
    });

    addMouseMotionListener(new MouseMotionAdapter() {
        @Override
        public void mouseDragged(MouseEvent e) {
            if (lastMousePosition != null) {
                int dx = e.getX() - lastMousePosition.x;
                int dy = e.getY() - lastMousePosition.y;

                if (nodeSedangDiedit != null) {
                    // A. JIKA KLIK DI ATAS NODE: Geser nodenya saja
                    nodeSedangDiedit.x += dx / zoomFactor;
                    nodeSedangDiedit.y += dy / zoomFactor;
                } else {
                    // B. JIKA KLIK DI AREA KOSONG: Geser seluruh background/papan
                    offsetX += dx / zoomFactor;
                    offsetY += dy / zoomFactor;
                }

                lastMousePosition = e.getPoint();
                repaint();
            }
        }
    });

    addMouseWheelListener(e -> {
        if (e.getWheelRotation() < 0) {
            zoomFactor = Math.min(3.0, zoomFactor * 1.1);
        } else {
            zoomFactor = Math.max(0.3, zoomFactor / 1.1);
        }
        repaint();
    });
}
    /**
     * FITUR BARU: Menampilkan menu konteks jika klik kanan dilakukan di area kosong.
     */
    private void showContextMenu(MouseEvent e) {
        try {
            // Konversi titik klik layar ke koordinat dunia (memperhitungkan zoom/pan)
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            AffineTransform tx = new AffineTransform();
            tx.translate(cx, cy);
            tx.scale(zoomFactor, zoomFactor);
            tx.translate(-cx + offsetX, -cy + offsetY);

            contextMenuClickPoint = new Point2D.Double();
            tx.inverseTransform(e.getPoint(), contextMenuClickPoint);

            // Hanya tampilkan menu jika tidak ada node yang diklik
            if (findNodeAtPoint(contextMenuClickPoint) == null) {
                contextMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        } catch (java.awt.geom.NoninvertibleTransformException ex) {
            // Abaikan jika transformasi tidak bisa dibalik
        }
    }

    /**
     * FITUR BARU: Helper untuk mencari node pada titik koordinat dunia.
     */
    private GraphNode findNodeAtPoint(Point2D worldPoint) {
        double clickTolerance = 18.0;
        for (GraphNode node : graphNodes) {
            // Titik tengah node (ditambah 4 karena di paintComponent digambar dengan offset +4)
            double dx = worldPoint.getX() - (node.x + 4);
            double dy = worldPoint.getY() - (node.y + 4);
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance <= clickTolerance) {
                return node;
            }
        }
        return null;
    }
private GraphNode cariNodeLama(String id) {
    if (graphNodes == null || id == null) {
        return null;
    }
    
    for (GraphNode node : graphNodes) {
        if (node.id != null && node.id.equals(id)) {
            return node; // Node lama ditemukan
        }
    }
    return null; // Node belum ada, nanti akan dibuat baru
}

private Color dapatkanWarnaKategori(String kategori) {
    // 1. Jika kategorinya kosong/null, berikan warna abu-abu netral
    if (kategori == null || kategori.trim().isEmpty()) {
        return new Color(140, 140, 150);
    }
    
    // 2. Lakukan Hashing dari teks kategori agar teks yang sama menghasilkan warna yang sama
    int hash = kategori.toLowerCase().trim().hashCode();
    
    // 3. Modulo nilai hash agar menghasilkan rentang warna RGB cerah (rentang 100 - 250)
    int r = Math.abs((hash & 0xFF0000) >> 16) % 150 + 100;
    int g = Math.abs((hash & 0x00FF00) >> 8) % 150 + 100;
    int b = Math.abs(hash & 0x0000FF) % 150 + 100;
    
    return new Color(r, g, b);
}
}