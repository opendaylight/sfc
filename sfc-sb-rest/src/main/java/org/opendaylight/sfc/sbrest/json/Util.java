package org.opendaylight.sfc.sbrest.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Function;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Lisp;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Mac;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

    protected static ObjectNode ObjectNodeFromSfDataPlaneLocator(SfDataPlaneLocator locator) {

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode node = mapper.createObjectNode();

        node.put("name", locator.getName())
                .put("service-function-forwarder", locator.getServiceFunctionForwarder());
        node.put("transport", getTransportFromDataPlaneLocator(locator));

        addVaryingLeafs(node, locator);

        return node;
    }

    protected static void addVaryingLeafs(final ObjectNode node, DataPlaneLocator dpLocator) {
        //String type = dpLocator.getLocatorType().getImplementedInterface().getSimpleName().toLowerCase();
        String type = "dummy type";
        try {
            Method method;
            Object result;
            switch (type) {
                case "function":
                    Function functionLocator = (Function) dpLocator;
                    node.put("function-name", functionLocator.getFunctionName());
                    break;
                case "ip":
                    method = dpLocator.getLocatorType().getImplementedInterface().getDeclaredMethod("getIp");
                    result = method.invoke(dpLocator);
                    IpAddress ip = (IpAddress) result;
                    node.put("ip", convertIpAddress(ip));
                    method = dpLocator.getLocatorType().getImplementedInterface().getDeclaredMethod("getPort");
                    result = method.invoke(dpLocator);
                    PortNumber port = (PortNumber) result;
                    node.put("port", port.getValue());
                    break;
                case "lisp":
                    Lisp lispLocator = (Lisp) dpLocator;
                    node.put("eid", convertIpAddress(lispLocator.getEid()));
                    break;
                case "mac":
                    Mac macLocator = (Mac) dpLocator;
                    node.put("mac", macLocator.getMac().getValue());
                    node.put("vlan-id", macLocator.getVlanId());
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    protected static String getTransportFromDataPlaneLocator(DataPlaneLocator locator) {
        String transport = "dummy transport";
        /*
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
        */
        return transport;
    }
}
