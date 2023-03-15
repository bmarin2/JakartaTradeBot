package com.tradebot.db;

import com.tradebot.model.TradeBot;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

public class TradeBotDB {
	public static void addDept(TradeBot bot) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "INSERT INTO TRADE_BOT (symbol, createdDate, taskId, quoteOrderQty, cycleMaxOrders, orderStep, description, initialDelay, delay, timeUnit) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try {
			ps = connection.prepareStatement(query);
			ps.setString(1, bot.getSymbol());
			ps.setTimestamp(2, Timestamp.valueOf(bot.getCreatedDate()));
			ps.setString(3, bot.getTaskId());
			ps.setInt(4, bot.getQuoteOrderQty());
			ps.setInt(5, bot.getCycleMaxOrders());
			ps.setDouble(6, bot.getOrderStep());
			ps.setString(7, bot.getDescription());
			ps.setLong(8, bot.getInitialDelay());
			ps.setLong(9, bot.getDelay());
			ps.setInt(10, bot.getTimeUnit().ordinal());
			
			ps.executeUpdate();
		} catch (SQLException e) {
			System.err.println(e);
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
	}
	
	public static TradeBot getOneTradeBot(int id) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "SELECT * FROM TRADE_BOT where id=?";
		try {
			ps = connection.prepareStatement(query);
			ps.setInt(1, id);
			rs = ps.executeQuery();
			TradeBot bot = new TradeBot();
			while (rs.next()) {
				bot.setId(rs.getLong("id"));
				bot.setSymbol(rs.getString("symbol"));
				bot.setCreatedDate(rs.getTimestamp("createdDate").toLocalDateTime());
				bot.setTaskId(rs.getString("taskId"));
				bot.setQuoteOrderQty(rs.getInt("quoteOrderQty"));
				bot.setCycleMaxOrders(rs.getInt("cycleMaxOrders"));
				bot.setOrderStep(rs.getDouble("orderStep"));
				bot.setDescription(rs.getString("description"));
				bot.setInitialDelay(rs.getLong("initialDelay"));
				bot.setDelay(rs.getLong("delay"));
				bot.setTimeUnit(TimeUnit.values()[rs.getInt("timeUnit")]);
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
