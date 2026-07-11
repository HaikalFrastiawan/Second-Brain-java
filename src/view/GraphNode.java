package view;

import java.awt.Color;

// File Blueprint struktur data rasi bintang untuk sistem fisika/rendering GraphPanel
public class GraphNode {
    public String id;
    public String title;
    public double x, y;   // Posisi koordinat saat ini
    public double vx, vy; // Kecepatan (Velocity) untuk simulasi gerakan fisika elastis
    public Color color;

    public GraphNode(String id, String title, double x, double y, Color color) {
        this.id = id;
        this.title = title;
        this.x = x;
        this.y = y;
        this.vx = 0;
        this.vy = 0;
        this.color = color;
    }
}