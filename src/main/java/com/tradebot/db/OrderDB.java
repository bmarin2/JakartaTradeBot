package com.tradebot.db;

import com.tradebot.model.OrderTracker;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OrderDB {
	
	public static void addOrder(OrderTracker order) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "INSERT INTO ORDER_TRACKER (side, createdDate, orderId) VALUES (?, ?, ?)";
		try {
			ps = connection.prepareStatement(query);
			
			ps.setInt(1, order.getSide());
			ps.setDate(2, Date.valueOf(order.getCreatedDate().toLocalDate()));
			ps.setLong(3, order.getOrderId());
			
			ps.executeUpdate();
			
		} catch (SQLException e) {
			System.err.println(e);
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
	}
	
	
	public static OrderTracker getOneOrder(int id) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "SELECT * FROM ORDER_TRACKER where id=?";
		try {
			ps = connection.prepareStatement(query);
			ps.setInt(1, id);
			rs = ps.executeQuery();
			OrderTracker order = new OrderTracker();
			while (rs.next()) {
				order.setId(rs.getLong("id"));
				order.setSide(rs.getInt("side"));
				order.setCreatedDate(rs.getTimestamp("createdDate").toLocalDateTime());
				order.setOrderId(rs.getLong("orderId"));
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
	
	public static List<OrderTracker> getOrdersFromBot(long botId) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "SELECT * FROM ORDER_TRACKER where tradebot_id=?";
		try {
			ps = connection.prepareStatement(query);
			rs = ps.executeQuery();
			List<OrderTracker> orders = new ArrayList<>();
			while (rs.next()) {
				OrderTracker order = new OrderTracker();
				order.setId(rs.getLong("id"));
				order.setSide(rs.getInt("side"));
				order.setCreatedDate(rs.getTimestamp("createdDate").toLocalDateTime());
				order.setOrderId(rs.getLong("orderId"));
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
}
