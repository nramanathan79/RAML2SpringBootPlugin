package com.easyapp.raml2springbootplugin.generate.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class DatabaseUtil {
	private static DatabaseUtil databaseUtil = null;

	private final String jdbcUrl;
	private final String userName;
	private final String password;

	private DatabaseUtil(final String jdbcUrl, final String userName, final String password) {
		this.jdbcUrl = jdbcUrl;
		this.userName = userName;
		this.password = password;
	}

	public static DatabaseUtil getInstance(final String driverClassName, final String jdbcUrl, final String userName,
			final String password) throws Exception {
		if (databaseUtil == null) {
			Class.forName(driverClassName);
			databaseUtil = new DatabaseUtil(jdbcUrl, userName, password);
		}

		return databaseUtil;
	}

	public TableDefinition getTableDefinition(final String tableName) throws Exception {
		final TableDefinition table = new TableDefinition(tableName);

		Connection connection = null;
		try {
			connection = DriverManager.getConnection(jdbcUrl, userName, password);

			final DatabaseMetaData databaseMetaData = connection.getMetaData();
			final List<ColumnDefinition> columns = new ArrayList<>();
			final ResultSet columnsResultSet = databaseMetaData.getColumns(null, null, tableName, "%");

			while (columnsResultSet.next()) {
				columns.add(new ColumnDefinition(columnsResultSet.getString(4),
						JDBCType.valueOf(columnsResultSet.getInt(5)), columnsResultSet.getInt(17),
						"YES".equals(columnsResultSet.getString(18)) ? true : false,
						"YES".equals(columnsResultSet.getString(23)) ? true : false));
			}

			columnsResultSet.close();

			final ResultSet primaryKeysResultSet = databaseMetaData.getPrimaryKeys(null, null, tableName);

			while (primaryKeysResultSet.next()) {
				final String columnName = primaryKeysResultSet.getString(4);
				final int primaryKeyOrder = primaryKeysResultSet.getInt(5);

				columns.stream().filter(column -> column.getColumnName().equals(columnName))
						.forEach(column -> column.setPrimaryKeyOrder(primaryKeyOrder));
			}

			primaryKeysResultSet.close();

			table.setColumns(columns);
		} finally {
			if (connection != null && !connection.isClosed()) {
				connection.close();
			}
		}

		return table;
	}
}
