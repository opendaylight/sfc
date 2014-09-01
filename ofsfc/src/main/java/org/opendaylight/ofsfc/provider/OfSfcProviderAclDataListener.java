package org.opendaylight.ofsfc.provider;

import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
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

public class OfSfcProviderAclDataListener implements DataChangeListener {

    private static final Logger LOG = LoggerFactory
            .getLogger(OfSfcProviderSfpDataListener.class);
    private static final OfSfcProvider odlSfc = OfSfcProvider
            .getOpendaylightSfcObj();

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        // TODO Auto-generated method stub

        LOG.debug("\n########## Start: {}", Thread.currentThread()
                .getStackTrace()[1]);

        Map<InstanceIdentifier<?>, DataObject> dataOriginalConfigurationObject = change
                .getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalConfigurationObject
                .entrySet()) {
            if (entry.getValue() instanceof AccessListEntries) {
                AccessListEntries originalAccessListEntries = (AccessListEntries) entry
                        .getValue();
                AclServiceFunctionPath action = (AclServiceFunctionPath) ((SfcAction) originalAccessListEntries
                        .getActions());

                // will now get the sfp name and fetch the sfp id from it
                //need to fetch the sfpid from the sfp name
                //String sPath= action.getServiceFunctionPath();
                Long pathId = 1L;

                Matches matches;
                AceIp aceIp;
                AceIpv4 aceIpv4;
                matches = originalAccessListEntries.getMatches();
                aceIp = (AceIp) matches.getAceType();
                aceIpv4 = (AceIpv4) aceIp.getAceIpVersion();

                String srcIpAddress = aceIpv4.getSourceIpv4Address().getValue();
                String dstIpAddress = aceIpv4.getDestinationIpv4Address()
                        .getValue();

                Short srcPort = aceIp.getSourcePortRange().getLowerPort()
                        .getValue().shortValue();
                Short dstPort = aceIp.getDestinationPortRange().getLowerPort()
                        .getValue().shortValue();

                byte protocol = aceIp.getIpProtocol().byteValue();

                // aceIp.getDestinationPortRange().getLowerPort()

                // aceIpv4.getDestinationIpv4Address();

                // TODO open item how to retieve SFP details using SFPname, then
                // for each of the SF from the list of SFs, take the sff and
                // provision
                // table 0 entry

                // SfcProviderSffFlowWriter.getInstance().setNodeInfo(TODO
                // sffname);

                // TODO need to get the ip src/dst, srcport/dstport and protocol
                // and call it
                OfSfcProviderSffFlowWriter.getInstance().writeClassificationFlow(
                        srcIpAddress, (short) 32, dstIpAddress, (short) 32,
                        srcPort, dstPort, protocol, pathId);

            }
        }

    }
}
