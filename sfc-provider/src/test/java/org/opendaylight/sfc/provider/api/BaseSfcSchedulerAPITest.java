package org.opendaylight.sfc.provider.api;

import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHopBuilder;

public class BaseSfcSchedulerAPITest extends AbstractDataBrokerTest{

    public BaseSfcSchedulerAPITest() {
        // TODO Auto-generated constructor stub
    }

    protected ServicePathHop buildSFHop(String sffName, String sfName, short index){
        ServicePathHopBuilder sphb = new ServicePathHopBuilder();
        sphb.setHopNumber(index);
        sphb.setServiceFunctionForwarder(sffName);
        sphb.setServiceFunctionName(sfName);
        return sphb.build();
    }
}
