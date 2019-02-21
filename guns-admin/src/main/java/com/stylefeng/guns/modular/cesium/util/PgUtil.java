package com.stylefeng.guns.modular.cesium.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class PgUtil {
	private static Connection conn;
	 

	public static Connection getConn(){
		try {
			Class.forName("org.postgresql.Driver");
			String url = "jdbc:postgresql://localhost:5432/shanghai";
			conn =DriverManager.getConnection(url, "postgres", "123456");	
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return conn;
	
	}
	
	public static void destroy() throws SQLException {
		if (conn != null) {
			conn.close();
		}
	}
	public static void executeSql(String sql) {
		Statement stmt;
		try {
			stmt = PgUtil.getConn().createStatement();
			stmt.execute(sql);
			stmt.close();
			//PgUtil.getConn().close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
