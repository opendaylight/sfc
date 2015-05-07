package org.opendaylight.sfc.l2renderer;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.SlTransportType;

public class RspBuilder {

    private static String SF_NAME_PREFIX  = "SF_";
    private static String SFF_NAME_PREFIX = "SFF_";
    private static String SFC_NAME_PREFIX = "SFC_";
    private static String SFP_NAME_PREFIX = "SFP_";
    private static String RSP_NAME_PREFIX = "RSP_";
    private int SF_NAME_INDEX = 0;
    private int SFF_NAME_INDEX = 0;
    private int SFC_NAME_INDEX = 0;
    private int SFP_NAME_INDEX = 0;
    private int RSP_NAME_INDEX = 0;

    public RspBuilder() {
    }

    public RenderedServicePath createRspFromSfTypes(
            List<Class<? extends ServiceFunctionTypeIdentity>> sfTypes,
            Class<? extends SlTransportType> transportType) {
        return null;
    }

    public ServiceFunctionChain createServiceFunctionChain() {
        return null;
    }

    public ServiceFunctionChain createServiceFunctionPath() {
        return null;
    }

    public ServiceFunctionForwarder createServiceFunctionForwarder() {
        return null;
    }

    public ServiceFunction createServiceFunction(Class<? extends ServiceFunctionTypeIdentity> sfType) {
        return null;
    }
}
