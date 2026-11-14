import javax.sound.sampled.*;
import javax.swing.AbstractButton;
import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.io.File;

// 게임 소리 담당: 브금 반복 재생·정지, 버튼 클릭음
// 소리 파일이 없거나 스피커가 없어도 예외 없이 소리만 나지 않는다.
// (자바 기본 기능은 WAV만 재생 가능 → MP3/OGG는 WAV로 변환해서 sounds/ 폴더에 넣기)
public class SoundManager {
    public static final String BGM_MAIN = "sounds/bgm_intro.wav";    // 타이틀·주제 선택·스테이지 맵·전투 시작 화면
    public static final String BGM_BATTLE = "sounds/bgm_battle.wav"; // 공격 버튼 이후 퀴즈·탄막 중 (playing.mp3 변환)
    public static final String CLICK = "sounds/click.wav";        // 버튼 클릭
    public static final String HURT = "sounds/hitHurt.wav";      // 내 하트가 깎였을 때
    public static final String LASER = "sounds/laserShoot.wav";  // 보스에게 공격이 들어갔을 때
    public static final String BOSS_DEATH = "sounds/bossdeath.wav";     // 보스를 쓰러뜨림 (승리)
    public static final String PLAYER_DEATH = "sounds/playerdeath.wav"; // 내가 쓰러짐 (패배)

    private static final float BGM_VOLUME_DB = -8f;   // 브금은 원본보다 조금 작게 (0 = 원본)
    private static final float EFFECT_VOLUME_DB = 0f;

    private static Clip bgmClip;
    private static String bgmPath;
    private static final java.util.Map<String, Clip> effects = new java.util.HashMap<>(); // 효과음은 한 번 읽어 두고 재사용
    private static boolean clickInstalled;

    // 브금 반복 재생. 같은 브금이 이미 나오고 있으면 그대로 둠 (화면이 바뀌어도 이어서 재생)
    public static synchronized void playBgm(String path) {
        if (path.equals(bgmPath) && bgmClip != null) return;
        stopBgm();
        bgmClip = loadClip(path, BGM_VOLUME_DB);
        if (bgmClip == null) return;
        bgmPath = path;
        bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    public static synchronized void stopBgm() {
        if (bgmClip != null) {
            bgmClip.stop();
            bgmClip.close();
        }
        bgmClip = null;
        bgmPath = null;
    }

    public static synchronized boolean isBgmPlaying() {
        return bgmClip != null;
    }

    // 지금 나오는 브금 경로 (없으면 null)
    public static synchronized String currentBgm() {
        return bgmPath;
    }

    static synchronized long bgmPositionMicros() {
        return bgmClip == null ? 0 : bgmClip.getMicrosecondPosition();
    }

    // 효과음: 부를 때마다 처음부터 재생 (CLICK, HURT, LASER 등)
    public static synchronized void playEffect(String path) {
        Clip clip = effects.computeIfAbsent(path, p -> loadClip(p, EFFECT_VOLUME_DB));
        if (clip == null) {
            effects.remove(path); // 실패한 파일은 다음에 다시 시도
            return;
        }
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    public static void playClick() { playEffect(CLICK); }

    // 효과음 길이(ms). 파일을 못 읽으면 0
    public static synchronized long effectLengthMillis(String path) {
        Clip clip = effects.computeIfAbsent(path, p -> loadClip(p, EFFECT_VOLUME_DB));
        if (clip == null) {
            effects.remove(path);
            return 0;
        }
        return clip.getMicrosecondLength() / 1000;
    }

    // 브금을 멈추고 효과음을 낸 뒤, 효과음이 끝나면 브금을 다시 재생
    // (그 사이에 다른 브금이 시작됐으면 — 예: 맵으로 돌아감 — 건드리지 않음)
    public static void playEffectThenBgm(String effect, String bgm) {
        stopBgm();
        playEffect(effect);
        javax.swing.Timer resume = new javax.swing.Timer((int) effectLengthMillis(effect), e -> {
            if (currentBgm() == null) playBgm(bgm);
        });
        resume.setRepeats(false);
        resume.start();
    }

    static synchronized boolean isEffectLoaded(String path) {
        return effects.get(path) != null;
    }

    // 게임 안의 모든 버튼에 클릭음 연결 (한 번만 호출)
    public static synchronized void installButtonClickSound() {
        if (clickInstalled) return;
        clickInstalled = true;
        Toolkit.getDefaultToolkit().addAWTEventListener(e -> {
            if (e.getID() != MouseEvent.MOUSE_PRESSED) return;
            MouseEvent me = (MouseEvent) e;
            if (me.getButton() == MouseEvent.BUTTON1 && me.getSource() instanceof AbstractButton
                    && ((AbstractButton) me.getSource()).isEnabled()) {
                playClick();
            }
        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    private static Clip loadClip(String path, float volumeDb) {
        try (AudioInputStream in = AudioSystem.getAudioInputStream(new File(path))) {
            Clip clip = AudioSystem.getClip();
            clip.open(in);
            if (volumeDb != 0f && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), volumeDb)));
            }
            return clip;
        } catch (Exception e) { // 파일 없음, 지원 안 하는 형식, 오디오 장치 없음 등
            System.err.println("소리 파일을 재생할 수 없음: " + path + " (" + e.getMessage() + ")");
            return null;
        }
    }
}
