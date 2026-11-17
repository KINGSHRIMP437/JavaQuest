import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameFrame extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainContainer = new JPanel(cardLayout);

    private TitleScreen titleScreen;
    private TopicSelectScreen topicSelectScreen;
    private StageMapScreen stageMapScreen;
    private BattleScreen battleScreen;

    private String selectedTopic = "coding";
    private int currentPlayingStage = 1; // 현재 플레이 중인 스테이지 번호 추적

    // 주제별·스테이지별 최고 별점 저장 (Key: "topicId_stageNo", Value: 별점 1~3)
    private Map<String, Integer> stageBestStars = new HashMap<>();

    public GameFrame() {
        setTitle("JavaQuest - RPG & Quiz");
        setSize(1050, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 1. 화면 인스턴스 생성
        titleScreen = new TitleScreen(() -> cardLayout.show(mainContainer, "topicSelect"));
        
        topicSelectScreen = new TopicSelectScreen(topicId -> {
            selectedTopic = topicId;
            openStageMap(selectedTopic);
        });

        battleScreen = new BattleScreen();
        
        // 전투 종료 리스너 설정 (승리 시 별점 기록 및 맵 갱신)
        battleScreen.setBattleListener(win -> {
            if (win) {
                int remainingHearts = battleScreen.getPlayerHearts();
                recordStageResult(selectedTopic, currentPlayingStage, win, remainingHearts);
            }
            openStageMap(selectedTopic);
            cardLayout.show(mainContainer, "stageMap");
            SoundManager.playBgm(SoundManager.BGM_MAIN); // 전투가 끝나 맵으로 오면 브금 다시
        });

        // 2. 컨테이너에 등록
        mainContainer.add(titleScreen, "title");
        mainContainer.add(topicSelectScreen, "topicSelect");
        mainContainer.add(battleScreen, "battle");

        add(mainContainer);
        cardLayout.show(mainContainer, "title");
        setVisible(true);

        // 소리: 모든 버튼 클릭음 + 타이틀~스테이지 맵 브금
        SoundManager.installButtonClickSound();
        SoundManager.playBgm(SoundManager.BGM_MAIN);
    }

    // 별점 계산 및 최고 기록 갱신 메서드
    public void recordStageResult(String topicId, int stageNo, boolean won, int remainingHearts) {
        if (!won) return; // 패배하거나 도망치면 기록하지 않음

        int stars = 1;
        if (remainingHearts >= 3) {
            stars = 3;
        } else if (remainingHearts == 2) {
            stars = 2;
        } else {
            stars = 1;
        }

        String key = topicId + "_" + stageNo;
        int currentBest = stageBestStars.getOrDefault(key, 0);
        if (stars > currentBest) {
            stageBestStars.put(key, stars); // 최고 기록보다 높을 때만 갱신
        }
    }

    private void openStageMap(String topicId) {
        if (stageMapScreen != null) {
            mainContainer.remove(stageMapScreen);
        }

        stageMapScreen = new StageMapScreen(topicId, stageBestStars, stageNo -> {
            currentPlayingStage = stageNo; // 어떤 스테이지를 선택했는지 저장
            
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