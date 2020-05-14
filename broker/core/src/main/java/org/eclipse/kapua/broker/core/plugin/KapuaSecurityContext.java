/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates and others
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
import java.util.HashSet;
import java.util.Set;

import org.apache.activemq.security.AuthorizationMap;
import org.apache.activemq.security.SecurityContext;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.security.KapuaSession;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.service.authentication.KapuaPrincipal;

/**
 * Kapua security context implementation of ActiveMQ broker {@link SecurityContext}
 *
 * @since 1.0
 */
public class KapuaSecurityContext extends SecurityContext {

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

    private AuthorizationMap authMap;

    private KapuaConnectionContext kcc;

    //flag to help the correct lifecycle handling
    private boolean missing;

    public KapuaSecurityContext(KapuaConnectionContext kcc,
            AuthorizationMap authMap) {
        super(kcc.getPrincipal().getName());

        this.kcc = kcc;
        this.principal = kcc.getPrincipal();
        this.kapuaSession = KapuaSession.createFrom();
        principals = new HashSet<Principal>();
        principals.add(principal);

        this.authMap = authMap;
        kapuaConnectionId = kcc.getKapuaConnectionId();
        this.connectorDescriptor = kcc.getConnectorDescriptor();
        connectionId = kcc.getConnectionId();
    }

    public KapuaPrincipal getKapuaPrincipal() throws KapuaException {
        return principal;
    }

    public KapuaConnectionContext getKapuaConnectionContext() {
        return kcc;
    }

    public Principal getMainPrincipal() {
        return principal;
    }

    public Set<Principal> getPrincipals() {
        return principals;
    }

    public AuthorizationMap getAuthorizationMap() {
        return authMap;
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

}
