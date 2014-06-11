package org.opendaylight.sfc.sf;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140602.service.functions.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140602.*;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;

public class OpendaylightSFs implements ServiceFunctionService, AutoCloseable {

	public static final InstanceIdentifier<ServiceFunctions> IID = InstanceIdentifier
			.builder(ServiceFunctions.class).build();

	private static final Logger LOG = LoggerFactory
			.getLogger(OpendaylightSFs.class);

	private DataBrokerService dataProvider;
	
	private final Object taskLock = new Object();

	public OpendaylightSFs() {
	}

	private ServiceFunctions buildServiceFunctions(List<ServiceFunction> list) {

		ServiceFunctionsBuilder builder = new ServiceFunctionsBuilder();
		builder.setServiceFunction(list);
		return builder.build();
	}

	public void setDataProvider(DataBrokerService salDataProvider,
			List<ServiceFunction> list) {
		this.dataProvider = salDataProvider;
		updateStatus(list);
	}

	/**
	 * Implemented from the AutoCloseable interface.
	 */
	@Override
	public void close() throws ExecutionException, InterruptedException {
		if (dataProvider != null) {
			final DataModificationTransaction t = dataProvider
					.beginTransaction();
			t.removeOperationalData(IID);
			t.commit().get();
		}
	}

	private void updateStatus(List<ServiceFunction> list) {
		if (dataProvider != null) {
			final DataModificationTransaction t = dataProvider
					.beginTransaction();
			t.removeOperationalData(IID);
			t.putOperationalData(IID, buildServiceFunctions(list));
			try {
				t.commit().get();
			} catch (InterruptedException | ExecutionException e) {
				LOG.warn(
						"Failed to update ServiceFunctions status, operational otherwise",
						e);
			}
		} else {
			LOG.trace("No data provider configured, not updating status");
		}
	}

	@Override
	public Future<RpcResult<Void>> deleteAll() {
		//synchronized (taskLock) {
			if (dataProvider != null) {
				final DataModificationTransaction t = dataProvider
						.beginTransaction();
				t.removeOperationalData(IID);
				try {
					t.commit().get();
				} catch (ExecutionException | InterruptedException e) {
					LOG.warn("Failed to delete-all, operational otherwise",
							e);
				}
			}
		//}
		// Always return success
		return Futures.immediateFuture(Rpcs.<Void> getRpcResult(true,
				Collections.<RpcError> emptySet()));
	}

}