package me.tehbeard.BeardStat.listeners;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ShapedRecipe;
 
import me.tehbeard.BeardStat.containers.PlayerStatManager;

public class StatCraftListener implements Listener {

    List<String> worlds;
    private PlayerStatManager playerStatManager;


    public StatCraftListener(List<String> worlds,
            PlayerStatManager playerStatManager) {
        this.worlds = worlds;
        this.playerStatManager = playerStatManager;
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onCraftItem(CraftItemEvent event){
        if(!worlds.contains(event.getWhoClicked().getWorld().getName())){
            String item = event.getRecipe().getResult().getType().toString().toLowerCase().replace("_","");
            int amount = event.getRecipe().getResult().getAmount();
            
            if(event.isShiftClick()){
                event.getInventory().getMatrix();

            }
            
            Player p = (Player)event.getWhoClicked();
            
            /**
             * if MetaDataable, make the item string correct
             */
            if(MetaDataCapture.hasMetaData(event.getRecipe().getResult().getType())){
                item = 
                        event.getRecipe().getResult().getType().toString().toLowerCase().replace("_","") + 
                        "_" + event.getRecipe().getResult().getDurability();
                
            }
            
            playerStatManager.getPlayerBlob(p.getName()).getStat("crafting", item).incrementStat(amount);
        }
    }
    
    
}
