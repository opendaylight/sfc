/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.netfloc.impl;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.nbhandlers.INeutronRouterHandler;
import org.opendaylight.neutron.spi.INeutronRouterAware;
import org.opendaylight.neutron.spi.NeutronRouter;
import org.opendaylight.neutron.spi.NeutronRouter_Interface;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.HttpURLConnection;

/**
 * Handle requests for Neutron Router.
 */
public class RouterHandler implements INeutronRouterAware {
    static final Logger logger = LoggerFactory.getLogger(RouterHandler.class);
    private INeutronRouterHandler handler;
    public RouterHandler(INeutronRouterHandler handler) {
        this.handler = handler;
    }

    /**
     * Services provide this interface method to indicate if the specified router can be created
     *
     * @param router
     *            instance of proposed new Neutron Router object
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the create operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    @Override
    public int canCreateRouter(NeutronRouter router) {
        logger.info("RouterHandler Can create neutron router");
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Services provide this interface method for taking action after a router has been created
     *
     * @param router
     *            instance of new Neutron Router object
     */
    @Override
    public void neutronRouterCreated(NeutronRouter router) {
        logger.info("RouterHandler Neutron router created");
    }

    /**
     * Services provide this interface method to indicate if the specified router can be changed using the specified
     * delta
     *
     * @param delta
     *            updates to the router object using patch semantics
     * @param original
     *            instance of the Neutron Router object to be updated
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the update operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    @Override
    public int canUpdateRouter(NeutronRouter delta, NeutronRouter original) {
        logger.info("RouterHandler Can update router");
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Services provide this interface method for taking action after a router has been updated
     *
     * @param router
     *            instance of modified Neutron Router object
     */
    @Override
    public void neutronRouterUpdated(NeutronRouter router) {
        logger.info("RouterHandler Neutron router updated");
    }

    /**
     * Services provide this interface method to indicate if the specified router can be deleted
     *
     * @param router
     *            instance of the Neutron Router object to be deleted
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the delete operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    @Override
    public int canDeleteRouter(NeutronRouter router) {
        logger.info("RouterHandler Can delete router");
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Services provide this interface method for taking action after a router has been deleted
     *
     * @param router
     *            instance of deleted Router Network object
     */
    @Override
    public void neutronRouterDeleted(NeutronRouter router) {
        logger.info("RouterHandler Neutron router deleted");
    }

    /**
     * Services provide this interface method to indicate if the specified interface can be attached to the specified
     * route
     *
     * @param router
     *            instance of the base Neutron Router object
     * @param routerInterface
     *            instance of the NeutronRouter_Interface to be attached to the router
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the attach operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    @Override
    public int canAttachInterface(NeutronRouter router, NeutronRouter_Interface routerInterface) {
        logger.info("RouterHandler Router {} asked if it can attach interface {}. Subnet {}",
                     router.getName(),
                     routerInterface.getPortUUID(),
                     routerInterface.getSubnetUUID());
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Services provide this interface method for taking action after an interface has been added to a router
     *
     * @param router
     *            instance of the base Neutron Router object
     * @param routerInterface
     *            instance of the NeutronRouter_Interface being attached to the router
     */
    @Override
    public void neutronRouterInterfaceAttached(NeutronRouter router, NeutronRouter_Interface routerInterface) {
        logger.info("RouterHandler Neutron router interface attached");
    }

    /**
     * Services provide this interface method to indicate if the specified interface can be detached from the specified
     * router
     *
     * @param router
     *            instance of the base Neutron Router object
     * @param routerInterface
     *            instance of the NeutronRouter_Interface to be detached to the router
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the detach operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    @Override
    public int canDetachInterface(NeutronRouter router, NeutronRouter_Interface routerInterface) {
        logger.debug("RouterHandler Router {} asked if it can detach interface {}. Subnet {}",
                     router.getName(),
                     routerInterface.getPortUUID(),
                     routerInterface.getSubnetUUID());

        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Services provide this interface method for taking action after an interface has been removed from a router
     *
     * @param router
     *            instance of the base Neutron Router object
     * @param routerInterface
     *            instance of the NeutronRouter_Interface being detached from the router
     */
    @Override
    public void neutronRouterInterfaceDetached(NeutronRouter router, NeutronRouter_Interface routerInterface) {
        logger.info("RouterHandler Neutron router interface detached");
    }
}
