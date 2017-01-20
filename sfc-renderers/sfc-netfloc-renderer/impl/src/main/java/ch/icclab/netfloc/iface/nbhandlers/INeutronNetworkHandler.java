/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.iface.nbhandlers;

import org.opendaylight.neutron.spi.NeutronNetwork;

public interface INeutronNetworkHandler {
    /**
     * Invoked to take action after a network has been created.
     *
     * @param network  An instance of new Neutron Network object.
     */
    public void neutronNetworkCreated(NeutronNetwork network);

    /**
     * Invoked to take action after a network has been updated.
     *
     * @param network An instance of modified Neutron Network object.
     */
    public void neutronNetworkUpdated(NeutronNetwork network);

    /**
     * Invoked to take action after a network has been deleted.
     *
     * @param network  An instance of deleted Neutron Network object.
     */
    public void neutronNetworkDeleted(NeutronNetwork network);
}