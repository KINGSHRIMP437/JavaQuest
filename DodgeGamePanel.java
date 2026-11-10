import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

public class DodgeGamePanel extends JPanel {
    int playerX = 400, playerY = 180;
    List<Bullet> bullets = new ArrayList<>();
    int spawnCounter = 0;
    Timer gameLoopTimer;
    boolean hitCooldown = false;
    int hitsTaken = 0;
    
    int currentStage = 1;
    String currentTopicId = "coding";
    
    int laserTimer = 0;
    boolean isHorizontalLaser = false;
    int laserPos = 0;
    boolean isLaserCharging = false;
    boolean isLaserFiring = false;
    int chargeCounter = 0;
    int fireCounter = 0;

    // 포탑 빔 패턴 (테스트: coding 1스테이지)
    // 탄막 3초 → 위쪽 포탑 10개 일제 사격 → 왼쪽 포탑 6개 일제 사격. 매번 한 줄만 안전
    // 포탑 한 번: 등장(미끄러져 들어옴) → 충전(빛·예고선) → 발사(빔·번쩍임·흔들림) → 사라짐
    private static final int BULLET_END_MS = 3000;
    private static final int SLIDE_MS = 250;
    private static final int CHARGE_MS = 900;
    private static final int FIRE_MS = 600;
    private static final int FADE_MS = 200;
    private static final int WAVE_LEN_MS = SLIDE_MS + CHARGE_MS + FIRE_MS + FADE_MS;
    // {시작 시각(ms), 방향(1=위에서 아래, 0=왼쪽에서 오른쪽), 줄 수(안전한 줄 1개 포함)}
    private static final int[][] WAVE_SCRIPT = {
        {3000, 1, 11},
        {5000, 0, 7},
    };
    private static final int PATTERN_END_MS = 5000 + WAVE_LEN_MS + 250;
    private static final Color BEAM_COLOR = new Color(0, 230, 220);
    long startTime;
    int[] safeLanes = new int[WAVE_SCRIPT.length];

    private final Set<Integer> pressedKeys = new HashSet<>();
    private Consumer<Integer> onFinish;
    Random rand = new Random();

    public DodgeGamePanel(Consumer<Integer> onFinish) {
        this.onFinish = onFinish;
        setFocusable(true);
        setBackground(new Color(15, 20, 35));
        setPreferredSize(new Dimension(870, 390));

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                pressedKeys.add(e.getKeyCode());
            }
            @Override
            public void keyReleased(KeyEvent e) {
                pressedKeys.remove(e.getKeyCode());
            }
        });

        gameLoopTimer = new Timer(30, e -> {
            updatePlayerPosition();
            repaint();
        });
    }

    public void startDodgeGame(String topicId, int stageNo) {
        this.currentTopicId = topicId;
        this.currentStage = stageNo;
        resetGame();
        startTime = System.currentTimeMillis();
        gameLoopTimer.start();
        requestFocusInWindow();

        int duration = useCannonPattern() ? PATTERN_END_MS : 5000 + (stageNo * 500);
        Timer phaseTimer = new Timer(duration, e -> {
            gameLoopTimer.stop();
            if (onFinish != null) {
                onFinish.accept(hitsTaken);
            }
        });
        phaseTimer.setRepeats(false);
        phaseTimer.start();
    }

    private void updatePlayerPosition() {
        int speed = 10;
        int panelWidth = getWidth();
        int panelHeight = getHeight();

        if (panelWidth <= 0 || panelHeight <= 0) return;

        if (pressedKeys.contains(KeyEvent.VK_LEFT) && playerX > 25) playerX -= speed;
        if (pressedKeys.contains(KeyEvent.VK_RIGHT) && playerX < panelWidth - 45) playerX += speed;
        if (pressedKeys.contains(KeyEvent.VK_UP) && playerY > 25) playerY -= speed;
        if (pressedKeys.contains(KeyEvent.VK_DOWN) && playerY < panelHeight - 45) playerY += speed;
    }

    public void resetGame() {
        playerX = 400;
        playerY = 180;
        bullets.clear();
        spawnCounter = 0;
        hitsTaken = 0;
        hitCooldown = false;
        laserTimer = 0;
        isLaserCharging = false;
        isLaserFiring = false;
        for (int i = 0; i < WAVE_SCRIPT.length; i++) safeLanes[i] = rand.nextInt(WAVE_SCRIPT[i][2]);
        pressedKeys.clear();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        if (panelWidth < 100 || panelHeight < 100) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        long elapsed = System.currentTimeMillis() - startTime;
        if (useCannonPattern()) {   // 빔 발사 순간 화면 흔들림
            int[] shake = shakeOffset(elapsed);
            g2.translate(shake[0], shake[1]);
        }

        g.setColor(Color.WHITE);
        g.drawRect(15, 15, panelWidth - 30, panelHeight - 30); 

        String[] symbols;
        if ("coding".equals(currentTopicId)) {
            symbols = new String[]{";", "{}", "null", "Object", "Error"};
        } else if ("common".equals(currentTopicId)) {
            symbols = new String[]{"?", "!", "상식", "백과", "퀴즈"};
        } else {
            symbols = new String[]{"★", "♪", "캐릭터", "주인공", "스타"};
        }

        // 포탑 패턴에서는 처음 3초 동안만 탄막을 새로 만든다
        boolean spawnBullets = !useCannonPattern() || elapsed < BULLET_END_MS;

        int spawnRate = Math.max(1, 4 - currentStage);
        spawnCounter++;
        if (spawnBullets && spawnCounter >= spawnRate) {
            spawnCounter = 0;
            int bx = 25 + rand.nextInt(Math.max(50, panelWidth - 60));
            int by = 20;
            int bvx = rand.nextInt(3) - 1;
            int bvy = (2 + currentStage) + rand.nextInt(3);
            
            String selectedText = symbols[rand.nextInt(symbols.length)];
            bullets.add(new Bullet(bx, by, bvx, bvy, selectedText));
        }

        Rectangle playerRect = new Rectangle(playerX, playerY, 20, 20);

        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.x += b.vx;
            b.y += b.vy;

            g.setColor(Color.ORANGE);
            g.setFont(new Font("Consolas", Font.BOLD, 13));
            g.drawString(b.text, b.x, b.y);

            Rectangle bulletRect = new Rectangle(b.x, b.y, 30, 15);
            if (playerRect.intersects(bulletRect)) {
                applyHit();
            }

            if (b.y > panelHeight - 20 || b.x < 18 || b.x > panelWidth - 20) {
                bullets.remove(i);
            }
        }

        if (useCannonPattern()) {
            for (int i = 0; i < WAVE_SCRIPT.length; i++) {
                long t = elapsed - WAVE_SCRIPT[i][0];
                if (t < 0 || t >= WAVE_LEN_MS) continue;
                drawWave(g2, t, WAVE_SCRIPT[i][1] == 1, WAVE_SCRIPT[i][2], safeLanes[i], playerRect, panelWidth, panelHeight);
            }
        } else {
            drawRandomLaser(g, playerRect, panelWidth, panelHeight);
        }

        drawPlayerAndStatus(g);
    }

    // 포탑 빔 패턴을 쓰는 스테이지 (지금은 테스트로 coding 1스테이지만)
    private boolean useCannonPattern() {
        return "coding".equals(currentTopicId) && currentStage == 1;
    }

    // 포탑 한 줄 일제 사격. t = 이 웨이브가 시작된 뒤 지난 시간(ms)
    private void drawWave(Graphics2D g2, long t, boolean vertical, int lanes, int safe,
                          Rectangle playerRect, int w, int h) {
        int left = 20, top = 20, right = w - 20, bottom = h - 20;
        double laneSize = (vertical ? right - left : bottom - top) / (double) lanes;
        long chargeT = t - SLIDE_MS;
        long fireT = chargeT - CHARGE_MS;
        long fadeT = fireT - FIRE_MS;

        // 등장: 화면 밖에서 미끄러져 들어옴 (끝에서 감속)
        double slide = Math.min(1.0, t / (double) SLIDE_MS);
        slide = 1 - Math.pow(1 - slide, 3);
        int cannonPos = (int) Math.round(-30 + 68 * slide);   // 가장자리에서 포탑까지 거리 (-30 → 38)
        int beamStart = cannonPos + 16;                         // 포구 위치

        // 퇴장: 서서히 투명해짐
        float alpha = fadeT > 0 ? Math.max(0f, 1f - fadeT / (float) FADE_MS) : 1f;
        Composite oldComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        for (int lane = 0; lane < lanes; lane++) {
            if (lane == safe) continue;
            int laneStart = (vertical ? left : top) + (int) Math.round(lane * laneSize) + 3;
            int size = (int) Math.round(laneSize) - 6;   // 빔 사이 틈(6px)은 플레이어(20px)보다 좁아 못 빠져나감
            int center = laneStart + size / 2;
            Rectangle laneRect = vertical
                    ? new Rectangle(laneStart, top, size, bottom - top)
                    : new Rectangle(left, laneStart, right - left, size);

            // 충전 중: 빔이 지나갈 자리를 깜빡이며 예고
            if (chargeT >= 0 && fireT < 0) {
                boolean blink = (chargeT / 90) % 2 == 0;
                g2.setColor(new Color(0, 220, 210, blink ? 70 : 30));
                g2.fill(laneRect);
            }

            // 발사 중: 굵은 빔이 순간 확 펼쳐졌다가 가늘어지며 사라짐
            if (fireT >= 0 && fireT < FIRE_MS) {
                double k = fireT < 70 ? fireT / 70.0
                         : fireT > FIRE_MS - 160 ? (FIRE_MS - fireT) / 160.0 : 1.0;
                double wobble = 1 + 0.08 * Math.sin(fireT / 18.0);   // 빔 굵기가 살짝 떨림
                int beamW = (int) Math.round(size * k * wobble);
                int coreW = (int) Math.round(beamW * 0.45);
                drawBeam(g2, vertical, center, beamStart, vertical ? bottom : right, beamW + 8,
                         new Color(BEAM_COLOR.getRed(), BEAM_COLOR.getGreen(), BEAM_COLOR.getBlue(), 60));
                drawBeam(g2, vertical, center, beamStart, vertical ? bottom : right, beamW,
                         new Color(BEAM_COLOR.getRed(), BEAM_COLOR.getGreen(), BEAM_COLOR.getBlue(), 190));
                drawBeam(g2, vertical, center, beamStart, vertical ? bottom : right, coreW, Color.WHITE);
                if (k > 0.5 && playerRect.intersects(laneRect)) applyHit();
            }

            drawCannon(g2, vertical ? center : cannonPos, vertical ? cannonPos : center, vertical, chargeT, fireT);
        }
        g2.setComposite(oldComposite);

        // 발사 순간 화면 번쩍임
        if (fireT >= 0 && fireT < 120) {
            g2.setColor(new Color(255, 255, 255, (int) (100 * (1 - fireT / 120.0))));
            g2.fillRect(left, top, right - left, bottom - top);
        }
    }

    private void drawBeam(Graphics2D g2, boolean vertical, int center, int start, int end, int width, Color c) {
        if (width <= 0) return;
        g2.setColor(c);
        if (vertical) g2.fillRect(center - width / 2, start, width, end - start);
        else g2.fillRect(start, center - width / 2, end - start, width);
    }

    // </> 모양 포탑. 기본은 아래를 향하고, 가로 웨이브는 오른쪽을 향하도록 회전
    private void drawCannon(Graphics2D g2, int cx, int cy, boolean vertical, long chargeT, long fireT) {
        java.awt.geom.AffineTransform saved = g2.getTransform();
        g2.translate(cx, cy);
        if (!vertical) g2.rotate(-Math.PI / 2);
        if (fireT >= 0 && fireT < 150) g2.translate(0, -4 * (1 - fireT / 150.0));   // 발사 반동

        g2.setColor(new Color(35, 40, 60));
        g2.fillRoundRect(-17, -16, 34, 24, 10, 10);
        g2.fillRect(-8, 6, 16, 10);
        g2.setColor(BEAM_COLOR);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(-17, -16, 34, 24, 10, 10);
        g2.drawRect(-8, 6, 16, 10);
        g2.setStroke(new BasicStroke(1));
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.BOLD, 12));
        g2.drawString("</>", -11, 0);

        // 충전 빛: 포구에 빛이 점점 커지며 깜빡임
        if (chargeT >= 0 && fireT < 0) {
            double p = chargeT / (double) CHARGE_MS;
            int r = (int) Math.round(3 + 10 * p);
            int a = (int) Math.round((110 + 145 * p) * ((chargeT / 60) % 2 == 0 ? 1.0 : 0.6));
            g2.setColor(new Color(190, 255, 250, Math.min(255, a)));
            g2.fillOval(-r, 16 - r, r * 2, r * 2);
        }
        g2.setTransform(saved);
    }

    // 빔이 발사된 직후 0.25초 동안 화면을 흔든다
    private int[] shakeOffset(long elapsed) {
        for (int[] wave : WAVE_SCRIPT) {
            long fireT = elapsed - wave[0] - SLIDE_MS - CHARGE_MS;
            if (fireT >= 0 && fireT < 250) {
                int m = (int) Math.round(6 * (1 - fireT / 250.0));
                return new int[]{rand.nextInt(2 * m + 1) - m, rand.nextInt(2 * m + 1) - m};
            }
        }
        return new int[]{0, 0};
    }

    // 기존 패턴: 무작위 위치에 레이저 1개
    private void drawRandomLaser(Graphics g, Rectangle playerRect, int panelWidth, int panelHeight) {
        int laserInterval = Math.max(25, 55 - (currentStage * 8));
        laserTimer++;
        if (laserTimer >= laserInterval && !isLaserCharging && !isLaserFiring) {
            isLaserCharging = true;
            chargeCounter = 0;
            isHorizontalLaser = rand.nextBoolean();
            
            if (isHorizontalLaser) {
                laserPos = 30 + rand.nextInt(Math.max(30, panelHeight - 60));
            } else {
                laserPos = 30 + rand.nextInt(Math.max(30, panelWidth - 60));
            }
        }

        if (isLaserCharging) {
            chargeCounter++;
            g.setColor(Color.YELLOW);
            Graphics2D g2d = (Graphics2D) g;
            float[] dashPattern = {6, 6};
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, dashPattern, 0));

            if (isHorizontalLaser) {
                g2d.drawLine(20, laserPos, panelWidth - 20, laserPos);
            } else {
                g2d.drawLine(laserPos, 20, laserPos, panelHeight - 20);
            }
            g2d.setStroke(new BasicStroke(1));

            g.setFont(new Font("맑은 고딕", Font.BOLD, 13));
            g.drawString("! 충전 중...", isHorizontalLaser ? 30 : Math.max(20, laserPos - 30), isHorizontalLaser ? Math.max(20, laserPos - 8) : 38);

            if (chargeCounter >= 25) {
                isLaserCharging = false;
                isLaserFiring = true;
                fireCounter = 0;
            }
        }

        if (isLaserFiring) {
            fireCounter++;
            g.setColor(new Color(255, 50, 50, 200));

            Rectangle laserRect;
            if (isHorizontalLaser) {
                g.fillRect(20, laserPos - 10, panelWidth - 40, 20);
                laserRect = new Rectangle(20, laserPos - 10, panelWidth - 40, 20);
            } else {
                g.fillRect(laserPos - 10, 20, 20, panelHeight - 40);
                laserRect = new Rectangle(laserPos - 10, 20, 20, panelHeight - 40);
            }

            if (playerRect.intersects(laserRect)) {
                applyHit();
            }

            if (fireCounter >= 15) {
                isLaserFiring = false;
                laserTimer = 0;
            }
        }
    }

    private void drawPlayerAndStatus(Graphics g) {
        g.setColor(hitCooldown ? Color.GRAY : Color.RED);
        g.fillRect(playerX, playerY, 20, 20);

        g.setColor(Color.YELLOW);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        g.drawString("[" + currentTopicId.toUpperCase() + " / Stage " + currentStage + "] 회피 중! 피격: " + hitsTaken, 25, getHeight() - 24);
    }

    private void applyHit() {
        if (!hitCooldown) {
            hitsTaken++;
            hitCooldown = true;
            Timer coolTimer = new Timer(500, evt -> hitCooldown = false);
            coolTimer.setRepeats(false);
            coolTimer.start();
        }
    }

    private static class Bullet {
        int x, y, vx, vy;
        String text;

        Bullet(int x, int y, int vx, int vy, String text) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.text = text;
        }
    }
}