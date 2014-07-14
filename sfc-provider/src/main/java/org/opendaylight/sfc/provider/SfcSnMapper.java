package org.opendaylight.sfc.provider;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;

import java.util.*;

/**
 * This class maps service nodes to lists of service functions,
 * filtered by a chosen service function type,
 * i.e. node-1 -> [firewall-1, firewall-2, ...]
 *
 * <p>
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @since       2014-07-14
 */
class SfcSnMapper {
    private final String type;
    // partial list of SFs, filtered by given SFType
    // SNodeName -> SFNameList
    private Map<String, List<ServiceFunction>> map;

    public SfcSnMapper(String sfType) {
        this.type = sfType;
        this.map = new HashMap<>();
    }

    public String getType() {
        return type;
    }

    public Map<String, List<ServiceFunction>> getMap() {
        return map;
    }

    public void add(String nodeName, ServiceFunction sf) {
        if (this.map.containsKey(nodeName)) {
            this.map.get(nodeName).add(sf);
        } else {
            List<ServiceFunction> list = new ArrayList<>();
            list.add(sf);
            this.map.put(nodeName, list);
        }
    }

    public void addAll(String nodeName, List<ServiceFunction> sfList) {
        if (this.map.containsKey(nodeName)) {
            this.map.get(nodeName).addAll(sfList);
        } else {
            this.map.put(nodeName, sfList);
        }
    }

}
