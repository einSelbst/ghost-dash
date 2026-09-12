package dev.einselbst.ghostdash;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReplayPlannerTest {
    @Test
    void mapsFirstAndLastReplayTicksToRouteEnds() {
        assertEquals(0, ReplayPlanner.routeIndex(0, 12, 81));
        assertEquals(80, ReplayPlanner.routeIndex(11, 12, 81));
    }

    @Test
    void handlesSinglePointRoutes() {
        assertEquals(0, ReplayPlanner.routeIndex(7, 12, 1));
    }

    @Test
    void clampsTicksOutsideReplay() {
        assertEquals(0, ReplayPlanner.routeIndex(-4, 12, 81));
        assertEquals(80, ReplayPlanner.routeIndex(99, 12, 81));
    }
}
