/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.impl;

import ch.icclab.netfloc.iface.nbhandlers.INeutronFloatingIPHandler;
import org.opendaylight.neutron.spi.INeutronFloatingIPAware;
import org.opendaylight.neutron.spi.NeutronFloatingIP;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.HttpURLConnection;
/**
 * Handle requests for Neutron Floating IP.
 */
public class FloatingIPHandler implements INeutronFloatingIPAware {

  static final Logger logger = LoggerFactory.getLogger(FloatingIPHandler.class);

  private INeutronFloatingIPHandler handler;

  public FloatingIPHandler(INeutronFloatingIPHandler handler) {
      this.handler = handler;
  }

  // The implementation for each of these services is resolved by the OSGi Service Manager

  /**
   * Services provide this interface method to indicate if the specified floatingIP can be created
   *
   * @param floatingIP
   *            instance of Neutron FloatingIP object
   * @return integer
   *            the return value is HTTP status code.  A return value outside of 200 through 299
   *            results in the create operation being interrupted and the returned status value reflected in the
   *            HTTP response.
   */
  @Override
  public int canCreateFloatingIP(NeutronFloatingIP floatingIP) {
      logger.info("can create floating ip");
      return HttpURLConnection.HTTP_OK;
  }


  /**
   * Services provide this interface method for taking action after a floatingIP has been created
   *
   * @param floatingIP
   *            instance of new Neutron FloatingIP object
   */
  @Override
  public void neutronFloatingIPCreated(NeutronFloatingIP floatingIP) {
      logger.info("neutron floating ip created");
      handler.neutronFloatingIPCreated(floatingIP);
  }

  /**
   * Services provide this interface method to indicate if the specified floatingIP can be changed using the specified
   * delta
   *
   * @param delta
   *            updates to the floatingIP object using patch semantics
   * @param original
   *            instance of the Neutron FloatingIP object to be updated
   * @return integer
   *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
   *            results in the update operation being interrupted and the returned status value reflected in the
   *            HTTP response.
   */
  @Override
  public int canUpdateFloatingIP(NeutronFloatingIP delta, NeutronFloatingIP original) {
      logger.info("can update floating ip");
      return HttpURLConnection.HTTP_OK;
  }

  /**
   * Services provide this interface method for taking action after a floatingIP has been updated
   *
   * @param floatingIP
   *            instance of modified Neutron FloatingIP object
   */
  @Override
  public void neutronFloatingIPUpdated(NeutronFloatingIP floatingIP) {
      logger.info("neutron floating ip updated");
      handler.neutronFloatingIPUpdated(floatingIP);
  }

  /**
   * Services provide this interface method to indicate if the specified floatingIP can be deleted
   *
   * @param floatingIP
   *            instance of the Neutron FloatingIP object to be deleted
   * @return integer
   *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
   *            results in the delete operation being interrupted and the returned status value reflected in the
   *            HTTP response.
   */
  @Override
  public int canDeleteFloatingIP(NeutronFloatingIP floatingIP) {
      logger.info("can delete floating ip");
      return HttpURLConnection.HTTP_OK;
  }

  /**
   * Services provide this interface method for taking action after a floatingIP has been deleted
   *
   * @param floatingIP
   *            instance of deleted Neutron FloatingIP object
   */
  @Override
  public void neutronFloatingIPDeleted(NeutronFloatingIP floatingIP) {
      logger.info("neutron floating ip deleted");
      handler.neutronFloatingIPDeleted(floatingIP);
  }
}
