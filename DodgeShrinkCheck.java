// 조여 오는 원(SHRINK) 점검: java -ea DodgeShrinkCheck
// 1) 틈 한가운데에 있으면 안 맞고, 반대쪽에 있으면 맞는지
// 2) 필드 어디에서 출발해도 첫 원이 오기 전에 틈 방향으로 나갈 수 있는지 (플레이어는 30ms에 10px, 대각선은 계산하지 않음)
// 3) 연달아 오는 다음 원: 틈이 최대 120도 옆으로 옮겨도 간격 안에 갈 수 있는지 (반지름 150px에서 옮긴다고 봄)
public class DodgeShrinkCheck {
    public static void main(String[] args) {
        double gap = 1.0;
        for (double r : new double[]{60, 150, 300, 450}) {
            assert !DodgeGamePanel.shrinkHits(r * Math.cos(gap), r * Math.sin(gap), r, gap, 100) : "틈 가운데 r=" + r;
            assert DodgeGamePanel.shrinkHits(r * Math.cos(gap + Math.PI), r * Math.sin(gap + Math.PI), r, gap, 100) : "반대쪽 r=" + r;
            assert !DodgeGamePanel.shrinkHits(0, 0, r, gap, 100) : "원 안쪽 가운데는 아직 안 맞음 r=" + r;
        }
        for (double r : new double[]{0, 4, 10})   // 원이 가운데로 모이면 가운데에 서 있어도 맞음 (꼼수 방지)
            assert DodgeGamePanel.shrinkHits(0, 0, r, gap, 100) : "가운데 r=" + r;
        int fw = 830, fh = 340;   // 탄막 필드 (패널 870×380에서 테두리 20px씩)
        double r0 = Math.hypot(fw, fh) / 2 + 12, speed = 10 / 30.0;   // px/ms
        for (int dur : new int[]{2600, 2000, 2100}) {   // 쉬움·중간·어려움 (어려움은 틈이 돌지만 줄어드는 시간은 같다고 봄)
            double worstSlack = Double.MAX_VALUE;
            for (int g = 0; g < 36; g++) {
                double ga = g * Math.PI * 2 / 36, ux = Math.cos(ga), uy = Math.sin(ga);
                for (int x = -fw / 2 + 15; x <= fw / 2 - 15; x += 20)
                    for (int y = -fh / 2 + 15; y <= fh / 2 - 15; y += 20) {
                        // 틈 방향 반직선 위 한 점(반지름 rho)까지 가서, 원이 rho에 오기 전에 도착하면 성공. 가장 여유 있는 rho를 고름
                        double best = -Double.MAX_VALUE;
                        for (double rho = 80; rho <= r0; rho += 5) {   // 틈이 좁아지는 가운데 80px 안에서는 못 나간다고 봄
                            double tx = rho * ux, ty = rho * uy;
                            if (Math.abs(tx) > fw / 2.0 - 10 || Math.abs(ty) > fh / 2.0 - 10) break;   // 필드 밖
                            double arrive = Math.hypot(tx - x, ty - y) / speed;
                            double ringAt = dur * (1 - rho / r0);   // 예고 없이 바로 줄어듦
                            best = Math.max(best, ringAt - arrive);
                        }
                        worstSlack = Math.min(worstSlack, best);
                    }
            }
            double move = 2 * 150 * Math.sin(Math.toRadians(120) / 2) / speed;   // 반지름 150에서 120도 옆 틈까지 (직선)
            assert DodgeGamePanel.shrinkNext(dur) - move > 300 : "다음 원까지 " + (int) DodgeGamePanel.shrinkNext(dur) + "ms, 이동 " + (int) move + "ms";
            assert worstSlack > 100 :   // 대각선 이동(더 빠름)은 빼고 계산한 값이라 실제 여유는 더 큼
                   "줄어드는 시간 " + dur + "ms: 가장 불리한 자리 여유 " + (int) worstSlack + "ms";
            System.out.println("OK 줄어드는 시간 " + dur + "ms: 가장 불리한 자리에서도 " + (int) worstSlack + "ms 여유");
        }
    }
}
