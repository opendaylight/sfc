package org.opendaylight.sfc.sbrest.json;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class SfExporterFactory implements ExporterFactory {
    @Override
    public Exporter getExporter() {
        return new SfExporter();
    }
}

class SfExporter extends AbstractExporter implements Exporter {

    @Override
    public String exportJson(DataObject dataObject) {

        String ret;
        if (dataObject instanceof ServiceFunction) {
            ServiceFunction sf = (ServiceFunction) dataObject;

            ArrayNode sfArray = mapper.createArrayNode();

            ObjectNode node = mapper.createObjectNode();
            node.put("name", sf.getName());
            node.put("ip-mgmt-address", Util.convertIpAddress(sf.getIpMgmtAddress()));
            if (sf.getRestUri() != null)
                node.put("rest-uri", sf.getRestUri().getValue());
            if (sf.getType() != null)
                node.put("type", "service-function-type:" + sf.getType().getSimpleName().toLowerCase());
            node.put("nsh-aware", sf.isNshAware());
            node.put("request-reclassification", sf.isRequestReclassification());

            if (sf.getSfDataPlaneLocator() != null) {
                ArrayNode locatorArray = mapper.createArrayNode();
                for (SfDataPlaneLocator e : sf.getSfDataPlaneLocator()) {
                    ObjectNode o = Util.ObjectNodeFromSfDataPlaneLocator(e);
                    locatorArray.add(o);
                }
                node.putArray("sf-data-plane-locator").addAll(locatorArray);
            }

            sfArray.add(node);

            ret = "{ \"service-function\" : " + sfArray.toString() + " }";

        } else {
            throw new IllegalArgumentException("Argument is not an instance of ServiceFunction");
        }

        return ret;
    }

    @Override
    public String exportJsonNameOnly(DataObject dataObject) {

        String ret = null;
        if (dataObject instanceof ServiceFunction) {
            ServiceFunction obj = (ServiceFunction) dataObject;

            ObjectNode node = mapper.createObjectNode();
            node.put("name", obj.getName());
            ret = "{ \"service-function\" : " + node.toString() + " }";
        } else {
            throw new IllegalArgumentException("Argument is not an instance of ServiceFunction");
        }

        return ret;
    }
}
