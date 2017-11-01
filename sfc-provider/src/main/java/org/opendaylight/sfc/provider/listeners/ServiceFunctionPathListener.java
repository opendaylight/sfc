/*
 * Copyright (c) 2017 Inocybe Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.listeners;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to changes (addition, update, removal) in Service
 * Function Paths, taking the appropriate actions.
 */
@Singleton
public class ServiceFunctionPathListener extends AbstractSyncDataTreeChangeListener<ServiceFunctionPath> {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceFunctionPathListener.class);

    @Inject
    public ServiceFunctionPathListener(final DataBroker dataBroker) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
              InstanceIdentifier.create(ServiceFunctionPaths.class).child(ServiceFunctionPath.class));
    }

    @Override
    public void add(@Nonnull ServiceFunctionPath serviceFunctionPath) {
        LOG.info("add: SFP: {}", serviceFunctionPath.getName().getValue());

        RenderedServicePath newRenderedServicePath = SfcProviderRenderedPathAPI
                 .createRenderedServicePathInConfig(serviceFunctionPath);
        if (newRenderedServicePath == null) {
            LOG.error("Failed to create Rendered Service Path {}", serviceFunctionPath.getName().getValue());
            return;
        }

        // Optionally create the Symmetric RSP
        if (SfcProviderRenderedPathAPI.isChainSymmetric(serviceFunctionPath, newRenderedServicePath)) {
            RenderedServicePath revRenderedServicePath = SfcProviderRenderedPathAPI
                    .createSymmetricRenderedServicePathInConfig(newRenderedServicePath);
            if (revRenderedServicePath == null) {
                LOG.error("Failed to create symmetric Rendered Service Path for input SFP: {}",
                        serviceFunctionPath.getName().getValue());
            } else {
                SfcProviderRenderedPathAPI.setSymmetricPathId(revRenderedServicePath,
                        revRenderedServicePath.getPathId());
            }
        }
    }

    @Override
    public void remove(@Nonnull ServiceFunctionPath serviceFunctionPath) {
        LOG.info("remove: Deleting SFP: {}", serviceFunctionPath.getName().getValue());

        // TODO finish this

    }

    @Override
    public void update(@Nonnull ServiceFunctionPath originalServiceFunctionPath,
                       ServiceFunctionPath updatedServiceFunctionPath) {
        LOG.info("Update SFP: {}", updatedServiceFunctionPath.getName().getValue());

        if (!originalServiceFunctionPath.getName().equals(updatedServiceFunctionPath.getName())) {
            LOG.warn("Updating the SFP name is not supported: [{}] [{}]",
                    originalServiceFunctionPath.getName().getValue(), updatedServiceFunctionPath.getName().getValue());
            return;
        }

        if (!originalServiceFunctionPath.getServiceChainName().equals(
                updatedServiceFunctionPath.getServiceChainName())) {
            LOG.warn("Updating the SFP SFC is not supported: SFP [{}] old SFC [{}] new SFC [{}]",
                    originalServiceFunctionPath.getName().getValue(),
                    originalServiceFunctionPath.getServiceChainName().getValue(),
                    updatedServiceFunctionPath.getServiceChainName().getValue());
            return;
        }

        add(updatedServiceFunctionPath);
    }
}
