import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

// 던전 탈출 엔딩: 모든 주제(코딩·상식·이름 맞추기)의 STAGE 1~3을 다 깨면 주제 클리어 화면 다음에 나옴
// 1050x800 기준으로 그리고, 창 크기에 맞춰 비율을 유지한 채 확대·축소해 가운데 배치
public class EscapeScreen extends JPanel {
    private static final int BASE_W = 1050, BASE_H = 800;
    private static final Color CARD = new Color(25, 30, 50);
    private static final Color MINT = new Color(0, 200, 190);
    private static final Color GOLD = new Color(255, 215, 90);
    private static final String F = "맑은 고딕";
    private static final int BTN_Y = 695, BTN_W = 220, BTN_H = 56;

    // 주제별 마지막 보스 (그림 경로, 이름)
    private static final String[][] BOSSES = {
        {"images/boss/coding_3.png", "코딩"},
        {"images/boss/common_3.png", "상식"},
        {"images/boss/name_3.png", "이름 맞추기"},
    };

    private final JButton topicBtn = makeButton("주제 선택으로", Color.YELLOW);
    private final JButton titleBtn = makeButton("타이틀로", Color.WHITE);
    private final BufferedImage[] bossImages = new BufferedImage[BOSSES.length];

    public EscapeScreen(Runnable onToTopicSelect, Runnable onToTitle) {
        setBackground(new Color(15, 20, 35));
        setLayout(null);
        for (int i = 0; i < BOSSES.length; i++) {
            try { bossImages[i] = ImageIO.read(new File(BOSSES[i][0])); } catch (Exception ignored) {}
        }
        topicBtn.addActionListener(e -> onToTopicSelect.run());
        titleBtn.addActionListener(e -> onToTitle.run());
        add(topicBtn);
        add(titleBtn);
    }

    private static JButton makeButton(String text, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(CARD);
        b.setForeground(fg);
        b.setBorder(new LineBorder(MINT, 2));
        b.setFocusPainted(false);
        return b;
    }

    private double scale() { return Math.min(getWidth() / (double) BASE_W, getHeight() / (double) BASE_H); }
    private int offX() { return (int) Math.round((getWidth() - BASE_W * scale()) / 2); }
    private int offY() { return (int) Math.round((getHeight() - BASE_H * scale()) / 2); }

    @Override
    public void doLayout() {
        double s = scale();
        int[] xs = {BASE_W / 2 - BTN_W - 12, BASE_W / 2 + 12};
        JButton[] bs = {topicBtn, titleBtn};
        for (int i = 0; i < 2; i++) {
            bs[i].setBounds(offX() + (int) (xs[i] * s), offY() + (int) (BTN_Y * s), (int) (BTN_W * s), (int) (BTN_H * s));
            bs[i].setFont(new Font(F, Font.BOLD, Math.max(12, (int) Math.round(21 * s))));
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // 배경은 창 전체에
        int W = getWidth(), H = getHeight();
        g.setPaint(new GradientPaint(0, 0, new Color(30, 44, 70), 0, H, new Color(8, 10, 20)));
        g.fillRect(0, 0, W, H);
        Random r = new Random(11);
        int n = W * H / 7000;
        for (int i = 0; i < n; i++) {
            g.setColor(new Color(220, 240, 255, 40 + r.nextInt(140)));
            int s = r.nextInt(3) + 1;
            g.fillOval(r.nextInt(Math.max(1, W)), r.nextInt(Math.max(1, H)), s, s);
        }

        g.translate(offX(), offY());
        g.scale(scale(), scale());
        paintContent(g);
        g.dispose();
    }

    private void paintContent(Graphics2D g) {
        int W = BASE_W, H = BASE_H;
        // 열린 출구 문 + 쏟아지는 빛
        int dw = 150, dh = 190, dx = (W - dw) / 2, dy = 40;
        g.setPaint(new RadialGradientPaint(new Point2D.Float(W / 2f, dy + dh / 2f), 420,
                new float[]{0f, 0.35f, 1f},
                new Color[]{new Color(255, 240, 180, 150), new Color(255, 215, 90, 50), new Color(255, 215, 90, 0)}));
        g.fillRect(-W, -H, W * 3, H * 3);
        g.setColor(new Color(255, 235, 170, 28));   // 빛줄기
        for (int i = -3; i <= 3; i++) {
            int bx = W / 2 + i * 26;
            g.fillPolygon(new int[]{bx - 10, bx + 10, bx + i * 120 + 90, bx + i * 120 - 90},
                          new int[]{dy + dh, dy + dh, H, H}, 4);
        }
        g.setColor(new Color(70, 78, 98));          // 돌 문틀
        g.fill(arch(dx - 14, dy - 14, dw + 28, dh + 14));
        g.setPaint(new GradientPaint(0, dy, new Color(255, 250, 225), 0, dy + dh, new Color(255, 215, 120)));
        g.fill(arch(dx, dy, dw, dh));               // 문 안쪽 빛
        int[] doorX = {dx, dx - 46, dx - 46, dx}, doorY = {dy + 30, dy + 52, dy + dh + 6, dy + dh};
        g.setColor(new Color(95, 60, 35));          // 활짝 열린 문짝
        g.fillPolygon(doorX, doorY, 4);
        g.setColor(new Color(60, 38, 22));
        g.setStroke(new BasicStroke(2f));
        g.drawPolygon(doorX, doorY, 4);
        g.setFont(new Font("Dialog", Font.BOLD, 40)); // 문으로 걸어가는 플레이어(하트)
        g.setColor(new Color(255, 80, 110));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("♥", W / 2 - fm.stringWidth("♥") / 2, dy + dh - 14);

        // 제목 + 이야기
        center(g, "ESCAPE CLEAR", new Font(F, Font.BOLD, 18), MINT, 285);
        Font tf = new Font(F, Font.BOLD, 54);
        g.setFont(tf);
        g.setColor(new Color(0, 0, 0, 120));
        g.drawString("탈출 성공!", (W - g.getFontMetrics().stringWidth("탈출 성공!")) / 2 + 3, 351);
        center(g, "탈출 성공!", tf, GOLD, 348);
        Font sf = new Font(F, Font.PLAIN, 21);
        center(g, "마지막 보스가 쓰러지자, 굳게 닫혀 있던 출구가 열렸다.", sf, new Color(225, 230, 240), 400);
        center(g, "탄막을 피하고 퀴즈를 풀며 지나온 길이 빛 속으로 이어진다.", sf, new Color(225, 230, 240), 434);
        center(g, "당신은 마침내 던전에서 탈출했다.", new Font(F, Font.BOLD, 22), Color.WHITE, 474);

        // 쓰러뜨린 보스 (흐리게 + CLEAR 도장)
        int cw = 200, ch = 150, gap = 28, cy = 510;
        int cx0 = (W - (cw * 3 + gap * 2)) / 2;
        for (int i = 0; i < BOSSES.length; i++) {
            int x = cx0 + i * (cw + gap);
            g.setColor(new Color(25, 30, 50, 235));
            g.fillRoundRect(x, cy, cw, ch, 16, 16);
            g.setColor(MINT);
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(x, cy, cw, ch, 16, 16);

            BufferedImage b = bossImages[i];
            if (b != null) {
                double s = Math.min(150.0 / b.getWidth(), 95.0 / b.getHeight());
                int w = (int) (b.getWidth() * s), h = (int) (b.getHeight() * s);
                Composite old = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
                g.drawImage(b, x + (cw - w) / 2, cy + 12, w, h, null);
                g.setComposite(old);
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.rotate(Math.toRadians(-14), x + cw / 2.0, cy + 60);
            g2.setColor(new Color(255, 90, 90));
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(x + cw / 2 - 58, cy + 42, 116, 38, 8, 8);
            g2.setFont(new Font(F, Font.BOLD, 24));
            FontMetrics f2 = g2.getFontMetrics();
            g2.drawString("CLEAR", x + cw / 2 - f2.stringWidth("CLEAR") / 2, cy + 70);
            g2.dispose();

            g.setFont(new Font(F, Font.BOLD, 18));
            g.setColor(Color.WHITE);
            fm = g.getFontMetrics();
            g.drawString(BOSSES[i][1], x + (cw - fm.stringWidth(BOSSES[i][1])) / 2, cy + 132);
        }
    }

    // 위가 둥근 아치 모양
    private static Shape arch(int x, int y, int w, int h) {
        Path2D p = new Path2D.Double();
        p.moveTo(x, y + h);
        p.lineTo(x, y + w / 2.0);
        p.append(new Arc2D.Double(x, y, w, w, 180, -180, Arc2D.OPEN), true);
        p.lineTo(x + w, y + h);
        p.closePath();
        return p;
    }

    private static void center(Graphics2D g, String s, Font f, Color c, int y) {
        g.setFont(f);
        g.setColor(c);
        g.drawString(s, (BASE_W - g.getFontMetrics().stringWidth(s)) / 2, y);
    }
}
