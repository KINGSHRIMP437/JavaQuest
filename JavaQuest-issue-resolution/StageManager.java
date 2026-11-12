public class StageManager extends Manager<Stage> {
    public StageManager(String filename) {
        readAll(filename, Stage::new);
    }

    public Stage findStage(String topic, int stageNo) {
        for (Stage s : list) if (s.is(topic, stageNo)) return s;
        return null;
    }
}
