package com.tradebot.db;

import com.tradebot.model.TradeBot;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TradeBotDB {
	public static long addBot(TradeBot bot) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		long order_id = 0;

		String query = "INSERT INTO TRADE_BOT (symbol, createdDate, taskId, quoteOrderQty, cycleMaxOrders, orderStep, description,"
                  + "initialDelay, delay, timeUnit, stopLoss, demaAlertTaskId, enableStopLoss, profitBase, priceGridLimit) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try {
			ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
			
			ps.setString(1, bot.getSymbol());
			ps.setDate(2, new java.sql.Date(bot.getCreatedDate().getTime()));
			ps.setString(3, bot.getTaskId());
			ps.setInt(4, bot.getQuoteOrderQty());
			ps.setInt(5, bot.getCycleMaxOrders());
			ps.setDouble(6, bot.getOrderStep());
			ps.setString(7, bot.getDescription());
			ps.setInt(8, bot.getInitialDelay());
			ps.setInt(9, bot.getDelay());
			ps.setInt(10, bot.getTimeUnit().ordinal());
			ps.setDouble(11, bot.getStopLoss());
			ps.setString(12, bot.getDemaAlertTaskId());
               ps.setBoolean(13, bot.isEnableStopLoss());
               ps.setBoolean(14, bot.isProfitBase());
			ps.setDouble(15, bot.getPriceGridLimit());
			
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
	
	public static TradeBot getOneTradeBot(long id) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "SELECT * FROM TRADE_BOT where id=?";
		try {
			ps = connection.prepareStatement(query);
			ps.setLong(1, id);
			rs = ps.executeQuery();
			TradeBot bot = new TradeBot();
			while (rs.next()) {
				bot.setId(rs.getLong("id"));
				bot.setSymbol(rs.getString("symbol"));
				bot.setCreatedDate(rs.getDate("createdDate"));
				bot.setTaskId(rs.getString("taskId"));
				bot.setQuoteOrderQty(rs.getInt("quoteOrderQty"));
				bot.setCycleMaxOrders(rs.getInt("cycleMaxOrders"));
				bot.setOrderStep(rs.getDouble("orderStep"));
				bot.setDescription(rs.getString("description"));
				bot.setInitialDelay(rs.getInt("initialDelay"));
				bot.setDelay(rs.getInt("delay"));
				bot.setTimeUnit(TimeUnit.values()[rs.getInt("timeUnit")]);
				bot.setStopLoss(rs.getDouble("stopLoss"));
				bot.setDemaAlertTaskId(rs.getString("demaAlertTaskId"));
                    bot.setEnableStopLoss(rs.getBoolean("enableStopLoss"));
                    bot.setProfitBase(rs.getBoolean("profitBase"));
				bot.setPriceGridLimit(rs.getDouble("priceGridLimit"));
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
	
	public static TradeBot getOneTradeBot(String taskId) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "SELECT * FROM TRADE_BOT where taskId=?";
		try {
			ps = connection.prepareStatement(query);
			ps.setString(1, taskId);
			rs = ps.executeQuery();
			TradeBot bot = new TradeBot();
			while (rs.next()) {
				bot.setId(rs.getLong("id"));
				bot.setSymbol(rs.getString("symbol"));
				bot.setCreatedDate(rs.getDate("createdDate"));
				bot.setTaskId(rs.getString("taskId"));
				bot.setQuoteOrderQty(rs.getInt("quoteOrderQty"));
				bot.setCycleMaxOrders(rs.getInt("cycleMaxOrders"));
				bot.setOrderStep(rs.getDouble("orderStep"));
				bot.setDescription(rs.getString("description"));
				bot.setInitialDelay(rs.getInt("initialDelay"));
				bot.setDelay(rs.getInt("delay"));
				bot.setTimeUnit(TimeUnit.values()[rs.getInt("timeUnit")]);
				bot.setStopLoss(rs.getDouble("stopLoss"));
				bot.setDemaAlertTaskId(rs.getString("demaAlertTaskId"));
                    bot.setEnableStopLoss(rs.getBoolean("enableStopLoss"));
                    bot.setProfitBase(rs.getBoolean("profitBase"));
				bot.setPriceGridLimit(rs.getDouble("priceGridLimit"));
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
	
	public static List<TradeBot> getAllTradeBots() throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "SELECT * FROM TRADE_BOT";
		try {
			ps = connection.prepareStatement(query);
			rs = ps.executeQuery();
			List<TradeBot> bots = new ArrayList<>();
			while (rs.next()) {
				TradeBot bot = new TradeBot();
				bot.setId(rs.getLong("id"));
				bot.setSymbol(rs.getString("symbol"));
				bot.setCreatedDate(rs.getDate("createdDate"));
				bot.setTaskId(rs.getString("taskId"));
				bot.setQuoteOrderQty(rs.getInt("quoteOrderQty"));
				bot.setCycleMaxOrders(rs.getInt("cycleMaxOrders"));
				bot.setOrderStep(rs.getDouble("orderStep"));
				bot.setDescription(rs.getString("description"));
				bot.setInitialDelay(rs.getInt("initialDelay"));
				bot.setDelay(rs.getInt("delay"));
				bot.setTimeUnit(TimeUnit.values()[rs.getInt("timeUnit")]);
				bot.setStopLoss(rs.getDouble("stopLoss"));
				bot.setDemaAlertTaskId(rs.getString("demaAlertTaskId"));
                    bot.setEnableStopLoss(rs.getBoolean("enableStopLoss"));
                    bot.setProfitBase(rs.getBoolean("profitBase"));
				bot.setPriceGridLimit(rs.getDouble("priceGridLimit"));
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
	
	public static void updateTradeBot(TradeBot bot) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "UPDATE TRADE_BOT SET symbol=?, createdDate=?, taskId=?, quoteOrderQty=?, cycleMaxOrders=?, orderStep=?,"
                  + "description=?, initialDelay=?, delay=?, timeUnit=?, stopLoss=?, demaAlertTaskId=?, enableStopLoss=?,"
			   + "profitBase=?, priceGridLimit=? WHERE id = ?";

		try {
			ps = connection.prepareStatement(query);
			
			ps.setString(1, bot.getSymbol());
			ps.setDate(2, new java.sql.Date(bot.getCreatedDate().getTime()));
			ps.setString(3, bot.getTaskId());
			ps.setInt(4, bot.getQuoteOrderQty());
			ps.setInt(5, bot.getCycleMaxOrders());
			ps.setDouble(6, bot.getOrderStep());
			ps.setString(7, bot.getDescription());
			ps.setInt(8, bot.getInitialDelay());
			ps.setInt(9, bot.getDelay());
			ps.setInt(10, bot.getTimeUnit().ordinal());
			ps.setDouble(11, bot.getStopLoss());
			ps.setString(12, bot.getDemaAlertTaskId());
               ps.setBoolean(13, bot.isEnableStopLoss());
               ps.setBoolean(14, bot.isProfitBase());
			ps.setDouble(15, bot.getPriceGridLimit());
			ps.setLong(16, bot.getId());
			
			ps.executeUpdate();

		} catch (SQLException e) {
			System.err.println(e);
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
	}
	
	public static void deleteTradeBot(int id) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "DELETE FROM TRADE_BOT WHERE id = ?";
		try {
			ps = connection.prepareStatement(query);
			ps.setInt(1, id);
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
