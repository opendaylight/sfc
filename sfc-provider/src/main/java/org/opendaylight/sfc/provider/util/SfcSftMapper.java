package org.opendaylight.sfc.provider.util;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypeIdentity;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.ServiceNodes;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.service.nodes.ServiceNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * This class maps service function types to service node mappers
 *
 * @see SfcSnMapper
 * <p>
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @since       2014-07-14
 */
public class SfcSftMapper {
    private Map<Class<? extends ServiceFunctionTypeIdentity>, SfcSnMapper> map;

    private final OpendaylightSfc odlSfc;

    public SfcSftMapper(OpendaylightSfc odlSfc){
        this.map = new HashMap<>();
        this.odlSfc = odlSfc;
        this.update();
    }

    public void update() {
        if (odlSfc != null) {
            ReadOnlyTransaction readTx = odlSfc.getDataProvider().newReadOnlyTransaction();
            Optional<ServiceNodes> dataObject = null;
            try {
                dataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, OpendaylightSfc.snIID).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            if (dataObject instanceof ServiceNodes) {
                ServiceNodes nodes = (ServiceNodes) dataObject;
                List<ServiceNode> snList = nodes.getServiceNode();

                for(ServiceNode sn : snList){
                    List<String> sfNameList = sn.getServiceFunction();
                    for(String sfName : sfNameList){
                        ServiceFunction sf = null;
                        try {
                            sf = (ServiceFunction) odlSfc.executor.submit(SfcProviderServiceFunctionAPI.getRead(
                                    new Object[]{sfName}, new Class[]{String.class})).get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                        if ( sf != null) {
                            this.add(sf.getType(), sn.getName(), sf);
                        } else {
                            throw new IllegalStateException("Service Function not found in datastore");
                        }
                    }
                }
            } else {
                throw new IllegalStateException("Wrong dataObject instance.");
            }
        } else {
            throw new NullPointerException("odlSfc is null");
        }
    }

    public void add(Class<? extends ServiceFunctionTypeIdentity> type, String snName, ServiceFunction sf){
        if(this.map.containsKey(type)){
            this.map.get(type).add(snName, sf);
        }else{
            SfcSnMapper snMapper = new SfcSnMapper(type);
            snMapper.add(snName, sf);
            this.map.put(type, snMapper);
        }
    }

    public void addAll(Class<? extends ServiceFunctionTypeIdentity> type, String snName, List<ServiceFunction> sfList){
        if(this.map.containsKey(type)){
            this.map.get(type).addAll(snName, sfList);
        }else{
            SfcSnMapper snMapper = new SfcSnMapper(type);
            snMapper.addAll(snName, sfList);
            this.map.put(type, snMapper);
        }
    }

    public List<ServiceFunction> getSfList(Class<? extends ServiceFunctionTypeIdentity> type) {
        List<ServiceFunction> ret = new ArrayList<>();
        if (this.map.containsKey(type)) {
            SfcSnMapper snMapper = this.map.get(type);
            Map<String, List<ServiceFunction>> snMap = snMapper.getMap();
            for (String nodeName : snMap.keySet()) {
                List<ServiceFunction> listOnNode = snMap.get(nodeName);
                ret.addAll(listOnNode);
            }
        }
        return ret;
    }
}
