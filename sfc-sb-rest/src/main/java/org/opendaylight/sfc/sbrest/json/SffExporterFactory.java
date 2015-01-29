package org.opendaylight.sfc.sbrest.json;

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

            ArrayNode sffArray = mapper.createArrayNode();

            ObjectNode node = mapper.createObjectNode();
            node.put("name", sff.getName());
            if (sff.getIpMgmtAddress() != null)
                node.put("ip-mgmt-address", Util.convertIpAddress(sff.getIpMgmtAddress()));
            if (sff.getRestUri() != null)
                node.put("rest-uri", sff.getRestUri().getValue());
            node.put("service-node", sff.getServiceNode());

            if (sff.getSffDataPlaneLocator() != null) {
                ArrayNode locatorArray = mapper.createArrayNode();
                for (SffDataPlaneLocator e : sff.getSffDataPlaneLocator()) {
                    ObjectNode o = mapper.createObjectNode();
                    o.put("name", e.getName());

                    ObjectNode dplNode = mapper.createObjectNode();
                    DataPlaneLocator dpl = e.getDataPlaneLocator();
                    if (dpl != null) {
                        dplNode.put("transport", Util.getTransportFromDataPlaneLocator(dpl));
                        Util.addVaryingLeafs(dplNode, dpl);
                        o.put("data-plane-locator", dplNode);
                    }

                    // for ovs-bridge
                    if (e instanceof SffDataPlaneLocator1) {
                        SffDataPlaneLocator1 dploc1 = (SffDataPlaneLocator1) e;
                        ObjectNode ovsBridgeNode = mapper.createObjectNode();
                        OvsBridge ovsBridge = dploc1.getOvsBridge();
                        if (ovsBridge != null) {
                            ovsBridgeNode.put("bridge-name", ovsBridge.getBridgeName());
                            if (ovsBridge.getUuid() != null)
                                ovsBridgeNode.put("uuid", ovsBridge.getUuid().getValue());

                            if (ovsBridge.getExternalIds() != null) {
                                ArrayNode externalIdsArray = mapper.createArrayNode();
                                for (ExternalIds ei : ovsBridge.getExternalIds()) {
                                    ObjectNode eiNode = mapper.createObjectNode();
                                    eiNode.put("name", ei.getName());
                                    eiNode.put("value", ei.getValue());
                                    externalIdsArray.add(eiNode);
                                }
                                ovsBridgeNode.putArray("external-ids").addAll(externalIdsArray);
                            }
                            o.put("service-function-forwarder-ovs:ovs-bridge", ovsBridgeNode);
                        }
                    }
                    locatorArray.add(o);
                }
                node.putArray("sff-data-plane-locator").addAll(locatorArray);
            }

            if (sff.getServiceFunctionDictionary() != null) {
                ArrayNode dictionaryArray = mapper.createArrayNode();
                for (ServiceFunctionDictionary e : sff.getServiceFunctionDictionary()) {
                    ObjectNode o = mapper.createObjectNode();
                    o.put("name", e.getName());
                    if (e.getType() != null)
                        o.put("type", "service-function-type:" + e.getType().getSimpleName().toLowerCase());
                    SffSfDataPlaneLocator sffSfDpl = e.getSffSfDataPlaneLocator();
                    if (sffSfDpl != null) {
                        ObjectNode sffSfDplNode = mapper.createObjectNode();
                        Util.addVaryingLeafs(sffSfDplNode, sffSfDpl);
                        o.put("sff-sf-data-plane-locator", sffSfDplNode);
                    }
                    dictionaryArray.add(o);
                }
                node.putArray("service-function-dictionary").addAll(dictionaryArray);
            }

            sffArray.add(node);

            ret = "{ \"service-function-forwarder\" : " + sffArray.toString() + "}";

            //ret = "{ \"service-function-forwarder\" : [ {" + node.toString() + " } ] }";

        } else {
                throw new IllegalArgumentException("Argument is not an instance of ServiceFunctionForwarder");
            }

            return ret;
        }

        @Override
        public String exportJsonNameOnly (DataObject dataObject){

            String ret = null;
            if (dataObject instanceof ServiceFunctionForwarder) {
                ServiceFunctionForwarder obj = (ServiceFunctionForwarder) dataObject;

                ObjectNode node = mapper.createObjectNode();
                node.put("name", obj.getName());
                ret = "{ \"service-function-forwarder\" : " + node.toString() + " }";
            } else {
                throw new IllegalArgumentException("Argument is not an instance of ServiceFunctionForwarder");
            }

            return ret;
        }
    }
