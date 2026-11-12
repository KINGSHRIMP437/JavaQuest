import java.util.Scanner;

// 이름 맞추기(이미지 퀴즈) 점검 (프로젝트 폴더에서 실행)
//   javac -encoding UTF-8 -d out *.java namequiz\*.java
//   java -ea -cp out NameQuizTest
public class NameQuizTest {
    public static void main(String[] args) {
        // 1. 한 줄 읽기 + 정답 판정
        NameQuiz q = new NameQuiz();
        q.read(new Scanner("1|images/name/a.png|피카츄, Pikachu |노란 쥐").useDelimiter("\\|"));
        assert q.getStage() == 1;
        assert q.getImagePath().equals("images/name/a.png");
        assert q.getExplain().equals("노란 쥐");
        assert q.isCorrect("피카츄");
        assert q.isCorrect(" 피 카 츄 ") : "공백 무시";
        assert q.isCorrect("PIKACHU") : "대소문자 무시";
        assert !q.isCorrect("라이츄");
        assert !q.isCorrect("") : "빈 입력은 오답";
        assert !q.isCorrect(null) : "null은 오답";
        assert q.getMainAnswer().equals("피카츄") : "대표 정답은 첫 번째";

        // 2. 정답 칸이 비어 있으면 형식 오류
        boolean failed = false;
        try {
            new NameQuiz().read(new Scanner("1|a.png| , |해설").useDelimiter("\\|"));
        } catch (IllegalArgumentException e) { failed = true; }
        assert failed : "정답이 없으면 예외";


        // 4. 파일 읽기: 스테이지 1~3에 10문제씩
        NameQuizManager nm = new NameQuizManager(NameQuizPanel.DIR + "name_quiz.txt");
        for (int s = 1; s <= 3; s++)
            assert nm.findByStage(s).size() == 10 : "name_quiz.txt 스테이지 " + s + " 문제는 10개";
        assert nm.findByStage(4).isEmpty();
        assert nm.findByStage(1).get(0).isCorrect("iu") : "아이유 영문 정답";
        assert nm.findByStage(3).get(9).isCorrect("자장면") : "짜장면/자장면 둘 다 정답";

        // 5. 스테이지별 질문 문구
        assert NameQuizPanel.promptFor(1).contains("연예인");
        assert NameQuizPanel.promptFor(2).contains("캐릭터");
        assert NameQuizPanel.promptFor(3).contains("음식");

        // 8. 스테이지별 보스 HP
        assert NameQuizPanel.bossHpFor(1, 5) == 3;
        assert NameQuizPanel.bossHpFor(2, 5) == 4;
        assert NameQuizPanel.bossHpFor(3, 5) == 5;
        assert NameQuizPanel.bossHpFor(1, 2) == 2 : "문제 수보다 많을 수 없음";

        // 7. 10문제 중 무작위 5문제 (겹치지 않음)
        java.util.List<NameQuiz> picked = NameQuizPanel.pickRandom(nm.findByStage(2), NameQuizPanel.QUIZ_COUNT, new java.util.Random());
        assert picked.size() == 5 : "5문제 출제";
        assert new java.util.HashSet<>(picked).size() == 5 : "중복 없음";
        for (NameQuiz p : picked) assert p.getStage() == 2;
        assert NameQuizPanel.pickRandom(nm.findByStage(4), 5, new java.util.Random()).isEmpty();

        // 6. 확장자가 달라도 같은 이름의 이미지를 찾음
        try {
            java.io.File dir = java.nio.file.Files.createTempDirectory("nq").toFile();
            java.io.File jpg = new java.io.File(dir, "x.jpg");
            assert jpg.createNewFile();
            String base = dir.getPath().replace('\\', '/');
            assert NameQuizPanel.findImageFile(base + "/x.png").equals(jpg) : "png로 적어도 jpg를 찾음";
            assert NameQuizPanel.findImageFile(base + "/x.jpg").equals(jpg);
            assert NameQuizPanel.findImageFile(base + "/x").equals(jpg) : "확장자 없이 적어도 찾음";
            assert !NameQuizPanel.findImageFile(base + "/y.png").isFile() : "없는 파일은 없음";
            jpg.delete(); dir.delete();
        } catch (java.io.IOException e) { throw new RuntimeException(e); }

        nm.printAll();
        System.out.println("OK: 이름 맞추기 문제 " + nm.getAll().size() + "개");
    }
}
