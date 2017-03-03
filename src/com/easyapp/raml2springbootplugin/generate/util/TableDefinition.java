package com.easyapp.raml2springbootplugin.generate.util;

import java.util.Comparator;
import java.util.List;
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
		return columns;
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

	public Stream<ColumnDefinition> getColumnStream() {
		if (columns != null) {
			return columns.stream().sorted(byColumnOrder);
		} else {
			return null;
		}
	}

	public Stream<ColumnDefinition> getKeyColumnStream() {
		if (columns != null) {
			return columns.stream().filter(ColumnDefinition::isInPrimaryKey).sorted(byColumnOrder);
		} else {
			return null;
		}
	}

	public Stream<ColumnDefinition> getNonKeyColumnStream() {
		if (columns != null) {
			return columns.stream().filter(column -> !column.isInPrimaryKey()).sorted(byColumnOrder);
		} else {
			return null;
		}
	}
}
