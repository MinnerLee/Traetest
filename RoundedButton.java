import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

public class RoundedButton extends JButton {

    private static final int ARC = 18;
    private Color normalBg, hoverBg, pressBg;
    private boolean hovered = false;
    private boolean pressed = false;

    public RoundedButton(String text, Color normalBg, Color hoverBg, Color pressBg) {
        super(text);
        this.normalBg = normalBg;
        this.hoverBg = hoverBg;
        this.pressBg = pressBg;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
            @Override public void mouseReleased(MouseEvent e){ pressed = false; repaint(); }
        });
    }

    public void setNormalBg(Color n, Color h, Color p) {
        this.normalBg = n;
        this.hoverBg = h;
        this.pressBg = p;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color bg = pressed ? pressBg : (hovered ? hoverBg : normalBg);
        g2.setPaint(bg);
        g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC));
        g2.dispose();
        super.paintComponent(g);
    }
}