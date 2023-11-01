package com.tradebot.db;

import com.tradebot.model.Alarm;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AlarmDB {
	public static long createAlarm(Alarm alarm) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		long alarm_id = 0;

		String query = "INSERT INTO ALARM (symbol, alarmId, alarmPrice, initialDelay, delay, timeUnit, description, msgSent, intervall, firstDema, secondDema, thirdDema, crosss) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try {
			ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
			
			ps.setString(1, alarm.getSymbol());
			ps.setString(2, alarm.getAlarmId());			
			ps.setBigDecimal(3, alarm.getAlarmPrice());
			ps.setInt(4, alarm.getInitialDelay());
			ps.setInt(5, alarm.getDelay());
			ps.setInt(6, alarm.getTimeUnit().ordinal());
			ps.setString(7, alarm.getDescription());
			ps.setBoolean(8, alarm.getMsgSent());
			ps.setString(9, alarm.getIntervall());
			ps.setInt(10, alarm.getFirstDema());
			ps.setInt(11, alarm.getSecondDema());
			ps.setInt(12, alarm.getThirdDema());
			ps.setBoolean(13, alarm.getCrosss());
			
			ps.executeUpdate();
			
			rs = ps.getGeneratedKeys();

			if (rs.next()) {
				alarm_id = rs.getLong(1);
			}
			
		} catch (SQLException e) {
			System.err.println(e);
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
		return alarm_id;
	}
	
	public static Alarm getOneAlarm(long id) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "SELECT * FROM ALARM where id=?";
		try {
			ps = connection.prepareStatement(query);
			ps.setLong(1, id);
			rs = ps.executeQuery();
			Alarm alarm = new Alarm();
			while (rs.next()) {
				alarm.setId(rs.getLong("id"));
				alarm.setSymbol(rs.getString("symbol"));
				alarm.setAlarmId(rs.getString("alarmId"));
				alarm.setAlarmPrice(rs.getBigDecimal("alarmPrice"));
				alarm.setInitialDelay(rs.getInt("initialDelay"));
				alarm.setDelay(rs.getInt("delay"));
				alarm.setTimeUnit(TimeUnit.values()[rs.getInt("timeUnit")]);
				alarm.setDescription(rs.getString("description"));
				alarm.setMsgSent(rs.getBoolean("msgSent"));
				alarm.setIntervall(rs.getString("intervall"));
				alarm.setFirstDema(rs.getInt("firstDema"));
				alarm.setSecondDema(rs.getInt("secondDema"));
				alarm.setThirdDema(rs.getInt("thirdDema"));
				alarm.setCrosss(rs.getBoolean("crosss"));
			}
			return alarm;
		} catch (SQLException e) {
			System.err.println(e);
			return null;
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
	}
	
	public static Alarm getOneAlarm(String taskId) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "SELECT * FROM ALARM where alarmId=?";
		try {
			ps = connection.prepareStatement(query);
			ps.setString(1, taskId);
			rs = ps.executeQuery();
			Alarm alarm = new Alarm();
			while (rs.next()) {
				alarm.setId(rs.getLong("id"));
				alarm.setSymbol(rs.getString("symbol"));
				alarm.setAlarmId(rs.getString("alarmId"));
				alarm.setAlarmPrice(rs.getBigDecimal("alarmPrice"));
				alarm.setInitialDelay(rs.getInt("initialDelay"));
				alarm.setDelay(rs.getInt("delay"));
				alarm.setTimeUnit(TimeUnit.values()[rs.getInt("timeUnit")]);
				alarm.setDescription(rs.getString("description"));
				alarm.setMsgSent(rs.getBoolean("msgSent"));
				alarm.setIntervall(rs.getString("intervall"));
				alarm.setFirstDema(rs.getInt("firstDema"));
				alarm.setSecondDema(rs.getInt("secondDema"));
				alarm.setThirdDema(rs.getInt("thirdDema"));
				alarm.setCrosss(rs.getBoolean("crosss"));
			}
			return alarm;
		} catch (SQLException e) {
			System.err.println(e);
			return null;
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
	}
	
	public static List<Alarm> getAllAlarms() throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "SELECT * FROM ALARM";
		try {
			ps = connection.prepareStatement(query);
			rs = ps.executeQuery();
			List<Alarm> alarms = new ArrayList<>();
			while (rs.next()) {
				Alarm alarm = new Alarm();
				alarm.setId(rs.getLong("id"));
				alarm.setSymbol(rs.getString("symbol"));
				alarm.setAlarmId(rs.getString("alarmId"));
				alarm.setAlarmPrice(rs.getBigDecimal("alarmPrice"));
				alarm.setInitialDelay(rs.getInt("initialDelay"));
				alarm.setDelay(rs.getInt("delay"));
				alarm.setTimeUnit(TimeUnit.values()[rs.getInt("timeUnit")]);
				alarm.setDescription(rs.getString("description"));
				alarm.setMsgSent(rs.getBoolean("msgSent"));
				alarm.setIntervall(rs.getString("intervall"));
				alarm.setFirstDema(rs.getInt("firstDema"));
				alarm.setSecondDema(rs.getInt("secondDema"));
				alarm.setThirdDema(rs.getInt("thirdDema"));
				alarm.setCrosss(rs.getBoolean("crosss"));
				alarms.add(alarm);
			}
			return alarms;
		} catch (SQLException e) {
			System.err.println(e);
			return null;
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
	}
	
	public static void editAlarm(Alarm alarm) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "UPDATE ALARM SET symbol=?, alarmId=?, alarmPrice=?, initialDelay=?, delay=?, timeUnit=?, description=?, "
			   + "msgSent=?, intervall=?, firstDema=?, secondDema=?, thirdDema=?, crosss=? WHERE id = ?";

		try {
			ps = connection.prepareStatement(query);
			
			ps.setString(1, alarm.getSymbol());
			ps.setString(2, alarm.getAlarmId());			
			ps.setBigDecimal(3, alarm.getAlarmPrice());
			ps.setInt(4, alarm.getInitialDelay());
			ps.setInt(5, alarm.getDelay());
			ps.setInt(6, alarm.getTimeUnit().ordinal());
			ps.setString(7, alarm.getDescription());
			ps.setBoolean(8, alarm.getMsgSent());
			ps.setString(9, alarm.getIntervall());
			ps.setInt(10, alarm.getFirstDema());
			ps.setInt(11, alarm.getSecondDema());
			ps.setInt(12, alarm.getThirdDema());
			ps.setBoolean(13, alarm.getCrosss());
			ps.setLong(14, alarm.getId());
			
			ps.executeUpdate();

		} catch (SQLException e) {
			System.err.println(e);
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
	}
	
	public static void markMessageSent(long id) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "UPDATE ALARM SET msgSent=true WHERE id = ?";

		try {
			ps = connection.prepareStatement(query);
			
			ps.setLong(1, id);
			
			ps.executeUpdate();

		} catch (SQLException e) {
			System.err.println(e);
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
	}
	
	public static void markMessageUnSent(String alarmId) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "UPDATE ALARM SET msgSent=false WHERE alarmId = ?";

		try {
			ps = connection.prepareStatement(query);
			
			ps.setString(1, alarmId);
			
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
