/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.iface.nbhandlers;

import org.opendaylight.neutron.spi.NeutronSubnet;

public interface INeutronSubnetHandler {

    /**
     * Services provide this interface method for taking action after a subnet has been created
     *
     * @param subnet
     *            instance of new Neutron Subnet object
     */
    public void neutronSubnetCreated(NeutronSubnet subnet);

    /**
     * Services provide this interface method for taking action after a subnet has been updated
     *
     * @param subnet
     *            instance of modified Neutron Subnet object
     */
    public void neutronSubnetUpdated(NeutronSubnet subnet);

    /**
     * Services provide this interface method for taking action after a subnet has been deleted
     *
     * @param subnet
     *            instance of deleted Neutron Subnet object
     */
    public void neutronSubnetDeleted(NeutronSubnet subnet);
}
