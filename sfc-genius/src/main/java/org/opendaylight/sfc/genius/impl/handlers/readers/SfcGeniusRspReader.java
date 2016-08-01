/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.handlers.readers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SfcGeniusRspReader extends SfcGeniusReaderAbstract {

    public SfcGeniusRspReader(ReadTransaction readTransaction, Executor executor) {
        super(readTransaction, executor);
    }

    public CompletableFuture<RenderedServicePath> readRsp(RspName rspName) {
        InstanceIdentifier<RenderedServicePath> rspID = InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, new RenderedServicePathKey(rspName))
                .build();
        return doRead(LogicalDatastoreType.OPERATIONAL, rspID);
    }
}
