// 아이템 로직 검사 (화면 없음): java -ea -cp out ItemTest
public class ItemTest {
    public static void main(String[] args) throws Exception {
        // 체력 완충
        BattleEngine e = new BattleEngine();
        e.startBattle("보스", 5, 5);
        assert !e.healFull() : "가득 차 있으면 false";
        e.hitByBullet();
        assert e.getPlayerHearts() == BattleEngine.MAX_HEARTS - 1;
        assert e.healFull() && e.getPlayerHearts() == BattleEngine.MAX_HEARTS : "줄었으면 채우고 true";

        // 문제 개수 초기화
        assert !e.resetAttacks() : "시작 값이면 false";
        e.submitAnswer(false);
        e.submitAnswer(true);
        assert e.getAttacksLeft() == 3;
        assert e.resetAttacks() && e.getAttacksLeft() == 5 : "되돌리고 true";

        // 무적
        assert !e.isInvincible();
        assert e.activateInvincible(300);
        assert e.isInvincible() && e.getInvincibleMsLeft() > 0;
        assert !e.activateInvincible(300) : "무적 중 중복 사용은 false";
        e.hitByBullet();
        assert e.getPlayerHearts() == BattleEngine.MAX_HEARTS : "무적 중에는 하트가 안 줄어듦";
        Thread.sleep(400);
        assert !e.isInvincible() && e.getInvincibleMsLeft() == 0;
        e.hitByBullet();
        assert e.getPlayerHearts() == BattleEngine.MAX_HEARTS - 1 : "무적이 끝나면 다시 줄어듦";

        // 새 전투 시작 시 무적 초기화 + 시작 공격 횟수 갱신
        e.activateInvincible(10000);
        e.startBattle("보스2", 3, 7);
        assert !e.isInvincible();
        e.submitAnswer(true);
        assert e.resetAttacks() && e.getAttacksLeft() == 7;

        // 보관함
        Inventory inv = new Inventory();
        for (int t = 0; t < Inventory.TYPES; t++) assert inv.count(t) == 1 && inv.has(t) : "시작 1개";
        assert inv.use(Inventory.INVINCIBLE) && inv.count(Inventory.INVINCIBLE) == 0;
        assert !inv.has(Inventory.INVINCIBLE) && !inv.use(Inventory.INVINCIBLE) : "0개면 사용 불가";
        assert Inventory.name(Inventory.RESET_ATTACKS).equals("문제 개수 초기화");

        System.out.println("ItemTest 모두 통과");
    }
}
