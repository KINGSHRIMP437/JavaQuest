import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Supplier;

// 파일 읽기 → 리스트 보관 → 검색·추가·삭제
public class Manager<T extends Manageable> {
    protected List<T> list = new ArrayList<>();

    // factory: 빈 객체를 만드는 방법 (예: Question::new)
    public void readAll(String filename, Supplier<T> factory) {
        // UTF-8 명시: Java 17 이하 Windows에서 한글 깨짐 방지
        try (Scanner file = new Scanner(new File(filename), "UTF-8")) {
            int lineNo = 0;
            while (file.hasNextLine()) {
                String line = file.nextLine();
                lineNo++;
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                try (Scanner scan = new Scanner(line).useDelimiter("\\|")) {
                    T item = factory.get();
                    item.read(scan);
                    list.add(item);
                } catch (RuntimeException e) { // 형식이 틀린 줄은 건너뛰고 알려줌
                    System.err.println(filename + " " + lineNo + "번째 줄 형식 오류: " + line);
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println(filename + " 파일을 찾을 수 없음");
        }
    }

    public void printAll() {
        for (T item : list) item.print();
    }

    public T find(String kwd) {
        for (T item : list) if (item.matches(kwd)) return item;
        return null;
    }

    public List<T> findAll(String kwd) {
        List<T> result = new ArrayList<>();
        for (T item : list) if (item.matches(kwd)) result.add(item);
        return result;
    }

    public void add(T item) { list.add(item); }

    public boolean remove(T item) { return list.remove(item); }

    public List<T> getAll() { return list; }
}
