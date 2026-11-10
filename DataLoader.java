import java.util.ArrayList;
import java.util.List;

// 프론트(GameFrame)와 백엔드 Manager를 잇는 창구. 파일은 처음 한 번만 읽는다.
public class DataLoader {
    private static QuestionManager questionManager;
    private static StageManager stageManager;

    // 주제와 스테이지에 맞는 문제 목록
    public static List<QuizItem> loadQuestions(String filePath, String targetTopic, int targetStage) {
        if (questionManager == null) questionManager = new QuestionManager(filePath);

        List<QuizItem> questionList = new ArrayList<>();
        for (Question q : questionManager.findByStage(targetTopic, targetStage))
            questionList.add(q.toQuizItem());

        // 데이터가 없을 경우를 대비한 안전 장치
        if (questionList.isEmpty()) {
            questionList.add(new QuizItem("기본 문제: 자바의 프로그램 시작 메서드는?",
                new String[]{"start()", "main()", "run()", "init()"},
                1, "자바 앱은 main 메서드에서 시작합니다.", null));
        }
        return questionList;
    }

    // 주제와 스테이지 번호에 맞는 스테이지 정보 (보스 이름, HP 비율)
    public static Stage loadStage(String filePath, String targetTopic, int targetStage) {
        if (stageManager == null) stageManager = new StageManager(filePath);
        Stage stage = stageManager.findStage(targetTopic, targetStage);
        // 데이터가 없을 경우를 대비한 안전 장치
        return stage != null ? stage : new Stage(targetTopic, targetStage, "Unknown Boss", 50);
    }
}
