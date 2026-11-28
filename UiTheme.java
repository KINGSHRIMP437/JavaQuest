import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 타이틀·주제 선택·스테이지 맵 화면이 같이 쓰는 색상, 글꼴, 이미지 도우미
// 색을 바꾸고 싶으면 여기 한 곳만 고치면 세 화면이 같이 바뀐다.
public final class UiTheme {
    private UiTheme() {}   // 객체를 만들지 않고 UiTheme.MINT 처럼 바로 사용

    // ===== 색상 (시안의 다크 네이비 & 민트 테마) =====
    public static final Color BG          = new Color(15, 20, 35);    // 화면 배경
    public static final Color CARD        = new Color(20, 25, 45);    // 카드 배경
    public static final Color CARD_HOVER  = new Color(26, 34, 61);    // 카드에 마우스를 올렸을 때
    public static final Color BUTTON      = new Color(25, 30, 50);    // 버튼 배경
    public static final Color BUTTON_HOVER = new Color(34, 43, 71);   // 버튼에 마우스를 올렸을 때
    public static final Color BADGE_BG    = new Color(22, 29, 51);    // 오른쪽 위 진행도 배지

    public static final Color MINT        = new Color(0, 200, 190);
    public static final Color MINT_SOFT   = new Color(0, 200, 190, 170);   // 덜 강조된 민트 테두리
    public static final Color YELLOW      = new Color(253, 224, 71);       // 강조 글자
    public static final Color GOLD        = new Color(255, 215, 0);        // 별
    public static final Color GREEN       = new Color(52, 211, 153);       // 클리어
    public static final Color RED         = new Color(239, 68, 68);        // CLEAR 도장, 하트

    public static final Color TEXT        = new Color(241, 245, 249);      // 기본 글자
    public static final Color TEXT_SUB    = new Color(148, 163, 184);      // 보조 설명 글자
    public static final Color TEXT_DIM    = new Color(100, 116, 139);      // 잠긴 카드 글자, 하단 안내
    public static final Color LINE        = new Color(30, 41, 59);         // 카드 안 구분선
    public static final Color GRAY_BORDER = new Color(71, 85, 105);        // 회색 테두리

    // ===== 글꼴 =====
    // "맑은 고딕"이 없는 컴퓨터(맥·리눅스)에서도 한글이 깨지지 않게 있는 글꼴 중 첫 번째를 고른다.
    private static final List<String> FONT_CANDIDATES = Arrays.asList(
            "맑은 고딕", "Malgun Gothic", "Apple SD Gothic Neo", "AppleGothic",
            "NanumGothic", "나눔고딕", "Noto Sans KR", "Noto Sans CJK KR");
    private static String fontFamily;

    public static Font font(int style, int size) {
        if (fontFamily == null) {
            fontFamily = Font.DIALOG;   // 후보가 하나도 없으면 자바 기본 글꼴
            List<String> installed = Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
            for (String name : FONT_CANDIDATES) {
                if (installed.contains(name)) {
                    fontFamily = name;
                    break;
                }
            }
        }
        int readableSize = size <= 18 ? size + 2 : size <= 30 ? size + 1 : size;
        return new Font(fontFamily, style, readableSize);
    }

    // ===== 라벨 =====
    public static JLabel label(String text, Color color, int style, int size) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setForeground(color);
        l.setFont(font(style, size));
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    // 화면 위쪽 가운데의 "작은 영문 태그 + 큰 제목" 두 줄 묶음
    public static JPanel headerTitle(String tag, String title) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(label(tag, MINT, Font.BOLD, 12));
        p.add(Box.createVerticalStrut(4));
        p.add(label(title, Color.WHITE, Font.BOLD, 26));
        return p;
    }

    // 오른쪽 위 진행도 배지 (예: "탈출 진행도 1 / 3"). 숫자 부분은 setBadgeValue로 바꾼다.
    public static JqCard badge() {
        JqCard badge = new JqCard(BADGE_BG, MINT_SOFT, 1, 12);
        badge.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        badge.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        JLabel l = new JLabel();
        l.setFont(font(Font.BOLD, 13));
        badge.add(l);
        return badge;
    }

    public static void setBadgeValue(JqCard badge, String name, String value, Color valueColor) {
        JLabel l = (JLabel) badge.getComponent(0);
        l.setText("<html><font color='" + hex(TEXT) + "'>" + name + "</font>&nbsp;&nbsp;"
                + "<font color='" + hex(valueColor) + "'>" + value + "</font></html>");
    }

    // 헤더 좌우 칸을 같은 너비로 만들어 가운데 제목이 정확히 가운데 오게 하는 틀
    public static JPanel side(Component c, int align) {
        JPanel p = new JPanel(new FlowLayout(align, 0, 0));
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(170, 44));
        if (c != null) p.add(c);
        return p;
    }

    // 별 3칸 문자열 (얻은 별은 금색 ★, 나머지는 회색 ☆)
    public static String starHtml(int stars) {
        StringBuilder sb = new StringBuilder("<html>");
        for (int i = 1; i <= 3; i++) {
            if (i > 1) sb.append("&nbsp;");
            sb.append(i <= stars ? "<font color='" + hex(GOLD) + "'>★</font>" : "<font color='#555B6E'>☆</font>");
        }
        return sb.append("</html>").toString();
    }

    public static String hex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    // ===== 이미지 =====
    private static final Map<String, BufferedImage> imageCache = new HashMap<>();

    // 이미지 파일을 한 번만 읽어서 재사용. 파일이 없으면 null
    public static BufferedImage loadImage(String path) {
        if (path == null) return null;
        if (imageCache.containsKey(path)) return imageCache.get(path);
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File(path));
        } catch (IOException ignored) {
        }
        imageCache.put(path, img);
        return img;
    }

    // 가로세로 비율을 유지한 채 maxW × maxH 안에 들어가게 줄인 아이콘
    public static Icon scaledIcon(Image img, int maxW, int maxH) {
        if (img == null) return null;
        int w = img.getWidth(null), h = img.getHeight(null);
        double scale = Math.min((double) maxW / w, (double) maxH / h);
        return new ImageIcon(img.getScaledInstance(
                Math.max(1, (int) (w * scale)), Math.max(1, (int) (h * scale)), Image.SCALE_SMOOTH));
    }
}
