import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyEvent;

public class TitleScreen extends JPanel {
    private static final Color BG_COLOR = new Color(15, 20, 35);
    private static final Color BORDER_COLOR = new Color(0, 200, 190);
    // 무적 모드 커맨드: A B A B ← → ← → (키보드 방향키)
    static final int[] GOD_CODE = {KeyEvent.VK_A, KeyEvent.VK_B, KeyEvent.VK_A, KeyEvent.VK_B,
                                   KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT};
    private int codeIdx = 0;
    private final JLabel godLabel = new JLabel(" ", SwingConstants.CENTER);

    public TitleScreen(Runnable onStartClicked, Runnable onHowToPlayClicked) {
        setPreferredSize(new Dimension(1050, 800));
        setBackground(BG_COLOR);
        setLayout(new GridBagLayout());

        JPanel container = new JPanel();
        container.setOpaque(false);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        // 게임 타이틀 로고
        JLabel titleLabel = new JLabel("JavaQuest", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 52));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("프로그래밍 & 지식 모험 RPG", SwingConstants.CENTER);
        subLabel.setForeground(BORDER_COLOR);
        subLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 게임 시작 버튼
        JButton startBtn = new JButton("게임 시작");
        startBtn.setBackground(new Color(25, 30, 50));
        startBtn.setForeground(Color.YELLOW);
        startBtn.setFont(new Font("맑은 고딕", Font.BOLD, 22));
        startBtn.setBorder(new LineBorder(BORDER_COLOR, 2));
        startBtn.setFocusPainted(false);
        startBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        startBtn.setMaximumSize(new Dimension(250, 60));
        startBtn.addActionListener(e -> onStartClicked.run());

        // 게임 설명 버튼
        JButton howToPlayBtn = new JButton("게임 설명");
        howToPlayBtn.setBackground(new Color(25, 30, 50));
        howToPlayBtn.setForeground(Color.WHITE);
        howToPlayBtn.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        howToPlayBtn.setBorder(new LineBorder(BORDER_COLOR, 1));
        howToPlayBtn.setFocusPainted(false);
        howToPlayBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        howToPlayBtn.setMaximumSize(new Dimension(250, 48));
        howToPlayBtn.addActionListener(e -> onHowToPlayClicked.run());

        // 소리 설정 버튼
        JButton soundBtn = new JButton("소리 설정");
        soundBtn.setBackground(new Color(25, 30, 50));
        soundBtn.setForeground(Color.WHITE);
        soundBtn.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        soundBtn.setBorder(new LineBorder(Color.GRAY, 1));
        soundBtn.setFocusPainted(false);
        soundBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        soundBtn.setMaximumSize(new Dimension(250, 48));
        soundBtn.addActionListener(e -> showSoundDialog());

        container.add(titleLabel);
        container.add(Box.createVerticalStrut(10));
        container.add(subLabel);
        container.add(Box.createVerticalStrut(60));
        container.add(startBtn);
        container.add(Box.createVerticalStrut(14));
        container.add(howToPlayBtn);
        container.add(Box.createVerticalStrut(14));
        container.add(soundBtn);

        godLabel.setForeground(new Color(255, 90, 90));
        godLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        godLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        container.add(Box.createVerticalStrut(20));
        container.add(godLabel);
        updateGodLabel();

        add(container);

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

    private void showSoundDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "소리 설정", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setResizable(false);

        JPanel root = new JPanel();
        root.setBackground(BG_COLOR);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 2),
                BorderFactory.createEmptyBorder(24, 28, 24, 28)
        ));

        JLabel header = new JLabel("소리 설정");
        header.setForeground(Color.WHITE);
        header.setFont(new Font("맑은 고딕", Font.BOLD, 22));
        header.setAlignmentX(Component.CENTER_ALIGNMENT);

        root.add(header);
        root.add(Box.createVerticalStrut(22));
        root.add(createVolumeRow("배경음", SoundManager.getBgmVolume(), SoundManager::setBgmVolume, false));
        root.add(Box.createVerticalStrut(16));
        root.add(createVolumeRow("효과음", SoundManager.getEffectVolume(), SoundManager::setEffectVolume, true));
        root.add(Box.createVerticalStrut(24));

        JButton closeBtn = new JButton("닫기");
        closeBtn.setBackground(new Color(25, 30, 50));
        closeBtn.setForeground(Color.YELLOW);
        closeBtn.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        closeBtn.setBorder(new LineBorder(BORDER_COLOR, 1));
        closeBtn.setFocusPainted(false);
        closeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeBtn.setMaximumSize(new Dimension(120, 40));
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
        nameLabel.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        nameLabel.setPreferredSize(new Dimension(56, 30));

        JLabel valueLabel = new JLabel(formatVolume(initialValue), SwingConstants.RIGHT);
        valueLabel.setForeground(BORDER_COLOR);
        valueLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
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