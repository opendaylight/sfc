/**
 * Copyright (c) 2014 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.ofsfc.provider;

import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ofsfc.provider.utils.SfcInstanceIdentifierUtils;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.Actions1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.access.lists.access.list.access.list.entries.actions.SfcAction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.access.lists.access.list.access.list.entries.actions.sfc.action.AclServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenflowAclDataListener extends OpenflowAbstractDataListener {

    private static final Logger LOG = LoggerFactory.getLogger(OpenflowAclDataListener.class);
    private static final OpenflowSfcRenderer odlSfc = OpenflowSfcRenderer.getOpendaylightSfcObj();

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

        AclServiceFunctionPath sfcAction = (AclServiceFunctionPath) (SfcAction) createdAccessListEntries.getActions();

        // TODO: retrieve sfpid from sfpname that you will get by querying
        // String sfpName= sfcAction.getServiceFunctionPath();
        Long pathId = 1L;

        Matches matches;
        AceIp aceIp;
        AceIpv4 aceIpv4;

        matches = createdAccessListEntries.getMatches();
        aceIp = (AceIp) matches.getAceType();
        aceIpv4 = (AceIpv4) aceIp.getAceIpVersion();

        String srcIpAddress = aceIpv4.getSourceIpv4Address().getValue();
        String dstIpAddress = aceIpv4.getDestinationIpv4Address().getValue();

        Short srcPort = aceIp.getSourcePortRange().getLowerPort().getValue().shortValue();
        Short dstPort = aceIp.getDestinationPortRange().getLowerPort().getValue().shortValue();

        byte protocol = aceIp.getIpProtocol().byteValue();

        // TODO program classification table on each sff of sfp
        // SfcProviderSffFlowWriter.getInstance().setNodeInfo(sffname);

        OpenflowSfcFlowProgrammer.getInstance().configureClassificationFlow(srcIpAddress, (short) 32, dstIpAddress,
                (short) 32, srcPort, dstPort, protocol, pathId, isAddFlow);
    }

}
