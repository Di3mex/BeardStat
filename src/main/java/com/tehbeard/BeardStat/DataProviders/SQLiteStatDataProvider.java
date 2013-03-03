package com.tehbeard.BeardStat.DataProviders;

import java.sql.SQLException;

public class SQLiteStatDataProvider extends JDBCStatDataProvider {

	public SQLiteStatDataProvider(String filename) throws SQLException {
		
		super("sqlite","org.sqlite.JDBC");
		
		tblConfig.put("TBL_ENTITY", "entity");
		tblConfig.put("TBL_KEYSTORE","keystore");
		
		connectionUrl = String.format("jdbc:sqlite:%s",filename);
		
		initialise();
		
		
	}

}
