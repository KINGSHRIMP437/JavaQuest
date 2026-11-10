import javax.swing.*;
import java.awt.*;
import java.util.List;

public class GameFrame extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainContainer = new JPanel(cardLayout);

    private TitleScreen titleScreen;
    private TopicSelectScreen topicSelectScreen;
    private StageMapScreen stageMapScreen;
    private BattleScreen battleScreen;

    private String selectedTopic = "coding";

    public GameFrame() {
        setTitle("JavaQuest - RPG & Quiz");
        setSize(1050, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 1. 화면 인스턴스 생성
        titleScreen = new TitleScreen(() -> cardLayout.show(mainContainer, "topicSelect"));
        
        // GameFrame.java의 topicSelectScreen 생성 부분을 아래와 같이 수정하세요.
        topicSelectScreen = new TopicSelectScreen(topicId -> {
            selectedTopic = topicId;
            openStageMap(selectedTopic);
        });

        battleScreen = new BattleScreen();
        battleScreen.setBattleListener(win -> {
            cardLayout.show(mainContainer, "stageMap");
        });

        // 2. 컨테이너에 등록
        mainContainer.add(titleScreen, "title");
        mainContainer.add(topicSelectScreen, "topicSelect");
        mainContainer.add(battleScreen, "battle");

        add(mainContainer);
        cardLayout.show(mainContainer, "title");
        setVisible(true);
    }

    private void openStageMap(String topicId) {
        if (stageMapScreen != null) {
            mainContainer.remove(stageMapScreen);
        }

        stageMapScreen = new StageMapScreen(topicId, stageNo -> {
            // 텍스트 파일로부터 주제별/난이도별 문제 목록과 스테이지(보스 이름·HP) 동적 로드
            List<QuizItem> questions = DataLoader.loadQuestions("questions.txt", topicId, stageNo);
            Stage stage = DataLoader.loadStage("monsters.txt", topicId, stageNo);

            battleScreen.startBattle(getTopicTitle(topicId), stage, questions);
            cardLayout.show(mainContainer, "battle");
        }, () -> {
            cardLayout.show(mainContainer, "topicSelect");
        });

        mainContainer.add(stageMapScreen, "stageMap");
        mainContainer.revalidate();
        mainContainer.repaint();
        cardLayout.show(mainContainer, "stageMap");
    }

    private String getTopicTitle(String topicId) {
        switch (topicId) {
            case "common": return "상식 퀴즈";
            case "name": return "이름 맞추기";
            default: return "프로그래밍 코딩";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameFrame::new);
    }
}