package com.tradebot.db;

import com.tradebot.enums.ChartMode;
import com.tradebot.model.MACDAlarm;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MACDAlarmDB {
	
	public static long createAlarm(MACDAlarm alarm) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		long alarm_id = 0;

		String query = "INSERT INTO MACD_ALARM (symbol, alarmId, initialDelay, delay, timeUnit, description,"
			   + " intervall, dema, currentEma, crosss, macdLine, signalLine, lastClosingCandle, minGap, chartMode) "
			   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try {
			ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

			ps.setString(1, alarm.getSymbol());
			ps.setString(2, alarm.getAlarmId());
			ps.setInt(3, alarm.getInitialDelay());
			ps.setInt(4, alarm.getDelay());
			ps.setInt(5, alarm.getTimeUnit().ordinal());
			ps.setString(6, alarm.getDescription());
			ps.setString(7, alarm.getIntervall());
			ps.setInt(8, alarm.getDema());
			ps.setDouble(9, alarm.getCurrentEma());
			ps.setBoolean(10, alarm.getCrosss());
			ps.setDouble(11, alarm.getMacdLine());
			ps.setDouble(12, alarm.getSignalLine());
			ps.setDouble(13, alarm.getLastClosingCandle());
			ps.setDouble(14, alarm.getMinGap());
			ps.setInt(15, alarm.getChartMode().ordinal());

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
	
	public static void editAlarm(MACDAlarm alarm) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "UPDATE MACD_ALARM SET symbol=?, alarmId=?, initialDelay=?, delay=?, timeUnit=?, description=?,"
			   + " intervall=?, dema=?, currentEma=?, crosss=?, macdLine=?, signalLine=?, lastClosingCandle=?, minGap=?, chartMode=?"
			   + "WHERE id = ?";
		try {
			ps = connection.prepareStatement(query);

			ps.setString(1, alarm.getSymbol());
			ps.setString(2, alarm.getAlarmId());
			ps.setInt(3, alarm.getInitialDelay());
			ps.setInt(4, alarm.getDelay());
			ps.setInt(5, alarm.getTimeUnit().ordinal());
			ps.setString(6, alarm.getDescription());
			ps.setString(7, alarm.getIntervall());
			ps.setInt(8, alarm.getDema());
			ps.setDouble(9, alarm.getCurrentEma());
			ps.setBoolean(10, alarm.getCrosss());
			ps.setDouble(11, alarm.getMacdLine());
			ps.setDouble(12, alarm.getSignalLine());
			ps.setDouble(13, alarm.getLastClosingCandle());
			ps.setDouble(14, alarm.getMinGap());
			ps.setInt(15, alarm.getChartMode().ordinal());
			ps.setLong(16, alarm.getId());

			ps.executeUpdate();

		} catch (SQLException e) {
			System.err.println(e);
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
	}
	
	public static MACDAlarm getOneAlarm(long id) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "SELECT * FROM MACD_ALARM where id=?";
		try {
			ps = connection.prepareStatement(query);
			ps.setLong(1, id);
			rs = ps.executeQuery();
			MACDAlarm alarm = new MACDAlarm();
			while (rs.next()) {
				alarm.setId(rs.getLong("id"));
				alarm.setSymbol(rs.getString("symbol"));
				alarm.setAlarmId(rs.getString("alarmId"));
				alarm.setInitialDelay(rs.getInt("initialDelay"));
				alarm.setDelay(rs.getInt("delay"));
				alarm.setTimeUnit(TimeUnit.values()[rs.getInt("timeUnit")]);
				alarm.setDescription(rs.getString("description"));
				alarm.setIntervall(rs.getString("intervall"));
				alarm.setDema(rs.getInt("dema"));
				alarm.setCurrentEma(rs.getDouble("currentEma"));
				alarm.setCrosss(rs.getBoolean("crosss"));
				alarm.setMacdLine(rs.getDouble("macdLine"));
				alarm.setSignalLine(rs.getDouble("signalLine"));				
				alarm.setLastClosingCandle(rs.getDouble("lastClosingCandle"));
				alarm.setMinGap(rs.getDouble("minGap"));
				alarm.setChartMode(ChartMode.values()[rs.getInt("chartMode")]);
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
	
	public static MACDAlarm getOneAlarm(String taskId) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "SELECT * FROM MACD_ALARM where alarmId=?";
		try {
			ps = connection.prepareStatement(query);
			ps.setString(1, taskId);
			rs = ps.executeQuery();
			MACDAlarm alarm = new MACDAlarm();
			while (rs.next()) {
				alarm.setId(rs.getLong("id"));
				alarm.setSymbol(rs.getString("symbol"));
				alarm.setAlarmId(rs.getString("alarmId"));
				alarm.setInitialDelay(rs.getInt("initialDelay"));
				alarm.setDelay(rs.getInt("delay"));
				alarm.setTimeUnit(TimeUnit.values()[rs.getInt("timeUnit")]);
				alarm.setDescription(rs.getString("description"));
				alarm.setIntervall(rs.getString("intervall"));
				alarm.setDema(rs.getInt("dema"));
				alarm.setCurrentEma(rs.getDouble("currentEma"));
				alarm.setCrosss(rs.getBoolean("crosss"));
				alarm.setMacdLine(rs.getDouble("macdLine"));
				alarm.setSignalLine(rs.getDouble("signalLine"));
				alarm.setLastClosingCandle(rs.getDouble("lastClosingCandle"));
				alarm.setMinGap(rs.getDouble("minGap"));
				alarm.setChartMode(ChartMode.values()[rs.getInt("chartMode")]);
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
	
	public static List<MACDAlarm> getAllAlarms() throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "SELECT * FROM MACD_ALARM";
		try {
			ps = connection.prepareStatement(query);
			rs = ps.executeQuery();
			List<MACDAlarm> alarms = new ArrayList<>();
			while (rs.next()) {
				MACDAlarm alarm = new MACDAlarm();
				alarm.setId(rs.getLong("id"));
				alarm.setSymbol(rs.getString("symbol"));
				alarm.setAlarmId(rs.getString("alarmId"));
				alarm.setInitialDelay(rs.getInt("initialDelay"));
				alarm.setDelay(rs.getInt("delay"));
				alarm.setTimeUnit(TimeUnit.values()[rs.getInt("timeUnit")]);
				alarm.setDescription(rs.getString("description"));
				alarm.setIntervall(rs.getString("intervall"));
				alarm.setDema(rs.getInt("dema"));
				alarm.setCurrentEma(rs.getDouble("currentEma"));
				alarm.setCrosss(rs.getBoolean("crosss"));
				alarm.setMacdLine(rs.getDouble("macdLine"));
				alarm.setSignalLine(rs.getDouble("signalLine"));
				alarm.setLastClosingCandle(rs.getDouble("lastClosingCandle"));
				alarm.setMinGap(rs.getDouble("minGap"));
				alarm.setChartMode(ChartMode.values()[rs.getInt("chartMode")]);
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
	
}
