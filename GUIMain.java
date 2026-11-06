import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class GUIMain extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    static GameManager engine;

    // 화면(카드) 이름 상수 정의 (요청하신 이름으로 변경)
    public static final String HOME = "HOME";
    public static final String ATTACK = "ATTACK";
    public static final String ANSWER = "ANSWER";
    public static final String DEFEND = "DEFEND";

    public GUIMain(GameManager manager) {
        engine = manager;
    }

    // GUI 초기화 및 화면 표시를 담당하는 메서드
    public void createAndShowGUI() {
        setTitle("JavaQuest - 전투");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(15, 20, 35));

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // 4개의 화면을 CardLayout에 등록
        mainPanel.add(new HomePanel(this), HOME);
        mainPanel.add(new AttackPanel(this), ATTACK);
        mainPanel.add(new AnswerPanel(this), ANSWER);
        mainPanel.add(new DefendPanel(this), DEFEND);

        add(mainPanel);
        setVisible(true); // 프레임이 보이게 설정
    }

    public void showCard(String cardName) {
        cardLayout.show(mainPanel, cardName);
        // 패널이 전환될 때마다 키보드 포커스를 강제로 가져와야 탄막 피하기 조작이 가능합니다.
        mainPanel.getComponents();
        for (Component c : mainPanel.getComponents()) {
            if (c.isVisible()) c.requestFocusInWindow();
        }
    }

    public static void main(String[] args) {
        engine = new GameManager();
        GUIMain main = new GUIMain(engine);
        // 스레드를 안전하게 만들어 GUI 담당 메서드 호출
        SwingUtilities.invokeLater(() -> main.createAndShowGUI());
    }
}

// ====================================================================
// 1. 시작 화면
// ====================================================================
class HomePanel extends JPanel {
    public HomePanel(GUIMain main) {
        setLayout(new GridBagLayout());
        setBackground(new Color(15, 20, 35));
        
        JLabel titleLabel = new JLabel("JAVAQUEST", SwingConstants.CENTER);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 70));
        titleLabel.setForeground(Color.WHITE);
        
        JButton startBtn = new JButton("전투 시작");
        startBtn.setFont(new Font("맑은 고딕", Font.BOLD, 30));
        startBtn.setBackground(Color.BLACK);
        startBtn.setForeground(Color.YELLOW);
        startBtn.setBorder(new LineBorder(Color.WHITE, 3));
        startBtn.setFocusPainted(false);
        startBtn.addActionListener(e -> main.showCard(GUIMain.ATTACK));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(0, 0, 50, 0);
        add(titleLabel, gbc);
        gbc.gridy = 1; add(startBtn, gbc);
    }
}

// ====================================================================
// 2. 공격(Attack) 화면 - 난이도 선택 및 문제 풀이
// ====================================================================
class AttackPanel extends JPanel {
    JTextArea questionArea;
    JPanel choicesPanel;

    public AttackPanel(GUIMain main) {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(15, 20, 35));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 상단: 몬스터 정보 및 난이도 선택 버튼
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        
        JLabel bossHpLabel = new JLabel("NULLPOINTEREXCEPTION HP: " + GUIMain.engine.bossHp);
        bossHpLabel.setForeground(Color.WHITE);
        bossHpLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        topPanel.add(bossHpLabel, BorderLayout.NORTH);

        JPanel diffPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        diffPanel.setOpaque(false);
        String[] diffs = {"하", "중", "상"};
        for (String d : diffs) {
            JButton btn = new JButton(d + " 난이도 선택");
            btn.setBackground(Color.BLACK); btn.setForeground(Color.WHITE);
            btn.setBorder(new LineBorder(Color.GRAY, 2));
            btn.addActionListener(e -> loadQuiz(d));
            diffPanel.add(btn);
        }
        topPanel.add(diffPanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // 중앙: 퀴즈 내용
        questionArea = new JTextArea("난이도를 고르세요. 어려울수록 강하게 공격합니다.");
        questionArea.setFont(new Font("맑은 고딕", Font.PLAIN, 20));
        questionArea.setForeground(Color.WHITE);
        questionArea.setBackground(new Color(25, 30, 50));
        questionArea.setEditable(false);
        questionArea.setBorder(new LineBorder(Color.WHITE, 2));
        add(questionArea, BorderLayout.CENTER);

        // 하단: 4지선다 버튼 및 플레이어 상태
        JPanel bottomWrap = new JPanel(new BorderLayout());
        bottomWrap.setOpaque(false);

        choicesPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        choicesPanel.setOpaque(false);
        choicesPanel.setVisible(false);
        
        for(int i = 0; i < 4; i++) {
            final int choiceIdx = i;
            JButton choiceBtn = new JButton("선택지 " + (i+1));
            choiceBtn.setBackground(Color.BLACK); choiceBtn.setForeground(Color.WHITE);
            choiceBtn.addActionListener(e -> {
                GUIMain.engine.checkAnswer(choiceIdx); 
                main.showCard(GUIMain.ANSWER); // 답변 결과 화면으로 이동
            });
            choicesPanel.add(choiceBtn);
        }
        bottomWrap.add(choicesPanel, BorderLayout.CENTER);

        JLabel playerStatus = new JLabel(String.format("LV 3   HP: %d / %d   남은 턴: %d / 10", 
            GUIMain.engine.playerHp, GUIMain.engine.playerMaxHp, GUIMain.engine.attackTurns));
        playerStatus.setForeground(Color.CYAN);
        playerStatus.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        bottomWrap.add(playerStatus, BorderLayout.SOUTH);

        add(bottomWrap, BorderLayout.SOUTH);
    }

    private void loadQuiz(String diff) {
        GUIMain.engine.setQuizByDifficulty(diff);
        questionArea.setText("\n" + GUIMain.engine.currentQuestion);
        
        for(int i = 0; i < 4; i++) {
            JButton btn = (JButton) choicesPanel.getComponent(i);
            btn.setText(GUIMain.engine.currentChoices[i]);
        }
        choicesPanel.setVisible(true);
    }
}

// ====================================================================
// 3. 답변 결과(Answer) 화면
// ====================================================================
class AnswerPanel extends JPanel {
    public AnswerPanel(GUIMain main) {
        setLayout(new BorderLayout(20, 20));
        setBackground(new Color(15, 20, 35));
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel resultLabel = new JLabel("CORRECT! (또는 WRONG!)", SwingConstants.CENTER);
        resultLabel.setFont(new Font("맑은 고딕", Font.BOLD, 40));
        resultLabel.setForeground(Color.GREEN);
        add(resultLabel, BorderLayout.NORTH);

        JTextArea expArea = new JTextArea("해설이 출력되는 곳입니다.\n버튼을 누르면 적의 턴(탄막 방어)으로 넘어갑니다.");
        expArea.setFont(new Font("맑은 고딕", Font.PLAIN, 20));
        expArea.setForeground(Color.WHITE);
        expArea.setBackground(new Color(25, 30, 50));
        expArea.setEditable(false);
        expArea.setBorder(new LineBorder(Color.WHITE, 2));
        add(expArea, BorderLayout.CENTER);

        JButton nextBtn = new JButton("◆ Z 계속 (적의 턴으로)");
        nextBtn.setFont(new Font("맑은 고딕", Font.BOLD, 24));
        nextBtn.setBackground(Color.BLACK); nextBtn.setForeground(Color.YELLOW);
        nextBtn.setBorder(new LineBorder(Color.YELLOW, 3));
        nextBtn.addActionListener(e -> main.showCard(GUIMain.DEFEND));
        add(nextBtn, BorderLayout.SOUTH);
    }
}

// ====================================================================
// 4. 방어(Defend) 화면 - 탄막 피하기
// ====================================================================
class DefendPanel extends JPanel {
    private int playerX = 200, playerY = 150; 
    private int bulletY = 0; 
    private Timer gameLoop;

    public DefendPanel(GUIMain main) {
        setLayout(new BorderLayout());
        setBackground(new Color(15, 20, 35));

        JLabel titleLabel = new JLabel("ENEMY TURN - 방향키로 피하세요! (피격: " + GUIMain.engine.playerHitCount + "/3)", SwingConstants.CENTER);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        titleLabel.setForeground(Color.RED);
        add(titleLabel, BorderLayout.NORTH);

        JButton backBtn = new JButton("내 턴(공격)으로 돌아가기 임시버튼");
        backBtn.addActionListener(e -> main.showCard(GUIMain.ATTACK));
        add(backBtn, BorderLayout.SOUTH);

        setupKeyBindings();

        gameLoop = new Timer(16, e -> {
            bulletY += 3;
            if(bulletY > getHeight()) bulletY = 0; 
            repaint();
        });
        gameLoop.start();
    }

    private void setupKeyBindings() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "moveUp");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "moveDown");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "moveLeft");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "moveRight");

        am.put("moveUp", new AbstractAction() { public void actionPerformed(ActionEvent e) { playerY -= 15; repaint(); }});
        am.put("moveDown", new AbstractAction() { public void actionPerformed(ActionEvent e) { playerY += 15; repaint(); }});
        am.put("moveLeft", new AbstractAction() { public void actionPerformed(ActionEvent e) { playerX -= 15; repaint(); }});
        am.put("moveRight", new AbstractAction() { public void actionPerformed(ActionEvent e) { playerX += 15; repaint(); }});
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(Color.WHITE);
        g2d.drawRect(50, 50, getWidth() - 100, getHeight() - 150);

        g2d.setColor(Color.CYAN);
        g2d.fillRect(playerX, playerY, 15, 15);

        g2d.setColor(Color.RED);
        g2d.fillOval(getWidth()/2, bulletY, 20, 20);
    }
}