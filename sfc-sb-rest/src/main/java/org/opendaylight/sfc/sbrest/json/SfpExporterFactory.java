package org.opendaylight.sfc.sbrest.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHop;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class SfpExporterFactory implements ExporterFactory {
    @Override
    public Exporter getExporter() {
        return new SfpExporter();
    }
}

class SfpExporter implements Exporter {

    @Override
    public String exportJson(DataObject dataObject) {

        String ret;
        if (dataObject instanceof ServiceFunctionPath) {
            ServiceFunctionPath sfp = (ServiceFunctionPath) dataObject;

            ObjectMapper mapper = new ObjectMapper();

            ObjectNode node = mapper.getNodeFactory().objectNode();
            node.put("classifier", sfp.getClassifier())
                    .put("context-metadata", sfp.getContextMetadata())
                    .put("name", sfp.getName())
                    .put("path-id", sfp.getPathId())
                    .put("service-chain-name", sfp.getServiceChainName())
                    .put("starting-index", sfp.getStartingIndex())
                    .put("symmetric-classifier", sfp.getSymmetricClassifier())
                    .put("variable-metadata", sfp.getVariableMetadata())
                    .put("symmetric", sfp.isSymmetric());

            ArrayNode hopArray = mapper.getNodeFactory().arrayNode();
            for (ServicePathHop e : sfp.getServicePathHop()) {
                ObjectNode o = mapper.getNodeFactory().objectNode();
                o.put("hop-number", e.getHopNumber())
                        .put("service-function-forwarder", e.getServiceFunctionForwarder())
                        .put("service-function-name", e.getServiceFunctionName())
                        .put("service-index", e.getServiceIndex());
                hopArray.add(o);
            }
            node.putArray("service-path-hop").addAll(hopArray);

            ret = "{ \"service-function-path\" : " + node.toString() + " }";

        } else {
            throw new IllegalArgumentException("Argument is not an instance of ServiceFunctionPath");
        }

        return ret;
    }
}
