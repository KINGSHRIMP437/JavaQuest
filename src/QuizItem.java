// 문제 한 개 (TODO: 팀의 Question 클래스와 합치기)
public class QuizItem {
    public final String text;       // 문제 글
    public final String[] choices;  // 보기 4개
    public final int answer;        // 정답 번호(0~3)
    public final String explain;    // 해설
    public final String imagePath;  // 이미지 경로 (없으면 null)

    public QuizItem(String text, String[] choices, int answer, String explain, String imagePath) {
        this.text = text;
        this.choices = choices;
        this.answer = answer;
        this.explain = explain;
        this.imagePath = imagePath;
    }
}
