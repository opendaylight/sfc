package org.opendaylight.sfc.sbrest.provider.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.sfc.sbrest.json.SfstExporterFactory;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerType;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.service.function.scheduler.types.ServiceFunctionSchedulerTypeBuilder;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static junit.framework.TestCase.assertTrue;

/**
 * @author Vladimir Lavor
 * @version 0.1
 * @see SfstExporterFactory;
 * <p/>
 * @since 2015-06-1
 */

public class SbRestSfstTaskTest {

    private static final String SFST_NAME = "Dummy_SFST";
    private static final String SFST_REST_URI = "/config/service-function-scheduler-type:service-function-scheduler-types/service-function-scheduler-type/";
    private final ObjectMapper mapper = new ObjectMapper();
    private ExecutorService executorService;

    @Before
    public void init() {
        executorService = Executors.newFixedThreadPool(10);
    }

    @Test
    public void testSbRestSfstTask() throws IOException {
        SbRestSfstTask sbRestSfstTask = new SbRestSfstTask(RestOperation.PUT, this.buildServiceFunctionSchedulerType(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestSfstTask.jsonObject);
        assertTrue("Must be true", jsonObject.equals(this.buildServiceFunctionSchedulerTypeObjectNode()));
        assertTrue("Must be true", sbRestSfstTask.restUriList.get(0).contains(SFST_REST_URI));
    }

    @Test
    public void testSbRestSfstTask1() throws IOException {
        SbRestSfstTask sbRestSfstTask = new SbRestSfstTask(RestOperation.DELETE, this.buildServiceFunctionSchedulerType(), executorService);

        JsonNode jsonObject = mapper.readTree(sbRestSfstTask.jsonObject);
        assertTrue("Must be true", jsonObject.equals(buildServiceFunctionSchedulerTypeObjectNode()));
        assertTrue("Must be true", sbRestSfstTask.restUriList.get(0).contains(SFST_REST_URI));
    }

    private ServiceFunctionSchedulerType buildServiceFunctionSchedulerType() {
        ServiceFunctionSchedulerTypeBuilder serviceFunctionSchedulerTypeBuilder = new ServiceFunctionSchedulerTypeBuilder();
        serviceFunctionSchedulerTypeBuilder.setName(SFST_NAME);

        return serviceFunctionSchedulerTypeBuilder.build();
    }

    private ObjectNode buildServiceFunctionSchedulerTypeObjectNode() {
        ObjectNode topNode = mapper.createObjectNode();

        ObjectNode sfstNode = mapper.createObjectNode();
        sfstNode.put(SfstExporterFactory._NAME, SFST_NAME);

        ArrayNode arrayNode = mapper.createArrayNode();
        arrayNode.add(sfstNode);

        topNode.put(SfstExporterFactory._SERVICE_FUNCTION_SCHEDULE_TYPE, arrayNode);
        return topNode;
    }
}