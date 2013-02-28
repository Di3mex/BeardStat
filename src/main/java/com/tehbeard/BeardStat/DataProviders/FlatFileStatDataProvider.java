package com.tehbeard.BeardStat.DataProviders;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import net.dragonzone.promise.Deferred;
import net.dragonzone.promise.Promise;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.tehbeard.BeardStat.BeardStat;
import com.tehbeard.BeardStat.containers.PlayerStat;
import com.tehbeard.BeardStat.containers.PlayerStatBlob;


/**
 * YAML data storage provider for stats
 * @author James
 *
 */
public class FlatFileStatDataProvider implements IStatDataProvider {

	YamlConfiguration database;


	File file;
	public FlatFileStatDataProvider(File file) {
		database = YamlConfiguration.loadConfiguration(file);
		this.file = file;
		BeardStat.printCon("Creating FlatFile DataProvider");


	}


	public Promise<PlayerStatBlob> pullPlayerStatBlob(String player) {
		return pullPlayerStatBlob(player,true);
	}

	public  Promise<PlayerStatBlob> pullPlayerStatBlob(final String player,final boolean create) {
		BeardStat.printDebugCon("Loading stats for player " + player);

		try{

			final Deferred<PlayerStatBlob> promise = new Deferred<PlayerStatBlob>();


			PlayerStatBlob blob = new PlayerStatBlob(player,"");
			ConfigurationSection pl = database.getConfigurationSection("stats.players." + player);
			if(pl!=null){
				for(String key : pl.getKeys(false)){
					PlayerStat ps = blob.getStat(key.split("\\-")[0],key.split("\\-")[1]);
					ps.setValue(pl.getInt(key, 0));
					ps.clearArchive();
				}
				promise.resolve(blob);
			}
			else if(pl==null && create)
			{
				promise.resolve(blob);
			}
			promise.resolve(null);


			return promise;
		}
		catch(Exception e){
			e.printStackTrace();
			return new Deferred<PlayerStatBlob>(null);
		}
	}



	public void pushPlayerStatBlob(PlayerStatBlob player) {
		HashMap<String,Integer> nodes = new HashMap<String, Integer>();

		for(PlayerStat stat : player.getStats()){
			if(stat.isArchive()){
				nodes.put(stat.getCat() + "-" + stat.getStatistic(),stat.getValue());
			}
		}
		database.set("stats.players." + player.getName(), nodes);
		try {
			database.save(file);
		} catch (IOException e) {
			BeardStat.printCon("IO error occured when trying to save player data");
			e.printStackTrace();
		}
	}

	public void flush() {
		try {
			database.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void flushSync(){
		flush();
	}


	public void deletePlayerStatBlob(String player) {
		database.set("stats.players."+player, null);
		try {
			database.save(file);
		} catch (IOException e) {
			BeardStat.printCon("IO error occured when trying to clear player data");
			e.printStackTrace();
		}
	}


	public boolean hasStatBlob(String player) {
		return database.contains("stats.players."+player);
	}


	public List<String> getStatBlobsHeld() {
		return new ArrayList<String>(database.getConfigurationSection("stats.players").getKeys(false));
	}


}
