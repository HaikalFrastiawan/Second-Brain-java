package view;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JButton;

/**
 * JButton kustom dengan sudut melengkung dan efek hover.
 * Tombol ini digambar secara manual untuk kontrol visual penuh.
 */
public class RoundedButton extends JButton {

    private Color backgroundColor;
    private Color hoverColor;
    private boolean isHovering = false;

    public RoundedButton(String text) {
        super(text);
        setOpaque(false);
        setContentAreaFilled(false); // Penting agar background default tidak digambar
        setBorderPainted(false);     // Hilangkan border default
        setFocusPainted(false);
        setForeground(Color.WHITE);
        setFont(new Font("Segoe UI", Font.BOLD, 12));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovering = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovering = false;
                repaint();
            }
        });
    }

    @Override
    public void setBackground(Color bg) {
        this.backgroundColor = bg;
        // Buat warna hover sedikit lebih terang dari warna dasar
        this.hoverColor = bg.brighter();
        super.setBackground(bg);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Pilih warna berdasarkan state hover
        Color color = isHovering ? hoverColor : backgroundColor;
        if (color != null) {
            g2.setColor(color);
        } else {
            g2.setColor(Color.GRAY); // Fallback
        }

        // Gambar bentuk persegi panjang dengan sudut melengkung
        // Radius 10px untuk lengkungan yang halus
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));

        // Gambar teks tombol di tengah
        FontMetrics fm = g2.getFontMetrics();
        Rectangle stringBounds = fm.getStringBounds(this.getText(), g2).getBounds();
        int textX = (getWidth() - stringBounds.width) / 2;
        int textY = (getHeight() - stringBounds.height) / 2 + fm.getAscent();
        g2.setColor(getForeground());
        g2.drawString(getText(), textX, textY);

        g2.dispose();
    }
}