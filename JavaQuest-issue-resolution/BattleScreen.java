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
    private List<QuizItem> allQuestions = new ArrayList<>(); 
    private List<QuizItem> pool = new ArrayList<>();         
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
    private NameQuizPanel nameQuizPanel; 

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
        
        int count = questions.size();
        int bossHp = stage.getBossHp(count);
        if (isNameQuiz()) { 
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

        SwingUtilities.invokeLater(() -> {
            dodgeGamePanel.startDodgeGame(currentTopicId, stageNo);
            dodgeGamePanel.requestFocusInWindow();

            System.out.println(
                "DodgeGamePanel 크기: "
                + dodgeGamePanel.getWidth() + " x "
                + dodgeGamePanel.getHeight()
            );
        });
    }

    public void endEnemyTurn(int hitsTaken) {
        updateStatus();

        monsterView.setNormalState();

        String text = hitsTaken > 0 ? "탄막에 " + hitsTaken + "번 맞았다!" : "공격을 모두 피했다!";
        
        if (engine.isLose()) {
            return;
        }

        if (isNameQuiz() && nameQuizPanel.hasQuestions()) {
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

        JPanel dodgeWrapper = new JPanel(new BorderLayout());
        dodgeWrapper.setOpaque(false);
        dodgeWrapper.add(dodgeGamePanel, BorderLayout.CENTER);

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
            } else if (i == 3) { 
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
            showCard(bottomCards, "wait"); 
            nameQuizPanel.startNext();
            return;
        }
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

            drawBoss(g2, startX, startY, width, height);
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