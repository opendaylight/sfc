/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others. All rights reserved.
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
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RspExporter extends AbstractExporter implements Exporter {

    private static final Logger LOG = LoggerFactory.getLogger(RspExporter.class);

    public static final String RENDERED_SERVICE_PATH = "rendered-service-path";
    public static final String NAME = "name";
    public static final String CONTEXT_METADATA = "context-metadata";
    public static final String PATH_ID = "path-id";
    public static final String PARENT_SERVICE_FUNCTION_PATH = "parent-service-function-path";
    public static final String SERVICE_CHAIN_NAME = "service-chain-name";
    public static final String STARTING_INDEX = "starting-index";
    public static final String VARIABLE_METADATA = "variable-metadata";
    public static final String HOP_NUMBER = "hop-number";
    public static final String SERVICE_FUNCTION_FORWARDER = "service-function-forwarder";
    public static final String SERVICE_FUNCTION_NAME = "service-function-name";
    public static final String SERVICE_INDEX = "service-index";
    public static final String RENDERED_SERVICE_PATH_HOP = "rendered-service-path-hop";

    @Override
    public String exportJson(DataObject dataObject) {
        String ret = null;
        if (dataObject instanceof RenderedServicePath) {
            RenderedServicePath rsp = (RenderedServicePath) dataObject;

            ObjectNode node = mapper.createObjectNode();
            node.put(CONTEXT_METADATA, rsp.getContextMetadata());
            if (rsp.getName() != null) {
                node.put(NAME, rsp.getName().getValue());
            }

            Uint32 pathId = rsp.getPathId();
            if (pathId != null) {
                node.put(PATH_ID, pathId.toJava());
            }
            if (rsp.getParentServiceFunctionPath() != null) {
                node.put(PARENT_SERVICE_FUNCTION_PATH, rsp.getParentServiceFunctionPath().getValue());
            }
            if (rsp.getServiceChainName() != null) {
                node.put(SERVICE_CHAIN_NAME, rsp.getServiceChainName().getValue());
            }
            Uint8 startingIndex = rsp.getStartingIndex();
            if (startingIndex != null) {
                node.put(STARTING_INDEX, startingIndex.toJava());
            }
            if (rsp.getVariableMetadata() != null) {
                node.put(VARIABLE_METADATA, rsp.getVariableMetadata());
            }

            List<RenderedServicePathHop> hopList = rsp.getRenderedServicePathHop();
            if (hopList != null) {
                ArrayNode hopArray = mapper.createArrayNode();
                for (RenderedServicePathHop e : hopList) {
                    ObjectNode objectNode = mapper.createObjectNode();
                    Uint8 hopNumber = e.getHopNumber();
                    if (hopNumber != null) {
                        objectNode.put(HOP_NUMBER, hopNumber.toJava());
                    }
                    if (e.getServiceFunctionForwarder() != null) {
                        objectNode.put(SERVICE_FUNCTION_FORWARDER, e.getServiceFunctionForwarder().getValue());
                    }
                    if (e.getServiceFunctionName() != null) {
                        objectNode.put(SERVICE_FUNCTION_NAME, e.getServiceFunctionName().getValue());
                    }
                    Uint8 serviceIndex = e.getServiceIndex();
                    if (serviceIndex != null) {
                        objectNode.put(SERVICE_INDEX, serviceIndex.toJava());
                    }
                    hopArray.add(objectNode);
                }
                node.putArray(RENDERED_SERVICE_PATH_HOP).addAll(hopArray);
            }
            ArrayNode rspArray = mapper.createArrayNode();
            rspArray.add(node);
            try {
                Object rspObject = mapper.treeToValue(rspArray, Object.class);
                ret = mapper.writeValueAsString(rspObject);
                ret = "{\"" + RENDERED_SERVICE_PATH + "\":" + ret + "}";
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
            ret = "{\"" + RENDERED_SERVICE_PATH + "\":" + rspArray.toString() + "}";
        } else {
            throw new IllegalArgumentException("Argument is not an instance of RenderedServicePath");
        }

        return ret;
    }
}
