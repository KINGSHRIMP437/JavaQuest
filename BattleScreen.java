import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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
        
        // 스테이지에서 이미지 경로를 읽어와 MonsterView에 전달
        monsterView.setBossStyle(topicId, stageNo, stage.getImagePath());
        
        messageArea.setText("* 야생의 " + bossName + "이(가) 나타났다!\n* 힙 메모리 어딘가에서 이상한 기운이 느껴진다.");
        showCard(centerCards, "msg");
        showCard(bottomCards, "cmd");
        updateStatus();

        revalidate();
        repaint();
    }

    public void setBattleListener(BattleListener l) {
        listener = l;
    }

    public void startEnemyTurn() {
        monsterView.setAttackState();

        messageArea.setText("적의 턴! 방향키로 탄막을 피하세요!");

        showCard(centerCards, "dodge");
        showCard(bottomCards, "wait");

        centerCards.revalidate();
        centerCards.repaint();

        // 화면 전환 후 레이아웃이 끝난 다음에 시작해야 패널 크기가 0이 아님
        SwingUtilities.invokeLater(() -> {
            dodgeGamePanel.startDodgeGame(currentTopicId, stageNo);
            dodgeGamePanel.requestFocusInWindow();
        });
    }

    public void endEnemyTurn(int hitsTaken) {
        updateStatus();

        monsterView.setNormalState();

        String text = hitsTaken > 0 ? "탄막에 " + hitsTaken + "번 맞았다!" : "공격을 모두 피했다!";
        
        // 탄막 중 하트가 0이 되면 handleRealtimeHit에서 이미 패배 화면을 띄움
        if (engine.isLose()) {
            return;
        }

        if (isNameQuiz() && nameQuizPanel.hasQuestions()) {
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
        centerCards.setPreferredSize(new Dimension(900, 400));

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
        msgPanel.add(new JScrollPane(messageArea), BorderLayout.CENTER);

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

        dodgeGamePanel = new DodgeGamePanel(
            hits -> endEnemyTurn(hits),
            () -> handleRealtimeHit()
        );
        dodgeGamePanel.setPreferredSize(new Dimension(800, 380));
        dodgeGamePanel.setOpaque(true);

        // 탄막 패널은 800x380으로 고정해 가운데 배치 (창 크기에 따라 늘어나면 패턴 난이도가 달라짐)
        // 최소 크기도 같이 지정: GridBagLayout은 공간이 모자라면 최소 크기로 줄여 패널이 찌그러짐
        dodgeGamePanel.setMinimumSize(new Dimension(800, 380));
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

    private void handleRealtimeHit() {
        engine.hitByBullet();
        updateStatus(); 

        if (engine.isLose()) {
            dodgeGamePanel.stopDodgeGame();
            showEnd(false, "탄막에 맞아 쓰러졌다...", "패배");
        }
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
        bottomCards.setPreferredSize(new Dimension(900, 60));
        
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
            b.setBackground(new Color(25, 30, 50));
            b.setForeground(Color.WHITE);
            b.setFont(new Font("맑은 고딕", Font.BOLD, 18));
            b.setOpaque(true);
            b.setContentAreaFilled(true);
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
        b.setBackground(new Color(25, 30, 50));
        b.setForeground(Color.YELLOW);
        b.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        b.setOpaque(true);
        b.setContentAreaFilled(true);
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
        monsterView.setNormalState();

        if (quizPanel.isShowing() && timer.isRunning()) {
            return;
        }
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
        monsterView.setNormalState();
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
        private String topicId = "coding";
        private int stage = 1;

        private BufferedImage normalImage;
        private BufferedImage hitImage;
        private BufferedImage attackImage;
        private String currentState = "normal"; // "normal", "hit", "attack"

        // ---------- 정답 시 베기 연출 (이슈 #8, 전체 약 0.75초) ----------
        // 0~110ms 검기가 대각선으로 지나감 → 110ms 번쩍임 → 110~460ms 두 동강으로 벌어졌다 붙음
        // 110~410ms 좌우 떨림·금빛 베인 자국 / 110~560ms 불꽃 파편 / 110ms~ "-1" 팝업
        private static final int SLASH_MS = 110, SPLIT_MS = 350, SHAKE_MS = 300, SPARK_MS = 450, TOTAL_MS = 750;
        private long hitStart = -1;          // 연출 시작 시각(ms), -1이면 연출 없음
        private boolean slashFromRight;      // 베는 방향: 오른쪽 위→왼쪽 아래 / 왼쪽 위→오른쪽 아래 (맞힐 때마다 번갈아)
        private double[][] sparks = new double[0][];   // 파편 {시작 x, 시작 y, 속도 x, 속도 y, 크기}
        private Timer anim = new Timer(16, e -> {      // 약 60fps
            if (hitStart >= 0 && System.currentTimeMillis() - hitStart > TOTAL_MS) {
                hitStart = -1;
                ((Timer) e.getSource()).stop();
                if ("hit".equals(currentState)) currentState = "normal";   // 연출이 끝나면 평소 모습으로
            }
            repaint();
        });

        MonsterView() {
            setOpaque(false);
            setPreferredSize(new Dimension(240, 150));
        }

        // Stage에서 이미지 경로를 받아 로드하도록 매개변수 추가
        void setBossStyle(String topicId, int stage, String imagePath) {
            this.topicId = topicId;
            this.stage = stage;
            loadBossImages(imagePath);
            currentState = "normal";
            repaint();
        }

        private BufferedImage loadAndConvertImage(String imagePath) {
            if (imagePath == null) return null;
            try {
                File file = new File(imagePath);
                if (!file.exists()) return null;
                
                BufferedImage original = ImageIO.read(file);
                if (original == null) return null;

                // 투명 레이어(알파 채널)가 깨지지 않도록 ARGB 타입의 새 이미지로 완벽하게 복원/변환
                BufferedImage converted = new BufferedImage(
                    original.getWidth(), 
                    original.getHeight(), 
                    BufferedImage.TYPE_INT_ARGB
                );
                Graphics2D g2d = converted.createGraphics();
                g2d.drawImage(original, 0, 0, null);
                g2d.dispose();
                
                return converted;
            } catch (IOException e) {
                return null;
            }
        }

        private void loadBossImages(String imagePath) {
            normalImage = null;
            hitImage = null;
            attackImage = null;

            if (imagePath != null) {
                normalImage = loadAndConvertImage(imagePath);

                int dotIdx = imagePath.lastIndexOf('.');
                if (dotIdx != -1) {
                    String basePath = imagePath.substring(0, dotIdx);
                    String ext = imagePath.substring(dotIdx);

                    hitImage = loadAndConvertImage(basePath + "_hit" + ext);
                    attackImage = loadAndConvertImage(basePath + "_attack" + ext);
                }
            }
        }

        void setNormalState() {
            currentState = "normal";
            repaint();
        }

        void setHitState() {
            currentState = "hit";
            repaint();
        }

        void setAttackState() {
            currentState = "attack";
            repaint();
        }

        void hit() {
            setHitState();
            slashFromRight = !slashFromRight;
            hitStart = System.currentTimeMillis();

            // 파편 10~15개: 베인 선 가운데 근처에서 사방으로 (위쪽으로 조금 더 튀게)
            double[] u = slashDir();
            sparks = new double[10 + random.nextInt(6)][];
            for (int i = 0; i < sparks.length; i++) {
                double along = (random.nextDouble() - 0.5) * 60;
                double ang = random.nextDouble() * Math.PI * 2;
                double speed = 120 + random.nextDouble() * 180;   // px/초
                sparks[i] = new double[]{getWidth() / 2.0 + u[0] * along, getHeight() / 2.0 + u[1] * along,
                        Math.cos(ang) * speed, Math.sin(ang) * speed - 80, 2 + random.nextDouble() * 2.5};
            }
            anim.restart();
        }

        // 베는 방향 단위 벡터 (화면 좌표, y는 아래로)
        private double[] slashDir() {
            double x = slashFromRight ? -1 : 1, y = 0.75, len = Math.hypot(x, y);
            return new double[]{x / len, y / len};
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int panelW = getWidth();
            int panelH = getHeight();

            int width = 100 + (stage * 5);
            int height = 80 + (stage * 5);
            int startX = (panelW - width) / 2;
            int startY = (panelH - height) / 2;

            long t = hitStart < 0 ? -1 : System.currentTimeMillis() - hitStart;
            if (t < SLASH_MS) {   // 연출 없음 또는 검기가 지나가는 중: 보스는 그대로
                drawBoss(g2, startX, startY, width, height);
                if (t >= 0) drawSlash(g2, t / (double) SLASH_MS);
                return;
            }
            long a = t - SLASH_MS;   // 벤 순간부터 지난 시간

            // 보스를 따로 그려 두고 베인 선 기준 두 조각으로 나눠 그림
            BufferedImage boss = new BufferedImage(Math.max(1, panelW), Math.max(1, panelH), BufferedImage.TYPE_INT_ARGB);
            Graphics2D bg = boss.createGraphics();
            bg.setRenderingHints(g2.getRenderingHints());
            drawBoss(bg, startX, startY, width, height);
            bg.dispose();
            // 번쩍임용 흰 실루엣 (보스가 있는 픽셀만 흰색)
            BufferedImage white = new BufferedImage(boss.getWidth(), boss.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D wg = white.createGraphics();
            wg.drawImage(boss, 0, 0, null);
            wg.setComposite(AlphaComposite.SrcAtop);
            wg.setColor(Color.WHITE);
            wg.fillRect(0, 0, white.getWidth(), white.getHeight());
            wg.dispose();

            double[] u = slashDir();
            double[] n = {-u[1], u[0]};   // 베인 선의 수직 방향
            double cx = panelW / 2.0, cy = panelH / 2.0;
            double split = a < SPLIT_MS ? Math.sin(Math.PI * a / SPLIT_MS) : 0;   // 0 → 1 → 0 (벌어졌다 붙음)
            int shake = a < SHAKE_MS ? ((a / 30) % 2 == 0 ? 6 : -6) : 0;
            float flash = Math.max(0f, 1f - a / 120f);

            for (int side = -1; side <= 1; side += 2) {
                Graphics2D pg = (Graphics2D) g2.create();
                // 조각은 베인 선을 따라 서로 반대로 미끄러지고, 살짝 바깥으로 벌어짐
                pg.translate(shake + side * split * (14 * u[0] + 4 * n[0]), side * split * (14 * u[1] + 4 * n[1]));
                pg.clip(halfPlane(cx, cy, u, n, side));
                pg.drawImage(boss, 0, 0, null);
                if (flash > 0) {   // 번쩍임도 같은 변환·클립 안에서 그려 조각과 어긋나지 않음
                    pg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, flash));
                    pg.drawImage(white, 0, 0, null);
                }
                pg.dispose();
            }

            // 금빛 베인 자국: 선을 따라 남았다가 사라짐
            if (a < SHAKE_MS) {
                float k = 1f - a / (float) SHAKE_MS;
                g2.setStroke(new BasicStroke(3f * k + 1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(255, 215, 90, (int) (230 * k)));
                g2.drawLine((int) (cx + shake - u[0] * 90), (int) (cy - u[1] * 90), (int) (cx + shake + u[0] * 90), (int) (cy + u[1] * 90));
            }

            // 불꽃 파편: 중력으로 떨어지며 작아지고 사라짐
            if (a < SPARK_MS) {
                double s = a / 1000.0, k = 1 - a / (double) SPARK_MS;
                for (double[] p : sparks) {
                    double x = p[0] + p[2] * s, y = p[1] + p[3] * s + 0.5 * 900 * s * s;
                    double r = p[4] * k + 0.5;
                    g2.setColor(new Color(255, 200 + (int) (55 * k), 60, (int) (255 * k)));
                    g2.fill(new java.awt.geom.Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
                }
            }

            // "-1": 크게 튀어나왔다가(0.12초) 위로 떠오르며 사라짐
            double p = Math.min(1.0, a / (double) (TOTAL_MS - SLASH_MS));
            double scale = a < 120 ? 1.8 - 0.8 * a / 120.0 : 1.0;
            g2.setFont(new Font("Dialog", Font.BOLD, (int) Math.round(28 * scale)));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(new Color(255, 80, 80, (int) (255 * (1 - p))));
            g2.drawString("-1", (int) (cx - fm.stringWidth("-1") / 2.0), (int) (cy - 4 - p * 40));
        }

        // 검기: 대각선을 따라 지나가는 칼날 (progress 0~1). 흰 심지 + 노란 빛 2겹, 꼬리로 갈수록 가늘고 옅게
        private void drawSlash(Graphics2D g2, double progress) {
            double[] u = slashDir();
            double cx = getWidth() / 2.0, cy = getHeight() / 2.0, reach = 130;
            double headD = -reach + 2 * reach * progress;   // 칼날 끝 위치 (가운데 기준 거리)
            double tail = 110;
            int segs = 12;
            for (int i = 0; i < segs; i++) {
                double d0 = headD - tail * (i + 1) / segs, d1 = headD - tail * i / segs;
                float k = 1f - i / (float) segs;   // 1(칼날 끝) → 0(꼬리)
                int x0 = (int) (cx + u[0] * d0), y0 = (int) (cy + u[1] * d0);
                int x1 = (int) (cx + u[0] * d1), y1 = (int) (cy + u[1] * d1);
                drawStroke(g2, 18 * k, new Color(255, 220, 80, (int) (60 * k)), x0, y0, x1, y1);    // 넓고 옅은 빛
                drawStroke(g2, 8 * k, new Color(255, 200, 40, (int) (200 * k)), x0, y0, x1, y1);    // 좁고 진한 빛
                drawStroke(g2, 3 * k, new Color(255, 255, 255, (int) (255 * k)), x0, y0, x1, y1);   // 흰 심지
            }
        }

        private void drawStroke(Graphics2D g2, float w, Color c, int x0, int y0, int x1, int y1) {
            if (w < 0.5f) return;
            g2.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(c);
            g2.drawLine(x0, y0, x1, y1);
        }

        // 가운데(cx, cy)를 지나는 베인 선의 한쪽 절반 (side = 1 또는 -1)
        private Polygon halfPlane(double cx, double cy, double[] u, double[] n, int side) {
            double L = 1000;
            Polygon poly = new Polygon();
            poly.addPoint((int) (cx - u[0] * L), (int) (cy - u[1] * L));
            poly.addPoint((int) (cx + u[0] * L), (int) (cy + u[1] * L));
            poly.addPoint((int) (cx + u[0] * L + side * n[0] * L), (int) (cy + u[1] * L + side * n[1] * L));
            poly.addPoint((int) (cx - u[0] * L + side * n[0] * L), (int) (cy - u[1] * L + side * n[1] * L));
            return poly;
        }

        private void drawBoss(Graphics2D g2, int x, int y, int w, int h) {
            // 현재 상태에 맞는 이미지 선택 (없으면 기본 이미지로 Fallback)
            BufferedImage targetImage = normalImage;
            if ("hit".equals(currentState) && hitImage != null) {
                targetImage = hitImage;
            } else if ("attack".equals(currentState) && attackImage != null) {
                targetImage = attackImage;
            }

            if (targetImage != null) {
                int imgW = targetImage.getWidth(this);
                int imgH = targetImage.getHeight(this);
                if (imgW <= 0) imgW = w;
                if (imgH <= 0) imgH = h;

                int drawW = 200;
                int drawH = (int) (((double) imgH / imgW) * drawW);
                if (drawH > 130) {
                    drawH = 130;
                    drawW = (int) (((double) imgW / imgH) * drawH);
                }

                int startX = (getWidth() - drawW) / 2;
                int startY = (getHeight() - drawH) / 2;

                Composite originalComposite = g2.getComposite();
                g2.setComposite(AlphaComposite.SrcOver);

                g2.drawImage(targetImage, startX, startY, drawW, drawH, this);

                // 원래 컴포지트 복원
                g2.setComposite(originalComposite);
            } else {
                // 이미지가 없을 때의 기존 사각형 Fallback
                Color bossColor = new Color(0, 200, 190);
                if ("common".equals(topicId)) {
                    bossColor = new Color(240, 160, 40);
                } else if ("name".equals(topicId)) {
                    bossColor = new Color(200, 80, 220);
                }

                g2.setColor(bossColor);
                g2.fillRect(x, y, w, h);

                g2.setColor(stage == 3 ? Color.RED : Color.WHITE);
                g2.fillRect(x + 20, y + 20, 22, 22);
                g2.fillRect(x + w - 42, y + 20, 22, 22);
            }
        }
    }

    private void showCard(JPanel cards, String name) {
        ((CardLayout) cards.getLayout()).show(cards, name);
    }

    public int getPlayerHearts() {
        return engine.getPlayerHearts();
    }
}