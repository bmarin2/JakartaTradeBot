package com.tradebot.db;

import com.tradebot.enums.ChartMode;
import com.tradebot.enums.FutresDemaStrategy;
import com.tradebot.model.FuturesBot;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FuturesBotDB {
	
	public static long addFuturesBot(FuturesBot bot) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		long order_id = 0;

		String query = "INSERT INTO FUTURES_BOT (symbol, createdDate, taskId, quantity, description, initialDelay, "
			   + "delay, timeUnit, stopLoss, takeProfit, demaAlertTaskId, futresDemaStrategy, chartMode) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try {
			ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
			
			ps.setString(1, bot.getSymbol());
			ps.setDate(2, new java.sql.Date(bot.getCreatedDate().getTime()));
			ps.setString(3, bot.getTaskId());
			ps.setDouble(4, bot.getQuantity());
			ps.setString(5, bot.getDescription());
			ps.setInt(6, bot.getInitialDelay());
			ps.setInt(7, bot.getDelay());
			ps.setInt(8, bot.getTimeUnit().ordinal());
			ps.setDouble(9, bot.getStopLoss());
			ps.setDouble(10, bot.getTakeProfit());
			ps.setString(11, bot.getDemaAlertTaskId());
			ps.setInt(12, bot.getFutresDemaStrategy().ordinal());
			ps.setInt(13, bot.getChartMode().ordinal());
			
			ps.executeUpdate();
			
			rs = ps.getGeneratedKeys();

			if (rs.next()) {
				order_id = rs.getLong(1);
			}
			
		} catch (SQLException e) {
			System.err.println(e);
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
		return order_id;
	}
	
	public static void updateFuturesBot(FuturesBot bot) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "UPDATE FUTURES_BOT SET symbol=?, createdDate=?, taskId=?, quantity=?, description=?, initialDelay=?,"
			   + " delay=?, timeUnit=?, stopLoss=?, takeProfit=?, demaAlertTaskId=?, futresDemaStrategy=?, chartMode=? WHERE id=?";

		try {
			ps = connection.prepareStatement(query);
			
			ps.setString(1, bot.getSymbol());
			ps.setDate(2, new java.sql.Date(bot.getCreatedDate().getTime()));
			ps.setString(3, bot.getTaskId());
			ps.setDouble(4, bot.getQuantity());
			ps.setString(5, bot.getDescription());
			ps.setInt(6, bot.getInitialDelay());
			ps.setInt(7, bot.getDelay());
			ps.setInt(8, bot.getTimeUnit().ordinal());
			ps.setDouble(9, bot.getStopLoss());
			ps.setDouble(10, bot.getTakeProfit());
			ps.setString(11, bot.getDemaAlertTaskId());
			ps.setInt(12, bot.getFutresDemaStrategy().ordinal());
			ps.setInt(13, bot.getChartMode().ordinal());
			ps.setLong(14, bot.getId());
			
			ps.executeUpdate();

		} catch (SQLException e) {
			System.err.println(e);
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
	}
	
	public static List<FuturesBot> getAllFuturesBots() throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "SELECT * FROM FUTURES_BOT";
		try {
			ps = connection.prepareStatement(query);
			rs = ps.executeQuery();
			List<FuturesBot> bots = new ArrayList<>();
			while (rs.next()) {
				FuturesBot bot = new FuturesBot();
				bot.setId(rs.getLong("id"));
				bot.setSymbol(rs.getString("symbol"));
				bot.setCreatedDate(rs.getDate("createdDate"));
				bot.setTaskId(rs.getString("taskId"));
				bot.setQuantity(rs.getDouble("quantity"));
				bot.setDescription(rs.getString("description"));
				bot.setInitialDelay(rs.getInt("initialDelay"));
				bot.setDelay(rs.getInt("delay"));
				bot.setTimeUnit(TimeUnit.values()[rs.getInt("timeUnit")]);
				bot.setStopLoss(rs.getDouble("stopLoss"));
				bot.setTakeProfit(rs.getDouble("takeProfit"));
				bot.setDemaAlertTaskId(rs.getString("demaAlertTaskId"));
				bot.setFutresDemaStrategy(FutresDemaStrategy.values()[rs.getInt("futresDemaStrategy")]);
				bot.setChartMode(ChartMode.values()[rs.getInt("chartMode")]);
				bots.add(bot);
			}
			return bots;
		} catch (SQLException e) {
			System.err.println(e);
			return null;
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
	}

	public static FuturesBot getOneFuturesBot(long id) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "SELECT * FROM FUTURES_BOT where id=?";
		try {
			ps = connection.prepareStatement(query);
			ps.setLong(1, id);
			rs = ps.executeQuery();
			FuturesBot bot = new FuturesBot();
			while (rs.next()) {
				bot.setId(rs.getLong("id"));
				bot.setSymbol(rs.getString("symbol"));
				bot.setCreatedDate(rs.getDate("createdDate"));
				bot.setTaskId(rs.getString("taskId"));
				bot.setQuantity(rs.getDouble("quantity"));
				bot.setDescription(rs.getString("description"));
				bot.setInitialDelay(rs.getInt("initialDelay"));
				bot.setDelay(rs.getInt("delay"));
				bot.setTimeUnit(TimeUnit.values()[rs.getInt("timeUnit")]);
				bot.setStopLoss(rs.getDouble("stopLoss"));
				bot.setTakeProfit(rs.getDouble("takeProfit"));
				bot.setDemaAlertTaskId(rs.getString("demaAlertTaskId"));
				bot.setFutresDemaStrategy(FutresDemaStrategy.values()[rs.getInt("futresDemaStrategy")]);
				bot.setChartMode(ChartMode.values()[rs.getInt("chartMode")]);
			}
			return bot;
		} catch (SQLException e) {
			System.err.println(e);
			return null;
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
	}
	
	public static FuturesBot getOneTradeBot(String taskId) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "SELECT * FROM FUTURES_BOT where taskId=?";
		try {
			ps = connection.prepareStatement(query);
			ps.setString(1, taskId);
			rs = ps.executeQuery();
			FuturesBot bot = new FuturesBot();
			while (rs.next()) {
				bot.setId(rs.getLong("id"));
				bot.setSymbol(rs.getString("symbol"));
				bot.setCreatedDate(rs.getDate("createdDate"));
				bot.setTaskId(rs.getString("taskId"));
				bot.setQuantity(rs.getDouble("quantity"));
				bot.setDescription(rs.getString("description"));
				bot.setInitialDelay(rs.getInt("initialDelay"));
				bot.setDelay(rs.getInt("delay"));
				bot.setTimeUnit(TimeUnit.values()[rs.getInt("timeUnit")]);
				bot.setStopLoss(rs.getDouble("stopLoss"));
				bot.setTakeProfit(rs.getDouble("takeProfit"));
				bot.setDemaAlertTaskId(rs.getString("demaAlertTaskId"));
				bot.setFutresDemaStrategy(FutresDemaStrategy.values()[rs.getInt("futresDemaStrategy")]);
				bot.setChartMode(ChartMode.values()[rs.getInt("chartMode")]);
			}
			return bot;
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
