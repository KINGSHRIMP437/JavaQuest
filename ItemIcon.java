import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

// 아이템 아이콘 (이미지 파일 없이 선으로 그림): 무적 = 금색 방패, 체력 완충 = 빨간 하트+, 문제 개수 초기화 = 민트 되돌리기 화살표
public class ItemIcon implements Icon {
    public static final Color[] COLORS = {
        new Color(255, 215, 0), new Color(255, 107, 107), new Color(0, 200, 190)
    };

    private final int type, size;
    private boolean dim;   // 개수 0이면 흐리게

    public ItemIcon(int type, int size) { this.type = type; this.size = size; }

    public void setDim(boolean dim) { this.dim = dim; }

    @Override public int getIconWidth() { return size; }
    @Override public int getIconHeight() { return size; }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        if (dim) g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        draw(g2, type, x, y, size);
        g2.dispose();
    }

    // (x, y)에서 s×s 크기로 아이콘 선을 그림
    public static void draw(Graphics2D g2, int type, double x, double y, double s) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(COLORS[type]);
        g2.translate(x, y);
        g2.scale(s / 24.0, s / 24.0);   // 아래 좌표는 24×24 기준
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (type == Inventory.INVINCIBLE) {
            Path2D p = new Path2D.Double();   // 방패
            p.moveTo(12, 3);
            p.curveTo(15, 5, 18, 5.5, 20, 5.5);
            p.curveTo(20.5, 13, 17.5, 18.5, 12, 21);
            p.curveTo(6.5, 18.5, 3.5, 13, 4, 5.5);
            p.curveTo(6, 5.5, 9, 5, 12, 3);
            p.closePath();
            g2.draw(p);
        } else if (type == Inventory.HEAL_FULL) {
            Path2D p = new Path2D.Double();   // 하트 (오른쪽 아래는 + 자리라 열어 둠)
            p.moveTo(13.5, 19.5);
            p.lineTo(12, 21);
            p.curveTo(7, 16.5, 3, 13, 3, 8.5);
            p.curveTo(3, 5.5, 5.3, 3.5, 8, 3.5);
            p.curveTo(9.8, 3.5, 11.2, 4.5, 12, 6);
            p.curveTo(12.8, 4.5, 14.2, 3.5, 16, 3.5);
            p.curveTo(18.7, 3.5, 21, 5.5, 21, 8.5);
            p.curveTo(21, 9.8, 20.7, 11, 20, 12);
            g2.draw(p);
            g2.draw(new Line2D.Double(15, 18, 21, 18));   // +
            g2.draw(new Line2D.Double(18, 15, 18, 21));
        } else {
            // 되돌리기: 위·아래 두 호와 끝의 화살촉
            g2.draw(new Arc2D.Double(4, 4, 16, 16, 20, 150, Arc2D.OPEN));
            g2.draw(new Arc2D.Double(4, 4, 16, 16, 200, 150, Arc2D.OPEN));
            g2.draw(new Line2D.Double(19.5, 9.3, 19.6, 4.6));
            g2.draw(new Line2D.Double(19.5, 9.3, 15, 9));
            g2.draw(new Line2D.Double(4.5, 14.7, 4.4, 19.4));
            g2.draw(new Line2D.Double(4.5, 14.7, 9, 15));
        }
    }

    // 하트 줄 오른쪽 아이템 칸: 테두리 상자 + 아이콘 + 왼쪽 위 숫자 키 + 오른쪽 아래 개수(무적 중이면 남은 초)
    public static class Slot extends JComponent {
        private static final Color MINT = new Color(0, 200, 190);
        private static final Color LINE = new Color(85, 92, 117);
        private static final Color BOX = new Color(25, 30, 50);
        private static final Color BG = new Color(20, 26, 46);
        private final int type;
        private String count = "1";
        private boolean dim, active;

        public Slot(int type) {
            this.type = type;
            setPreferredSize(new Dimension(60, 58));
        }

        // count: 오른쪽 아래 글자, active: 무적 사용 중(금색 테두리)
        public void set(String count, boolean dim, boolean active) {
            this.count = count; this.dim = dim; this.active = active;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int bx = 8, by = 6, bs = 44;
            Color edge = active ? COLORS[Inventory.INVINCIBLE] : LINE;
            g2.setColor(BOX);
            g2.fillRoundRect(bx, by, bs, bs, 10, 10);
            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(edge);
            g2.drawRoundRect(bx, by, bs, bs, 10, 10);

            Graphics2D ig = (Graphics2D) g2.create();
            if (dim) ig.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            draw(ig, type, bx + 10, by + 10, 24);
            ig.dispose();

            // 숫자 키 배지 (민트 원)
            g2.setColor(MINT);
            g2.fillOval(1, 0, 22, 22);
            g2.setColor(new Color(4, 52, 44));
            g2.setFont(new Font("맑은 고딕", Font.BOLD, 14));
            centerText(g2, String.valueOf(type + 1), 12, 11);

            // 개수 배지
            g2.setFont(new Font("맑은 고딕", Font.BOLD, 14));
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(count) + 10, h = 20;
            int cx = bx + bs - w + 6, cy = by + bs - h + 6;
            g2.setColor(BG);
            g2.fillRoundRect(cx, cy, w, h, 8, 8);
            g2.setColor(edge);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(cx, cy, w, h, 8, 8);
            g2.setColor(active ? COLORS[Inventory.INVINCIBLE] : Color.WHITE);
            centerText(g2, count, cx + w / 2.0, cy + h / 2.0);
            g2.dispose();
        }

        private static void centerText(Graphics2D g2, String s, double cx, double cy) {
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(s, (float) (cx - fm.stringWidth(s) / 2.0), (float) (cy + (fm.getAscent() - fm.getDescent()) / 2.0));
        }
    }
}
