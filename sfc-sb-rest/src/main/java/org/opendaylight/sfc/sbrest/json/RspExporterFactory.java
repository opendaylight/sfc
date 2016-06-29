/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sbrest.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RspExporterFactory implements ExporterFactory {

    public static final String _RENDERED_SERVICE_PATH = RspExporter._RENDERED_SERVICE_PATH;
    public static final String _NAME = RspExporter._NAME;
    public static final String _SERVICE_FUNCTION_FORWARDER = RspExporter._SERVICE_FUNCTION_FORWARDER;
    public static final String _RENDERED_SERVICE_PATH_HOP = RspExporter._RENDERED_SERVICE_PATH_HOP;

    @Override
    public Exporter getExporter() {
        return new RspExporter();
    }
}


class RspExporter extends AbstractExporter implements Exporter {

    private static final Logger LOG = LoggerFactory.getLogger(RspExporter.class);

    public static final String _RENDERED_SERVICE_PATH = "rendered-service-path";
    public static final String _NAME = "name";
    public static final String _CONTEXT_METADATA = "context-metadata";
    public static final String _PATH_ID = "path-id";
    public static final String _PARENT_SERVICE_FUNCTION_PATH = "parent-service-function-path";
    public static final String _SERVICE_CHAIN_NAME = "service-chain-name";
    public static final String _STARTING_INDEX = "starting-index";
    public static final String _VARIABLE_METADATA = "variable-metadata";
    public static final String _HOP_NUMBER = "hop-number";
    public static final String _SERVICE_FUNCTION_FORWARDER = "service-function-forwarder";
    public static final String _SERVICE_FUNCTION_NAME = "service-function-name";
    public static final String _SERVICE_INDEX = "service-index";
    public static final String _RENDERED_SERVICE_PATH_HOP = "rendered-service-path-hop";

    @Override
    public String exportJson(DataObject dataObject) {

        String ret = null;
        if (dataObject instanceof RenderedServicePath) {
            RenderedServicePath rsp = (RenderedServicePath) dataObject;
            ArrayNode rspArray = mapper.createArrayNode();

            ObjectNode node = mapper.createObjectNode();
            node.put(_CONTEXT_METADATA, rsp.getContextMetadata());
            if (rsp.getName() != null) {
                node.put(_NAME, rsp.getName().getValue());
            }
            node.put(_PATH_ID, rsp.getPathId());
            if (rsp.getParentServiceFunctionPath() != null) {
                node.put(_PARENT_SERVICE_FUNCTION_PATH, rsp.getParentServiceFunctionPath().getValue());
            }
            if (rsp.getServiceChainName() != null) {
                node.put(_SERVICE_CHAIN_NAME, rsp.getServiceChainName().getValue());
            }
            node.put(_STARTING_INDEX, rsp.getStartingIndex());
            if (rsp.getVariableMetadata() != null) {
                node.put(_VARIABLE_METADATA, rsp.getVariableMetadata());
            }

            List<RenderedServicePathHop> hopList = rsp.getRenderedServicePathHop();
            if (hopList != null) {
                ArrayNode hopArray = mapper.createArrayNode();
                for (RenderedServicePathHop e : hopList) {
                    ObjectNode o = mapper.createObjectNode();
                    o.put(_HOP_NUMBER, e.getHopNumber());
                    if (e.getServiceFunctionForwarder() != null) {
                        o.put(_SERVICE_FUNCTION_FORWARDER, e.getServiceFunctionForwarder().getValue());
                    }
                    if (e.getServiceFunctionName() != null) {
                        o.put(_SERVICE_FUNCTION_NAME, e.getServiceFunctionName().getValue());
                    }
                    o.put(_SERVICE_INDEX, e.getServiceIndex());
                    hopArray.add(o);
                }
                node.putArray(_RENDERED_SERVICE_PATH_HOP).addAll(hopArray);
            }

            rspArray.add(node);
            try {
                Object rspObject = mapper.treeToValue(rspArray, Object.class);
                ret = mapper.writeValueAsString(rspObject);
                ret = "{\"" + _RENDERED_SERVICE_PATH + "\":" + ret + "}";
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
            node.put("name", obj.getName().getValue());
            ArrayNode rspArray = mapper.createArrayNode();
            rspArray.add(node);
            ret = "{\"" + _RENDERED_SERVICE_PATH + "\":" + rspArray.toString() + "}";
        } else {
            throw new IllegalArgumentException("Argument is not an instance of RenderedServicePath");
        }

        return ret;
    }
}
