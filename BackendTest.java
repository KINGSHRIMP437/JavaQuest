// 백엔드 자체 점검: java -ea BackendTest (프로젝트 폴더에서 실행)
public class BackendTest {
    public static void main(String[] args) {
        QuestionManager qm = new QuestionManager("questions.txt");
        StageManager sm = new StageManager("monsters.txt");

        assert sm.getAll().size() == 9 : "스테이지는 9개";
        for (String topic : new String[]{"coding", "common", "name"})
            for (int s = 1; s <= 3; s++) {
                assert sm.findStage(topic, s) != null : topic + " " + s + " 보스 없음";
                assert !qm.findByStage(topic, s).isEmpty() : topic + " " + s + " 문제 없음";
            }
        assert sm.findStage("CODING", 3).getBossName().equals("NullPointerException (Boss)");
        assert sm.findStage("coding", 4) == null;
        assert qm.findByStage("coding", 1).get(0).answer == 0; // 5/2 → 보기1 "2"
        assert DataLoader.loadBossName("monsters.txt", "common", 2).equals("상식 백과사전 (Lv.2)");

        qm.printAll();
        sm.printAll();
        System.out.println("OK: 문제 " + qm.getAll().size() + "개, 스테이지 " + sm.getAll().size() + "개");
    }
}
