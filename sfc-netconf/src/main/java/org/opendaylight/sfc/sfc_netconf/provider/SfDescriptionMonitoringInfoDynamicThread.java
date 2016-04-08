package org.opendaylight.sfc.sfc_netconf.provider;

import org.opendaylight.sfc.sfc_netconf.provider.api.SfcNetconfServiceFunctionAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.sfc.sf.desc.mon.MonitoringInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

public class SfDescriptionMonitoringInfoDynamicThread implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(SfDescriptionMonitoringInfoDynamicThread.class);
    private int ticket = 10;
    private String nodeName;

    public SfDescriptionMonitoringInfoDynamicThread(String nodeName) {
        this.nodeName = nodeName;
    }

    @Override
    public void run() {
        while (true) {
            printTraceStart(LOG);
            MonitoringInfo monInfo = null; /*SfcNetconfServiceFunctionAPI.getServiceFunctionMonitor(nodeName);*/
            if (monInfo != null) {
                SfName sfNodeName = new SfName(nodeName);
                SfcNetconfServiceFunctionAPI.putServiceFunctionMonitor(monInfo, sfNodeName);
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                LOG.warn("failed to ....", e);
            }
            printTraceStop(LOG);
        }
    }
}
