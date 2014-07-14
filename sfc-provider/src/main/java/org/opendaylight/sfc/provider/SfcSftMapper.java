package org.opendaylight.sfc.provider;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.ServiceNodes;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.service.nodes.ServiceNode;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class maps service function types to service node mappers
 *
 * @see org.opendaylight.sfc.provider.SfcSnMapper
 * <p>
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @since       2014-07-14
 */
public class SfcSftMapper {
    private Map<String, SfcSnMapper> map;

    private final OpendaylightSfc odlSfc;

    public SfcSftMapper(OpendaylightSfc odlSfc){
        this.map = new HashMap<>();
        this.odlSfc = odlSfc;
        this.update();
    }

    public void update() {
        if (odlSfc != null) {
            DataObject dataObject = odlSfc.dataProvider.readConfigurationData(OpendaylightSfc.snIID);
            if (dataObject instanceof ServiceNodes) {
                ServiceNodes nodes = (ServiceNodes) dataObject;
                List<ServiceNode> snList = nodes.getServiceNode();

                for(ServiceNode sn : snList){
                    List<String> sfNameList = sn.getServiceFunction();
                    for(String sfName : sfNameList){
                        ServiceFunction sf = findServiceFunction(sfName);
                        this.add(sf.getType(), sn.getName(), sf);
                    }
                }
            } else {
                throw new IllegalStateException("Wrong dataObject instance.");
            }
        } else {
            throw new NullPointerException("odlSfc is null");
        }
    }

    public void add(String type, String snName, ServiceFunction sf){
        if(this.map.containsKey(type)){
            this.map.get(type).add(snName, sf);
        }else{
            SfcSnMapper snMapper = new SfcSnMapper(type);
            snMapper.add(snName, sf);
            this.map.put(type, snMapper);
        }
    }

    public void addAll(String type, String snName, List<ServiceFunction> sfList){
        if(this.map.containsKey(type)){
            this.map.get(type).addAll(snName, sfList);
        }else{
            SfcSnMapper snMapper = new SfcSnMapper(type);
            snMapper.addAll(snName, sfList);
            this.map.put(type, snMapper);
        }
    }

    public List<ServiceFunction> getSfList(String type) {
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

    private ServiceFunction findServiceFunction(String name) {
        ServiceFunctionKey key = new ServiceFunctionKey(name);
        InstanceIdentifier<ServiceFunction> iid =
                InstanceIdentifier.builder(ServiceFunctions.class)
                        .child(ServiceFunction.class, key)
                        .toInstance();
        DataObject dataObject = odlSfc.dataProvider.readConfigurationData(iid);
        if (dataObject instanceof ServiceFunction) {
            return (ServiceFunction) dataObject;
        } else {
            throw new IllegalStateException("Wrong dataObject instance (expected ServiceFunction).");
        }
    }


}
