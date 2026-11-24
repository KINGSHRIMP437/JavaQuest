import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameFrame extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainContainer = new JPanel(cardLayout);

    private TitleScreen titleScreen;
    private HowToPlayScreen howToPlayScreen;
    private TopicSelectScreen topicSelectScreen;
    private StageMapScreen stageMapScreen;
    private BattleScreen battleScreen;
    private TopicClearScreen topicClearScreen;

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
        titleScreen = new TitleScreen(
            this::showTopicSelect,
            () -> cardLayout.show(mainContainer, "howToPlay")
        );

        howToPlayScreen = new HowToPlayScreen(
            () -> cardLayout.show(mainContainer, "title")
        );

        topicSelectScreen = new TopicSelectScreen(stageBestStars, topicId -> {
            selectedTopic = topicId;
            openStageMap(selectedTopic);
        }, () -> cardLayout.show(mainContainer, "title"));   // 타이틀도 메인 브금이라 다시 틀지 않음

        battleScreen = new BattleScreen();
        
        topicClearScreen = new TopicClearScreen(
            () -> openStageMap(selectedTopic),
            this::showTopicSelect);

        // 전투 종료 리스너 설정 (승리 시 별점 기록 및 맵 갱신)
        battleScreen.setBattleListener(win -> {
            if (!win) { // 패배·도망: 창 없이 맵으로
                openStageMap(selectedTopic);
                SoundManager.playBgm(SoundManager.BGM_MAIN); // 전투가 끝나 맵으로 오면 브금 다시
                return;
            }
            int stageNo = currentPlayingStage;
            boolean firstClear = !stageBestStars.containsKey(selectedTopic + "_" + stageNo);
            int stars = recordStageResult(selectedTopic, stageNo, true, battleScreen.getPlayerHearts());

            if (isTopicAllCleared(selectedTopic)) {
                // 마지막 스테이지까지 모두 클리어: 클리어 창 대신 축하 화면 (브금 멈추고 팡파르 → 끝나면 메인 브금)
                showTopicClear(selectedTopic);
                SoundManager.playEffectThenBgm(SoundManager.TOPIC_CLEAR, SoundManager.BGM_MAIN);
                return;
            }
            openStageMap(selectedTopic);
            // 이번에 처음 깼으면 다음 스테이지가 새로 열림 (#17)
            int unlocked = firstClear && stageNo < 3 && !BattleEngine.isGodMode() ? stageNo + 1 : 0;
            StageClearPopup.show(getRootPane(), stageNo, stars, unlocked);
            // 클리어음이 나는 동안은 브금 없음 → 끝나면 메인 브금
            SoundManager.playEffectThenBgm(SoundManager.STAGE_CLEAR, SoundManager.BGM_MAIN);
        });

        // 2. 컨테이너에 등록
        mainContainer.add(titleScreen, "title");
        mainContainer.add(howToPlayScreen, "howToPlay");
        mainContainer.add(topicSelectScreen, "topicSelect");
        mainContainer.add(battleScreen, "battle");
        mainContainer.add(topicClearScreen, "topicClear");

        add(mainContainer);
        cardLayout.show(mainContainer, "title");
        setVisible(true);

        // 소리: 모든 버튼 클릭음 + 타이틀~스테이지 맵 브금
        SoundManager.installButtonClickSound();
        SoundManager.playBgm(SoundManager.BGM_MAIN);
    }

    // 별점 계산 및 최고 기록 갱신 메서드. 이번 판의 별점을 반환 (패배·도망이면 0)
    public int recordStageResult(String topicId, int stageNo, boolean won, int remainingHearts) {
        if (!won) return 0; // 패배하거나 도망치면 기록하지 않음

        int stars = BattleEngine.starsFor(remainingHearts);

        String key = topicId + "_" + stageNo;
        int currentBest = stageBestStars.getOrDefault(key, 0);
        // 무적 모드에서 깬 판은 기록하지 않음 (잠금을 무시하고 깬 기록이 무적 모드를 끈 뒤에도 남아 다음 스테이지가 열리지 않게)
        if (stars > currentBest && !BattleEngine.isGodMode()) {
            stageBestStars.put(key, stars); // 최고 기록보다 높을 때만 갱신
        }
        return stars;
    }

    // 주제 선택 화면: 클리어 기록을 반영해 다시 그린 뒤 표시
    private void showTopicSelect() {
        topicSelectScreen.refresh();
        cardLayout.show(mainContainer, "topicSelect");
    }

    // 주제의 STAGE 1~3을 모두 클리어했는지
    private boolean isTopicAllCleared(String topicId) {
        for (int i = 1; i <= 3; i++) {
            if (!stageBestStars.containsKey(topicId + "_" + i)) return false;
        }
        return true;
    }

    // 주제 올클리어 축하 화면 (스테이지별 최고 별점 요약)
    private void showTopicClear(String topicId) {
        int[] stars = new int[3];
        for (int i = 0; i < 3; i++) stars[i] = stageBestStars.getOrDefault(topicId + "_" + (i + 1), 0);
        topicClearScreen.showResult(getTopicTitle(topicId), stars);
        cardLayout.show(mainContainer, "topicClear");
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
        }, this::showTopicSelect);

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