import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.function.Consumer;

public class StageMapScreen extends JPanel {
    private static final Color BG_COLOR = new Color(15, 20, 35);
    private static final Color BORDER_COLOR = new Color(0, 200, 190);

    // stageNo (1, 2, 3)를 전달받아 전투 화면으로 진입
    public StageMapScreen(String currentTopic, Consumer<Integer> onStageSelected, Runnable onBackToTopic) {
        setPreferredSize(new Dimension(1050, 800));
        setBackground(BG_COLOR);
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        // 상단 타이틀 및 뒤로가기 버튼
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("스테이지 맵 — [" + getTopicTitle(currentTopic) + "]", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 26));
        topPanel.add(titleLabel, BorderLayout.CENTER);

        JButton backBtn = new JButton("주제 변경");
        backBtn.setBackground(new Color(25, 30, 50));
        backBtn.setForeground(Color.YELLOW);
        backBtn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        backBtn.setBorder(new LineBorder(Color.GRAY, 1));
        backBtn.setFocusPainted(false);
        backBtn.addActionListener(e -> onBackToTopic.run());
        topPanel.add(backBtn, BorderLayout.WEST);

        add(topPanel, BorderLayout.NORTH);

        // 스테이지 3개 목록 (길처럼 이어지는 카드형태 구조)
        JPanel stagesPanel = new JPanel(new GridLayout(1, 3, 30, 0));
        stagesPanel.setOpaque(false);
        stagesPanel.setBorder(BorderFactory.createEmptyBorder(60, 40, 60, 40));

        String[][] stageInfo = {
            {"STAGE 1", "쉬움 난이도\n기본 보스 등장", "1"},
            {"STAGE 2", "중간 난이도\n탄막 속도 증가", "2"},
            {"STAGE 3", "어려움 난이도\n최종 보스전", "3"}
        };

        for (int i = 0; i < 3; i++) {
            JPanel stageCard = new JPanel(new BorderLayout());
            stageCard.setBackground(new Color(20, 25, 45));
            stageCard.setBorder(new LineBorder(i == 0 ? BORDER_COLOR : Color.GRAY, i == 0 ? 2 : 1));
            
            JLabel sTitle = new JLabel(stageInfo[i][0], SwingConstants.CENTER);
            sTitle.setForeground(Color.YELLOW);
            sTitle.setFont(new Font("맑은 고딕", Font.BOLD, 20));
            sTitle.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));

            JTextArea sDesc = new JTextArea(stageInfo[i][1]);
            sDesc.setEditable(false);
            sDesc.setOpaque(false);
            sDesc.setForeground(Color.WHITE);
            sDesc.setFont(new Font("맑은 고딕", Font.PLAIN, 15));
            sDesc.setMargin(new Insets(10, 20, 10, 20));

            int stageNum = Integer.parseInt(stageInfo[i][2]);
            JButton enterBtn = new JButton("입장하기");
            enterBtn.setBackground(new Color(25, 30, 50));
            enterBtn.setForeground(Color.WHITE);
            enterBtn.setFont(new Font("맑은 고딕", Font.BOLD, 16));
            enterBtn.setBorder(new LineBorder(BORDER_COLOR, 1));
            enterBtn.setFocusPainted(false);
            enterBtn.addActionListener(e -> onStageSelected.accept(stageNum));

            JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
            btnWrapper.setOpaque(false);
            btnWrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
            btnWrapper.add(enterBtn);

            stageCard.add(sTitle, BorderLayout.NORTH);
            stageCard.add(sDesc, BorderLayout.CENTER);
            stageCard.add(btnWrapper, BorderLayout.SOUTH);

            stagesPanel.add(stageCard);
        }

        add(stagesPanel, BorderLayout.CENTER);
    }

    private String getTopicTitle(String id) {
        if ("coding".equals(id)) return "코딩";
        if ("common".equals(id)) return "상식";
        if ("name".equals(id)) return "이름 맞추기";
        return "모험";
    }
}