package com.tradebot.db;

import com.tradebot.model.UserAccount;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UserAccountDB {

	public static long createUserAccount(UserAccount userAccount) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		long order_id = 0;

		String query = "INSERT INTO USER_ACCOUNT (username, password) VALUES (?, ?)";
		try {
			ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

			ps.setString(1, userAccount.getUsername());
			ps.setString(2, userAccount.getPassword());

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
	
	
	public static void updateUserAccount(UserAccount userAccount) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "UPDATE USER_ACCOUNT SET username=?, password=? WHERE id=?";

		try {
			ps = connection.prepareStatement(query);
			
			ps.setString(1, userAccount.getUsername());
			ps.setString(1, userAccount.getPassword());
			ps.setLong(3, userAccount.getId());
			
			ps.executeUpdate();

		} catch (SQLException e) {
			System.err.println(e);
		} finally {
			DBUtil.closeResultSet(rs);
			DBUtil.closePreparedStatement(ps);
			pool.freeConnection(connection);
		}
	}
	
	public static UserAccount getOneUserAccount(String username) throws Exception {
		ConnectionPool pool = ConnectionPool.getInstance();
		Connection connection = pool.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String query = "SELECT * FROM USER_ACCOUNT where username=?";
		try {
			ps = connection.prepareStatement(query);
			ps.setString(1, username);
			rs = ps.executeQuery();
			UserAccount userAccount = new UserAccount();
			while (rs.next()) {
				userAccount.setId(rs.getLong("id"));
				userAccount.setUsername(rs.getString("username"));
				userAccount.setPassword(rs.getString("password"));
			}
			return userAccount;
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
