package org.opendaylight.sfc.sf.consumer.api;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140602.service.functions.ServiceFunction;

public interface ServiceFunctionsConsumer {

	
    boolean updateServiceFunctions(ServiceFunction sf);
}
