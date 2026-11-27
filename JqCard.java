import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

// 모서리가 둥근 카드 패널 (스테이지 카드, 진행도 배지, "수호자: ..." 알약 표시 등에 사용)
// JPanel을 상속해서 배경과 테두리만 직접 그리고, 안에 넣는 컴포넌트 배치는 JPanel 그대로 쓴다.
public class JqCard extends JPanel {
    private Color fill;          // 배경색 (null이면 투명)
    private Color line;          // 테두리색 (null이면 테두리 없음)
    private int lineWidth;
    private final int arc;       // 모서리 둥글기

    public JqCard(Color fill, Color line, int lineWidth, int arc) {
        this.fill = fill;
        this.line = line;
        this.lineWidth = lineWidth;
        this.arc = arc;
        setOpaque(false);   // 둥근 모서리 바깥은 뒤 배경(파티클)이 보이게
    }

    public void setFill(Color fill) {
        this.fill = fill;
        repaint();
    }

    public void setLine(Color line, int lineWidth) {
        this.line = line;
        this.lineWidth = lineWidth;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        if (fill != null) {
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w, h, arc, arc);
        }
        if (line != null && lineWidth > 0) {
            g2.setColor(line);
            g2.setStroke(new BasicStroke(lineWidth));
            float half = lineWidth / 2f;   // 선 두께의 절반만큼 안쪽에 그려야 잘리지 않음
            g2.draw(new RoundRectangle2D.Float(half, half, w - lineWidth, h - lineWidth, arc, arc));
        }
        g2.dispose();
    }
}
