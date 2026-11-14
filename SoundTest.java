// 소리 점검: java -ea SoundTest (프로젝트 폴더에서 실행, 스피커에서 짧게 소리가 날 수 있음)
public class SoundTest {
    public static void main(String[] args) throws Exception {
        // 1. 브금 재생 → 같은 브금을 다시 요청해도 처음부터 다시 시작하지 않음
        SoundManager.playBgm(SoundManager.BGM_MAIN);
        assert SoundManager.isBgmPlaying() : "브금이 재생 중이어야 함";
        Thread.sleep(300);
        long pos = SoundManager.bgmPositionMicros();
        assert pos > 0 : "브금이 실제로 흘러가야 함";
        SoundManager.playBgm(SoundManager.BGM_MAIN); // 화면 전환 시 다시 불려도
        assert SoundManager.bgmPositionMicros() >= pos : "처음부터 다시 시작하면 안 됨";

        // 1-1. 전투 노래로 전환 → 다시 요청해도 이어서
        SoundManager.playBgm(SoundManager.BGM_BATTLE);
        assert SoundManager.BGM_BATTLE.equals(SoundManager.currentBgm()) : "전투 노래로 바뀌어야 함";
        Thread.sleep(300);
        long bpos = SoundManager.bgmPositionMicros();
        SoundManager.playBgm(SoundManager.BGM_BATTLE);
        assert SoundManager.bgmPositionMicros() >= bpos : "전투 노래도 처음부터 다시 시작하면 안 됨";

        // 2. 정지
        SoundManager.stopBgm();
        assert !SoundManager.isBgmPlaying() : "정지 후에는 재생 중이 아니어야 함";

        // 3. 효과음(클릭·피격·공격): 여러 번 연달아 불러도 예외 없음
        for (int i = 0; i < 3; i++) SoundManager.playClick();
        assert SoundManager.isEffectLoaded(SoundManager.CLICK) : "클릭음 파일을 읽어야 함";
        SoundManager.playEffect(SoundManager.HURT);
        assert SoundManager.isEffectLoaded(SoundManager.HURT) : "피격음 파일을 읽어야 함";
        SoundManager.playEffect(SoundManager.LASER);
        assert SoundManager.isEffectLoaded(SoundManager.LASER) : "공격음 파일을 읽어야 함";
        SoundManager.playEffect(SoundManager.BOSS_DEATH);
        assert SoundManager.isEffectLoaded(SoundManager.BOSS_DEATH) : "보스 사망음 파일을 읽어야 함";
        SoundManager.playEffect(SoundManager.PLAYER_DEATH);
        assert SoundManager.isEffectLoaded(SoundManager.PLAYER_DEATH) : "플레이어 사망음 파일을 읽어야 함";
        SoundManager.playEffect("sounds/없는효과음.wav");
        assert !SoundManager.isEffectLoaded("sounds/없는효과음.wav") : "없는 효과음은 예외 없이 무시";

        // 3-1. 효과음 뒤에 브금 다시: 효과음 동안은 멈춤 → 끝나면 재생
        long len = SoundManager.effectLengthMillis(SoundManager.PLAYER_DEATH);
        assert len > 1000 && len < 1500 : "playerdeath 길이 약 1.2초, 실제 " + len;
        javax.swing.SwingUtilities.invokeAndWait(() ->
                SoundManager.playEffectThenBgm(SoundManager.PLAYER_DEATH, SoundManager.BGM_BATTLE));
        assert SoundManager.currentBgm() == null : "효과음 동안은 브금 멈춤";
        Thread.sleep(len + 400);
        assert SoundManager.BGM_BATTLE.equals(SoundManager.currentBgm()) : "효과음이 끝나면 브금 다시";
        // 효과음 중에 다른 브금이 시작되면 덮어쓰지 않음
        javax.swing.SwingUtilities.invokeAndWait(() ->
                SoundManager.playEffectThenBgm(SoundManager.PLAYER_DEATH, SoundManager.BGM_BATTLE));
        SoundManager.playBgm(SoundManager.BGM_MAIN); // 예: 확인을 눌러 맵으로 감
        Thread.sleep(len + 400);
        assert SoundManager.BGM_MAIN.equals(SoundManager.currentBgm()) : "맵 브금을 전투 노래가 덮으면 안 됨";
        SoundManager.stopBgm();

        // 4. 없는 파일: 게임이 멈추지 않고 그냥 소리 없음
        SoundManager.playBgm("sounds/없는파일.wav");
        assert !SoundManager.isBgmPlaying() : "없는 파일은 재생 안 됨 (예외 없이)";

        System.out.println("OK: 소리 점검 통과");
        System.exit(0);
    }
}
