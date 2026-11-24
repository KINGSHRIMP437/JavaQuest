import javax.sound.sampled.*;
import javax.swing.AbstractButton;
import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.io.File;

// 게임 소리 담당: 브금 반복 재생·정지, 버튼 클릭음, 음량 조절
// 소리 파일이 없거나 스피커가 없어도 예외 없이 소리만 나지 않는다.
// (자바 기본 기능은 WAV만 재생 가능 → MP3/OGG는 WAV로 변환해서 sounds/ 폴더에 넣기)
public class SoundManager {
    public static final String BGM_MAIN = "sounds/bgm_intro.wav";    // 타이틀·주제 선택·스테이지 맵·전투 시작 화면
    public static final String BGM_BATTLE = "sounds/bgm_battle.wav"; // 공격 버튼 이후 퀴즈·탄막 중 (playing.mp3 변환)
    public static final String CLICK = "sounds/click.wav";        // 버튼 클릭
    public static final String HURT = "sounds/hitHurt.wav";      // 내 하트가 깎였을 때
    public static final String LASER = "sounds/laserShoot.wav";  // 탄막 빔이 발사될 때
    public static final String SLASH_SWING = "sounds/swordswing.wav"; // 정답: 검기가 지나갈 때 "슉"
    public static final String SLASH_HIT = "sounds/slashHit.wav";     // 정답: 보스가 두 동강 날 때 "챙/퍽"
    public static final String PLAYER_DEATH = "sounds/playerdeath.wav"; // 내가 쓰러짐 (패배)
    public static final String STAGE_CLEAR = "sounds/stageClear.wav";   // 스테이지 클리어 창 (코드로 합성한 소리)
    public static final String TOPIC_CLEAR = "sounds/topicClear.wav";   // 주제 올클리어 축하 화면 (코드로 합성한 소리)

    private static final float BGM_VOLUME_DB = -8f;   // 브금은 원본보다 조금 작게 (0 = 원본)
    private static final float EFFECT_VOLUME_DB = 0f;
    // 효과음별 음량 보정(dB). 원본이 작은 파일만 키움 (자바 최대 약 +6dB)
    private static final java.util.Map<String, Float> EFFECT_VOLUME_OVERRIDE =
            java.util.Map.of(SLASH_SWING, 6f);   // swordswing.wav 원본이 작아서 (최대 -7.7dB)

    private static int bgmVolume = 100;     // 0~100 (0 = 음소거, 100 = 기준 음량)
    private static int effectVolume = 100;  // 0~100 (0 = 음소거, 100 = 기준 음량)

    private static Clip bgmClip;
    private static String bgmPath;
    private static final java.util.Map<String, Clip> effects = new java.util.HashMap<>(); // 효과음은 한 번 읽어 두고 재사용
    private static boolean clickInstalled;

    public static synchronized int getBgmVolume() {
        return bgmVolume;
    }

    public static synchronized int getEffectVolume() {
        return effectVolume;
    }

    // 배경음 음량 설정 (0~100). 지금 재생 중인 브금에도 바로 적용
    public static synchronized void setBgmVolume(int percent) {
        bgmVolume = clampVolume(percent);
        if (bgmClip != null) {
            applyVolume(bgmClip, BGM_VOLUME_DB, bgmVolume);
        }
    }

    // 효과음 음량 설정 (0~100). 이미 불러둔 효과음에도 바로 적용
    public static synchronized void setEffectVolume(int percent) {
        effectVolume = clampVolume(percent);
        for (java.util.Map.Entry<String, Clip> entry : effects.entrySet()) {
            Clip clip = entry.getValue();
            if (clip != null) {
                float baseDb = EFFECT_VOLUME_OVERRIDE.getOrDefault(entry.getKey(), EFFECT_VOLUME_DB);
                applyVolume(clip, baseDb, effectVolume);
            }
        }
    }

    // 브금 반복 재생. 같은 브금이 이미 나오고 있으면 그대로 둠 (화면이 바뀌어도 이어서 재생)
    public static synchronized void playBgm(String path) {
        if (path.equals(bgmPath) && bgmClip != null) return;
        stopBgm();
        bgmClip = loadClip(path, BGM_VOLUME_DB, bgmVolume);
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
        Clip clip = effects.computeIfAbsent(path, SoundManager::loadEffect);
        if (clip == null) {
            effects.remove(path); // 실패한 파일은 다음에 다시 시도
            return;
        }
        clip.stop();
        if (effectVolume == 0) return; // 음량 0이면 재생하지 않음
        clip.setFramePosition(0);
        clip.start();
    }

    public static void playClick() { playEffect(CLICK); }

    // 효과음 길이(ms). 파일을 못 읽으면 0
    public static synchronized long effectLengthMillis(String path) {
        Clip clip = effects.computeIfAbsent(path, SoundManager::loadEffect);
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

    private static int clampVolume(int percent) {
        return Math.max(0, Math.min(100, percent));
    }

    private static void applyVolume(Clip clip, float baseDb, int percent) {
        if (clip.isControlSupported(BooleanControl.Type.MUTE)) {
            ((BooleanControl) clip.getControl(BooleanControl.Type.MUTE)).setValue(percent == 0);
        }
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float db = (percent == 0)
                    ? gain.getMinimum()
                    : baseDb + (float) (20.0 * Math.log10(percent / 100.0));
            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
        }
    }

    private static Clip loadEffect(String path) {
        return loadClip(path, EFFECT_VOLUME_OVERRIDE.getOrDefault(path, EFFECT_VOLUME_DB), effectVolume);
    }

    private static Clip loadClip(String path, float baseDb, int percent) {
        try (AudioInputStream in = toPlayableFormat(AudioSystem.getAudioInputStream(new File(path)))) {
            Clip clip = AudioSystem.getClip();
            clip.open(in);
            applyVolume(clip, baseDb, percent);
            return clip;
        } catch (Exception e) { // 파일 없음, 지원 안 하는 형식, 오디오 장치 없음 등
            System.err.println("소리 파일을 재생할 수 없음: " + path + " (" + e.getMessage() + ")");
            return null;
        }
    }

    // 24비트 등 Clip이 못 여는 WAV는 16비트 PCM으로 바꿔서 연다 (효과음 사이트 파일 중 24비트가 많음)
    private static AudioInputStream toPlayableFormat(AudioInputStream in) {
        AudioFormat f = in.getFormat();
        if (f.getEncoding() == AudioFormat.Encoding.PCM_SIGNED && f.getSampleSizeInBits() == 16) return in;
        AudioFormat pcm16 = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, f.getSampleRate(), 16,
                f.getChannels(), f.getChannels() * 2, f.getSampleRate(), false);
        return AudioSystem.isConversionSupported(pcm16, f) ? AudioSystem.getAudioInputStream(pcm16, in) : in;
    }
}