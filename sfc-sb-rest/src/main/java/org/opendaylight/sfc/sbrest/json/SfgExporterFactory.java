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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.group.entry.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SfgExporterFactory implements ExporterFactory {

    public static final String _SERVICE_FUNCTION_GROUP = SfgExporter._SERVICE_FUNCTION_GROUP;
    public static final String _NAME = SfgExporter._NAME;
    public static final String _REST_URI = SfgExporter._REST_URI;

    @Override
    public Exporter getExporter() {
        return new SfgExporter();
    }
}


class SfgExporter extends AbstractExporter implements Exporter {

    private static final Logger LOG = LoggerFactory.getLogger(SfgExporter.class);

    public static final String _SERVICE_FUNCTION_GROUP = "service-function-group";
    public static final String _SERVICE_FUNCTION = "service-function";
    public static final String _NAME = "name";
    public static final String _KEY = "key";
    public static final String _TYPE = "type";
    public static final String _REST_URI = "rest-uri";
    public static final String _ALGORITHM = "algorithm";
    public static final String _IP_MGMT_ADDRESS = "ip-mgmt-address";

    public static final String SERVICE_FUNCTION_TYPE_PREFIX = "service-function-type:";

    @Override
    public String exportJson(DataObject dataObject) {

        String ret = null;
        if (dataObject instanceof ServiceFunctionGroup) {
            ServiceFunctionGroup sfg = (ServiceFunctionGroup) dataObject;

            ArrayNode sfgArray = mapper.createArrayNode();
            ObjectNode sfgNode = mapper.createObjectNode();
            sfgNode.put(_NAME, sfg.getName());
            sfgNode.put(_IP_MGMT_ADDRESS, ExporterUtil.convertIpAddress(sfg.getIpMgmtAddress()));
            sfgNode.put(_ALGORITHM, sfg.getAlgorithm());
            if (sfg.getRestUri() != null) {
                sfgNode.put(_REST_URI, sfg.getRestUri().getValue());
            }
            if (sfg.getType() != null) {
                sfgNode.put(_TYPE, SERVICE_FUNCTION_TYPE_PREFIX + sfg.getType().getValue().toLowerCase());
            }

            // this should be revamped
            if (sfg.getSfcServiceFunction() != null) {
                ArrayNode sfArray = mapper.createArrayNode();
                for (SfcServiceFunction entry : sfg.getSfcServiceFunction()) {
                    ObjectNode o = mapper.createObjectNode();
                    o.put(_NAME, entry.getName().getValue());
                    sfArray.add(o);
                }
                sfgNode.putArray(_SERVICE_FUNCTION).addAll(sfArray);
            }

            sfgArray.add(sfgNode);
            try {
                Object sfObject = mapper.treeToValue(sfgArray, Object.class);
                ret = mapper.writeValueAsString(sfObject);
                ret = "{\"" + _SERVICE_FUNCTION_GROUP + "\":" + ret + "}";
                LOG.debug("Created Service Function Group JSON: {}", ret);
            } catch (JsonProcessingException e) {
                LOG.error("Error during creation of JSON for Service Function {}", sfg.getName());
            }

        } else {
            throw new IllegalArgumentException("Argument is not an instance of ServiceFunction");
        }

        return ret;
    }

    @Override
    public String exportJsonNameOnly(DataObject dataObject) {

        String ret = null;
        if (dataObject instanceof ServiceFunctionGroup) {
            ServiceFunctionGroup obj = (ServiceFunctionGroup) dataObject;

            ObjectNode node = mapper.createObjectNode();
            node.put(_NAME, obj.getName());
            ArrayNode sfArray = mapper.createArrayNode();
            sfArray.add(node);
            ret = "{\"" + _SERVICE_FUNCTION_GROUP + "\":" + sfArray.toString() + "}";
        } else {
            throw new IllegalArgumentException("Argument is not an instance of ServiceFunction");
        }

        return ret;
    }
}
