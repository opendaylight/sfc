/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.iface.nbhandlers;

import org.opendaylight.neutron.spi.NeutronPort;

public interface INeutronPortHandler {


    /**
     * Services provide this interface method for taking action after a port has been created
     *
     * @param port
     *            instance of new Neutron Port object
     */
    public void neutronPortCreated(NeutronPort port);

    /**
     * Services provide this interface method for taking action after a port has been updated
     *
     * @param port
     *            instance of modified Neutron Port object
     */
    public void neutronPortUpdated(NeutronPort port);

    /**
     * Services provide this interface method for taking action after a port has been deleted
     *
     * @param port
     *            instance of deleted Port Network object
     */
    public void neutronPortDeleted(NeutronPort port);
}