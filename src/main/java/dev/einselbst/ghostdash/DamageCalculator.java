package dev.einselbst.ghostdash;

public final class DamageCalculator {
    private DamageCalculator() {
    }

    public static double calculate(double routeDistance, double baseDamage,
                                   double damagePerBlock, double maximumDamage) {
        double safeDistance = Math.max(0.0, routeDistance);
        double safeBase = Math.max(0.0, baseDamage);
        double safePerBlock = Math.max(0.0, damagePerBlock);
        double safeMaximum = Math.max(0.0, maximumDamage);
        return Math.min(safeMaximum, safeBase + safeDistance * safePerBlock);
    }
}
