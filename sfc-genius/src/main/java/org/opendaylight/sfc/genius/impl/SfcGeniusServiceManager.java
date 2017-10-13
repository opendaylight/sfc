/*
 * Copyright (c) 2017 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl;

import java.math.BigInteger;
import java.util.List;

/**
 * Genius SFC Service manager.
 *
 * <p>
 * Service functions can be configured with logical interface data plane
 * locators managed by Genius. When such service functions are included in a
 * rendered service path and to be able to steer downlink traffic from the
 * service function, we need to bind SFC service to the interface through
 * Genius. Logical interfaces are associated to a data plane node and port. Such
 * nodes receive incoming traffic from other nodes through tunnel ports managed
 * by Genius. To be able to steer SFC uplink traffic towards the service
 * functions, we need to bind SFC service to the node through Genius.
 *
 * <p>
 * Logical interfaces dynamically move from one node/port to another. On doing
 * so, they become temporarily unavailable.
 */
public interface SfcGeniusServiceManager {

    /**
     * Interface becomes available at a node.
     *
     * @param interfaceName
     *            the name of the interface.
     * @param dpnId
     *            the data plane id of the new node associated to the interface.
     */
    void interfaceStateUp(String interfaceName, BigInteger dpnId);

    /**
     * Bind the interfaces of a service function to SFC service.
     *
     * @param sfName
     *            the name of the service function.
     */
    void bindInterfacesOfServiceFunction(String sfName);

    /**
     * Unbind the interfaces of a service function to SFC service.
     *
     * @param sfName
     *            the name of the service function.
     */
    void unbindInterfacesOfServiceFunction(String sfName);

    /**
     * Unbind the specified interfaces.
     *
     * @param interfaceNames
     *            a list containing the name of the interfaces
     */
    void unbindInterfaces(List<String> interfaceNames);

    /**
     * Bind node to SFC service.
     *
     * @param dpnId
     *            the node DPN Id.
     */
    void bindNode(BigInteger dpnId);

    /**
     * Unbind node from SFC service.
     *
     * @param dpnId
     *            the node DPN Id.
     */
    void unbindNode(BigInteger dpnId);
}
