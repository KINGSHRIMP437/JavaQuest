import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

public class DodgeGamePanel extends JPanel {
    int playerX = 400, playerY = 180;
    List<Bullet> bullets = new ArrayList<>();
    int spawnCounter = 0;
    Timer gameLoopTimer;
    Timer phaseTimer; // 필드로 승격하여 외부에서 중지 가능하도록 수정
    boolean hitCooldown = false;
    int hitsTaken = 0;
    
    private Timer hitFlashTimer;
    private int hitFlashCount = 0;
    private boolean isFlashing = false;
    
    int currentStage = 1;
    String currentTopicId = "coding";
    
    int laserTimer = 0;
    boolean isHorizontalLaser = false;
    int laserPos = 0;
    boolean isLaserCharging = false;
    boolean isLaserFiring = false;
    int chargeCounter = 0;
    int fireCounter = 0;

    // 포탑 빔 패턴 (테스트: coding 1스테이지)
    // 포탑 한 번: 등장(미끄러져 들어옴) → 충전(빛·예고선) → 발사(빔·번쩍임·흔들림) → 사라짐
    private static final int ALL = 0;    // 일제 사격: 모든 줄이 동시에 발사, 한 줄만 안전
    private static final int SEQ = 1;    // 순차 사격: 한 줄씩 차례로 발사 (방향은 매번 무작위)
    private static final int RING = 2;   // 원형 사격: 원 위의 포탑이 시계 방향으로 하나씩 플레이어를 조준해 발사
    private static final int LANE3 = 3;  // 상·중·하 사격: 필드를 가로 3줄로 나눠, 왼쪽/오른쪽 포탑이 먼저 나온 뒤 그 줄에 발사
    private static final int BOX = 4;    // 작은 상자: 필드가 작은 정사각형으로 줄고, 아래에서 포탑이 올라와 세로 칸에 위로 발사
                                         // 화면(필드)이 바뀌므로 플로우의 맨 마지막에만 둔다
    private static final int RING2 = 5;  // 유도 레이저 강화: RING과 같지만 포탑이 2개씩(서로 90도 떨어져) 나오고 조금 더 빨리 나옴
    private static final int SEQ2 = 6;   // 순차 사격 강화: 양 끝 줄에서 동시에 시작해 가운데로 좁혀 옴 (가로 7줄이면 0·6 → 1·5 → 2·4 → 3)
    private static final int LANE3F = 7; // 상·중·하 사격 강화: 한 줄만 쏘는 발사(앞 3번)는 더 빠르게, 두 줄 동시 발사는 LANE3과 같게 + 세로 빔 1개
    private static final int SWEEP = 8;  // 회전 빔: 필드가 정사각형으로 바뀐 뒤, 가운데를 지나는 넓은 빔이 45도씩 돌며 발사 → 빔 바깥쪽으로 돌며 피함
    private static final int SWEEP_W = 240;         // 정사각형 필드(340px)를 거의 채우는 굵기: 벽 쪽에 플레이어 한 칸 남짓만 남음
    private static final int SWEEPF = 9; // 회전 빔 강화: SWEEP과 같지만 예고가 짧고 더 빨리 빨라짐 (어려움)
    // 한 발마다 다음 발사 간격이 ACCEL배로 줄어듦, GAP_MIN보다는 빨라지지 않음 (DodgeSweepCheck로 피할 수 있는지 확인)
    private static final double SWEEP_ACCEL = 0.93, SWEEPF_ACCEL = 0.88;
    private static final int SWEEP_GAP_MIN = 700, SWEEPF_GAP_MIN = 480;   // 480보다 빠르면 다음 안전 자리까지 못 감
    private static final int MORPH_MS = 2000;       // SWEEP 직전 필드가 직사각형 → 정사각형으로 천천히 바뀌는 시간
    private static final int LANE3F_CHARGE = 600;   // 한 줄 발사의 예고 시간 (LANE3은 900)
    private static final int LANE3F_GAP = 900;      // 한 줄 발사 뒤 다음 발사까지 간격 (LANE3은 1200)
    private static final int LANE3F_COLS = 8;      // 세로 빔 칸: 필드를 세로 8칸으로 나눈 칸 하나에 위 벽에서 아래로
    private static final int LANE3H = 10;          // 상·중·하 사격 강화 (어려움): LANE3F와 같지만 세로 빔이 매번 2개, 한 줄 발사 때는 세로 빔이 조금 늦게
    private static final int LANE3H_V_DELAY = 300; // 한 줄 발사 때 세로 빔이 가로 빔보다 늦게 나가는 시간
    // 유도 레이저 (어려움): 초반엔 1개씩 아주 빠른 간격, 중반엔 1·2·3개 묶음을 무작위로 (묶음끼리 타이밍이 겹쳐도 됨)
    // 패턴 한 줄: {시작, RING3, 0, 포탑 칸 수 = 초반 개수 + 묶음 수 × 3, 초반 개수}. 묶음에서 안 쓰는 칸은 safeLanes로 빼 둠
    private static final int RING3 = 11;
    private static final int RING3_FAST_GAP = 300;   // 초반: 다음 포탑까지 (RING은 1000)
    private static final int RING3_PAUSE = 500;      // 초반이 끝나고 중반 첫 묶음까지 더 쉬는 시간
    // 안전 원: 필드 전체가 레이저 피격 범위가 되고, 8자를 그리며 움직이는 원 안에서만 살아남음. {시작, ZONE, 0, 길이(ZONE_LEG 단위)}
    private static final int ZONE = 12;
    private static final int ZONE_WARN = 1500;   // 예고: 레이저가 흐리게 깜빡이고 원이 필드 가운데에 나타남 (어디서든 원까지 갈 시간)
    private static final int ZONE_LEG = 1100;    // 길이 단위 (ms)
    private static final double ZONE_OMEGA = 0.45; // 8자 움직임 빠르기 (rad/s): 기준 필드에서 최대 약 160px/s (플레이어 333px/s)
    private static final int ZONE_R = 60;        // 원 반지름 (플레이어 20px)
    private static final int ZONE_BEAMS = 35;    // 원 밖에 동시에 보이는 배경 빔 수 (보기용, 판정은 원 밖 전체)
    private static final int BOX_SIDE = 160;       // 줄어든 정사각형 한 변 (세로 4칸 → 칸 40px, 플레이어 20px)
    private static final int BOX_LANES = 4;
    private static final int BOX_SHRINK_MS = 300;  // 필드가 줄어드는 시간 (아주 빠르게)
    private static final int BOX_READY_MS = 300;   // 다 줄어든 뒤 첫 포탑이 나오기까지 쉬는 시간

    private static final int BULLET_END_MS = 3000;
    private static final int SLIDE_MS = 250;
    private static final int FADE_MS = 200;
    // 패턴별 {충전 시간, 발사 시간, 다음 포탑까지 간격} (ms)
    private static final int[][] TIMING = {
        {900, 600, 0},     // ALL
        {600, 160, 500},   // SEQ: 앞 줄 빔이 끝나고 다음 줄이 쏘기 전까지 약 0.4초 틈 → 지나간 줄로 피하기
        {650, 350, 1000},  // RING: 간격 = 충전 + 발사 → 앞 포탑이 쏘고 사라질 때 다음 포탑이 나옴
        {900, 450, 1200},  // LANE3: 포탑이 0.9초 먼저 보이고 발사, 1.2초마다 다음 발사
        {600, 350, 800},   // BOX: 포탑이 0.6초 먼저 보이고 발사 (줄 예고 표시 없음), 0.8초마다 다음 발사
        {650, 350, 850},   // RING2: RING보다 다음 포탑 쌍이 0.15초 빨리 나옴
        {600, 160, 500},   // SEQ2: SEQ와 같은 속도
        {900, 450, 1200},  // LANE3F: 두 줄 발사는 LANE3과 같음 (한 줄 발사는 LANE3F_CHARGE·LANE3F_GAP)
        {800, 400, 1000},  // SWEEP: 예고 0.8초, 첫 간격 1초에서 점점 빨라짐 (최소 0.7초)
        {600, 400, 700},   // SWEEPF: 예고 0.6초, 첫 간격 0.7초에서 점점 빨라짐 (최소 0.48초)
        {900, 450, 1200},  // LANE3H: LANE3F와 같음
        {650, 350, 750},   // RING3: 중반 묶음 간격 0.75초 (충전+발사 1초보다 짧아 앞 묶음과 겹침)
        {ZONE_WARN, 0, ZONE_LEG},   // ZONE: 길이는 eventLength에서 따로 계산
    };
    private static final int RING_LOCK_MS = 250;   // 원형 포탑이 발사 직전 조준을 고정하는 시간
    private static final int RING_BEAM_W = 34;
    private static final int BEAM_LEN = 1500;
    // 패턴 플로우 하나: 글자 탄막 방향 + 패턴 목록
    // 패턴 한 줄: {시작 시각(ms), 패턴, 방향(1=세로 빔, 0=가로 빔), 포탑 수(LANE3·BOX는 발사 횟수), [ALL만: 비울 줄 수(생략하면 1, 최대 2)]}
    // 글자 탄막은 0~3초에 나오고, 다 지나갈 때까지 쉰 뒤 레이저 시작 (아래로 ≈5.5초, 옆으로 ≈6.5초부터)
    // 필드가 바뀌는 패턴(BOX)은 플로우의 맨 마지막에만 둔다
    private static class Flow {
        final boolean sideSnow;   // true = 글자 탄막이 옆으로 (왼쪽→오른쪽 또는 오른쪽→왼쪽)
        final int[][] events;

        Flow(boolean sideSnow, int[]... events) {
            this.sideSnow = sideSnow;
            this.events = events;
        }
    }

    // 스테이지(난이도)별 플로우 목록: [0] = 1스테이지(쉬움), [1] = 2스테이지(중간), [2] = 3스테이지(어려움)
    // 적의 턴마다 그 스테이지 목록에서 하나를 무작위로 고른다 (같은 플로우가 두 번 연속 나오지 않게)
    // 목록이 없는 스테이지는 예전 무작위 탄막·레이저
    private static final Flow[][] FLOWS = {
        {   // ---------- 1스테이지: 쉬움 ----------
            // 플로우 1 (약 20.6초): 글자 탄막 아래로 → 일제 사격 ×2 → 가로 순차 → 유도 레이저
            new Flow(false,
                new int[]{5500,  ALL,  1, 11, 2},   // 위쪽 포탑 일제 사격 (11줄 중 2줄 비움: 필드가 넓어 한 줄로는 못 피함)
                new int[]{7500,  ALL,  0, 7},    // 왼쪽 포탑 일제 사격
                new int[]{9500,  SEQ,  0, 7},    // 가로 빔 순차: 위→아래 또는 아래→위
                new int[]{13900, RING, 0, 6}),   // 원형 포탑 6개가 하나씩 나와 조준 사격 후 사라짐
            // 플로우 2 (약 14.6초): 글자 탄막 옆으로 → 상·중·하 사격 6번 (앞 3번은 한 줄, 뒤 3번은 두 줄 동시)
            new Flow(true,
                new int[]{6500,  LANE3, 0, 6}),
            // 플로우 3 (약 14.6초): 글자 탄막 아래로 → 상·중·하 사격 → 유도 레이저
            new Flow(false,
                new int[]{5500,  LANE3, 0, 3},
                new int[]{9900,  RING,  0, 4}),
            // 플로우 4 (약 13.1초): 글자 탄막 옆으로 → 위쪽 일제 사격 → 가로 순차
            new Flow(true,
                new int[]{6500,  ALL, 1, 11, 2},   // 위쪽 일제 사격, 2줄 비움
                new int[]{8600,  SEQ, 0, 7}),
            // 플로우 5 (약 18.5초): 글자 탄막 아래로 → 왼쪽 일제 사격 → 세로 순차(왼쪽→오른쪽 또는 반대) → 유도 레이저
            new Flow(false,
                new int[]{5500,  ALL,  0, 7},
                new int[]{7500,  SEQ,  1, 11},   // 세로 빔 순차, 약 13.7초에 끝남
                new int[]{13800, RING, 0, 4}),
        },
        {   // ---------- 2스테이지: 중간 (쉬움 구성에서 순차 사격 강화(SEQ2)·유도 레이저 강화(RING2) + 작은 상자) ----------
            // 플로우 1 (약 18.4초): 가로 순차가 위·아래에서 동시에 → 유도 레이저가 2개씩 6번 (포탑 12개)
            new Flow(false,
                new int[]{5500,  ALL,   1, 11, 2},
                new int[]{7500,  ALL,   0, 7},
                new int[]{9500,  SEQ2,  0, 7},    // 약 12.2초에 끝남
                new int[]{12400, RING2, 0, 12}),
            // 플로우 2 (약 13.7초): 상·중·하 사격 강화 6번 (한 줄 발사 3번은 빠르게, 두 줄 발사 3번은 쉬움과 같게)
            new Flow(true,
                new int[]{6500,  LANE3F, 0, 6}),
            // 플로우 3 (약 22.6초): 상·중·하 사격 강화 3번(모두 한 줄 → 빠르게) → 유도 레이저가 2개씩 4번 (포탑 8개)
            //                     → 필드가 정사각형으로 바뀐 뒤 회전 빔 (맨 마지막)
            new Flow(false,
                new int[]{5500,  LANE3F, 0, 3},   // 약 8.8초에 끝남
                new int[]{9000,  RING2,  0, 8},   // 약 13.0초에 끝남
                new int[]{15000, SWEEP,  0, 8}),  // 13~15초에 필드 변형, 넓은 빔 8번 × 45도 = 360도 (방향 무작위), 천천히 빨라짐
            // 플로우 4 (약 19.2초): 글자 탄막 옆으로 → 위쪽 일제 사격 → 가로 순차(위·아래 동시) → 작은 상자 (맨 마지막)
            new Flow(true,
                new int[]{6500,  ALL,  1, 11, 2},   // 위쪽 일제 사격, 2줄 비움
                new int[]{8600,  SEQ2, 0, 7},       // 약 11.3초에 끝남
                new int[]{11900, BOX,  0, 8}),      // 순차 사격이 끝나면 필드가 줄어들고(0.3초) 0.3초 뒤 시작, 8번 발사
            // 플로우 5 (약 15.5초): 왼쪽 일제 사격 → 세로 순차가 양 끝에서 가운데로 → 유도 레이저가 2개씩 4번
            new Flow(false,
                new int[]{5500,  ALL,   0, 7},
                new int[]{7500,  SEQ2,  1, 11},   // 약 11.2초에 끝남
                new int[]{11300, RING2, 0, 8}),
        },
        {   // ---------- 3스테이지: 어려움 (중간 구성 + 상·중·하 사격 강화(어려움) LANE3H + 회전 빔 강화 SWEEPF) ----------
            new Flow(false,
                new int[]{5500,  ALL,   1, 11, 2},
                new int[]{7500,  ALL,   0, 7},
                new int[]{9500,  SEQ2,  0, 7},    // 가로 순차 강화와 동시에
                new int[]{9500,  SEQ,   1, 11},   // 세로 순차 (약 15.7초에 끝남)
                new int[]{15800, RING3, 0, 21, 6}),   // 유도 레이저 (어려움): 초반 6개를 빠르게 → 1~3개 묶음 5번
            new Flow(true,
                new int[]{6500,  LANE3H, 0, 6},   // 약 13.4초에 끝남
                new int[]{13500, ZONE,   0, 6}),  // 안전 원: 1.5초 예고 뒤 원이 6번 옮겨 감 (맨 마지막)
            // 플로우 3 (약 20.5초): 중간 플로우 3과 같지만 세로 빔 2개(LANE3H)·회전 빔이 빠름(SWEEPF)
            new Flow(false,
                new int[]{5500,  LANE3H, 0, 3},
                new int[]{9000,  RING3,  0, 10, 4},   // 초반 4개 → 묶음 2번 (회전 빔 필드 변형 전에 끝남)
                new int[]{15000, SWEEPF, 0, 8}),
            new Flow(true,
                new int[]{6500,  ALL,  1, 11, 2},
                new int[]{8600,  SEQ2, 0, 7},       // 가로 순차 강화와 동시에
                new int[]{8600,  SEQ,  1, 11},      // 세로 순차 (약 14.8초에 끝남)
                new int[]{15300, BOX,  0, 8}),      // 세로 순차가 끝나면 필드가 줄어듦
            // 플로우 5 (약 17.2초): 중간 플로우 5와 같지만 유도 레이저가 2개씩 6번
            new Flow(false,
                new int[]{5500,  ALL,   0, 7},
                new int[]{7500,  SEQ2,  1, 11},   // 세로 순차 강화와 동시에
                new int[]{7500,  SEQ,   0, 7},    // 가로 순차 (약 11.7초에 끝남)
                new int[]{11800, RING3, 0, 21, 6}),
        },
    };
    private static final Color BEAM_COLOR = new Color(0, 230, 220);
    private static final Font BULLET_FONT = new Font("Consolas", Font.BOLD, 13);
    private final int[] recentY = new int[3];   // 옆으로 오는 탄막: 최근에 나온 높이 (겹침 방지)
    private int recentIdx = 0;

    int flow = -1;                      // 이번 적의 턴의 플로우 번호 (-1 = 아직 없음)
    int flowStage = 0;                  // flow를 고른 스테이지 (스테이지가 바뀌면 직전 기록 무시)
    int[][] script = FLOWS[0][0].events;   // 이번 적의 턴에 쓰는 패턴 목록
    int patternEndMs;                   // 이번 스크립트가 끝나는 시각
    boolean sideSnow, snowFromLeft;     // 글자 탄막이 옆으로 오는지, 왼쪽에서 오는지

    long startTime;
    int[][] safeLanes = new int[0][];      // ALL: 비워 둘 줄들
    boolean[] reversed = new boolean[0];   // SEQ: 발사 방향 반대로
    double[] ringOffset = new double[0];   // RING: 첫 포탑 각도
    double[][] ringAim = new double[0][];  // RING: 포탑별 현재 조준 각도
    int[][][] laneShots = new int[0][][];  // LANE3: 이벤트별, 발사마다 {쏘는 줄 비트(위=1, 가운데=2, 아래=4), 오른쪽에서 쏘는 줄 비트}

    private final Set<Integer> pressedKeys = new HashSet<>();
    private Consumer<Integer> onFinish;
    private Runnable onHit; // 맞을 때마다 호출될 콜백
    Random rand = new Random();

    public DodgeGamePanel(Consumer<Integer> onFinish, Runnable onHit) {
        this.onFinish = onFinish;
        this.onHit = onHit;
        setFocusable(true);
        setBackground(new Color(15, 20, 35));
        setPreferredSize(new Dimension(870, 390));

        hitFlashTimer = new Timer(80, e -> {
            hitFlashCount++;
            if (hitFlashCount >= 12) {
                isFlashing = false;
                hitFlashTimer.stop();
            }
            repaint();
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                pressedKeys.add(e.getKeyCode());
            }
            @Override
            public void keyReleased(KeyEvent e) {
                pressedKeys.remove(e.getKeyCode());
            }
        });

        gameLoopTimer = new Timer(30, e -> {
            updatePlayerPosition();
            repaint();
        });
        resetGame();   // 첫 적의 턴 전에 화면이 그려져도 패턴용 배열이 비어 있지 않게
    }

    public void startDodgeGame(String topicId, int stageNo) {
        this.currentTopicId = topicId;
        this.currentStage = stageNo;
        
        // 기존 타이머 중지
        if (gameLoopTimer != null) gameLoopTimer.stop();
        if (phaseTimer != null) phaseTimer.stop();

        // 이번 턴 플로우 무작위 선택 (직전 플로우는 제외). 플로우가 없는 스테이지는 예전 무작위 탄막
        if (useCannonPattern()) {
            Flow[] flows = FLOWS[stageNo - 1];
            if (stageNo != flowStage || flow >= flows.length) flow = -1;   // 스테이지가 바뀌면 직전 기록 무시
            flowStage = stageNo;
            if (flow < 0 || flows.length == 1) {
                flow = rand.nextInt(flows.length);
            } else {
                int next = rand.nextInt(flows.length - 1);   // 직전 플로우를 뺀 나머지 중에서
                flow = next >= flow ? next + 1 : next;
            }
            // 테스트용: -Djavaquest.flow=N 이면 항상 N번 플로우 (1부터)
            int forced = Integer.getInteger("javaquest.flow", 0);
            if (forced >= 1 && forced <= flows.length) flow = forced - 1;
            script = flows[flow].events;
            sideSnow = flows[flow].sideSnow;
            snowFromLeft = rand.nextBoolean();
            patternEndMs = computePatternEnd(script);
        }

        resetGame();
        startTime = System.currentTimeMillis();
        
        if (gameLoopTimer != null) {
            gameLoopTimer.start();
        }
        
        requestFocusInWindow();

        // 패턴 지속 시간 계산 (최소 5초 보장 안전장치 추가)
        int rawDuration = useCannonPattern() ? patternEndMs : 5000 + (stageNo * 500);
        int duration = Math.max(5000, rawDuration); 
        
        phaseTimer = new Timer(duration, e -> {
            stopDodgeGame();
            if (onFinish != null) {
                onFinish.accept(hitsTaken);
            }
        });
        phaseTimer.setRepeats(false);
        phaseTimer.start();
    }

    // 하트가 0이 되었을 때 외부에서 즉시 회피 게임을 멈출 수 있는 메서드
    public void stopDodgeGame() {
        if (gameLoopTimer != null) gameLoopTimer.stop();
        if (phaseTimer != null) phaseTimer.stop();
    }

    private void updatePlayerPosition() {
        int speed = 10;
        int panelWidth = getWidth();
        int panelHeight = getHeight();

        if (panelWidth <= 0 || panelHeight <= 0) return;

        if (pressedKeys.contains(KeyEvent.VK_LEFT)) playerX -= speed;
        if (pressedKeys.contains(KeyEvent.VK_RIGHT)) playerX += speed;
        if (pressedKeys.contains(KeyEvent.VK_UP)) playerY -= speed;
        if (pressedKeys.contains(KeyEvent.VK_DOWN)) playerY += speed;

        // 필드 안(벽에서 5px 안쪽)에만 있게. 필드가 줄어들면 벽에 밀려 같이 안으로 들어옴
        Rectangle f = field(panelWidth, panelHeight, System.currentTimeMillis() - startTime);
        playerX = Math.max(f.x + 5, Math.min(f.x + f.width - 25, playerX));
        playerY = Math.max(f.y + 5, Math.min(f.y + f.height - 25, playerY));
    }

    // 지금 필드 영역 (테두리 안쪽). 평소엔 패널 가장자리에서 20px 안쪽,
    // 스크립트에 BOX 패턴이 있으면 그 직전에 BOX_SHRINK_MS 동안 가운데 작은 정사각형으로 빠르게 줄어듦
    private Rectangle field(int w, int h, long elapsed) {
        Rectangle full = new Rectangle(20, 20, w - 40, h - 40);
        if (!useCannonPattern()) return full;
        for (int[] e : script) {
            if (e[1] == SWEEP || e[1] == SWEEPF) {   // 직사각형 → 세로 길이와 같은 정사각형으로, 천천히 시작해서 천천히 멈춤
                double p = Math.max(0, Math.min(1, (elapsed - (e[0] - MORPH_MS)) / (double) MORPH_MS));
                p = p * p * (3 - 2 * p);
                int side = Math.min(full.width, full.height);
                int x = (int) Math.round(full.x + (w / 2.0 - side / 2.0 - full.x) * p);
                return new Rectangle(x, full.y, (int) Math.round(full.width + (side - full.width) * p), full.height);
            }
            if (e[1] != BOX) continue;
            long shrinkStart = e[0] - BOX_SHRINK_MS - BOX_READY_MS;
            double p = Math.max(0, Math.min(1, (elapsed - shrinkStart) / (double) BOX_SHRINK_MS));
            if (p <= 0) return full;
            p = 1 - Math.pow(1 - p, 3);   // 빠르게 줄다가 끝에서 멈춤
            int side = Math.min(BOX_SIDE, Math.min(full.width, full.height));
            int x = (int) Math.round(full.x + (w / 2.0 - side / 2.0 - full.x) * p);
            int y = (int) Math.round(full.y + (h / 2.0 - side / 2.0 - full.y) * p);
            int fw = (int) Math.round(full.width + (side - full.width) * p);
            int fh = (int) Math.round(full.height + (side - full.height) * p);
            return new Rectangle(x, y, fw, fh);
        }
        return full;
    }

    public void resetGame() {
        playerX = 400;
        playerY = 180;
        bullets.clear();
        spawnCounter = 0;
        hitsTaken = 0;
        hitCooldown = false;
        isFlashing = false;
        if (hitFlashTimer != null) hitFlashTimer.stop();
        if (phaseTimer != null) phaseTimer.stop();
        laserTimer = 0;
        isLaserCharging = false;
        isLaserFiring = false;
        int n = script.length;
        safeLanes = new int[n][];
        reversed = new boolean[n];
        ringOffset = new double[n];
        ringAim = new double[n][];
        laneShots = new int[n][][];
        for (int i = 0; i < n; i++) {
            int lanes = script[i][3];
            safeLanes[i] = script[i][1] == ALL ? pickSafeLanes(lanes, script[i].length > 4 ? script[i][4] : 1)
                         : script[i][1] == RING3 ? ring3Unused(lanes, script[i][4]) : new int[0];
            reversed[i] = rand.nextBoolean();
            ringOffset[i] = rand.nextDouble() * Math.PI * 2;
            ringAim[i] = new double[lanes];
            if (script[i][1] == LANE3 || script[i][1] == LANE3F || script[i][1] == LANE3H) laneShots[i] = makeLaneShots(lanes, 3);
            if (script[i][1] == BOX) laneShots[i] = makeLaneShots(lanes, BOX_LANES);
        }
        pressedKeys.clear();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        if (panelWidth < 100 || panelHeight < 100) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        long elapsed = System.currentTimeMillis() - startTime;
        if (useCannonPattern()) {   // 빔 발사 순간 화면 흔들림
            int[] shake = shakeOffset(elapsed);
            g2.translate(shake[0], shake[1]);
        }

        g.setColor(Color.WHITE);
        Rectangle fieldNow = field(panelWidth, panelHeight, elapsed);
        g.drawRect(fieldNow.x - 5, fieldNow.y - 5, fieldNow.width + 10, fieldNow.height + 10);

        String[] symbols;
        if ("coding".equals(currentTopicId)) {
            symbols = new String[]{";", "{}", "null", "Object", "Error"};
        } else if ("common".equals(currentTopicId)) {
            symbols = new String[]{"?", "!", "상식", "백과", "퀴즈"};
        } else {
            symbols = new String[]{"★", "♪", "캐릭터", "주인공", "스타"};
        }

        // 포탑 패턴에서는 처음 3초 동안만 탄막을 새로 만든다
        boolean spawnBullets = !useCannonPattern() || elapsed < BULLET_END_MS;

        int spawnRate = Math.max(2, 4 - currentStage);   // 몇 프레임마다 하나: 1스테이지 3, 2·3스테이지 2 (1이면 매 프레임이라 거의 못 피함)
        spawnCounter++;
        FontMetrics fm = g.getFontMetrics(BULLET_FONT);
        if (spawnBullets && spawnCounter >= spawnRate) {
            spawnCounter = 0;
            String selectedText = symbols[rand.nextInt(symbols.length)];
            int bx, by, bvx, bvy;
            if (useCannonPattern() && sideSnow) {   // 옆으로: 왼쪽 또는 오른쪽 벽에서 반대편으로 (필드가 가로로 길어 더 빠르게)
                // 오른쪽에서 나올 때는 글자 폭만큼 안쪽에서 시작 (테두리와 겹치지 않게)
                bx = snowFromLeft ? 22 : panelWidth - 20 - fm.stringWidth(selectedText);
                // 같은 벽에서 연달아 나오므로, 최근 3개와 높이가 16px 이상 떨어지게 (글자끼리 겹침 방지)
                int tries = 0;
                do {
                    by = 35 + rand.nextInt(Math.max(50, panelHeight - 70));
                } while (++tries < 20 && tooCloseToRecent(by));
                recentY[recentIdx++ % recentY.length] = by;
                // 속도는 모두 같게(속도가 다르면 뒤 탄막이 앞 탄막을 따라잡아 글자가 자주 겹침),
                // 대신 위아래로 살짝 흘러가는 방향(-1·0·+1)만 무작위로 줘서 일직선으로 보이지 않게
                bvx = (snowFromLeft ? 1 : -1) * (6 + currentStage);
                bvy = rand.nextInt(3) - 1;
            } else {                                 // 아래로: 위에서 떨어짐
                bx = 25 + rand.nextInt(Math.max(50, panelWidth - 60));
                by = 20;
                bvx = rand.nextInt(3) - 1;
                bvy = (2 + currentStage) + rand.nextInt(3);
            }
            bullets.add(new Bullet(bx, by, bvx, bvy, selectedText));
        }

        Rectangle playerRect = new Rectangle(playerX, playerY, 20, 20);

        // 글자 탄막은 필드 안쪽에만 그림 (드나들 때 테두리 선 위로 겹쳐 보이지 않게)
        Shape oldClip = g2.getClip();
        g2.clipRect(16, 16, panelWidth - 32, panelHeight - 32);
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.x += b.vx;
            b.y += b.vy;

            g.setColor(Color.ORANGE);
            g.setFont(BULLET_FONT);
            g.drawString(b.text, b.x, b.y);

            // 판정 상자 = 실제 글자 영역 (drawString의 y는 글자 아래쪽 기준선이라 위로 ascent만큼)
            Rectangle bulletRect = new Rectangle(b.x, b.y - fm.getAscent(), fm.stringWidth(b.text), fm.getAscent());
            if (playerRect.intersects(bulletRect)) {
                applyHit();
                bullets.remove(i);   // 맞은 탄막은 바로 사라짐 (같은 탄막에 다시 맞지 않게, 이슈 #5)
                continue;
            }

            // 위로 나가는 경우(옆으로 오는 탄막이 살짝 위로 흐를 때)도 제거. 위에서 떨어지는 탄막은 y=20에서 시작하므로 15
            if (b.y > panelHeight - 20 || b.y < 15 || b.x < 18 || b.x > panelWidth - 20) {
                bullets.remove(i);
            }
        }
        g2.setClip(oldClip);

        if (useCannonPattern()) {
            for (int ev = 0; ev < script.length; ev++) {
                long t = elapsed - script[ev][0];
                if (t < 0 || t >= eventLength(script[ev])) continue;
                drawEvent(g2, ev, t, playerRect, panelWidth, panelHeight);
            }
        } else {
            drawRandomLaser(g, playerRect, panelWidth, panelHeight);
        }

        drawPlayer(g);
    }

    private boolean tooCloseToRecent(int y) {
        for (int r : recentY) if (Math.abs(r - y) < 16) return true;
        return false;
    }

    // ALL: 비워 둘 줄을 count개 고른다. 2개면 왼쪽 절반·오른쪽 절반에 하나씩 (가장자리에서 1/4쯤 안쪽)
    // → 넓은 필드 어디에 있어도 가까운 빈 줄까지 3칸 이내 (11줄 기준 약 210px, 예고 0.9초 안에 갈 수 있음)
    private int[] pickSafeLanes(int lanes, int count) {
        if (count < 2) return new int[]{rand.nextInt(lanes)};
        int q = lanes / 4;
        return new int[]{q + rand.nextInt(2), lanes - 1 - q - rand.nextInt(2)};
    }

    // RING3 중반 묶음 크기를 1~3개 중 무작위로 정하고, 묶음(3칸)에서 안 쓰는 칸 목록을 돌려줌
    private int[] ring3Unused(int lanes, int singles) {
        List<Integer> unused = new ArrayList<>();
        for (int g = singles; g < lanes; g += 3) {
            int size = 1 + rand.nextInt(3);
            for (int m = size; m < 3; m++) unused.add(g + m);
        }
        return unused.stream().mapToInt(Integer::intValue).toArray();
    }

    // RING3: lane이 속한 묶음에서 실제로 나오는 포탑 수 (1~3)
    private int ring3GroupSize(int ev, int lane) {
        int first = lane - (lane - script[ev][4]) % 3, size = 0;
        for (int m = 0; m < 3; m++) if (!isSafe(ev, first + m)) size++;
        return size;
    }

    private boolean isSafe(int ev, int lane) {
        for (int s : safeLanes[ev]) if (s == lane) return true;
        return false;
    }

    // 포탑 패턴을 쓰는 스테이지: coding 주제에서 FLOWS에 목록이 있는 스테이지 (지금은 1·2스테이지)
    private boolean useCannonPattern() {
        return "coding".equals(currentTopicId) && currentStage >= 1 && currentStage <= FLOWS.length;
    }

    // ---------- 패턴 시간 계산 ----------

    // 이벤트 하나의 전체 길이: 등장 + (포탑 수-1)×간격 + 충전 + 발사 + 퇴장
    static int eventLength(int[] e) {
        int[] tm = TIMING[e[1]];
        int rounds = e[1] == RING2 ? e[3] / 2            // RING2는 2개씩 나오므로 등장 횟수 = 포탑 수 / 2
                   : e[1] == SEQ2 ? (e[3] + 1) / 2       // SEQ2는 양 끝에서 2줄씩 → 가운데까지 절반
                   : e[3];
        if (e[1] == ZONE) return ZONE_WARN + e[3] * ZONE_LEG + FADE_MS;
        if (e[1] == RING3) return (int) (SLIDE_MS + ring3Offset(e[4], e[3] - 1) + tm[0] + tm[1] + FADE_MS);
        int vDelay = e[1] == LANE3H && rounds <= 3 ? LANE3H_V_DELAY : 0;   // 마지막이 한 줄 발사면 늦게 나가는 세로 빔까지
        return (int) (SLIDE_MS + roundOffset(e[1], rounds - 1) + shotCharge(e[1], rounds - 1) + vDelay + tm[1] + FADE_MS);
    }

    // order번째 등장이 첫 등장보다 몇 ms 늦게 충전을 시작하는지. 보통은 order × 간격,
    // LANE3F는 한 줄 발사(앞 3번) 뒤 간격이 더 짧아서 하나씩 더함
    // RING3: lane번 포탑이 첫 포탑보다 몇 ms 늦게 충전을 시작하는지. 초반은 1개씩 FAST_GAP, 중반은 3칸 묶음마다 간격
    static long ring3Offset(int singles, int lane) {
        if (lane < singles) return (long) lane * RING3_FAST_GAP;
        return (long) singles * RING3_FAST_GAP + RING3_PAUSE + (long) ((lane - singles) / 3) * TIMING[RING3][2];
    }

    static long roundOffset(int type, int order) {
        if (type == SWEEP || type == SWEEPF) {   // 한 발 쏠 때마다 간격이 줄어듦
            boolean fast = type == SWEEPF;
            double s = 0, gap = TIMING[type][2];
            for (int j = 0; j < order; j++) {
                s += Math.max(fast ? SWEEPF_GAP_MIN : SWEEP_GAP_MIN, gap);
                gap *= fast ? SWEEPF_ACCEL : SWEEP_ACCEL;
            }
            return Math.round(s);
        }
        if (type != LANE3F && type != LANE3H) return (long) order * TIMING[type][2];
        long s = 0;
        for (int j = 0; j < order; j++) s += j < 3 ? LANE3F_GAP : TIMING[type][2];
        return s;
    }

    // order번째 등장의 충전(예고) 시간. LANE3F의 한 줄 발사(앞 3번)만 더 짧음
    private static int shotCharge(int type, int order) {
        return (type == LANE3F || type == LANE3H) && order < 3 ? LANE3F_CHARGE : TIMING[type][0];
    }

    private static int computePatternEnd(int[][] s) {
        int end = 0;
        for (int[] e : s) end = Math.max(end, e[0] + eventLength(e));
        return end + 250;
    }

    // LANE3·BOX: 발사마다 쏠 줄과 방향을 무작위로 정한다. 앞 3번은 한 줄, 그 뒤는 두 줄 (줄이 3개 이상이라 항상 안전한 줄이 남음)
    // 반환: 발사마다 {쏘는 줄 비트(0번 줄 = 1), 반대쪽 벽에서 쏘는 줄 비트, 세로 빔 칸(직전과 다른 칸), 두 번째 세로 빔 칸(LANE3H)}
    private int[][] makeLaneShots(int shots, int lanes) {
        List<Integer> one = new ArrayList<>(), two = new ArrayList<>();
        for (int i = 0; i < lanes; i++) {
            one.add(1 << i);
            for (int j = i + 1; j < lanes; j++) two.add((1 << i) | (1 << j));
        }
        int[][] r = new int[shots][];
        int prev = 0, prevCol = -1;
        for (int i = 0; i < shots; i++) {
            List<Integer> pool = i < 3 ? one : two;
            int mask;
            do { mask = pool.get(rand.nextInt(pool.size())); } while (mask == prev);   // 같은 줄 연속 방지
            prev = mask;
            int col;
            do { col = rand.nextInt(LANE3F_COLS); } while (col == prevCol);
            prevCol = col;
            int col2;
            do { col2 = rand.nextInt(LANE3F_COLS); } while (col2 == col);
            r[i] = new int[]{mask, rand.nextInt(1 << lanes) & mask, col, col2};
        }
        return r;
    }

    // 포탑(줄) 하나가 충전을 시작하는 시각 (이벤트 시작 기준)
    private long chargeStart(int ev, int lane) {
        int type = script[ev][1], lanes = script[ev][3];
        if (type == RING3) return SLIDE_MS + ring3Offset(script[ev][4], lane);
        int order = (type == SEQ && reversed[ev]) ? lanes - 1 - lane
                  : type == RING2 ? lane / 2   // 포탑 2개(쌍)가 같은 시각에 나옴
                  : type == SEQ2 ? Math.min(lane, lanes - 1 - lane)   // 양 끝 줄부터 가운데로
                  : lane;
        return SLIDE_MS + roundOffset(type, order);
    }

    // ---------- 패턴 그리기 ----------

    // t = 이 이벤트가 시작된 뒤 지난 시간(ms)
    private void drawEvent(Graphics2D g2, int ev, long t, Rectangle playerRect, int w, int h) {
        int type = script[ev][1];
        int lanes = script[ev][3];
        Rectangle fr = field(w, h, script[ev][0] + t);
        int left = fr.x, top = fr.y, right = fr.x + fr.width, bottom = fr.y + fr.height;
        long len = eventLength(script[ev]);

        // 등장: 화면 밖에서 미끄러져 들어옴 (끝에서 감속) / 퇴장: 서서히 투명해짐
        double slide = 1 - Math.pow(1 - Math.min(1.0, t / (double) SLIDE_MS), 3);
        float alpha = t > len - FADE_MS ? Math.max(0f, (len - t) / (float) FADE_MS) : 1f;

        Composite oldComposite = g2.getComposite();
        Shape oldClip = g2.getClip();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.clipRect(left, top, right - left, bottom - top);   // 포탑과 빔이 테두리 밖으로 안 나가게

        double flash = 0;
        if (type == ZONE) {
            drawZone(g2, ev, t, playerRect, fr);
            lanes = 0;   // 포탑 없음
        }
        for (int lane = 0; lane < lanes; lane++) {
            if (isSafe(ev, lane)) continue;
            long chargeT = t - chargeStart(ev, lane);
            long fireT = chargeT - shotCharge(type, lane);
            double f = type == RING || type == RING2 || type == RING3
                    ? drawRingTurret(g2, type, ev, lane, lanes, chargeT, fireT, slide, playerRect, w, h)
                    : type == SWEEP || type == SWEEPF
                    ? drawSweepBeam(g2, type, ev, lane, chargeT, fireT, playerRect, w, h)
                    : type == LANE3 || type == LANE3F || type == LANE3H || type == BOX
                    ? drawLaneShot(g2, type, ev, lane, chargeT, fireT, playerRect, left, top, right, bottom)
                    : drawLaneTurret(g2, type, script[ev][2] == 1, false, lane, lanes, chargeT, fireT, slide,
                                     playerRect, left, top, right, bottom);
            flash = Math.max(flash, f);
        }
        g2.setClip(oldClip);
        g2.setComposite(oldComposite);

        // 발사 순간 화면 번쩍임
        if (flash > 0) {
            g2.setColor(new Color(255, 255, 255, (int) Math.min(255, flash)));
            g2.fillRect(left, top, right - left, bottom - top);
        }
    }

    // 발사 한 번 (LANE3·BOX): 이번에 쏘는 줄마다 벽에서 포탑이 미끄러져 나와 충전하다 발사하고, 쏘고 나면 FADE_MS 동안 사라진다
    // - LANE3: 가로 3줄, 왼쪽 또는 오른쪽 벽에서 / 충전 중 줄 깜빡임 예고
    // - BOX: 세로 BOX_LANES칸, 아래 벽에서 위로 / 예고 없이 포탑만 먼저 보임
    private double drawLaneShot(Graphics2D g2, int type, int ev, int shot, long chargeT, long fireT, Rectangle playerRect,
                                int left, int top, int right, int bottom) {
        int mask = laneShots[ev][shot][0], fromRight = laneShots[ev][shot][1];
        boolean twoLanes = Integer.bitCount(mask) == 2;
        // 세로 빔: LANE3F는 두 줄 발사 때 1개, LANE3H는 매번 2개 (한 줄 발사 때는 LANE3H_V_DELAY만큼 늦게)
        int vBeams = type == LANE3H ? 2 : type == LANE3F && twoLanes ? 1 : 0;
        long vDelay = type == LANE3H && !twoLanes ? LANE3H_V_DELAY : 0;

        long goneT = fireT - vDelay - TIMING[type][1];   // 마지막 빔 발사가 끝난 뒤 지난 시간
        if (chargeT < -SLIDE_MS || goneT >= FADE_MS) return 0;   // 아직 안 나왔거나 이미 사라짐
        double slide = 1 - Math.pow(1 - Math.min(1.0, (chargeT + SLIDE_MS) / (double) SLIDE_MS), 3);
        Composite savedComposite = g2.getComposite();
        if (goneT > 0) g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f - goneT / (float) FADE_MS));

        double flash = 0;
        boolean box = type == BOX;
        int lanes = box ? BOX_LANES : 3;
        for (int lane = 0; lane < lanes; lane++) {   // LANE3: 0=위, 1=가운데, 2=아래 / BOX: 왼쪽부터
            if ((mask & (1 << lane)) == 0) continue;
            boolean flip = box || (fromRight & (1 << lane)) != 0;   // BOX는 항상 아래 벽에서 위로
            flash = Math.max(flash, drawLaneTurret(g2, type, box, flip, lane, lanes,
                    chargeT, fireT, slide, playerRect, left, top, right, bottom));
        }
        for (int v = 0; v < vBeams; v++) {   // 위 벽에서 아래로
            flash = Math.max(flash, drawLaneTurret(g2, type, true, false, laneShots[ev][shot][2 + v], LANE3F_COLS,
                    chargeT, fireT - vDelay, slide, playerRect, left, top, right, bottom));
        }
        g2.setComposite(savedComposite);
        return flash;
    }

    // 일제·순차·상중하 사격: 줄마다 포탑 하나. flip이면 반대쪽 벽(오른쪽/아래)에서 쏨. 반환값 = 화면 번쩍임 세기
    private double drawLaneTurret(Graphics2D g2, int type, boolean vertical, boolean flip, int lane, int lanes,
                                  long chargeT, long fireT, double slide, Rectangle playerRect,
                                  int left, int top, int right, int bottom) {
        int fireLen = TIMING[type][1];
        double laneSize = (vertical ? right - left : bottom - top) / (double) lanes;
        int laneStart = (vertical ? left : top) + (int) Math.round(lane * laneSize) + 3;
        int size = (int) Math.round(laneSize) - 6;   // 빔 사이 틈(6px)은 플레이어(20px)보다 좁아 못 빠져나감
        int center = laneStart + size / 2;
        int edge = (int) Math.round(-50 + 68 * slide);   // 필드 벽에서 포탑까지 (-50 → 18, 벽 밖에서 미끄러져 들어옴)
        int cannonPos = flip ? (vertical ? bottom : right) - edge : (vertical ? top : left) + edge;   // 반대쪽 벽은 대칭 위치
        Rectangle laneRect = vertical
                ? new Rectangle(laneStart, top, size, bottom - top)
                : new Rectangle(left, laneStart, right - left, size);
        // 빔 방향: 아래/오른쪽, flip이면 위/왼쪽
        double angle = vertical ? (flip ? -Math.PI / 2 : Math.PI / 2) : (flip ? Math.PI : 0);

        // 충전 중: 빔이 지나갈 자리를 깜빡이며 예고 (BOX는 예고 없이 포탑만 보여 줌)
        if (type != BOX && chargeT >= 0 && fireT < 0) {
            boolean blink = (chargeT / 90) % 2 == 0;
            g2.setColor(new Color(0, 220, 210, blink ? 70 : 30));
            g2.fill(laneRect);
        }

        double flash = 0;
        if (fireT >= 0 && fireT < fireLen) {
            int muzzle = cannonPos + (flip ? -16 : 16);   // 포구: 포탑에서 빔 방향으로 16px
            int mx = vertical ? center : muzzle;
            int my = vertical ? muzzle : center;
            double k = drawBeamFx(g2, mx, my, angle, size, fireT, fireLen);
            if (k > 0.5 && playerRect.intersects(laneRect)) applyHit();
            if (fireT < 120) flash = (type == ALL ? 100 : 30) * (1 - fireT / 120.0);
        }

        drawCannon(g2, vertical ? center : cannonPos, vertical ? cannonPos : center,
                   angle - Math.PI / 2, chargeT, (int) (chargeT - fireT), fireT);   // 충전 시간 = chargeT - fireT (발사마다 다를 수 있음)
        return flash;
    }

    // 원형 사격: 타원 위에 포탑을 두고, 충전하는 동안 플레이어를 따라 조준하다가 발사 직전에 고정
    // 포탑은 하나씩 따로 나온다: 충전 SLIDE_MS 전에 미끄러져 들어와 → 쏘고 → FADE_MS 동안 사라짐
    private double drawRingTurret(Graphics2D g2, int type, int ev, int lane, int lanes, long chargeT, long fireT,
                                  double slide, Rectangle playerRect, int w, int h) {
        int chargeLen = TIMING[type][0], fireLen = TIMING[type][1];
        long goneT = fireT - fireLen;   // 발사가 끝난 뒤 지난 시간
        if (chargeT < -SLIDE_MS || goneT >= FADE_MS) return 0;   // 아직 안 나왔거나 이미 사라짐
        slide = 1 - Math.pow(1 - Math.min(1.0, (chargeT + SLIDE_MS) / (double) SLIDE_MS), 3);   // 이 포탑의 등장 진행도
        Composite savedComposite = g2.getComposite();
        if (goneT > 0) g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f - goneT / (float) FADE_MS));

        double cx = w / 2.0, cy = h / 2.0;
        double rx = w / 2.0 - 55, ry = h / 2.0 - 48;
        double scale = 1.5 - 0.5 * slide;                        // 바깥에서 안쪽으로 모여듦
        // 포탑 위치 각도 (각도가 커지는 쪽 = 시계 방향)
        // RING: 원을 포탑 수만큼 고르게 나눔 / RING2: 쌍마다 원을 고르게 나누고, 짝은 90도 옆
        //   (180도 맞은편이면 둘 다 플레이어를 조준할 때 같은 직선이 되어 빔이 겹쳐 보임)
        //   RING3: 초반은 한 바퀴를 고르게 나눠 차례로, 중반 묶음은 묶음마다 137도씩 돌린 자리에서 묶음 크기만큼 고르게 나눔
        int singles = type == RING3 ? script[ev][4] : 0;
        double theta = type == RING2
                ? ringOffset[ev] + (lane / 2) * Math.PI * 2 / (lanes / 2) + (lane % 2) * Math.PI / 2
                : type == RING3 && lane < singles
                ? ringOffset[ev] + lane * Math.PI * 2 / singles
                : type == RING3
                ? ringOffset[ev] + ((lane - singles) / 3) * 2.39 + ((lane - singles) % 3) * Math.PI * 2 / ring3GroupSize(ev, lane)
                : ringOffset[ev] + lane * Math.PI * 2 / lanes;
        double tx = cx + rx * scale * Math.cos(theta);
        double ty = cy + ry * scale * Math.sin(theta);
        double px = playerRect.getCenterX(), py = playerRect.getCenterY();

        double[] aim = ringAim[ev];
        if (chargeT < 0) aim[lane] = Math.atan2(cy - ty, cx - tx);              // 대기: 가운데를 바라봄
        else if (fireT < -RING_LOCK_MS) aim[lane] = Math.atan2(py - ty, px - tx); // 충전: 플레이어 추적
        double a = aim[lane];                                                     // 고정 후에는 그대로
        double mx = tx + 16 * Math.cos(a), my = ty + 16 * Math.sin(a);           // 포구 위치
        double ex = mx + BEAM_LEN * Math.cos(a), ey = my + BEAM_LEN * Math.sin(a);

        // 충전 중: 조준선 (추적 중엔 점선 깜빡임, 고정되면 실선)
        if (chargeT >= 0 && fireT < 0) {
            boolean locked = fireT >= -RING_LOCK_MS;
            boolean blink = (chargeT / 80) % 2 == 0;
            g2.setColor(new Color(0, 220, 210, locked ? 170 : (blink ? 100 : 45)));
            g2.setStroke(locked ? new BasicStroke(3)
                    : new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{8, 6}, 0));
            g2.draw(new java.awt.geom.Line2D.Double(mx, my, ex, ey));
            g2.setStroke(new BasicStroke(1));
        }

        double flash = 0;
        if (fireT >= 0 && fireT < fireLen) {
            double k = drawBeamFx(g2, mx, my, a, RING_BEAM_W, fireT, fireLen);
            if (k > 0.5 && java.awt.geom.Line2D.ptSegDist(mx, my, ex, ey, px, py) < RING_BEAM_W / 2.0 + 8) applyHit();
            if (fireT < 120) flash = 40 * (1 - fireT / 120.0);
        }

        drawCannon(g2, tx, ty, a - Math.PI / 2, chargeT, chargeLen, fireT);
        g2.setComposite(savedComposite);
        return flash;
    }

    // ZONE: 원 밖은 전부 피격 범위. 예고 동안은 가운데에 멈춰 있고, 그 뒤엔 가운데에서 출발해 8자(가로 1번, 세로 2번 왕복)를 그림
    // 8자를 도는 방향(좌우·상하)만 매번 무작위
    private void drawZone(Graphics2D g2, int ev, long t, Rectangle playerRect, Rectangle fr) {
        int legs = script[ev][3];
        double sec = Math.max(0, t - ZONE_WARN) / 1000.0;
        double fx = (reversed[ev] ? -1 : 1) * Math.sin(ZONE_OMEGA * sec);
        double fy = (ringOffset[ev] < Math.PI ? -1 : 1) * Math.sin(2 * ZONE_OMEGA * sec);
        double hx = fr.width / 2.0 - ZONE_R - 12, hy = fr.height / 2.0 - ZONE_R - 12;
        double cx = fr.getCenterX() + fx * hx, cy = fr.getCenterY() + fy * hy;
        boolean active = t >= ZONE_WARN && t < ZONE_WARN + (long) legs * ZONE_LEG;
        boolean blink = (t / 120) % 2 == 0;

        // 원 밖: 얇은 빔이 무작위 기울기·불투명도로 끊임없이 발사됨 (예고 때는 적고 흐리게)
        java.awt.geom.Ellipse2D circle = new java.awt.geom.Ellipse2D.Double(cx - ZONE_R, cy - ZONE_R, ZONE_R * 2, ZONE_R * 2);
        java.awt.geom.Area outside = new java.awt.geom.Area(fr);
        outside.subtract(new java.awt.geom.Area(circle));
        Shape oldClip = g2.getClip();
        g2.clip(outside);
        drawZoneBeams(g2, ev, t, fr, active ? ZONE_BEAMS : ZONE_BEAMS / 6, active ? 1f : (blink ? 0.45f : 0.2f));
        g2.setClip(oldClip);

        // 원: 안쪽은 어둡게, 테두리는 밝게
        g2.setColor(new Color(10, 12, 20, 200));
        g2.fill(circle);
        g2.setStroke(new BasicStroke(4));
        g2.setColor(new Color(220, 200, 190, active ? 230 : (blink ? 230 : 120)));
        g2.draw(circle);
        g2.setStroke(new BasicStroke(1));

        // 판정: 플레이어 중심이 원 안쪽(테두리에서 8px 안)을 벗어나면 피격
        if (active && Math.hypot(playerRect.getCenterX() - cx, playerRect.getCenterY() - cy) > ZONE_R - 8) applyHit();
    }

    // ZONE 배경 빔: 칸(slot)마다 빔 하나가 [흐린 예고선 → 밝아지며 발사 → 서서히 꺼짐]을 반복하고, 주기마다 위치·기울기·굵기·불투명도를 새로 뽑음
    // (같은 칸·같은 주기면 같은 값이 나오도록 이벤트 씨앗으로 Random을 만들어 씀 → 프레임마다 깜빡이지 않음)
    private void drawZoneBeams(Graphics2D g2, int ev, long t, Rectangle fr, int count, float dim) {
        long seed = Double.doubleToLongBits(ringOffset[ev]);
        for (int slot = 0; slot < count; slot++) {
            int period = 900 + (slot * 37) % 400;               // 칸마다 주기가 달라 한꺼번에 발사되지 않음
            long tt = t + slot * 131L;
            long cycle = tt / period;
            double k = (tt % period) / (double) period;
            Random r = new Random(seed ^ (slot * 7919L + cycle * 104729L));
            double px = fr.x + r.nextDouble() * fr.width, py = fr.y + r.nextDouble() * fr.height;
            double a = r.nextDouble() * Math.PI;
            float strength = (float) (0.35 + 0.65 * r.nextDouble()) * dim;
            float width = 1.5f + r.nextFloat() * 2.5f;
            double dx = Math.cos(a) * BEAM_LEN, dy = Math.sin(a) * BEAM_LEN;
            java.awt.geom.Line2D line = new java.awt.geom.Line2D.Double(px - dx, py - dy, px + dx, py + dy);
            int cr = BEAM_COLOR.getRed(), cg = BEAM_COLOR.getGreen(), cb = BEAM_COLOR.getBlue();

            if (k < 0.5) {   // 예고: 얇고 흐린 선이 점점 진해짐
                g2.setStroke(new BasicStroke(1));
                g2.setColor(new Color(cr, cg, cb, (int) (25 + 70 * (k / 0.5) * strength)));
                g2.draw(line);
                continue;
            }
            double f = (k - 0.5) / 0.5;                         // 발사: 확 밝아졌다가 서서히 꺼짐
            float on = (float) (f < 0.12 ? f / 0.12 : 1 - (f - 0.12) / 0.88) * strength;
            float w = width * (0.5f + 0.5f * on);
            g2.setStroke(new BasicStroke(w + 6));
            g2.setColor(new Color(cr, cg, cb, (int) (55 * on)));
            g2.draw(line);
            g2.setStroke(new BasicStroke(w));
            g2.setColor(new Color(cr, cg, cb, (int) (235 * on)));
            g2.draw(line);
            if (on > 0.5f) {   // 밝을 때만 가운데 흰 심
                g2.setStroke(new BasicStroke(Math.max(1f, w * 0.4f)));
                g2.setColor(new Color(255, 255, 255, (int) (230 * on)));
                g2.draw(line);
            }
        }
        g2.setStroke(new BasicStroke(1));
    }

    // 회전 빔: shot번째 빔은 필드 가운데를 지나고, 한 발마다 45도씩 같은 방향으로 돈다
    private double drawSweepBeam(Graphics2D g2, int type, int ev, int shot, long chargeT, long fireT,
                                 Rectangle playerRect, int w, int h) {
        int fireLen = TIMING[type][1];
        double step = Math.PI / 4 * (reversed[ev] ? -1 : 1);
        double start = Math.floor(ringOffset[ev] / (Math.PI / 4)) * (Math.PI / 4);   // 시작 각도도 45도 단위
        double a = start + shot * step;
        double cx = w / 2.0, cy = h / 2.0;

        // 충전 중: 빔이 지나갈 띠를 반투명 회색으로 깜빡이며 예고
        if (chargeT >= 0 && fireT < 0) {
            java.awt.geom.AffineTransform saved = g2.getTransform();
            g2.translate(cx, cy);
            g2.rotate(a);
            g2.setColor(new Color(200, 200, 200, (chargeT / 90) % 2 == 0 ? 70 : 45));
            g2.fillRect(-BEAM_LEN / 2, -SWEEP_W / 2, BEAM_LEN, SWEEP_W);
            g2.setTransform(saved);
        }

        double flash = 0;
        if (fireT >= 0 && fireT < fireLen) {
            // drawBeamFx는 시작점에서 한쪽으로만 그리므로 시작점을 반대편 끝으로 옮겨 가운데를 관통시킴
            double k = drawBeamFx(g2, cx - Math.cos(a) * BEAM_LEN / 2, cy - Math.sin(a) * BEAM_LEN / 2, a, SWEEP_W, fireT, fireLen);
            if (k > 0.5 && sweepHits(a, playerRect.getCenterX() - cx, playerRect.getCenterY() - cy)) applyHit();
            if (fireT < 120) flash = 60 * (1 - fireT / 120.0);
        }
        return flash;
    }

    // 필드 가운데 기준 좌표(dx, dy)의 플레이어가 각도 a인 회전 빔에 맞는지: 빔 중심선까지 거리로 판정
    static boolean sweepHits(double a, double dx, double dy) {
        return Math.abs(-dx * Math.sin(a) + dy * Math.cos(a)) < SWEEP_W / 2.0 + 8;
    }

    // 빔 굵기 비율(0~1): 순간 확 펼쳐졌다가 끝에서 가늘어짐
    static double beamStrength(long fireT, int fireLen) {
        double rise = Math.min(70, fireLen * 0.25), fall = Math.min(160, fireLen * 0.4);
        return fireT < rise ? fireT / rise
             : fireT > fireLen - fall ? (fireLen - fireT) / fall : 1.0;
    }

    // 빔 이펙트 (모든 패턴 공통): 순간 확 펼쳐졌다가 가늘어지며 사라짐. 반환값 = 현재 굵기 비율(0~1)
    private double drawBeamFx(Graphics2D g2, double mx, double my, double angle, int size, long fireT, int fireLen) {
        double k = beamStrength(fireT, fireLen);
        double wobble = 1 + 0.08 * Math.sin(fireT / 18.0);   // 빔 굵기가 살짝 떨림
        int beamW = (int) Math.round(size * k * wobble);
        int coreW = (int) Math.round(beamW * 0.45);

        java.awt.geom.AffineTransform saved = g2.getTransform();
        g2.translate(mx, my);
        g2.rotate(angle);
        fillBeamLayer(g2, beamW + 8, new Color(BEAM_COLOR.getRed(), BEAM_COLOR.getGreen(), BEAM_COLOR.getBlue(), 60));
        fillBeamLayer(g2, beamW, new Color(BEAM_COLOR.getRed(), BEAM_COLOR.getGreen(), BEAM_COLOR.getBlue(), 190));
        fillBeamLayer(g2, coreW, Color.WHITE);
        g2.setTransform(saved);
        return k;
    }

    private void fillBeamLayer(Graphics2D g2, int width, Color c) {
        if (width <= 0) return;
        g2.setColor(c);
        g2.fillRect(0, -width / 2, BEAM_LEN, width);
    }

    // </> 모양 포탑. rotation = 0이면 아래를 향함
    private void drawCannon(Graphics2D g2, double cx, double cy, double rotation, long chargeT, int chargeLen, long fireT) {
        java.awt.geom.AffineTransform saved = g2.getTransform();
        g2.translate(cx, cy);
        g2.rotate(rotation);
        if (fireT >= 0 && fireT < 150) g2.translate(0, -4 * (1 - fireT / 150.0));   // 발사 반동

        g2.setColor(new Color(35, 40, 60));
        g2.fillRoundRect(-17, -16, 34, 24, 10, 10);
        g2.fillRect(-8, 6, 16, 10);
        g2.setColor(BEAM_COLOR);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(-17, -16, 34, 24, 10, 10);
        g2.drawRect(-8, 6, 16, 10);
        g2.setStroke(new BasicStroke(1));
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.BOLD, 12));
        g2.drawString("</>", -11, 0);

        // 충전 빛: 포구에 빛이 점점 커지며 깜빡임
        if (chargeT >= 0 && fireT < 0) {
            double p = Math.min(1.0, chargeT / (double) chargeLen);
            int r = (int) Math.round(3 + 10 * p);
            int a = (int) Math.round((110 + 145 * p) * ((chargeT / 60) % 2 == 0 ? 1.0 : 0.6));
            g2.setColor(new Color(190, 255, 250, Math.min(255, a)));
            g2.fillOval(-r, 16 - r, r * 2, r * 2);
        }
        g2.setTransform(saved);
    }

    // 빔이 발사된 직후 잠깐 화면을 흔든다 (일제 사격이 가장 세게)
    private int[] shakeOffset(long elapsed) {
        double[] power = {6, 3, 4, 4, 3, 5, 4, 4, 5, 5, 4, 5, 0};   // ALL, SEQ, RING, LANE3, BOX, RING2, SEQ2, LANE3F, SWEEP, SWEEPF, LANE3H, RING3, ZONE
        double m = 0;
        for (int ev = 0; ev < script.length; ev++) {
            int type = script[ev][1];
            for (int lane = 0; lane < script[ev][3]; lane++) {
                if (isSafe(ev, lane)) continue;
                long fireT = elapsed - script[ev][0] - chargeStart(ev, lane) - shotCharge(type, lane);
                if (fireT >= 0 && fireT < 220) m = Math.max(m, power[type] * (1 - fireT / 220.0));
            }
        }
        int s = (int) Math.round(m);
        return new int[]{rand.nextInt(2 * s + 1) - s, rand.nextInt(2 * s + 1) - s};
    }

    // 기존 패턴: 무작위 위치에 레이저 1개
    private void drawRandomLaser(Graphics g, Rectangle playerRect, int panelWidth, int panelHeight) {
        int laserInterval = Math.max(25, 55 - (currentStage * 8));
        laserTimer++;
        if (laserTimer >= laserInterval && !isLaserCharging && !isLaserFiring) {
            isLaserCharging = true;
            chargeCounter = 0;
            isHorizontalLaser = rand.nextBoolean();
            
            if (isHorizontalLaser) {
                laserPos = 30 + rand.nextInt(Math.max(30, panelHeight - 60));
            } else {
                laserPos = 30 + rand.nextInt(Math.max(30, panelWidth - 60));
            }
        }

        if (isLaserCharging) {
            chargeCounter++;
            g.setColor(Color.YELLOW);
            Graphics2D g2d = (Graphics2D) g;
            float[] dashPattern = {6, 6};
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, dashPattern, 0));

            if (isHorizontalLaser) {
                g2d.drawLine(20, laserPos, panelWidth - 20, laserPos);
            } else {
                g2d.drawLine(laserPos, 20, laserPos, panelHeight - 20);
            }
            g2d.setStroke(new BasicStroke(1));

            g.setFont(new Font("맑은 고딕", Font.BOLD, 13));
            g.drawString("! 충전 중...", isHorizontalLaser ? 30 : Math.max(20, laserPos - 30), isHorizontalLaser ? Math.max(20, laserPos - 8) : 38);

            if (chargeCounter >= 25) {
                isLaserCharging = false;
                isLaserFiring = true;
                fireCounter = 0;
            }
        }

        if (isLaserFiring) {
            fireCounter++;
            g.setColor(new Color(255, 50, 50, 200));

            Rectangle laserRect;
            if (isHorizontalLaser) {
                g.fillRect(20, laserPos - 10, panelWidth - 40, 20);
                laserRect = new Rectangle(20, laserPos - 10, panelWidth - 40, 20);
            } else {
                g.fillRect(laserPos - 10, 20, 20, panelHeight - 40);
                laserRect = new Rectangle(laserPos - 10, 20, 20, panelHeight - 40);
            }

            if (playerRect.intersects(laserRect)) {
                applyHit();
            }

            if (fireCounter >= 15) {
                isLaserFiring = false;
                laserTimer = 0;
            }
        }
    }

    private void drawPlayer(Graphics g) {
        if (isFlashing) {
            g.setColor(hitFlashCount % 2 == 0 ? Color.WHITE : Color.RED);
        } else {
            g.setColor(hitCooldown ? Color.GRAY : Color.RED);
        }
        
        g.fillRect(playerX, playerY, 20, 20);
        // 주제·스테이지·피격 수 글자는 필드 밖(BattleScreen의 dodgeStatus 라벨)에 표시
    }

    // 필드 아래에 띄울 상태 글자
    public String getStatusText() {
        return "[" + currentTopicId.toUpperCase() + " / Stage " + currentStage + "] 회피 중! 피격: " + hitsTaken;
    }

    private void applyHit() {
        if (!hitCooldown) {
            hitsTaken++;
            hitCooldown = true;
            
            isFlashing = true;
            hitFlashCount = 0;
            if (hitFlashTimer != null) hitFlashTimer.restart();

            // 맞을 때마다 즉시 실행할 콜백(onHit) 호출
            if (onHit != null) {
                onHit.run();
            }

            Timer coolTimer = new Timer(1000, evt -> hitCooldown = false);
            coolTimer.setRepeats(false);
            coolTimer.start();
        }
    }

    private static class Bullet {
        int x, y, vx, vy;
        String text;

        Bullet(int x, int y, int vx, int vy, String text) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.text = text;
        }
    }
}