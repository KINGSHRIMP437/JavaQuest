import java.util.Scanner;

// monsters.txt 한 줄: 주제|스테이지번호|보스이름|최대HP
public class Stage implements Manageable {
    String topic;
    int stageNo;
    String bossName;
    int bossHp;

    @Override
    public void read(Scanner scan) {
        topic = scan.next().trim();
        stageNo = Integer.parseInt(scan.next().trim());
        bossName = scan.next().trim();
        bossHp = Integer.parseInt(scan.next().trim());
    }

    @Override
    public void print() {
        System.out.printf("[%s %d] %s HP %d%n", topic, stageNo, bossName, bossHp);
    }

    @Override
    public boolean matches(String kwd) {
        return topic.equalsIgnoreCase(kwd) || bossName.contains(kwd);
    }

    public boolean is(String topic, int stageNo) {
        return this.topic.equalsIgnoreCase(topic) && this.stageNo == stageNo;
    }

    public String getBossName() { return bossName; }
    public int getBossHp() { return bossHp; }
}
