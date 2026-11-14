import java.util.ArrayList;
import java.util.List;

public class NameQuizManager extends Manager<NameQuiz> {
    public NameQuizManager(String filename) {
        readAll(filename, NameQuiz::new);
    }

    public List<NameQuiz> findByStage(int stageNo) {
        List<NameQuiz> result = new ArrayList<>();
        for (NameQuiz q : list)
            if (q.getStage() == stageNo) result.add(q);
        return result;
    }
}
