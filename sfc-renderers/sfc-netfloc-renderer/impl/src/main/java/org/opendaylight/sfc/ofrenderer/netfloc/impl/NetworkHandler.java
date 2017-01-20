/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.netfloc.impl;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.nbhandlers.INeutronNetworkHandler;
import org.opendaylight.neutron.spi.INeutronNetworkAware;
import org.opendaylight.neutron.spi.INeutronNetworkCRUD;
import org.opendaylight.neutron.spi.NeutronNetwork;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.HttpURLConnection;
import java.util.List;

/**
 * Handles requests for Neutron Network.
 */
public class NetworkHandler implements INeutronNetworkAware {
    private static final Logger logger = LoggerFactory.getLogger(NetworkHandler.class);
    public static final String NETWORK_TYPE_VXLAN = "vxlan";
    public static final String NETWORK_TYPE_GRE = "gre";
    public static final String NETWORK_TYPE_VLAN = "vlan";
    public static final String NETWORK_TYPE_NETFLOC = "netfloc";
    private INeutronNetworkHandler netHandler;

    public NetworkHandler(INeutronNetworkHandler netHandler) {
      this.netHandler = netHandler;
    }

    /**
     * Invoked when a network creation is requested
     * to indicate if the specified network can be created.
     *
     * @param network  An instance of proposed new Neutron Network object.
     * @return A HTTP status code to the creation request.
     */
    @Override
    public int canCreateNetwork(NeutronNetwork network) {
      if (network.isShared()) {
          logger.error("NetworkHandler Network shared attribute not supported ");
          return HttpURLConnection.HTTP_NOT_ACCEPTABLE;
      }
      return HttpURLConnection.HTTP_OK;
    }

    /**
     * Invoked to take action after a network has been created.
     *
     * @param network  An instance of new Neutron Network object.
     */
    @Override
    public void neutronNetworkCreated(NeutronNetwork network) {
      logger.info("NetworkHandler Neutron network {} created ", network);
      this.netHandler.neutronNetworkCreated(network);
    }

    /**
     * Invoked when a network update is requested
     * to indicate if the specified network can be changed
     * using the specified delta.
     *
     * @param delta     Updates to the network object using patch semantics.
     * @param original  An instance of the Neutron Network object
     *                  to be updated.
     * @return A HTTP status code to the update request.
     */
    @Override
    public int canUpdateNetwork(NeutronNetwork delta,
                                NeutronNetwork original) {
      if (delta.isShared()) {
          logger.error("NetworkHandler Network shared attribute not supported ");
          return HttpURLConnection.HTTP_NOT_ACCEPTABLE;
      }
      return HttpURLConnection.HTTP_OK;
    }

    /**
     * Invoked to take action after a network has been updated.
     *
     * @param network An instance of modified Neutron Network object.
     */
    @Override
    public void neutronNetworkUpdated(NeutronNetwork network) {
      logger.info("NetworkHandler Neutron network {} updated ", network);
      this.netHandler.neutronNetworkUpdated(network);
    }

    /**
     * Invoked when a network deletion is requested
     * to indicate if the specified network can be deleted.
     *
     * @param network  An instance of the Neutron Network object to be deleted.
     * @return A HTTP status code to the deletion request.
     */
    @Override
    public int canDeleteNetwork(NeutronNetwork network) {
      logger.info("NetworkHandler Can delete network");
      return HttpURLConnection.HTTP_OK;
    }

    /**
     * Invoked to take action after a network has been deleted.
     *
     * @param network  An instance of deleted Neutron Network object.
     */
    @Override
    public void neutronNetworkDeleted(NeutronNetwork network) {
      logger.info("NetworkHandler Neutron network {} deleted ", network);
      this.netHandler.neutronNetworkDeleted(network);
    }
}
