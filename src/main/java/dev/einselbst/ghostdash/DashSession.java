package dev.einselbst.ghostdash;

import org.bukkit.Location;
import org.bukkit.entity.Mannequin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class DashSession {
    enum Phase { RECORDING, REPLAYING }

    final UUID playerId;
    final Location origin;
    final Mannequin vessel;
    final List<PathPoint> route = new ArrayList<>();
    Phase phase = Phase.RECORDING;
    Location previousLocation;
    Location finalLocation;
    UUID targetId;
    double routeDistance;
    int elapsedTicks;
    int replayTick;

    DashSession(UUID playerId, Location origin, Mannequin vessel) {
        this.playerId = playerId;
        this.origin = origin.clone();
        this.vessel = vessel;
        this.previousLocation = origin.clone();
        this.route.add(PathPoint.from(origin));
    }
}
