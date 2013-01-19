package me.tehbeard.BeardStat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import me.tehbeard.BeardStat.listeners.MetaDataCapture;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.material.Dye;
import org.bukkit.material.Leaves;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Sandstone;
import org.bukkit.material.SmoothBrick;
import org.bukkit.material.Step;
import org.bukkit.material.Tree;
import org.bukkit.material.WoodenStep;
import org.bukkit.material.Wool;
import org.bukkit.potion.PotionType;

public class MaterialListOutput {

	private static Properties materialOutputList = new Properties() {
		@Override
		public Set<Object> keySet(){
			return Collections.unmodifiableSet(new TreeSet<Object>(super.keySet()));
		}

		@Override
		public synchronized Enumeration<Object> keys() {
			return Collections.enumeration(new TreeSet<Object>(super.keySet()));
		}
	};

	private static final String DEFAULT_SUFFIX = ", %";

	public static void addIfNotFound(String base,String name,String defaultEntry){
		if(!materialOutputList.containsKey(base +"." + name)){
			materialOutputList.put(
					base +"." + name,
					defaultEntry + DEFAULT_SUFFIX
					);
			//System.out.println("NEW ENTRY : " + base +"." + name + " = " + defaultEntry + DEFAULT_SUFFIX);
		}
	}


	private static Map<Class<? extends MaterialData>,Method> readers = new HashMap<Class<? extends MaterialData>, Method>();

	static{
		try {
			readers.put(Dye.class, Dye.class.getMethod("getColor"));
			readers.put(Wool.class, Wool.class.getMethod("getColor"));
			readers.put(Leaves.class, Leaves.class.getMethod("getSpecies"));
			readers.put(Tree.class, Tree.class.getMethod("getSpecies"));
			readers.put(WoodenStep.class, WoodenStep.class.getMethod("getSpecies"));
			readers.put(Step.class, Step.class.getMethod("getMaterial"));
			readers.put(SmoothBrick.class, SmoothBrick.class.getMethod("getMaterial"));
			readers.put(Sandstone.class,Sandstone.class.getMethod("getType"));
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private static String getDataBasedName(Material m,byte data){
		MaterialData md = m.getNewData(data);
		try {
			Method method = readers.get(md.getClass());
			if(method == null){return "";}

			//System.out.println(m + "" + data);
			return method.invoke(md,null).toString();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch(NullPointerException e){
			return "";
		}
		return null;
	}

	public static void main(String[] args) throws FileNotFoundException, IOException{


		if(args.length == 1){
			materialOutputList.load(new FileReader(new File(args[0])));
		}

		addIfNotFound("item","inksack", "dye");

		//Blocks and items
		for(Material m : Material.values()){
			if(m.isBlock()){
				addIfNotFound("block",m.toString().replace("_", "").toLowerCase(),m.toString().replace("_", " ").toLowerCase());
			}else{
				addIfNotFound("item",m.toString().replace("_", "").toLowerCase(),m.toString().replace("_", " ").toLowerCase());
			}
		}


		//Mobs and Entities
		for(EntityType m : EntityType.values()){
			if(m == EntityType.COMPLEX_PART){continue;}

			if(m.isAlive()){
				addIfNotFound("mob",m.toString().replace("_", "").toLowerCase(),m.toString().replace("_", " ").toLowerCase());
			}
			else{
				addIfNotFound("entity",m.toString().replace("_", "").toLowerCase(),m.toString().replace("_", " ").toLowerCase());
			}
		}

		//potion types
		for(PotionType m : PotionType.values()){
			addIfNotFound("potionType",m.toString().replace("_", "").toLowerCase(),m.toString().replace("_", " ").toLowerCase());
		}

		//damage causes
		for(DamageCause m : DamageCause.values()){
			addIfNotFound("damage",m.toString().replace("_", "").toLowerCase(),m.toString().replace("_", " ").toLowerCase());
		}

		//health regain reason
		for(RegainReason m : RegainReason.values()){
			addIfNotFound("regenreason",m.toString().replace("_", "").toLowerCase(),m.toString().replace("_", " ").toLowerCase());
		}

		//metadata processing
		HashSet<String> lines = new HashSet<String>();
		for(Entry<Material, Integer> entry  : MetaDataCapture.mats.entrySet()){

			Material m = entry.getKey();
			int mask = entry.getValue();

			String base = m.isBlock() ? "block":"item";

			String def = materialOutputList.getProperty(base + "." + m.toString().replace("_", "").toLowerCase()).split(",")[0];

			for(int i = 0;i<16;i++){
				String s = m.toString().toLowerCase().replace("_","") + "_" + (i & mask);
				if(!lines.contains(s)){
					lines.add(s);
					String prefix = getDataBasedName(m,(byte)i).replace("_", " ").toLowerCase();
					addIfNotFound(base,s,prefix + " " + def);
				}
			}
		}

		materialOutputList.store(System.out, "BeardStat data mapping");


	}

}
