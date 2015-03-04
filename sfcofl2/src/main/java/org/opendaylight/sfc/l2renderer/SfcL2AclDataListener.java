/**
 * Copyright (c) 2014 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.sfc.l2renderer;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.access.lists.access.list.access.list.entries.actions.sfc.action.AclRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessList;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.access.lists.access.list.access.list.entries.actions.SfcAction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.Actions1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.access.list.entries.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SfcL2AclDataListener extends SfcL2AbstractDataListener {

    private static final short DEFAULT_MASK = 32;
    private static final String SUBNET_MASK = "/";
    private SfcL2FlowProgrammerInterface sfcL2FlowProgrammer;
    private static final Logger LOG = LoggerFactory.getLogger(SfcL2AclDataListener.class);

    public SfcL2AclDataListener(DataBroker dataBroker, SfcL2FlowProgrammerInterface sfcL2FlowProgrammer) {
        setDataBroker(dataBroker);
        setIID(OpendaylightSfc.ACL_ENTRY_IID);
        registerAsDataChangeListener();
        this.sfcL2FlowProgrammer = sfcL2FlowProgrammer;
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        Map<InstanceIdentifier<?>, DataObject> dataOriginalConfigurationObject;
        dataOriginalConfigurationObject = change.getOriginalData();

        // ACL create
        Map<InstanceIdentifier<?>, DataObject> dataCreatedConfigurationObject;
        dataCreatedConfigurationObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedConfigurationObject.entrySet()) {
            if (entry.getValue() instanceof AccessList) {
                LOG.info("SfcL2AclDataListener.onDataChanged ACL {}", ((AccessList) entry.getValue()).getAclName());

                AccessList createdAccessListEntry = (AccessList) entry.getValue();
                configureAclFlows(createdAccessListEntry, true);
            }
        }

        // ACL delete

        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier<?> instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalConfigurationObject.get(instanceIdentifier);
            if (dataObject instanceof AccessLists) {
                AccessList removedAccessListEntry = (AccessList) dataObject;
                configureAclFlows(removedAccessListEntry, false);
            }
        }

        // ACL update

        Map<InstanceIdentifier<?>, DataObject> dataUpdatedConfigurationObject;
        dataUpdatedConfigurationObject = change.getUpdatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedConfigurationObject.entrySet()) {
            if ((entry.getValue() instanceof AccessLists && (!(dataCreatedConfigurationObject.containsKey(entry
                    .getKey()))))) {
                AccessList updatedAccessListEntry = (AccessList) entry.getValue();

                configureAclFlows(updatedAccessListEntry, true);

            }
        }

    }

    private void configureAclFlows(AccessList acl, boolean isAddFlow) {

        Iterator<AccessListEntries> aclEntryIter = acl.getAccessListEntries().iterator();

        while (aclEntryIter.hasNext()) {

            AccessListEntries createdAccessListEntries = aclEntryIter.next();
            LOG.info("configureAclFlows createdAccessListEntries {}", createdAccessListEntries);
            LOG.info("configureAclFlows createdAccessListEntries.getActions() {}", createdAccessListEntries.getActions());
            LOG.info("configureAclFlows createdAccessListEntries.getActions().getAugmentation(Actions1.class) {}",
                    createdAccessListEntries.getActions().getAugmentation(Actions1.class));
            LOG.info("configureAclFlows createdAccessListEntries.getActions().getAugmentation(Actions1.class).getSfcAction() {}",
                    createdAccessListEntries.getActions().getAugmentation(Actions1.class).getSfcAction());
            SfcAction sfcAction =
                    createdAccessListEntries.getActions().getAugmentation(Actions1.class).getSfcAction();

            String aclRenderedServicePathName = ((AclRenderedServicePath) sfcAction).getRenderedServicePath();
            RenderedServicePath renderedServicePath = SfcProviderRenderedPathAPI.readRenderedServicePathExecutor(aclRenderedServicePathName);

            if(renderedServicePath == null) {
                LOG.info("ACL renderedServicePath {} does not exist", aclRenderedServicePathName);
                continue;
            }

            Matches matches = createdAccessListEntries.getMatches();
            AceIp aceIp = (AceIp) matches.getAceType();
            AceIpv4 aceIpv4 = (AceIpv4) aceIp.getAceIpVersion();

            // IP Addresses
            Short srcMask = 0;
            String srcIpAddress = null;
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

            Short dstMask = 0;
            String dstIpAddress = null;
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

            // Ports and Protocol
            Short srcPort = 0;
            Short dstPort = 0;
            byte protocol = (byte) 0;
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

            // Write the ACL for each SFF in the service chain
            List<RenderedServicePathHop> servicePathHopList = renderedServicePath.getRenderedServicePathHop();
            if(servicePathHopList == null) {
                LOG.info("ACL no servicePathHop available for {}", aclRenderedServicePathName);
                continue;
            }

            for (RenderedServicePathHop servicePathHop : servicePathHopList) {

                this.sfcL2FlowProgrammer.configureClassificationFlow(
                        servicePathHop.getServiceFunctionForwarder(),
                        srcIpAddress,
                        srcMask,
                        dstIpAddress,
                        dstMask,
                        srcPort,
                        dstPort,
                        protocol,
                        renderedServicePath.getPathId(),
                        isAddFlow);
            }
        }
    }
}
