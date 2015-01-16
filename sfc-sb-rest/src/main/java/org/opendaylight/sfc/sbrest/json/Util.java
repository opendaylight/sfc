package org.opendaylight.sfc.sbrest.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.*;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {

    private static final Logger LOG = LoggerFactory.getLogger(Util.class);

    public static String convertIpAddress(IpAddress ip) {
        String ret;
        if (ip.getIpv4Address() != null) {
            ret = ip.getIpv4Address().getValue();
        } else {
            ret = ip.getIpv6Address().getValue();
        }
        return ret;
    }

    public static ObjectNode ObjectNodeFromSfDataPlaneLocator(SfDataPlaneLocator locator) {

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode node = mapper.getNodeFactory().objectNode();

        node.put("name", locator.getName())
                .put("service-function-forwarder", locator.getServiceFunctionForwarder());

        // TODO I'm not sure if "transport" should be a field or just a case-selector
        /*
        String transport;
        switch (locator.getTransport().getSimpleName().toLowerCase()) {
            case "vxlangpe":
                transport = "vxlan-gpe";
                break;
            case "gre":
                transport = "gre";
                break;
            default:
                transport = "other";
        }
        node.put("transport", transport);
        */

        String type = locator.getLocatorType().getImplementedInterface().getSimpleName().toLowerCase();
        DataPlaneLocator dpLocator = (DataPlaneLocator) locator;
        switch (type) {
            case "function":
                Function functionLocator = (Function) dpLocator;
                node.put("function-name", functionLocator.getFunctionName());
                break;
            case "ip":
                Ip ipLocator = (Ip) dpLocator;
                node.put("ip", convertIpAddress(ipLocator.getIp()));
                node.put("port", ipLocator.getPort().getValue());
                break;
            case "lisp":
                Lisp lispLocator = (Lisp) dpLocator;
                node.put("eid", convertIpAddress(lispLocator.getEid()));
                ;
                break;
            case "mac":
                Mac macLocator = (Mac) dpLocator;
                node.put("mac", macLocator.getMac().getValue());
                node.put("vlan-id", macLocator.getVlanId());
        }

        return node;
    }
}
