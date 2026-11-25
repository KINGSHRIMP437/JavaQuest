import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.geom.Point2D;
import java.util.Random;

// 던전 분위기 배경: 어두운 남색 바탕 위로 민트·노란 먼지 입자가 천천히 떠오른다.
// 타이틀 화면은 위쪽에 "출구에서 새어 나오는 빛"(archGlow)도 함께 그린다.
// 타이틀·주제 선택·스테이지 맵 화면이 이 클래스를 상속해서 배경을 공유한다.
public class DungeonBackground extends JPanel {
    private static final int PARTICLE_COUNT = 75;   // 먼지 개수 (늘리면 더 많이 떠오름)
    private static final int FRAME_MS = 33;   // 약 30fps

    // 먼지 입자 하나 (위치는 화면 크기에 대한 비율 0~1로 저장 → 창 크기가 바뀌어도 그대로)
    private static class Particle {
        double x, y, vx, vy, r;
        int alpha;
        boolean mint;
    }

    private final Particle[] particles = new Particle[PARTICLE_COUNT];
    private final boolean archGlow;
    private final Timer timer;
    private double glowPhase = 0;   // 출구 빛이 천천히 밝아졌다 어두워지는 주기

    public DungeonBackground(boolean archGlow) {
        this.archGlow = archGlow;
        setBackground(UiTheme.BG);
        setOpaque(true);

        Random rnd = new Random();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            Particle p = new Particle();
            p.x = rnd.nextDouble();
            p.y = rnd.nextDouble();
            p.r = rnd.nextDouble() * 1.8 + 0.8;
            p.vx = (rnd.nextDouble() - 0.5) * 0.00025;
            p.vy = -(rnd.nextDouble() * 0.0004 + 0.00012);   // 위로 떠오름
            p.alpha = 40 + rnd.nextInt(110);
            p.mint = rnd.nextDouble() > 0.45;
            particles[i] = p;
        }

        timer = new Timer(FRAME_MS, e -> step());
        // 화면이 보일 때만 움직이고, 다른 화면으로 넘어가면 멈춤 (CPU 낭비 방지)
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) timer.start();
                else timer.stop();
            }
        });
    }

    // 한 프레임: 입자 이동 → 다시 그리기
    private void step() {
        for (Particle p : particles) {
            p.x += p.vx;
            p.y += p.vy;
            if (p.y < 0) p.y = 1;   // 위로 빠져나가면 아래에서 다시 등장
            if (p.x < 0) p.x = 1;
            if (p.x > 1) p.x = 0;
        }
        glowPhase += FRAME_MS / 4000.0 * Math.PI * 2;   // 4초 주기
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);   // 배경색 칠하기
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();

        if (archGlow) paintArchGlow(g2, w, h);

        for (Particle p : particles) {
            Color c = p.mint ? new Color(0, 200, 190, p.alpha) : new Color(253, 224, 71, (int) (p.alpha * 0.7));
            g2.setColor(c);
            double px = p.x * w, py = p.y * h;
            g2.fillOval((int) (px - p.r), (int) (py - p.r), (int) (p.r * 2), (int) (p.r * 2));
        }
        g2.dispose();
    }

    // 화면 위쪽 가운데에서 퍼지는 노란빛+민트빛 (밝기가 천천히 변함)
    private void paintArchGlow(Graphics2D g2, int w, int h) {
        float pulse = (float) (0.62 + 0.38 * Math.sin(glowPhase));   // 0.24 ~ 1.0
        float radius = Math.max(260, w * 0.32f);
        Color yellow = new Color(253, 224, 71, (int) (46 * pulse));
        Color mint = new Color(0, 200, 190, (int) (26 * pulse));
        RadialGradientPaint paint = new RadialGradientPaint(
                new Point2D.Float(w / 2f, 0), radius,
                new float[]{0f, 0.45f, 1f},
                new Color[]{yellow, mint, new Color(0, 0, 0, 0)});
        g2.setPaint(paint);
        g2.fillRect(0, 0, w, (int) radius);
    }
}
