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
        gameLoopTimer.start();
        requestFocusInWindow();

        Timer phaseTimer = new Timer(5000 + (stageNo * 500), e -> {
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
        pressedKeys.clear();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        if (panelWidth < 100 || panelHeight < 100) return;
        
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

        int spawnRate = Math.max(1, 4 - currentStage);
        spawnCounter++;
        if (spawnCounter >= spawnRate) {
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

        g.setColor(hitCooldown ? Color.GRAY : Color.RED);
        g.fillRect(playerX, playerY, 20, 20);

        g.setColor(Color.YELLOW);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        g.drawString("[" + currentTopicId.toUpperCase() + " / Stage " + currentStage + "] 회피 중! 피격: " + hitsTaken, 25, 38);
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