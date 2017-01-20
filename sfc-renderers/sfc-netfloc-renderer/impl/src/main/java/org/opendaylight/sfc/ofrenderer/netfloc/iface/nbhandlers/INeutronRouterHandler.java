/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.netfloc.iface.nbhandlers;

import org.opendaylight.neutron.spi.NeutronRouter;

public interface INeutronRouterHandler {
    /**
     * Invoked to take action after a router has been created.
     *
     * @param router  An instance of new Neutron Router object.
     */
    public void neutronRouterCreated(NeutronRouter router);

    /**
     * Invoked to take action after a router has been updated.
     *
     * @param router An instance of modified Neutron Router object.
     */
    public void neutronRouterUpdated(NeutronRouter router);

    /**
     * Invoked to take action after a router has been deleted.
     *
     * @param router  An instance of deleted Neutron Router object.
     */
    public void neutronRouterDeleted(NeutronRouter router);
}
