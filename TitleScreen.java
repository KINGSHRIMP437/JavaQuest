import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.Path2D;
import java.util.Map;

// 타이틀 화면: "지식의 던전에서 탈출하라" 스토리를 보여 주고 모험 시작 / 게임 설명 / 소리 설정으로 이동
// 배경(먼지 입자 + 위쪽 출구 빛)은 DungeonBackground가 그린다.
public class TitleScreen extends DungeonBackground {
    // 무적 모드 커맨드: A B A B ← → ← → (키보드 방향키)
    static final int[] GOD_CODE = {KeyEvent.VK_A, KeyEvent.VK_B, KeyEvent.VK_A, KeyEvent.VK_B,
                                   KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT};
    private int codeIdx = 0;
    private final JLabel godLabel = new JLabel(" ", SwingConstants.CENTER);

    public TitleScreen(Runnable onStartClicked, Runnable onHowToPlayClicked) {
        super(true);   // 위쪽 출구 빛 켜기
        setPreferredSize(new Dimension(1050, 800));
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(40, 24, 32, 24));

        // ===== 위쪽: 세계관 태그 =====
        add(createWorldTag(), BorderLayout.NORTH);

        // ===== 가운데: 문 아이콘 + 로고 + 슬로건 + 메뉴 버튼 =====
        JPanel container = new JPanel();
        container.setOpaque(false);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        DoorIcon door = new DoorIcon();
        door.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new GlowLabel("JavaQuest");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(UiTheme.font(Font.BOLD, 60));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sloganLabel = UiTheme.label("지식의 보스를 물리치고 던전에서 탈출하라", UiTheme.MINT, Font.BOLD, 20);
        JLabel subLabel = UiTheme.label("프로그래밍 · 상식 · 이름 맞추기 3개의 관문을 돌파하세요",
                UiTheme.TEXT_SUB, Font.PLAIN, 14);

        // 메뉴 버튼 3개 (중요한 순서대로 크고 밝게)
        JqButton startBtn = new JqButton(
                "<html>모험 시작&nbsp;&nbsp;<span style='font-size:13pt; color:#00C8BE;'>▶</span></html>",
                UiTheme.YELLOW, UiTheme.MINT, 2, 22);
        setFixedSize(startBtn, 300, 60);
        startBtn.addActionListener(e -> onStartClicked.run());

        JqButton howToPlayBtn = new JqButton("게임 설명", UiTheme.TEXT, UiTheme.MINT_SOFT, 1, 17);
        setFixedSize(howToPlayBtn, 300, 48);
        howToPlayBtn.addActionListener(e -> onHowToPlayClicked.run());

        JqButton soundBtn = new JqButton("소리 설정", new Color(203, 213, 225), UiTheme.GRAY_BORDER, 1, 17);
        setFixedSize(soundBtn, 300, 48);
        soundBtn.addActionListener(e -> showSoundDialog());

        godLabel.setForeground(new Color(255, 90, 90));
        godLabel.setFont(UiTheme.font(Font.BOLD, 18));
        godLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        updateGodLabel();

        container.add(door);
        container.add(Box.createVerticalStrut(14));
        container.add(titleLabel);
        container.add(Box.createVerticalStrut(10));
        container.add(sloganLabel);
        container.add(Box.createVerticalStrut(6));
        container.add(subLabel);
        container.add(Box.createVerticalStrut(40));
        container.add(startBtn);
        container.add(Box.createVerticalStrut(12));
        container.add(howToPlayBtn);
        container.add(Box.createVerticalStrut(12));
        container.add(soundBtn);
        container.add(Box.createVerticalStrut(18));
        container.add(godLabel);

        JPanel centerWrap = new JPanel(new GridBagLayout());   // 가운데 정렬용
        centerWrap.setOpaque(false);
        centerWrap.add(container);
        add(centerWrap, BorderLayout.CENTER);

        // ===== 아래쪽: 안내 문구 =====
        add(UiTheme.label("모든 관문의 보스를 격파하면 던전 출구가 열립니다", UiTheme.TEXT_DIM, Font.PLAIN, 13),
                BorderLayout.SOUTH);

        // 버튼에 포커스가 있어도 키를 받도록 창 전체의 키 입력을 봄 (타이틀 화면이 보일 때만)
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED && isShowing()) {
                codeIdx = nextCodeIndex(codeIdx, e.getKeyCode());
                if (codeIdx == GOD_CODE.length) {
                    codeIdx = 0;
                    BattleEngine.toggleGodMode();
                    updateGodLabel();
                    SoundManager.playEffect(SoundManager.STAGE_CLEAR);
                }
            }
            return false;   // 다른 키 처리는 그대로
        });
    }

    private static void setFixedSize(JComponent c, int w, int h) {
        Dimension d = new Dimension(w, h);
        c.setPreferredSize(d);
        c.setMaximumSize(d);
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
    }

    // "—— ESCAPE THE KNOWLEDGE DUNGEON ——" (글자 사이를 넓힌 영문 태그 + 양옆 짧은 선)
    private JPanel createWorldTag() {
        JPanel tag = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        tag.setOpaque(false);

        JLabel text = new JLabel("ESCAPE THE KNOWLEDGE DUNGEON");
        text.setForeground(new Color(0, 200, 190, 210));
        Font base = UiTheme.font(Font.BOLD, 12);
        text.setFont(base.deriveFont(Map.of(TextAttribute.TRACKING, 0.22f)));   // 자간 넓히기

        tag.add(new TagLine());
        tag.add(text);
        tag.add(new TagLine());
        return tag;
    }

    // 태그 양옆의 가는 민트색 선
    private static class TagLine extends JComponent {
        TagLine() {
            setPreferredSize(new Dimension(26, 14));
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(new Color(0, 200, 190, 130));
            g.fillRect(0, getHeight() / 2, getWidth(), 1);
        }
    }

    // 로고 뒤에 민트빛이 은은하게 번지는 라벨
    private static class GlowLabel extends JLabel {
        GlowLabel(String text) {
            super(text, SwingConstants.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(new Color(0, 200, 190, 16));
            for (int dx = -4; dx <= 4; dx += 2) {          // 글자를 사방으로 살짝 옮겨 연하게 여러 번 그려 빛 효과
                for (int dy = -2; dy <= 6; dy += 2) {
                    g2.drawString(getText(), x + dx, y + dy);
                }
            }
            g2.dispose();
            super.paintComponent(g);   // 원래 글자는 그 위에
        }
    }

    // 빛이 새어 나오는 던전 출구(아치형 문) + 가운데 하트 아이콘
    private static class DoorIcon extends JComponent {
        private static final double SCALE = 1.3;   // 원본 도안 56×64 기준 확대 비율

        DoorIcon() {
            Dimension d = new Dimension((int) (56 * SCALE), (int) (64 * SCALE));
            setPreferredSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate((getWidth() - 56 * SCALE) / 2, 0);
            g2.scale(SCALE, SCALE);

            // 바깥 문틀
            Path2D frame = new Path2D.Double();
            frame.moveTo(10, 60);
            frame.lineTo(10, 26);
            frame.curveTo(10, 15.5, 18, 7, 28, 7);
            frame.curveTo(38, 7, 46, 15.5, 46, 26);
            frame.lineTo(46, 60);
            g2.setColor(new Color(253, 224, 71, 20));
            g2.fill(frame);
            g2.setColor(UiTheme.MINT);
            g2.setStroke(new BasicStroke(2.5f));
            g2.draw(frame);

            // 안쪽 문 (위는 밝고 아래로 갈수록 옅어지는 빛)
            Path2D inner = new Path2D.Double();
            inner.moveTo(16, 60);
            inner.lineTo(16, 27);
            inner.curveTo(16, 19.5, 21.4, 13.5, 28, 13.5);
            inner.curveTo(34.6, 13.5, 40, 19.5, 40, 27);
            inner.lineTo(40, 60);
            inner.closePath();
            g2.setPaint(new GradientPaint(28, 13, new Color(254, 240, 138, 217),
                                          28, 60, new Color(253, 224, 71, 38)));
            g2.fill(inner);

            // 하트 (목숨)
            Path2D heart = new Path2D.Double();
            heart.moveTo(28, 45);
            heart.curveTo(28, 45, 23, 41.4, 23, 38.2);
            heart.curveTo(23, 36.4, 24.4, 35, 26.1, 35);
            heart.curveTo(27.1, 35, 28, 35.5, 28, 36.3);
            heart.curveTo(28, 35.5, 28.9, 35, 29.9, 35);
            heart.curveTo(31.6, 35, 33, 36.4, 33, 38.2);
            heart.curveTo(33, 41.4, 28, 45, 28, 45);
            heart.closePath();
            g2.setColor(UiTheme.RED);
            g2.fill(heart);
            g2.dispose();
        }
    }

    private void showSoundDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "소리 설정", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setResizable(false);

        JPanel root = new JPanel();
        root.setBackground(UiTheme.BG);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiTheme.MINT, 2),
                BorderFactory.createEmptyBorder(24, 28, 24, 28)
        ));

        JLabel header = UiTheme.label("소리 설정", Color.WHITE, Font.BOLD, 22);

        root.add(header);
        root.add(Box.createVerticalStrut(22));
        root.add(createVolumeRow("배경음", SoundManager.getBgmVolume(), SoundManager::setBgmVolume, false));
        root.add(Box.createVerticalStrut(16));
        root.add(createVolumeRow("효과음", SoundManager.getEffectVolume(), SoundManager::setEffectVolume, true));
        root.add(Box.createVerticalStrut(24));

        JqButton closeBtn = new JqButton("닫기", UiTheme.YELLOW, UiTheme.MINT, 1, 16);
        setFixedSize(closeBtn, 120, 40);
        closeBtn.addActionListener(e -> dialog.dispose());
        root.add(closeBtn);

        dialog.setContentPane(root);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JPanel createVolumeRow(String labelText, int initialValue, java.util.function.IntConsumer onChange, boolean playPreview) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(380, 40));

        JLabel nameLabel = new JLabel(labelText);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(UiTheme.font(Font.BOLD, 15));
        nameLabel.setPreferredSize(new Dimension(56, 30));

        JLabel valueLabel = new JLabel(formatVolume(initialValue), SwingConstants.RIGHT);
        valueLabel.setForeground(UiTheme.MINT);
        valueLabel.setFont(UiTheme.font(Font.BOLD, 14));
        valueLabel.setPreferredSize(new Dimension(54, 30));

        JSlider slider = new JSlider(0, 100, initialValue);
        slider.setOpaque(false);
        slider.setFocusable(false);
        slider.setPreferredSize(new Dimension(220, 30));
        slider.addChangeListener(e -> {
            int val = slider.getValue();
            valueLabel.setText(formatVolume(val));
            onChange.accept(val);
            if (playPreview && !slider.getValueIsAdjusting() && val > 0) {
                SoundManager.playClick();
            }
        });

        row.add(nameLabel, BorderLayout.WEST);
        row.add(slider, BorderLayout.CENTER);
        row.add(valueLabel, BorderLayout.EAST);
        return row;
    }

    private static String formatVolume(int value) {
        return value == 0 ? "음소거" : value + "%";
    }

    private void updateGodLabel() {
        godLabel.setText(BattleEngine.isGodMode() ? "무적 모드 ON" : " ");
    }

    // 지금까지 맞게 누른 개수 idx에서 key를 눌렀을 때 다음 개수 (틀리면 처음부터, 틀린 키가 첫 키면 1)
    static int nextCodeIndex(int idx, int key) {
        if (key == GOD_CODE[idx]) return idx + 1;
        return key == GOD_CODE[0] ? 1 : 0;
    }
}
