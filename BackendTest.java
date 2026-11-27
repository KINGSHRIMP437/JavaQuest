// 백엔드 자체 점검: java -ea BackendTest (프로젝트 폴더에서 실행)
public class BackendTest {
    public static void main(String[] args) {
        QuestionManager qm = new QuestionManager("questions.txt");
        StageManager sm = new StageManager("monsters.txt");

        assert sm.getAll().size() == 9 : "스테이지는 9개";
        for (String topic : new String[]{"coding", "common", "name"})
            for (int s = 1; s <= 3; s++) {
                assert sm.findStage(topic, s) != null : topic + " " + s + " 보스 없음";
                // 이름 맞추기 문제는 namequiz/name_quiz.txt에 있음 (NameQuizTest가 검사)
                int expected = topic.equals("name") ? 0 : 10;
                assert qm.findByStage(topic, s).size() == expected : topic + " " + s + " 문제는 " + expected + "개";
            }
        assert sm.findStage("CODING", 3).getBossName().equals("NullPointerException (Boss)");
        assert sm.findStage("coding", 4) == null;
        assert qm.findByStage("coding", 1).get(0).answer == 1; // 클래스로 만든 실제 대상 → 보기2 "객체"
        assert BattleEngine.starsFor(5) == 3 && BattleEngine.starsFor(4) == 2 && BattleEngine.starsFor(3) == 2
            && BattleEngine.starsFor(2) == 1 && BattleEngine.starsFor(1) == 1 : "별점: 하트 5 → 3, 4·3 → 2, 2·1 → 1";
        BattleEngine skip = new BattleEngine();
        skip.startBattle("boss", 5, 7);
        skip.skipBattle();
        assert skip.isWin() && !skip.isLose() : "스킵하면 바로 승리";
        int idx = 0;   // 무적 모드 커맨드: A B A B ← → ← → (중간에 틀리면 처음부터, A를 또 누르면 거기서 다시 시작)
        for (int k : new int[]{java.awt.event.KeyEvent.VK_A, java.awt.event.KeyEvent.VK_A}) idx = TitleScreen.nextCodeIndex(idx, k);
        assert idx == 1 : "A A → 두 번째 A부터 다시 셈";
        for (int i = 1; i < TitleScreen.GOD_CODE.length; i++) idx = TitleScreen.nextCodeIndex(idx, TitleScreen.GOD_CODE[i]);
        assert idx == TitleScreen.GOD_CODE.length : "커맨드를 끝까지 맞게 누르면 완성";
        assert TitleScreen.nextCodeIndex(4, java.awt.event.KeyEvent.VK_UP) == 0 : "틀리면 처음부터";
        Stage common2 = DataLoader.loadStage("monsters.txt", "common", 2);
        assert common2.getBossName().equals("상식 백과사전 (Lv.2)");
        assert common2.getHpPercent() == 40 : "HP 비율은 monsters.txt 값";
        assert common2.getBossHp(10) == 4 : "중간 보스: 문제 10개 → HP 4";
        assert sm.findStage("coding", 3).getBossHp(10) == 5 : "마지막 보스: 문제 10개 → HP 5";
        assert sm.findStage("coding", 1).getBossHp(10) == 3 : "쉬움 보스: 문제 10개 → HP 3";
        assert common2.getBossHp(1) == 1 : "HP는 최소 1";
        assert DataLoader.loadStage("monsters.txt", "coding", 9).getBossName().equals("Unknown Boss");

        qm.printAll();
        sm.printAll();
        System.out.println("OK: 문제 " + qm.getAll().size() + "개, 스테이지 " + sm.getAll().size() + "개");
    }
}
