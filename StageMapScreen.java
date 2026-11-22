import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

public class StageMapScreen extends JPanel {
    private static final Color BG_COLOR = new Color(15, 20, 35);
    private static final Color BORDER_COLOR = new Color(0, 200, 190);
    private static final Color LOCKED_TEXT = new Color(110, 115, 130);   // 잠긴 스테이지 글자색
    private static final String LOCK_IMAGE = "images/ui/lock.png";
    private static final int LOCK_SIZE = 80;
    private static Icon lockIcon;   // 한 번만 불러와서 재사용

    public StageMapScreen(String currentTopic, Map<String, Integer> stageBestStars, Consumer<Integer> onStageSelected, Runnable onBackToTopic) {
        setPreferredSize(new Dimension(1050, 800));
        setBackground(BG_COLOR);
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        // 상단 타이틀 및 뒤로가기 버튼
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("스테이지 맵 — [" + getTopicTitle(currentTopic) + "]", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 26));
        topPanel.add(titleLabel, BorderLayout.CENTER);

        JButton backBtn = new JButton("주제 변경");
        backBtn.setBackground(new Color(25, 30, 50));
        backBtn.setForeground(Color.YELLOW);
        backBtn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        backBtn.setBorder(new LineBorder(Color.GRAY, 1));
        backBtn.setFocusPainted(false);
        backBtn.addActionListener(e -> onBackToTopic.run());
        topPanel.add(backBtn, BorderLayout.WEST);

        add(topPanel, BorderLayout.NORTH);

        // 스테이지 3개 목록 패널
        JPanel stagesPanel = new JPanel(new GridLayout(1, 3, 30, 0));
        stagesPanel.setOpaque(false);
        stagesPanel.setBorder(BorderFactory.createEmptyBorder(60, 40, 60, 40));

        String[][] stageInfo = {
            {"STAGE 1", "쉬움 난이도\n기본 보스 등장", "1"},
            {"STAGE 2", "중간 난이도\n탄막 속도 증가", "2"},
            {"STAGE 3", "어려움 난이도\n최종 보스전", "3"}
        };

        // 다음에 도전할 스테이지 찾기 (아직 클리어하지 않은 첫 번째 스테이지)
        int nextPlayableStage = 1;
        for (int i = 1; i <= 3; i++) {
            String key = currentTopic + "_" + i;
            if (stageBestStars.containsKey(key)) {
                nextPlayableStage = i + 1; // 클리어했으면 다음 스테이지가 도전 대상
            } else {
                break;
            }
        }

        for (int i = 0; i < 3; i++) {
            int stageNum = Integer.parseInt(stageInfo[i][2]);
            String key = currentTopic + "_" + stageNum;
            boolean isCleared = stageBestStars.containsKey(key);
            int stars = stageBestStars.getOrDefault(key, 0);
            // 순서대로 열림: STAGE 1은 항상, 2·3은 같은 주제의 바로 앞 스테이지를 클리어해야 열림
            // 무적 모드(타이틀 커맨드)에서는 모든 스테이지 열림
            boolean isUnlocked = stageNum == 1 || stageBestStars.containsKey(currentTopic + "_" + (stageNum - 1))
                    || BattleEngine.isGodMode();

            // 다음에 도전할 스테이지인지 여부 (3스테이지까지 모두 깼다면 민트색 테두리 없음)
            boolean isNextTarget = (stageNum == nextPlayableStage && nextPlayableStage <= 3);

            JPanel stageCard = new JPanel(new BorderLayout());
            stageCard.setBackground(new Color(20, 25, 45));
            // 조건에 따라 테두리 색상 및 두께 다르게 설정
            stageCard.setBorder(new LineBorder(isNextTarget ? BORDER_COLOR : Color.GRAY, isNextTarget ? 2 : 1));
            
            // 상단 타이틀 영역 (클리어 시 [CLEAR] 표시 병행)
            String titleText = stageInfo[i][0] + (isCleared ? " [CLEAR]" : "");
            JLabel sTitle = new JLabel(titleText, SwingConstants.CENTER);
            sTitle.setForeground(!isUnlocked ? LOCKED_TEXT : isCleared ? new Color(100, 255, 150) : Color.YELLOW);
            sTitle.setFont(new Font("맑은 고딕", Font.BOLD, 18));
            sTitle.setBorder(BorderFactory.createEmptyBorder(20, 10, 5, 10));

            // 별점 표시 라벨 (클리어된 경우에만 별 표시)
            JLabel starLabel = new JLabel(getStarString(stars, isCleared), SwingConstants.CENTER);
            starLabel.setFont(new Font("Dialog", Font.BOLD, 20));
            starLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 15, 10));

            // 설명 텍스트
            JTextArea sDesc = new JTextArea(stageInfo[i][1]);
            sDesc.setEditable(false);
            sDesc.setOpaque(false);
            sDesc.setForeground(isUnlocked ? Color.WHITE : LOCKED_TEXT);
            sDesc.setFont(new Font("맑은 고딕", Font.PLAIN, 15));
            sDesc.setMargin(new Insets(10, 20, 10, 20));

            // 카드 내부 상단 패널 (제목 + 별점 묶음)
            JPanel cardHeader = new JPanel();
            cardHeader.setOpaque(false);
            cardHeader.setLayout(new BoxLayout(cardHeader, BoxLayout.Y_AXIS));
            cardHeader.add(sTitle);
            cardHeader.add(starLabel);

            JButton enterBtn = new JButton("입장하기");
            enterBtn.setBackground(new Color(25, 30, 50));
            enterBtn.setForeground(Color.WHITE);
            enterBtn.setFont(new Font("맑은 고딕", Font.BOLD, 16));
            enterBtn.setBorder(new LineBorder(isUnlocked ? BORDER_COLOR : LOCKED_TEXT, 1));
            enterBtn.setFocusPainted(false);
            enterBtn.setEnabled(isUnlocked); // 잠긴 스테이지는 입장 불가
            enterBtn.addActionListener(e -> onStageSelected.accept(stageNum));

            JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
            btnWrapper.setOpaque(false);
            btnWrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
            btnWrapper.add(enterBtn);

            stageCard.add(cardHeader, BorderLayout.NORTH);
            if (isUnlocked) {
                stageCard.add(sDesc, BorderLayout.CENTER);
            } else {
                // 잠긴 카드: 흐린 설명 아래 가운데에 자물쇠
                JPanel lockedCenter = new JPanel(new BorderLayout());
                lockedCenter.setOpaque(false);
                lockedCenter.add(sDesc, BorderLayout.NORTH);
                JLabel lockLabel = new JLabel(getLockIcon(), SwingConstants.CENTER);
                lockLabel.setToolTipText("STAGE " + (stageNum - 1) + "을(를) 클리어하면 열립니다");
                lockedCenter.add(lockLabel, BorderLayout.CENTER);
                stageCard.add(lockedCenter, BorderLayout.CENTER);
            }
            stageCard.add(btnWrapper, BorderLayout.SOUTH);

            stagesPanel.add(stageCard);
        }

        add(stagesPanel, BorderLayout.CENTER);
    }

    // 자물쇠 아이콘: images/ui/lock.png를 줄여서 쓰고, 파일이 없거나 못 읽으면 코드로 그린 자물쇠
    private static Icon getLockIcon() {
        if (lockIcon != null) return lockIcon;
        try {
            BufferedImage img = ImageIO.read(new File(LOCK_IMAGE));
            if (img != null) {
                return lockIcon = new ImageIcon(img.getScaledInstance(LOCK_SIZE, LOCK_SIZE, Image.SCALE_SMOOTH));
            }
        } catch (IOException ignored) {
        }
        BufferedImage img = new BufferedImage(LOCK_SIZE, LOCK_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(170, 175, 190));
        g.setStroke(new BasicStroke(7));
        g.drawArc(22, 8, 36, 46, 0, 180);   // 고리
        g.drawLine(22, 31, 22, 40);
        g.drawLine(58, 31, 58, 40);
        g.setColor(new Color(230, 170, 50));
        g.fillRoundRect(12, 36, 56, 40, 10, 10);   // 몸통
        g.setColor(new Color(60, 35, 5));
        g.fillOval(34, 46, 12, 12);   // 열쇠 구멍
        g.fillRect(38, 54, 4, 12);
        g.dispose();
        return lockIcon = new ImageIcon(img);
    }

    // 별점 개수에 맞춰 노란색/회색 별 문자열 반환
    private String getStarString(int stars, boolean isCleared) {
        if (!isCleared) return " ";
        StringBuilder sb = new StringBuilder("<html><span style= 'font-size: 22pt; color: #FFD700;'>");
        for (int i = 1; i <= 3; i++) {
            if (i <= stars) {
                sb.append("★ ");
            } else {
                sb.append("</span><span style= 'font-size: 22pt; color: #555555;'>☆ </span><span style= 'font-size: 22pt; color: #FFD700;'>");
            }
        }
        sb.append("</span></html>");
        return sb.toString().trim();
    }

    private String getTopicTitle(String id) {
        if ("coding".equals(id)) return "코딩";
        if ("common".equals(id)) return "상식";
        if ("name".equals(id)) return "이름 맞추기";
        return "모험";
    }
}