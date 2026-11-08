import java.util.Scanner;

// Manager가 관리하는 데이터 한 개 (txt 한 줄 = 객체 하나)
public interface Manageable {
    void read(Scanner scan);       // '|'로 구분된 한 줄을 읽어 필드 채우기
    void print();                  // 콘솔 출력 (디버그용)
    boolean matches(String kwd);   // 검색어와 일치하는지
}
