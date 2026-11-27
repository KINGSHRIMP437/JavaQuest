// 유도 미사일(HOMING) 점검: java -ea DodgeHomingCheck
// 1) steer: 목표 쪽으로 최대 각도만큼만 돌고, 반대편(±180도 근처)도 가까운 쪽으로 돎
// 2) 단순한 피하기(가장 가까운 미사일이 140px 안이면 미사일 진행 방향의 옆으로 비킴)로 3초 유도 + 곧게 날아가는 동안 안 맞는지
//    (플레이어 30ms에 10px, 필드 830×340, 플레이어는 가운데에서 시작, 미사일은 플레이어 반대편부터 고르게)
public class DodgeHomingCheck {
    public static void main(String[] args) {
        assert Math.abs(DodgeGamePanel.steer(0, 1.0, 0.2) - 0.2) < 1e-9 : "최대 각도까지만";
        assert Math.abs(DodgeGamePanel.steer(0, 0.1, 0.2) - 0.1) < 1e-9 : "목표에 바로 맞춤";
        assert Math.abs(DodgeGamePanel.steer(3.0, -3.0, 0.1) - 3.1) < 1e-9 : "±180도 근처는 가까운 쪽(위)으로";
        int[][] kinds = {{1, 190, 110}, {2, 200, 115}, {3, 210, 120}};   // {개수, 빠르기, 트는 빠르기}
        double fw = 830, fh = 340;
        for (int[] k : kinds) {
            int n = k[0];
            double[] mx = new double[n], my = new double[n], mh = new double[n];
            double px = fw / 2 + 60, py = fh / 2;
            for (int i = 0; i < n; i++) {   // 가운데에서 보면 플레이어 반대쪽부터, 벽에서 30px 안
                double a = Math.atan2(fh / 2 - py, fw / 2 - px) + Math.PI * 2 * i / n, dx = Math.cos(a), dy = Math.sin(a);
                double s = Math.min(Math.abs(dx) < 1e-9 ? 1e9 : (fw / 2 - 30) / Math.abs(dx), Math.abs(dy) < 1e-9 ? 1e9 : (fh / 2 - 30) / Math.abs(dy));
                mx[i] = fw / 2 + dx * s; my[i] = fh / 2 + dy * s; mh[i] = Math.atan2(py - my[i], px - mx[i]);
            }
            int hits = 0; boolean cooldown = false; int cool = 0;
            for (int ms = 600; ms < 600 + 3000 + 1500; ms += 30) {
                // 피하기: 가장 가까운 미사일
                int near = 0;
                for (int i = 1; i < n; i++) if (Math.hypot(mx[i] - px, my[i] - py) < Math.hypot(mx[near] - px, my[near] - py)) near = i;
                double d = Math.hypot(mx[near] - px, my[near] - py);
                if (d < 140) {   // 미사일 진행 방향의 옆으로 (벽이 가까우면 반대 옆으로)
                    double sx = -Math.sin(mh[near]), sy = Math.cos(mh[near]);
                    if ((sx * (px - mx[near]) + sy * (py - my[near])) < 0) { sx = -sx; sy = -sy; }
                    double nx = px + Math.signum(sx) * 10 * Math.min(1, Math.abs(sx) * 2), ny = py + Math.signum(sy) * 10 * Math.min(1, Math.abs(sy) * 2);
                    if (nx < 15 || nx > fw - 15 || ny < 15 || ny > fh - 15) { nx = px - Math.signum(sx) * 10; ny = py - Math.signum(sy) * 10; }
                    px = Math.max(15, Math.min(fw - 15, nx)); py = Math.max(15, Math.min(fh - 15, ny));
                }
                for (int i = 0; i < n; i++) {
                    if (ms < 600 + 3000) mh[i] = DodgeGamePanel.steer(mh[i], Math.atan2(py - my[i], px - mx[i]), Math.toRadians(k[2]) * 0.03);
                    mx[i] += Math.cos(mh[i]) * k[1] * 0.03; my[i] += Math.sin(mh[i]) * k[1] * 0.03;
                    if (!cooldown && Math.hypot(px - mx[i], py - my[i]) < 16 + 9) { hits++; cooldown = true; cool = 0; }
                }
                if (cooldown && (cool += 30) > 1000) cooldown = false;   // 맞은 뒤 잠깐 무적 (게임과 비슷하게)
            }
            System.out.println("미사일 " + n + "개 (빠르기 " + k[1] + ", 트는 빠르기 " + k[2] + "도/초): 단순 피하기로 맞은 횟수 " + hits);
            assert n > 1 || hits == 0 : "미사일 1개는 옆으로 비키면 안 맞아야 함";
        }
    }
}
