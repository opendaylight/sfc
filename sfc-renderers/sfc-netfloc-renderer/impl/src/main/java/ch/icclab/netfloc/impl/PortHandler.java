/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.impl;
import org.opendaylight.neutron.spi.INeutronPortAware;
import org.opendaylight.neutron.spi.NeutronPort;
import ch.icclab.netfloc.iface.nbhandlers.INeutronPortHandler;
import java.net.HttpURLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles requests for Neutron Port.
 */
public class PortHandler implements INeutronPortAware {
	static final Logger logger = LoggerFactory.getLogger(PortHandler.class);
  private INeutronPortHandler network;
  public PortHandler(INeutronPortHandler network) {
      this.network = network;
  }

	/**
     * Invoked when a port creation is requested
     * to indicate if the specified port can be created.
     *
     * @param port An instance of proposed new Port object.
     * @return A HTTP status code to the creation request.
     */
    @Override
    public int canCreatePort(NeutronPort port) {
      logger.info("PortHandler Can create port");
      return HttpURLConnection.HTTP_OK;
    }

    /**
     * Invoked to take action after a port has been created.
     *
     * @param neutronPort An instance of new Neutron Port object.
     */
    @Override
    public void neutronPortCreated(NeutronPort neutronPort) {
      logger.info("PortHandler Neutron port {} created", neutronPort);
      network.neutronPortCreated(neutronPort);
    }

    /**
     * Invoked when a port update is requested
     * to indicate if the specified port can be changed
     * using the specified delta.
     *
     * @param delta	Updates to the port object using patch semantics.
     * @param original An instance of the Neutron Port object
     *                  to be updated.
     * @return A HTTP status code to the update request.
     */
    @Override
    public int canUpdatePort(NeutronPort delta,
                           NeutronPort original) {
      logger.info("PortHandler Can update port");
      return HttpURLConnection.HTTP_OK;
    }

    /**
     * Invoked to take action after a port has been updated.
     *
     * @param neutronPort An instance of modified Neutron Port object.
     */
    @Override
    public void neutronPortUpdated(NeutronPort neutronPort) {
      logger.info("PortHandler Neutron port {} updated ", neutronPort);
    }

    /**
     * Invoked when a port deletion is requested
     * to indicate if the specified port can be deleted.
     *
     * @param port An instance of the Neutron Port object to be deleted.
     * @return A HTTP status code to the deletion request.
     */
    @Override
    public int canDeletePort(NeutronPort port) {
      logger.info("PortHandler Can delete port");
      return HttpURLConnection.HTTP_OK;
    }

    /**
     * Invoked to take action after a port has been deleted.
     *
     * @param neutronPort	An instance of deleted Neutron Port object.
     */
    @Override
    public void neutronPortDeleted(NeutronPort neutronPort) {
      logger.info("PortHandler Neutron port {} deleted ", neutronPort);
    }
}
