package org.opendaylight.sfc.l2renderer;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.ServiceFunctionDictionary;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.ServiceFunctionDictionary1;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.ofs.rev150408.port.details.OfsPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcL2Utils {

    private static final Logger LOG = LoggerFactory.getLogger(SfcL2Utils.class);

    public SfcL2Utils() {
    }

    public static ServiceFunctionDictionary getSffSfDictionary(ServiceFunctionForwarder sff, String sfName) {
        ServiceFunctionDictionary sffSfDict = null;

        List<ServiceFunctionDictionary> sffSfDictList = sff.getServiceFunctionDictionary();
        for (ServiceFunctionDictionary dict : sffSfDictList) {
            if(dict.getName().equals(sfName)) {
                sffSfDict = dict;
                break;
            }
        }
        return sffSfDict;
    }

    public static String getDictPortInfoPort(final ServiceFunctionDictionary dict) {
        OfsPort ofsPort = getSffPortInfoFromSffSfDict(dict);

        if(ofsPort == null) {
            // This case is most likely because the sff-of augmentation wasnt used
            // assuming the packet should just be sent on the same port it was received on
            return OutputPortValues.INPORT.toString();
        }

        return ofsPort.getPortId();
    }

    public static OfsPort getSffPortInfoFromSffSfDict(final ServiceFunctionDictionary sffSfDict) {
        if(sffSfDict == null) {
            return null;
        }
        ServiceFunctionDictionary1 ofsSffSfDict = sffSfDict.getAugmentation(ServiceFunctionDictionary1.class);
        if(ofsSffSfDict == null) {
            LOG.debug("No OFS SffSf Dictionary available for dict [{}]", sffSfDict.getName());
            return null;
        }

        return ofsSffSfDict.getOfsPort();
    }




}
