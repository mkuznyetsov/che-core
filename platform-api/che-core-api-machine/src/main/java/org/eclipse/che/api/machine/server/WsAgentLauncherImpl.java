/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.machine.server;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.rest.HttpJsonRequest;
import org.eclipse.che.api.core.rest.HttpJsonRequestFactory;
import org.eclipse.che.api.core.rest.HttpJsonResponse;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.model.impl.CommandImpl;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

/**
 * Starts ws agent in the machine and waits until ws agent sends notification about its start
 *
 * @author Alexander Garagatyi
 */
@Singleton
public class WsAgentLauncherImpl implements WsAgentLauncher {
    public static final String WS_AGENT_PROCESS_START_COMMAND  = "machine.ws_agent.run_command";
    public static final String WS_AGENT_PROCESS_NAME           = "CheWsAgent";
    public static final int    WS_AGENT_PORT                   = 4401;

    private static final Logger LOG                             = LoggerFactory.getLogger(WsAgentLauncherImpl.class);
    private static final String WS_AGENT_PROCESS_OUTPUT_CHANNEL = "workspace:%s:ext-server:output";

    private final MachineManager         machineManager;
    private final HttpJsonRequestFactory httpJsonRequestFactory;
    private final String                 wsAgentStartCommandLine;
    private final long                   wsAgentMaxStartTimeMs;
    private final long                   wsAgentPingDelayMs;
    private final int                    wsAgentPingConnectionTimeoutMs;
    private final String                 wsAgentPingPath;

    @Inject
    public WsAgentLauncherImpl(MachineManager machineManager,
                               HttpJsonRequestFactory httpJsonRequestFactory,
                               @Named(WS_AGENT_PROCESS_START_COMMAND) String wsAgentStartCommandLine,
                               @Named("api.endpoint") URI apiEndpoint,
                               @Named("machine.ws_agent.max_start_time_ms") long wsAgentMaxStartTimeMs,
                               @Named("machine.ws_agent.ping_delay_ms") long wsAgentPingDelayMs,
                               @Named("machine.ws_agent.ping_conn_timeout_ms") int wsAgentPingConnectionTimeoutMs) {
        this.machineManager = machineManager;
        this.httpJsonRequestFactory = httpJsonRequestFactory;
        this.wsAgentStartCommandLine = wsAgentStartCommandLine;
        this.wsAgentMaxStartTimeMs = wsAgentMaxStartTimeMs;
        this.wsAgentPingDelayMs = wsAgentPingDelayMs;
        this.wsAgentPingConnectionTimeoutMs = wsAgentPingConnectionTimeoutMs;
        // everest respond 404 to path to rest without trailing slash
        this.wsAgentPingPath = apiEndpoint.getPath().endsWith("/") ? apiEndpoint.getPath() : apiEndpoint.getPath() + "/";
    }

    public static String getWsAgentProcessOutputChannel(String workspaceId) {
        return String.format(WS_AGENT_PROCESS_OUTPUT_CHANNEL, workspaceId);
    }

    @Override
    public void startWsAgent(String workspaceId) throws NotFoundException, MachineException, InterruptedException {
        final Instance devMachine = machineManager.getDevMachine(workspaceId);
        try {
            machineManager.exec(devMachine.getId(),
                                new CommandImpl(WS_AGENT_PROCESS_NAME, wsAgentStartCommandLine, "Arbitrary"),
                                getWsAgentProcessOutputChannel(workspaceId));

            final HttpJsonRequest wsAgentPingRequest = createPingRequest(devMachine);

            long pingStartTimestamp = System.currentTimeMillis();
            LOG.debug("Starts pinging ws agent. Workspace ID:{}. Url:{}. Timestamp:{}",
                      workspaceId,
                      wsAgentPingRequest,
                      pingStartTimestamp);

            while (System.currentTimeMillis() - pingStartTimestamp < wsAgentMaxStartTimeMs) {
                if (pingWsAgent(wsAgentPingRequest)) {
                    return;
                } else {
                    Thread.sleep(wsAgentPingDelayMs);
                }
            }
        } catch (BadRequestException wsAgentLaunchingExc) {
            throw new MachineException(wsAgentLaunchingExc.getLocalizedMessage(), wsAgentLaunchingExc);
        }
        throw new MachineException("Workspace agent is not responding. Workspace " + workspaceId + " will be stopped");
    }

    private HttpJsonRequest createPingRequest(Instance devMachine) {
        final String wsAgentPingUrl = UriBuilder.fromUri(devMachine.getMetadata()
                                                                   .getServers()
                                                                   .get(Integer.toString(WS_AGENT_PORT))
                                                                   .getUrl())
                                                .replacePath(wsAgentPingPath)
                                                .build()
                                                .toString();
        return httpJsonRequestFactory.fromUrl(wsAgentPingUrl)
                                     .setMethod(HttpMethod.OPTIONS)
                                     .setTimeout(wsAgentPingConnectionTimeoutMs);
    }

    private boolean pingWsAgent(HttpJsonRequest wsAgentPingRequest) throws MachineException {
        try {
            final HttpJsonResponse pingResponse = wsAgentPingRequest.request();
            if (pingResponse.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return true;
            }
        } catch (ApiException | IOException ignored) {
        }
        return false;
    }
}