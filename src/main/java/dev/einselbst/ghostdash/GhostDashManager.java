package dev.einselbst.ghostdash;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.AbstractWindCharge;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.projectiles.ProjectileSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class GhostDashManager {
    private static final String VESSEL = "vessel";
    private static final String AFTERIMAGE = "afterimage";

    private final GhostDashPlugin plugin;
    private final Map<UUID, DashSession> sessions = new HashMap<>();
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();
    private final Set<UUID> spawnedAvatars = new HashSet<>();
    private final Set<UUID> forwardedVesselDamage = new HashSet<>();
    private final Set<UUID> resolvingDashDamage = new HashSet<>();
    private final NamespacedKey avatarKindKey;
    private final NamespacedKey ownerKey;
    private GhostDashSettings settings;
    private BukkitTask ticker;

    GhostDashManager(GhostDashPlugin plugin, GhostDashSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
        this.avatarKindKey = new NamespacedKey(plugin, "avatar-kind");
        this.ownerKey = new NamespacedKey(plugin, "owner");
    }

    void startTicker() {
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    void updateSettings(GhostDashSettings settings) {
        this.settings = settings;
    }

    int activeCount() {
        return sessions.size();
    }

    boolean isActive(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    boolean tryActivate(Player player) {
        if (!player.hasPermission("ghostdash.use")) {
            message(player, "Dafür fehlt dir die Berechtigung.");
            return false;
        }
        if (!settings.isAllowed(player.getName())) {
            message(player, "Ghost Dash ist für dich noch nicht freigeschaltet.");
            return false;
        }
        if (isActive(player)) {
            message(player, "Ghost Dash ist bereits aktiv. /ghostdash cancel bricht ihn ab.");
            return true;
        }

        ItemStack spear = player.getInventory().getItemInMainHand();
        if (!isSpear(spear)) {
            return false;
        }
        if (settings.requireLunge() && spear.getEnchantmentLevel(Enchantment.LUNGE) <= 0) {
            message(player, "Der Speer braucht die Verzauberung Lunge.");
            return true;
        }
        if (player.getFoodLevel() < settings.minimumFoodLevel()) {
            message(player, "Du hast nicht genug Hungerpunkte für Ghost Dash.");
            return true;
        }
        if (player.isGliding() || player.isSwimming() || player.isInsideVehicle()
                || player.getGameMode() == GameMode.SPECTATOR || player.isDead()) {
            message(player, "Ghost Dash kann in diesem Zustand nicht gestartet werden.");
            return true;
        }

        long remaining = cooldownUntil.getOrDefault(player.getUniqueId(), 0L) - System.currentTimeMillis();
        if (remaining > 0L) {
            message(player, "Ghost Dash lädt noch " + formatSeconds(remaining) + " Sekunden.");
            return true;
        }

        Location origin = player.getLocation().clone();
        Mannequin vessel = spawnAvatar(player, origin, VESSEL);
        DashSession session = new DashSession(player.getUniqueId(), origin, vessel);
        sessions.put(player.getUniqueId(), session);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.getUniqueId().equals(player.getUniqueId())) {
                viewer.hidePlayer(plugin, player);
            }
        }

        origin.getWorld().spawnParticle(Particle.REVERSE_PORTAL, origin.clone().add(0, 1, 0),
                45, 0.45, 0.8, 0.45, 0.08);
        origin.getWorld().playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.55f);
        player.sendActionBar(Component.text("GHOST: Bewege dich und triff dann mit dem Speer.", NamedTextColor.AQUA));
        return true;
    }

    void tryStoreHit(Player attacker, Entity target) {
        DashSession session = sessions.get(attacker.getUniqueId());
        if (session == null || session.phase != DashSession.Phase.RECORDING) {
            return;
        }
        if (!(target instanceof LivingEntity livingTarget) || target.getUniqueId().equals(attacker.getUniqueId())) {
            return;
        }
        if (!isSpear(attacker.getInventory().getItemInMainHand())) {
            message(attacker, "Nur ein Speertreffer löst Ghost Dash aus.");
            return;
        }
        if (settings.requireLunge()
                && attacker.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.LUNGE) <= 0) {
            message(attacker, "Der auslösende Speer braucht Lunge.");
            return;
        }

        session.phase = DashSession.Phase.REPLAYING;
        session.finalLocation = attacker.getLocation().clone();
        session.targetId = livingTarget.getUniqueId();
        session.route.add(PathPoint.from(session.finalLocation));
        session.replayTick = 0;
        attacker.setVelocity(attacker.getVelocity().zero());
        attacker.sendActionBar(Component.text("GHOST DASH!", NamedTextColor.LIGHT_PURPLE));
    }

    private void tick() {
        for (DashSession session : new ArrayList<>(sessions.values())) {
            Player player = Bukkit.getPlayer(session.playerId);
            if (player == null || !player.isOnline()) {
                cleanupSession(session, false);
                continue;
            }
            if (session.phase == DashSession.Phase.RECORDING) {
                tickRecording(player, session);
            } else {
                tickReplay(player, session);
            }
        }
    }

    private void tickRecording(Player player, DashSession session) {
        if (!player.getWorld().equals(session.origin.getWorld())) {
            cancel(player, false, null);
            return;
        }

        Location current = player.getLocation().clone();
        double step = session.previousLocation.distance(current);
        if (step < 8.0) {
            session.routeDistance += step;
        }
        session.previousLocation = current;
        session.route.add(PathPoint.from(current));
        session.elapsedTicks++;

        if (session.elapsedTicks % 10 == 0) {
            double secondsLeft = Math.max(0.0,
                    (settings.maxDurationTicks() - session.elapsedTicks) / 20.0);
            player.sendActionBar(Component.text(
                    "GHOST  " + String.format("%.1fs", secondsLeft)
                            + "  " + String.format("%.1f/%.0f Blöcke", session.routeDistance,
                            settings.maxRouteDistance()), NamedTextColor.AQUA));
        }

        if (session.elapsedTicks >= settings.maxDurationTicks()) {
            cancel(player, true, "Ghost-Zeit abgelaufen — Rückkehr zur Hülle.");
        } else if (session.routeDistance >= settings.maxRouteDistance()) {
            cancel(player, true, "Maximale Ghost-Strecke erreicht — Rückkehr zur Hülle.");
        }
    }

    private void tickReplay(Player player, DashSession session) {
        int index = ReplayPlanner.routeIndex(session.replayTick,
                settings.replayDurationTicks(), session.route.size());
        Location point = session.route.get(index).toLocation(session.origin.getWorld());
        session.vessel.teleport(point);

        World world = point.getWorld();
        if (settings.showRouteParticles()) {
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, point.clone().add(0, 0.9, 0),
                    9, 0.25, 0.45, 0.25, 0.015);
            world.spawnParticle(Particle.CLOUD, point.clone().add(0, 0.15, 0),
                    5, 0.2, 0.12, 0.2, 0.01);
        }
        if (settings.showAfterimages()
                && session.replayTick > 0
                && session.replayTick % settings.afterimageEveryTicks() == 0) {
            spawnAfterimage(player, point);
        }

        session.replayTick++;
        if (session.replayTick >= settings.replayDurationTicks()) {
            completeReplay(player, session);
        }
    }

    private void completeReplay(Player player, DashSession session) {
        Entity possibleTarget = Bukkit.getEntity(session.targetId);
        LivingEntity target = possibleTarget instanceof LivingEntity living ? living : null;
        Location endpoint = session.finalLocation.clone();
        double rawDamage = DamageCalculator.calculate(session.routeDistance,
                settings.baseDamage(), settings.damagePerBlock(), settings.maximumDamage());

        cleanupSession(session, true);
        endpoint.getWorld().spawnParticle(Particle.EXPLOSION, endpoint.clone().add(0, 1, 0),
                2, 0.2, 0.2, 0.2, 0.0);
        endpoint.getWorld().spawnParticle(Particle.SWEEP_ATTACK, endpoint.clone().add(0, 1, 0),
                4, 0.4, 0.4, 0.4, 0.0);
        endpoint.getWorld().playSound(endpoint, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.65f, 1.65f);

        if (target != null && target.isValid() && !target.isDead()
                && target.getWorld().equals(endpoint.getWorld())
                && target.getLocation().distance(endpoint) <= settings.targetLockRange()) {
            double healthBefore = target.getHealth() + target.getAbsorptionAmount();
            target.setNoDamageTicks(0);
            resolvingDashDamage.add(target.getUniqueId());
            try {
                target.damage(rawDamage, player);
            } finally {
                resolvingDashDamage.remove(target.getUniqueId());
            }
            double effectiveDamage = Math.max(0.0,
                    healthBefore - target.getHealth() - target.getAbsorptionAmount());
            plugin.getLogger().info("Resolved Ghost Dash hit from " + player.getName()
                    + " to " + target.getName()
                    + ": raw=" + String.format("%.2f", rawDamage)
                    + ", effective=" + String.format("%.2f", effectiveDamage));
            player.sendActionBar(Component.text(
                    "Treffer: " + String.format("%.1f", effectiveDamage / 2.0)
                            + " Herzen effektiv (" + String.format("%.1f", rawDamage / 2.0) + " roh)",
                    NamedTextColor.LIGHT_PURPLE));
        } else {
            player.sendActionBar(Component.text("Der gespeicherte Treffer ist entkommen.", NamedTextColor.GRAY));
        }
    }

    boolean cancel(Player player, boolean returnToOrigin, String reason) {
        DashSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return false;
        }
        Location origin = session.origin.clone();
        cleanupSession(session, true);
        if (returnToOrigin && player.isOnline() && !player.isDead()) {
            player.teleport(origin);
        }
        if (reason != null) {
            message(player, reason);
        }
        return true;
    }

    void cancelAll(boolean returnToOrigin, String reason) {
        for (DashSession session : new ArrayList<>(sessions.values())) {
            Player player = Bukkit.getPlayer(session.playerId);
            if (player != null) {
                cancel(player, returnToOrigin, reason);
            } else {
                cleanupSession(session, false);
            }
        }
    }

    private void cleanupSession(DashSession session, boolean applyCooldown) {
        sessions.remove(session.playerId);
        if (session.vessel.isValid()) {
            session.vessel.remove();
        }
        spawnedAvatars.remove(session.vessel.getUniqueId());
        Player player = Bukkit.getPlayer(session.playerId);
        if (player != null) {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                viewer.showPlayer(plugin, player);
            }
        }
        if (applyCooldown) {
            cooldownUntil.put(session.playerId, System.currentTimeMillis() + settings.cooldownMillis());
        }
    }

    void handleAvatarHit(EntityDamageByEntityEvent event) {
        Entity avatar = event.getEntity();
        String kind = avatar.getPersistentDataContainer().get(avatarKindKey, PersistentDataType.STRING);
        if (!VESSEL.equals(kind)) {
            return;
        }
        String ownerValue = avatar.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (ownerValue == null) {
            return;
        }

        UUID ownerId;
        try {
            ownerId = UUID.fromString(ownerValue);
        } catch (IllegalArgumentException ignored) {
            return;
        }
        DashSession session = sessions.get(ownerId);
        Player owner = Bukkit.getPlayer(ownerId);
        if (session == null || owner == null) {
            avatar.remove();
            return;
        }

        Entity source = resolveDamageSource(event.getDamager());
        if (source != null && source.getUniqueId().equals(ownerId)) {
            return;
        }
        double forwardedDamage = Math.max(0.0, event.getDamage() * settings.vesselDamageMultiplier());
        forwardedVesselDamage.add(ownerId);
        try {
            if (source != null) {
                owner.damage(forwardedDamage, source);
            } else {
                owner.damage(forwardedDamage);
            }
        } finally {
            forwardedVesselDamage.remove(ownerId);
        }
    }

    private Entity resolveDamageSource(Entity directSource) {
        if (directSource instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Entity entity) {
                return entity;
            }
        }
        return directSource;
    }

    boolean isAvatar(Entity entity) {
        return entity.getPersistentDataContainer().has(avatarKindKey, PersistentDataType.STRING);
    }

    boolean isForwardedVesselDamage(Player player) {
        return forwardedVesselDamage.contains(player.getUniqueId());
    }

    boolean isResolvingDashDamage(Entity entity) {
        return resolvingDashDamage.contains(entity.getUniqueId());
    }

    boolean protectsGhostBody(Player player, EntityDamageEvent event) {
        if (!sessions.containsKey(player.getUniqueId())) {
            return false;
        }
        Entity directSource = event.getDamageSource().getDirectEntity();
        return !(directSource instanceof AbstractWindCharge);
    }

    void freezeDuringReplay(PlayerMoveEvent event) {
        DashSession session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null || session.phase != DashSession.Phase.REPLAYING || session.finalLocation == null) {
            return;
        }
        if (event.hasChangedPosition()) {
            Location frozen = session.finalLocation.clone();
            frozen.setYaw(event.getTo().getYaw());
            frozen.setPitch(event.getTo().getPitch());
            event.setTo(frozen);
        }
    }

    void applyVisibilityForJoiningPlayer(Player viewer) {
        for (UUID playerId : sessions.keySet()) {
            Player ghost = Bukkit.getPlayer(playerId);
            if (ghost != null && !viewer.getUniqueId().equals(playerId)) {
                viewer.hidePlayer(plugin, ghost);
            }
        }
    }

    Component statusMessage(Player player) {
        DashSession session = sessions.get(player.getUniqueId());
        if (session != null) {
            String phase = session.phase == DashSession.Phase.RECORDING ? "Ghost" : "Zeitraffer";
            return Component.text("GhostDash: " + phase + " aktiv, "
                    + String.format("%.1f", session.routeDistance) + " Blöcke aufgezeichnet.",
                    NamedTextColor.AQUA);
        }
        long remaining = cooldownUntil.getOrDefault(player.getUniqueId(), 0L) - System.currentTimeMillis();
        if (remaining > 0L) {
            return Component.text("GhostDash: Cooldown noch " + formatSeconds(remaining) + " Sekunden.",
                    NamedTextColor.YELLOW);
        }
        return Component.text("GhostDash ist bereit: Schleichen + F mit einem Lunge-Speer.",
                NamedTextColor.GREEN);
    }

    void message(Player player, String text) {
        player.sendActionBar(Component.text(text, NamedTextColor.GRAY));
    }

    void shutdown() {
        if (ticker != null) {
            ticker.cancel();
        }
        cancelAll(false, null);
        for (UUID avatarId : new HashSet<>(spawnedAvatars)) {
            Entity avatar = Bukkit.getEntity(avatarId);
            if (avatar != null) {
                avatar.remove();
            }
        }
        spawnedAvatars.clear();
    }

    private Mannequin spawnAvatar(Player player, Location location, String kind) {
        Mannequin avatar = location.getWorld().spawn(location, Mannequin.class, mannequin -> {
            mannequin.setProfile(ResolvableProfile.resolvableProfile()
                    .uuid(player.getUniqueId())
                    .name(player.getName())
                    .build());
            mannequin.setDescription(null);
            mannequin.setImmovable(true);
            mannequin.setGravity(false);
            mannequin.setSilent(true);
            mannequin.setPersistent(false);
            mannequin.setCollidable(false);
            mannequin.getPersistentDataContainer().set(avatarKindKey, PersistentDataType.STRING, kind);
            mannequin.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING,
                    player.getUniqueId().toString());
            copyEquipment(player, mannequin);
        });
        spawnedAvatars.add(avatar.getUniqueId());
        return avatar;
    }

    private void spawnAfterimage(Player player, Location location) {
        Mannequin afterimage = spawnAvatar(player, location, AFTERIMAGE);
        afterimage.setInvulnerable(true);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            spawnedAvatars.remove(afterimage.getUniqueId());
            if (afterimage.isValid()) {
                afterimage.remove();
            }
        }, settings.afterimageLifetimeTicks());
    }

    private static void copyEquipment(Player player, Mannequin mannequin) {
        EntityEquipment source = player.getEquipment();
        EntityEquipment target = mannequin.getEquipment();
        target.setArmorContents(source.getArmorContents());
        target.setItemInMainHand(source.getItemInMainHand());
        target.setItemInOffHand(source.getItemInOffHand());
    }

    private static boolean isSpear(ItemStack item) {
        return item != null && !item.getType().isAir() && Tag.ITEMS_SPEARS.isTagged(item.getType());
    }

    private static String formatSeconds(long millis) {
        return String.format("%.1f", Math.max(0L, millis) / 1000.0);
    }
}
