// 백엔드 자체 점검: java -ea BackendTest (프로젝트 폴더에서 실행)
public class BackendTest {
    public static void main(String[] args) {
        QuestionManager qm = new QuestionManager("questions.txt");
        StageManager sm = new StageManager("monsters.txt");

        assert sm.getAll().size() == 9 : "스테이지는 9개";
        for (String topic : new String[]{"coding", "common", "name"})
            for (int s = 1; s <= 3; s++) {
                assert sm.findStage(topic, s) != null : topic + " " + s + " 보스 없음";
                assert qm.findByStage(topic, s).size() == 10 : topic + " " + s + " 문제는 10개";
            }
        assert sm.findStage("CODING", 3).getBossName().equals("NullPointerException (Boss)");
        assert sm.findStage("coding", 4) == null;
        assert qm.findByStage("coding", 1).get(0).answer == 0; // 5/2 → 보기1 "2"
        Stage common2 = DataLoader.loadStage("monsters.txt", "common", 2);
        assert common2.getBossName().equals("상식 백과사전 (Lv.2)");
        assert common2.getHpPercent() == 50 : "HP 비율은 monsters.txt 값";
        assert common2.getBossHp(10) == 5 : "일반 보스: 문제 10개 → HP 5";
        assert sm.findStage("coding", 3).getBossHp(10) == 7 : "마지막 보스: 문제 10개 → HP 7";
        assert common2.getBossHp(1) == 1 : "HP는 최소 1";
        assert DataLoader.loadStage("monsters.txt", "coding", 9).getBossName().equals("Unknown Boss");

        qm.printAll();
        sm.printAll();
        System.out.println("OK: 문제 " + qm.getAll().size() + "개, 스테이지 " + sm.getAll().size() + "개");
    }
}
