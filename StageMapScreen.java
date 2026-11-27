import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.function.Consumer;

// 스테이지 맵 화면: 고른 관문(주제)의 STAGE 1~3을 카드로 보여 준다.
// 카드 상태는 4가지 → 클리어 / 다음 도전 대상(NEXT TARGET) / 열림(무적 모드) / 잠김
// 각 스테이지의 수호자(보스) 이름은 백엔드(monsters.txt → DataLoader)에서 받아 온다.
public class StageMapScreen extends DungeonBackground {
    private static final String LOCK_IMAGE = "images/ui/lock.png";
    private static final int LOCK_SIZE = 96;
    private static Icon lockIcon;   // 한 번만 만들어서 재사용

    private static final String[][] STAGE_INFO = {
        {"STAGE 1", "쉬움 난이도\n기본 보스 등장"},
        {"STAGE 2", "중간 난이도\n탄막 속도 증가"},
        {"STAGE 3", "어려움 난이도\n최종 보스전"}
    };

    private enum CardState { CLEARED, NEXT, OPEN, LOCKED }

    public StageMapScreen(String currentTopic, Map<String, Integer> stageBestStars, Consumer<Integer> onStageSelected, Runnable onBackToTopic) {
        super(false);
        setPreferredSize(new Dimension(1050, 800));
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(32, 40, 28, 40));

        // 다음에 도전할 스테이지 (아직 클리어하지 않은 첫 번째 스테이지, 모두 깼으면 4)
        int nextPlayableStage = 1;
        while (nextPlayableStage <= 3 && stageBestStars.containsKey(currentTopic + "_" + nextPlayableStage)) {
            nextPlayableStage++;
        }
        int clearedCount = 0;
        for (int i = 1; i <= 3; i++) {
            if (stageBestStars.containsKey(currentTopic + "_" + i)) clearedCount++;
        }

        // ===== 위쪽 헤더: [← 주제 변경]  GATE 0n · DUNGEON FLOORS / 제목  [관문 돌파 n/3] =====
        JqButton backBtn = new JqButton("← 주제 변경", UiTheme.YELLOW, UiTheme.GRAY_BORDER, 1, 14);
        backBtn.setPreferredSize(new Dimension(124, 36));
        backBtn.addActionListener(e -> onBackToTopic.run());

        JqCard badge = UiTheme.badge();
        badge.setLine(GRAY_70, 1);
        UiTheme.setBadgeValue(badge, "관문 돌파", clearedCount + " / 3", UiTheme.YELLOW);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(UiTheme.side(backBtn, FlowLayout.LEFT), BorderLayout.WEST);
        topPanel.add(UiTheme.headerTitle(
                String.format("GATE %02d · DUNGEON FLOORS", getGateNo(currentTopic)),
                "지식의 관문 — [" + getTopicTitle(currentTopic) + "]"), BorderLayout.CENTER);
        topPanel.add(UiTheme.side(badge, FlowLayout.RIGHT), BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // ===== 가운데: 스테이지 카드 3개 =====
        JPanel stagesPanel = new JPanel(new GridLayout(1, 3, 28, 0));
        stagesPanel.setOpaque(false);
        stagesPanel.setBorder(BorderFactory.createEmptyBorder(34, 20, 34, 20));

        for (int stageNo = 1; stageNo <= 3; stageNo++) {
            String key = currentTopic + "_" + stageNo;
            boolean isCleared = stageBestStars.containsKey(key);
            // 순서대로 열림: STAGE 1은 항상, 2·3은 바로 앞 스테이지를 클리어해야 열림 (무적 모드는 모두 열림)
            boolean isUnlocked = stageNo == 1 || stageBestStars.containsKey(currentTopic + "_" + (stageNo - 1))
                    || BattleEngine.isGodMode();

            CardState state;
            if (isCleared) state = CardState.CLEARED;
            else if (stageNo == nextPlayableStage) state = CardState.NEXT;
            else if (isUnlocked) state = CardState.OPEN;
            else state = CardState.LOCKED;

            Stage stage = DataLoader.loadStage("monsters.txt", currentTopic, stageNo);
            int stars = stageBestStars.getOrDefault(key, 0);
            stagesPanel.add(createStageCard(stageNo, state, stars, stage.getBossName(), onStageSelected));
        }
        add(stagesPanel, BorderLayout.CENTER);

        // ===== 아래쪽 안내 =====
        add(UiTheme.label("모든 스테이지를 클리어하면 다음 지식의 관문으로 나아갈 수 있습니다",
                UiTheme.TEXT_SUB, Font.PLAIN, 13), BorderLayout.SOUTH);
    }

    private static final Color GRAY_70 = new Color(51, 65, 85);   // 잠긴 카드·배지 테두리

    private JqCard createStageCard(int stageNo, CardState state, int stars, String bossName, Consumer<Integer> onStageSelected) {
        // --- 상태별 색과 문구 ---
        Color titleColor, borderColor, subColor;
        int borderWidth;
        String subText, btnText;
        switch (state) {
            case CLEARED:
                titleColor = UiTheme.GREEN;  borderColor = UiTheme.GRAY_BORDER;  borderWidth = 1;
                subText = UiTheme.starHtml(stars);  subColor = UiTheme.GOLD;  btnText = "다시 도전";
                break;
            case NEXT:
                titleColor = UiTheme.YELLOW; borderColor = UiTheme.MINT;  borderWidth = 2;
                subText = "NEXT TARGET";  subColor = UiTheme.MINT;  btnText = "입장하기  ▶";
                break;
            case OPEN:
                titleColor = UiTheme.YELLOW; borderColor = UiTheme.MINT_SOFT;  borderWidth = 1;
                subText = "OPEN";  subColor = UiTheme.MINT;  btnText = "입장하기  ▶";
                break;
            default:   // LOCKED
                titleColor = UiTheme.TEXT_DIM; borderColor = GRAY_70;  borderWidth = 1;
                subText = "LOCKED";  subColor = new Color(71, 85, 105);  btnText = "잠김";
        }

        JqCard card = new JqCard(state == CardState.NEXT ? new Color(24, 31, 55) : UiTheme.CARD, borderColor, borderWidth, 16);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(28, 22, 22, 22));

        // --- 위: 제목 + (별점 / NEXT TARGET / LOCKED) ---
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        String title = STAGE_INFO[stageNo - 1][0] + (state == CardState.CLEARED ? " [CLEAR]" : "");
        head.add(UiTheme.label(title, titleColor, Font.BOLD, state == CardState.CLEARED ? 24 : 28));
        head.add(Box.createVerticalStrut(8));
        boolean isStars = state == CardState.CLEARED;
        head.add(UiTheme.label(subText, subColor, Font.BOLD, isStars ? 24 : 12));

        // --- 가운데: 수호자 이름 + 설명 (잠긴 카드는 설명 위에 자물쇠를 겹침) ---
        JPanel middle = new JPanel();
        middle.setOpaque(false);
        middle.setLayout(new BoxLayout(middle, BoxLayout.Y_AXIS));
        middle.add(Box.createVerticalGlue());

        String desc = "<html><center>" + STAGE_INFO[stageNo - 1][1].replace("\n", "<br>") + "</center></html>";
        if (state == CardState.LOCKED) {
            // 설명글 위에 자물쇠를 겹쳐서 카드 정중앙에 배치 (마우스를 올리면 해금 조건 안내)
            middle.add(createLockedCenter(desc, stageNo));
        } else {
            Color chipColor = state == CardState.CLEARED ? UiTheme.GREEN : UiTheme.MINT;
            middle.add(createChip("수호자: " + bossName, chipColor));
            middle.add(Box.createVerticalStrut(14));
            middle.add(UiTheme.label(desc, state == CardState.CLEARED ? new Color(226, 232, 240) : Color.WHITE,
                    Font.PLAIN, 19));
        }
        middle.add(Box.createVerticalGlue());

        // --- 아래: 입장 버튼 (카드 가로폭을 채우는 긴 바) ---
        JqButton enterBtn;
        if (state == CardState.NEXT) {
            enterBtn = new JqButton(btnText, UiTheme.YELLOW, UiTheme.MINT, 2, 17);
        } else if (state == CardState.LOCKED) {
            enterBtn = new JqButton(btnText, UiTheme.TEXT_DIM, GRAY_70, 1, 17);
            enterBtn.setEnabled(false);   // 잠긴 스테이지는 입장 불가
        } else {
            enterBtn = new JqButton(btnText, Color.WHITE, UiTheme.MINT_SOFT, 2, 17);
        }
        enterBtn.setPreferredSize(new Dimension(0, 50));
        enterBtn.addActionListener(e -> onStageSelected.accept(stageNo));

        card.add(head, BorderLayout.NORTH);
        card.add(middle, BorderLayout.CENTER);
        card.add(enterBtn, BorderLayout.SOUTH);
        return card;
    }

    // "수호자: SyntaxError (Lv.1)" 처럼 둥근 알약 모양 라벨
    private static JComponent createChip(String text, Color color) {
        JqCard chip = new JqCard(new Color(color.getRed(), color.getGreen(), color.getBlue(), 38),
                new Color(color.getRed(), color.getGreen(), color.getBlue(), 100), 1, 24);
        chip.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        chip.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(UiTheme.font(Font.BOLD, 12));
        chip.add(l);

        // BoxLayout 안에서 가로로 늘어나지 않고 글자 크기만큼만 차지하도록 감싸기
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        wrap.setOpaque(false);
        wrap.add(chip);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, chip.getPreferredSize().height));
        wrap.setAlignmentX(Component.CENTER_ALIGNMENT);
        return wrap;
    }

    // 잠긴 카드 가운데: 설명글과 자물쇠를 OverlayLayout으로 같은 자리에 겹침
    // OverlayLayout은 먼저 add한 컴포넌트가 위에 그려지므로 자물쇠를 먼저 넣는다.
    private static JComponent createLockedCenter(String desc, int stageNo) {
        JPanel overlay = new JPanel();
        overlay.setLayout(new OverlayLayout(overlay));
        overlay.setOpaque(false);
        overlay.setToolTipText("STAGE " + (stageNo - 1) + "을(를) 클리어하면 열립니다");

        JLabel lockLabel = new JLabel(getLockIcon());
        lockLabel.setAlignmentX(0.5f);
        lockLabel.setAlignmentY(0.5f);

        JLabel descLabel = UiTheme.label(desc, UiTheme.TEXT_SUB, Font.PLAIN, 19);
        descLabel.setAlignmentX(0.5f);
        descLabel.setAlignmentY(0.5f);

        overlay.add(lockLabel);   // 위
        overlay.add(descLabel);   // 아래
        overlay.setAlignmentX(Component.CENTER_ALIGNMENT);
        overlay.setMaximumSize(new Dimension(Integer.MAX_VALUE, overlay.getPreferredSize().height));
        return overlay;
    }

    // 자물쇠 아이콘: images/ui/lock.png를 줄여서 쓰고, 파일이 없거나 못 읽으면 코드로 그린 자물쇠
    private static Icon getLockIcon() {
        if (lockIcon != null) return lockIcon;
        BufferedImage img = UiTheme.loadImage(LOCK_IMAGE);
        if (img == null) img = drawFallbackLock();
        return lockIcon = UiTheme.scaledIcon(img, LOCK_SIZE, LOCK_SIZE);
    }

    private static BufferedImage drawFallbackLock() {
        BufferedImage img = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(148, 163, 184));
        g.setStroke(new BasicStroke(7));
        g.drawArc(22, 8, 36, 46, 0, 180);   // 고리
        g.drawLine(22, 31, 22, 40);
        g.drawLine(58, 31, 58, 40);
        g.setColor(new Color(100, 116, 139));
        g.fillRoundRect(12, 36, 56, 40, 10, 10);   // 몸통
        g.setColor(UiTheme.BG);
        g.fillOval(34, 46, 12, 12);   // 열쇠 구멍
        g.fillRect(38, 54, 4, 12);
        g.dispose();
        return img;
    }

    private static int getGateNo(String id) {
        if ("common".equals(id)) return 2;
        if ("name".equals(id)) return 3;
        return 1;
    }

    private String getTopicTitle(String id) {
        if ("coding".equals(id)) return "코딩";
        if ("common".equals(id)) return "상식";
        if ("name".equals(id)) return "이름 맞추기";
        return "모험";
    }
}
