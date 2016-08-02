/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl;


import org.opendaylight.controller.md.sal.binding.api.DataBroker;

/**
 * This class is the entry point to perform genius interface handling
 * associated to the service functions.
 */
public class SfcGeniusSfInterfaceManager {

    private final DataBroker dataBroker;

    public SfcGeniusSfInterfaceManager(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    /**
     * Binds SFC service to interface. If the interface is UP and is the first
     * bound interface for a node, the SFC service terminating service action
     * is created on such node.
     *
     * @param interfaceName
     */
    public void bindToInterface(String interfaceName) {
        // TODO implement
    }

    /**
     * Unbinds SFC service to interface. If the interface is UP and is the last
     * bound interface for a node, the SFC service terminating service action
     * is removed from such node.
     *
     * @param interfaceName
     */
    public void unbindFromInterface(String interfaceName) {
        // TODO implement
    }


    /**
     * Notifies a interface change of state to UP. This should
     * be done when the node associated to an interface becomes known.
     * If the interface has been bound to, some related tasks will be
     * performed:
     *
     * - RSPs associated to such nodes will be re-rendered.
     * - If the interface is the fist known for the node, SFC terminating
     *   service action will be added to such node.
     *
     * @param interfaceName
     */
    public void setInterfaceUp(String interfaceName) {
        // TODO implement
    }

    /**
     * Notifies a interface change of state to UP. This should
     * be done when the node associated to an interface becomes unknown.
     * If the interface has been bound to, some related tasks will be
     * performed:
     *
     * - If the interface is the last known for the node, SFC terminating
     *   service action will be added to such node.
     *
     * Note that this scenario is considered a VM migration and the
     * associated RSPs will not be handled with until the corresponding
     * interface up event is handled.
     *
     * @param interfaceName
     */
    public void setInterfaceDown(String interfaceName) {
        // TODO implement
    }
}
