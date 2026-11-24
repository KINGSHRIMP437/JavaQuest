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
    private final JLabel[] titleLabels = new JLabel[TOPICS.length];
    private final JLabel[] descLabels = new JLabel[TOPICS.length];

    // 주제 선택 시 어떤 주제가 선택되었는지 콜백으로 전달 (예: "coding", "common", "name"), 왼쪽 위 `타이틀` 버튼은 onBackToTitle
    public TopicSelectScreen(Map<String, Integer> stageBestStars, Consumer<String> onTopicSelected, Runnable onBackToTitle) {
        this.stageBestStars = stageBestStars;
        setPreferredSize(new Dimension(1050, 800));
        setBackground(BG_COLOR);
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JLabel titleLabel = new JLabel("모험할 주제를 선택하세요", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 28));

        // 왼쪽 위: 타이틀로 돌아가기 (스테이지 맵의 `주제 변경` 버튼과 같은 모양)
        JButton backBtn = new JButton("Title");
        backBtn.setBackground(new Color(25, 30, 50));
        backBtn.setForeground(Color.YELLOW);
        backBtn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        backBtn.setBorder(new LineBorder(Color.GRAY, 1));
        backBtn.setFocusPainted(false);
        backBtn.addActionListener(e -> onBackToTitle.run());
        backBtn.setPreferredSize(new Dimension(63, 36));   // 스테이지 맵 `주제 변경` 버튼과 같은 크기
        JPanel backWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));   // 위쪽 칸 높이에 맞춰 늘어나지 않게
        backWrap.setOpaque(false);
        backWrap.add(backBtn);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.add(backWrap, BorderLayout.WEST);
        add(topPanel, BorderLayout.NORTH);

        // 3개의 주제 카드 배치를 위한 패널 (StageMapScreen과 동일한 크기·간격)
        JPanel cardPanel = new JPanel(new GridLayout(1, 3, 30, 0));
        cardPanel.setOpaque(false);
        cardPanel.setBorder(BorderFactory.createEmptyBorder(60, 40, 60, 40));

        for (int i = 0; i < TOPICS.length; i++) {
            JButton topicBtn = new JButton();
            topicBtn.setBackground(new Color(25, 30, 50));
            topicBtn.setLayout(new BorderLayout());
            topicBtn.setFocusPainted(false);

            // 상단: 주제 제목 (클리어 시 [CLEAR] + 별점)
            JLabel cardTitle = new JLabel("", SwingConstants.CENTER);
            cardTitle.setForeground(Color.WHITE);
            cardTitle.setBorder(BorderFactory.createEmptyBorder(45, 10, 0, 10));

            // 중앙: 설명글 (스테이지 선택 화면과 같은 카드 정중앙 위치)
            JLabel cardDesc = new JLabel("", SwingConstants.CENTER);
            cardDesc.setForeground(Color.WHITE);
            cardDesc.setFont(new Font("맑은 고딕", Font.PLAIN, 22));
            cardDesc.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));

            // 하단 여백: 스테이지 카드의 '입장하기' 버튼 영역 높이와 맞춰 설명글을 정중앙에 고정
            Component bottomSpacer = Box.createVerticalStrut(72);

            topicBtn.add(cardTitle, BorderLayout.NORTH);
            topicBtn.add(cardDesc, BorderLayout.CENTER);
            topicBtn.add(bottomSpacer, BorderLayout.SOUTH);

            String topicId = TOPICS[i][2];
            topicBtn.addActionListener(e -> onTopicSelected.accept(topicId));
            topicBtns[i] = topicBtn;
            titleLabels[i] = cardTitle;
            descLabels[i] = cardDesc;
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

            StringBuilder titleHtml = new StringBuilder("<html><center>");
            if (allCleared) {
                titleHtml.append("<span style='font-size:30pt; font-weight:bold; color:#64FF96;'>")
                         .append(t[0]).append(" [CLEAR]</span><br>")
                         .append(starHtml(stars));
            } else {
                titleHtml.append("<span style='font-size:34pt; font-weight:bold; color:#FFFFFF;'>")
                         .append(t[0]).append("</span>");
            }
            titleHtml.append("</center></html>");

            titleLabels[i].setText(titleHtml.toString());
            descLabels[i].setText("<html><center>" + t[1].replace("\n", "<br>") + "</center></html>");

            LineBorder line = allCleared ? new LineBorder(Color.GRAY, 1) : new LineBorder(BORDER_COLOR, 2);
            topicBtns[i].setBorder(line);
        }
    }

    // 스테이지 1~3 최고 별점의 평균(반올림)을 별 3칸으로 표시 (얻은 별 금색, 나머지 회색). 예: 3·2·1 → 평균 2 → ★★☆
    private static String starHtml(int[] stars) {
        int avg = (int) Math.round((stars[0] + stars[1] + stars[2]) / 3.0);
        StringBuilder sb = new StringBuilder("<span style='font-size:26pt;'>");
        for (int k = 1; k <= 3; k++) {
            sb.append(k <= avg ? "<font color='#FFD700'>★</font>" : "<font color='#555555'>☆</font>");
        }
        sb.append("</span>");
        return sb.toString();
    }
}