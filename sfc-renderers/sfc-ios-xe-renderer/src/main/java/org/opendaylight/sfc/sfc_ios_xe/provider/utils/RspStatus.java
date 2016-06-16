/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_ios_xe.provider.utils;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.rsp.manager.rev160421.RendererPathStates;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.rsp.manager.rev160421.renderer.path.states.RendererPathState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.rsp.manager.rev160421.renderer.path.states.RendererPathStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.rsp.manager.rev160421.renderer.path.states.renderer.path.state.ConfiguredRenderedPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.rsp.manager.rev160421.renderer.path.states.renderer.path.state.configured.rendered.paths.ConfiguredRenderedPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.rsp.manager.rev160421.renderer.path.states.renderer.path.state.configured.rendered.paths.ConfiguredRenderedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.rsp.manager.rev160421.renderer.path.states.renderer.path.state.configured.rendered.paths.ConfiguredRenderedPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RendererName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RspStatus {

    private static final Logger LOG = LoggerFactory.getLogger(RspStatus.class);
    private final DataBroker dataBroker;
    private final RspName rspName;

    public RspStatus(final DataBroker dataBroker, final RspName rspName) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.rspName = Preconditions.checkNotNull(rspName);
        Preconditions.checkNotNull(rspName);
    }

    public void writeStatus(ConfiguredRenderedPath.PathStatus status) {
        final ConfiguredRenderedPathBuilder configuredRenderedPathBuilder = new ConfiguredRenderedPathBuilder();
        configuredRenderedPathBuilder.setKey(new ConfiguredRenderedPathKey(rspName))
                .setRspName(rspName);
        configuredRenderedPathBuilder.setPathStatus(status);
        final ConfiguredRenderedPath configuredRenderedPath = configuredRenderedPathBuilder.build();
        final InstanceIdentifier<ConfiguredRenderedPath> statusIid = InstanceIdentifier.builder(RendererPathStates.class)
                .child(RendererPathState.class, new RendererPathStateKey(new RendererName("ios-xe-renderer")))
                .child(ConfiguredRenderedPaths.class)
                .child(ConfiguredRenderedPath.class, configuredRenderedPath.getKey()).build();
        // Write new status
        final ReadWriteTransaction wtx = dataBroker.newReadWriteTransaction();
        wtx.merge(LogicalDatastoreType.OPERATIONAL, statusIid, configuredRenderedPath, true);
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = wtx.submit();
        try {
            submitFuture.checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("Write transaction failed to {}", e.getMessage());
        }
    }
}
