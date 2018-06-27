/*
 * Copyright (c) 2017 Inocybe Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.listeners;

import java.util.Collections;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to changes (addition, update, removal) in Rendered
 * Service Paths (RSPs) in the Config data store, taking the appropriate
 * actions. As of the Oxygen release, RSPs are written to the config data
 * store when an SFP is created in the config data store.
 */
@Singleton
public class RenderedServicePathListener extends AbstractSyncDataTreeChangeListener<RenderedServicePath> {

    private static final Logger LOG = LoggerFactory.getLogger(RenderedServicePathListener.class);

    @Inject
    public RenderedServicePathListener(final DataBroker dataBroker) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
              InstanceIdentifier.create(RenderedServicePaths.class).child(RenderedServicePath.class));
    }

    @Override
    public void add(@Nonnull InstanceIdentifier<RenderedServicePath> instanceIdentifier,
                    @Nonnull RenderedServicePath renderedServicePath) {

        // As of the Oxygen release, the RSP creation workflow is as follows:
        // - An SFP creation in the Config Data Store triggers an RSP and
        //   optionally the symmetric RSP creation in the Config Data Store.
        // - This listener listens for RSPs created in the Config Data Store
        //   and will write the RSP in the Operational Data Store

        LOG.info("add: RSP: {}", renderedServicePath.getName().getValue());

        ServiceFunctionPath serviceFunctionPath = SfcProviderServicePathAPI
                .readServiceFunctionPath(renderedServicePath.getParentServiceFunctionPath());

        if (serviceFunctionPath == null) {
            LOG.error("Service Function Path does not exist, cant create Rendered Service Path {}",
                    renderedServicePath.getName().getValue());
            return;
        }

        RenderedServicePath newRenderedServicePath = SfcProviderRenderedPathAPI
                .createRenderedServicePathAndState(serviceFunctionPath, renderedServicePath);
        if (newRenderedServicePath == null) {
            LOG.error("Failed to create Rendered Service Path {}", renderedServicePath.getName().getValue());
        }

        // As mentioned above, no need to do anything here for Symmetric
        // RSPs, since that will be taken care of when an SFP is created.
    }

    @Override
    public void remove(@Nonnull InstanceIdentifier<RenderedServicePath> instanceIdentifier,
                       @Nonnull RenderedServicePath renderedServicePath) {
        LOG.info("remove: Deleting RSP {}", renderedServicePath.getName().getValue());
        // It may be that someone deleted the Config RSP without first deleting
        // the SFP, but if we delete the SFP here, that could cause a race condition
        // if the SFP is deleted and immediately created.
        ServiceFunctionPath sfp = SfcProviderServicePathAPI.readServiceFunctionPath(
                renderedServicePath.getParentServiceFunctionPath());
        if (sfp != null) {
            LOG.warn("The RSP has been deleted without first deleting the corresponding SFP.");
        }

        // Now delete the Operational RSP
        SfcProviderRenderedPathAPI.deleteRenderedServicePathsAndStates(
                Collections.singletonList(renderedServicePath.getName()));
    }

    @Override
    public void update(@Nonnull InstanceIdentifier<RenderedServicePath> instanceIdentifier,
                       @Nonnull RenderedServicePath originalRenderedServicePath,
                       @Nonnull RenderedServicePath updatedRenderedServicePath) {
        LOG.warn("Updating the RSP in config is not supported: {}",
                updatedRenderedServicePath.getName().getValue());
    }
}
