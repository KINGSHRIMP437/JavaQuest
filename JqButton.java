import javax.swing.*;
import java.awt.*;

// 모서리가 둥글고, 마우스를 올리면 밝아지면서 민트색으로 빛나는 버튼
// 기본 JButton은 운영체제/룩앤필마다 모양이 달라서 배경과 테두리를 직접 그린다.
// 글자와 아이콘은 JButton(super)이 그대로 그리므로 ActionListener 등 사용법은 일반 버튼과 같다.
public class JqButton extends JButton {
    private static final int ARC = 14;

    private Color normalFill = UiTheme.BUTTON;
    private Color hoverFill = UiTheme.BUTTON_HOVER;
    private Color line;
    private int lineWidth;

    public JqButton(String text, Color textColor, Color line, int lineWidth, int fontSize) {
        super(text);
        this.line = line;
        this.lineWidth = lineWidth;
        setForeground(textColor);
        setFont(UiTheme.font(Font.BOLD, fontSize));
        setContentAreaFilled(false);   // 룩앤필 기본 배경 대신 paintComponent에서 직접 칠함
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);      // 마우스 올림 상태(getModel().isRollover())를 쓰기 위해
        setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));   // 안쪽 여백
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // 카드형 버튼(주제 카드)처럼 평소/마우스 올림 배경색을 바꿀 때
    public void setFills(Color normalFill, Color hoverFill) {
        this.normalFill = normalFill;
        this.hoverFill = hoverFill;
        repaint();
    }

    public void setLine(Color line, int lineWidth) {
        this.line = line;
        this.lineWidth = lineWidth;
        repaint();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        ButtonModel m = getModel();
        boolean hover = isEnabled() && m.isRollover();

        // 배경: 비활성 → 어둡게, 누름 → 평소색, 마우스 올림 → 밝게
        Color fill = !isEnabled() ? new Color(20, 24, 41)
                : (m.isPressed() ? normalFill : (hover ? hoverFill : normalFill));
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, w, h, ARC, ARC);

        // 마우스를 올리면 테두리 안쪽으로 민트빛이 번지는 효과
        if (hover) {
            for (int i = 1; i <= 4; i++) {
                g2.setColor(new Color(0, 200, 190, 34 - i * 7));
                g2.setStroke(new BasicStroke(lineWidth + i * 2));
                g2.drawRoundRect(0, 0, w - 1, h - 1, ARC, ARC);
            }
        }

        // 테두리 (마우스를 올리면 진한 민트)
        Color border = !isEnabled() ? new Color(51, 65, 85) : (hover ? UiTheme.MINT : line);
        int bw = Math.max(1, lineWidth);
        if (border != null) {
            g2.setColor(border);
            g2.setStroke(new BasicStroke(bw));
            g2.drawRoundRect(bw / 2, bw / 2, w - bw, h - bw, ARC, ARC);
        }
        g2.dispose();

        super.paintComponent(g);   // 글자·아이콘은 JButton이 그림
    }
}
