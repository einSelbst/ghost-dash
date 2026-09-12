package dev.einselbst.ghostdash;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public record GhostDashSettings(
        boolean requireLunge,
        int minimumFoodLevel,
        Set<String> allowedPlayerNames,
        int maxDurationTicks,
        double maxRouteDistance,
        long cooldownMillis,
        int replayDurationTicks,
        int afterimageEveryTicks,
        int afterimageLifetimeTicks,
        double targetLockRange,
        double baseDamage,
        double damagePerBlock,
        double maximumDamage,
        double vesselDamageMultiplier,
        boolean showRouteParticles,
        boolean showAfterimages
) {
    static GhostDashSettings from(FileConfiguration config) {
        Set<String> allowlist = new HashSet<>();
        for (String name : config.getStringList("activation.allowed-player-names")) {
            allowlist.add(name.toLowerCase(Locale.ROOT));
        }

        return new GhostDashSettings(
                config.getBoolean("activation.require-lunge", true),
                boundedInt(config.getInt("activation.minimum-food-level", 6), 0, 20),
                Set.copyOf(allowlist),
                secondsToTicks(config.getDouble("limits.max-duration-seconds", 8.0), 1, 1200),
                bounded(config.getDouble("limits.max-route-distance-blocks", 64.0), 1.0, 512.0),
                secondsToMillis(config.getDouble("limits.cooldown-seconds", 20.0)),
                boundedInt(config.getInt("replay.duration-ticks", 12), 1, 100),
                boundedInt(config.getInt("replay.afterimage-every-ticks", 2), 1, 20),
                boundedInt(config.getInt("replay.afterimage-lifetime-ticks", 4), 1, 40),
                bounded(config.getDouble("replay.target-lock-range-blocks", 12.0), 1.0, 64.0),
                bounded(config.getDouble("damage.base-raw-damage", 4.0), 0.0, 100.0),
                bounded(config.getDouble("damage.raw-damage-per-route-block", 0.65), 0.0, 10.0),
                bounded(config.getDouble("damage.maximum-raw-damage", 20.0), 0.0, 100.0),
                bounded(config.getDouble("damage.vessel-damage-multiplier", 1.0), 0.0, 10.0),
                config.getBoolean("effects.show-route-particles", true),
                config.getBoolean("effects.show-afterimages", true)
        );
    }

    boolean isAllowed(String playerName) {
        return allowedPlayerNames.isEmpty()
                || allowedPlayerNames.contains(playerName.toLowerCase(Locale.ROOT));
    }

    private static int secondsToTicks(double seconds, int minimum, int maximum) {
        return boundedInt((int) Math.round(seconds * 20.0), minimum, maximum);
    }

    private static long secondsToMillis(double seconds) {
        return Math.round(bounded(seconds, 0.0, 3600.0) * 1000.0);
    }

    private static int boundedInt(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double bounded(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
