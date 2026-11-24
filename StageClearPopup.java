import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

// 스테이지 클리어 창: 스테이지 맵 위에 겹쳐 떴다가 약 2초 뒤 저절로 사라짐 (클릭·아무 키나 누르면 바로 닫힘)
// 창의 glassPane으로 띄워서, 떠 있는 동안에는 뒤의 맵 버튼이 눌리지 않음
public class StageClearPopup extends JComponent {
    private static final int APPEAR_MS = 250;   // 커지며 나타남
    private static final int FADE_MS = 350;     // 흐려지며 사라짐
    private static final int TOTAL_MS = 2000;
    private static final Color BORDER_COLOR = new Color(0, 200, 190);

    private final int stageNo;
    private final int stars;
    private final int unlockedStage;   // 이번에 새로 열린 스테이지 (없으면 0)
    private final String rewardText;   // 아이템 보상 문구 (없으면 null)
    private final long start = System.currentTimeMillis();
    private final Timer anim;
    private boolean closed;

    // root 창 위에 클리어 창을 띄움
    public static StageClearPopup show(JRootPane root, int stageNo, int stars, int unlockedStage) {
        return show(root, stageNo, stars, unlockedStage, null);
    }

    // 아이템 보상 문구(null이면 없음)를 한 줄 더 그리는 버전
    public static StageClearPopup show(JRootPane root, int stageNo, int stars, int unlockedStage, String rewardText) {
        StageClearPopup p = new StageClearPopup(stageNo, stars, unlockedStage, rewardText);
        root.setGlassPane(p);
        p.setVisible(true);
        p.requestFocusInWindow();
        return p;
    }

    private StageClearPopup(int stageNo, int stars, int unlockedStage, String rewardText) {
        this.rewardText = rewardText;
        this.stageNo = stageNo;
        this.stars = stars;
        this.unlockedStage = unlockedStage;
        setOpaque(false);
        setFocusable(true);
        // 마우스 리스너가 있어야 glassPane이 클릭을 가로채서 뒤의 버튼이 안 눌림
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { close(); }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) { close(); }
        });
        anim = new Timer(16, e -> {
            if (elapsed() >= TOTAL_MS) close();
            else repaint();
        });
        anim.start();
    }

    public boolean isClosed() { return closed; }

    public void close() {
        if (closed) return;
        closed = true;
        anim.stop();
        setVisible(false);
    }

    private long elapsed() { return System.currentTimeMillis() - start; }

    @Override
    protected void paintComponent(Graphics g) {
        long t = elapsed();
        float alpha;
        double scale;
        if (t < APPEAR_MS) {
            double k = t / (double) APPEAR_MS;
            alpha = (float) k;
            scale = 0.6 + 0.4 * (1 - Math.pow(1 - k, 3)) + 0.06 * Math.sin(Math.PI * k);   // 살짝 튀어나오듯
        } else if (t > TOTAL_MS - FADE_MS) {
            alpha = Math.max(0f, (TOTAL_MS - t) / (float) FADE_MS);
            scale = 1.0;
        } else {
            alpha = 1f;
            scale = 1.0;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 뒤의 맵을 어둡게
        g2.setColor(new Color(0, 0, 0, (int) (150 * alpha)));
        g2.fillRect(0, 0, getWidth(), getHeight());

        int w = rewardText != null ? 540 : 440;
        int h = (unlockedStage > 0 ? 250 : 210) + (rewardText != null ? 40 : 0);
        double cx = getWidth() / 2.0, cy = getHeight() / 2.0;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.translate(cx, cy);
        g2.scale(scale, scale);

        RoundRectangle2D card = new RoundRectangle2D.Double(-w / 2.0, -h / 2.0, w, h, 24, 24);
        g2.setColor(new Color(20, 25, 45));
        g2.fill(card);
        g2.setStroke(new BasicStroke(3));
        g2.setColor(BORDER_COLOR);
        g2.draw(card);

        int y = -h / 2 + 70;
        drawCentered(g2, "STAGE " + stageNo + " CLEAR!", new Font("맑은 고딕", Font.BOLD, 40), Color.YELLOW, y);

        // 별: 얻은 별은 금색, 나머지는 회색
        y += 70;
        Font starFont = new Font("Dialog", Font.BOLD, 44);
        g2.setFont(starFont);
        FontMetrics fm = g2.getFontMetrics();
        int gap = 12, sw = fm.stringWidth("★");
        int x = -(sw * 3 + gap * 2) / 2;
        for (int i = 1; i <= 3; i++) {
            g2.setColor(i <= stars ? new Color(255, 215, 0) : new Color(85, 85, 85));
            g2.drawString(i <= stars ? "★" : "☆", x, y);
            x += sw + gap;
        }

        if (unlockedStage > 0) {
            drawCentered(g2, "STAGE " + unlockedStage + " 열림!", new Font("맑은 고딕", Font.BOLD, 22), new Color(100, 255, 150), y + 55);
            y += 40;
        }
        if (rewardText != null) {
            drawCentered(g2, rewardText, new Font("맑은 고딕", Font.BOLD, 15), new Color(255, 215, 0), y + 55);
        }
        g2.dispose();
    }

    private void drawCentered(Graphics2D g2, String text, Font font, Color color, int y) {
        g2.setFont(font);
        g2.setColor(color);
        g2.drawString(text, -g2.getFontMetrics().stringWidth(text) / 2, y);
    }
}
