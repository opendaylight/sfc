package org.opendaylight.sfc.sf;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140602.service.functions.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140602.*;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;

public class OpendaylightSFs 
	implements ServiceFunctionService, AutoCloseable, DataChangeListener {

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
			t.removeConfigurationData(IID);
			t.commit().get();
		}
	}

	private void updateStatus(List<ServiceFunction> list) {
		if (dataProvider != null) {
			final DataModificationTransaction t = dataProvider
					.beginTransaction();
			t.removeConfigurationData(IID);
			t.putConfigurationData(IID, buildServiceFunctions(list));
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
				final DataModificationTransaction t = dataProvider.beginTransaction();
				t.removeConfigurationData(IID);
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

	@Override
	public Future<RpcResult<Void>> updateFunction(UpdateFunctionInput input) {
		LOG.info("updateFunction: " + input);
		ServiceFunctionBuilder builder = new ServiceFunctionBuilder();
		ServiceFunction sf = builder.setName(input.getName()).setType(input.getType())
				.setIpHostAddress(input.getIpHostAddress()).build();
		
		if (dataProvider != null) {
			final DataModificationTransaction t = dataProvider
					.beginTransaction();

			t.putConfigurationData(IID, sf);
			try {
				t.commit().get();
			} catch (ExecutionException | InterruptedException e) {
				LOG.warn("Failed to update-function, operational otherwise", e);
			}
		}
		
		return Futures.immediateFuture(Rpcs.<Void> getRpcResult(true,
				Collections.<RpcError> emptySet()));
	}
	
	@Override
	public Future<RpcResult<Void>> updateFunctionDpiWa20() {
		LOG.info("updateFunctionDpiWa20: \n");
		IpAddress ip = new IpAddress(new char[]{10,0,0,111});
		ServiceFunctionBuilder builder = new ServiceFunctionBuilder();
		ServiceFunction sf = builder.setName("dpi-wa20")
				.setType(org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140602.Firewall.class)
				.setIpHostAddress(ip).build();
		
		LOG.info("updateFunctionDpiWa20: bbb\n");
		if (dataProvider != null) {
			LOG.info("updateFunctionDpiWa20: dataProvider not null\n");
			final DataModificationTransaction t = dataProvider
					.beginTransaction();

			t.putConfigurationData(IID, sf);
			try {
				t.commit().get();
			} catch (ExecutionException | InterruptedException e) {
				LOG.warn("Failed to update-function, operational otherwise", e);
			}
		}
		else{
			LOG.info("updateFunctionDpiWa20: dataProvider not null\n");
		}
		LOG.info("updateFunctionDpiWa20: eee\n");
		return Futures.immediateFuture(Rpcs.<Void> getRpcResult(true,
				Collections.<RpcError> emptySet()));
	}

	
	@Override
	public void onDataChanged(
			DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
		
		DataObject dataObject = change.getUpdatedConfigurationData().get(IID);
		if (dataObject instanceof ServiceFunctions ){
			ServiceFunctions sfs = (ServiceFunctions) dataObject;
			List<ServiceFunction> list = sfs.getServiceFunction();
			
			final DataModificationTransaction t = dataProvider.beginTransaction();
			t.removeConfigurationData(IID);
			t.putConfigurationData(IID, buildServiceFunctions(list));
			try {
				t.commit().get();
				LOG.info("==***==\nData changed:\n" + list.toString() + "\n==***==\n");
			} catch (ExecutionException | InterruptedException e) {
				LOG.warn("Failed to commit operational data", e);
			}
		}
		String m = dataObject != null ? dataObject.toString() : "-null-";
		LOG.info("\n==***==\n"+ m + "\n==***==\n");
		
	}

}