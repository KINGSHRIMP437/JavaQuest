// 전투 규칙 (화면 코드 없음)
public class BattleEngine {
    public static final int MAX_HEARTS = 5;   // 교수님 피드백: 너무 어려워서 3 → 5
    private int playerHearts = MAX_HEARTS;
    private int attacksLeft;
    private String bossName;
    private int bossHp;
    private int bossMaxHp;

    // 전투 시작: 값 초기화
    public void startBattle(String bossName, int bossHp, int attacks) {
        this.bossName = bossName;
        this.bossHp = bossHp;
        this.bossMaxHp = bossHp;
        this.attacksLeft = attacks;
        this.playerHearts = MAX_HEARTS;
    }

    // 답 제출: 공격 횟수 -1, 정답이면 보스 HP -1
    public void submitAnswer(boolean correct) {
        attacksLeft--;
        if (correct) bossHp--;
    }

    // 테스트용 무적: java -Djavaquest.god=true GameFrame 으로 실행하면 하트가 줄지 않음 (평소 실행에는 영향 없음)
    // 타이틀 화면에서 A B A B ← → ← → 를 입력해도 켜고 끌 수 있음 (TitleScreen)
    private static boolean godMode = Boolean.getBoolean("javaquest.god");

    public static boolean isGodMode() { return godMode; }
    public static boolean toggleGodMode() { return godMode = !godMode; }

    // 총알에 맞음: 하트 -1
    public void hitByBullet() {
        if (godMode) return;
        playerHearts--;
    }

    // 보스 HP가 0 이하면 승리
    public boolean isWin() {
        return bossHp <= 0;
    }

    // 하트가 없거나, 공격 횟수를 다 썼는데 보스가 남아 있으면 패배
    public boolean isLose() {
        return playerHearts <= 0 || (attacksLeft <= 0 && bossHp > 0);
    }

    public int getPlayerHearts() { return playerHearts; }

    // 남은 하트로 별점: 5개 → 별 3개, 4·3개 → 별 2개, 2·1개 → 별 1개 (0개 = 패배)
    public static int starsFor(int hearts) {
        return hearts >= 5 ? 3 : hearts >= 3 ? 2 : hearts >= 1 ? 1 : 0;
    }
    public int getAttacksLeft() { return attacksLeft; }
    public String getBossName() { return bossName; }
    public int getBossHp() { return bossHp; }
    public int getBossMaxHp() { return bossMaxHp; }
}
