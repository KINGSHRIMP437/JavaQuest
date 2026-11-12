import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BattleScreen extends JPanel {
    private static final Color BG_COLOR = new Color(15, 20, 35);
    private static final Color BORDER_COLOR = new Color(0, 200, 190);
    private static final int MAX_HEARTS = 3;
    private static final int QUIZ_SECONDS = 15;

    private BattleEngine engine = new BattleEngine();
    private BattleListener listener;
    private Random random = new Random();
    private List<QuizItem> allQuestions = new ArrayList<>(); // 이번 스테이지 전체 문제
    private List<QuizItem> pool = new ArrayList<>();         // 아직 맞히지 않은 문제
    private QuizItem current;
    private int stageNo;
    private String currentTopicId;
    private boolean won;

    private JPanel centerCards = new JPanel(new CardLayout());
    private JPanel bottomCards = new JPanel(new CardLayout());
    
    private JPanel msgPanel;
    private JTextArea messageArea = new JTextArea();
    private JPanel quizPanel;
    private JTextArea questionText = new JTextArea();
    private JButton[] choiceBtns = new JButton[4];
    private JProgressBar timeBar = new JProgressBar();

    private JLabel stageLabel = new JLabel("", SwingConstants.LEFT);
    private JLabel turnLabel = new JLabel("", SwingConstants.RIGHT);
    private JLabel bossNameLabel = new JLabel("", SwingConstants.CENTER);
    private JProgressBar bossHpBar = new JProgressBar();
    
    private JLabel[] hearts = new JLabel[MAX_HEARTS];
    private JPanel heartRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    private MonsterView monsterView = new MonsterView();

    private Timer flashTimer;
    private int flashCount;
    private int lastHearts;

    private Timer timer;
    private int remain;

    private DodgeGamePanel dodgeGamePanel;
    private NameQuizPanel nameQuizPanel; // 이름 맞추기 주제 전용 이미지 퀴즈

    public BattleScreen() {
        setPreferredSize(new Dimension(1050, 800));
        setBackground(BG_COLOR);
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        initTopArea();
        initCenterArea();
        initBottomArea();

        timer = new Timer(100, e -> tick());
        flashTimer = new Timer(100, e -> flashStep());
    }

    public void startBattle(String topicName, Stage stage, List<QuizItem> questions) {
        String topicId = stage.getTopic();
        String bossName = stage.getBossName();
        this.stageNo = stage.getStageNo();
        this.currentTopicId = topicId;
        allQuestions = new ArrayList<>(questions);
        pool = new ArrayList<>(questions);
        // 공격 횟수 = 문제 수, 보스 HP = 문제 수 × 스테이지 HP 비율
        int count = questions.size();
        int bossHp = stage.getBossHp(count);
        if (isNameQuiz()) { // 이름 맞추기: 무작위 5문제, 보스 HP는 스테이지별 (없으면 기존 4지선다)
            int n = nameQuizPanel.prepare(stageNo);
            if (n > 0) { count = n; bossHp = NameQuizPanel.bossHpFor(stageNo, n); }
        }
        engine.startBattle(bossName, bossHp, count);
        timer.stop();
        lastHearts = engine.getPlayerHearts();

        stageLabel.setText("STAGE " + stageNo + " · " + topicName);
        bossNameLabel.setText(bossName);
        bossHpBar.setMaximum(engine.getBossMaxHp());
        bossHpBar.setValue(engine.getBossHp());
        
        monsterView.setBossStyle(topicId, stageNo);
        
        messageArea.setText("* 야생의 " + bossName + "이(가) 나타났다!\n* 힙 메모리 어딘가에서 이상한 기운이 느껴진다.");
        showCard(centerCards, "msg");
        showCard(bottomCards, "cmd");
        updateStatus();
    }

    public void setBattleListener(BattleListener l) {
        listener = l;
    }

    public void startEnemyTurn() {
        messageArea.setText("적의 턴! 방향키로 탄막을 피하세요!");
        showCard(centerCards, "dodge");
        showCard(bottomCards, "wait");
        dodgeGamePanel.startDodgeGame(currentTopicId, stageNo);
    }

    public void endEnemyTurn(int hitsTaken) {
        for (int i = 0; i < hitsTaken; i++) engine.hitByBullet();
        updateStatus();

        String text = hitsTaken > 0 ? "탄막에 " + hitsTaken + "번 맞았다!" : "공격을 모두 피했다!";
        if (engine.isLose()) {
            showEnd(false, text, "패배");
        } else if (isNameQuiz() && nameQuizPanel.hasQuestions()) {
            // 이름 맞추기: 결과를 1초 보여 주고 버튼 없이 다음 문제로
            messageArea.setText(text + "\n\n다음 문제!");
            showCard(centerCards, "msg");
            showCard(bottomCards, "wait");
            Timer next = new Timer(1000, e -> startQuiz());
            next.setRepeats(false);
            next.start();
        } else {
            messageArea.setText(text);
            showCard(centerCards, "msg");
            showCard(bottomCards, "cmd");
        }
    }

    public void debugHit() {
        engine.hitByBullet();
        updateStatus();
    }

    private void initTopArea() {
        JPanel topPanel = new JPanel();
        topPanel.setOpaque(false);
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JPanel headerTop = new JPanel(new BorderLayout());
        headerTop.setOpaque(false);
        stageLabel.setForeground(Color.YELLOW);
        stageLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));

        turnLabel.setForeground(Color.WHITE);
        turnLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));

        headerTop.add(stageLabel, BorderLayout.WEST);
        headerTop.add(turnLabel, BorderLayout.EAST);
        topPanel.add(headerTop);
        topPanel.add(Box.createVerticalStrut(10));

        JPanel monsterCenter = new JPanel(new FlowLayout(FlowLayout.CENTER));
        monsterCenter.setOpaque(false);
        monsterCenter.add(monsterView);
        topPanel.add(monsterCenter);
        topPanel.add(Box.createVerticalStrut(8));

        bossNameLabel.setForeground(Color.WHITE);
        bossNameLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        bossNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(bossNameLabel);
        topPanel.add(Box.createVerticalStrut(6));

        bossHpBar.setStringPainted(true);
        bossHpBar.setForeground(new Color(220, 50, 50));
        bossHpBar.setBackground(new Color(30, 35, 50));
        bossHpBar.setBorder(new LineBorder(Color.GRAY, 1));
        bossHpBar.setPreferredSize(new Dimension(500, 22));
        bossHpBar.setMaximumSize(new Dimension(500, 22));
        bossHpBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(bossHpBar);

        add(topPanel, BorderLayout.NORTH);
    }

    private void initCenterArea() {
        centerCards.setOpaque(false);

        msgPanel = new JPanel(new BorderLayout());
        msgPanel.setOpaque(false);
        msgPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(new Font("맑은 고딕", Font.PLAIN, 20));
        messageArea.setForeground(Color.WHITE);
        messageArea.setBackground(new Color(25, 30, 50));
        messageArea.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        msgPanel.add(messageArea, BorderLayout.CENTER);

        quizPanel = new JPanel(new BorderLayout(10, 10));
        quizPanel.setOpaque(false);
        quizPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel quizContent = new JPanel(new BorderLayout(5, 5));
        quizContent.setOpaque(false);
        quizContent.setBorder(new LineBorder(Color.GRAY, 1));
        quizContent.setBackground(new Color(20, 25, 45));

        timeBar.setForeground(BORDER_COLOR);
        quizContent.add(timeBar, BorderLayout.NORTH);

        questionText.setEditable(false);
        questionText.setLineWrap(true);
        questionText.setWrapStyleWord(true);
        questionText.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        questionText.setForeground(Color.WHITE);
        questionText.setBackground(new Color(20, 25, 45));
        questionText.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));
        quizContent.add(questionText, BorderLayout.CENTER);

        JPanel choiceGrid = new JPanel(new GridLayout(2, 2, 12, 12));
        choiceGrid.setOpaque(false);
        choiceGrid.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            choiceBtns[i] = new JButton();
            choiceBtns[i].setBackground(new Color(25, 30, 50));
            choiceBtns[i].setForeground(Color.WHITE);
            choiceBtns[i].setFont(new Font("맑은 고딕", Font.PLAIN, 16));
            choiceBtns[i].setHorizontalAlignment(SwingConstants.LEFT);
            choiceBtns[i].setBorder(new LineBorder(Color.GRAY, 1));
            choiceBtns[i].addActionListener(e -> answer(idx));
            choiceGrid.add(choiceBtns[i]);
        }
        quizContent.add(choiceGrid, BorderLayout.SOUTH);
        quizPanel.add(quizContent, BorderLayout.CENTER);

        dodgeGamePanel = new DodgeGamePanel(hits -> endEnemyTurn(hits));
        dodgeGamePanel.setPreferredSize(new Dimension(800, 380));
        dodgeGamePanel.setOpaque(true);

        JPanel dodgeWrapper = new JPanel(new GridBagLayout());
        dodgeWrapper.setOpaque(false);
        dodgeWrapper.add(dodgeGamePanel);

        centerCards.add(msgPanel, "msg");
        centerCards.add(quizPanel, "quiz");
        centerCards.add(dodgeWrapper, "dodge");

        nameQuizPanel = new NameQuizPanel((correct, timeout, quiz) -> finishNameQuiz(correct, timeout, quiz));
        centerCards.add(nameQuizPanel, "nameQuiz");

        add(centerCards, BorderLayout.CENTER);
    }

    private void initBottomArea() {
        JPanel bottomWrap = new JPanel();
        bottomWrap.setOpaque(false);
        bottomWrap.setLayout(new BoxLayout(bottomWrap, BoxLayout.Y_AXIS));

        heartRow.setBackground(BG_COLOR);
        heartRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int i = 0; i < MAX_HEARTS; i++) {
            hearts[i] = new JLabel("♥");
            hearts[i].setFont(new Font("Dialog", Font.BOLD, 32));
            heartRow.add(hearts[i]);
        }
        bottomWrap.add(heartRow);
        bottomWrap.add(Box.createVerticalStrut(8));

        bottomCards.setOpaque(false);
        bottomCards.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottomCards.add(makeCommandPanel(), "cmd");
        bottomCards.add(makeSingleButtonPanel("계속", () -> startEnemyTurn()), "continue");
        
        JPanel waitPanel = new JPanel();
        waitPanel.setOpaque(false);
        bottomCards.add(waitPanel, "wait");

        bottomCards.add(makeSingleButtonPanel("확인", () -> {
            if (listener != null) listener.onBattleEnd(won);
        }), "end");

        bottomWrap.add(bottomCards);
        add(bottomWrap, BorderLayout.SOUTH);
    }

    private JPanel makeCommandPanel() {
        JPanel p = new JPanel(new GridLayout(1, 4, 15, 0));
        p.setOpaque(false);
        String[] names = {"◆ 공격", "Q 디버그", "아이템", "→ 도망"};
        for (int i = 0; i < 4; i++) {
            final String name = names[i];
            JButton b = new JButton(name);
            b.setBackground(new Color(15, 20, 35));
            b.setForeground(Color.WHITE);
            b.setFont(new Font("맑은 고딕", Font.BOLD, 18));
            b.setPreferredSize(new Dimension(0, 50));
            b.setBorder(new LineBorder(i == 0 ? BORDER_COLOR : Color.GRAY, i == 0 ? 3 : 2));
            b.setFocusPainted(false);

            if (i == 0) {
                b.addActionListener(e -> startQuiz());
            } else if (i == 3) { // 도망: 패배 처리 없이 스테이지 맵으로
                b.addActionListener(e -> { if (listener != null) listener.onBattleEnd(false); });
            } else {
                b.addActionListener(e -> messageArea.setText("준비 중인 기능입니다."));
            }
            p.add(b);
        }
        return p;
    }

    private JPanel makeSingleButtonPanel(String text, Runnable action) {
        JPanel p = new JPanel(new GridLayout(1, 1));
        p.setOpaque(false);
        JButton b = new JButton(text);
        b.setBackground(new Color(15, 20, 35));
        b.setForeground(Color.YELLOW);
        b.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        b.setPreferredSize(new Dimension(0, 50));
        b.setBorder(new LineBorder(BORDER_COLOR, 2));
        b.addActionListener(e -> action.run());
        p.add(b);
        return p;
    }

    private boolean isNameQuiz() {
        return "name".equals(currentTopicId);
    }

    // 이름 맞추기 결과: 정답이면 보스 HP -1, 오답·시간 초과면 공격 기회만 소모 (하트는 탄막에서만 감소)
    // 전투가 끝나지 않았으면 결과 글자를 잠깐 띄우고 Enter 없이 탄막으로 → 탄막 후 다음 문제
    private void finishNameQuiz(boolean correct, boolean timeout, NameQuiz quiz) {
        engine.submitAnswer(correct);
        if (correct) monsterView.hit();
        updateStatus();

        String text = correct ? "정답! " : (timeout ? "시간 초과! " : "오답! ") + "(정답: " + quiz.getMainAnswer() + ")";
        if (engine.isWin()) {
            showEnd(true, text + "\n\n해설: " + quiz.getExplain(), "");
        } else if (engine.isLose()) {
            showEnd(false, text + "\n\n해설: " + quiz.getExplain(), "패배");
        } else {
            String shortText = correct ? "정답!" : (timeout ? "시간 초과! " : "오답! ") + "정답: " + quiz.getMainAnswer();
            nameQuizPanel.showResult(shortText, correct, correct ? 1000 : 1500, this::startEnemyTurn);
        }
    }

    private void startQuiz() {
        if (isNameQuiz() && nameQuizPanel.hasQuestions()) {
            showCard(centerCards, "nameQuiz");
            showCard(bottomCards, "wait"); // 퀴즈 중에는 공격 버튼 숨김 (연타 방지)
            nameQuizPanel.startNext();
            return;
        }
        // 보스 HP가 문제 수보다 많으면 문제를 다 맞힌 뒤 처음부터 다시 출제
        if (pool.isEmpty()) pool = new ArrayList<>(allQuestions);
        if (pool.isEmpty()) return;
        current = pool.get(random.nextInt(pool.size()));

        questionText.setText("Q. " + current.text + "\n");
        for (int i = 0; i < 4; i++) {
            char prefix = (char) ('A' + i);
            choiceBtns[i].setText("  " + prefix + ". " + current.choices[i]);
        }

        remain = QUIZ_SECONDS * 10;
        timeBar.setMaximum(remain);
        timeBar.setValue(remain);

        showCard(centerCards, "quiz");
        timer.restart();
    }

    private void tick() {
        remain--;
        timeBar.setValue(remain);
        if (remain <= 0) finishQuiz(false, "시간 초과! ");
    }

    private void answer(int idx) {
        finishQuiz(idx == current.answer, "");
    }

    private void finishQuiz(boolean correct, String prefix) {
        timer.stop();
        engine.submitAnswer(correct);
        if (correct) {
            pool.remove(current);
            monsterView.hit();
        }
        updateStatus();

        String text = correct ? "정답! " : prefix + "오답 (정답: " + (current.answer + 1) + "번) ";
        text += "\n\n해설: " + current.explain;

        if (engine.isWin()) {
            showEnd(true, text, "");
        } else if (engine.isLose()) {
            showEnd(false, text, "패배");
        } else {
            messageArea.setText(text);
            showCard(centerCards, "msg");
            showCard(bottomCards, "continue");
        }
    }

    private void showEnd(boolean win, String text, String reason) {
        won = win;
        messageArea.setText(text + (win ? "\n\n[승리!]" : "\n\n[패배]"));
        showCard(centerCards, "msg");
        showCard(bottomCards, "end");
    }

    private void updateStatus() {
        int left = Math.max(0, engine.getBossHp());
        bossHpBar.setValue(left);
        bossHpBar.setString("HP: " + left + " / " + engine.getBossMaxHp());
        turnLabel.setText("남은 공격: " + engine.getAttacksLeft() + "회");
        
        for (int i = 0; i < MAX_HEARTS; i++) {
            hearts[i].setForeground(i < engine.getPlayerHearts() ? Color.RED : Color.GRAY);
        }
        if (engine.getPlayerHearts() < lastHearts) flashHearts();
        lastHearts = engine.getPlayerHearts();
    }

    private void flashHearts() {
        flashCount = 0;
        heartRow.setBackground(new Color(140, 20, 20));
        flashTimer.restart();
    }

    private void flashStep() {
        flashCount++;
        heartRow.setBackground(flashCount % 2 == 0 ? new Color(140, 20, 20) : BG_COLOR);
        if (flashCount >= 3) flashTimer.stop();
    }

    private class MonsterView extends JPanel {
        private int step = -1;
        private String topicId = "coding";
        private int stage = 1;

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
            setPreferredSize(new Dimension(160, 120));
        }

        void setBossStyle(String topicId, int stage) {
            this.topicId = topicId;
            this.stage = stage;
            repaint();
        }

        void hit() {
            step = 0;
            anim.restart();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int dx = (step >= 0 && step < 10) ? (step % 2 == 0 ? 6 : -6) : 0;
            
            Color bossColor = new Color(0, 200, 190);
            if ("common".equals(topicId)) {
                bossColor = new Color(240, 160, 40);
            } else if ("name".equals(topicId)) {
                bossColor = new Color(200, 80, 220);
            }

            int width = 100 + (stage * 5);
            int height = 80 + (stage * 5);

            g.setColor(bossColor);
            g.fillRect(30 + dx, 30, width, height);

            g.setColor(stage == 3 ? Color.RED : Color.WHITE);
            g.fillRect(50 + dx, 50, 22, 22);
            g.fillRect(30 + width - 42 + dx, 50, 22, 22);

            if (step >= 0) {
                g.setColor(new Color(255, 80, 80, 255 * (20 - step) / 20));
                g.setFont(new Font("Dialog", Font.BOLD, 28));
                g.drawString("-1", 65, 85 - step * 2);
            }
        }
    }

    private void showCard(JPanel cards, String name) {
        ((CardLayout) cards.getLayout()).show(cards, name);
    }
}