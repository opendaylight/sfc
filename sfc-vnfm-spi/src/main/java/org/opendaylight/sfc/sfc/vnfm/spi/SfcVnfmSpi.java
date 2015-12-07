package org.opendaylight.sfc.sfc.vnfm.spi;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ss.rev140701.service.statistics.group.ServiceStatistics;

public interface SfcVnfmSpi {

    public boolean createSf(ServiceFunctionType sfType);

    public boolean deleteSf(ServiceFunction sf);

    public ServiceStatistics getSfStatistics(ServiceFunction sf);
}
