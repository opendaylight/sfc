package org.opendaylight.sfc.provider;

import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev140701.access.list.access.list.entries.actions.Action;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.list.AccessListEntries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcProviderAclDataListener implements DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderSfpDataListener.class);
    private static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        // TODO Auto-generated method stub

        LOG.debug("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);

        Map<InstanceIdentifier<?>, DataObject> dataOriginalConfigurationObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalConfigurationObject.entrySet()) {
            if (entry.getValue() instanceof AccessListEntries) {
                AccessListEntries originalAccessListEntries = (AccessListEntries) entry.getValue();
                // Action action =
                // originalAccessListEntries.getActions().getAction();
                // Long pathId = action.getPathId();

                // TODO open item how to retieve SFP details using SFPID, then
                // for each of the SF from the list of SFs, take the sff and
                // provision
                // table 0 entry

                // SfcProviderSffFlowWriter.getInstance().setNodeInfo(TODO
                // sffname);

                // TODO need to get the ip src/dst, srcport/dstport and protocol
                // and call it
                // SfcProviderSffFlowWriter.getInstance().writeSffAcl(originalAccessListEntries.getMatches(),
                // dstIp,
                // srcPort, dstPort, protocol, sfp.getPathId());

            }
        }

    }

}
