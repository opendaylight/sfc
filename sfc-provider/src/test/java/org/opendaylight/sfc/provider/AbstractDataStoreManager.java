package org.opendaylight.sfc.provider;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;

import java.util.concurrent.ExecutorService;

/**
 * This class contains auxiliary methods to manage abstract data store
 *
 * @author Vladimir Lavor vladimir.lavor@pantheon.sk
 * @version 0.1
 * @since 2015-07-22
 */

/*
 * The purpose of this class is to manage abstract data store used in tests
 */
public abstract class AbstractDataStoreManager extends AbstractDataBrokerTest {

    //executor will be removed, when all tests using it will be reworked
    private static boolean executorSet = false;

    protected DataBroker dataBroker;
    protected static ExecutorService executor;
    protected final OpendaylightSfc opendaylightSfc = new OpendaylightSfc();

    //initial sfc odl setup, executor is set only once, new data broker is created before every set, it ensures empty data store
    protected void setOdlSfc() {
        if(!executorSet) {
            executor = opendaylightSfc.getExecutor();
            executorSet = true;
        }
        dataBroker = getDataBroker();
        opendaylightSfc.setDataProvider(dataBroker);
    }
}
