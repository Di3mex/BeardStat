package com.tehbeard.BeardStat.DataProviders;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import net.dragonzone.promise.Deferred;
import net.dragonzone.promise.Promise;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import com.tehbeard.BeardStat.BeardStat;
import com.tehbeard.BeardStat.NoRecordFoundException;
import com.tehbeard.BeardStat.containers.IStat;
import com.tehbeard.BeardStat.containers.EntityStatBlob;


/**
 * base class for JDBC based data providers
 * @author James
 *
 */
public abstract class JDBCStatDataProvider implements IStatDataProvider {

	protected Connection conn;

	//protected static PreparedStatement prepGetPlayerStat;

	//Load data from db
	protected PreparedStatement loadEntity;
	protected PreparedStatement loadEntityData;

	//save to db
	protected PreparedStatement saveEntity;
	protected PreparedStatement saveEntityDataNew;
	protected PreparedStatement saveEntityDataUpdate;
	

	//Maintenance
	protected PreparedStatement keepAlive;
	protected PreparedStatement listEntities;
	protected PreparedStatement deleteEntity;
	protected PreparedStatement createTable;


	private HashMap<String,EntityStatBlob> writeCache = new HashMap<String,EntityStatBlob>();

	protected String connectionUrl = "";
	protected Properties connectionProperties = new Properties();

	protected Map<String,String> tblConfig = new HashMap<String, String>();

	//private WorkQueue loadQueue = new WorkQueue(1);
	private ExecutorService loadQueue = Executors.newSingleThreadExecutor();

	private String type = "sql";
	public JDBCStatDataProvider(String type,String driverClass){
		this.type = type;
		try {
			Class.forName(driverClass);
		} catch (ClassNotFoundException e) {
			BeardStat.printCon("JDBC "+ driverClass + "Library not found!");
		}
	}

	protected void initialise(){
		createConnection();

		checkAndMakeTable();
		prepareStatements();
	}

	/**
	 * Connection to the database.
	 * @throws SQLException
	 */
	private void createConnection() {

		BeardStat.printCon("Connecting....");

		try {
			conn = DriverManager.getConnection(connectionUrl,connectionProperties);

			//conn.setAutoCommit(false);
		} catch (SQLException e) {
			BeardStat.mysqlError(e);
			conn = null;
		}


	}

	/**
	 * 
	 * @return
	 */
	private synchronized boolean checkConnection(){
		BeardStat.printDebugCon("Checking connection");
		try {
			if(conn == null || !conn.isValid(0)){
				BeardStat.printDebugCon("Something is derp, rebooting connection.");
				createConnection();
				if(conn!=null){
					BeardStat.printDebugCon("Rebuilding statements");
					prepareStatements();
				}
				else
				{
					BeardStat.printDebugCon("Reboot failed!");
				}

			}
		} catch (SQLException e) {
			conn = null;
			return false;
		}catch(AbstractMethodError e){
			
		}
		BeardStat.printDebugCon("Checking is " + conn != null ? "up" : "down");
		return conn != null;
	}

	protected void checkAndMakeTable(){
		BeardStat.printCon("Constructing table as needed.");

		try{

			String[] creates = BeardStat.self().readSQL(type,"sql/maintenence/create.tables", tblConfig).replaceAll("\n|\r", "").split(";");
			for(String sql : creates){
				BeardStat.printDebugCon("Creating: " + sql);
				conn.prepareStatement(sql).execute();
			}
		} catch (SQLException e) {
			BeardStat.mysqlError(e);
		}		
	}

	protected void prepareStatements(){
		try{
			BeardStat.printDebugCon("Preparing statements");

			loadEntity     = conn.prepareStatement(BeardStat.self().readSQL(type,"sql/load/getEntity", tblConfig));
			loadEntityData = conn.prepareStatement(BeardStat.self().readSQL(type,"sql/load/getEntityData", tblConfig));

			//save to db
			saveEntity     = conn.prepareStatement(BeardStat.self().readSQL(type,"sql/save/saveEntity", tblConfig),Statement.RETURN_GENERATED_KEYS);
			saveEntityDataNew = conn.prepareStatement(BeardStat.self().readSQL(type,"sql/save/saveStat", tblConfig));

			//Maintenance
			keepAlive      = conn.prepareStatement(BeardStat.self().readSQL(type,"sql/maintenence/keepAlive", tblConfig));
			listEntities   = conn.prepareStatement(BeardStat.self().readSQL(type,"sql/maintenence/listEntities", tblConfig));
			deleteEntity   = conn.prepareStatement(BeardStat.self().readSQL(type,"sql/maintenence/deletePlayerFully", tblConfig));


			BeardStat.printDebugCon("Set player stat statement created");
			BeardStat.printCon("Initaised MySQL Data Provider.");
		} catch (SQLException e) {
			BeardStat.mysqlError(e);
		}
	}



	public Promise<EntityStatBlob> pullPlayerStatBlob(String player) {
		return pullPlayerStatBlob(player,true);
	}

	public Promise<EntityStatBlob> pullPlayerStatBlob(final String player, final boolean create) {

		final Deferred<EntityStatBlob> promise = new Deferred<EntityStatBlob>();

		Runnable run = new Runnable() {

			public void run() {
				try {
					if(!checkConnection()){
						BeardStat.printCon("Database connection error!");
						promise.reject(new SQLException("Error connecting to database"));
						return;
					}
					long t1 = (new Date()).getTime();


					//Ok, try to get entity from database
					loadEntity.setString(1, player);
					loadEntity.setString(2,"player");//TODO: ALLOW CHOICE OF ENTITY TYPE

					ResultSet rs = loadEntity.executeQuery();
					EntityStatBlob pb = null;

					if(!rs.next()){
						//No player found! Let's create an entry for them!
						rs.close();
						rs = null;
						saveEntity.setString(1, player);
						saveEntity.setString(2,"player");
						saveEntity.executeUpdate();
						rs = saveEntity.getGeneratedKeys();//get dat key
						rs.next();

					}

					//make the player object, close out result set.
					pb = new EntityStatBlob(player,rs.getInt(1),"player");
					rs.close();
					rs = null;

					//load all stats data
					loadEntityData.setInt(1, pb.getEntityID());
					BeardStat.printDebugCon("executing " + loadEntityData);
					rs = loadEntityData.executeQuery();

					boolean foundStats = false;
					while(rs.next()){
						//`domain`,`world`,`category`,`statistic`,`value`
						IStat ps = pb.getStat(
								rs.getString(1),
								rs.getString(2),
								rs.getString(3),
								rs.getString(4)
								);
						ps.setValue(rs.getInt(5));
						ps.clearArchive();
						foundStats = true;
					}
					rs.close();

					BeardStat.printDebugCon("time taken to retrieve: "+((new Date()).getTime() - t1) +" Milliseconds");
					if(!foundStats && create==false){
						promise.reject(new NoRecordFoundException());
						return;}

					promise.resolve(pb);return;
				} catch (SQLException e) {
					BeardStat.mysqlError(e);
					promise.reject(e);
				}


			}
		};


		loadQueue.execute(run);

		return promise;

	}

	public void pushPlayerStatBlob(EntityStatBlob player) {

		synchronized (writeCache) {


			EntityStatBlob copy = player.cloneForArchive();



			if(!writeCache.containsKey(player.getName())){
				writeCache.put(player.getName(), copy);
			}
		}

	}

	private Runnable flush = new Runnable() {

		public void run() {
			synchronized (writeCache) {
				try {
					keepAlive.execute();
				} catch (SQLException e1) {
				}

				if(!checkConnection()){
					Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Could not restablish connection, will try again later, WARNING: CACHE WILL GROW WHILE THIS HAPPENS");
				}
				else{
					BeardStat.printDebugCon("Saving to database");
					for(Entry<String, EntityStatBlob> entry : writeCache.entrySet()){
						try {
							EntityStatBlob pb = entry.getValue();

							saveEntityDataNew.clearBatch();
							for(IStat stat : pb.getStats()){
								saveEntityDataNew.setInt(1, pb.getEntityID());
								saveEntityDataNew.setString(2,stat.getDomain());
								saveEntityDataNew.setString(3,stat.getWorld());
								saveEntityDataNew.setString(4,stat.getCategory());
								saveEntityDataNew.setString(5,stat.getStatistic());
								saveEntityDataNew.setInt(6,stat.getValue());
								try{
								saveEntityDataNew.setInt(7,stat.getValue());
								}
								catch(Exception e){
									//Catch sqlite error
								}
								saveEntityDataNew.addBatch();
							}
							saveEntityDataNew.executeBatch();

						} catch (SQLException e) {
							checkConnection();
						}
					}
					BeardStat.printDebugCon("Clearing write cache");
					writeCache.clear();
				}
			}

		}
	};

	public void flushSync(){
		BeardStat.printCon("Flushing in main thread! Game will lag!");
		flush.run();
		BeardStat.printCon("Flushed!");
	}

	public void flush() {

		new Thread(flush).start();
	}

	public void deletePlayerStatBlob(String player) {
		throw new UnsupportedOperationException();
	}

	public boolean hasStatBlob(String player) {
		try {
			loadEntity.clearParameters();
			loadEntity.setString(1,player);
			loadEntity.setString(2,"player");

			ResultSet rs = loadEntity.executeQuery();
			boolean found = rs.next();
			rs.close();
			return found;

		} catch (SQLException e) {
			checkConnection();
		}
		return false;
	}

	public List<String> getStatBlobsHeld() {
		List<String> list = new ArrayList<String>();
		try {
			listEntities.setString(1, "player");

			ResultSet rs = listEntities.executeQuery();
			while(rs.next()){
				list.add(rs.getString(1));
			}
			rs.close();

		} catch (SQLException e) {
			checkConnection();
		}
		return list;
	}
}