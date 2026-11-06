import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataLoader {

    // 1. questions.txt 파일에서 주제와 스테이지 난이도에 맞는 문제들을 로드
    public static List<QuizItem> loadQuestions(String filePath, String targetTopic, int targetStage) {
        List<QuizItem> questionList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\|");
                if (parts.length >= 9) {
                    int difficulty = Integer.parseInt(parts[0].trim()); // 난이도
                    String topic = parts[1].trim();                    // 주제 ID

                    // 스테이지별 난이도 필터링 조건
                    boolean stageCondition = false;
                    if (targetStage == 1) {
                        stageCondition = (difficulty == 1);
                    } else if (targetStage == 2) {
                        stageCondition = (difficulty == 2);
                    } else {
                        stageCondition = (difficulty >= 3);
                    }

                    if (topic.equalsIgnoreCase(targetTopic) && stageCondition) {
                        String text = parts[2].trim();
                        String[] choices = { parts[3].trim(), parts[4].trim(), parts[5].trim(), parts[6].trim() };
                        int answer = Integer.parseInt(parts[7].trim());
                        String explain = parts[8].trim();

                        questionList.add(new QuizItem(text, choices, answer, explain, null));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("questions.txt 파일 읽기 오류: " + e.getMessage());
        }

        // 데이터가 없을 경우를 대비한 안전 장치
        if (questionList.isEmpty()) {
            questionList.add(new QuizItem("기본 문제: 자바의 프로그램 시작 메서드는?", 
                new String[]{"start()", "main()", "run()", "init()"}, 
                1, "자바 앱은 main 메서드에서 시작합니다.", null));
        }

        return questionList;
    }

    // 2. monsters.txt 파일에서 주제와 스테이지 번호에 맞는 보스 이름을 로드
    public static String loadBossName(String filePath, String targetTopic, int targetStage) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\|");
                if (parts.length >= 5) {
                    String topic = parts[0].trim();
                    int stage = Integer.parseInt(parts[1].trim());

                    if (topic.equalsIgnoreCase(targetTopic) && stage == targetStage) {
                        return parts[2].trim(); // 보스 이름 반환
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("monsters.txt 파일을 읽기 오류: " + e.getMessage());
        }

        return "Unknown Boss";
    }
}