/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.netfloc.impl;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.nbhandlers.INeutronSubnetHandler;
import org.opendaylight.neutron.spi.INeutronSubnetAware;
import org.opendaylight.neutron.spi.NeutronSubnet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.HttpURLConnection;

/**
 * Handles requests for Neutron Subnet.
 */
public class SubnetHandler implements INeutronSubnetAware {

    static final Logger logger = LoggerFactory.getLogger(SubnetHandler.class);
    private INeutronSubnetHandler network;

    public SubnetHandler(INeutronSubnetHandler network) {
      this.network = network;
    }

    @Override
    public int canCreateSubnet(NeutronSubnet subnet) {
      logger.info("SubnetHandler Can create subnet");
      return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronSubnetCreated(NeutronSubnet subnet) {
      logger.info("SubnetHandler Neutron subnet {} created ", subnet);
      network.neutronSubnetCreated(subnet);
    }

    @Override
    public int canUpdateSubnet(NeutronSubnet delta, NeutronSubnet original) {
      logger.info("SubnetHandler Can update subnet");
      return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronSubnetUpdated(NeutronSubnet subnet) {
      logger.info("SubnetHandler Neutron subnet {} updated ", subnet);
      network.neutronSubnetUpdated(subnet);
    }

    @Override
    public int canDeleteSubnet(NeutronSubnet subnet) {
      logger.info("SubnetHandler Can delete subnet");
      return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronSubnetDeleted(NeutronSubnet subnet) {
      logger.info("SubnetHandler Neutron subnet {} deleted ", subnet);
      network.neutronSubnetDeleted(subnet);
    }
}
