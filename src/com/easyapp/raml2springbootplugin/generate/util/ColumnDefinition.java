package com.easyapp.raml2springbootplugin.generate.util;

import java.sql.JDBCType;

public class ColumnDefinition {
	private final String columnName;
	private final JDBCType dataType;
	private final int columnOrder;
	private final boolean nullable;
	private final boolean autoIncrement;
	private boolean isInPrimaryKey = false;
	private int primaryKeyOrder = -1;

	public ColumnDefinition(final String columnName, final JDBCType dataType, final int columnOrder,
			final boolean nullable, final boolean autoIncrement) {
		this.columnName = columnName;
		this.dataType = dataType;
		this.columnOrder = columnOrder;
		this.nullable = nullable;
		this.autoIncrement = autoIncrement;
	}

	public String getColumnName() {
		return columnName;
	}

	public JDBCType getDataType() {
		return dataType;
	}

	public int getColumnOrder() {
		return columnOrder;
	}

	public boolean isNullable() {
		return nullable;
	}

	public boolean isAutoIncrement() {
		return autoIncrement;
	}

	public boolean isInPrimaryKey() {
		return isInPrimaryKey;
	}

	public int getPrimaryKeyOrder() {
		return primaryKeyOrder;
	}

	public void setPrimaryKeyOrder(final int primaryKeyOrder) {
		this.isInPrimaryKey = true;
		this.primaryKeyOrder = primaryKeyOrder;
	}
}
