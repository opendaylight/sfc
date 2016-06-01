/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_verify.provider;

//import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
//import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
//import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.sfc_verify.listener.RenderedPathListener;
//import org.opendaylight.sfc.sfc_verify.utils.SfcVerifyDataStoreAPI;
//import org.opendaylight.sfc.sfc_verify.utils.SfcVerifyUtils;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;


public class SfcVerifyRspProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SfcVerifyRspProcessor.class);

    private final SfcVerifyNodeManager nodeManager;
    private final RenderedPathListener rspListener;

    public SfcVerifyRspProcessor(DataBroker dataBroker, SfcVerifyNodeManager nodeManager) {
        this.nodeManager = nodeManager;
        // Register RSP listener
        rspListener = new RenderedPathListener(dataBroker, this);
    }

    public void updateRsp(RenderedServicePath renderedServicePath) {
        //TODO: handle RSP updates to add SFCV related augmentations
    }

    public void deleteRsp(RenderedServicePath renderedServicePath) {
        //TODO: handle RSP deletes, as needed.
    }

    public void unregisterRspListener() {
        rspListener.getRegistrationObject().close();
    }
}
