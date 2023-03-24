package com.tradebot.db;

import com.tradebot.model.ErrorTracker;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ErrorTrackerDB {

	public static void addError(ErrorTracker errorTracker) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "INSERT INTO ERROR_TRACKER (errorTimestamp, errorMessage, acknowledged, tradebot_id)"
				+ "VALUES (?, ?, ?, ?)";
		try {
			ps = connection.prepareStatement(query);
			
			ps.setTimestamp(1, Timestamp.valueOf(errorTracker.getErrorTimestamp()));
			ps.setString(2, errorTracker.getErrorMessage());
			ps.setBoolean(3, false);
			ps.setLong(4, errorTracker.getTradebot_id());
			
			ps.executeUpdate();
			
		} catch (SQLException e) {
			System.err.println(e);
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
	}

	public static void updateError(ErrorTracker errorTracker) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "UPDATE ERROR_TRACKER SET acknowledged=? WHERE id=?";

		try {
			ps = connection.prepareStatement(query);
			
			ps.setBoolean(1, true);
			ps.setLong(2, errorTracker.getId());
			
			ps.executeUpdate();

		} catch (SQLException e) {
			System.err.println(e);
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
	}

	public static List<ErrorTracker> getTradeBotErrors(long botId, boolean onlyUnacknowledged) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		String query = "SELECT * FROM ERROR_TRACKER where tradebot_id=?";
		String queryOnlyUnacknowledged = "SELECT * FROM ERROR_TRACKER where tradebot_id=? AND acknowledged=false";
		
		try {
			ps = connection.prepareStatement(onlyUnacknowledged ? queryOnlyUnacknowledged : query);
			ps.setLong(1, botId);
			rs = ps.executeQuery();
			
			List<ErrorTracker> errorTrackers = new ArrayList<>();
			
			while (rs.next()) {
				ErrorTracker errorTracker = new ErrorTracker();
				errorTracker.setId(rs.getLong("id"));
				errorTracker.setErrorTimestamp(rs.getTimestamp("errorTimestamp").toLocalDateTime());
				errorTracker.setErrorMessage(rs.getString("errorMessage"));
				errorTracker.setAcknowledged(rs.getBoolean("acknowledged"));
				errorTracker.setTradebot_id(rs.getLong("tradebot_id"));
				errorTrackers.add(errorTracker);
			}
			return errorTrackers;
		} catch (SQLException e) {
			System.err.println(e);
			return null;
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
	}
}
