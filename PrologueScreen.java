import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

// 프롤로그 (2쪽): 타이틀에서 "게임 시작"을 누르면 나옴
//   1쪽 = 이야기 + 탄막 회피 → 퀴즈 공격 → 보스 격파,  2쪽 = 아이템 설명
// 1050x800 기준으로 그리고, 창 크기에 맞춰 비율을 유지한 채 확대·축소해 가운데 배치
public class PrologueScreen extends JPanel {
    static final int BASE_W = 1050, BASE_H = 800;
    static final Color BG = new Color(15, 20, 35);
    static final Color CARD = new Color(25, 30, 50);
    static final Color MINT = new Color(0, 200, 190);
    static final Color GOLD = new Color(255, 215, 90);
    static final Color LINE = new Color(85, 92, 117);
    static final String F = "맑은 고딕";
    private static final int BTN_Y = 668, BTN_H = 56;

    private final JButton nextBtn = makeButton("다음  ▶", Color.WHITE);
    private final JButton prevBtn = makeButton("◀  이전", Color.WHITE);
    private final JButton startBtn = makeButton("모험 시작  ▶", Color.YELLOW);
    private final BufferedImage boss;
    private int page = 0;

    public PrologueScreen(Runnable onStart) {
        setBackground(BG);
        setLayout(null);

        BufferedImage img = null;
        try { img = ImageIO.read(new File("images/boss/coding_1.png")); } catch (Exception ignored) {}
        boss = img;

        nextBtn.addActionListener(e -> setPage(1));
        prevBtn.addActionListener(e -> setPage(0));
        startBtn.addActionListener(e -> onStart.run());
        add(nextBtn);
        add(prevBtn);
        add(startBtn);
        setPage(0);

        // Enter = 1쪽에서는 다음, 2쪽에서는 모험 시작 / ← → 로 쪽 넘기기 (이 화면이 보일 때만)
        bindKey(KeyEvent.VK_ENTER, "enter", () -> (page == 0 ? nextBtn : startBtn).doClick());
        bindKey(KeyEvent.VK_RIGHT, "right", () -> { if (page == 0) nextBtn.doClick(); });
        bindKey(KeyEvent.VK_LEFT, "left", () -> { if (page == 1) prevBtn.doClick(); });
    }

    // 프롤로그를 띄울 때마다 1쪽부터
    public void reset() { setPage(0); }

    private void setPage(int p) {
        page = p;
        nextBtn.setVisible(p == 0);
        prevBtn.setVisible(p == 1);
        startBtn.setVisible(p == 1);
        revalidate();
        repaint();
    }

    private void bindKey(int key, String name, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, 0), name);
        getActionMap().put(name, new AbstractAction() {
            // 화면이 안 보일 때는 비활성: 다른 화면의 키 입력(이름 맞추기 Enter, 탄막 방향키 등)을 가로채지 않음
            @Override public boolean isEnabled() { return isShowing(); }
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    private static JButton makeButton(String text, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(CARD);
        b.setForeground(fg);
        b.setBorder(new LineBorder(MINT, 2));
        b.setFocusPainted(false);
        return b;
    }

    double scale() { return Math.min(getWidth() / (double) BASE_W, getHeight() / (double) BASE_H); }
    int offX() { return (int) Math.round((getWidth() - BASE_W * scale()) / 2); }
    int offY() { return (int) Math.round((getHeight() - BASE_H * scale()) / 2); }

    // 기준 좌표(1050x800)의 사각형을 실제 화면 위치로
    void place(JButton b, int x, int y, int w, int h) {
        double s = scale();
        b.setBounds(offX() + (int) (x * s), offY() + (int) (y * s), (int) (w * s), (int) (h * s));
        b.setFont(new Font(F, Font.BOLD, Math.max(12, (int) Math.round(22 * s))));
    }

    @Override
    public void doLayout() {
        int W = BASE_W;
        place(nextBtn, (W - 240) / 2, BTN_Y, 240, BTN_H);
        place(prevBtn, W / 2 - 220, BTN_Y, 200, BTN_H);
        place(startBtn, W / 2 + 20, BTN_Y, 240, BTN_H);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        hints(g);
        paintBackground(g, getWidth(), getHeight(), scale());
        g.translate(offX(), offY());
        g.scale(scale(), scale());
        if (page == 0) paintStory(g); else paintItems(g);
        paintPageDots(g);
        g.dispose();
    }

    static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    // 배경은 창 전체에: 남색 → 어두운 던전, 별빛, 아래쪽 출구 빛
    static void paintBackground(Graphics2D g, int W, int H, double scale) {
        g.setPaint(new GradientPaint(0, 0, new Color(22, 30, 58), 0, H, new Color(8, 10, 20)));
        g.fillRect(0, 0, W, H);
        Random r = new Random(7);
        int n = W * H / 6000;
        for (int i = 0; i < n; i++) {
            g.setColor(new Color(200, 230, 255, 40 + r.nextInt(150)));
            int s = r.nextInt(3) + 1;
            g.fillOval(r.nextInt(Math.max(1, W)), r.nextInt(Math.max(1, H)), s, s);
        }
        g.setPaint(new RadialGradientPaint(new Point2D.Float(W / 2f, H + 60), Math.max(1, (int) (520 * scale)),
                new float[]{0f, 1f}, new Color[]{new Color(0, 200, 190, 70), new Color(0, 200, 190, 0)}));
        g.fillRect(0, 0, W, H);
    }

    // ---------------- 1쪽: 이야기 + 3단계 ----------------
    private void paintStory(Graphics2D g) {
        int W = BASE_W;
        center(g, "PROLOGUE", new Font(F, Font.BOLD, 20), MINT, 70);
        center(g, "던전에서 탈출하라", new Font(F, Font.BOLD, 46), Color.WHITE, 130);
        g.setColor(MINT);
        g.fillRect(W / 2 - 40, 152, 80, 3);

        String[] story = {
            "눈을 떠 보니, 낯선 던전 한가운데였다.",
            "출구로 가는 길은 지식의 보스들이 가로막고 있다.",
            "쏟아지는 탄막을 피하고, 퀴즈를 맞혀 보스를 쓰러뜨려라.",
        };
        Font sf = new Font(F, Font.PLAIN, 23);
        for (int i = 0; i < story.length; i++) center(g, story[i], sf, new Color(220, 225, 235), 205 + i * 36);
        center(g, "모든 보스를 물리치면, 이 모험에서 탈출할 수 있다.", new Font(F, Font.BOLD, 24), GOLD, 205 + 3 * 36 + 6);

        int cw = 290, ch = 250, gap = 30, cy = 370;
        int cx0 = (W - (cw * 3 + gap * 2)) / 2;
        String[][] txt = {
            {"1", "탄막 회피", "방향키로 움직여", "쏟아지는 탄막을 피하라"},
            {"2", "퀴즈 공격", "정답을 맞히면", "보스에게 데미지!"},
            {"3", "보스 격파", "보스를 쓰러뜨리고", "출구에 한 걸음 더"},
        };
        for (int i = 0; i < 3; i++) {
            int x = cx0 + i * (cw + gap);
            g.setColor(new Color(25, 30, 50, 235));
            g.fillRoundRect(x, cy, cw, ch, 18, 18);
            g.setColor(i == 2 ? GOLD : MINT);
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(x, cy, cw, ch, 18, 18);
            g.fillOval(x + 16, cy + 16, 34, 34);   // 번호 동그라미
            g.setColor(BG);
            g.setFont(new Font(F, Font.BOLD, 20));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(txt[i][0], x + 33 - fm.stringWidth(txt[i][0]) / 2, cy + 40);

            Rectangle art = new Rectangle(x + 20, cy + 60, cw - 40, 105);
            if (i == 0) drawBullets(g, art);
            else if (i == 1) drawQuiz(g, art);
            else drawBoss(g, art);

            g.setFont(new Font(F, Font.BOLD, 26));
            g.setColor(Color.WHITE);
            fm = g.getFontMetrics();
            g.drawString(txt[i][1], x + (cw - fm.stringWidth(txt[i][1])) / 2, cy + 198);
            g.setFont(new Font(F, Font.PLAIN, 18));
            g.setColor(new Color(180, 190, 205));
            fm = g.getFontMetrics();
            g.drawString(txt[i][2], x + (cw - fm.stringWidth(txt[i][2])) / 2, cy + 222);
            g.drawString(txt[i][3], x + (cw - fm.stringWidth(txt[i][3])) / 2, cy + 242);
            if (i < 2) { // 카드 사이 화살표
                g.setColor(MINT);
                int ax = x + cw + 6, ay = cy + ch / 2;
                g.fillPolygon(new int[]{ax, ax + 18, ax}, new int[]{ay - 10, ay, ay + 10}, 3);
            }
        }
    }

    // ---------------- 2쪽: 아이템 ----------------
    private void paintItems(Graphics2D g) {
        int W = BASE_W;
        center(g, "ITEM", new Font(F, Font.BOLD, 20), MINT, 70);
        center(g, "위기를 넘기는 아이템", new Font(F, Font.BOLD, 46), Color.WHITE, 130);
        g.setColor(MINT);
        g.fillRect(W / 2 - 40, 152, 80, 3);
        Font sf = new Font(F, Font.PLAIN, 23);
        center(g, "전투를 시작할 때마다 종류별로 1개씩 받는다.", sf, new Color(220, 225, 235), 205);
        center(g, "숫자 키 1 · 2 · 3 으로 바로 쓰거나, 명령 막대의 아이템 버튼으로 쓸 수 있다.", sf, new Color(220, 225, 235), 241);

        String[][] items = {
            {"1", "무적", "3초 동안 탄막에 맞아도", "하트가 줄지 않는다", "탄막 중에 숫자 키 1"},
            {"2", "체력 완충", "줄어든 하트를", "가득 채운다", "숫자 키 2 · 아이템 버튼"},
            {"3", "문제 개수 초기화", "다 써 버린 공격 횟수를", "처음으로 되돌린다", "숫자 키 3 · 아이템 버튼"},
        };
        int cw = 290, ch = 330, gap = 30, cy = 285;
        int cx0 = (W - (cw * 3 + gap * 2)) / 2;
        for (int i = 0; i < Inventory.TYPES; i++) {
            int x = cx0 + i * (cw + gap);
            Color c = ItemIcon.COLORS[i];
            g.setColor(new Color(25, 30, 50, 235));
            g.fillRoundRect(x, cy, cw, ch, 18, 18);
            g.setColor(c);
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(x, cy, cw, ch, 18, 18);

            // 큰 아이템 칸 (전투 화면 아이템 칸 모양을 크게)
            int bs = 110, bx = x + (cw - bs) / 2, by = cy + 34;
            g.setPaint(new RadialGradientPaint(new Point2D.Float(bx + bs / 2f, by + bs / 2f), bs,
                    new float[]{0f, 1f}, new Color[]{new Color(c.getRed(), c.getGreen(), c.getBlue(), 55),
                                                     new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)}));
            g.fillOval(bx - bs / 2, by - bs / 2, bs * 2, bs * 2);
            g.setColor(CARD);
            g.fillRoundRect(bx, by, bs, bs, 22, 22);
            g.setColor(LINE);
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(bx, by, bs, bs, 22, 22);
            Graphics2D ig = (Graphics2D) g.create();
            ItemIcon.draw(ig, i, bx + 18, by + 18, bs - 36);
            ig.dispose();
            // 숫자 키 배지
            g.setColor(MINT);
            g.fillOval(bx - 14, by - 14, 34, 34);
            g.setColor(new Color(4, 52, 44));
            g.setFont(new Font(F, Font.BOLD, 21));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(items[i][0], bx + 3 - fm.stringWidth(items[i][0]) / 2, by + 10);

            g.setFont(new Font(F, Font.BOLD, 26));
            g.setColor(c);
            fm = g.getFontMetrics();
            g.drawString(items[i][1], x + (cw - fm.stringWidth(items[i][1])) / 2, cy + 192);
            g.setFont(new Font(F, Font.PLAIN, 19));
            g.setColor(new Color(220, 225, 235));
            fm = g.getFontMetrics();
            g.drawString(items[i][2], x + (cw - fm.stringWidth(items[i][2])) / 2, cy + 228);
            g.drawString(items[i][3], x + (cw - fm.stringWidth(items[i][3])) / 2, cy + 252);

            // 사용법 알약
            g.setFont(new Font(F, Font.BOLD, 16));
            fm = g.getFontMetrics();
            int pw = fm.stringWidth(items[i][4]) + 28, ph = 30, px = x + (cw - pw) / 2, py = cy + 278;
            g.setColor(new Color(20, 26, 46));
            g.fillRoundRect(px, py, pw, ph, ph, ph);
            g.setColor(LINE);
            g.setStroke(new BasicStroke(1.2f));
            g.drawRoundRect(px, py, pw, ph, ph, ph);
            g.setColor(new Color(170, 180, 200));
            g.drawString(items[i][4], px + 14, py + 20);
        }
        center(g, "효과가 없을 때(하트가 가득하거나, 공격 횟수가 그대로일 때)는 아이템이 줄지 않는다.",
                new Font(F, Font.PLAIN, 17), new Color(140, 150, 170), 645);
    }

    // 쪽 표시: 현재 쪽은 긴 민트 막대, 나머지는 회색 점 + "1 / 2"
    private void paintPageDots(Graphics2D g) {
        int y = 752;
        for (int i = 0; i < 2; i++) {
            int x = BASE_W / 2 - 22 + i * 30;
            if (i == page) { g.setColor(MINT); g.fillRoundRect(x - 6, y, 26, 10, 10, 10); }
            else { g.setColor(new Color(90, 100, 125)); g.fillOval(x + 2, y, 10, 10); }
        }
        center(g, (page + 1) + " / 2", new Font(F, Font.PLAIN, 15), new Color(130, 140, 160), y + 34);
    }

    static void center(Graphics2D g, String s, Font f, Color c, int y) {
        g.setFont(f);
        g.setColor(c);
        g.drawString(s, (BASE_W - g.getFontMetrics().stringWidth(s)) / 2, y);
    }

    // 탄막: 붉은 탄 여러 개 + 가운데 플레이어(하트)
    private static void drawBullets(Graphics2D g, Rectangle a) {
        g.setColor(new Color(12, 15, 28));
        g.fillRect(a.x, a.y, a.width, a.height);
        g.setColor(new Color(70, 80, 100));
        g.setStroke(new BasicStroke(1f));
        g.drawRect(a.x, a.y, a.width, a.height);
        Random r = new Random(3);
        for (int i = 0; i < 16; i++) {
            int bx = a.x + 8 + r.nextInt(a.width - 16), by = a.y + 8 + r.nextInt(a.height - 16);
            if (Math.abs(bx - a.getCenterX()) < 28 && Math.abs(by - a.getCenterY()) < 24) continue;
            g.setColor(new Color(255, 90, 90, 60));
            g.fillOval(bx - 8, by - 8, 16, 16);
            g.setColor(new Color(255, 110, 110));
            g.fillOval(bx - 4, by - 4, 8, 8);
        }
        g.setColor(new Color(255, 80, 110));
        g.setFont(new Font("Dialog", Font.BOLD, 34));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("♥", (int) a.getCenterX() - fm.stringWidth("♥") / 2, (int) a.getCenterY() + 12);
    }

    // 퀴즈: 문제 한 줄 + 보기 4개, 하나는 정답(민트)
    private static void drawQuiz(Graphics2D g, Rectangle a) {
        g.setColor(new Color(20, 25, 45));
        g.fillRect(a.x, a.y, a.width, a.height);
        g.setColor(MINT);
        g.fillRect(a.x, a.y, (int) (a.width * 0.65), 4); // 시간 막대
        g.setColor(Color.WHITE);
        g.setFont(new Font(F, Font.BOLD, 17));
        g.drawString("Q. 정답은?", a.x + 10, a.y + 26);
        int bw = (a.width - 30) / 2, bh = 26;
        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i < 4; i++) {
            int x = a.x + 10 + (i % 2) * (bw + 10), y = a.y + 40 + (i / 2) * (bh + 8);
            boolean ok = i == 1;
            g.setColor(ok ? new Color(0, 200, 190, 60) : CARD);
            g.fillRect(x, y, bw, bh);
            g.setColor(ok ? MINT : Color.GRAY);
            g.drawRect(x, y, bw, bh);
            g.setColor(ok ? MINT : new Color(200, 200, 200));
            g.setFont(new Font(F, ok ? Font.BOLD : Font.PLAIN, 15));
            g.drawString((i + 1) + ".  " + (ok ? "정답!" : "보기"), x + 8, y + 18);
        }
    }

    // 보스: 실제 보스 이미지 + 거의 빈 HP 막대
    private void drawBoss(Graphics2D g, Rectangle a) {
        if (boss != null) {
            double s = Math.min((a.width - 20.0) / boss.getWidth(), (a.height - 22.0) / boss.getHeight());
            int w = (int) (boss.getWidth() * s), h = (int) (boss.getHeight() * s);
            g.drawImage(boss, a.x + (a.width - w) / 2, a.y, w, h, null);
        }
        int hy = a.y + a.height - 12;
        g.setColor(new Color(60, 20, 25));
        g.fillRect(a.x + 30, hy, a.width - 60, 10);
        g.setColor(new Color(255, 90, 90));
        g.fillRect(a.x + 30, hy, (int) ((a.width - 60) * 0.12), 10);
        g.setColor(Color.GRAY);
        g.setStroke(new BasicStroke(1f));
        g.drawRect(a.x + 30, hy, a.width - 60, 10);
    }
}
