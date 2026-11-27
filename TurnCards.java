import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

// 전투 화면 오른쪽 위의 턴 카드 두 장: 나의 턴(위, 민트)·적의 턴(아래, 주황·보스 얼굴)
// 턴이 바뀌면 그 턴 카드가 오른쪽 끝에서 튀어나왔다가(살짝 튕김 + 빛 훑기) 1초 뒤 다시 들어감.
// 들어간 뒤에도 지금 턴 카드는 끝에 조금 더 밝게 남아 있어서 누구 턴인지 계속 보임
public class TurnCards {
    static final Color MY_COLOR = new Color(0, 210, 195), ENEMY_COLOR = new Color(255, 145, 40);
    static final int CARD_W = 290, CARD_H = 72, SKEW = 22, MY_Y = 66, EN_Y = 146;
    static final int PEEK = 40, REST = 70;            // 끝에 숨어 있을 때 보이는 폭: 지난 턴 40, 지금 턴 70
    static final int OUT_MS = 280, HOLD_MS = 1000, BACK_MS = 300, PREV_MS = 150, SHINE_MS = 450;

    private final JComponent owner;   // 카드를 그리는 화면 (애니메이션 동안 그 부분만 다시 그림)
    private final Timer anim;
    private boolean mine = true;
    private boolean started = false;  // 아직 아무 턴도 없으면 두 카드 모두 숨김
    private long start;
    private int top = MY_Y;   // 나의 턴 카드 위쪽 y (적의 턴 카드는 그 아래 EN_Y - MY_Y만큼)

    public TurnCards(JComponent owner) {
        this.owner = owner;
        anim = new Timer(16, e -> {
            if (elapsed() > OUT_MS + HOLD_MS + BACK_MS) ((Timer) e.getSource()).stop();
            repaintArea();
        });
    }

    // 카드 위치: 화면의 다른 글자 아래로 내릴 때 (MY_Y보다 위로는 안 올림)
    public void setTop(int y) { top = Math.max(MY_Y, y); }

    // 턴이 바뀜: 그 턴 카드가 튀어나옴
    public void show(boolean mine) {
        this.mine = mine;
        started = true;
        start = System.currentTimeMillis();
        anim.restart();
        repaintArea();
    }

    private long elapsed() { return System.currentTimeMillis() - start; }

    private void repaintArea() {
        int w = owner.getWidth();
        owner.repaint(w - CARD_W - 40, top - 10, CARD_W + 40, EN_Y + CARD_H - MY_Y + 20);
    }

    private static double easeOutBack(double p) { double c1 = 1.70158, c3 = c1 + 1; return 1 + c3 * Math.pow(p - 1, 3) + c1 * Math.pow(p - 1, 2); }

    // 이번 턴 카드가 오른쪽 끝에서 나와 있는 폭
    private double activeWidth(long t) {
        if (t < OUT_MS) return REST + (CARD_W - REST) * easeOutBack(t / (double) OUT_MS);
        if (t < OUT_MS + HOLD_MS) return CARD_W;
        if (t < OUT_MS + HOLD_MS + BACK_MS) { double p = (t - OUT_MS - HOLD_MS) / (double) BACK_MS; return CARD_W - (CARD_W - REST) * p * p * p; }
        return REST;
    }

    // 지난 턴 카드: 끝으로 빠르게 들어감
    private double prevWidth(long t) { return t >= PREV_MS ? PEEK : REST - (REST - PEEK) * (t / (double) PREV_MS); }

    // owner의 paintChildren 끝에서 부름 (다른 화면 요소 위에 그려짐)
    public void paint(Graphics g, int w, BufferedImage bossImage) {
        if (!started) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        long t = elapsed();
        double aw = activeWidth(t), pw = prevWidth(t);
        double shine = t >= OUT_MS && t < OUT_MS + SHINE_MS ? (t - OUT_MS) / (double) SHINE_MS : -1;
        card(g2, w, top, mine ? aw : pw, true, mine, mine ? shine : -1, bossImage);
        card(g2, w, top + EN_Y - MY_Y, mine ? pw : aw, false, !mine, mine ? -1 : shine, bossImage);
        g2.dispose();
    }

    // 오른쪽 끝에서 visible 만큼 나온 비스듬한 카드
    private void card(Graphics2D g0, int w, int y, double visible, boolean myCard, boolean active, double shine, BufferedImage boss) {
        Graphics2D g = (Graphics2D) g0.create();
        double x = w - visible;
        Color acc = myCard ? MY_COLOR : ENEMY_COLOR;
        Path2D shape = new Path2D.Double();
        shape.moveTo(x + SKEW, y); shape.lineTo(w + 10, y); shape.lineTo(w + 10, y + CARD_H); shape.lineTo(x, y + CARD_H); shape.closePath();
        if (!active) g.setComposite(AlphaComposite.SrcOver.derive(0.55f));

        g.setColor(new Color(0, 0, 0, 120));   // 그림자
        g.fill(AffineTransform.getTranslateInstance(-6, 6).createTransformedShape(shape));
        g.setPaint(new GradientPaint((float) x, y, new Color(28, 34, 58), (float) (x + CARD_W), y + CARD_H, new Color(12, 15, 28)));
        g.fill(shape);

        Shape oldClip = g.getClip();
        g.clip(shape);
        // 초상화 칸 (카드 왼쪽)
        int px = (int) x, pw = 100;
        g.setPaint(new GradientPaint(px, y, alpha(acc, 120), px + pw, y + CARD_H, alpha(acc, 20)));
        g.fillRect(px, y, pw, CARD_H);
        if (!myCard && boss != null) {   // 보스 얼굴: 칸 안에만 보이게 꽉 채워서
            Shape c2 = g.getClip();
            g.clipRect(px, y, pw, CARD_H);
            double sc = Math.max((pw - 10) / (double) boss.getWidth(), (CARD_H + 10) / (double) boss.getHeight()) * 1.15;
            int bw = (int) (boss.getWidth() * sc), bh = (int) (boss.getHeight() * sc);
            g.drawImage(boss, px + 12 + (pw - bw) / 2, y + (CARD_H - bh) / 2 + 4, bw, bh, null);
            g.setClip(c2);
        } else {                         // 나의 턴(또는 보스 그림이 없을 때): 기호
            g.setColor(Color.WHITE);
            g.setFont(new Font("Segoe UI Symbol", Font.BOLD, 46));
            g.drawString(myCard ? "⚔" : "⚠", px + 28, y + 54);
        }
        g.setColor(acc);
        g.fillRect(px, y, CARD_W + 20, 4);   // 위쪽 색 줄
        g.setColor(Color.WHITE);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 26));
        g.drawString(myCard ? "나의 턴" : "적의 턴", px + pw + 12, y + 38);
        g.setColor(acc);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        g.drawString(myCard ? "문제를 풀어 공격!" : "방향키로 피하기!", px + pw + 14, y + 60);
        if (shine >= 0) {                    // 빛이 카드 위를 한 번 훑고 지나감
            double sx = x + (CARD_W + 120) * shine - 60;
            g.setPaint(new GradientPaint((float) sx - 40, 0, new Color(255, 255, 255, 0), (float) sx, 0, new Color(255, 255, 255, 110), true));
            Path2D band = new Path2D.Double();
            band.moveTo(sx - 30, y); band.lineTo(sx + 10, y); band.lineTo(sx - 20, y + CARD_H); band.lineTo(sx - 60, y + CARD_H); band.closePath();
            g.fill(band);
        }
        g.setClip(oldClip);
        g.setColor(active ? acc : new Color(90, 95, 110));
        g.setStroke(new BasicStroke(active ? 2.5f : 1.5f));
        g.draw(shape);
        g.dispose();
    }

    private static Color alpha(Color c, int a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), a); }
}
