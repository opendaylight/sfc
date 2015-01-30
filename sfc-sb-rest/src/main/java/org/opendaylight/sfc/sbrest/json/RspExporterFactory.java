package org.opendaylight.sfc.sbrest.json;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yangtools.yang.binding.DataObject;

import java.util.List;

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
            ArrayNode rspArray = mapper.createArrayNode();

            ObjectNode node = mapper.createObjectNode();
            node.put("context-metadata", rsp.getContextMetadata());
            node.put("name", rsp.getName());
            node.put("path-id", rsp.getPathId());
            node.put("parent-service-function-path", rsp.getParentServiceFunctionPath());
            node.put("service-chain-name", rsp.getServiceChainName());
            node.put("starting-index", rsp.getStartingIndex());
            node.put("variable-metadata", rsp.getVariableMetadata());

            List<RenderedServicePathHop> hopList = rsp.getRenderedServicePathHop();
            if (hopList != null) {
                ArrayNode hopArray = mapper.createArrayNode();
                for (RenderedServicePathHop e : hopList) {
                    ObjectNode o = mapper.createObjectNode();
                    o.put("hop-number", e.getHopNumber());
                    o.put("service-function-forwarder", e.getServiceFunctionForwarder());
                    o.put("service-function-name", e.getServiceFunctionName());
                    o.put("service-index", e.getServiceIndex());
                    hopArray.add(o);
                }
                node.putArray("rendered-service-path-hop").addAll(hopArray);
            }

            rspArray.add(node);
            ret = "{ \"rendered-service-path\" : " + rspArray.toString() + " }";

        } else {
            throw new IllegalArgumentException("Argument is not an instance of RenderedServicePath");
        }

        return ret;
    }

    @Override
    public String exportJsonNameOnly(DataObject dataObject) {

        String ret = null;
        if (dataObject instanceof RenderedServicePath) {
            RenderedServicePath obj = (RenderedServicePath) dataObject;

            ObjectNode node = mapper.createObjectNode();
            node.put("name", obj.getName());
            ret = "{ \"rendered-service-path\" : " + node.toString() + " }";
        } else {
            throw new IllegalArgumentException("Argument is not an instance of RenderedServicePath");
        }

        return ret;
    }
}
