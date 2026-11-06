// 백엔드 팀원이 데이터 파일 입출력, 리스트 관리, Manageable 로직을 구현할 클래스입니다.
public class GameManager {
    
    // 게임 상태 변수들 (GUI에서 화면에 표시하기 위해 가져다 씁니다)
    public int attackTurns = 10;     // 남은 공격턴 (10개 중 7개 맞추면 클리어)
    public int correctCount = 0;     // 맞춘 퀴즈 수
    public int playerHitCount = 0;   // 탄막 피격 횟수 (3번 맞으면 게임 오버)
    
    public int bossHp = 120;
    public int bossMaxHp = 120;
    public int playerHp = 20;
    public int playerMaxHp = 20;

    // 현재 선택된 난이도와 문제 데이터
    public String currentDifficulty = "";
    public String currentQuestion = "";
    public String[] currentChoices = {"", "", "", ""};
    public String currentExplanation = "";
    
    public GameManager() {
        // 백엔드 팀원이 텍스트 파일(문제, 몬스터 정보)을 읽어 리스트에 보관하는 로직이 들어갈 자리
    }

    // 난이도 선택 시 백엔드에서 문제를 하나 뽑아서 세팅해주는 임시 메서드
    public void setQuizByDifficulty(String difficulty) {
        this.currentDifficulty = difficulty;
        if(difficulty.equals("하")) {
            currentQuestion = "String s = null; System.out.println(s.length());\n이 코드를 실행하면 어떻게 될까?";
            currentChoices = new String[]{"0이 출력된다", "null이 출력된다", "NullPointerException 발생", "컴파일 오류"};
            currentExplanation = "null인 참조로 메서드를 호출하면 실행 중에 NullPointerException이 발생해요.";
        }
        // 중, 상 난이도 데이터 추가 예정...
    }

    // 답안 판별 로직
    public boolean checkAnswer(int choiceIndex) {
        attackTurns--; // 공격턴 1 감소
        
        if(choiceIndex == 2) { // 3번째(인덱스 2)가 정답이라고 가정
            correctCount++;
            bossHp -= 30; // 몬스터 피 깎임
            return true;
        }
        return false; // 틀리면 데미지 없음
    }
}