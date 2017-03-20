package com.easyapp.raml2springbootplugin.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public class ExternalConfig {
	private boolean generateTests = true;
	private boolean overwriteFiles = true;
	private boolean generateHealthCheck = true;
	private DockerConfig dockerConfig = null;
	private JpaConfig jpaConfig = null;

	public static class DockerConfig {
		private String dockerHost = null;
		private String dockerBaseImageName = "openjdk:8-alpine";
		private String dockerImageName;

		public String getDockerHost() {
			return dockerHost;
		}

		public void setDockerHost(final String dockerHost) {
			this.dockerHost = dockerHost;
		}

		public String getDockerBaseImageName() {
			return dockerBaseImageName;
		}

		public void setDockerBaseImageName(final String dockerBaseImageName) {
			this.dockerBaseImageName = dockerBaseImageName;
		}

		public String getDockerImageName() {
			return dockerImageName;
		}

		public void setDockerImageName(final String dockerImageName) {
			this.dockerImageName = dockerImageName;
		}

		public String getConfigError() {
			if (StringUtils.isEmpty(dockerBaseImageName)) {
				return "Docker Base Image is missing";
			}

			if (!dockerBaseImageName.equals(dockerBaseImageName.toLowerCase())) {
				return "Docker Base Image " + dockerBaseImageName
						+ " is invalid (Docker Images should be all lower case)";
			}

			if (StringUtils.isEmpty(dockerImageName)) {
				return "Docker Image is missing";
			}

			if (!dockerImageName.equals(dockerImageName.toLowerCase())) {
				return "Docker Image " + dockerImageName + " is invalid (Docker Images should be all lower case)";
			}

			return null;
		}
	}

	public static class JpaConfig {
		private List<Table> tables = null;

		public static class Table {
			private String tableName = null;
			private String sequenceName = null;
			private int sequenceIncrement = 1;
			private List<Relationship> relationships = null;
			private List<EntityMapping> entityMappings = null;

			public static class Relationship {
				private String relationshipType = null;
				private String referencedTableName = null;
				private String fetchType = "FetchType.LAZY";
				private String cascadeType = "CascadeType.DETACH";
				private List<Join> joins = null;
				private String whereClause = null;

				public static class Join {
					private String columnName = null;
					private String referencedColumnName = null;

					public String getColumnName() {
						return columnName;
					}

					public void setColumnName(final String columnName) {
						this.columnName = columnName;
					}

					public String getReferencedColumnName() {
						return referencedColumnName;
					}

					public void setReferencedColumnName(final String referencedColumnName) {
						this.referencedColumnName = referencedColumnName;
					}

					public String getConfigError(final String relationshipType, final String tableName) {
						if (StringUtils.isEmpty(columnName)) {
							return "Column Name is missing in relationship: " + relationshipType + " for table: "
									+ tableName + " in JPA Config";
						}

						if (StringUtils.isEmpty(referencedColumnName)) {
							return "Referenced Column Name is missing in relationship: " + relationshipType
									+ " for table: " + tableName + " in JPA Config";
						}

						return null;
					}
				}

				public String getRelationshipType() {
					return relationshipType;
				}

				public void setRelationshipType(final String relationshipType) {
					this.relationshipType = relationshipType;
				}

				public String getReferencedTableName() {
					return referencedTableName;
				}

				public void setReferencedTableName(final String referencedTableName) {
					this.referencedTableName = referencedTableName;
				}

				public String getFetchType() {
					return fetchType;
				}

				public void setFetchType(final String fetchType) {
					this.fetchType = fetchType;
				}

				public String getCascadeType() {
					return cascadeType;
				}

				public void setCascadeType(final String cascadeType) {
					this.cascadeType = cascadeType;
				}

				public List<Join> getJoins() {
					return joins;
				}

				public String getWhereClause() {
					return whereClause;
				}

				public void setWhereClause(final String whereClause) {
					this.whereClause = whereClause;
				}

				public String getJoinColumns() {
					if (joins == null) {
						return null;
					}

					if (joins.size() > 1) {
						return "{" + joins.stream()
								.map(join -> "@JoinColumn(name = \"" + join.getColumnName()
										+ "\", referencedColumnName = \"" + join.getReferencedColumnName()
										+ "\", nullable = true, updatable = false, insertable = false)")
								.collect(Collectors.joining(", ")) + "}";
					} else {
						return "@JoinColumn(name = \"" + joins.get(0).getColumnName() + "\", referencedColumnName = \""
								+ joins.get(0).getReferencedColumnName()
								+ "\", nullable = true, updatable = false, insertable = false)";
					}
				}

				public String getInverseJoinColumns() {
					if (joins == null) {
						return null;
					}

					if (joins.size() > 1) {
						return "{" + joins.stream()
								.map(join -> "@JoinColumn(name = \"" + join.getReferencedColumnName()
										+ "\", referencedColumnName = \"" + join.getColumnName()
										+ "\", nullable = true, updatable = false, insertable = false)")
								.collect(Collectors.joining(", ")) + "}";
					} else {
						return "@JoinColumn(name = \"" + joins.get(0).getReferencedColumnName()
								+ "\", referencedColumnName = \"" + joins.get(0).getColumnName()
								+ "\", nullable = true, updatable = false, insertable = false)";
					}
				}

				public void setJoins(final List<Join> joins) {
					this.joins = joins;
				}

				public String getConfigError(final String tableName) {
					if (StringUtils.isEmpty(relationshipType)) {
						return "Relationship Type is missing for table: " + tableName + " in JPA Config";
					}

					if (!"OneToOne".equals(relationshipType) && !"OneToMany".equals(relationshipType)
							&& !"ManyToOne".equals(relationshipType) && !"ManyToMany".equals(relationshipType)) {
						return "Relationship Type should be one of [OneToOne, OneToMany, ManyToOne, ManyToMany] and is incorrect for table: "
								+ tableName + " in JPA Config";
					}

					if (StringUtils.isEmpty(referencedTableName)) {
						return "Referenced Table Name is missing in relationship for table: " + tableName
								+ " in JPA Config";
					}

					if (StringUtils.isEmpty(fetchType)) {
						return "Fetch Type is missing in relationship for table: " + tableName + " in JPA Config";
					}

					if (StringUtils.isEmpty(cascadeType)) {
						return "Cascade Type is missing in relationship for table: " + tableName + " in JPA Config";
					}

					if (joins == null) {
						if ("ManyToOne".equals(relationshipType)) {
							return "Joins missing in relationship: " + relationshipType + " for table: " + tableName
									+ " in JPA Config";
						}
					} else {
						return joins.stream().map(join -> join.getConfigError(relationshipType, tableName))
								.filter(error -> error != null).findAny().orElse(null);
					}

					return null;
				}
			}

			public static class EntityMapping {
				private String ramlType;
				private boolean useForCRUD = false;
				private ColumnMappings columnMappings;

				public static class ColumnMappings {
					private Map<String, String> columnMappings = new HashMap<>();

					@JsonAnyGetter
					public Map<String, String> get() {
						return columnMappings;
					}

					@JsonAnySetter
					public void setColumnMapping(final String key, final String value) {
						this.columnMappings.put(key, value);
					}
				}

				public String getRamlType() {
					return ramlType;
				}

				public void setRamlType(final String ramlType) {
					this.ramlType = ramlType;
				}

				public boolean useForCRUD() {
					return useForCRUD;
				}

				public void setUseForCRUD(final boolean useForCRUD) {
					this.useForCRUD = useForCRUD;
				}

				public Map<String, String> getColumnMappings() {
					return columnMappings.get();
				}

				public void setColumnMappings(final ColumnMappings columnMappings) {
					this.columnMappings = columnMappings;
				}

				public String getConfigError(final String tableName, final List<Relationship> relationships) {
					if (StringUtils.isEmpty(ramlType)) {
						return "RAML Type missing in entity mapping for table = " + tableName + " in JPA Config";
					}

					if (columnMappings == null || getColumnMappings().isEmpty()) {
						return "Mappings missing for entity with RAML Type = " + ramlType + " for table = " + tableName
								+ " in JPA Config";
					}

					if (!getColumnMappings().keySet().stream().filter(column -> column.contains("."))
							.map(referenceColumn -> referenceColumn.substring(0, referenceColumn.indexOf('.')))
							.allMatch(referenceTable -> relationships != null && relationships.stream().anyMatch(
									relationship -> relationship.getReferencedTableName().equals(referenceTable)))) {
						return "Column Mapping contains reference table for which relationship is not defined for table "
								+ tableName + " in JPA Config";
					}

					return null;
				}
			}

			public Table() {

			}

			public Table(final String tableName) {
				this.tableName = tableName;
			}

			public String getTableName() {
				return tableName;
			}

			public void setTableName(final String tableName) {
				this.tableName = tableName;
			}

			public String getSequenceName() {
				return sequenceName;
			}

			public void setSequenceName(final String sequenceName) {
				this.sequenceName = sequenceName;
			}

			public int getSequenceIncrement() {
				return sequenceIncrement;
			}

			public void setSequenceIncrement(final int sequenceIncrement) {
				this.sequenceIncrement = sequenceIncrement;
			}

			public List<Relationship> getRelationships() {
				return relationships;
			}

			public void setRelationships(final List<Relationship> relationships) {
				this.relationships = relationships;
			}

			public List<EntityMapping> getEntityMappings() {
				return entityMappings;
			}

			public void setEntityMappings(final List<EntityMapping> entityMappings) {
				this.entityMappings = entityMappings;
			}

			public String getConfigError() {
				if (StringUtils.isEmpty(tableName)) {
					return "Table Name is missing in JPA Config";
				}

				if (relationships != null && !relationships.isEmpty()) {
					relationships.stream().map(relationship -> relationship.getConfigError(tableName))
							.filter(configError -> configError != null).findAny().orElse(null);
				}

				if (entityMappings != null && !entityMappings.isEmpty()) {
					if (entityMappings.stream().filter(entityMapping -> entityMapping.useForCRUD()).count() > 1) {
						return "More than one Entity Mapping cannot be used for CRUD for table " + tableName
								+ " in JPA Config";
					} else {
						return entityMappings.stream()
								.map(entityMapping -> entityMapping.getConfigError(tableName, relationships))
								.filter(configError -> configError != null).findAny().orElse(null);
					}
				}

				return null;
			}
		}

		public List<Table> getTables() {
			return tables;
		}

		public void setTables(final List<Table> tables) {
			this.tables = tables;
		}

		public String getConfigError() {
			if (tables == null || tables.isEmpty()) {
				return "Missing tables in JPA Config";
			} else {
				return tables.stream().map(table -> table.getConfigError()).filter(configError -> configError != null)
						.findAny().orElse(null);
			}
		}
	}

	public boolean generateTests() {
		return generateTests;
	}

	public void setGenerateTests(boolean generateTests) {
		this.generateTests = generateTests;
	}

	public boolean overwriteFiles() {
		return overwriteFiles;
	}

	public void setOverwriteFiles(final boolean overwriteFiles) {
		this.overwriteFiles = overwriteFiles;
	}

	public boolean generateHealthCheck() {
		return generateHealthCheck;
	}

	public void setGenerateHealthCheck(final boolean generateHealthCheck) {
		this.generateHealthCheck = generateHealthCheck;
	}

	public boolean dockerize() {
		return dockerConfig != null;
	}

	public DockerConfig getDockerConfig() {
		return dockerConfig;
	}

	public void setDockerConfig(final DockerConfig dockerConfig) {
		this.dockerConfig = dockerConfig;
	}

	public boolean hasJpaConfig() {
		return jpaConfig != null;
	}

	public JpaConfig getJpaConfig() {
		return jpaConfig;
	}

	public void setJpaConfig(final JpaConfig jpaConfig) {
		this.jpaConfig = jpaConfig;
	}

	public String getConfigError() {
		String configError = null;

		if (dockerize()) {
			configError = dockerConfig.getConfigError();
		}

		if (configError == null && hasJpaConfig()) {
			configError = jpaConfig.getConfigError();
		}

		return configError;
	}
}
