package com.easyapp.raml2springbootplugin.generate.util;

import java.sql.JDBCType;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TableDefinition {
	private String tableName;
	private List<ColumnDefinition> columns;
	private static final Comparator<ColumnDefinition> byColumnOrder = (e1,
			e2) -> e1.getColumnOrder() > e2.getColumnOrder() ? 1 : e1.getColumnOrder() == e2.getColumnOrder() ? 0 : -1;

	public TableDefinition(final String tableName) {
		this.tableName = tableName;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(final String tableName) {
		this.tableName = tableName;
	}

	public List<ColumnDefinition> getColumns() {
		if (columns != null) {
			return columns.stream().sorted(byColumnOrder).collect(Collectors.toList());
		} else {
			return null;
		}
	}

	public void setColumns(final List<ColumnDefinition> columns) {
		this.columns = columns;
	}

	public void addColumn(final ColumnDefinition column) {
		this.columns.add(column);
	}

	public boolean hasCompositeKey() {
		if (columns != null) {
			return columns.stream().filter(ColumnDefinition::isInPrimaryKey).count() > 1L;
		}

		return false;
	}

	public List<ColumnDefinition> getKeyColumns() {
		if (columns != null) {
			return columns.stream().filter(ColumnDefinition::isInPrimaryKey).sorted(byColumnOrder)
					.map(column -> new ColumnDefinition(column.getColumnName(), column.getDataType(),
							column.getColumnOrder(), column.isNullable(), column.isAutoIncrement()))
					.collect(Collectors.toList());
		} else {
			return null;
		}
	}

	public List<ColumnDefinition> getNonKeyColumns() {
		if (columns != null) {
			final ColumnDefinition embeddedIdColumn = new ColumnDefinition("ID", JDBCType.JAVA_OBJECT, 0, false, false);
			embeddedIdColumn.setPrimaryKeyOrder(1);

			return Stream.concat(columns.stream().filter(column -> !column.isInPrimaryKey()).sorted(byColumnOrder),
					Stream.of(embeddedIdColumn)).collect(Collectors.toList());
		} else {
			return null;
		}
	}
}
