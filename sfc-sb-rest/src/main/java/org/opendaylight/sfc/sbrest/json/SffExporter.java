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
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffOvsLocatorBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridge;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.ovs.bridge.ExternalIds;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SffExporter extends AbstractExporter implements Exporter {

    private static final Logger LOG = LoggerFactory.getLogger(SffExporter.class);

    public static final String SERVICE_FUNCTION_FORWARDER = "service-function-forwarder";
    public static final String NAME = "name";
    public static final String SERVICE_NODE = "service-node";
    public static final String IP_MGMT_ADDRESS = "ip-mgmt-address";
    public static final String REST_URI = "rest-uri";
    public static final String SFF_DATA_PLANE_LOCATOR = "sff-data-plane-locator";
    public static final String DATA_PLANE_LOCATOR = "data-plane-locator";
    public static final String SERVICE_FUNCTION_DICTIONARY = "service-function-dictionary";
    public static final String TYPE = "type";
    public static final String SFF_SF_DATA_PLANE_LOCATOR = "sff-sf-data-plane-locator";
    public static final String SFF_INTERFACES = "sff-interfaces";
    public static final String SFF_INTERFACE = "sff-interface";
    public static final String FAILMODE = "failmode";
    public static final String OVS_BRIDGE = "ovs-bridge";
    public static final String BRIDGE_NAME = "bridge-name";
    public static final String UUID = "uuid";
    public static final String EXTERNAL_IDS = "external-id";
    public static final String VALUE = "value";

    public static final String SERVICE_FUNCTION_TYPE_PREFIX = "service-function-type:";
    public static final String SERVICE_FUNCTION_FORWARDER_PREFIX = "service-function-forwarder-ovs:";

    @Override
    public String exportJson(DataObject dataObject) {
        String ret = null;
        if (dataObject instanceof ServiceFunctionForwarder) {
            ServiceFunctionForwarder sff = (ServiceFunctionForwarder) dataObject;

            ObjectNode sffNode = mapper.createObjectNode();
            if (sff.getName() != null) {
                sffNode.put(NAME, sff.getName().getValue());
            }
            if (sff.getIpMgmtAddress() != null) {
                sffNode.put(IP_MGMT_ADDRESS, ExporterUtil.convertIpAddress(sff.getIpMgmtAddress()));
            }
            if (sff.getRestUri() != null) {
                sffNode.put(REST_URI, sff.getRestUri().getValue());
            }
            if (sff.getServiceNode() != null) {
                sffNode.put(SERVICE_NODE, sff.getServiceNode().getValue());
            }
            if (sff.getSffDataPlaneLocator() != null) {
                ArrayNode locatorArray = mapper.createArrayNode();
                for (SffDataPlaneLocator sffDataPlaneLocator : sff.getSffDataPlaneLocator()) {
                    locatorArray.add(this.getSffDataPlaneLocatorObjectNode(sffDataPlaneLocator));
                }

                sffNode.putArray(SFF_DATA_PLANE_LOCATOR).addAll(locatorArray);
            }

            if (sff.getServiceFunctionDictionary() != null) {
                ArrayNode dictionaryArray = mapper.createArrayNode();
                for (ServiceFunctionDictionary serviceFunctionDictionary : sff.getServiceFunctionDictionary()) {
                    dictionaryArray.add(this.getSfDictionaryObjectNode(serviceFunctionDictionary));
                }

                sffNode.putArray(SERVICE_FUNCTION_DICTIONARY).addAll(dictionaryArray);
            }
            ArrayNode sffArray = mapper.createArrayNode();
            sffArray.add(sffNode);
            try {
                Object sffObject = mapper.treeToValue(sffArray, Object.class);
                ret = mapper.writeValueAsString(sffObject);
                ret = "{\"" + SERVICE_FUNCTION_FORWARDER + "\":" + ret + "}";
                LOG.debug("Created Service Function Forwarder JSON: {}", ret);
            } catch (JsonProcessingException e) {
                LOG.error("Error during creation of JSON for Service Function Forwarder {}", sff.getName());
            }
        } else {
            throw new IllegalArgumentException("Argument is not an instance of ServiceFunctionForwarder");
        }

        return ret;
    }

    @Override
    public String exportJsonNameOnly(DataObject dataObject) {

        String ret = null;
        if (dataObject instanceof ServiceFunctionForwarder) {
            ServiceFunctionForwarder obj = (ServiceFunctionForwarder) dataObject;

            ObjectNode node = mapper.createObjectNode();
            node.put(NAME, obj.getName().getValue());
            ArrayNode sffArray = mapper.createArrayNode();
            sffArray.add(node);
            ret = "{\"" + SERVICE_FUNCTION_FORWARDER + "\":" + sffArray.toString() + "}";
        } else {
            throw new IllegalArgumentException("Argument is not an instance of ServiceFunctionForwarder");
        }

        return ret;
    }

    private ObjectNode getSffDataPlaneLocatorObjectNode(SffDataPlaneLocator sffDataPlaneLocator) {
        if (sffDataPlaneLocator == null) {
            return null;
        }

        ObjectNode sffDataPlaneLocatorNode = mapper.createObjectNode();
        sffDataPlaneLocatorNode.put(NAME, sffDataPlaneLocator.getName().getValue());

        sffDataPlaneLocatorNode.put(DATA_PLANE_LOCATOR,
                ExporterUtil.getDataPlaneLocatorObjectNode(sffDataPlaneLocator.getDataPlaneLocator()));

        sffDataPlaneLocatorNode.put(SERVICE_FUNCTION_FORWARDER_PREFIX + OVS_BRIDGE,
                this.getSffDataPlaneLocatorOvsBridgeObjectNode(sffDataPlaneLocator));

        return sffDataPlaneLocatorNode;
    }

    private ObjectNode getSffDataPlaneLocatorOvsBridgeObjectNode(SffDataPlaneLocator sffDataPlaneLocator) {
        if (sffDataPlaneLocator == null
                || sffDataPlaneLocator.augmentation(SffOvsLocatorBridgeAugmentation.class) == null) {
            return null;
        }

        SffOvsLocatorBridgeAugmentation sffDataPlaneLocator1 = sffDataPlaneLocator
                .augmentation(SffOvsLocatorBridgeAugmentation.class);

        if (sffDataPlaneLocator1 != null) {
            return this.getOvsBridgeObjectNode(sffDataPlaneLocator1.getOvsBridge());
        }

        return null;
    }

    private ObjectNode getOvsBridgeObjectNode(OvsBridge ovsBridge) {
        if (ovsBridge == null) {
            return null;
        }

        ObjectNode ovsBridgeNode = mapper.createObjectNode();
        ovsBridgeNode.put(BRIDGE_NAME, ovsBridge.getBridgeName());

        try {
            if (ovsBridge.getUuid() != null && !ovsBridge.getUuid().getValue().isEmpty()) {
                ovsBridgeNode.put(UUID, ovsBridge.getUuid().getValue());
            }
        } catch (IllegalArgumentException e) {
            LOG.error("Supplied value does not match any of the permitted UUID patterns");
        }

        if (ovsBridge.getExternalIds() != null) {
            ArrayNode externalIdsArray = mapper.createArrayNode();
            for (ExternalIds externalId : ovsBridge.getExternalIds()) {
                ObjectNode externalIdNode = mapper.createObjectNode();
                externalIdNode.put(NAME, externalId.getName());
                externalIdNode.put(VALUE, externalId.getValue());
                externalIdsArray.add(externalIdNode);
            }
            ovsBridgeNode.putArray(EXTERNAL_IDS).addAll(externalIdsArray);
        }

        return ovsBridgeNode;
    }

    private ObjectNode getSfDictionaryObjectNode(ServiceFunctionDictionary serviceFunctionDictionary) {
        if (serviceFunctionDictionary == null) {
            return null;
        }

        ObjectNode sfObjectNode = mapper.createObjectNode();
        sfObjectNode.put(NAME, serviceFunctionDictionary.getName().getValue());

        ObjectNode sffSfDataPlaneLocatorNode = ExporterUtil
                .getSffSfDataPlaneLocatorObjectNode(serviceFunctionDictionary.getSffSfDataPlaneLocator());
        if (sffSfDataPlaneLocatorNode != null) {
            sfObjectNode.put(SFF_SF_DATA_PLANE_LOCATOR, sffSfDataPlaneLocatorNode);
        } else {
            ObjectNode emptySffSfDataPlaneLocatorNode = mapper.createObjectNode();
            sfObjectNode.put(SFF_SF_DATA_PLANE_LOCATOR, emptySffSfDataPlaneLocatorNode);
        }

        return sfObjectNode;
    }
}
