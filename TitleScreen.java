import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

public class TitleScreen extends JPanel {
    private static final Color BG_COLOR = new Color(15, 20, 35);
    private static final Color BORDER_COLOR = new Color(0, 200, 190);

    public TitleScreen(Runnable onStartClicked) {
        setPreferredSize(new Dimension(1050, 800));
        setBackground(BG_COLOR);
        setLayout(new GridBagLayout());

        JPanel container = new JPanel();
        container.setOpaque(false);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        // 게임 타이틀 로고
        JLabel titleLabel = new JLabel("JavaQuest", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 52));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("프로그래밍 & 지식 모험 RPG", SwingConstants.CENTER);
        subLabel.setForeground(BORDER_COLOR);
        subLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 게임 시작 버튼
        JButton startBtn = new JButton("게임 시작");
        startBtn.setBackground(new Color(25, 30, 50));
        startBtn.setForeground(Color.YELLOW);
        startBtn.setFont(new Font("맑은 고딕", Font.BOLD, 22));
        startBtn.setBorder(new LineBorder(BORDER_COLOR, 2));
        startBtn.setFocusPainted(false);
        startBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        startBtn.setMaximumSize(new Dimension(250, 60));
        startBtn.addActionListener(e -> onStartClicked.run());

        container.add(titleLabel);
        container.add(Box.createVerticalStrut(10));
        container.add(subLabel);
        container.add(Box.createVerticalStrut(60));
        container.add(startBtn);

        add(container);
    }
}