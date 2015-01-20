package org.opendaylight.sfc.sbrest.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffDataPlaneLocator1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridge;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.ovs.bridge.ExternalIds;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.service.function.dictionary.SffSfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocator;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class SffExporterFactory implements ExporterFactory {
    @Override
    public Exporter getExporter() {
        return new SffExporter();
    }
}

class SffExporter implements Exporter {

    @Override
    public String exportJson(DataObject dataObject) {

        String ret;
        if (dataObject instanceof ServiceFunctionForwarder) {
            ServiceFunctionForwarder sff = (ServiceFunctionForwarder) dataObject;

            ObjectMapper mapper = new ObjectMapper();

            ObjectNode node = mapper.getNodeFactory().objectNode();
            node.put("name", sff.getName())
                    .put("ip-mgmt-address", Util.convertIpAddress(sff.getIpMgmtAddress()))
                    .put("rest-uri", sff.getRestUri().getValue())
                    .put("service-node", sff.getServiceNode());

            ArrayNode locatorArray = mapper.getNodeFactory().arrayNode();
            for (SffDataPlaneLocator e : sff.getSffDataPlaneLocator()) {
                ObjectNode o = mapper.getNodeFactory().objectNode();
                o.put("name", e.getName());

                ObjectNode dplNode = mapper.getNodeFactory().objectNode();
                DataPlaneLocator dpl = e.getDataPlaneLocator();
                dplNode.put("transport", Util.getTransportFromDataPlaneLocator(dpl));
                Util.addVaryingLeafs(dplNode, dpl);
                o.put("data-plane-locator", dplNode);

                // for ovs-bridge
                if (e instanceof SffDataPlaneLocator1) {
                    SffDataPlaneLocator1 dploc1 = (SffDataPlaneLocator1) e;
                    ObjectNode ovsBridgeNode = mapper.getNodeFactory().objectNode();
                    OvsBridge ovsBridge = dploc1.getOvsBridge();
                    ovsBridgeNode.put("bridge-name", ovsBridge.getBridgeName())
                            .put("uuid", ovsBridge.getUuid().getValue());

                    ArrayNode externalIdsArray = mapper.getNodeFactory().arrayNode();
                    for (ExternalIds ei : ovsBridge.getExternalIds()) {
                        ObjectNode eiNode = mapper.getNodeFactory().objectNode();
                        eiNode.put("name", ei.getName())
                                .put("value", ei.getValue());
                        externalIdsArray.add(eiNode);
                    }
                    ovsBridgeNode.putArray("external-ids").addAll(externalIdsArray);
                    o.put("service-function-forwarder-ovs:ovs-bridge", ovsBridgeNode);
                }
                locatorArray.add(o);
            }
            node.putArray("sff-data-plane-locator").addAll(locatorArray);

            ArrayNode dictionaryArray = mapper.getNodeFactory().arrayNode();
            for (ServiceFunctionDictionary e : sff.getServiceFunctionDictionary()) {
                ObjectNode o = mapper.getNodeFactory().objectNode();
                o.put("name", e.getName());
                o.put("type", "service-function-type:" + e.getType().getSimpleName().toLowerCase());
                SffSfDataPlaneLocator sffSfDpl = e.getSffSfDataPlaneLocator();
                ObjectNode sffSfDplNode = mapper.getNodeFactory().objectNode();
                Util.addVaryingLeafs(sffSfDplNode, sffSfDpl);
                o.put("sff-sf-data-plane-locator", sffSfDplNode);
                dictionaryArray.add(o);
            }
            node.putArray("service-function-dictionary").addAll(locatorArray);

            ret = "{ \"service-function-forwarder\" : " + node.toString() + " }";

        } else {
            throw new IllegalArgumentException("Argument is not an instance of ServiceFunctionForwarder");
        }

        return ret;
    }

}
