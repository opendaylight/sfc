package org.opendaylight.sfc.sbrest.json;

import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessList;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AclExporterFactory implements ExporterFactory {
    @Override
    public Exporter getExporter() {
        return new AclExporter();
    }
}

class AclExporter implements Exporter {

    private static final Logger LOG = LoggerFactory.getLogger(AclExporter.class);

    @Override
    public String exportJson(DataObject dataObject) {

        String ret;
        if (dataObject instanceof AccessList) {
            AccessList acl = (AccessList) dataObject;

            ObjectMapper mapper = new ObjectMapper();

            ObjectNode node = mapper.getNodeFactory().objectNode();
            node.put("acl-name", acl.getAclName());

            ret = "{ \"access-list\" : " + node.toString() + " }";
            LOG.debug("Created Access List JSON: {}", ret);

        } else {
            throw new IllegalArgumentException("Argument is not an instance of Access List");
        }

        return ret;
    }

    @Override
    public String exportJsonNameOnly(DataObject dataObject) {

        String ret = null;
        if (dataObject instanceof AccessList) {
            AccessList acl = (AccessList) dataObject;

            ObjectMapper mapper = new ObjectMapper();

            ObjectNode node = mapper.getNodeFactory().objectNode();
            node.put("acl-name", acl.getAclName());

            ret = "{ \"access-list\" : " + node.toString() + " }";
            LOG.debug("Created Access List JSON: {}", ret);

        } else {
            throw new IllegalArgumentException("Argument is not an instance of Access List");
        }

        return ret;
    }
}

