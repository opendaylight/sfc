/*
 * Copyright (c) 2015 Intel .Ltd, and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sbrest.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfstExporterFactory implements ExporterFactory {
    public static final String _SERVICE_FUNCTION_SCHEDULE_TYPE = SfstExporter._SERVICE_FUNCTION_SCHEDULE_TYPE;
    public static final String _NAME = SfstExporter._NAME;

    @Override
    public Exporter getExporter() {
        return new SfstExporter();
    }
}

class SfstExporter extends AbstractExporter implements Exporter {
    private static final Logger LOG = LoggerFactory.getLogger(SfstExporter.class);
    public static final String _SERVICE_FUNCTION_SCHEDULE_TYPE = "service-function-schedule-type";
    public static final String SERVICE_FUNCTION_SCHEDULE_TYPE_PREFIX = "service-function-schedule-type:";
    public static final String _NAME = "name";
    public static final String _TYPE = "type";
    public static final String _ENABLED = "enabled";

    @Override
    public String exportJson(DataObject dataObject) {
        String ret = null;

        if (dataObject instanceof ServiceFunctionSchedulerType) {
            ServiceFunctionSchedulerType sfst = (ServiceFunctionSchedulerType) dataObject;

            ArrayNode sfstArray = mapper.createArrayNode();
            ObjectNode sfstNode = mapper.createObjectNode();
            sfstNode.put(_NAME, sfst.getName());
            sfstNode.put(_ENABLED, sfst.isEnabled());
            if (sfst.getType() != null) {
                sfstNode.put(_TYPE, SERVICE_FUNCTION_SCHEDULE_TYPE_PREFIX + sfst.getType().getSimpleName().toLowerCase());
            }

            sfstArray.add(sfstNode);
            try {
                Object sfstObject = mapper.treeToValue(sfstArray, Object.class);
                ret = mapper.writeValueAsString(sfstObject);
                ret = "{\"" + _SERVICE_FUNCTION_SCHEDULE_TYPE + "\":" + ret + "}";
                LOG.debug("Created Service Function Schedule Type JSON: {}", ret);
            } catch (JsonProcessingException e) {
                LOG.error("Error during creation of JSON for Service Function Schedule Type {}", sfst.getName());
            }
        } else {
            throw new IllegalArgumentException("Argument is not an instance of ServiceFunctionSchedulerType");
        }

        return ret;
    }

    @Override
    public String exportJsonNameOnly(DataObject dataObject) {
        String ret = null;

        if (dataObject instanceof ServiceFunctionSchedulerType) {
            ServiceFunctionSchedulerType obj = (ServiceFunctionSchedulerType) dataObject;

            ObjectNode node = mapper.createObjectNode();
            node.put(_NAME, obj.getName());
            ArrayNode sfstArray = mapper.createArrayNode();
            sfstArray.add(node);
            ret = "{\"" + _SERVICE_FUNCTION_SCHEDULE_TYPE + "\":" + sfstArray.toString() + "}";
        } else {
            throw new IllegalArgumentException("Argument is not an instance of ServiceFunctionSchedulerType");
        }

        return ret;
    }
}
