package view;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import model.Catatan;
import repository.CatatanRepository;

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

    // Constructor bawaan (urutan baru)
    public GraphPanel(CatatanRepository repo, MainFrame mainFrame) {
        this.repo = repo;
        this.mainFrame = mainFrame;
        setBackground(new Color(10, 10, 18));
        initControls();
        initMouseListeners();
        initGraphEngineTimer();
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

    // Overload Constructor untuk mendukung urutan pemanggilan di MainFrame lama Anda
    public GraphPanel(MainFrame mainFrame, CatatanRepository repo) {
        this(repo, mainFrame);
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
        GraphNode lama = cariNodeLama(c.getId());
        if (lama != null) {
            lama.title = c.getJudul();
            lama.color = dapatkanWarnaKategori(c.getKategori());
            nodesBaru.add(lama);
        } else {
            // Perbaikan: Gunakan sudut (angle) acak yang distribusinya lebih luas agar memencar
            double angle = Math.random() * 2.0 * Math.PI;
            double radius = 100 + (Math.random() * 200); // Radius lingkaran sebaran diperluas
            double nx = cx + Math.cos(angle) * radius;
            double ny = cy + Math.sin(angle) * radius;
            
            nodesBaru.add(new GraphNode(c.getId(), c.getJudul(), nx, ny, dapatkanWarnaKategori(c.getKategori())));
        }
    }
    this.graphNodes = nodesBaru;
    repaint();
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

        // 1. Loop Hitung Gaya Fisika Dinamis (Tolak-menolak antar node)
        for (int i = 0; i < graphNodes.size(); i++) {
            GraphNode n1 = graphNodes.get(i);
            for (int j = 0; j < graphNodes.size(); j++) {
                if (i == j) {
                    continue;
                }
                GraphNode n2 = graphNodes.get(j);
                double dx = n1.x - n2.x;
                double dy = n1.y - n2.y;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist == 0) {
                    dist = 1;
                }

                // Jarak tolak diperkecil sedikit agar tidak mental terlalu ekstrem saat berdekatan
                if (dist < 180) {
                    double force = REPEL_FORCE / (dist * dist);
                    n1.vx += (dx / dist) * force;
                    n1.vy += (dy / dist) * force;
                }
            }

            // 2. Gaya Tarik Pegas (Untuk Node dalam Kategori yang Sama)
            for (int j = i + 1; j < graphNodes.size(); j++) {
                GraphNode n2 = graphNodes.get(j);
                if (n1.color.getRGB() == n2.color.getRGB()) {
                    double dx = n1.x - n2.x;
                    double dy = n1.y - n2.y;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist <= MAX_LINE_DIST) {
                        double force = SPRING_FORCE * (dist - 100);
                        n1.vx -= (dx / dist) * force;
                        n1.vy -= (dy / dist) * force;
                        n2.vx += (dx / dist) * force;
                        n2.vy += (dy / dist) * force;
                    }
                }
            }
        }

        // 3. Update Posisi Node & Batasi Kecepatan (Damping Eksplosif Dimatikan)
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

        Map<String, ArrayList<GraphNode>> categoryGroups = new HashMap<>();
        List<Catatan> semuaCatatan = repo.getAllCatatan();
        Map<String, String> idToCategoryMap = new HashMap<>();
        Map<String, String> idToTitleMap = new HashMap<>();

        for (Catatan c : semuaCatatan) {
            String kat = (c.getKategori() == null || c.getKategori().trim().isEmpty()) ? "Uncategorized" : c.getKategori();
            idToCategoryMap.put(c.getId(), kat);
            idToTitleMap.put(c.getId(), c.getJudul().toLowerCase());
        }

        for (GraphNode node : graphNodes) {
            String cat = idToCategoryMap.getOrDefault(node.id, "Uncategorized");
            categoryGroups.putIfAbsent(cat, new ArrayList<>());
            categoryGroups.get(cat).add(node);
        }

        String selectedId = mainFrame.getSelectedId();
        String selectedCategory = (selectedId != null) ? idToCategoryMap.get(selectedId) : null;

        int MAX_LINE_DIST = 350;
        if (selectedId != null && selectedCategory != null) {
            ArrayList<GraphNode> nodesInSelectedGroup = categoryGroups.get(selectedCategory);
            if (nodesInSelectedGroup != null) {
                for (int i = 0; i < nodesInSelectedGroup.size(); i++) {
                    for (int j = i + 1; j < nodesInSelectedGroup.size(); j++) {
                        GraphNode n1 = nodesInSelectedGroup.get(i);
                        GraphNode n2 = nodesInSelectedGroup.get(j);
                        double d = Math.sqrt(Math.pow(n1.x - n2.x, 2) + Math.pow(n1.y - n2.y, 2));
                        if (d <= MAX_LINE_DIST) {
                            g2d.setColor(new Color(0, 215, 255, 175));
                            g2d.setStroke(new BasicStroke(1.5f));
                            g2d.drawLine((int) n1.x + 4, (int) n1.y + 4, (int) n2.x + 4, (int) n2.y + 4);
                            int pulseX = (int) (n1.x + 4 + (n2.x - n1.x) * pulseTracker);
                            int pulseY = (int) (n1.y + 4 + (n2.y - n1.y) * pulseTracker);
                            g2d.setColor(Color.WHITE);
                            g2d.fillOval(pulseX - 2, pulseY - 2, 4, 4);
                        }
                    }
                }
            }
        }

        for (GraphNode node : graphNodes) {
            int nx = (int) node.x;
            int ny = (int) node.y;
            boolean isSelected = selectedId != null && selectedId.equals(node.id);
            String currentCategory = idToCategoryMap.getOrDefault(node.id, "Uncategorized");
            String lowerTitle = idToTitleMap.getOrDefault(node.id, "");
            boolean matchesSearch = searchQuery.isEmpty()
                    || lowerTitle.contains(searchQuery)
                    || currentCategory.toLowerCase().contains(searchQuery);
            int baseGroupSize = categoryGroups.getOrDefault(currentCategory, new ArrayList<>()).size();
            int nodeDiameter = 8 + Math.min(10, baseGroupSize / 2);
            int glowDiameter = nodeDiameter + 12;
            int alphaGlow = 35, alphaCore = 220, alphaText = 165;

            if (!matchesSearch) {
                alphaGlow = 2;
                alphaCore = 25;
                alphaText = 15;
            } else if (selectedId != null) {
                if (!currentCategory.equals(selectedCategory) && !isSelected) {
                    alphaGlow = 3;
                    alphaCore = 40;
                    alphaText = 30;
                } else if (!isSelected) {
                    alphaGlow = 75;
                    alphaCore = 255;
                    alphaText = 225;
                }
            }

            if (isSelected) {
                g2d.setColor(new Color(255, 0, 100, 110));
                g2d.fillOval(nx - (glowDiameter / 2) + 4, ny - (glowDiameter / 2) + 4, glowDiameter, glowDiameter);
                g2d.setColor(Color.WHITE);
                g2d.fillOval(nx - (nodeDiameter / 2) + 4, ny - (nodeDiameter / 2) + 4, nodeDiameter, nodeDiameter);
            } else {
                g2d.setColor(new Color(node.color.getRed(), node.color.getGreen(), node.color.getBlue(), alphaGlow));
                g2d.fillOval(nx - (glowDiameter / 2) + 4, ny - (glowDiameter / 2) + 4, glowDiameter, glowDiameter);
                g2d.setColor(new Color(node.color.getRed(), node.color.getGreen(), node.color.getBlue(), alphaCore));
                g2d.fillOval(nx - (nodeDiameter / 2) + 4, ny - (nodeDiameter / 2) + 4, nodeDiameter, nodeDiameter);
            }
            if (isSelected) {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
            } else {
                g2d.setColor(new Color(195, 195, 215, alphaText));
                g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));
            }
            g2d.drawString(node.title, nx - 15, ny + nodeDiameter + 10);
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

    private void initMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMousePosition = e.getPoint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;

                // 1. Buat ulang matriks transformasi yang persis sama dengan yang ada di paintComponent
                java.awt.geom.AffineTransform tx = new java.awt.geom.AffineTransform();
                tx.translate(cx, cy);
                tx.scale(zoomFactor, zoomFactor);
                tx.translate(-cx + offsetX, -cy + offsetY);

                java.awt.Point srcTransform = e.getPoint();
                java.awt.geom.Point2D dstTransform = new java.awt.geom.Point2D.Double();

                try {
                    // 2. Minta Java menghitung balik koordinat klik layar ke koordinat dunia nyata si node
                    tx.inverseTransform(srcTransform, dstTransform);

                    double clickedX = dstTransform.getX();
                    double clickedY = dstTransform.getY();

                    GraphNode nodeTerpilih = null;
                    // Toleransi area klik lingkaran node (radius 18px)
                    double klikToleransi = 18.0;

                    for (GraphNode node : graphNodes) {
                        // Titik tengah node (ditambah 4 karena di paintComponent digambar dengan offset +4)
                        double dx = clickedX - (node.x + 4);
                        double dy = clickedY - (node.y + 4);
                        double distance = Math.sqrt(dx * dx + dy * dy);

                        if (distance <= klikToleransi) {
                            nodeTerpilih = node;
                            break;
                        }
                    }

                    if (nodeTerpilih != null) {
                        // Lempar ID node yang diklik ke MainFrame agar tabel & form terisi, lalu node menyala
                        mainFrame.setSelectedId(nodeTerpilih.id);
                    } else {
                        // Klik di area kosong, lepas seleksi
                        mainFrame.setSelectedId(null);
                    }

                    // Paksa gambar ulang layar agar efek menyala langsung aktif
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
                    offsetX += dx / zoomFactor;
                    offsetY += dy / zoomFactor;
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
