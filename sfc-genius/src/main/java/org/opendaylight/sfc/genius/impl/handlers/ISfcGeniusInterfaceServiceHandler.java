/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers;

import java.math.BigInteger;

/**
 * Handles logical interfaces in relation with SFC service.
 *
 * Service functions can be configured with logical interface data plane
 * locators. When such service functions are included in a rendered
 * service path and to be able to steer traffic towards them, certain
 * actions need to be taken in regards to these logical interfaces,
 * referred to as binding SFC service to the interface.
 *
 * Logical interfaces are associated to a data plane node and port. They may
 * dynamically move from one node/port to another. On doing so, they become
 * temporarily unavailable.
 */
public interface ISfcGeniusInterfaceServiceHandler {

    /**
     * Interface becomes available at a node.
     *
     * @param interfaceName the name of the interface
     * @param dpnId the data plane id of the new node associated to the
     *              interface
     */
    void interfaceStateUp(String interfaceName, BigInteger dpnId);

    /**
     * Interface becomes unavailable on a node.
     *
     * @param interfaceName the name of the interface
     * @param dpnId the data plane id of the old node associated to the
     *              interface
     */
    void interfaceStateDown(String interfaceName, BigInteger dpnId);

    /**
     * Bind the interfaces of a service function to SFC service
     *
     * @param sfName the name of the service function
     */
    void bindInterfacesOfServiceFunction(String sfName);

    /**
     * Unbind the interfaces of a service function to SFC service
     *
     * @param sfName the name of the service function
     */
    void unbindInterfacesOfServiceFunction(String sfName);
}
