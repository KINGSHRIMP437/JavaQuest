import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.geom.Path2D;

public class HowToPlayScreen extends JPanel {
    private static final Color BG_COLOR = new Color(15, 20, 35);
    private static final Color CARD_BG = new Color(25, 30, 50);
    private static final Color BORDER_COLOR = new Color(0, 200, 190);

    public HowToPlayScreen(Runnable onBackToTitle) {
        setPreferredSize(new Dimension(1050, 800));
        setBackground(BG_COLOR);
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        // 상단 헤더 (좌측 Title 버튼 + 정중앙 제목 + 우측 균형용 투명 여백)
        JLabel titleLabel = new JLabel("게임 설명", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 30));

        JButton backBtn = new JButton("Title");
        backBtn.setBackground(CARD_BG);
        backBtn.setForeground(Color.YELLOW);
        backBtn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        backBtn.setBorder(new LineBorder(Color.GRAY, 1));
        backBtn.setFocusPainted(false);
        backBtn.setPreferredSize(new Dimension(63, 36));
        backBtn.addActionListener(e -> onBackToTitle.run());

        JPanel backWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        backWrap.setOpaque(false);
        backWrap.add(backBtn);

        JPanel rightSpacer = new JPanel();
        rightSpacer.setOpaque(false);
        rightSpacer.setPreferredSize(new Dimension(63, 36));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(backWrap, BorderLayout.WEST);
        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.add(rightSpacer, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 중앙: 3개 카드를 가로로 배치 (주제·스테이지 선택 화면과 통일)
        JPanel cardPanel = new JPanel(new GridLayout(1, 3, 28, 0));
        cardPanel.setOpaque(false);
        cardPanel.setBorder(BorderFactory.createEmptyBorder(40, 20, 40, 20));

        // 1. 게임 목표
        cardPanel.add(createInfoCard(
            "1. 게임 목표",
            createStageBadgeIcon(),
            "<html><center>"
                + "코딩·상식·이름 맞추기 중<br>"
                + "원하는 주제를 선택하고<br><br>"
                + "<font color='#64FF96'><b>STAGE 1 ~ 3 보스</b></font>를<br>"
                + "모두 격파하세요!"
                + "</center></html>"
        ));

        // 2. 전투 진행 (방향키 그래픽 포함)
        cardPanel.add(createInfoCard(
            "2. 전투 진행",
            createArrowKeysIcon(),
            "<html><center>"
                + "<font color='#FFD700'><b>[내 턴]</b></font><br>"
                + "퀴즈를 맞혀 보스에게<br>데미지를 입히세요.<br><br>"
                + "<font color='#00C8BE'><b>[적의 턴]</b></font><br>"
                + "방향키로 이동해<br>보스의 탄막을 피하세요!"
                + "</center></html>"
        ));

        // 3. 목숨 & 별점 (하트 그래픽 포함)
        cardPanel.add(createInfoCard(
            "3. 목숨 & 별점",
            createHeartStarIcon(),
            "<html><center>"
                + "앞 스테이지를 클리어하면<br>"
                + "다음 단계가 열립니다.<br><br>"
                + "남은 <font color='#FF5A5A'><b>목숨(하트)</b></font>에 따라<br>"
                + "최대 <font color='#FFD700'><b>★★★</b></font>를 획득합니다!"
                + "</center></html>"
        ));

        add(cardPanel, BorderLayout.CENTER);
    }

    private JPanel createInfoCard(String heading, JComponent iconPanel, String bodyHtml) {
        JPanel card = new JPanel(new BorderLayout(0, 16));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 2),
            BorderFactory.createEmptyBorder(32, 20, 32, 20)
        ));

        JLabel headLabel = new JLabel(heading, SwingConstants.CENTER);
        headLabel.setForeground(Color.YELLOW);
        headLabel.setFont(new Font("맑은 고딕", Font.BOLD, 24));

        JPanel centerWrap = new JPanel(new BorderLayout(0, 16));
        centerWrap.setOpaque(false);
        centerWrap.add(iconPanel, BorderLayout.NORTH);

        JLabel bodyLabel = new JLabel(bodyHtml, SwingConstants.CENTER);
        bodyLabel.setForeground(Color.WHITE);
        bodyLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 18));
        centerWrap.add(bodyLabel, BorderLayout.CENTER);

        card.add(headLabel, BorderLayout.NORTH);
        card.add(centerWrap, BorderLayout.CENTER);
        return card;
    }

    // 1번 카드용: STAGE 1 -> 2 -> 3 아이콘
    private JComponent createStageBadgeIcon() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int cy = getHeight() / 2;
                int r = 24;
                int gap = 74;
                int startX = w / 2 - gap;

                // 연결선
                g2.setColor(new Color(0, 200, 190, 140));
                g2.setStroke(new BasicStroke(3f));
                g2.drawLine(startX, cy, startX + gap * 2, cy);

                // 스테이지 원 3개
                for (int i = 0; i < 3; i++) {
                    int cx = startX + i * gap;
                    g2.setColor(BG_COLOR);
                    g2.fillOval(cx - r, cy - r, r * 2, r * 2);
                    g2.setColor(i == 2 ? Color.YELLOW : BORDER_COLOR);
                    g2.setStroke(new BasicStroke(2.5f));
                    g2.drawOval(cx - r, cy - r, r * 2, r * 2);

                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("맑은 고딕", Font.BOLD, 16));
                    String text = "S" + (i + 1);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(text, cx - fm.stringWidth(text) / 2, cy + fm.getAscent() / 2 - 2);
                }
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(220, 110));
        return p;
    }

    // 2번 카드용: 방향키 (↑ ↓ ← →) 그래픽
    private JComponent createArrowKeysIcon() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = 40;
                int gap = 8;
                int cx = getWidth() / 2;
                int topY = 12;
                int botY = topY + size + gap;

                drawKeyCap(g2, cx - size / 2, topY, size, "↑");
                drawKeyCap(g2, cx - size / 2 - size - gap, botY, size, "←");
                drawKeyCap(g2, cx - size / 2, botY, size, "↓");
                drawKeyCap(g2, cx + size / 2 + gap, botY, size, "→");

                g2.dispose();
            }

            private void drawKeyCap(Graphics2D g2, int x, int y, int size, String arrow) {
                g2.setColor(BG_COLOR);
                g2.fillRoundRect(x, y, size, size, 10, 10);
                g2.setColor(BORDER_COLOR);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(x, y, size, size, 10, 10);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("맑은 고딕", Font.BOLD, 20));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(arrow, x + (size - fm.stringWidth(arrow)) / 2, y + (size + fm.getAscent()) / 2 - 3);
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(220, 110));
        return p;
    }

    // 3번 카드용: 하트 3개(목숨) + 별 3개 그래픽
    private JComponent createHeartStarIcon() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int cx = getWidth() / 2;
                int heartY = 22;
                int spacing = 46;

                // 빨간 하트 3개
                for (int i = -1; i <= 1; i++) {
                    drawHeart(g2, cx + i * spacing, heartY, 16);
                }

                // 아래 금색 별 3개
                g2.setColor(new Color(255, 215, 0));
                g2.setFont(new Font("맑은 고딕", Font.BOLD, 28));
                String stars = "★ ★ ★";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(stars, cx - fm.stringWidth(stars) / 2, 94);

                g2.dispose();
            }

            private void drawHeart(Graphics2D g2, int x, int y, int size) {
                Path2D path = new Path2D.Double();
                path.moveTo(x, y + size * 0.35);
                path.curveTo(x, y - size * 0.2, x - size, y - size * 0.2, x - size, y + size * 0.35);
                path.curveTo(x - size, y + size * 0.85, x, y + size * 1.25, x, y + size * 1.55);
                path.curveTo(x, y + size * 1.25, x + size, y + size * 0.85, x + size, y + size * 0.35);
                path.curveTo(x + size, y - size * 0.2, x, y - size * 0.2, x, y + size * 0.35);
                path.closePath();

                g2.setColor(new Color(255, 75, 95));
                g2.fill(path);
                g2.setColor(new Color(255, 160, 170));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(path);
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(220, 110));
        return p;
    }
}