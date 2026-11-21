import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Map;
import java.util.function.Consumer;

public class TopicSelectScreen extends JPanel {
    private static final Color BG_COLOR = new Color(15, 20, 35);
    private static final Color BORDER_COLOR = new Color(0, 200, 190);

    private static final String[][] TOPICS = {
        {"코딩", "자바, 파이썬 등\n프로그래밍 문제", "coding"},
        {"상식", "일상 및 일반\n상식 문제", "common"},
        {"이름 맞추기", "사진을 보고\n인물·포켓몬 맞추기", "name"}
    };

    private final Map<String, Integer> stageBestStars;   // GameFrame의 클리어 기록 (Key: "topicId_stageNo")
    private final JButton[] topicBtns = new JButton[TOPICS.length];

    // 주제 선택 시 어떤 주제가 선택되었는지 콜백으로 전달 (예: "coding", "common", "name")
    public TopicSelectScreen(Map<String, Integer> stageBestStars, Consumer<String> onTopicSelected) {
        this.stageBestStars = stageBestStars;
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

        for (int i = 0; i < TOPICS.length; i++) {
            JButton topicBtn = new JButton();
            topicBtn.setBackground(new Color(25, 30, 50));
            topicBtn.setForeground(Color.WHITE);
            topicBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
            topicBtn.setFocusPainted(false);

            String topicId = TOPICS[i][2];
            topicBtn.addActionListener(e -> onTopicSelected.accept(topicId));
            topicBtns[i] = topicBtn;
            cardPanel.add(topicBtn);
        }

        add(cardPanel, BorderLayout.CENTER);
        refresh();
    }

    // 클리어 기록에 맞춰 카드 다시 그리기 (주제 선택 화면을 보여 줄 때마다 호출)
    // 스테이지 맵과 같은 규칙: 올클리어한 주제는 제목 초록 + [CLEAR] + 별, 테두리는 회색 / 아직이면 민트 테두리
    public void refresh() {
        for (int i = 0; i < TOPICS.length; i++) {
            String[] t = TOPICS[i];
            int[] stars = new int[3];
            boolean allCleared = true;
            for (int s = 0; s < 3; s++) {
                Integer v = stageBestStars.get(t[2] + "_" + (s + 1));
                if (v == null) allCleared = false;
                else stars[s] = v;
            }

            StringBuilder html = new StringBuilder("<html><center>");
            if (allCleared) {
                html.append("<b><font size='6' color='#64FF96'>").append(t[0]).append(" [CLEAR]</font></b><br>")
                    .append(starHtml(stars));
            } else {
                html.append("<b><font size='6'>").append(t[0]).append("</font></b><br>");
            }
            html.append("<br>").append(t[1].replace("\n", "<br>")).append("</center></html>");

            topicBtns[i].setText(html.toString());
            topicBtns[i].setBorder(allCleared ? new LineBorder(Color.GRAY, 1) : new LineBorder(BORDER_COLOR, 2));
        }
    }

    // 스테이지 1~3 최고 별점의 평균(반올림)을 별 3칸으로 표시 (얻은 별 금색, 나머지 회색). 예: 3·2·1 → 평균 2 → ★★☆
    private static String starHtml(int[] stars) {
        int avg = (int) Math.round((stars[0] + stars[1] + stars[2]) / 3.0);
        StringBuilder sb = new StringBuilder("<span style='font-size:24pt;'>");
        for (int k = 1; k <= 3; k++) {
            sb.append(k <= avg ? "<font color='#FFD700'>★</font>" : "<font color='#555555'>☆</font>");
        }
        sb.append("</span><br>");
        return sb.toString();
    }
}
