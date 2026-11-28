import java.util.Scanner;

// 끝에 SHORT_ANSWER 또는 INITIAL_CONSONANT를 붙이면 입력형 퀴즈로 출제 (생략 시 객관식)
public class Question implements Manageable {
    int difficulty;   // 1, 2, 3 이상 → 스테이지 1, 2, 3
    String topic;     // coding / common / name
    String text;
    String[] choices = new String[4];
    int answer;       // 0~3
    String explain;
    QuizItem.Type type = QuizItem.Type.MULTIPLE_CHOICE;

    @Override
    public void read(Scanner scan) {
        difficulty = Integer.parseInt(scan.next().trim());
        topic = scan.next().trim();
        text = scan.next().trim();
        for (int i = 0; i < 4; i++) choices[i] = scan.next().trim();
        answer = Integer.parseInt(scan.next().trim());
        explain = scan.next().trim();
        if (answer < 0 || answer > 3) throw new IllegalArgumentException("정답 인덱스는 0~3");
        if (scan.hasNext()) {
            String rawType = scan.next().trim();
            if (!rawType.isEmpty()) type = QuizItem.Type.valueOf(rawType.toUpperCase());
        }
    }

    @Override
    public void print() {
        System.out.printf("[%s %d] %s (정답 %d번)%n", topic, getStage(), text, answer + 1);
    }

    @Override
    public boolean matches(String kwd) {
        return topic.equalsIgnoreCase(kwd) || text.contains(kwd);
    }

    public int getStage() { return Math.min(difficulty, 3); }

    // 프론트(BattleScreen)가 쓰는 형태로 변환
    public QuizItem toQuizItem() {
        return new QuizItem(text, choices.clone(), answer, explain, null, type);
    }
}
