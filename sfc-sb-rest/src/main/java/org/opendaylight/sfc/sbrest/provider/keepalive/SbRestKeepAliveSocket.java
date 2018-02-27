/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sbrest.provider.keepalive;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class creates Keep Alive Socket which is used by SFC_AGENT or other REST
 * clients to track status of ODL (e.g. signalize restarts)
 *
 * @author Andrej Kincel (akincel@cisco.com)
 * @version 0.1
 * @see org.opendaylight.sfc.sbrest.provider.keepalive.SbRestKeepAliveSocket
 * @since 2015-03-09
 */
@Singleton
public class SbRestKeepAliveSocket implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(SbRestKeepAliveSocket.class);
    private static final int KEEP_ALIVE_LISTENER_PORT = 9999;
    private static final int RETRY_DELAY = 5;
    private final ScheduledExecutorService executorService =
            Executors.newSingleThreadScheduledExecutor("SbRestKeepAliveSocket", LOG);

    @PostConstruct
    public void accept() {
        executorService.scheduleWithFixedDelay(this, 0, RETRY_DELAY, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void close() {
        executorService.shutdownNow();
    }

    @Override
    public void run() {

        List<Socket> clientSocketList = new ArrayList<>();

        // try-resource block closes the serverSocket automatically
        try (ServerSocket serverSocket = new ServerSocket(KEEP_ALIVE_LISTENER_PORT)) {
            LOG.info("Created Keep Alive Socket on port {}", KEEP_ALIVE_LISTENER_PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientSocketList.add(clientSocket);
                LOG.info("SB REST client/agent connected to Keep Alive {}", clientSocket.toString());
            }
        } catch (ClosedByInterruptException e) {
            LOG.info("Closing Keep Alive Socket on port {}", KEEP_ALIVE_LISTENER_PORT);
        } catch (IOException e) {
            LOG.debug("Cannot create Keep Alive Socket on port {}", KEEP_ALIVE_LISTENER_PORT, e);
        } finally {
            for (Socket clientSocket : clientSocketList) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    LOG.warn("Cannot close Client connection: {} to Keep Alive Socket", clientSocket.toString());
                }
            }
        }
    }
}
