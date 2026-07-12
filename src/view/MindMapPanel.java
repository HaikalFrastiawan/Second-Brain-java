package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * MindMapPanel adalah sebuah JPanel kustom yang dirancang untuk menampilkan
 * dan berinteraksi dengan grafik node (mind map) secara dinamis dan interaktif.
 * Fitur utamanya meliputi:
 * - Mesin fisika sederhana untuk simulasi pergerakan node (force-directed).
 * - Interaksi mouse: hover, drag-and-drop dengan efek lempar, dan double-click.
 * - Rendering visual yang modern dan futuristik dengan efek glow dan antialiasing.
 */
public class MindMapPanel extends JPanel {

    // --- STRUKTUR DATA INTERNAL ---
    private final List<Node> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();

    // --- KONTROL INTERAKSI ---
    private Node hoveredNode = null;
    private Node draggedNode = null;
    private Point lastMouseDragPoint;

    // --- KONSTANTA FISIKA & VISUAL ---
    private static final double REPEL_FORCE = 60.0;
    private static final double SPRING_FORCE = 0.01;
    private static final double DAMPING = 0.85; // Redaman untuk efek pegas elastis
    private static final int HOVER_PROXIMITY = 25; // Jarak piksel untuk deteksi hover

    /**
     * Inner class untuk merepresentasikan sebuah Node (titik) dalam grafik.
     */
    private static class Node {
        int id;
        String label;
        double x, y, vx, vy;
        int baseRadius;
        double currentRadius;
        Color color;
        boolean isFixed = false; // Node yang sedang di-drag tidak terpengaruh fisika

        Node(int id, String label, double x, double y, int radius, Color color) {
            this.id = id;
            this.label = label;
            this.x = x;
            this.y = y;
            this.baseRadius = radius;
            this.currentRadius = radius;
            this.color = color;
            this.vx = 0;
            this.vy = 0;
        }
    }

    /**
     * Inner class untuk merepresentasikan sebuah Edge (garis penghubung) antar Node.
     */
    private static class Edge {
        final Node source;
        final Node target;

        Edge(Node source, Node target) {
            this.source = source;
            this.target = target;
        }
    }

    public MindMapPanel() {
        // 1. Inisialisasi Panel
        setBackground(new Color(0x0B, 0x0F, 0x19)); // Warna gelap pekat #0B0F19
        
        // 2. Buat Data Mockup
        createMockData();

        // 3. Inisialisasi Listener untuk Interaksi Mouse
        addMouseListeners();

        // 4. Jalankan Timer untuk Animasi & Physics Engine
        // Timer berjalan setiap 16ms untuk mencapai ~60 FPS
        new Timer(16, e -> {
            updatePhysics();
            updateHoverState(getMousePosition());
            repaint();
        }).start();
    }

    private void createMockData() {
        Random rand = new Random();
        Node node1 = new Node(1, "OOP - ABSTRACK", rand.nextInt(600) + 100, rand.nextInt(400) + 100, 12, new Color(0, 255, 200));
        Node node2 = new Node(2, "Inheritance", rand.nextInt(600) + 100, rand.nextInt(400) + 100, 10, new Color(0, 255, 200));
        Node node3 = new Node(3, "Polymorphism", rand.nextInt(600) + 100, rand.nextInt(400) + 100, 10, new Color(0, 255, 200));
        Node node4 = new Node(4, "Rest day", rand.nextInt(600) + 100, rand.nextInt(400) + 100, 15, new Color(255, 100, 180));
        Node node5 = new Node(5, "Encapsulation", rand.nextInt(600) + 100, rand.nextInt(400) + 100, 10, new Color(0, 255, 200));

        nodes.add(node1);
        nodes.add(node2);
        nodes.add(node3);
        nodes.add(node4);
        nodes.add(node5);

        edges.add(new Edge(node1, node2));
        edges.add(new Edge(node1, node3));
        edges.add(new Edge(node1, node5));
    }

    /**
     * Mesin fisika sederhana untuk mengatur pergerakan node.
     */
    private void updatePhysics() {
        // 1. Terapkan gaya tarik-menarik (pegas) hanya pada node yang terhubung
        for (Edge edge : edges) {
            applySpringForce(edge.source, edge.target);
        }

        // 2. Terapkan gaya tolak-menolak antar semua pasangan node
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                Node n1 = nodes.get(i);
                Node n2 = nodes.get(j);

                double dx = n2.x - n1.x;
                double dy = n2.y - n1.y;
                double distance = Math.sqrt(dx * dx + dy * dy);

                // Jika jarak terlalu dekat, berikan gaya tolak
                if (distance < 150) { // Jarak minimal antar node
                    // Hindari pembagian dengan nol jika node bertumpuk
                    if (distance == 0) {
                        dx = (Math.random() - 0.5);
                        dy = (Math.random() - 0.5);
                        distance = Math.sqrt(dx*dx + dy*dy);
                    }
                    double force = -REPEL_FORCE / (distance * distance);
                    double forceX = force * dx / distance;
                    double forceY = force * dy / distance;

                    n1.vx += forceX;
                    n1.vy += forceY;
                    n2.vx -= forceX; // Terapkan gaya berlawanan pada node kedua
                    n2.vy -= forceY; // Terapkan gaya berlawanan pada node kedua
                }
            }

        }

        // 3. Update posisi semua node berdasarkan akumulasi gaya
        for (Node n1 : nodes) {
            // Terapkan redaman (damping/friction) agar gerakan melambat
            n1.vx *= DAMPING;
            n1.vy *= DAMPING;

            // Update posisi berdasarkan kecepatan
            n1.x += n1.vx;
            n1.y += n1.vy;

            // Jaga agar node tidak keluar dari batas panel (Boundary Checking)
            n1.x = Math.max(n1.currentRadius, Math.min(getWidth() - n1.currentRadius, n1.x));
            n1.y = Math.max(n1.currentRadius, Math.min(getHeight() - n1.currentRadius, n1.y));
        }
    }

    private void applySpringForce(Node n1, Node n2) {
        if (n1.isFixed && n2.isFixed) return;

        double dx = n2.x - n1.x;
        double dy = n2.y - n1.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance == 0) return;

        double force = SPRING_FORCE * (distance - 200); // 200 adalah panjang ideal pegas

        if (!n1.isFixed) {
            n1.vx += force * dx / distance;
            n1.vy += force * dy / distance;
        }
        if (!n2.isFixed) {
            n2.vx -= force * dx / distance;
            n2.vy -= force * dy / distance;
        }
    }

    private void updateHoverState(Point mousePos) {
        if (mousePos == null) {
            hoveredNode = null;
        } else {
            hoveredNode = findNodeAt(mousePos.x, mousePos.y, HOVER_PROXIMITY);
        }

        // Animasi pembesaran/pengecilan radius saat hover
        for (Node node : nodes) {
            double targetRadius = (node == hoveredNode) ? node.baseRadius * 1.5 : node.baseRadius;
            // Interpolasi linear untuk transisi yang mulus
            node.currentRadius += (targetRadius - node.currentRadius) * 0.2;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Aktifkan Antialiasing untuk rendering yang super halus
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawGrid(g2d);

        g2d.setStroke(new BasicStroke(1.5f));
        g2d.setColor(new Color(0, 150, 255, 70));
        for (Edge edge : edges) {
            g2d.drawLine((int) edge.source.x, (int) edge.source.y, (int) edge.target.x, (int) edge.target.y);
        }

        for (Node node : nodes) {
            drawNode(g2d, node);
        }
    }

    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(new Color(40, 45, 60, 50));
        int gridSize = 50;
        for (int x = 0; x < getWidth(); x += gridSize) {
            g2d.drawLine(x, 0, x, getHeight());
        }
        for (int y = 0; y < getHeight(); y += gridSize) {
            g2d.drawLine(0, y, getWidth(), y);
        }
    }

    private void drawNode(Graphics2D g2d, Node node) {
        int x = (int) node.x;
        int y = (int) node.y;
        int r = (int) node.currentRadius;

        Color baseColor = (node == hoveredNode) ? Color.WHITE : node.color;
        Color glowColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 0);
        
        Point2D center = new Point2D.Float(x, y);
        float[] dist = {0.0f, 1.0f};
        Color[] colors = {baseColor, glowColor};
        RadialGradientPaint p = new RadialGradientPaint(center, r + 10, dist, colors);
        
        g2d.setPaint(p);
        g2d.fillOval(x - (r + 10), y - (r + 10), (r + 10) * 2, (r + 10) * 2);

        g2d.setColor(baseColor);
        g2d.fillOval(x - r, y - r, r * 2, r * 2);

        g2d.setColor(new Color(200, 210, 220));
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 12));
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(node.label, x - fm.stringWidth(node.label) / 2, y + r + fm.getAscent());
    }

    private Node findNodeAt(int x, int y, double tolerance) {
        for (Node node : nodes) {
            if (Point2D.distance(x, y, node.x, node.y) < node.currentRadius + tolerance) {
                return node;
            }
        }
        return null;
    }

    private void addMouseListeners() {
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                draggedNode = findNodeAt(e.getX(), e.getY(), 5);
                if (draggedNode != null) {
                    draggedNode.isFixed = true;
                    lastMouseDragPoint = e.getPoint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggedNode != null) {
                    draggedNode.isFixed = false;
                    draggedNode = null;
                }
                lastMouseDragPoint = null;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Node clickedNode = findNodeAt(e.getX(), e.getY(), 5);
                    if (clickedNode != null) {
                        System.out.println("Node double-clicked: " + clickedNode.label);
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggedNode != null && lastMouseDragPoint != null) {
                    double dx = e.getX() - lastMouseDragPoint.x;
                    double dy = e.getY() - lastMouseDragPoint.y;

                    draggedNode.x = e.getX();
                    draggedNode.y = e.getY();

                    draggedNode.vx = dx;
                    draggedNode.vy = dy;

                    lastMouseDragPoint = e.getPoint();
                    repaint();
                }
            }
        };
        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Dynamic Mind-Mapping Panel");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new MindMapPanel());
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}