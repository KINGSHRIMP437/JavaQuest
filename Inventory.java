// 아이템 보관함 (전투를 시작할 때마다 새로 만들어 종류별 1개씩 지급)
public class Inventory {
    public static final int INVINCIBLE = 0;    // 무적
    public static final int HEAL_FULL = 1;     // 체력 완충
    public static final int RESET_ATTACKS = 2; // 문제 개수 초기화
    public static final int TYPES = 3;

    private static final String[] NAMES = {"무적", "체력 완충", "문제 개수 초기화"};

    private final int[] counts = new int[TYPES];

    // 시작할 때 각 1개씩 지급
    public Inventory() {
        java.util.Arrays.fill(counts, 1);
    }

    public int count(int type) { return counts[type]; }

    public boolean has(int type) { return counts[type] > 0; }

    // 있으면 1개 줄이고 true
    public boolean use(int type) {
        if (counts[type] <= 0) return false;
        counts[type]--;
        return true;
    }

    public static String name(int type) { return NAMES[type]; }
}
