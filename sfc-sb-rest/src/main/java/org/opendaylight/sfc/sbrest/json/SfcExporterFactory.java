package org.opendaylight.sfc.sbrest.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class SfcExporterFactory implements ExporterFactory {
    @Override
    public Exporter getExporter() {
        return new SfcExporter();
    }
}

class SfcExporter implements Exporter {

    @Override
    public String exportJson(DataObject dataObject) {

        String ret;
        if (dataObject instanceof ServiceFunctionChain) {
            ServiceFunctionChain sfc = (ServiceFunctionChain) dataObject;

            ObjectMapper mapper = new ObjectMapper();

            ObjectNode node = mapper.getNodeFactory().objectNode();
            node.put("name", sfc.getName());


            ArrayNode sfArray = mapper.getNodeFactory().arrayNode();
            for (SfcServiceFunction e : sfc.getSfcServiceFunction()) {
                ObjectNode o = mapper.getNodeFactory().objectNode();
                o.put("name", e.getName())
                        .put("type", "service-function-type:" + e.getType().getSimpleName().toLowerCase())
                        .put("order", e.getOrder());
                sfArray.add(o);
            }
            node.putArray("sfc-service-function").addAll(sfArray);

            ret = "{ \"service-function-chain\" : " + node.toString() + " }";

        } else {
            throw new IllegalArgumentException("Argument is not an instance of ServiceFunctionChain");
        }

        return ret;
    }

}
