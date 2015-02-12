package org.opendaylight.sfc.sbrest.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RspExporterFactory implements ExporterFactory {
    @Override
    public Exporter getExporter() {
        return new RspExporter();
    }
}

class RspExporter extends AbstractExporter implements Exporter {

    private static final Logger LOG = LoggerFactory.getLogger(RspExporter.class);

    @Override
    public String exportJson(DataObject dataObject) {

        String ret = null;
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
            try {
                Object rspObject = mapper.treeToValue(rspArray, Object.class);
                ret = mapper.writeValueAsString(rspObject);
                ret = "{ \"rendered-service-path\" : " + ret + " }";
                LOG.debug("Created Rendered Service Path JSON: {}", ret);
            } catch (JsonProcessingException e) {
                LOG.error("Error during creation of JSON for Rendered Service Path {}", rsp.getName());
            }

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
            ArrayNode rspArray = mapper.createArrayNode();
            rspArray.add(node);
            ret = "{ \"rendered-service-path\" : " + rspArray.toString() + " }";
        } else {
            throw new IllegalArgumentException("Argument is not an instance of RenderedServicePath");
        }

        return ret;
    }
}
