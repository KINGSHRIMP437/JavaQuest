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
    private static final int MAX_HEARTS = BattleEngine.MAX_HEARTS;
    private static final int QUIZ_SECONDS = 15;
    private static final int WIN_DELAY_MS = 1000;   // 마지막 일격 베기 연출(약 0.75초)이 끝난 뒤 클리어 창으로

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
    private JLabel stageStarsLabel = new JLabel("", SwingConstants.LEFT); // 이번 전투의 별점 (남은 하트에 따라 실시간)
    private JLabel turnLabel = new JLabel("", SwingConstants.RIGHT);
    private JButton skipBtn;   // 시연용: 무적 모드에서만 왼쪽 위에 보임, 나의 턴(명령 고르기)에만 누를 수 있음
    private final TurnCards turnCards = new TurnCards(this);   // 오른쪽 위 턴 카드 (턴이 바뀌면 튀어나왔다가 들어감)
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
    private JLabel dodgeStatus = new JLabel(" ");   // 탄막 필드 아래 상태 글자 (주제·스테이지·피격 수)
    private NameQuizPanel nameQuizPanel; // 이름 맞추기 주제 전용 이미지 퀴즈

    // ---------- 아이템 ----------
    private static final int INVINCIBLE_MS = 3000;   // 무적 시간
    private static final Color GOLD = new Color(255, 215, 0);
    private Inventory inventory = new Inventory();
    private ItemIcon.Slot[] itemSlots = new ItemIcon.Slot[Inventory.TYPES];   // 하트 줄 오른쪽 아이템 칸
    private JButton[] itemBtns = new JButton[Inventory.TYPES];   // 아이템 메뉴 버튼
    private ItemIcon[] itemBtnIcons = new ItemIcon[Inventory.TYPES];
    private Timer invincibleTimer;                               // 무적 남은 시간 갱신 (0.1초)
    private Timer itemMsgTimer;                                  // 탄막 중 사용 결과 문구를 잠깐 보였다 되돌림

    public BattleScreen() {
        setPreferredSize(new Dimension(1050, 800));
        setBackground(BG_COLOR);
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        initTopArea();
        initCenterArea();
        initBottomArea();

        timer = new Timer(100, e -> tick());
        flashTimer = new Timer(100, e -> flashStep());
        invincibleTimer = new Timer(100, e -> {
            updateStatus();
            if (!engine.isInvincible()) invincibleTimer.stop();   // 무적이 끝나면 원래대로 돌리고 멈춤
        });
        itemMsgTimer = new Timer(1500, e -> dodgeStatus.setText(dodgeGamePanel.getStatusText()));
        itemMsgTimer.setRepeats(false);
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
        inventory = new Inventory();   // 전투마다 아이템 1개씩 새로 지급 (누적 없음)
        timer.stop();
        invincibleTimer.stop();
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
        turnCards.show(true);
        skipBtn.setVisible(BattleEngine.isGodMode());
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
        showCard(bottomCards, "dodge");
        turnCards.show(false);

        centerCards.revalidate();
        centerCards.repaint();

        // 화면 전환 후 레이아웃이 끝난 다음에 시작해야 패널 크기가 0이 아님
        SwingUtilities.invokeLater(() -> {
            dodgeGamePanel.startDodgeGame(currentTopicId, stageNo);
            dodgeGamePanel.requestFocusInWindow();
            dodgeStatus.setText(dodgeGamePanel.getStatusText());
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

        // 모든 주제 공통: 탄막이 끝나면 공격·도망 버튼 (이름 맞추기도 여기서 도망칠 수 있게)
        messageArea.setText(text);
        showCard(centerCards, "msg");
        showCard(bottomCards, "cmd");
        turnCards.show(true);
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

        // 좌측 상단: 스테이지 텍스트, 그 아래에 이번 전투의 별점
        JPanel leftHeader = new JPanel();
        leftHeader.setLayout(new BoxLayout(leftHeader, BoxLayout.Y_AXIS));
        leftHeader.setOpaque(false);

        stageLabel.setForeground(Color.YELLOW);
        stageLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));

        stageStarsLabel.setForeground(new Color(255, 215, 0)); // 금색
        stageStarsLabel.setFont(new Font("Dialog", Font.BOLD, 22));

        stageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        stageStarsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftHeader.add(stageLabel);
        leftHeader.add(stageStarsLabel);

        // 시연용 스킵: 보스를 바로 쓰러뜨리고 클리어 창 (무적 모드에서만 보임)
        skipBtn = new JButton("스킵하기");
        skipBtn.setBackground(new Color(25, 30, 50));
        skipBtn.setForeground(Color.YELLOW);
        skipBtn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        skipBtn.setBorder(new LineBorder(Color.YELLOW, 1));
        skipBtn.setFocusPainted(false);
        skipBtn.setPreferredSize(new Dimension(90, 30));
        skipBtn.setMaximumSize(new Dimension(90, 30));
        skipBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        skipBtn.setVisible(false);
        skipBtn.addActionListener(e -> {
            engine.skipBattle();
            monsterView.hit();
            updateStatus();
            showEnd(true, "", "");
        });
        leftHeader.add(Box.createVerticalStrut(4));
        leftHeader.add(skipBtn);

        turnLabel.setForeground(Color.WHITE);
        turnLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));

        headerTop.add(leftHeader, BorderLayout.WEST);
        headerTop.add(turnLabel, BorderLayout.EAST);
        topPanel.add(headerTop);
        topPanel.add(Box.createVerticalStrut(4));

        JPanel monsterCenter = new JPanel(new FlowLayout(FlowLayout.CENTER));
        monsterCenter.setOpaque(false);
        monsterCenter.add(monsterView);
        topPanel.add(monsterCenter);
        topPanel.add(Box.createVerticalStrut(4));

        bossNameLabel.setForeground(Color.WHITE);
        bossNameLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        bossNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(bossNameLabel);
        topPanel.add(Box.createVerticalStrut(4));

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
        centerCards.setPreferredSize(new Dimension(900, 480));

        msgPanel = new JPanel(new BorderLayout());
        msgPanel.setOpaque(false);
        msgPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(new Font("맑은 고딕", Font.PLAIN, 24));
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
        // 시간 막대·문제는 위에 필요한 만큼, 선택지는 남는 공간을 채움 (창이 작으면 선택지가 줄어듦)
        JPanel quizHeader = new JPanel(new BorderLayout());
        quizHeader.setOpaque(false);
        quizHeader.add(timeBar, BorderLayout.NORTH);

        questionText.setEditable(false);
        questionText.setLineWrap(true);
        questionText.setWrapStyleWord(true);
        questionText.setFont(new Font("맑은 고딕", Font.BOLD, 26));
        questionText.setForeground(Color.WHITE);
        questionText.setBackground(new Color(20, 25, 45));
        questionText.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));
        quizHeader.add(questionText, BorderLayout.CENTER);
        quizContent.add(quizHeader, BorderLayout.NORTH);

        JPanel choiceGrid = new JPanel(new GridLayout(2, 2, 12, 12));
        choiceGrid.setOpaque(false);
        choiceGrid.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            choiceBtns[i] = new JButton();
            choiceBtns[i].setBackground(new Color(25, 30, 50));
            choiceBtns[i].setForeground(Color.WHITE);
            choiceBtns[i].setFont(new Font("맑은 고딕", Font.PLAIN, 22));
            choiceBtns[i].setPreferredSize(new Dimension(0, 110));
            choiceBtns[i].setHorizontalAlignment(SwingConstants.LEFT);
            choiceBtns[i].setBorder(new LineBorder(Color.GRAY, 1));
            choiceBtns[i].addActionListener(e -> answer(idx));
            choiceGrid.add(choiceBtns[i]);
        }
        quizContent.add(choiceGrid, BorderLayout.CENTER);
        quizPanel.add(quizContent, BorderLayout.CENTER);

        dodgeGamePanel = new DodgeGamePanel(
            hits -> endEnemyTurn(hits),
            () -> handleRealtimeHit()
        );
        dodgeGamePanel.setPreferredSize(new Dimension(800, 380));
        dodgeGamePanel.setOpaque(true);
        // 탄막 중 숫자 키 1/2/3 (윗줄·키패드)으로 아이템 사용 (기존 방향키 리스너는 그대로)
        dodgeGamePanel.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                int k = e.getKeyCode();
                int type = k >= java.awt.event.KeyEvent.VK_1 && k <= java.awt.event.KeyEvent.VK_3 ? k - java.awt.event.KeyEvent.VK_1
                        : k >= java.awt.event.KeyEvent.VK_NUMPAD1 && k <= java.awt.event.KeyEvent.VK_NUMPAD3 ? k - java.awt.event.KeyEvent.VK_NUMPAD1
                        : -1;
                if (type < 0 || !dodgeGamePanel.isShowing() || engine.isWin() || engine.isLose()) return;
                dodgeStatus.setText(useItem(type));
                itemMsgTimer.restart();
            }
        });

        // 탄막 패널은 최대 800x380, 공간이 모자라면 남은 크기에 맞춰 줄여 가운데 배치
        // - 창이 크다고 늘리지 않음: 필드가 커지면 패턴 난이도가 달라짐
        // - 창이 작으면 줄임: 고정 크기로 두면 필드 아랫부분(테두리)이 잘려 보임 (패턴은 패널 크기 기준으로 그려짐)
        JPanel dodgeWrapper = new JPanel(null) {
            @Override
            public void doLayout() {
                int w = Math.min(800, getWidth()), h = Math.min(380, getHeight());
                dodgeGamePanel.setBounds((getWidth() - w) / 2, (getHeight() - h) / 2, w, h);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(800, 380);
            }
        };
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
        dodgeStatus.setText(dodgeGamePanel.getStatusText());

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
        for (int i = 0; i < MAX_HEARTS; i++) {
            hearts[i] = new JLabel("♥");
            hearts[i].setFont(new Font("Dialog", Font.BOLD, 32));
            heartRow.add(hearts[i]);
        }
        // 하트 줄 오른쪽에 아이템 칸 3개 (투명: 하트 깜빡임 배경과 어울리게)
        JPanel itemRow = new JPanel(new GridBagLayout());
        itemRow.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(0, 4, 0, 0);
        for (int i = 0; i < Inventory.TYPES; i++) {
            itemSlots[i] = new ItemIcon.Slot(i);
            itemRow.add(itemSlots[i], gc);
        }
        JPanel heartLine = new JPanel(new BorderLayout());
        heartLine.setOpaque(false);
        heartLine.setAlignmentX(Component.LEFT_ALIGNMENT);
        heartLine.add(heartRow, BorderLayout.WEST);
        heartLine.add(itemRow, BorderLayout.EAST);
        bottomWrap.add(heartLine);
        bottomWrap.add(Box.createVerticalStrut(4));

        bottomCards.setOpaque(false);
        bottomCards.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottomCards.setPreferredSize(new Dimension(900, 80));
        
        bottomCards.add(makeCommandPanel(), "cmd");
        bottomCards.add(makeItemPanel(), "items");
        bottomCards.add(makeSingleButtonPanel("계속", () -> startEnemyTurn()), "continue");
        
        JPanel waitPanel = new JPanel();
        waitPanel.setOpaque(false);
        bottomCards.add(waitPanel, "wait");

        // 적의 턴: 명령 버튼 자리에 주제·스테이지·피격 수 표시 (탄막 필드 밖)
        JPanel dodgeStatusPanel = new JPanel(new BorderLayout());
        dodgeStatusPanel.setOpaque(false);
        dodgeStatus.setForeground(Color.YELLOW);
        dodgeStatus.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        dodgeStatusPanel.add(dodgeStatus, BorderLayout.WEST);
        bottomCards.add(dodgeStatusPanel, "dodge");

        bottomCards.add(makeSingleButtonPanel("확인", () -> {
            if (listener != null) listener.onBattleEnd(won);
        }), "end");

        bottomWrap.add(bottomCards);
        add(bottomWrap, BorderLayout.SOUTH);
    }

    private JPanel makeCommandPanel() {
        JPanel p = new JPanel(new GridLayout(1, 3, 15, 0));
        p.setOpaque(false);
        String[] names = {"◆ 공격", "아이템", "→ 도망"};
        for (int i = 0; i < names.length; i++) {
            final String name = names[i];
            JButton b = new JButton(name);
            b.setBackground(new Color(25, 30, 50));
            b.setForeground(Color.WHITE);
            b.setFont(new Font("맑은 고딕", Font.BOLD, 24));
            b.setOpaque(true);
            b.setContentAreaFilled(true);
            b.setPreferredSize(new Dimension(0, 70));
            b.setBorder(new LineBorder(i == 0 ? BORDER_COLOR : Color.GRAY, i == 0 ? 3 : 2));
            b.setFocusPainted(false);

            if (i == 0) {
                b.addActionListener(e -> {
                    // 인트로 노래는 공격 버튼까지, 이후 퀴즈·탄막 중에는 전투 노래 (이미 나오고 있으면 이어서)
                    SoundManager.playBgm(SoundManager.BGM_BATTLE);
                    startQuiz();
                });
            } else if (i == 2) { // 도망: 패배 처리 없이 스테이지 맵으로
                b.addActionListener(e -> { if (listener != null) listener.onBattleEnd(false); });
            } else {
                b.addActionListener(e -> {
                    messageArea.setText("사용할 아이템을 고르세요.");
                    showCard(bottomCards, "items");
                });
            }
            p.add(b);
        }
        return p;
    }

    // 아이템 메뉴: [무적] [체력 완충] [문제 개수 초기화] [← 뒤로]
    private JPanel makeItemPanel() {
        JPanel p = new JPanel(new GridLayout(1, 4, 15, 0));
        p.setOpaque(false);
        for (int i = 0; i < Inventory.TYPES; i++) {
            final int type = i;
            itemBtns[i] = styledButton("");
            itemBtnIcons[i] = new ItemIcon(i, 30);
            itemBtns[i].setIcon(itemBtnIcons[i]);
            itemBtns[i].setVerticalTextPosition(SwingConstants.BOTTOM);
            itemBtns[i].setHorizontalTextPosition(SwingConstants.CENTER);
            itemBtns[i].setIconTextGap(2);
            itemBtns[i].addActionListener(e -> {
                // 무적은 탄막 중 키(1)로만 사용 (버튼을 비활성으로 두면 글자색이 달라져서 문구로 안내)
                messageArea.setText(type == Inventory.INVINCIBLE ? "무적은 탄막 중에 숫자 키 1로 사용해요."
                        : useItem(type));   // 결과 문구 표시 → useItem 안에서 막대도 갱신
            });
            p.add(itemBtns[i]);
        }
        JButton back = styledButton("← 뒤로");
        back.addActionListener(e -> showCard(bottomCards, "cmd"));
        p.add(back);
        return p;
    }

    private JButton styledButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(25, 30, 50));
        b.setForeground(Color.WHITE);
        b.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setPreferredSize(new Dimension(0, 70));
        b.setBorder(new LineBorder(Color.GRAY, 2));
        b.setFocusPainted(false);
        return b;
    }

    // 아이템 사용 (메뉴·숫자 키 공통): 효과가 없으면 소모하지 않음. 결과 문구를 반환
    private String useItem(int type) {
        String name = Inventory.name(type);
        if (!inventory.has(type)) return name + "이(가) 없어요.";
        String result;
        switch (type) {
            case Inventory.INVINCIBLE:
                if (!engine.activateInvincible(INVINCIBLE_MS)) return "이미 무적 중이에요.";
                invincibleTimer.restart();
                result = "무적! 3초 동안 탄막에 맞아도 하트가 줄지 않아요.";
                break;
            case Inventory.HEAL_FULL:
                if (!engine.healFull()) return "이미 하트가 가득 찼어요.";
                result = "체력이 가득 찼어요!";
                break;
            default:
                if (!engine.resetAttacks()) return "아직 공격 횟수를 다 안 썼어요.";
                result = "공격 횟수가 " + engine.getAttacksLeft() + "회로 돌아왔어요!";
        }
        inventory.use(type);
        updateStatus();
        return result;
    }

    // 아이템 칸(하트 줄 오른쪽)과 메뉴 버튼 갱신: 개수 0이면 흐리게, 무적 중이면 1번 칸을 금색 남은 시간으로
    private void updateItemViews() {
        for (int i = 0; i < Inventory.TYPES; i++) {
            int n = inventory.count(i);
            boolean inv = i == Inventory.INVINCIBLE && engine.isInvincible();
            itemSlots[i].set(inv ? String.format("%.1fs", engine.getInvincibleMsLeft() / 1000.0) : String.valueOf(n),
                    n == 0 && !inv, inv);

            itemBtnIcons[i].setDim(n == 0);
            itemBtns[i].setText(Inventory.name(i) + " ×" + n);
            itemBtns[i].setForeground(n == 0 ? Color.GRAY : Color.WHITE);
            itemBtns[i].repaint();
        }
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
            // HTML 라벨로 넣어 긴 보기는 버튼 폭에서 줄바꿈 (<, > 같은 기호는 이스케이프)
            String choice = String.valueOf(current.choices[i])
                    .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            choiceBtns[i].setText("<html><body style='width:390px'>&nbsp;" + prefix + ". " + choice + "</body></html>");
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
        invincibleTimer.stop();
        itemMsgTimer.stop();
        if (win) {
            // 승리: 정답·해설과 "확인" 버튼 없이, 보스 베는 연출이 끝나면 맵으로 → 클리어 창(또는 축하 화면)
            // 연출 동안에는 보스와 처치 메시지만 보여 주고 버튼은 숨김 (선택지 연타로 두 번 끝나지 않게)
            // 소리는 마지막 일격의 베기 효과음만 (bossdeath는 쓰지 않음)
            messageArea.setText("* " + engine.getBossName() + "을(를) 쓰러뜨렸다!");
            showCard(centerCards, "msg");
            showCard(bottomCards, "wait");
            Timer toClear = new Timer(WIN_DELAY_MS, e -> {
                monsterView.setNormalState();
                if (listener != null) listener.onBattleEnd(true);
            });
            toClear.setRepeats(false);
            toClear.start();
            return;
        }
        // 패배: 전투 노래를 잠깐 멈추고 playerdeath → 끝나면 전투 노래 다시 (맵으로 가면 인트로)
        SoundManager.playEffectThenBgm(SoundManager.PLAYER_DEATH, SoundManager.BGM_BATTLE);
        monsterView.setNormalState();
        messageArea.setText(text + "\n\n[패배]");
        showCard(centerCards, "msg");
        showCard(bottomCards, "end");
    }

    // 턴 카드는 다른 화면 요소 위에 그림
    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        Point p = SwingUtilities.convertPoint(dodgeGamePanel, 0, 0, this);   // 조여 오는 원: 탄막 필드 밖(보스 쪽)까지
        dodgeGamePanel.paintOverlay(g, p.x, p.y);
        turnCards.paint(g, getWidth(), monsterView.normalImage);
    }

    private void updateStatus() {
        int left = Math.max(0, engine.getBossHp());
        bossHpBar.setValue(left);
        bossHpBar.setString("HP: " + left + " / " + engine.getBossMaxHp());
        turnLabel.setText("남은 공격: " + engine.getAttacksLeft() + "회");
        
        Color heartColor = engine.isInvincible() ? GOLD : Color.RED;   // 무적 중에는 금색 하트
        for (int i = 0; i < MAX_HEARTS; i++) {
            hearts[i].setForeground(i < engine.getPlayerHearts() ? heartColor : Color.GRAY);
        }
        updateItemViews();
        // 별 3개로 시작해서 하트가 줄면 같이 줄어듦 (클리어하면 이 별점이 기록됨)
        int stars = BattleEngine.starsFor(engine.getPlayerHearts());
        stageStarsLabel.setText("★".repeat(stars) + "☆".repeat(3 - stars));
        if (engine.getPlayerHearts() < lastHearts) {
            flashHearts();
            // 내 하트가 깎임 (쓰러지는 마지막 피격은 playerdeath만 나도록 건너뜀)
            if (!engine.isLose()) SoundManager.playEffect(SoundManager.HURT);
        }
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

        private final Timer slashHitSound = new Timer(SLASH_MS, e -> SoundManager.playEffect(SoundManager.SLASH_HIT));

        MonsterView() {
            slashHitSound.setRepeats(false);
            setOpaque(false);
            setPreferredSize(new Dimension(180, 110));
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
            // 보스에게 공격이 들어감. 휘두름 "슉"은 검기 시작(0ms), 맞는 "챙/퍽"은 두 동강 나는 순간(SLASH_MS)에 맞춤
            // 마지막 일격도 평소와 같은 소리 (bossdeath는 showEnd에서 "슉"이 끝난 뒤에 냄)
            SoundManager.playEffect(SoundManager.SLASH_SWING);
            slashHitSound.restart();
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
        if (cards == bottomCards) skipBtn.setEnabled("cmd".equals(name));   // 퀴즈·탄막·연출 중에는 스킵 못 함
    }

    public int getPlayerHearts() {
        return engine.getPlayerHearts();
    }
}
