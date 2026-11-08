import java.util.ArrayList;
import java.util.List;

public class QuestionManager extends Manager<Question> {
    public QuestionManager(String filename) {
        readAll(filename, Question::new);
    }

    public List<Question> findByStage(String topic, int stageNo) {
        List<Question> result = new ArrayList<>();
        for (Question q : list)
            if (q.topic.equalsIgnoreCase(topic) && q.getStage() == stageNo) result.add(q);
        return result;
    }
}
