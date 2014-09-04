package org.opendaylight.ofsfc.provider;

import java.util.Map;

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

    private static final Logger LOG = LoggerFactory.getLogger(OpenflowSfpDataListener.class);
    private static final OpenflowSfcRenderer odlSfc = OpenflowSfcRenderer.getOpendaylightSfcObj();

    public OpenflowAclDataListener(DataBroker dataBroker) {
        setDataBroker(dataBroker);
        setIID(SfcInstanceIdentifierUtils.createServiceFunctionAclsPath());
        registerAsDataChangeListener();
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        Map<InstanceIdentifier<?>, DataObject> dataOriginalConfigurationObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalConfigurationObject.entrySet()) {
            if (entry.getValue() instanceof AccessListEntries) {
                AccessListEntries originalAccessListEntries = (AccessListEntries) entry.getValue();
                AclServiceFunctionPath action = (AclServiceFunctionPath) (SfcAction) originalAccessListEntries
                        .getActions();

                // TODO: retrieve sfpid from sfpname
                Long pathId = 1L;

                Matches matches;
                AceIp aceIp;
                AceIpv4 aceIpv4;

                matches = originalAccessListEntries.getMatches();
                aceIp = (AceIp) matches.getAceType();
                aceIpv4 = (AceIpv4) aceIp.getAceIpVersion();

                String srcIpAddress = aceIpv4.getSourceIpv4Address().getValue();
                String dstIpAddress = aceIpv4.getDestinationIpv4Address().getValue();

                Short srcPort = aceIp.getSourcePortRange().getLowerPort().getValue().shortValue();
                Short dstPort = aceIp.getDestinationPortRange().getLowerPort().getValue().shortValue();

                byte protocol = aceIp.getIpProtocol().byteValue();

                // TODO program classification table on each sff of sfp
                // SfcProviderSffFlowWriter.getInstance().setNodeInfo(sffname);

                OpenflowSfcFlowProgrammer.getInstance().writeClassificationFlow(srcIpAddress, (short) 32, dstIpAddress,
                        (short) 32, srcPort, dstPort, protocol, pathId);
            }
        }
    }
}