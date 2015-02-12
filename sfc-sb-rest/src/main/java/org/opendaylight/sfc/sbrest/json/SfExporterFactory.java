package org.opendaylight.sfc.sbrest.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfExporterFactory implements ExporterFactory {
    @Override
    public Exporter getExporter() {
        return new SfExporter();
    }
}

class SfExporter extends AbstractExporter implements Exporter {

    private static final Logger LOG = LoggerFactory.getLogger(SfExporter.class);

    public static final String _SERVICE_FUNCTION = "service-function";
    public static final String _NAME = "name";
    public static final String _TYPE = "type";
    public static final String _REST_URI = "rest-uri";
    public static final String _IP_MGMT_ADDRESS = "ip-mgmt-address";
    public static final String _REQUEST_RECLASSIFICATION = "request_reclassification";
    public static final String _NSH_AWARE = "nsh-aware";
    public static final String _SF_DATA_PLANE_LOCATOR = "sf-data-plane-locator";
    public static final String _SERVICE_FUNCTION_FORWARDER = "service-function-forwarder";

    public static final String TYPE_PREFIX = "service-function-type:";

    @Override
    public String exportJson(DataObject dataObject) {

        String ret = null;
        if (dataObject instanceof ServiceFunction) {
            ServiceFunction sf = (ServiceFunction) dataObject;

            ArrayNode sfArray = mapper.createArrayNode();

            ObjectNode sfNode = mapper.createObjectNode();
            sfNode.put(_NAME, sf.getName());
            sfNode.put(_IP_MGMT_ADDRESS, Util.convertIpAddress(sf.getIpMgmtAddress()));
            if (sf.getRestUri() != null) {
                sfNode.put(_REST_URI, sf.getRestUri().getValue());
            }
            if (sf.getType() != null) {
                sfNode.put(_TYPE, TYPE_PREFIX + sf.getType().getSimpleName().toLowerCase());
            }
            sfNode.put(_NSH_AWARE, sf.isNshAware());
            sfNode.put(_REQUEST_RECLASSIFICATION, sf.isRequestReclassification());

            if (sf.getSfDataPlaneLocator() != null) {
                ArrayNode locatorArray = mapper.createArrayNode();
                for (SfDataPlaneLocator sfDataPlaneLocator : sf.getSfDataPlaneLocator()) {
                    ObjectNode sfLocatorNode = this.getObjectNodeFromSfDataPlaneLocator(sfDataPlaneLocator);
                    locatorArray.add(sfLocatorNode);
                }
                sfNode.putArray(_SF_DATA_PLANE_LOCATOR).addAll(locatorArray);
            }

            sfArray.add(sfNode);
            try {
                Object sfObject = mapper.treeToValue(sfArray, Object.class);
                ret = mapper.writeValueAsString(sfObject);
                ret = "{\"" + _SERVICE_FUNCTION + "\":" + ret + "}";
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
            node.put(_NAME, obj.getName());
            ArrayNode sfArray = mapper.createArrayNode();
            sfArray.add(node);
            ret = "{\"" + _SERVICE_FUNCTION + "\":" + sfArray.toString() + "}";
        } else {
            throw new IllegalArgumentException("Argument is not an instance of ServiceFunction");
        }

        return ret;
    }

    private ObjectNode getObjectNodeFromSfDataPlaneLocator(SfDataPlaneLocator locator) {
        ObjectNode sfLocatorNode = Util.getDataPlaneLocatorObjectNode(locator);
        sfLocatorNode.put(_NAME, locator.getName());
        sfLocatorNode.put(_SERVICE_FUNCTION_FORWARDER, locator.getServiceFunctionForwarder());

        return sfLocatorNode;
    }
}
