import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Random;

// 주제 올클리어 축하 화면: STAGE 1~3을 모두 클리어하면 표시. 자동으로 닫히지 않고 버튼으로 나감
public class TopicClearScreen extends JPanel {
    private static final Color BG_COLOR = new Color(15, 20, 35);
    private static final Color BORDER_COLOR = new Color(0, 200, 190);
    private static final Color[] CONFETTI_COLORS = {
            new Color(255, 215, 0), new Color(0, 200, 190), new Color(255, 90, 120),
            new Color(120, 200, 255), new Color(160, 255, 120), new Color(255, 160, 60)};
    private static final int CONFETTI_COUNT = 140;

    private final JLabel titleLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel[] stageRows = new JLabel[3];
    private final JLabel totalLabel = new JLabel(" ", SwingConstants.CENTER);

    private final Random random = new Random();
    private final double[][] confetti = new double[CONFETTI_COUNT][];   // {x, y, 낙하 속도, 흔들림 위상, 회전, 회전 속도, 크기, 색}
    private final Timer anim = new Timer(16, e -> { stepConfetti(); repaint(); });
    private long lastStep;

    public TopicClearScreen(Runnable onToStageMap, Runnable onToTopicSelect) {
        setPreferredSize(new Dimension(1050, 800));
        setBackground(BG_COLOR);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(60, 40, 50, 40));

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        titleLabel.setForeground(Color.YELLOW);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 44));
        center.add(centered(titleLabel));
        center.add(Box.createVerticalStrut(14));

        JLabel sub = new JLabel("축하합니다! 모든 스테이지를 클리어했어요", SwingConstants.CENTER);
        sub.setForeground(Color.WHITE);
        sub.setFont(new Font("맑은 고딕", Font.PLAIN, 24));
        center.add(centered(sub));
        center.add(Box.createVerticalStrut(40));

        // 스테이지별 별점 요약 상자
        JPanel summary = new JPanel(new GridLayout(4, 1, 0, 6));
        summary.setBackground(new Color(20, 25, 45));
        summary.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 2), BorderFactory.createEmptyBorder(18, 50, 18, 50)));
        for (int i = 0; i < 3; i++) {
            stageRows[i] = new JLabel(" ", SwingConstants.CENTER);
            summary.add(stageRows[i]);
        }
        totalLabel.setForeground(new Color(100, 255, 150));
        totalLabel.setFont(new Font("맑은 고딕", Font.BOLD, 24));
        summary.add(totalLabel);
        summary.setMaximumSize(new Dimension(460, 230));
        summary.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(summary);

        JPanel centerWrap = new JPanel(new GridBagLayout());   // 세로 가운데
        centerWrap.setOpaque(false);
        centerWrap.add(center);
        add(centerWrap, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 0));
        buttons.setOpaque(false);
        buttons.add(makeButton("스테이지 맵으로", onToStageMap));
        buttons.add(makeButton("다른 주제 고르기", onToTopicSelect));
        add(buttons, BorderLayout.SOUTH);
    }

    // 축하 화면 내용 채우고 색종이 시작. stars[i] = STAGE i+1의 최고 별점
    public void showResult(String topicTitle, int[] stars) {
        titleLabel.setText("★ [" + topicTitle + "] 주제 클리어! ★");
        int total = 0;
        for (int i = 0; i < 3; i++) {
            stageRows[i].setText("<html><span style='font-size:18pt; color:#FFFFFF;'>STAGE " + (i + 1) + "&nbsp;&nbsp;&nbsp;</span>"
                    + starHtml(stars[i]) + "</html>");
            total += stars[i];
        }
        totalLabel.setText("총 별 " + total + " / 9");

        for (int i = 0; i < CONFETTI_COUNT; i++) confetti[i] = newPiece(true);
        lastStep = System.currentTimeMillis();
        anim.start();
    }

    // 화면을 떠날 때 애니메이션 정지
    public void stop() {
        anim.stop();
    }

    public boolean isAnimating() { return anim.isRunning(); }

    private static String starHtml(int stars) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 3; i++) {
            sb.append(i <= stars ? "<span style='font-size:20pt; color:#FFD700;'>★</span>"
                                 : "<span style='font-size:20pt; color:#555555;'>☆</span>");
        }
        return sb.toString();
    }

    private JButton makeButton(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setBackground(new Color(25, 30, 50));
        b.setForeground(Color.YELLOW);
        b.setFont(new Font("맑은 고딕", Font.BOLD, 22));
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 2), BorderFactory.createEmptyBorder(10, 30, 10, 30)));
        b.setFocusPainted(false);
        b.addActionListener(e -> {
            stop();
            action.run();
        });
        return b;
    }

    private static Component centered(JComponent c) {
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
        return c;
    }

    // 색종이 한 조각. 처음에는 화면 전체에 흩뿌리고, 이후에는 화면 위에서 새로 떨어짐
    private double[] newPiece(boolean anywhere) {
        int w = Math.max(getWidth(), 1050), h = Math.max(getHeight(), 800);
        return new double[]{
                random.nextDouble() * w,
                anywhere ? -random.nextDouble() * h : -20 - random.nextDouble() * 60,
                80 + random.nextDouble() * 120,          // 낙하 속도 px/초
                random.nextDouble() * Math.PI * 2,       // 좌우 흔들림 위상
                random.nextDouble() * Math.PI * 2,       // 회전
                (random.nextDouble() - 0.5) * 8,         // 회전 속도 rad/초
                6 + random.nextDouble() * 6,             // 크기
                random.nextInt(CONFETTI_COLORS.length)};
    }

    private void stepConfetti() {
        long now = System.currentTimeMillis();
        double dt = Math.min(0.05, (now - lastStep) / 1000.0);
        lastStep = now;
        for (int i = 0; i < CONFETTI_COUNT; i++) {
            double[] p = confetti[i];
            p[1] += p[2] * dt;
            p[3] += dt * 2;
            p[0] += Math.sin(p[3]) * 30 * dt;
            p[4] += p[5] * dt;
            if (p[1] > getHeight() + 20) confetti[i] = newPiece(false);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (confetti[0] == null) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        AffineTransform base = g2.getTransform();
        for (double[] p : confetti) {
            g2.setTransform(base);
            g2.translate(p[0], p[1]);
            g2.rotate(p[4]);
            g2.setColor(CONFETTI_COLORS[(int) p[7]]);
            double s = p[6];
            // 뒤집히듯 보이게 세로 길이를 회전에 따라 바꿈
            int hh = (int) Math.max(2, s * 0.6 * Math.abs(Math.cos(p[4] * 1.7)));
            g2.fillRect((int) (-s / 2), -hh / 2, (int) s, hh);
        }
        g2.dispose();
    }
}
