import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;

// BattleScreen 테스트용 main (인자: 0=자바, 1=상식, 2=이름 맞추기)
public class BattleTest {
    private static final String[] TOPICS = {"자바 코딩", "상식", "이름 맞추기"};
    private static final String[] BOSSES = {"NullPointerException", "상식 박사", "이름 마스터"};

    public static void main(String[] args) {
        int topic = 0;
        if (args.length > 0) {
            try {
                topic = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                topic = 0;
            }
        }
        if (topic < 0 || topic > 2) topic = 0;
        final int t = topic;

        SwingUtilities.invokeLater(() -> {
            BattleScreen screen = new BattleScreen();
            screen.setBattleListener(win -> System.out.println("전투 종료: " + (win ? "승리" : "패배")));
            screen.startBattle(TOPICS[t], 1, BOSSES[t], questions(t), 12);

            // H 키: 총알 맞기 테스트
            screen.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_H, 0), "debugHit");
            screen.getActionMap().put("debugHit", new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    screen.debugHit();
                }
            });

            JFrame frame = new JFrame("JavaQuest");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(screen);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // 문제 한 개 만들기 (이미지 없음)
    private static QuizItem q(String text, int answer, String explain, String... choices) {
        return new QuizItem(text, choices, answer, explain, null);
    }

    // 주제별 임시 문제 10개
    private static List<QuizItem> questions(int topic) {
        if (topic == 0) return java();
        if (topic == 1) return common();
        return names();
    }

    private static List<QuizItem> java() {
        return Arrays.asList(
            q("int a = 5 / 2; 의 a 값은?", 0, "정수 나눗셈은 소수점을 버린다.", "2", "2.5", "3", "0"),
            q("자바의 문자열 클래스는?", 1, "문자열은 String이다.", "string", "String", "Str", "Text"),
            q("main 메소드의 반환형은?", 1, "main은 void이다.", "int", "void", "String", "boolean"),
            q("int[] a = {1, 2, 3}; 의 a.length 는?", 1, "원소가 3개이다.", "2", "3", "4", "1"),
            q("true/false를 저장하는 기본형은?", 2, "논리값은 boolean이다.", "int", "char", "boolean", "double"),
            q("null 변수의 메소드를 부르면 나는 예외는?", 0, "NullPointerException이 발생한다.",
                    "NullPointerException", "IOException", "ArithmeticException", "ClassCastException"),
            q("한 줄 주석 기호는?", 0, "// 로 시작한다.", "//", "#", "--", "/*"),
            q("반복문이 아닌 것은?", 3, "if는 조건문이다.", "for", "while", "do-while", "if"),
            q("클래스 상속에 쓰는 키워드는?", 1, "extends로 상속한다.", "implements", "extends", "inherits", "super"),
            q("5 % 3 의 값은?", 1, "5를 3으로 나눈 나머지는 2이다.", "1", "2", "3", "0")
        );
    }

    private static List<QuizItem> common() {
        return Arrays.asList(
            q("대한민국의 수도는?", 1, "수도는 서울이다.", "부산", "서울", "대구", "인천"),
            q("일본의 수도는?", 2, "수도는 도쿄이다.", "오사카", "교토", "도쿄", "나고야"),
            q("프랑스의 수도는?", 1, "수도는 파리이다.", "런던", "파리", "로마", "베를린"),
            q("미국의 수도는?", 1, "수도는 워싱턴 D.C.이다.", "뉴욕", "워싱턴 D.C.", "LA", "시카고"),
            q("이탈리아의 수도는?", 2, "수도는 로마이다.", "밀라노", "베네치아", "로마", "나폴리"),
            q("가는 말이 고와야 오는 말이 ( )", 1, "속담: 가는 말이 고와야 오는 말이 곱다.", "크다", "곱다", "많다", "좋다"),
            q("낮말은 새가 듣고 밤말은 ( )가 듣는다", 2, "속담: 밤말은 쥐가 듣는다.", "개", "고양이", "쥐", "소"),
            q("호랑이도 제 말 하면 ( )", 1, "속담: 호랑이도 제 말 하면 온다.", "운다", "온다", "간다", "난다"),
            q("티끌 모아 ( )", 2, "속담: 티끌 모아 태산.", "바다", "하늘", "태산", "구름"),
            q("백지장도 맞들면 ( )", 3, "속담: 백지장도 맞들면 낫다.", "찢어진다", "가볍다", "무겁다", "낫다")
        );
    }

    private static List<QuizItem> names() {
        return Arrays.asList(
            q("애니 '원피스'의 주인공은?", 3, "주인공은 루피이다.", "나루토", "고쿠", "이치고", "루피"),
            q("애니 '드래곤볼'의 주인공은?", 0, "주인공은 손오공이다.", "손오공", "루피", "나루토", "탄지로"),
            q("애니 '포켓몬스터'의 대표 포켓몬은?", 1, "대표 포켓몬은 피카츄이다.", "이상해씨", "피카츄", "파이리", "꼬부기"),
            q("애니 '귀멸의 칼날'의 주인공은?", 2, "주인공은 탄지로이다.", "젠이츠", "이노스케", "탄지로", "기유"),
            q("애니 '진격의 거인'의 주인공은?", 1, "주인공은 에렌이다.", "리바이", "에렌", "미카사", "아르민"),
            q("영화 '기생충'에 출연한 배우는?", 3, "송강호가 출연했다.", "유재석", "박지성", "김연아", "송강호"),
            q("영화 '아이언맨'에서 토니 스타크를 연기한 배우는?", 1, "로버트 다우니 주니어가 연기했다.",
                    "톰 크루즈", "로버트 다우니 주니어", "브래드 피트", "휴 잭맨"),
            q("영화 '타이타닉'의 남자 주인공 배우는?", 3, "레오나르도 디카프리오이다.",
                    "톰 행크스", "조니 뎁", "맷 데이먼", "레오나르도 디카프리오"),
            q("영화 '해리 포터'에서 해리를 연기한 배우는?", 0, "대니얼 래드클리프가 연기했다.",
                    "대니얼 래드클리프", "톰 홀랜드", "엘리엇 페이지", "제이크 질렌할"),
            q("영화 '어벤져스'에서 토르를 연기한 배우는?", 2, "크리스 헴스워스가 연기했다.",
                    "크리스 에반스", "마크 러팔로", "크리스 헴스워스", "제레미 레너")
        );
    }
}
