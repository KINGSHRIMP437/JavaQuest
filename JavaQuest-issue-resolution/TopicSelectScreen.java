import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.function.Consumer;

public class TopicSelectScreen extends JPanel {
    private static final Color BG_COLOR = new Color(15, 20, 35);
    private static final Color BORDER_COLOR = new Color(0, 200, 190);

    // 주제 선택 시 어떤 주제가 선택되었는지 콜백으로 전달 (예: "coding", "common", "name")
    public TopicSelectScreen(Consumer<String> onTopicSelected) {
        setPreferredSize(new Dimension(1050, 800));
        setBackground(BG_COLOR);
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JLabel titleLabel = new JLabel("모험할 주제를 선택하세요", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 28));
        add(titleLabel, BorderLayout.NORTH);

        // 3개의 주제 카드 배치를 위한 패널
        JPanel cardPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        cardPanel.setOpaque(false);
        cardPanel.setBorder(BorderFactory.createEmptyBorder(40, 20, 40, 20));

        String[][] topics = {
            {"코딩", "자바, 파이썬 등\n프로그래밍 문제", "coding"},
            {"상식", "일상 및 일반\n상식 문제", "common"},
            {"이름 맞추기", "사진을 보고\n인물·포켓몬 맞추기", "name"}
        };

        for (String[] t : topics) {
            JButton topicBtn = new JButton("<html><center><b><font size='6'>" + t[0] + "</font></b><br><br>" + t[1].replace("\n", "<br>") + "</center></html>");
            topicBtn.setBackground(new Color(25, 30, 50));
            topicBtn.setForeground(Color.WHITE);
            topicBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
            topicBtn.setBorder(new LineBorder(BORDER_COLOR, 2));
            topicBtn.setFocusPainted(false);
            
            String topicId = t[2];
            topicBtn.addActionListener(e -> onTopicSelected.accept(topicId));
            cardPanel.add(topicBtn);
        }

        add(cardPanel, BorderLayout.CENTER);
    }
}