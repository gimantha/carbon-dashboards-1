/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.dashboards.core.internal.database;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.dashboards.core.bean.DashboardMetadata;
import org.wso2.carbon.dashboards.core.exception.DashboardException;

import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;

/**
 * This is a core class of the DashboardMetadata JDBC Based implementation.
 */
public class DashboardMetadataDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardMetadataDao.class);
    private static final Gson GSON = new Gson();
    private static final String COLUMN_DASHBOARD_LANDING_PAGE = "LANDING_PAGE";
    private static final String COLUMN_DASHBOARD_ID = "ID";
    private static final String COLUMN_DASHBOARD_PARENT_ID = "PARENT_ID";
    private static final String COLUMN_DASHBOARD_CONTENT = "CONTENT";
    private static final String COLUMN_DASHBOARD_DESCRIPTION = "DESCRIPTION";
    private static final String COLUMN_DASHBOARD_NAME = "NAME";
    private static final String COLUMN_DASHBOARD_URL = "URL";
    private static final String COLUMN_DASHBOARD_OWNER = "OWNER";

    private final DataSource dataSource;
    private final QueryProvider queryProvider;

    public DashboardMetadataDao(DataSource dataSource, QueryProvider queryProvider) {
        this.dataSource = dataSource;
        this.queryProvider = queryProvider;
    }

    public void update(DashboardMetadata dashboardMetadata) throws DashboardException {
        Connection connection = null;
        PreparedStatement ps = null;
        String query = queryProvider.getQuery(QueryProvider.UPDATE_DASHBOARD_CONTENT_QUERY);
        try {
            connection = getConnection();
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(query);
            ps.setString(1, dashboardMetadata.getName());
            ps.setString(2, dashboardMetadata.getDescription());
            Blob blob = connection.createBlob();
            blob.setBytes(1, toJsonBytes(dashboardMetadata.getPages()));
            ps.setBlob(3, blob);
            ps.setString(4, dashboardMetadata.getUrl());
            ps.setString(5, dashboardMetadata.getParentId());
            ps.setString(6, dashboardMetadata.getLandingPage());
            ps.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            rollbackQuietly(connection);
            LOGGER.debug("Failed to execute SQL query {}", query);
            throw new DashboardException(
                    "Cannot update dashboard '" + dashboardMetadata.getId() + "' with " + dashboardMetadata + ".", e);
        } finally {
            closeQuietly(connection, ps, null);
        }
    }

    public void add(DashboardMetadata dashboardMetadata) throws DashboardException {
        Connection connection = null;
        PreparedStatement ps = null;
        String query = queryProvider.getQuery(QueryProvider.ADD_DASHBOARD_CONTENT_QUERY);
        try {
            connection = getConnection();
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(query);
            Blob blob = connection.createBlob();
            ps.setString(1, dashboardMetadata.getUrl());
            ps.setString(2, dashboardMetadata.getOwner());
            ps.setString(3, dashboardMetadata.getName());
            ps.setString(4, dashboardMetadata.getDescription());
            ps.setString(5, dashboardMetadata.getParentId());
            ps.setString(6, dashboardMetadata.getLandingPage());
            blob.setBytes(1, toJsonBytes(dashboardMetadata.getPages()));
            ps.setBlob(7, blob);
            ps.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            rollbackQuietly(connection);
            LOGGER.debug("Failed to execute SQL query {}", query);
            throw new DashboardException("Cannot create a new dashboard with " + dashboardMetadata + ".", e);
        } finally {
            closeQuietly(connection, ps, null);
        }
    }

    public void delete(String url) throws DashboardException {
        Connection connection = null;
        PreparedStatement ps = null;
        String query = queryProvider.getQuery(QueryProvider.DELETE_DASHBOARD_BY_URL_QUERY);
        try {
            connection = getConnection();
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(query);
            ps.setString(1, url);
            ps.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            rollbackQuietly(connection);
            LOGGER.debug("Failed to execute SQL query {}", query);
            throw new DashboardException("Cannot delete dashboard '" + url + "'.", e);
        } finally {
            closeQuietly(connection, ps, null);
        }
    }

    public Optional<DashboardMetadata> get(String url) throws DashboardException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String query = queryProvider.getQuery(QueryProvider.GET_DASHBOARD_BY_URL_QUERY);
        try {
            connection = getConnection();
            ps = connection.prepareStatement(query);
            ps.setString(1, url);
            result = ps.executeQuery();

            if (result.next()) {
                DashboardMetadata dashboardMetadata = toDashboardMetadata(result);
                dashboardMetadata.setPages(fromJasonBytes(result.getBlob(COLUMN_DASHBOARD_CONTENT)));
                return Optional.of(dashboardMetadata);
            } else {
                return Optional.empty();
            }
        } catch (SQLException e) {
            LOGGER.debug("Failed to execute SQL query {}", query);
            throw new DashboardException("Cannot retrieve dashboard for URl '" + url + "'.", e);
        } finally {
            closeQuietly(connection, ps, result);
        }
    }

    public Set<DashboardMetadata> getAll() throws DashboardException {
        Set<DashboardMetadata> dashboardMetadatas = new HashSet<>();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet results = null;
        String query = queryProvider.getQuery(QueryProvider.GET_DASHBOARD_METADATA_LIST_QUERY);
        try {
            connection = getConnection();
            ps = connection.prepareStatement(query);
            results = ps.executeQuery();
            while (results.next()) {
                dashboardMetadatas.add(toDashboardMetadata(results));
            }
        } catch (SQLException e) {
            LOGGER.debug("Failed to execute SQL query {}", query);
            throw new DashboardException("Cannot retrieve dashboards.", e);
        } finally {
            closeQuietly(connection, ps, results);
        }

        return dashboardMetadatas;
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private static byte[] toJsonBytes(Object dashboardPages) {
        return GSON.toJson(dashboardPages).getBytes(StandardCharsets.UTF_8);
    }

    private static Object fromJasonBytes(Blob byteBlob) throws SQLException {
        return new String(byteBlob.getBytes(1, (int) byteBlob.length()), StandardCharsets.UTF_8);
    }

    private static DashboardMetadata toDashboardMetadata(ResultSet result) throws SQLException {
        DashboardMetadata dashboardMetadata = new DashboardMetadata();
        dashboardMetadata.setId(result.getString(COLUMN_DASHBOARD_ID));
        dashboardMetadata.setName(result.getString(COLUMN_DASHBOARD_NAME));
        dashboardMetadata.setOwner(result.getString(COLUMN_DASHBOARD_OWNER));
        dashboardMetadata.setUrl(result.getString(COLUMN_DASHBOARD_URL));
        dashboardMetadata.setDescription(result.getString(COLUMN_DASHBOARD_DESCRIPTION));
        dashboardMetadata.setParentId(result.getString(COLUMN_DASHBOARD_PARENT_ID));
        dashboardMetadata.setLandingPage(result.getString(COLUMN_DASHBOARD_LANDING_PAGE));
        return dashboardMetadata;
    }

    static void closeQuietly(Connection connection, PreparedStatement preparedStatement, ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                LOGGER.error("An error occurred when closing result set.", e);
            }
        }
        if (preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                LOGGER.error("An error occurred when closing prepared statement.", e);
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOGGER.error("An error occurred when closing DB connection.", e);
            }
        }
    }

    static void rollbackQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                LOGGER.error("An error occurred when rollbacking DB connection.", e);
            }
        }
    }
}
