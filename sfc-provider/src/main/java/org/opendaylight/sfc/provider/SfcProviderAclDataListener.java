package org.opendaylight.sfc.provider;

import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.Actions1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.access.list.access.list.entries.actions.action.AclServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.list.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.list.access.list.entries.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.list.access.list.entries.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.list.access.list.entries.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcProviderAclDataListener implements DataChangeListener {

    private static final Logger LOG = LoggerFactory
            .getLogger(SfcProviderSfpDataListener.class);
    private static final OpendaylightSfc odlSfc = OpendaylightSfc
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
                AclServiceFunctionPath action = (AclServiceFunctionPath) ((Actions1) originalAccessListEntries
                        .getActions()).getAction();

                // will now get the sfp name and fetch the sfp id from it
                Long pathId = action.getPathId();

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
                SfcProviderSffFlowWriter.getInstance().writeSffAcl(
                        srcIpAddress, (short) 32, dstIpAddress, (short) 32,
                        srcPort, dstPort, protocol, pathId);

                SfcProviderSffFlowWriter.getInstance().writeOutGroup();

            }
        }

    }
}
