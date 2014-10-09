/**
 * Copyright (c) 2014 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.ofsfc.provider;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ofsfc.provider.utils.SfcInstanceIdentifierUtils;
import org.opendaylight.ofsfc.provider.utils.SfcOfL2APIUtil;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.access.lists.access.list.access.list.entries.actions.SfcAction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.Actions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.Actions1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.access.lists.access.list.access.list.entries.actions.SfcAction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.access.lists.access.list.access.list.entries.actions.sfc.action.AclServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenflowAclDataListener extends OpenflowAbstractDataListener {

    private static final Logger LOG = LoggerFactory.getLogger(OpenflowAclDataListener.class);
    private static final OpenflowSfcRenderer odlSfc = OpenflowSfcRenderer.getOpendaylightSfcObj();

    private static final short DEFAULT_MASK = 32;
    public static final String SUBNET_MASK = "/";

    public OpenflowAclDataListener(DataBroker dataBroker) {
        setDataBroker(dataBroker);
        setIID(SfcInstanceIdentifierUtils.createServiceFunctionAclsPath());
        registerAsDataChangeListener();
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        Map<InstanceIdentifier<?>, DataObject> dataOriginalConfigurationObject;
        dataOriginalConfigurationObject = change.getOriginalData();

        // ACL create
        Map<InstanceIdentifier<?>, DataObject> dataCreatedConfigurationObject;
        dataCreatedConfigurationObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedConfigurationObject.entrySet()) {
            if (entry.getValue() instanceof AccessListEntries) {
                AccessListEntries createdAccessListEntries = (AccessListEntries) entry.getValue();
                configureAclFlows(createdAccessListEntries, true);
            }
        }

        // ACL delete

        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalConfigurationObject.get(instanceIdentifier);
            if (dataObject instanceof AccessListEntries) {
                AccessListEntries removedAccessListEntries = (AccessListEntries) dataObject;
                configureAclFlows(removedAccessListEntries, false);
            }
        }

        // ACL update

        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject;
        dataUpdatedConfigurationObject = change.getUpdatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if ((entry.getValue() instanceof AccessListEntries && (!(dataCreatedConfigurationObject.containsKey(entry
                    .getKey()))))) {
                AccessListEntries updatedAccessListEntries = (AccessListEntries) entry.getValue();

                configureAclFlows(updatedAccessListEntries, true);

            }
        }

    }

    private void configureAclFlows(AccessListEntries createdAccessListEntries, boolean isAddFlow) {
        Matches matches;
        AceIp aceIp;
        AceIpv4 aceIpv4;
        Short srcPort = 0;
        Short dstPort = 0;
        Short srcMask = 0;
        Short dstMask = 0;
        String srcIpAddress = null;
        String dstIpAddress = null;
        byte protocol = (byte) 0;

        OpenflowSfcFlowProgrammer flowProgrammer = OpenflowSfcFlowProgrammer.getInstance();
        SfcAction sfcAction = createdAccessListEntries.getActions().getAugmentation(Actions1.class).getSfcAction();
        String aclServicePathName = ((AclServiceFunctionPath) sfcAction).getServiceFunctionPath();

        ServiceFunctionPath servicefunctionPath = SfcOfL2APIUtil.readServiceFunctionPath(getDataBroker(),
                aclServicePathName);

        Long pathId = servicefunctionPath.getPathId();

        matches = createdAccessListEntries.getMatches();
        aceIp = (AceIp) matches.getAceType();
        aceIpv4 = (AceIpv4) aceIp.getAceIpVersion();

        if (aceIpv4.getSourceIpv4Address() != null) {
            srcIpAddress = aceIpv4.getSourceIpv4Address().getValue();
            if (srcIpAddress.contains(SUBNET_MASK)) {
                String[] parts = srcIpAddress.split(SUBNET_MASK);
                srcIpAddress = parts[0];
                srcMask = Short.parseShort(parts[1]);
            } else {
                srcMask = DEFAULT_MASK;
            }
        }
        if (aceIpv4.getDestinationIpv4Address() != null) {
            dstIpAddress = aceIpv4.getDestinationIpv4Address().getValue();
            if (dstIpAddress.contains(SUBNET_MASK)) {
                String[] parts = dstIpAddress.split(SUBNET_MASK);
                dstIpAddress = parts[0];
                dstMask = Short.parseShort(parts[1]);
            } else {
                dstMask = DEFAULT_MASK;
            }
        }

        if (aceIp != null) {
            if (aceIp.getSourcePortRange() != null) {
                srcPort = aceIp.getSourcePortRange().getLowerPort().getValue().shortValue();
            }
            if (aceIp.getDestinationPortRange() != null) {
                dstPort = aceIp.getDestinationPortRange().getLowerPort().getValue().shortValue();
            }
            if (aceIp.getIpProtocol() != null) {
                protocol = aceIp.getIpProtocol().byteValue();
            }
        }
        List<ServicePathHop> servicePathHopList = servicefunctionPath.getServicePathHop();

        for (ServicePathHop servicePathHop : servicePathHopList) {

            flowProgrammer.setNodeInfo(servicePathHop.getServiceFunctionForwarder());
            flowProgrammer.configureClassificationFlow(srcIpAddress, srcMask, dstIpAddress, dstMask, srcPort, dstPort,
                    protocol, pathId, isAddFlow);
        }
    }

}
