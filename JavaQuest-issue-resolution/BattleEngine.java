// 전투 규칙 (화면 코드 없음)
public class BattleEngine {
    private int playerHearts = 3;
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
        this.playerHearts = 3;
    }

    // 답 제출: 공격 횟수 -1, 정답이면 보스 HP -1
    public void submitAnswer(boolean correct) {
        attacksLeft--;
        if (correct) bossHp--;
    }

    // 총알에 맞음: 하트 -1
    public void hitByBullet() {
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
    public int getAttacksLeft() { return attacksLeft; }
    public String getBossName() { return bossName; }
    public int getBossHp() { return bossHp; }
    public int getBossMaxHp() { return bossMaxHp; }
}
