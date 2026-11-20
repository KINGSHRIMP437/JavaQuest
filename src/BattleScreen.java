import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// 전투 화면 (명령 -> 문제 -> 결과 -> 적 턴)
public class BattleScreen extends JPanel {
    private static final Color BG = new Color(15, 20, 50); // 어두운 남색
    private static final int MAX_HEARTS = 3;
    private static final int QUIZ_SECONDS = 15;
    private static final int IMG_W = 240;
    private static final int IMG_H = 180;

    private BattleEngine engine = new BattleEngine(); // 규칙 담당
    private BattleListener listener;
    private Random random = new Random();
    private List<QuizItem> pool = new ArrayList<>(); // 아직 못 맞힌 문제
    private QuizItem current;                        // 지금 푸는 문제
    private int stageNo;
    private boolean won;                             // 전투 결과

    private JPanel centerCards = new JPanel(new CardLayout()); // 메시지 / 문제
    private JPanel bottomCards = new JPanel(new CardLayout()); // 명령 / 계속 / 대기 / 확인
    private JTextArea message = new JTextArea();
    private JPanel quizPanel = new JPanel(new BorderLayout(5, 5));

    private JLabel topicLabel = makeLabel("", 16);
    private JLabel bossNameLabel = makeLabel("", 22);
    private JProgressBar bossHpBar = new JProgressBar();
    private JLabel attackLabel = makeLabel("", 16);
    private JLabel[] hearts = new JLabel[MAX_HEARTS];
    private JPanel heartRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    private MonsterView monsterView = new MonsterView();

    private Color accent = Color.WHITE; // 주제 포인트 색
    private Timer flashTimer;           // 하트 깜빡임용
    private int flashCount;
    private int lastHearts;             // 하트 감소 감지용

    // 문제 화면 구성요소
    private JProgressBar timeBar = new JProgressBar();
    private JLabel imageLabel = new JLabel("이미지", SwingConstants.CENTER);
    private JTextArea questionText = new JTextArea();
    private JButton[] choiceBtns = new JButton[4];

    private Timer timer; // 제한시간용
    private int remain;  // 남은 틱(0.1초 단위)

    // 화면 전체 배치와 타이머 준비
    public BattleScreen() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(BG);
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        initMessageArea();
        initQuizPanel();
        centerCards.setOpaque(false);
        centerCards.add(message, "msg");
        centerCards.add(quizPanel, "quiz");

        add(makeTopArea(), BorderLayout.NORTH);
        add(centerCards, BorderLayout.CENTER);
        add(makeBottomArea(), BorderLayout.SOUTH);

        timer = new Timer(100, e -> tick());
        flashTimer = new Timer(100, e -> flashStep());
    }

    // ===== 밖에 제공하는 메소드 =====

    // 전투 시작: 엔진 초기화 후 명령 선택 화면 표시
    public void startBattle(String topicName, int stageNo, String bossName, List<QuizItem> questions, int attacks) {
        this.stageNo = stageNo;
        pool = new ArrayList<>(questions);
        engine.startBattle(bossName, questions.size(), attacks);
        timer.stop();
        lastHearts = engine.getPlayerHearts();

        topicLabel.setText(topicName + " · 스테이지" + stageNo);
        applyAccent(topicColor(topicName));
        message.setText(bossName + "이(가) 나타났다!");
        showCard(centerCards, "msg");
        showCard(bottomCards, "cmd");
        updateStatus();
    }

    // 전투 결과를 받을 리스너 등록
    public void setBattleListener(BattleListener l) {
        listener = l;
    }

    // 적 턴 시작 (임시: 2초 메시지 후 바로 종료)
    // TODO: 탄막 담당이 level(스테이지 1~3)에 맞는 탄막 화면을 붙이고 endEnemyTurn(맞은 횟수) 호출
    public void startEnemyTurn(int level) {
        message.setText("적의 턴!");
        showCard(centerCards, "msg");
        showCard(bottomCards, "wait");
        Timer t = new Timer(2000, e -> endEnemyTurn(0));
        t.setRepeats(false);
        t.start();
    }

    // 적 턴 종료: 맞은 만큼 하트 감소 후 패배 확인
    public void endEnemyTurn(int hitsTaken) {
        for (int i = 0; i < hitsTaken; i++) engine.hitByBullet();
        updateStatus();

        String text = hitsTaken > 0 ? "탄막에 " + hitsTaken + "번 맞았다!" : "공격을 모두 피했다!";
        if (engine.isLose()) {
            showEnd(false, text, loseReason());
        } else {
            message.setText(text);
            showCard(centerCards, "msg");
            showCard(bottomCards, "cmd");
        }
    }

    // 테스트용: 총알 한 번 맞기
    public void debugHit() {
        engine.hitByBullet();
        updateStatus();
    }

    // ===== 화면 구성 =====

    // 위쪽: 주제·스테이지, 보스 이름, HP 바, 남은 공격
    private JPanel makeTopArea() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        bossHpBar.setStringPainted(true);
        bossHpBar.setForeground(new Color(200, 50, 50));
        bossHpBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(topicLabel);
        p.add(bossNameLabel);
        p.add(Box.createVerticalStrut(8));
        p.add(bossHpBar);
        p.add(Box.createVerticalStrut(5));
        p.add(attackLabel);

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        top.add(p, BorderLayout.CENTER);
        top.add(monsterView, BorderLayout.EAST);
        return top;
    }

    // 아래쪽: 하트 3칸과 버튼 묶음
    private JPanel makeBottomArea() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        heartRow.setBackground(BG);
        heartRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int i = 0; i < MAX_HEARTS; i++) {
            hearts[i] = new JLabel("♥");
            hearts[i].setFont(new Font("Dialog", Font.BOLD, 30));
            heartRow.add(hearts[i]);
        }
        p.add(heartRow);
        p.add(Box.createVerticalStrut(8));

        bottomCards.setOpaque(false);
        bottomCards.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottomCards.add(makeCommandPanel(), "cmd");
        bottomCards.add(makeSingleButtonPanel("계속", () -> startEnemyTurn(stageNo)), "continue");
        JPanel wait = new JPanel(); // 적 턴 동안 버튼 없음
        wait.setOpaque(false);
        bottomCards.add(wait, "wait");
        bottomCards.add(makeSingleButtonPanel("확인", () -> {
            if (listener != null) listener.onBattleEnd(won);
        }), "end");
        p.add(bottomCards);
        return p;
    }

    // 명령 버튼 4개 (공격만 동작, 나머지는 준비 중)
    private JPanel makeCommandPanel() {
        JPanel p = new JPanel(new GridLayout(1, 4, 10, 0));
        p.setOpaque(false);
        for (String name : new String[]{"공격", "디버그", "아이템", "도망"}) {
            JButton b = new JButton(name);
            if (name.equals("공격")) b.addActionListener(e -> startQuiz());
            else b.addActionListener(e -> message.setText("준비 중입니다"));
            p.add(b);
        }
        return p;
    }

    // 버튼 하나짜리 패널 (계속 / 확인)
    private JPanel makeSingleButtonPanel(String text, Runnable action) {
        JPanel p = new JPanel(new GridLayout(1, 1));
        p.setOpaque(false);
        JButton b = new JButton(text);
        b.addActionListener(e -> action.run());
        p.add(b);
        return p;
    }

    // 메시지 영역 기본 설정
    private void initMessageArea() {
        message.setEditable(false);
        message.setLineWrap(true);
        message.setFont(new Font("Dialog", Font.PLAIN, 16));
    }

    // 문제 화면: 시간 바, 이미지, 문제 글, 보기 4개
    private void initQuizPanel() {
        quizPanel.setBackground(Color.WHITE);
        quizPanel.add(timeBar, BorderLayout.NORTH);

        imageLabel.setPreferredSize(new Dimension(IMG_W, IMG_H));
        imageLabel.setOpaque(true);
        imageLabel.setBackground(Color.GRAY);
        imageLabel.setForeground(Color.WHITE);

        questionText.setEditable(false);
        questionText.setLineWrap(true);
        questionText.setFont(new Font("Dialog", Font.BOLD, 16));

        JPanel mid = new JPanel(new BorderLayout(10, 0));
        mid.setOpaque(false);
        mid.add(imageLabel, BorderLayout.WEST);
        mid.add(questionText, BorderLayout.CENTER);
        quizPanel.add(mid, BorderLayout.CENTER);

        JPanel grid = new JPanel(new GridLayout(2, 2, 5, 5));
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            choiceBtns[i] = new JButton();
            choiceBtns[i].addActionListener(e -> answer(idx));
            grid.add(choiceBtns[i]);
        }
        quizPanel.add(grid, BorderLayout.SOUTH);
    }

    // 주제 포인트 색을 화면에 적용
    private void applyAccent(Color c) {
        accent = c;
        topicLabel.setForeground(c);
        timeBar.setForeground(c);
        quizPanel.setBorder(BorderFactory.createLineBorder(c, 3));
        message.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(c, 3),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
    }

    // 주제 이름에 맞는 포인트 색 (자바=청록, 상식=노랑, 그 외=분홍)
    private Color topicColor(String topicName) {
        if (topicName.contains("자바")) return new Color(0, 200, 190);
        if (topicName.contains("상식")) return new Color(240, 200, 40);
        return new Color(240, 100, 170);
    }

    // ===== 문제 흐름 =====

    // 못 맞힌 문제 중 하나를 뽑아 문제 화면을 열고 타이머 시작
    private void startQuiz() {
        current = pool.get(random.nextInt(pool.size()));

        questionText.setText(current.text);
        for (int i = 0; i < 4; i++) {
            choiceBtns[i].setText((i + 1) + ". " + current.choices[i]);
        }
        showImage(current.imagePath);

        remain = QUIZ_SECONDS * 10;
        timeBar.setMaximum(remain);
        timeBar.setValue(remain);
        timeBar.setForeground(accent);
        showCard(centerCards, "quiz");
        timer.restart();
    }

    // 이미지가 있으면 240x180으로 표시, 없으면 회색 사각형
    private void showImage(String path) {
        imageLabel.setIcon(null);
        imageLabel.setText("이미지");
        if (path == null || !new File(path).exists()) return;

        ImageIcon icon = new ImageIcon(path);
        if (icon.getIconWidth() <= 0) return;
        imageLabel.setIcon(new ImageIcon(icon.getImage().getScaledInstance(IMG_W, IMG_H, Image.SCALE_SMOOTH)));
        imageLabel.setText("");
    }

    // 0.1초마다 시간 바를 줄이고, 0이면 오답 처리
    private void tick() {
        remain--;
        timeBar.setValue(remain);
        if (remain <= 50) timeBar.setForeground(Color.RED); // 5초 이하면 빨강
        if (remain <= 0) finishQuiz(false, "시간 초과! ");
    }

    // 보기를 눌렀을 때 정답 여부 확인
    private void answer(int idx) {
        finishQuiz(idx == current.answer, "");
    }

    // 문제 종료: 규칙은 엔진에 맡기고 결과만 화면에 반영
    private void finishQuiz(boolean correct, String prefix) {
        timer.stop();
        engine.submitAnswer(correct);
        if (correct) {
            pool.remove(current); // 맞힌 문제는 다시 안 나옴
            monsterView.hit();
        }
        updateStatus();

        String text = correct ? "정답! " : prefix + "오답 (정답: " + (current.answer + 1) + "번) ";
        text += current.explain;

        if (engine.isWin()) {
            showEnd(true, text, "");
        } else if (engine.isLose()) {
            showEnd(false, text, loseReason());
        } else {
            message.setText(text);
            showCard(centerCards, "msg");
            showCard(bottomCards, "continue");
        }
    }

    // 전투 종료 표시 (확인 버튼을 누르면 리스너 호출)
    private void showEnd(boolean win, String text, String reason) {
        won = win;
        message.setText(text + (win ? "\n\n승리!" : "\n\n패배 (" + reason + ")"));
        showCard(centerCards, "msg");
        showCard(bottomCards, "end");
    }

    // 패배 이유
    private String loseReason() {
        return engine.getPlayerHearts() <= 0 ? "하트 소진" : "공격 횟수 소진";
    }

    // ===== 보조 =====

    // 엔진 값으로 보스 정보와 하트 표시 갱신
    private void updateStatus() {
        int left = Math.max(0, engine.getBossHp());
        bossNameLabel.setText(engine.getBossName());
        bossHpBar.setMaximum(engine.getBossMaxHp());
        bossHpBar.setValue(left);
        bossHpBar.setString("남은 문제 " + left + "/" + engine.getBossMaxHp());
        attackLabel.setText("남은 공격 " + engine.getAttacksLeft());
        for (int i = 0; i < MAX_HEARTS; i++) {
            hearts[i].setForeground(i < engine.getPlayerHearts() ? Color.RED : Color.GRAY);
        }
        if (engine.getPlayerHearts() < lastHearts) flashHearts(); // 하트를 잃었으면 깜빡
        lastHearts = engine.getPlayerHearts();
    }

    // 하트 영역을 0.3초 동안 붉게 깜빡이기 시작
    private void flashHearts() {
        flashCount = 0;
        heartRow.setBackground(new Color(140, 20, 20));
        flashTimer.restart();
    }

    // 깜빡임 한 단계 (빨강 -> 원래 -> 빨강 -> 원래)
    private void flashStep() {
        flashCount++;
        heartRow.setBackground(flashCount % 2 == 0 ? new Color(140, 20, 20) : BG);
        if (flashCount >= 3) flashTimer.stop();
    }

    // 몬스터 자리: 회색 사각형 + 정답 시 흔들림과 '-1' 연출
    private class MonsterView extends JPanel {
        private int step = -1; // 연출 진행 단계 (-1이면 없음)
        private Timer anim = new Timer(30, e -> {
            step++;
            if (step > 20) {
                step = -1;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });

        MonsterView() {
            setOpaque(false);
            setPreferredSize(new Dimension(120, 110));
        }

        // 연출 시작
        void hit() {
            step = 0;
            anim.restart();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int dx = (step >= 0 && step < 10) ? (step % 2 == 0 ? 6 : -6) : 0; // 0.3초 흔들림
            g.setColor(Color.GRAY);
            g.fillRect(15 + dx, 20, 90, 80);
            if (step >= 0) {
                g.setColor(new Color(255, 80, 80, 255 * (20 - step) / 20)); // 점점 투명
                g.setFont(new Font("Dialog", Font.BOLD, 26));
                g.drawString("-1", 48, 80 - step * 2);                       // 위로 떠오름
            }
        }
    }

    // 카드 레이아웃 패널의 화면 전환
    private void showCard(JPanel cards, String name) {
        ((CardLayout) cards.getLayout()).show(cards, name);
    }

    // 흰색 굵은 글씨 라벨 생성
    private JLabel makeLabel(String text, int size) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Dialog", Font.BOLD, size));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }
}
