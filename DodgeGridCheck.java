// 4×4 칸 패턴(GRID·GRIDT·GRIDT1) 점검: java -ea DodgeGridCheck
// 1) 라운드마다 안전 칸 수가 GRID 2곳, GRIDT 2곳, GRIDT1 1곳인지
// 2) 어느 칸에서 출발해도 예고 시간 안에 안전 칸까지 갈 수 있는지 (플레이어는 한 틱(30ms)에 가로·세로 각각 10px)
public class DodgeGridCheck {
    public static void main(String[] args) {
        int side = 380 - 40 - 60;       // 탄막 패널 높이 380 → 정사각형 필드 한 변 (위아래 GRID_MARGIN 30씩 줄임)
        double cs = side / 4.0;
        int[][] kinds = {{13, 2, 1200}, {14, 2, 1800}, {15, 1, 1500}};   // {타입, 안전 칸 수, 예고 시간}
        java.util.Random rnd = new java.util.Random(1);
        for (int[] kind : kinds) {
            int worstMs = 0;
            for (int[] data : DodgeGamePanel.makeGridShots(kind[0], 2000, rnd)) {
                int safe = 0;
                for (int c = 0; c < 16; c++) if (!DodgeGamePanel.gridDanger(kind[0], data, c)) safe++;
                assert safe == kind[1] : "타입 " + kind[0] + " 안전 칸 " + safe + "곳";
                for (int from = 0; from < 16; from++) {   // 출발 칸 → 가장 가까운 안전 칸 (가로·세로 동시에 움직이므로 더 먼 축이 걸리는 시간)
                    int best = Integer.MAX_VALUE;
                    for (int c = 0; c < 16; c++) {
                        if (DodgeGamePanel.gridDanger(kind[0], data, c)) continue;
                        double d = Math.max(Math.abs(c % 4 - from % 4), Math.abs(c / 4 - from / 4)) * cs;
                        best = Math.min(best, (int) Math.ceil(d / 10) * 30);
                    }
                    worstMs = Math.max(worstMs, best);
                }
            }
            assert worstMs < kind[2] : "타입 " + kind[0] + " 최악 이동 " + worstMs + "ms ≥ 예고 " + kind[2] + "ms";
            System.out.println("OK 타입 " + kind[0] + ": 안전 칸 " + kind[1] + "곳, 가장 먼 안전 칸까지 " + worstMs + "ms (예고 " + kind[2] + "ms)");
        }
    }
}
