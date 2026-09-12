package dev.einselbst.ghostdash;

public final class ReplayPlanner {
    private ReplayPlanner() {
    }

    public static int routeIndex(int replayTick, int replayTicks, int routeSize) {
        if (routeSize <= 1) {
            return 0;
        }
        int safeReplayTicks = Math.max(1, replayTicks);
        int safeTick = Math.max(0, Math.min(replayTick, safeReplayTicks - 1));
        double progress = safeReplayTicks == 1
                ? 1.0
                : (double) safeTick / (safeReplayTicks - 1);
        return Math.min(routeSize - 1, (int) Math.round(progress * (routeSize - 1)));
    }
}
