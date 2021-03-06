package com.tehbeard.BeardStat.listeners;

import java.util.List;

import net.dragonzone.promise.Promise;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.vehicle.VehicleMoveEvent;

import com.tehbeard.BeardStat.BeardStat;
import com.tehbeard.BeardStat.containers.EntityStatBlob;
import com.tehbeard.BeardStat.containers.PlayerStatManager;
import com.tehbeard.BeardStat.listeners.defer.DelegateIncrement;

public class StatVehicleListener extends StatListener {

    public StatVehicleListener(List<String> worlds, PlayerStatManager playerStatManager, BeardStat plugin) {
        super(worlds, playerStatManager, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleMove(VehicleMoveEvent event) {
        if (isBlacklistedWorld(event.getVehicle().getWorld())) {
            return;
        }

        if (((event.getTo().getBlockX() != event.getFrom().getBlockX())
                || (event.getTo().getBlockY() != event.getFrom().getBlockY()) || (event.getTo().getBlockZ() != event
                .getFrom().getBlockZ()))) {

            Location from, to;
            Player player = (event.getVehicle().getPassenger() instanceof Player ? (Player) event.getVehicle()
                    .getPassenger() : null);
            if (player == null) {
                return;
            }

            if (!shouldTrackPlayer(player)) {
                return;
            }

            from = event.getFrom();
            to = event.getTo();
            getPlugin().printDebugCon("Vehicle move fired!");
            if (from.getWorld().equals(to.getWorld())) {
                if (from.distance(to) < 10) {
                    Promise<EntityStatBlob> promiseblob = this.getPlayerStatManager().getPlayerBlobASync(
                            player.getName());
                    promiseblob.onResolve(new DelegateIncrement(BeardStat.DEFAULT_DOMAIN, to.getWorld().getName(),
                            "vehicle", event.getVehicle().getType().toString().toLowerCase().replace("_", ""),
                            (int) Math.ceil(from.distance(to))));

                }
            }
        }

    }
}
