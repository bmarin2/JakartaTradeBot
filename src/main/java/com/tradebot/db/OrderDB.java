package com.tradebot.db;

import com.tradebot.model.OrderTracker;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class OrderDB {

	public static long addOrder(OrderTracker order) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		long order_id = 0;

		String query = "INSERT INTO ORDER_TRACKER (sell, buyPrice, sellPrice, profit, buyDate, sellDate, buyOrderId, sellOrderId, tradebot_id, stopLossPrice ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try {
			ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

			ps.setBoolean(1, order.getSell());
			ps.setBigDecimal(2, order.getBuyPrice());
			ps.setBigDecimal(3, order.getSellPrice());
			ps.setBigDecimal(4, order.getProfit());
			ps.setTimestamp(5, Timestamp.valueOf(order.getBuyDate()));
			ps.setTimestamp(6, order.getSellDate() != null ? Timestamp.valueOf(order.getSellDate()) : null);
			ps.setLong(7, order.getBuyOrderId());
			ps.setLong(8, order.getSellOrderId());
			ps.setLong(9, order.getTradebot_id());
			ps.setBigDecimal(10, order.getStopLossPrice());

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

	public static void updateOrder(OrderTracker order) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "UPDATE ORDER_TRACKER SET sell=?, buyPrice=?, sellPrice=?, profit=?, buyDate=?, sellDate=?, buyOrderId=?, sellOrderId=?, tradebot_id=?, stopLossPrice=? WHERE id = ?";
		try {
			ps = connection.prepareStatement(query);

			ps.setBoolean(1, order.getSell());
			ps.setBigDecimal(2, order.getBuyPrice());
			ps.setBigDecimal(3, order.getSellPrice());
			ps.setBigDecimal(4, order.getProfit());
			ps.setTimestamp(5, Timestamp.valueOf(order.getBuyDate()));
			ps.setTimestamp(6, order.getSellDate() != null ? Timestamp.valueOf(order.getSellDate()) : null);
			ps.setLong(7, order.getBuyOrderId());
			ps.setLong(8, order.getSellOrderId());
			ps.setLong(9, order.getTradebot_id());
			ps.setBigDecimal(10, order.getStopLossPrice());
			ps.setLong(11, order.getId());

			ps.executeUpdate();

		} catch (SQLException e) {
			System.err.println(e);
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
	}

	public static OrderTracker getOneOrder(long id) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "SELECT * FROM ORDER_TRACKER where id=?";
		try {
			ps = connection.prepareStatement(query);
			ps.setLong(1, id);
			rs = ps.executeQuery();
			OrderTracker order = new OrderTracker();
			while (rs.next()) {
				order.setId(rs.getLong("id"));
				order.setSell(rs.getBoolean("sell"));
				order.setBuyPrice(rs.getBigDecimal("buyPrice"));
				order.setSellPrice(rs.getBigDecimal("sellPrice"));
				order.setProfit(rs.getBigDecimal("profit"));
				order.setBuyDate(rs.getTimestamp("buyDate").toLocalDateTime());
				order.setSellDate(rs.getTimestamp("sellDate") != null ? rs.getTimestamp("sellDate").toLocalDateTime() : null);
				order.setBuyOrderId(rs.getLong("buyOrderId"));
				order.setSellOrderId(rs.getLong("sellOrderId"));
				order.setStopLossPrice(rs.getBigDecimal("stopLossPrice"));
				order.setTradebot_id(rs.getLong("tradebot_id"));
			}
			return order;
		} catch (SQLException e) {
			System.err.println(e);
			return null;
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
	}

	public static List<OrderTracker> getOrdersFromBot(boolean all, long botId) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "SELECT * FROM ORDER_TRACKER WHERE tradebot_id=? AND sell=? ORDER BY buyDate";
		String queryAll = "SELECT * FROM ORDER_TRACKER WHERE tradebot_id=? ORDER BY buyDate";

		try {

			ps = connection.prepareStatement(all == true ? queryAll : query);

			ps.setLong(1, botId);

			if (!all) {
				ps.setBoolean(2, false);
			}

			rs = ps.executeQuery();

			List<OrderTracker> orders = new ArrayList<>();
			while (rs.next()) {
				OrderTracker order = new OrderTracker();
				order.setId(rs.getLong("id"));
				order.setSell(rs.getBoolean("sell"));
				order.setBuyPrice(rs.getBigDecimal("buyPrice"));
				order.setSellPrice(rs.getBigDecimal("sellPrice"));
				order.setProfit(rs.getBigDecimal("profit"));
				order.setBuyDate(rs.getTimestamp("buyDate").toLocalDateTime());
				order.setSellDate(rs.getTimestamp("sellDate") != null ? rs.getTimestamp("sellDate").toLocalDateTime() : null);
				order.setBuyOrderId(rs.getLong("buyOrderId"));
				order.setSellOrderId(rs.getLong("sellOrderId"));
				order.setStopLossPrice(rs.getBigDecimal("stopLossPrice"));
				order.setTradebot_id(rs.getLong("tradebot_id"));
				orders.add(order);
			}
			return orders;
		} catch (SQLException e) {
			System.err.println(e);
			return null;
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
	}

	public static int getOrderCount(long botId, boolean all, boolean sold) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		int count = 0;

		String queryAll = "SELECT COUNT(*) FROM ORDER_TRACKER WHERE tradeBot_id=?";
		String query = "SELECT COUNT(*) FROM ORDER_TRACKER WHERE tradeBot_id=? AND sell=" + sold;

		try {
			ps = connection.prepareStatement(all == true ? queryAll : query);
			ps.setLong(1, botId);
			rs = ps.executeQuery();

			while (rs.next()) {
				count = rs.getInt(1);
			}
			return count;
		} catch (SQLException e) {
			System.err.println(e);
			return 0;
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
	}

	public static BigDecimal getTadeBotProfits(long botId) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		BigDecimal profits = null;

		String query = "SELECT SUM(profit) FROM ORDER_TRACKER WHERE tradeBot_id=?";

		try {
			ps = connection.prepareStatement(query);
			ps.setLong(1, botId);
			rs = ps.executeQuery();

			while (rs.next()) {
				profits = rs.getBigDecimal(1);
			}

			return profits;

		} catch (SQLException e) {
			System.err.println(e);
			return null;
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
	}

	public static List<OrderTracker> getOrders24Hours() throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "SELECT * FROM ORDER_TRACKER WHERE BUYDATE >= DATEADD('HOUR', -24, NOW())";

		try {

			ps = connection.prepareStatement(query);
			rs = ps.executeQuery();
			List<OrderTracker> orders = new ArrayList<>();
			while (rs.next()) {
				OrderTracker order = new OrderTracker();
				order.setId(rs.getLong("id"));
				order.setSell(rs.getBoolean("sell"));
				order.setBuyPrice(rs.getBigDecimal("buyPrice"));
				order.setSellPrice(rs.getBigDecimal("sellPrice"));
				order.setProfit(rs.getBigDecimal("profit"));
				order.setBuyDate(rs.getTimestamp("buyDate").toLocalDateTime());
				order.setSellDate(rs.getTimestamp("sellDate") != null ? rs.getTimestamp("sellDate").toLocalDateTime() : null);
				order.setBuyOrderId(rs.getLong("buyOrderId"));
				order.setSellOrderId(rs.getLong("sellOrderId"));
				order.setStopLossPrice(rs.getBigDecimal("stopLossPrice"));
				order.setTradebot_id(rs.getLong("tradebot_id"));
				orders.add(order);
			}
			return orders;
		} catch (SQLException e) {
			System.err.println(e);
			return null;
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
	}

	public static void removeOrders(int botId) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "DELETE FROM ORDER_TRACKER WHERE TRADEBOT_ID = ?";
		try {
			ps = connection.prepareStatement(query);
			ps.setInt(1, botId);
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
