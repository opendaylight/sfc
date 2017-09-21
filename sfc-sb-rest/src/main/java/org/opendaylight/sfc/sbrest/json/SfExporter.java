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
import java.util.Locale;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SfExporter extends AbstractExporter implements Exporter {

    private static final Logger LOG = LoggerFactory.getLogger(SfExporter.class);

    public static final String SERVICE_FUNCTION = "service-function";
    public static final String NAME = "name";
    public static final String TYPE = "type";
    public static final String REST_URI = "rest-uri";
    public static final String IP_MGMT_ADDRESS = "ip-mgmt-address";
    public static final String SF_DATA_PLANE_LOCATOR = "sf-data-plane-locator";
    public static final String SERVICE_FUNCTION_FORWARDER = "service-function-forwarder";

    public static final String SERVICE_FUNCTION_TYPE_PREFIX = "service-function-type:";

    @Override
    public String exportJson(DataObject dataObject) {

        String ret = null;
        if (dataObject instanceof ServiceFunction) {
            ServiceFunction sf = (ServiceFunction) dataObject;

            ObjectNode sfNode = mapper.createObjectNode();
            if (sf.getName() != null && sf.getName().getValue() != null) {
                sfNode.put(NAME, sf.getName().getValue());
            }
            sfNode.put(IP_MGMT_ADDRESS, ExporterUtil.convertIpAddress(sf.getIpMgmtAddress()));
            if (sf.getRestUri() != null) {
                sfNode.put(REST_URI, sf.getRestUri().getValue());
            }
            if (sf.getType() != null) {
                sfNode.put(TYPE, SERVICE_FUNCTION_TYPE_PREFIX + sf.getType().getValue()
                        .toLowerCase(Locale.getDefault()));
            }

            if (sf.getSfDataPlaneLocator() != null) {
                ArrayNode locatorArray = mapper.createArrayNode();
                for (SfDataPlaneLocator sfDataPlaneLocator : sf.getSfDataPlaneLocator()) {
                    ObjectNode sfLocatorNode = this.getSfDataPlaneLocatorObjectNode(sfDataPlaneLocator);
                    locatorArray.add(sfLocatorNode);
                }
                sfNode.putArray(SF_DATA_PLANE_LOCATOR).addAll(locatorArray);
            }
            ArrayNode sfArray = mapper.createArrayNode();
            sfArray.add(sfNode);
            try {
                Object sfObject = mapper.treeToValue(sfArray, Object.class);
                ret = mapper.writeValueAsString(sfObject);
                ret = "{\"" + SERVICE_FUNCTION + "\":" + ret + "}";
                LOG.debug("Created Service Function JSON: {}", ret);
            } catch (JsonProcessingException e) {
                LOG.error("Error during creation of JSON for Service Function {}", sf.getName());
            }

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
            node.put(NAME, obj.getName().getValue());
            ArrayNode sfArray = mapper.createArrayNode();
            sfArray.add(node);
            ret = "{\"" + SERVICE_FUNCTION + "\":" + sfArray.toString() + "}";
        } else {
            throw new IllegalArgumentException("Argument is not an instance of ServiceFunction");
        }

        return ret;
    }

    private ObjectNode getSfDataPlaneLocatorObjectNode(SfDataPlaneLocator locator) {
        ObjectNode sfLocatorNode = ExporterUtil.getDataPlaneLocatorObjectNode(locator);

        if (sfLocatorNode == null) {
            sfLocatorNode = mapper.createObjectNode();
        }

        sfLocatorNode.put(NAME, locator.getName().getValue());
        sfLocatorNode.put(SERVICE_FUNCTION_FORWARDER, locator.getServiceFunctionForwarder().getValue());

        return sfLocatorNode;
    }
}

