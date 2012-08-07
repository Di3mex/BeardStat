package me.tehbeard.BeardStat.listeners;

import java.util.HashMap;
import java.util.Map;

import me.tehbeard.BeardStat.containers.PlayerStatBlob;

import org.bukkit.Material;

/**
 * translates a material to the metadata to capture.
 * 
 * Current materials
 * Wood
 * log
 * sapling
 * dye
 * coal
 * slab
 * wool
 * stone brick
 * @author james
 *
 */
public class MetaDataCapture {

    //private static Material[] mats = {Material.WOOD,Material.LOG,Material.SAPLING,Material.INK_SACK,Material.COAL,Material.STEP,Material.WOOL,Material.SMOOTH_BRICK};

    private static Map<Material,Integer> mats = new HashMap<Material, Integer>();

    static{
        //nature
        mats.put(Material.WOOD            ,0xF);
        mats.put(Material.LOG             ,0x3);
        mats.put(Material.LEAVES          ,0x3);
        mats.put(Material.SAPLING         ,0x3);
        mats.put(Material.DEAD_BUSH       ,0x3);

        //ART
        mats.put(Material.INK_SACK        ,0xF);
        mats.put(Material.WOOL            ,0xF);

        //INDUSTRY
        mats.put(Material.COAL            ,0x1);

        //CONSTRUCTION
        mats.put(Material.STEP            ,0x7);
        mats.put(Material.DOUBLE_STEP     ,0x7);
        mats.put(Material.WOOD_STEP       ,0x3);
        mats.put(Material.WOOD_DOUBLE_STEP,0x3);

        //STRONGBADS STRONGHOLD
        mats.put(Material.SMOOTH_BRICK    ,0x3);
        mats.put(Material.MONSTER_EGGS    ,0x3);

        //EYPGT
        mats.put(Material.SANDSTONE       ,0x3);
    }


    public static void saveMetaDataStat(PlayerStatBlob blob,String category,Material material,int dataValue,int value){
        String matName = material.toString().toLowerCase().replace("_","");
        if(mats.containsKey(material)){
            String tag = "_" + (dataValue & mats.get(material));
            blob.getStat(category, matName + tag);
        }
    }


    /*public static boolean hasMetaData(Material mat){
        for(Material i : mats){
            if(i == mat){
                return true;
            }
        }
        return false;

    }*/
    

}
