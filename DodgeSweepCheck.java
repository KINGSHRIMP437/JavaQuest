// 회전 빔(SWEEP) 패턴이 안 맞고 피할 수 있는지 점검: java DodgeSweepCheck
// 플레이어가 갈 수 있는 모든 위치를 30ms(게임 루프 한 틱)마다 넓혀 가며, 빔에 맞는 위치를 지운다.
// 끝까지 살아남는 위치가 하나라도 있으면 피할 수 있는 패턴.
public class DodgeSweepCheck {
    public static void main(String[] args) {
        int h = 380;                    // BattleScreen이 정한 탄막 패널 높이
        int range = h / 2 - 35;         // 정사각형 필드에서 플레이어 중심이 갈 수 있는 범위 (가운데 기준 ±)
        int step = 10, speed = 10, tick = 30;   // 플레이어는 한 틱에 가로·세로 각각 10px
        int shots = 8, fireLen = 400;
        int[][] kinds = {{8, 800}, {9, 600}};   // {SWEEP, 예고 시간}, {SWEEPF, 예고 시간}
        for (int[] kind : kinds) {
        int type = kind[0], charge = kind[1];
        int n = range / step * 2 + 1;
        int eventLen = DodgeGamePanel.eventLength(new int[]{0, type, 0, shots});

        int worst = Integer.MAX_VALUE;
        for (int startIdx = 0; startIdx < 8; startIdx++)
            for (int dir = -1; dir <= 1; dir += 2) {
                boolean[][] ok = new boolean[n][n];
                for (boolean[] row : ok) java.util.Arrays.fill(row, true);   // 시작 위치는 어디든 가능

                for (int t = 0; t < eventLen; t += tick) {
                    boolean[][] next = new boolean[n][n];
                    int alive = 0;
                    for (int i = 0; i < n; i++)
                        for (int j = 0; j < n; j++) {
                            boolean reach = false;   // 한 틱 전에 이웃(또는 제자리)에서 올 수 있었나
                            for (int di = -speed / step; di <= speed / step && !reach; di++)
                                for (int dj = -speed / step; dj <= speed / step && !reach; dj++) {
                                    int a = i + di, b = j + dj;
                                    reach = a >= 0 && b >= 0 && a < n && b < n && ok[a][b];
                                }
                            if (!reach) continue;
                            double dx = i * step - range, dy = j * step - range;
                            boolean hit = false;
                            for (int s = 0; s < shots && !hit; s++) {
                                long fireT = t - (250 + DodgeGamePanel.roundOffset(type, s)) - charge;
                                if (fireT < 0 || fireT >= fireLen || DodgeGamePanel.beamStrength(fireT, fireLen) <= 0.5) continue;
                                hit = DodgeGamePanel.sweepHits((startIdx + dir * s) * Math.PI / 4, dx, dy);
                            }
                            if (!hit) { next[i][j] = true; alive++; }
                        }
                    ok = next;
                    worst = Math.min(worst, alive);
                    assert alive > 0 : "피할 곳 없음: 시작 " + startIdx * 45 + "도, 방향 " + dir + ", " + t + "ms";
                    if (alive == 0) { System.out.println("FAIL 타입 " + type + " 시작 " + startIdx * 45 + "도 방향 " + dir + " @" + t + "ms"); return; }
                }
            }
        System.out.println("OK 타입 " + type + ": 모든 시작 각도·방향에서 피할 수 있음 (가장 좁을 때 안전 위치 " + worst + "곳 / " + n * n + ")");
        }
    }
}
