/*
 * Copyright (c) 2014 Contextream, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.lisp;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.lispflowmapping.interfaces.lisp.IFlowMapping;
import org.opendaylight.sfc.sfc_lisp.provider.ILispUpdater;
import org.opendaylight.sfc.sfc_lisp.provider.LispUpdater;

/**
 * Main application activator class for registering the dependencies.
 *
 */

public class Activator extends ComponentActivatorAbstractBase {

    /**
     * Function called when the activator starts just after some initializations
     * are done by the ComponentActivatorAbstractBase.
     *
     */
    @Override
    public void init() {
    }

    /**
     * Function called when the activator stops just before the cleanup done by
     * ComponentActivatorAbstractBase
     *
     */
    @Override
    public void destroy() {
    }

    /**
     * Function that is used to communicate to dependency manager the list of
     * known implementations for services inside a container
     *
     *
     * @return An array containing all the CLASS objects that will be
     *         instantiated in order to get an fully working implementation
     *         Object
     */
    @Override
    public Object[] getImplementations() {
        Object[] res = { LispUpdater.class, SfcLispListener.class };
        return res;
    }

    /**
     * Function that is called when configuration of the dependencies is
     * required.
     *
     * @param c
     *            dependency manager Component object, used for configuring the
     *            dependencies exported and imported
     * @param imp
     *            Implementation class that is being configured, needed as long
     *            as the same routine can configure multiple implementations
     * @param containerName
     *            The containerName being configured, this allow also optional
     *            per-container different behavior if needed, usually should not
     *            be the case though.
     */
    @Override
    public void configureInstance(Component c, Object imp, String containerName) {
        if (imp.equals(LispUpdater.class)) {
            // export the service
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("name", LispUpdater.class.getSimpleName());
            c.setInterface(new String[] { ILispUpdater.class.getName() }, props);
            c.add(createContainerServiceDependency(containerName).setService(IFlowMapping.class).setCallbacks("setFlowMapping", "unsetFlowMapping")
                    .setRequired(true));
        }
        if (imp.equals(SfcLispListener.class)) {
            // export the service
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("name", SfcLispListener.class.getSimpleName());
            c.setInterface(new String[] { ISfcLispListener.class.getName() }, props);
            c.add(createContainerServiceDependency(containerName).setService(ILispUpdater.class).setCallbacks("setLispUpdater", "unsetLispUpdater")
                    .setRequired(true));
            c.add(createServiceDependency().setService(BindingAwareBroker.class).setRequired(true)
                    .setCallbacks("setBindingAwareBroker", "unsetBindingAwareBroker"));
        }
    }


    /**
     * Method which tells how many Global implementations are supported by the
     * bundle. This way we can tune the number of components created. This
     * components will be created ONLY at the time of bundle startup and will be
     * destroyed only at time of bundle destruction, this is the major
     * difference with the implementation retrieved via getImplementations where
     * all of them are assumed to be in a container !
     *
     *
     * @return The list of implementations the bundle will support, in Global
     *         version
     */
    @Override
    protected Object[] getGlobalImplementations() {
        return null;
    }

    /**
     * Configure the dependency for a given instance Global
     *
     * @param c
     *            Component assigned for this instance, this will be what will
     *            be used for configuration
     * @param imp
     *            implementation to be configured
     */
    @Override
    protected void configureGlobalInstance(Component c, Object imp) {

    }
}
