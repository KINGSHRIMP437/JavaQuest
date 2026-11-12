import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// 이름 맞추기 전용 퀴즈 화면: 이미지를 보고 이름을 키보드로 입력 (Enter로 제출)
// BattleScreen은 prepare() → startNext() 를 부르고, 결과는 Listener로 받아
// 전투가 계속되면 showResult()로 결과를 잠깐 띄운 뒤 탄막으로 넘어간다.
public class NameQuizPanel extends JPanel {
    public interface Listener {
        // correct: 정답 여부, timeout: 시간 초과로 끝났는지, quiz: 방금 낸 문제
        void onResult(boolean correct, boolean timeout, NameQuiz quiz);
    }

    private static final Color BG = new Color(20, 25, 45);
    private static final Color MINT = new Color(0, 200, 190);
    private static final int QUIZ_SECONDS = 15;
    private static final int IMG_W = 360, IMG_H = 200;
    private static NameQuizManager manager; // 파일은 처음 한 번만 읽음

    private final Listener listener;
    private final Random random = new Random();
    // 이름 맞추기 파일(문제 txt, 이미지)이 모여 있는 폴더 (프로젝트 폴더 기준)
    public static final String DIR = "namequiz/";
    public static final int QUIZ_COUNT = 5;         // 스테이지 문제 중 무작위로 낼 개수
    private List<NameQuiz> quizzes = new ArrayList<>(); // 이번 전투에 낼 문제 (순서대로)
    private int index;                                // 다음에 낼 문제 위치
    private NameQuiz current;
    private boolean finished = true;   // 답 입력이 끝났는지
    private Timer afterResult;         // 결과를 잠깐 보여 준 뒤 다음 단계로 넘어가는 타이머

    // 스테이지별 질문 문구 (1: 연예인, 2: 애니메이션 캐릭터, 3: 음식)
    private static final String[] PROMPTS = {
        "Q. 이 연예인의 이름은?", "Q. 이 캐릭터의 이름은?", "Q. 이 음식의 이름은?"
    };

    private final JLabel guide = new JLabel();
    private final JLabel resultLabel = new JLabel(" ");
    private final ImageView imageLabel = new ImageView();
    private final JTextField input = new JTextField();
    private final JProgressBar timeBar = new JProgressBar();
    private final Timer timer;
    private int remain;

    public NameQuizPanel(Listener listener) {
        this.listener = listener;
        setOpaque(false);
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel box = new JPanel(new BorderLayout(5, 5));
        box.setBackground(BG);
        box.setBorder(new LineBorder(Color.GRAY, 1));

        timeBar.setForeground(MINT);
        box.add(timeBar, BorderLayout.NORTH);

        imageLabel.setPreferredSize(new Dimension(IMG_W, IMG_H));
        imageLabel.setForeground(Color.LIGHT_GRAY);
        imageLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 15));
        box.add(imageLabel, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 6));
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(10, 20, 15, 20));
        guide.setForeground(Color.WHITE);
        guide.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        bottom.add(guide, BorderLayout.NORTH);

        input.setFont(new Font("맑은 고딕", Font.PLAIN, 18));
        input.setForeground(Color.WHITE);
        input.setBackground(new Color(25, 30, 50));
        input.setCaretColor(Color.WHITE);
        input.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(MINT, 2), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        // 입력창에 포커스가 들어오면 한글 입력 모드로 전환
        input.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                switchToKorean();
            }
        });
        input.addActionListener(e -> {
            if (!finished && !input.getText().trim().isEmpty())
                finish(current.isCorrect(input.getText()), false);
        });
        bottom.add(input, BorderLayout.CENTER);

        resultLabel.setHorizontalAlignment(SwingConstants.CENTER);
        resultLabel.setFont(new Font("맑은 고딕", Font.BOLD, 26));
        bottom.add(resultLabel, BorderLayout.SOUTH);
        box.add(bottom, BorderLayout.SOUTH);

        add(box, BorderLayout.CENTER);

        timer = new Timer(100, e -> {
            remain--;
            timeBar.setValue(remain);
            if (remain <= 0) finish(false, true);
        });
    }

    // 전투 시작 때 호출: 스테이지 문제 중 무작위 5개를 고르고 개수를 돌려줌 (공격 횟수·보스 HP)
    public int prepare(int stageNo) {
        if (manager == null) manager = new NameQuizManager(DIR + "name_quiz.txt");
        stop();
        guide.setText(promptFor(stageNo) + " (입력 후 Enter)");
        quizzes = pickRandom(manager.findByStage(stageNo), QUIZ_COUNT, random);
        index = 0;
        return quizzes.size();
    }

    public boolean hasQuestions() { return !quizzes.isEmpty(); }

    // 목록에서 겹치지 않게 n개 무작위 선택 (n보다 적으면 전부)
    static List<NameQuiz> pickRandom(List<NameQuiz> src, int n, Random random) {
        List<NameQuiz> copy = new ArrayList<>(src);
        Collections.shuffle(copy, random);
        return new ArrayList<>(copy.subList(0, Math.min(n, copy.size())));
    }

    // 다음 문제 출제 (공격 버튼, 또는 탄막이 끝난 뒤)
    public void startNext() {
        if (index >= quizzes.size()) return;
        current = quizzes.get(index++);
        finished = false;

        guide.setText(promptFor(current.getStage()) + "  (" + index + "/" + quizzes.size() + ")  입력 후 Enter");
        resultLabel.setText(" ");
        showImage(current.getImagePath());
        input.setText("");
        remain = QUIZ_SECONDS * 10;
        timeBar.setMaximum(remain);
        timeBar.setValue(remain);
        timer.restart();
        SwingUtilities.invokeLater(() -> {
            input.requestFocusInWindow();
            switchToKorean(); // 이미 포커스가 있던 경우에도 문제마다 한글 모드로
        });
    }

    // 윈도우 한국어 입력기를 한글 모드로 (한/영 키를 누르지 않아도 바로 한글 입력)
    // 한국어 입력기가 없는 PC에서는 아무 일도 하지 않음
    void switchToKorean() {
        java.awt.im.InputContext ic = input.getInputContext();
        if (ic == null) return;
        try {
            ic.selectInputMethod(java.util.Locale.KOREA);
            ic.setCompositionEnabled(true);
        } catch (UnsupportedOperationException ignored) {
            // 입력기가 한/영 전환을 지원하지 않으면 그대로 둠
        }
    }

    boolean isKoreanMode() {
        java.awt.im.InputContext ic = input.getInputContext();
        try {
            return ic != null && ic.isCompositionEnabled();
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }

    public void stop() {
        timer.stop();
        finished = true;
        if (afterResult != null) afterResult.stop();
    }

    // 결과 글자를 크게 띄우고 delayMs 뒤에 then 실행 (Enter 없이 자동 진행)
    public void showResult(String text, boolean correct, int delayMs, Runnable then) {
        resultLabel.setForeground(correct ? new Color(80, 230, 120) : new Color(255, 90, 90));
        resultLabel.setText(text);
        if (afterResult != null) afterResult.stop();
        afterResult = new Timer(delayMs, e -> then.run());
        afterResult.setRepeats(false);
        afterResult.start();
    }

    // 스테이지별 보스 HP (정답 1개 = HP 1). 1스테이지 3, 2스테이지 4, 그 외는 문제 수(5)
    private static final int[] BOSS_HP = {3, 4};

    public static int bossHpFor(int stageNo, int quizCount) {
        int hp = (stageNo >= 1 && stageNo <= BOSS_HP.length) ? BOSS_HP[stageNo - 1] : quizCount;
        return Math.min(hp, quizCount); // 문제 수보다 많으면 이길 수 없으므로
    }

    public static String promptFor(int stageNo) {
        return (stageNo >= 1 && stageNo <= PROMPTS.length) ? PROMPTS[stageNo - 1] : "Q. 이것의 이름은?";
    }


    private void finish(boolean correct, boolean timeout) {
        if (finished) return; // Enter와 시간 초과가 겹쳐도 한 번만 처리
        finished = true;
        timer.stop();
        if (listener != null) listener.onResult(correct, timeout, current);
    }

    private static final String[] IMAGE_EXTS = {".png", ".jpg", ".jpeg", ".gif", ".bmp"};

    // 적힌 경로에 파일이 없으면 같은 이름의 다른 확장자를 찾음 (celeb_01.png → celeb_01.jpg 등)
    static File findImageFile(String path) {
        File f = new File(path);
        if (f.isFile()) return f;
        int dot = path.lastIndexOf('.');
        String base = dot > path.lastIndexOf('/') ? path.substring(0, dot) : path;
        for (String ext : IMAGE_EXTS) {
            for (String e : new String[]{ext, ext.toUpperCase()}) {
                File alt = new File(base + e);
                if (alt.isFile()) return alt;
            }
        }
        return f; // 못 찾으면 원래 경로 (읽기 실패 → "이미지 없음")
    }

    private void showImage(String path) {
        try {
            BufferedImage img = ImageIO.read(findImageFile(DIR + path));
            if (img == null) throw new IOException("이미지 형식이 아님");
            imageLabel.setImage(img);
            imageLabel.setText("");
        } catch (IOException e) { // 파일이 없거나 못 읽으면 글자로 대신 표시
            imageLabel.setImage(null);
            imageLabel.setText("이미지 없음: " + path);
        }
    }

    // 원본 이미지를 그리는 순간에 화면 크기에 맞춰 고품질로 축소·확대
    // (미리 줄인 아이콘을 쓰면 윈도우 배율 125%·150%에서 다시 늘어나 흐릿해짐)
    private static class ImageView extends JLabel {
        private BufferedImage image;

        ImageView() { super("", SwingConstants.CENTER); }

        void setImage(BufferedImage img) {
            image = img;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (image == null) { super.paintComponent(g); return; } // 글자 표시
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double scale = Math.min((double) getWidth() / image.getWidth(), (double) getHeight() / image.getHeight());
            int w = (int) Math.round(image.getWidth() * scale);
            int h = (int) Math.round(image.getHeight() * scale);
            g2.drawImage(image, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, null);
            g2.dispose();
        }
    }
}
