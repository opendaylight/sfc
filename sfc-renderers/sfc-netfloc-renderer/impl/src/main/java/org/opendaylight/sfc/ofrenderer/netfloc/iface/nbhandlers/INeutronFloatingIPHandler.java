/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.netfloc.iface.nbhandlers;

import org.opendaylight.neutron.spi.NeutronFloatingIP;

public interface INeutronFloatingIPHandler {
    /**
     * Invoked to take action after a floatingIP has been created.
     *
     * @param floatingIP  An instance of new Neutron FloatingIP object.
     */
    public void neutronFloatingIPCreated(NeutronFloatingIP floatingIP);

    /**
     * Invoked to take action after a floatingIP has been updated.
     *
     * @param floatingIP An instance of modified Neutron FloatingIP object.
     */
    public void neutronFloatingIPUpdated(NeutronFloatingIP floatingIP);

    /**
     * Invoked to take action after a floatingIP has been deleted.
     *
     * @param floatingIP  An instance of deleted Neutron FloatingIP object.
     */
    public void neutronFloatingIPDeleted(NeutronFloatingIP floatingIP);
}
