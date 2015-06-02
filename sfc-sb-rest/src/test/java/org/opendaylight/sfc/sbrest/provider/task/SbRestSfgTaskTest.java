package org.opendaylight.sfc.sbrest.provider.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.sfc.sbrest.json.SfExporterFactory;
import org.opendaylight.sfc.sbrest.json.SfgExporterFactory;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroup;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfg.rev150214.service.function.groups.ServiceFunctionGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNull;

/**
 * @author Vladimir Lavor
 * @version 0.1
 * @see SfgExporterFactory;
 * <p/>
 * @since 2015-06-1
 */

public class SbRestSfgTaskTest {

    private static final String SFG_NAME = "Dummy_SFG";
    private static final String REST_URI = "http://localhost:5000";
    private final ObjectMapper mapper = new ObjectMapper();
    private ExecutorService executorService;

    @Before
    public void init() {
        executorService = Executors.newFixedThreadPool(10);
    }

    @Test
    public void testSbRestSfgTask() throws IOException {
        SbRestSfgTask sbRestSfgTask = new SbRestSfgTask(RestOperation.PUT, this.buildServiceFunctionGroup(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestSfgTask.jsonObject);
        assertTrue("Must be true", jsonObject.equals(this.buildServiceFunctionGroupObjectNode()));
        assertTrue("Must be true", sbRestSfgTask.restUriList.get(0).contains(REST_URI));
    }

    @Test
    public void testSbRestSfgTask1() throws IOException {
        SbRestSfgTask sbRestSfgTask = new SbRestSfgTask(RestOperation.DELETE, this.buildServiceFunctionGroup(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestSfgTask.jsonObject);
        assertTrue("Must be true", jsonObject.equals(buildServiceFunctionObjectNode1()));
        assertTrue("Must be true", sbRestSfgTask.restUriList.get(0).contains(REST_URI));
    }

    @Test
    public void testSbRestSfgTaskEmpty() throws IOException {
        SbRestSfgTask sbRestSfgTask = new SbRestSfgTask(RestOperation.PUT, new ServiceFunctionGroupBuilder().build(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestSfgTask.jsonObject);
        Assert.assertTrue(jsonObject.equals(this.buildServiceFunctionTopNode()));
        assertNull(sbRestSfgTask.restUriList);
    }

    private ServiceFunctionGroup buildServiceFunctionGroup() {
        ServiceFunctionGroupBuilder serviceFunctionGroupBuilder = new ServiceFunctionGroupBuilder();
        serviceFunctionGroupBuilder.setName(SFG_NAME);
        serviceFunctionGroupBuilder.setRestUri(new Uri(REST_URI));

        return serviceFunctionGroupBuilder.build();
    }

    private ObjectNode buildServiceFunctionGroupObjectNode() {
        ObjectNode topNode = mapper.createObjectNode();

        ObjectNode sfgNode = mapper.createObjectNode();
        sfgNode.put(SfgExporterFactory._NAME, SFG_NAME);
        sfgNode.put(SfgExporterFactory._REST_URI, REST_URI);

        ArrayNode arrayNode = mapper.createArrayNode();
        arrayNode.add(sfgNode);

        topNode.put(SfgExporterFactory._SERVICE_FUNCTION_GROUP, arrayNode);
        return topNode;
    }

    private ObjectNode buildServiceFunctionObjectNode1() {
        ObjectNode topNode = mapper.createObjectNode();

        ObjectNode sfgNode = mapper.createObjectNode();
        sfgNode.put(SfExporterFactory._NAME, SFG_NAME);

        ArrayNode arrayNode = mapper.createArrayNode();
        arrayNode.add(sfgNode);

        topNode.put(SfgExporterFactory._SERVICE_FUNCTION_GROUP, arrayNode);
        return topNode;
    }

    private ObjectNode buildServiceFunctionTopNode() {
        ObjectNode topNode = mapper.createObjectNode();
        ArrayNode arrayNode = mapper.createArrayNode();
        ObjectNode sfgNode = mapper.createObjectNode();

        arrayNode.add(sfgNode);
        topNode.put(SfgExporterFactory._SERVICE_FUNCTION_GROUP, arrayNode);
        return topNode;
    }
}