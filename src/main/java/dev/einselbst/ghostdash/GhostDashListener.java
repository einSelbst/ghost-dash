package dev.einselbst.ghostdash;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

final class GhostDashListener implements Listener {
    private final GhostDashManager manager;

    GhostDashListener(GhostDashManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (!event.getPlayer().isSneaking()) {
            return;
        }
        if (manager.tryActivate(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (manager.isAvatar(event.getEntity())) {
            event.setCancelled(true);
            if (event instanceof EntityDamageByEntityEvent byEntity) {
                manager.handleAvatarHit(byEntity);
            }
            return;
        }

        if (event.getEntity() instanceof Player victim && manager.protectsGhostBody(victim)) {
            event.setCancelled(true);
            return;
        }

        if (!(event instanceof EntityDamageByEntityEvent byEntity)) {
            return;
        }
        if (byEntity.getDamager() instanceof Player attacker && manager.isActive(attacker)) {
            byEntity.setCancelled(true);
            manager.tryStoreHit(attacker, byEntity.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractWithAvatar(PlayerInteractEntityEvent event) {
        if (manager.isAvatar(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        manager.freezeDuringReplay(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (manager.isActive(event.getPlayer())) {
            event.setCancelled(true);
            manager.message(event.getPlayer(), "Teleportieren ist im Ghost-Zustand gesperrt.");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        manager.applyVisibilityForJoiningPlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.cancel(event.getPlayer(), false, null);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        manager.cancel(event.getPlayer(), false, null);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        manager.cancel(event.getPlayer(), false, null);
    }
}
