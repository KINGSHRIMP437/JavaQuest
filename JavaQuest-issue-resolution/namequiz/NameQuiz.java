import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// name_quiz.txt 한 줄: 스테이지|이미지경로|정답들(쉼표로 구분)|해설
public class NameQuiz implements Manageable {
    int stage;
    String imagePath;
    List<String> answers = new ArrayList<>(); // 첫 번째가 대표 정답
    String explain;

    @Override
    public void read(Scanner scan) {
        stage = Integer.parseInt(scan.next().trim());
        imagePath = scan.next().trim();
        answers.clear();
        for (String a : scan.next().split(","))
            if (!a.trim().isEmpty()) answers.add(a.trim());
        explain = scan.hasNext() ? scan.next().trim() : "";
        if (answers.isEmpty()) throw new IllegalArgumentException("정답이 하나 이상 있어야 함");
    }

    @Override
    public void print() {
        System.out.printf("[name %d] %s → %s%n", stage, imagePath, answers);
    }

    @Override
    public boolean matches(String kwd) {
        return isCorrect(kwd);
    }

    // 공백·대소문자 무시, 정답 중 하나와 같으면 정답
    public boolean isCorrect(String input) {
        if (input == null) return false;
        String in = normalize(input);
        if (in.isEmpty()) return false;
        for (String a : answers)
            if (normalize(a).equals(in)) return true;
        return false;
    }

    private static String normalize(String s) {
        return s.replaceAll("\\s+", "").toLowerCase();
    }

    public int getStage() { return stage; }
    public String getImagePath() { return imagePath; }
    public String getMainAnswer() { return answers.get(0); }
    public String getExplain() { return explain; }
}
