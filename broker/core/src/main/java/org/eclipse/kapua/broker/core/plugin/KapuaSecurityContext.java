/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.broker.core.plugin;

import java.security.Principal;
import java.security.cert.Certificate;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.activemq.command.ConnectionInfo;
import org.apache.activemq.security.AuthorizationMap;
import org.apache.activemq.security.SecurityContext;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.security.KapuaSession;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.service.authentication.KapuaPrincipal;
import org.eclipse.kapua.service.authentication.token.AccessToken;
import org.eclipse.kapua.service.device.registry.connection.DeviceConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kapua security context implementation of ActiveMQ broker {@link SecurityContext}
 *
 * @since 1.0
 */
public class KapuaSecurityContext extends SecurityContext {

    protected static final Logger logger = LoggerFactory.getLogger(KapuaSecurityContext.class);

    public static final int BROKER_CONNECT_IDX = 0;
    public static final int DEVICE_MANAGE_IDX = 1;
    public static final int DATA_VIEW_IDX = 2;
    public static final int DATA_MANAGE_IDX = 3;
    public static final int DEVICE_VIEW_IDX = 4;

    private KapuaPrincipal principal;
    private KapuaSession kapuaSession;
    private KapuaId kapuaConnectionId;
    private String connectionId;
    private Set<Principal> principals;
    private ConnectorDescriptor connectorDescriptor;

    private AuthorizationMap authorizationMap;

    private String brokerId;
    private String userName;
    private KapuaId scopeId;
    private KapuaId userId;
    private String accountName;
    private String clientId;
    private String fullClientId;
    private String clientIp;
    private String oldConnectionId;
    private boolean[] hasPermissions;
    private String brokerIpOrHostName;
    private Certificate[] clientCertificates;

    private boolean admin;
    private boolean provisioning;

    //flag to help the correct lifecycle handling
    private boolean missing;

    // use to track the allowed destinations for debug purpose
    private List<String> authDestinations;

    private KapuaSecurityContext(KapuaPrincipal principal) {
        super(principal.getName());
        this.principal = principal;
        principals = new HashSet<Principal>();
        principals.add(principal);
        authDestinations = new ArrayList<>();
    }

    public KapuaSecurityContext(KapuaPrincipal principal, KapuaId kapuaConnectionId, String connectionId, ConnectorDescriptor connectorDescriptor) {
        this(principal);
        this.kapuaSession = KapuaSession.createFrom();

        this.connectorDescriptor = connectorDescriptor;
        this.kapuaConnectionId = kapuaConnectionId;
        this.connectionId = connectionId;
    }

    public KapuaSecurityContext(Long scopeId, String clientId) {
        super(null);
        authDestinations = new ArrayList<>();
        this.clientId = clientId;
        updateFullClientId(scopeId);
    }

    public KapuaSecurityContext(String brokerId, ConnectionInfo info) {
        super(info.getUserName());
        authDestinations = new ArrayList<>();
        this.brokerId = brokerId;
        userName = info.getUserName();
        clientId = info.getClientId();
        clientIp = info.getClientIp();
        connectionId = info.getConnectionId().getValue();
        if(info.getTransportContext() instanceof Certificate[]) {
            clientCertificates = (Certificate[]) info.getTransportContext();
        }
    }

    public KapuaSecurityContext(KapuaPrincipal principal, String brokerId, String brokerIpOrHostName, String accountName, ConnectionInfo info, boolean missing) {
        this(principal);
        this.brokerId = brokerId;
        this.brokerIpOrHostName = brokerIpOrHostName;
        this.accountName = accountName;
        this.missing = missing;
        userName = info.getUserName();
        clientId = principal.getClientId();
        scopeId = principal.getAccountId();
        clientIp = info.getClientIp();
        connectionId = info.getConnectionId().getValue();
        updateFullClientId();
    }

    public void update(AccessToken accessToken, String accountName, KapuaId scopeId, KapuaId userId, String connectorName, String brokerIpOrHostName) {
        this.accountName = accountName;
        this.scopeId = scopeId;
        this.userId = userId;
        this.brokerIpOrHostName = brokerIpOrHostName;
        connectorDescriptor = ConnectorDescriptorProviders.getDescriptor(connectorName);
        if (connectorDescriptor == null) {
            throw new IllegalStateException(String.format("Unable to find connector descriptor for connector '%s'", connectorName));
        }
        updateFullClientId();
        principal = new KapuaPrincipalImpl(accessToken,
                userName,
                clientId,
                clientIp);
        principals = new HashSet<Principal>();
        principals.add(principal);
    }

    public KapuaPrincipal getKapuaPrincipal() throws KapuaException {
        return principal;
    }

    public Principal getMainPrincipal() {
        return principal;
    }

    public Set<Principal> getPrincipals() {
        return principals;
    }

    public void setAuthorizationMap(AuthorizationMap authorizationMap) {
        this.authorizationMap = authorizationMap;
    }

    public AuthorizationMap getAuthorizationMap() {
        return authorizationMap;
    }

    public KapuaId getKapuaConnectionId() {
        return kapuaConnectionId;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public ConnectorDescriptor getConnectorDescriptor() {
        return connectorDescriptor;
    }

    public KapuaSession getKapuaSession() {
        return kapuaSession;
    }

    public void setMissing() {
        missing = true;
    }

    public boolean isMissing() {
        return missing;
    }

    public void updatePermissions(boolean[] hasPermissions) {
        this.hasPermissions = hasPermissions;
    }

    public void updateKapuaConnectionId(DeviceConnection deviceConnection) {
        kapuaConnectionId = deviceConnection != null ? deviceConnection.getId() : null;
    }

    public void updateOldConnectionId(String oldConnectionId) {
        this.oldConnectionId = oldConnectionId;
    }

    private void updateFullClientId() {
        fullClientId = MessageFormat.format(KapuaSecurityBrokerFilter.MULTI_ACCOUNT_CLIENT_ID, scopeId.getId().longValue(), clientId);
    }

    private void updateFullClientId(Long scopeId) {
        fullClientId = MessageFormat.format(KapuaSecurityBrokerFilter.MULTI_ACCOUNT_CLIENT_ID, scopeId, clientId);
    }

    public String getFullClientId() {
        return fullClientId;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getBrokerId() {
        return brokerId;
    }

    public Certificate[] getClientCertificates() {
        return clientCertificates;
    }

    public String getUserName() {
        return userName;
    }

    public KapuaId getScopeId() {
        return scopeId;
    }

    public long getScopeIdAsLong() {
        return scopeId != null ? scopeId.getId().longValue() : 0;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientIp() {
        return clientIp;
    }

    public KapuaPrincipal getPrincipal() {
        return principal;
    }

    public String getOldConnectionId() {
        return oldConnectionId;
    }

    public KapuaId getUserId() {
        return userId;
    }

    public boolean[] getHasPermissions() {
        return hasPermissions;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isProvisioning() {
        return provisioning;
    }

    public void setProvisioning(boolean provisioning) {
        this.provisioning = provisioning;
    }

    public boolean isBrokerConnect() {
        return hasPermissions[BROKER_CONNECT_IDX];
    }

    public boolean isDeviceView() {
        return hasPermissions[DEVICE_VIEW_IDX];
    }

    public boolean isDeviceManage() {
        return hasPermissions[DEVICE_MANAGE_IDX];
    }

    public boolean isDataView() {
        return hasPermissions[DATA_VIEW_IDX];
    }

    public boolean isDataManage() {
        return hasPermissions[DATA_MANAGE_IDX];
    }

    public String getBrokerIpOrHostName() {
        return brokerIpOrHostName;
    }

    public void addAuthDestinationToLog(String message) {
        if (logger.isDebugEnabled()) {
            authDestinations.add(message);
        }
    }

    public void logAuthDestinationToLog() {
        if (!authDestinations.isEmpty()) {
            logger.debug("Authorization map:");
            for (String str : authDestinations) {
                logger.debug(str);
            }
        }
    }
}
