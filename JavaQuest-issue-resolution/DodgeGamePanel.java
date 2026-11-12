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
    Timer phaseTimer; // 필드로 승격하여 외부에서 중지 가능하도록 수정
    boolean hitCooldown = false;
    int hitsTaken = 0;
    
    private Timer hitFlashTimer;
    private int hitFlashCount = 0;
    private boolean isFlashing = false;
    
    int currentStage = 1;
    String currentTopicId = "coding";
    
    int laserTimer = 0;
    boolean isHorizontalLaser = false;
    int laserPos = 0;
    boolean isLaserCharging = false;
    boolean isLaserFiring = false;
    int chargeCounter = 0;
    int fireCounter = 0;

    private static final int ALL = 0;    
    private static final int SEQ = 1;    
    private static final int RING = 2;   

    private static final int BULLET_END_MS = 3000;
    private static final int SLIDE_MS = 250;
    private static final int FADE_MS = 200;
    private static final int[][] TIMING = {
        {900, 600, 0},     
        {600, 160, 500},   
        {650, 350, 260},   
    };
    private static final int RING_LOCK_MS = 250;   
    private static final int RING_BEAM_W = 34;
    private static final int BEAM_LEN = 1500;
    private static final int[][] SCRIPT = {
        {3000,  ALL,  1, 11},   
        {5000,  ALL,  0, 7},    
        {7000,  SEQ,  0, 7},    
        {11400, SEQ,  1, 11},   
        {17800, RING, 0, 12},   
    };
    private static final int PATTERN_END_MS = computePatternEnd();
    private static final Color BEAM_COLOR = new Color(0, 230, 220);

    long startTime;
    int[] safeLanes = new int[SCRIPT.length];             
    boolean[] reversed = new boolean[SCRIPT.length];      
    double[] ringOffset = new double[SCRIPT.length];      
    double[][] ringAim = new double[SCRIPT.length][];     

    private final Set<Integer> pressedKeys = new HashSet<>();
    private Consumer<Integer> onFinish;
    private Runnable onHit; // 맞을 때마다 호출될 콜백
    Random rand = new Random();

    public DodgeGamePanel(Consumer<Integer> onFinish, Runnable onHit) {
        this.onFinish = onFinish;
        this.onHit = onHit;
        setFocusable(true);
        setBackground(new Color(15, 20, 35));
        setPreferredSize(new Dimension(870, 390));

        hitFlashTimer = new Timer(80, e -> {
            hitFlashCount++;
            if (hitFlashCount >= 12) {
                isFlashing = false;
                hitFlashTimer.stop();
            }
            repaint();
        });

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
        
        // 기존 타이머 중지
        if (gameLoopTimer != null) gameLoopTimer.stop();
        if (phaseTimer != null) phaseTimer.stop();
        
        resetGame();
        startTime = System.currentTimeMillis();
        
        if (gameLoopTimer != null) {
            gameLoopTimer.start();
        }
        
        requestFocusInWindow();

        // 패턴 지속 시간 계산 (최소 5초 보장 안전장치 추가)
        int rawDuration = useCannonPattern() ? PATTERN_END_MS : 5000 + (stageNo * 500);
        int duration = Math.max(5000, rawDuration); 
        
        phaseTimer = new Timer(duration, e -> {
            stopDodgeGame();
            if (onFinish != null) {
                onFinish.accept(hitsTaken);
            }
        });
        phaseTimer.setRepeats(false);
        phaseTimer.start();
    }

    // 하트가 0이 되었을 때 외부에서 즉시 회피 게임을 멈출 수 있는 메서드
    public void stopDodgeGame() {
        if (gameLoopTimer != null) gameLoopTimer.stop();
        if (phaseTimer != null) phaseTimer.stop();
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
        isFlashing = false;
        if (hitFlashTimer != null) hitFlashTimer.stop();
        if (phaseTimer != null) phaseTimer.stop();
        laserTimer = 0;
        isLaserCharging = false;
        isLaserFiring = false;
        for (int i = 0; i < SCRIPT.length; i++) {
            int lanes = SCRIPT[i][3];
            safeLanes[i] = SCRIPT[i][1] == ALL ? rand.nextInt(lanes) : -1;
            reversed[i] = rand.nextBoolean();
            ringOffset[i] = rand.nextDouble() * Math.PI * 2;
            ringAim[i] = new double[lanes];
        }
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
        if (useCannonPattern()) {   
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
            for (int ev = 0; ev < SCRIPT.length; ev++) {
                long t = elapsed - SCRIPT[ev][0];
                if (t < 0 || t >= eventLength(SCRIPT[ev])) continue;
                drawEvent(g2, ev, t, playerRect, panelWidth, panelHeight);
            }
        } else {
            drawRandomLaser(g, playerRect, panelWidth, panelHeight);
        }

        drawPlayerAndStatus(g);
    }

    private boolean useCannonPattern() {
        return "coding".equals(currentTopicId) && currentStage == 1;
    }

    private static int eventLength(int[] e) {
        int[] tm = TIMING[e[1]];
        return SLIDE_MS + (e[3] - 1) * tm[2] + tm[0] + tm[1] + FADE_MS;
    }

    private static int computePatternEnd() {
        int end = 0;
        for (int[] e : SCRIPT) end = Math.max(end, e[0] + eventLength(e));
        return end + 250;
    }

    private long chargeStart(int ev, int lane) {
        int type = SCRIPT[ev][1], lanes = SCRIPT[ev][3];
        int order = (type == SEQ && reversed[ev]) ? lanes - 1 - lane : lane;
        return SLIDE_MS + (long) order * TIMING[type][2];
    }

    private void drawEvent(Graphics2D g2, int ev, long t, Rectangle playerRect, int w, int h) {
        int type = SCRIPT[ev][1];
        int lanes = SCRIPT[ev][3];
        int left = 20, top = 20, right = w - 20, bottom = h - 20;
        long len = eventLength(SCRIPT[ev]);

        double slide = 1 - Math.pow(1 - Math.min(1.0, t / (double) SLIDE_MS), 3);
        float alpha = t > len - FADE_MS ? Math.max(0f, (len - t) / (float) FADE_MS) : 1f;

        Composite oldComposite = g2.getComposite();
        Shape oldClip = g2.getClip();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.clipRect(left, top, right - left, bottom - top);   

        double flash = 0;
        for (int lane = 0; lane < lanes; lane++) {
            if (lane == safeLanes[ev]) continue;
            long chargeT = t - chargeStart(ev, lane);
            long fireT = chargeT - TIMING[type][0];
            double f = type == RING
                    ? drawRingTurret(g2, ev, lane, lanes, chargeT, fireT, slide, playerRect, w, h)
                    : drawLaneTurret(g2, type, SCRIPT[ev][2] == 1, lane, lanes, chargeT, fireT, slide,
                                     playerRect, left, top, right, bottom);
            flash = Math.max(flash, f);
        }
        g2.setClip(oldClip);
        g2.setComposite(oldComposite);

        if (flash > 0) {
            g2.setColor(new Color(255, 255, 255, (int) Math.min(255, flash)));
            g2.fillRect(left, top, right - left, bottom - top);
        }
    }

    private double drawLaneTurret(Graphics2D g2, int type, boolean vertical, int lane, int lanes,
                                  long chargeT, long fireT, double slide, Rectangle playerRect,
                                  int left, int top, int right, int bottom) {
        int fireLen = TIMING[type][1];
        double laneSize = (vertical ? right - left : bottom - top) / (double) lanes;
        int laneStart = (vertical ? left : top) + (int) Math.round(lane * laneSize) + 3;
        int size = (int) Math.round(laneSize) - 6;   
        int center = laneStart + size / 2;
        int cannonPos = (int) Math.round(-30 + 68 * slide);   
        Rectangle laneRect = vertical
                ? new Rectangle(laneStart, top, size, bottom - top)
                : new Rectangle(left, laneStart, right - left, size);
        double angle = vertical ? Math.PI / 2 : 0;   

        if (chargeT >= 0 && fireT < 0) {
            boolean blink = (chargeT / 90) % 2 == 0;
            g2.setColor(new Color(0, 220, 210, blink ? 70 : 30));
            g2.fill(laneRect);
        }

        double flash = 0;
        if (fireT >= 0 && fireT < fireLen) {
            int mx = vertical ? center : cannonPos + 16;
            int my = vertical ? cannonPos + 16 : center;
            double k = drawBeamFx(g2, mx, my, angle, size, fireT, fireLen);
            if (k > 0.5 && playerRect.intersects(laneRect)) applyHit();
            if (fireT < 120) flash = (type == ALL ? 100 : 30) * (1 - fireT / 120.0);
        }

        drawCannon(g2, vertical ? center : cannonPos, vertical ? cannonPos : center,
                   angle - Math.PI / 2, chargeT, TIMING[type][0], fireT);
        return flash;
    }

    private double drawRingTurret(Graphics2D g2, int ev, int lane, int lanes, long chargeT, long fireT,
                                  double slide, Rectangle playerRect, int w, int h) {
        int chargeLen = TIMING[RING][0], fireLen = TIMING[RING][1];
        double cx = w / 2.0, cy = h / 2.0;
        double rx = w / 2.0 - 55, ry = h / 2.0 - 48;
        double scale = 1.5 - 0.5 * slide;                        
        double theta = ringOffset[ev] + lane * Math.PI * 2 / lanes;   
        double tx = cx + rx * scale * Math.cos(theta);
        double ty = cy + ry * scale * Math.sin(theta);
        double px = playerRect.getCenterX(), py = playerRect.getCenterY();

        double[] aim = ringAim[ev];
        if (chargeT < 0) aim[lane] = Math.atan2(cy - ty, cx - tx);              
        else if (fireT < -RING_LOCK_MS) aim[lane] = Math.atan2(py - ty, px - tx); 
        double a = aim[lane];                                                     
        double mx = tx + 16 * Math.cos(a), my = ty + 16 * Math.sin(a);           
        double ex = mx + BEAM_LEN * Math.cos(a), ey = my + BEAM_LEN * Math.sin(a);

        if (chargeT >= 0 && fireT < 0) {
            boolean locked = fireT >= -RING_LOCK_MS;
            boolean blink = (chargeT / 80) % 2 == 0;
            g2.setColor(new Color(0, 220, 210, locked ? 170 : (blink ? 100 : 45)));
            g2.setStroke(locked ? new BasicStroke(3)
                    : new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{8, 6}, 0));
            g2.draw(new java.awt.geom.Line2D.Double(mx, my, ex, ey));
            g2.setStroke(new BasicStroke(1));
        }

        double flash = 0;
        if (fireT >= 0 && fireT < fireLen) {
            double k = drawBeamFx(g2, mx, my, a, RING_BEAM_W, fireT, fireLen);
            if (k > 0.5 && java.awt.geom.Line2D.ptSegDist(mx, my, ex, ey, px, py) < RING_BEAM_W / 2.0 + 8) applyHit();
            if (fireT < 120) flash = 40 * (1 - fireT / 120.0);
        }

        drawCannon(g2, tx, ty, a - Math.PI / 2, chargeT, chargeLen, fireT);
        return flash;
    }

    private double drawBeamFx(Graphics2D g2, double mx, double my, double angle, int size, long fireT, int fireLen) {
        double rise = Math.min(70, fireLen * 0.25), fall = Math.min(160, fireLen * 0.4);
        double k = fireT < rise ? fireT / rise
                 : fireT > fireLen - fall ? (fireLen - fireT) / fall : 1.0;
        double wobble = 1 + 0.08 * Math.sin(fireT / 18.0);   
        int beamW = (int) Math.round(size * k * wobble);
        int coreW = (int) Math.round(beamW * 0.45);

        java.awt.geom.AffineTransform saved = g2.getTransform();
        g2.translate(mx, my);
        g2.rotate(angle);
        fillBeamLayer(g2, beamW + 8, new Color(BEAM_COLOR.getRed(), BEAM_COLOR.getGreen(), BEAM_COLOR.getBlue(), 60));
        fillBeamLayer(g2, beamW, new Color(BEAM_COLOR.getRed(), BEAM_COLOR.getGreen(), BEAM_COLOR.getBlue(), 190));
        fillBeamLayer(g2, coreW, Color.WHITE);
        g2.setTransform(saved);
        return k;
    }

    private void fillBeamLayer(Graphics2D g2, int width, Color c) {
        if (width <= 0) return;
        g2.setColor(c);
        g2.fillRect(0, -width / 2, BEAM_LEN, width);
    }

    private void drawCannon(Graphics2D g2, double cx, double cy, double rotation, long chargeT, int chargeLen, long fireT) {
        java.awt.geom.AffineTransform saved = g2.getTransform();
        g2.translate(cx, cy);
        g2.rotate(rotation);
        if (fireT >= 0 && fireT < 150) g2.translate(0, -4 * (1 - fireT / 150.0));   

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

        if (chargeT >= 0 && fireT < 0) {
            double p = Math.min(1.0, chargeT / (double) chargeLen);
            int r = (int) Math.round(3 + 10 * p);
            int a = (int) Math.round((110 + 145 * p) * ((chargeT / 60) % 2 == 0 ? 1.0 : 0.6));
            g2.setColor(new Color(190, 255, 250, Math.min(255, a)));
            g2.fillOval(-r, 16 - r, r * 2, r * 2);
        }
        g2.setTransform(saved);
    }

    private int[] shakeOffset(long elapsed) {
        double[] power = {6, 3, 4};   
        double m = 0;
        for (int ev = 0; ev < SCRIPT.length; ev++) {
            int type = SCRIPT[ev][1];
            for (int lane = 0; lane < SCRIPT[ev][3]; lane++) {
                if (lane == safeLanes[ev]) continue;
                long fireT = elapsed - SCRIPT[ev][0] - chargeStart(ev, lane) - TIMING[type][0];
                if (fireT >= 0 && fireT < 220) m = Math.max(m, power[type] * (1 - fireT / 220.0));
            }
        }
        int s = (int) Math.round(m);
        return new int[]{rand.nextInt(2 * s + 1) - s, rand.nextInt(2 * s + 1) - s};
    }

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
        if (isFlashing) {
            g.setColor(hitFlashCount % 2 == 0 ? Color.WHITE : Color.RED);
        } else {
            g.setColor(hitCooldown ? Color.GRAY : Color.RED);
        }
        
        g.fillRect(playerX, playerY, 20, 20);

        g.setColor(Color.YELLOW);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        g.drawString("[" + currentTopicId.toUpperCase() + " / Stage " + currentStage + "] 회피 중! 피격: " + hitsTaken, 25, getHeight() - 24);
    }

    private void applyHit() {
        if (!hitCooldown) {
            hitsTaken++;
            hitCooldown = true;
            
            isFlashing = true;
            hitFlashCount = 0;
            if (hitFlashTimer != null) hitFlashTimer.restart();

            // 맞을 때마다 즉시 실행할 콜백(onHit) 호출
            if (onHit != null) {
                onHit.run();
            }

            Timer coolTimer = new Timer(1000, evt -> hitCooldown = false);
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