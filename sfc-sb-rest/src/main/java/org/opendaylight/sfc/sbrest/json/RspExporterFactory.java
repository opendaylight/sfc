package org.opendaylight.sfc.sbrest.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class RspExporterFactory implements ExporterFactory {
    @Override
    public Exporter getExporter() {
        return new RspExporter();
    }
}

class RspExporter implements Exporter {

    @Override
    public String exportJson(DataObject dataObject) {

        String ret;
        if (dataObject instanceof RenderedServicePath) {
            RenderedServicePath rsp = (RenderedServicePath) dataObject;

            ObjectMapper mapper = new ObjectMapper();

            ObjectNode node = mapper.getNodeFactory().objectNode();
            node.put("context-metadata", rsp.getContextMetadata())
                    .put("name", rsp.getName())
                    .put("path-id", rsp.getPathId())
                    .put("parent-service-function-path", rsp.getParentServiceFunctionPath())
                    .put("service-chain-name", rsp.getServiceChainName())
                    .put("starting-index", rsp.getStartingIndex())
                    .put("variable-metadata", rsp.getVariableMetadata());

            ArrayNode hopArray = mapper.getNodeFactory().arrayNode();
            for (RenderedServicePathHop e : rsp.getRenderedServicePathHop()) {
                ObjectNode o = mapper.getNodeFactory().objectNode();
                o.put("hop-number", e.getHopNumber())
                        .put("service-function-forwarder", e.getServiceFunctionForwarder())
                        .put("service-function-name", e.getServiceFunctionName())
                        .put("service-index", e.getServiceIndex());
                hopArray.add(o);
            }
            node.putArray("rendered-service-path-hop").addAll(hopArray);

            ret = "{ \"rendered-service-path\" : " + node.toString() + " }";

        } else {
            throw new IllegalArgumentException("Argument is not an instance of RenderedServicePath");
        }

        return ret;
    }
}
