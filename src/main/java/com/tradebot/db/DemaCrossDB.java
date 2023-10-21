package com.tradebot.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DemaCrossDB {
	
	public static boolean getCross() throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean cross = false;

		String query = "SELECT CROSSS FROM DEMACROSS";
		
		try {
			ps = connection.prepareStatement(query);
			rs = ps.executeQuery();
			while (rs.next()) {
				cross = rs.getBoolean("crosss");
			}
			return cross;
		} catch (SQLException e) {
			System.err.println(e);
			return false;
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
	}
	
	public static void updateCross(boolean newValue) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "UPDATE DEMACROSS SET CROSSS=?";

		try {
			ps = connection.prepareStatement(query);
			
			ps.setBoolean(1, newValue);			
			ps.executeUpdate();

		} catch (SQLException e) {
			System.err.println(e);
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
	}	
}
