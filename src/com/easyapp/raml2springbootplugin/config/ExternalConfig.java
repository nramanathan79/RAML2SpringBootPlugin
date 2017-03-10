package com.easyapp.raml2springbootplugin.config;

import java.util.List;

import org.springframework.util.StringUtils;

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
			private String collectionResource = null;
			private String uriPath = null;
			private String sequenceName = null;
			private int sequenceIncrement = 1;
			private List<Relationship> relationships = null;

			public static class Relationship {
				private String relationshipType = null;
				private String referencedTableName = null;
				private String fetchType = "FetchType.LAZY";
				private String cascadeType = "CascadeType.DETACH";
				private List<Join> joins = null;

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
						return "Referenced Table Name is missing in relationship for table: " + tableName + " in JPA Config";
					}

					if (StringUtils.isEmpty(fetchType)) {
						return "Fetch Type is missing in relationship for table: " + tableName + " in JPA Config";
					}

					if (StringUtils.isEmpty(cascadeType)) {
						return "Cascade Type is missing in relationship for table: " + tableName + " in JPA Config";
					}

					if (!"OneToOne".equals(relationshipType) && !"OneToMany".equals(relationshipType)
							&& !"ManyToOne".equals(relationshipType) && !"ManyToMany".equals(relationshipType)) {
						return "Relationship Type must be one of [OneToOne, OneToMany, ManyToOne, ManyToMany] for table: "
								+ tableName + " in JPA Config";
					}

					if ("ManyToOne".equals(relationshipType) || "ManyToMany".equals(relationshipType)) {
						if (joins == null) {
							return "Joins missing in relationship: " + relationshipType + " for table: " + tableName
									+ " in JPA Config";
						}

						return joins.stream().map(join -> join.getConfigError(relationshipType, tableName))
								.filter(error -> error != null).findFirst().orElse(null);
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

			public String getCollectionResource() {
				return collectionResource;
			}

			public void setCollectionResource(final String collectionResource) {
				this.collectionResource = collectionResource;
			}

			public String getUriPath() {
				return uriPath;
			}

			public void setUriPath(final String uriPath) {
				this.uriPath = uriPath;
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

			public String getConfigError() {
				if (StringUtils.isEmpty(tableName)) {
					return "Table Name is missing in JPA Config";
				}

				if (StringUtils.isEmpty(collectionResource)) {
					return "Collection Resource is missing in JPA Config";
				}

				if (StringUtils.isEmpty(uriPath)) {
					return "URI Path is missing in JPA Config";
				}

				if (relationships != null && !relationships.isEmpty()) {
					relationships.stream().map(relationship -> relationship.getConfigError(tableName))
							.filter(configError -> configError != null).findFirst().orElse(null);
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
						.findFirst().orElse(null);
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
