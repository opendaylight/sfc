package org.opendaylight.sfc.sf.consumer.api;

import org.opendaylight.controller.config.yang.config.test_consumer.impl.ServiceFunctionsConsumerRuntimeMXBean;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140602.ServiceFunctionService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140602.UpdateFunctionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140602.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceFunctionConsumerImpl implements ServiceFunctionsConsumer,
		ServiceFunctionsConsumerRuntimeMXBean {

	private static final Logger log = LoggerFactory
			.getLogger(ServiceFunctionConsumerImpl.class);

	private final ServiceFunctionService service;

	public ServiceFunctionConsumerImpl(ServiceFunctionService service) {
		this.service = service;
	}

	@Override
	public boolean updateServiceFunctions(ServiceFunction sf) {

		UpdateFunctionInputBuilder input = new UpdateFunctionInputBuilder();

		IpAddress ip = new IpAddress(new char[] { 10, 0, 0, 111 });
		input.setName("dpi-wa20")
				.setType(
						org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140602.Firewall.class)
				.setIpHostAddress(ip);
		try {
			RpcResult<Void> result = service.updateFunction(input.build())
					.get();

			if (result.isSuccessful()) {
				log.trace("updateServiceFunctions: successfully finished");
			} else {
				log.warn("updateServiceFunctions: not successfully finished");
			}

			return result.isSuccessful();
		} catch (Exception e) {
			log.warn("Error occurred during updateServiceFunctions");
		}
		return false;
	}

	@Override
	public Boolean updateSfWa20() {
		// TODO Auto-generated method stub
		return null;
	}

}
