/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.netconf.renderer.listener;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.pot.netconf.renderer.provider.SfcPotNetconfIoam;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles RSP changes and triggers config send to the SB nodes.
 *
 * <p>
 *
 * @version 0.1
 */
@Singleton
public class SfcPotNetconfRSPListener extends AbstractSyncDataTreeChangeListener<RenderedServicePath> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcPotNetconfRSPListener.class);

    private final SfcPotNetconfIoam sfcPotNetconfIoam;

    @Inject
    public SfcPotNetconfRSPListener(DataBroker dataBroker, SfcPotNetconfIoam sfcPotNetconfIoam) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL,
              InstanceIdentifier.builder(RenderedServicePaths.class).child(RenderedServicePath.class).build());
        this.sfcPotNetconfIoam = sfcPotNetconfIoam;
    }

    @Override
    public void add(@Nonnull RenderedServicePath renderedServicePath) {
        update(renderedServicePath, renderedServicePath);
    }

    @Override
    public void remove(@Nonnull RenderedServicePath renderedServicePath) {
        LOG.debug("iOAM:PoT:SB:Deleted RSP: {}", renderedServicePath.getName());
        sfcPotNetconfIoam.deleteRsp(renderedServicePath);
    }

    @Override
    public void update(@Nonnull RenderedServicePath originalRenderedServicePath,
                       @Nonnull RenderedServicePath updatedRenderedServicePath) {
        // As of now, it is not expected that PoT configurations will be
        // configured as part of the RSP creation itself.
        LOG.debug("iOAM:PoT:SB:Updated RSP: {}", updatedRenderedServicePath.getName());
        sfcPotNetconfIoam.processRspUpdate(updatedRenderedServicePath);
    }
}
