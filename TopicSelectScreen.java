import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.function.Consumer;

// 주제 선택 화면: 3개의 주제를 "지식의 관문(GATE)"으로 보여 준다.
// 각 카드에는 그 관문의 최종 보스(STAGE 3) 이미지, 진행 상태, 모은 별 개수가 표시되고
// 관문을 모두 깨면 보스 위에 빨간 CLEAR 도장이 찍힌다.
public class TopicSelectScreen extends DungeonBackground {
    private static final String[][] TOPICS = {
        {"코딩", "자바, 파이썬 등\n프로그래밍 문제", "coding"},
        {"상식", "일상 및 일반\n상식 문제", "common"},
        {"이름 맞추기", "사진을 보고\n인물·음식 이름 맞추기", "name"}
    };

    private final Map<String, Integer> stageBestStars;   // GameFrame의 클리어 기록 (Key: "topicId_stageNo")
    private final JqButton[] topicCards = new JqButton[TOPICS.length];
    private final JLabel[] gateLabels = new JLabel[TOPICS.length];
    private final BossMedallion[] medallions = new BossMedallion[TOPICS.length];
    private final JLabel[] statusLabels = new JLabel[TOPICS.length];
    private final JLabel[] starLabels = new JLabel[TOPICS.length];
    private final JqCard progressBadge = UiTheme.badge();

    // 주제 선택 시 어떤 주제가 선택되었는지 콜백으로 전달 (예: "coding", "common", "name"), 왼쪽 위 `← Title` 버튼은 onBackToTitle
    public TopicSelectScreen(Map<String, Integer> stageBestStars, Consumer<String> onTopicSelected, Runnable onBackToTitle) {
        super(false);
        this.stageBestStars = stageBestStars;
        setPreferredSize(new Dimension(1050, 800));
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(32, 40, 28, 40));

        // ===== 위쪽 헤더: [← Title]  SELECT GATEWAY / 제목  [탈출 진행도] =====
        JqButton backBtn = new JqButton("← Title", UiTheme.YELLOW, UiTheme.GRAY_BORDER, 1, 14);
        backBtn.setPreferredSize(new Dimension(100, 36));
        backBtn.addActionListener(e -> onBackToTitle.run());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(UiTheme.side(backBtn, FlowLayout.LEFT), BorderLayout.WEST);
        topPanel.add(UiTheme.headerTitle("SELECT GATEWAY", "돌파할 지식의 관문을 선택하라"), BorderLayout.CENTER);
        topPanel.add(UiTheme.side(progressBadge, FlowLayout.RIGHT), BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // ===== 가운데: 관문 카드 3개 =====
        JPanel cardPanel = new JPanel(new GridLayout(1, 3, 28, 0));
        cardPanel.setOpaque(false);
        cardPanel.setBorder(BorderFactory.createEmptyBorder(44, 20, 44, 20));

        for (int i = 0; i < TOPICS.length; i++) {
            String topicId = TOPICS[i][2];
            JqButton card = createTopicCard(i);
            card.addActionListener(e -> onTopicSelected.accept(topicId));
            topicCards[i] = card;
            cardPanel.add(card);
        }
        add(cardPanel, BorderLayout.CENTER);

        // ===== 아래쪽 안내 =====
        add(UiTheme.label("카드를 클릭하면 해당 관문의 스테이지 맵으로 이동합니다", UiTheme.TEXT_SUB, Font.PLAIN, 13),
                BorderLayout.SOUTH);

        refresh();
    }

    // 카드 하나 = 버튼 하나 (카드 어디를 눌러도 선택되고, 마우스를 올리면 밝아짐)
    // 안쪽 라벨들은 마우스 리스너가 없어서 클릭이 그대로 버튼에 전달된다.
    private JqButton createTopicCard(int i) {
        String[] t = TOPICS[i];
        JqButton card = new JqButton("", Color.WHITE, UiTheme.MINT_SOFT, 2, 14);
        card.setFills(UiTheme.CARD, UiTheme.CARD_HOVER);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(26, 22, 18, 22));

        // 위: GATE 번호 + 주제 이름
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        gateLabels[i] = UiTheme.label(String.format("GATE %02d", i + 1), UiTheme.MINT, Font.BOLD, 12);
        head.add(gateLabels[i]);
        head.add(Box.createVerticalStrut(4));
        head.add(UiTheme.label(t[0], Color.WHITE, Font.BOLD, 30));

        // 가운데: 보스 메달 + 설명
        // 보스 이미지는 백엔드(monsters.txt → DataLoader)에서 STAGE 3 보스의 이미지 경로를 받아 온다.
        Stage finalBoss = DataLoader.loadStage("monsters.txt", t[2], 3);
        medallions[i] = new BossMedallion(UiTheme.loadImage(finalBoss.getImagePath()));

        JLabel desc = UiTheme.label("<html><center>" + t[1].replace("\n", "<br>") + "</center></html>",
                new Color(226, 232, 240), Font.PLAIN, 17);

        JPanel middle = new JPanel(new GridBagLayout());
        middle.setOpaque(false);
        GridBagConstraints mc = new GridBagConstraints();
        mc.gridx = 0;
        mc.weightx = 1;
        mc.fill = GridBagConstraints.VERTICAL;
        mc.weighty = 1;
        mc.gridy = 0;
        middle.add(Box.createVerticalGlue(), mc);
        mc.fill = GridBagConstraints.NONE;
        mc.weighty = 0;
        mc.gridy = 1;
        mc.anchor = GridBagConstraints.CENTER;
        middle.add(medallions[i], mc);
        mc.gridy = 2;
        mc.insets = new Insets(24, 0, 0, 0);
        middle.add(desc, mc);
        mc.gridy = 3;
        mc.insets = new Insets(0, 0, 0, 0);
        mc.fill = GridBagConstraints.VERTICAL;
        mc.weighty = 1;
        middle.add(Box.createVerticalGlue(), mc);

        // 아래: 구분선 + 상태(왼쪽) + 별 개수(오른쪽)
        statusLabels[i] = new JLabel();
        statusLabels[i].setFont(UiTheme.font(Font.BOLD, 13));
        statusLabels[i].setHorizontalAlignment(SwingConstants.CENTER);
        starLabels[i] = new JLabel();
        starLabels[i].setFont(UiTheme.font(Font.BOLD, 13));
        starLabels[i].setHorizontalAlignment(SwingConstants.CENTER);

        JPanel footer = new JPanel(new GridLayout(2, 1, 0, 2));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.LINE),
                BorderFactory.createEmptyBorder(12, 2, 0, 2)));
        footer.add(statusLabels[i]);
        footer.add(starLabels[i]);

        card.add(head, BorderLayout.NORTH);
        card.add(middle, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    // 클리어 기록에 맞춰 카드 다시 그리기 (주제 선택 화면을 보여 줄 때마다 GameFrame이 호출)
    public void refresh() {
        int clearedTopics = 0;
        for (int i = 0; i < TOPICS.length; i++) {
            String topicId = TOPICS[i][2];
            int clearedStages = 0, starSum = 0;
            for (int s = 1; s <= 3; s++) {
                Integer v = stageBestStars.get(topicId + "_" + s);
                if (v != null) {
                    clearedStages++;
                    starSum += v;
                }
            }
            boolean allCleared = clearedStages == 3;
            if (allCleared) clearedTopics++;

            medallions[i].setCleared(allCleared);
            gateLabels[i].setForeground(allCleared ? UiTheme.GREEN : UiTheme.MINT);
            topicCards[i].setLine(allCleared ? UiTheme.MINT : UiTheme.MINT_SOFT, 2);

            // 상태 문구: 아직 시작 전 / 진행 중 / 관문 돌파
            if (allCleared) {
                setColored(statusLabels[i], "수호자 격파 완료", UiTheme.GREEN);
            } else if (clearedStages > 0) {
                setColored(statusLabels[i], "진행 중 · STAGE " + clearedStages + " / 3", UiTheme.MINT);
            } else {
                setColored(statusLabels[i], "도전 가능", UiTheme.MINT);
            }
            setColored(starLabels[i], "★ " + starSum + " / 9", starSum > 0 ? UiTheme.GOLD : UiTheme.TEXT_SUB);
        }
        UiTheme.setBadgeValue(progressBadge, "탈출 진행도", clearedTopics + " / " + TOPICS.length, UiTheme.MINT);
    }

    private static void setColored(JLabel l, String text, Color color) {
        l.setText(text);
        l.setForeground(color);
    }

    // 원 안에 보스 이미지를 넣은 메달. 관문을 모두 깨면 비스듬한 빨간 CLEAR 도장을 찍는다.
    private static class BossMedallion extends JComponent {
        private static final int SIZE = 150;
        private final BufferedImage bossImage;
        private boolean cleared;

        BossMedallion(BufferedImage bossImage) {
            this.bossImage = bossImage;
            Dimension d = new Dimension(SIZE + 40, SIZE);   // 도장이 원 밖으로 조금 나와도 잘리지 않게 가로 여유
            setPreferredSize(d);
            setMaximumSize(d);
        }

        void setCleared(boolean cleared) {
            this.cleared = cleared;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int x = (getWidth() - SIZE) / 2, y = (getHeight() - SIZE) / 2;
            Ellipse2D circle = new Ellipse2D.Double(x, y, SIZE, SIZE);

            // 어두운 원 배경
            g2.setColor(new Color(15, 20, 35, 235));
            g2.fill(circle);

            // 보스 이미지: 원 안쪽에 비율 유지하며 맞춤 (원 밖은 잘라냄)
            if (bossImage != null) {
                Shape oldClip = g2.getClip();
                g2.clip(circle);
                int box = SIZE - 22;
                double scale = Math.min((double) box / bossImage.getWidth(), (double) box / bossImage.getHeight());
                int iw = (int) (bossImage.getWidth() * scale), ih = (int) (bossImage.getHeight() * scale);
                g2.drawImage(bossImage, x + (SIZE - iw) / 2, y + (SIZE - ih) / 2, iw, ih, null);
                g2.setClip(oldClip);
            }

            // 원 테두리
            g2.setColor(cleared ? new Color(71, 85, 105, 200) : new Color(0, 200, 190, 90));
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(circle);

            if (cleared) paintClearStamp(g2, getWidth() / 2, getHeight() / 2);
            g2.dispose();
        }

        // -11도 기울어진 빨간 테두리 도장
        private void paintClearStamp(Graphics2D g2, int cx, int cy) {
            Graphics2D s = (Graphics2D) g2.create();
            s.rotate(Math.toRadians(-11), cx, cy);
            s.setFont(UiTheme.font(Font.BOLD, 18));
            FontMetrics fm = s.getFontMetrics();
            String text = "C L E A R";
            int w = fm.stringWidth(text) + 24, h = fm.getHeight() + 6;
            int rx = cx - w / 2, ry = cy - h / 2;

            s.setColor(new Color(15, 20, 35, 220));
            s.fillRoundRect(rx, ry, w, h, 6, 6);
            s.setColor(new Color(239, 68, 68, 60));        // 바깥쪽 번짐
            s.setStroke(new BasicStroke(7));
            s.drawRoundRect(rx, ry, w, h, 6, 6);
            s.setColor(UiTheme.RED);
            s.setStroke(new BasicStroke(3));
            s.drawRoundRect(rx, ry, w, h, 6, 6);
            s.drawString(text, cx - fm.stringWidth(text) / 2, ry + (h - fm.getHeight()) / 2 + fm.getAscent());
            s.dispose();
        }
    }
}
