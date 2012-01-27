package me.tehbeard.BeardStat.listeners;

import java.util.List;

import me.tehbeard.BeardStat.containers.PlayerStatManager;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;


public class StatBlockListener implements Listener{

	List<String> worlds;


	public StatBlockListener(List<String> worlds){
		this.worlds = worlds;
	}


	@EventHandler(priority=EventPriority.MONITOR)
	public void onBlockPlace(BlockPlaceEvent event) {
		if(event.isCancelled()==false && !worlds.contains(event.getPlayer().getWorld().getName())){
			PlayerStatManager.getPlayerBlob(event.getPlayer().getName()).getStat("blockcreate",event.getBlock().getType().toString().toLowerCase().replace("_","")).incrementStat(1);
			PlayerStatManager.getPlayerBlob(event.getPlayer().getName()).getStat("stats","totalblockcreate").incrementStat(1);
		}
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onBlockBreak(BlockBreakEvent event) {
		if(event.isCancelled()==false && !worlds.contains(event.getPlayer().getWorld().getName())){
			PlayerStatManager.getPlayerBlob(event.getPlayer().getName()).getStat("stats","totalblockdestroy").incrementStat(1);
			PlayerStatManager.getPlayerBlob(event.getPlayer().getName()).getStat("blockdestroy",event.getBlock().getType().toString().toLowerCase().replace("_","")).incrementStat(1);
		}
	}

	

}
