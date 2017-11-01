/*
 * Copyright (c) 2017 Inocybe Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.listeners;

import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.listeners.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePath;
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

        // Internally, this will optionally create the symmetric RSP
        RenderedServicePath newRenderedServicePath = SfcProviderRenderedPathAPI
                 .createRenderedServicePathInConfig(serviceFunctionPath);
        if (newRenderedServicePath == null) {
            LOG.error("Failed to create Rendered Service Path {}", serviceFunctionPath.getName().getValue());
        }
    }

    @Override
    public void remove(@Nonnull ServiceFunctionPath serviceFunctionPath) {
        LOG.info("remove: Deleting SFP: {}", serviceFunctionPath.getName().getValue());

        // Delete each RSP in config connected to this SFP
        // When the config RSP is deleted, it will delete the operational RSP
        List<SfpRenderedServicePath> sfpRspList =
                SfcProviderServicePathAPI.readServicePathState(serviceFunctionPath.getName());
        for (SfpRenderedServicePath sfpRsp : sfpRspList) {
            // Delete the RSP from config, which in turn will delete the RSP from operational
            SfcProviderRenderedPathAPI.deleteRenderedServicePath(sfpRsp.getName(), LogicalDatastoreType.CONFIGURATION);
        }
    }

    @Override
    public void update(@Nonnull ServiceFunctionPath originalServiceFunctionPath,
                       @Nonnull ServiceFunctionPath updatedServiceFunctionPath) {
        LOG.warn("Updating the SFP is not supported: {}", updatedServiceFunctionPath.getName().getValue());
    }
}
