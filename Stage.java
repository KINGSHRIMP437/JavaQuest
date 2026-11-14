import java.util.Scanner;

// monsters.txt 한 줄: 주제|스테이지번호|보스이름|HP비율(%)|이미지경로
public class Stage implements Manageable {
    String topic;
    int stageNo;
    String bossName;
    int hpPercent;    // 문제 수 대비 보스 HP 비율 (예: 50 → 문제 10개면 HP 5)
    String imagePath; // 보스 이미지 파일 경로

    public Stage() {}

    public Stage(String topic, int stageNo, String bossName, int hpPercent, String imagePath) {
        this.topic = topic;
        this.stageNo = stageNo;
        this.bossName = bossName;
        this.hpPercent = hpPercent;
        this.imagePath = imagePath;
    }

    @Override
    public void read(Scanner scan) {
        topic = scan.next().trim();
        stageNo = Integer.parseInt(scan.next().trim());
        bossName = scan.next().trim();
        hpPercent = Integer.parseInt(scan.next().trim());
        if (hpPercent <= 0 || hpPercent > 100) throw new IllegalArgumentException("HP비율은 1~100");
        
        // 경로 칸이 없을 경우를 대비해 hasNext()로 있을 때만 안전하게 읽음
        imagePath = scan.hasNext() ? scan.next().trim() : null;
    }

    @Override
    public void print() {
        System.out.printf("[%s %d] %s HP %d%%%n", topic, stageNo, bossName, hpPercent);
    }

    @Override
    public boolean matches(String kwd) {
        return topic.equalsIgnoreCase(kwd) || bossName.contains(kwd);
    }

    public boolean is(String topic, int stageNo) {
        return this.topic.equalsIgnoreCase(topic) && this.stageNo == stageNo;
    }

    public String getTopic() { return topic; }
    public int getStageNo() { return stageNo; }
    public String getBossName() { return bossName; }
    public int getHpPercent() { return hpPercent; }
    public String getImagePath() { return imagePath; } // 이미지 경로 반환 메서드 추가

    // 보스 HP = 문제 수 × 비율 (반올림, 최소 1). 공격 횟수는 문제 수이므로 (문제 수 - HP)번까지 틀려도 됨
    public int getBossHp(int questionCount) {
        return Math.max(1, (int) Math.round(questionCount * hpPercent / 100.0));
    }
}