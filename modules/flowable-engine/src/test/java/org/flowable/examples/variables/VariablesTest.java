/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.examples.variables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.flowable.engine.common.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.DataObject;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tom Baeyens
 */
public class VariablesTest extends PluggableFlowableTestCase {

    @Deployment
    public void testBasicVariableOperations() {
        processEngineConfiguration.getVariableTypes().addType(CustomVariableType.instance);

        Date now = new Date();
        List<String> serializable = new ArrayList<>();
        serializable.add("one");
        serializable.add("two");
        serializable.add("three");
        byte[] bytes1 = "somebytes1".getBytes();
        byte[] bytes2 = "somebytes2".getBytes();

        // 2000 characters * 2 bytes = 4000 bytes
        StringBuilder long2000StringBuilder = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            long2000StringBuilder.append("z");
        }

        // 2001 characters * 2 bytes = 4002 bytes
        StringBuilder long2001StringBuilder = new StringBuilder();

        for (int i = 0; i < 2000; i++) {
            long2001StringBuilder.append("a");
        }
        long2001StringBuilder.append("a");

        // 4002 characters
        StringBuilder long4001StringBuilder = new StringBuilder();

        for (int i = 0; i < 4000; i++) {
            long4001StringBuilder.append("a");
        }
        long4001StringBuilder.append("a");

        // Start process instance with different types of variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("longVar", 928374L);
        variables.put("shortVar", (short) 123);
        variables.put("integerVar", 1234);
        variables.put("longString2000chars", long2000StringBuilder.toString());
        variables.put("stringVar", "coca-cola");
        variables.put("dateVar", now);
        variables.put("nullVar", null);
        variables.put("serializableVar", serializable);
        variables.put("bytesVar", bytes1);
        variables.put("customVar1", new CustomType(bytes2));
        variables.put("customVar2", null);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("taskAssigneeProcess", variables);

        variables = runtimeService.getVariables(processInstance.getId());
        assertEquals(928374L, variables.get("longVar"));
        assertEquals((short) 123, variables.get("shortVar"));
        assertEquals(1234, variables.get("integerVar"));
        assertEquals("coca-cola", variables.get("stringVar"));
        assertEquals(long2000StringBuilder.toString(), variables.get("longString2000chars"));
        assertEquals(now, variables.get("dateVar"));
        assertNull(variables.get("nullVar"));
        assertEquals(serializable, variables.get("serializableVar"));
        assertTrue(Arrays.equals(bytes1, (byte[]) variables.get("bytesVar")));
        assertEquals(new CustomType(bytes2), variables.get("customVar1"));
        assertNull(variables.get("customVar2"));
        assertEquals(11, variables.size());

        // Set all existing variables values to null
        runtimeService.setVariable(processInstance.getId(), "longVar", null);
        runtimeService.setVariable(processInstance.getId(), "shortVar", null);
        runtimeService.setVariable(processInstance.getId(), "integerVar", null);
        runtimeService.setVariable(processInstance.getId(), "stringVar", null);
        runtimeService.setVariable(processInstance.getId(), "longString2000chars", null);
        runtimeService.setVariable(processInstance.getId(), "longString4000chars", null);
        runtimeService.setVariable(processInstance.getId(), "dateVar", null);
        runtimeService.setVariable(processInstance.getId(), "nullVar", null);
        runtimeService.setVariable(processInstance.getId(), "serializableVar", null);
        runtimeService.setVariable(processInstance.getId(), "bytesVar", null);
        runtimeService.setVariable(processInstance.getId(), "customVar1", null);
        runtimeService.setVariable(processInstance.getId(), "customVar2", null);

        variables = runtimeService.getVariables(processInstance.getId());
        assertNull(variables.get("longVar"));
        assertNull(variables.get("shortVar"));
        assertNull(variables.get("integerVar"));
        assertNull(variables.get("stringVar"));
        assertNull(variables.get("longString2000chars"));
        assertNull(variables.get("longString4000chars"));
        assertNull(variables.get("dateVar"));
        assertNull(variables.get("nullVar"));
        assertNull(variables.get("serializableVar"));
        assertNull(variables.get("bytesVar"));
        assertNull(variables.get("customVar1"));
        assertNull(variables.get("customVar2"));
        assertEquals(12, variables.size());

        // Update existing variable values again, and add a new variable
        runtimeService.setVariable(processInstance.getId(), "new var", "hi");
        runtimeService.setVariable(processInstance.getId(), "longVar", 9987L);
        runtimeService.setVariable(processInstance.getId(), "shortVar", (short) 456);
        runtimeService.setVariable(processInstance.getId(), "integerVar", 4567);
        runtimeService.setVariable(processInstance.getId(), "stringVar", "colgate");
        runtimeService.setVariable(processInstance.getId(), "longString2000chars", long2001StringBuilder.toString());
        runtimeService.setVariable(processInstance.getId(), "longString4000chars", long4001StringBuilder.toString());
        runtimeService.setVariable(processInstance.getId(), "dateVar", now);
        runtimeService.setVariable(processInstance.getId(), "serializableVar", serializable);
        runtimeService.setVariable(processInstance.getId(), "bytesVar", bytes1);
        runtimeService.setVariable(processInstance.getId(), "customVar1", new CustomType(bytes2));
        runtimeService.setVariable(processInstance.getId(), "customVar2", new CustomType(bytes1));

        variables = runtimeService.getVariables(processInstance.getId());
        assertEquals("hi", variables.get("new var"));
        assertEquals(9987L, variables.get("longVar"));
        assertEquals((short) 456, variables.get("shortVar"));
        assertEquals(4567, variables.get("integerVar"));
        assertEquals("colgate", variables.get("stringVar"));
        assertEquals(long2001StringBuilder.toString(), variables.get("longString2000chars"));
        assertEquals(long4001StringBuilder.toString(), variables.get("longString4000chars"));
        assertEquals(now, variables.get("dateVar"));
        assertNull(variables.get("nullVar"));
        assertEquals(serializable, variables.get("serializableVar"));
        assertTrue(Arrays.equals(bytes1, (byte[]) variables.get("bytesVar")));
        assertEquals(new CustomType(bytes2), variables.get("customVar1"));
        assertEquals(new CustomType(bytes1), variables.get("customVar2"));
        assertEquals(13, variables.size());

        Collection<String> varFilter = new ArrayList<>(2);
        varFilter.add("stringVar");
        varFilter.add("integerVar");

        Map<String, Object> filteredVariables = runtimeService.getVariables(processInstance.getId(), varFilter);
        assertEquals(2, filteredVariables.size());
        assertTrue(filteredVariables.containsKey("stringVar"));
        assertTrue(filteredVariables.containsKey("integerVar"));

        // Try setting the value of the variable that was initially created with value 'null'
        runtimeService.setVariable(processInstance.getId(), "nullVar", "a value");
        Object newValue = runtimeService.getVariable(processInstance.getId(), "nullVar");
        assertNotNull(newValue);
        assertEquals("a value", newValue);

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

    }

    @Deployment
    public void testLocalizeVariables() {
        // Start process instance with different types of variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("stringVar", "coca-cola");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("localizeVariables", variables);

        Map<String, VariableInstance> variableInstances = runtimeService.getVariableInstances(processInstance.getId());
        assertEquals(1, variableInstances.size());
        assertEquals("stringVar", variableInstances.get("stringVar").getName());
        assertEquals("coca-cola", variableInstances.get("stringVar").getValue());

        List<String> variableNames = new ArrayList<>();
        variableNames.add("stringVar");

        // getVariablesInstances via names
        variableInstances = runtimeService.getVariableInstances(processInstance.getId(), variableNames);
        assertEquals(1, variableInstances.size());
        assertEquals("stringVar", variableInstances.get("stringVar").getName());
        assertEquals("coca-cola", variableInstances.get("stringVar").getValue());

        // getVariableInstancesLocal via names
        variableInstances = runtimeService.getVariableInstancesLocal(processInstance.getId(), variableNames);
        assertEquals(1, variableInstances.size());
        assertEquals("stringVar", variableInstances.get("stringVar").getName());
        assertEquals("coca-cola", variableInstances.get("stringVar").getValue());

        // getVariableInstance
        VariableInstance variableInstance = runtimeService.getVariableInstance(processInstance.getId(), "stringVar");
        assertNotNull(variableInstance);
        assertEquals("stringVar", variableInstance.getName());
        assertEquals("coca-cola", variableInstance.getValue());

        // getVariableInstanceLocal
        variableInstance = runtimeService.getVariableInstanceLocal(processInstance.getId(), "stringVar");
        assertNotNull(variableInstance);
        assertEquals("stringVar", variableInstance.getName());
        assertEquals("coca-cola", variableInstance.getValue());

        // Verify TaskService behavior
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        variableInstances = taskService.getVariableInstances(task.getId());
        assertEquals(2, variableInstances.size());
        assertEquals("stringVar", variableInstances.get("stringVar").getName());
        assertEquals("coca-cola", variableInstances.get("stringVar").getValue());
        assertEquals("intVar", variableInstances.get("intVar").getName());
        assertNull(variableInstances.get("intVar").getValue());

        variableNames = new ArrayList<>();
        variableNames.add("stringVar");

        // getVariablesInstances via names
        variableInstances = taskService.getVariableInstances(task.getId(), variableNames);
        assertEquals(1, variableInstances.size());
        assertEquals("stringVar", variableInstances.get("stringVar").getName());
        assertEquals("coca-cola", variableInstances.get("stringVar").getValue());

        taskService.setVariableLocal(task.getId(), "stringVar", "pepsi-cola");

        // getVariableInstancesLocal via names
        variableInstances = taskService.getVariableInstancesLocal(task.getId(), variableNames);
        assertEquals(1, variableInstances.size());
        assertEquals("stringVar", variableInstances.get("stringVar").getName());
        assertEquals("pepsi-cola", variableInstances.get("stringVar").getValue());

        // getVariableInstance
        variableInstance = taskService.getVariableInstance(task.getId(), "stringVar");
        assertNotNull(variableInstance);
        assertEquals("stringVar", variableInstance.getName());
        assertEquals("pepsi-cola", variableInstance.getValue());

        // getVariableInstanceLocal
        variableInstance = taskService.getVariableInstanceLocal(task.getId(), "stringVar");
        assertNotNull(variableInstance);
        assertEquals("stringVar", variableInstance.getName());
        assertEquals("pepsi-cola", variableInstance.getValue());
    }

    @Deployment
    public void testLocalizeDataObjects() {
        // Start process instance with different types of variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("stringVar", "coca-cola");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("localizeVariables", variables);
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        ObjectNode infoNode = dynamicBpmnService.getProcessDefinitionInfo(processInstance.getProcessDefinitionId());
        dynamicBpmnService.changeLocalizationName("en-US", "stringVarId", "stringVar 'en-US' Name", infoNode);
        dynamicBpmnService.changeLocalizationDescription("en-US", "stringVarId", "stringVar 'en-US' Description", infoNode);
        dynamicBpmnService.changeLocalizationName("en-AU", "stringVarId", "stringVar 'en-AU' Name", infoNode);
        dynamicBpmnService.changeLocalizationDescription("en-AU", "stringVarId", "stringVar 'en-AU' Description", infoNode);
        dynamicBpmnService.changeLocalizationName("en", "stringVarId", "stringVar 'en' Name", infoNode);
        dynamicBpmnService.changeLocalizationDescription("en", "stringVarId", "stringVar 'en' Description", infoNode);

        dynamicBpmnService.changeLocalizationName("en-US", "intVarId", "intVar 'en-US' Name", infoNode);
        dynamicBpmnService.changeLocalizationDescription("en-US", "intVarId", "intVar 'en-US' Description", infoNode);
        dynamicBpmnService.changeLocalizationName("en-AU", "intVarId", "intVar 'en-AU' Name", infoNode);
        dynamicBpmnService.changeLocalizationDescription("en-AU", "intVarId", "intVar 'en-AU' Description", infoNode);
        dynamicBpmnService.changeLocalizationName("en", "intVarId", "intVar 'en' Name", infoNode);
        dynamicBpmnService.changeLocalizationDescription("en", "intVarId", "intVar 'en' Description", infoNode);

        dynamicBpmnService.saveProcessDefinitionInfo(processInstance.getProcessDefinitionId(), infoNode);

        Map<String, DataObject> dataObjects = runtimeService.getDataObjects(processInstance.getId(), "es", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'es' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'es' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = runtimeService.getDataObjects(processInstance.getId(), "it", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'it' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'it' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        // getDataObjects
        dataObjects = runtimeService.getDataObjects(processInstance.getId(), "en-US", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'en-US' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'en-US' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = runtimeService.getDataObjects(processInstance.getId(), "en-AU", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'en-AU' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'en-AU' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = runtimeService.getDataObjects(processInstance.getId(), "en-GB", true);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'en' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'en' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = runtimeService.getDataObjects(processInstance.getId(), "en-GB", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'default' description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        List<String> variableNames = new ArrayList<>();
        variableNames.add("stringVar");

        // getDataObjects via names
        dataObjects = runtimeService.getDataObjects(processInstance.getId(), variableNames, "es", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'es' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'es' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());


        dataObjects = runtimeService.getDataObjects(processInstance.getId(), variableNames, "it", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'it' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'it' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = runtimeService.getDataObjects(processInstance.getId(), variableNames, "en-US", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'en-US' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'en-US' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = runtimeService.getDataObjects(processInstance.getId(), variableNames, "en-AU", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'en-AU' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'en-AU' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = runtimeService.getDataObjects(processInstance.getId(), variableNames, "en-GB", true);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'en' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'en' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = runtimeService.getDataObjects(processInstance.getId(), variableNames, "en-GB", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'default' description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        // getDataObjectsLocal
        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), "es", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'es' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'es' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), "it", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'it' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'it' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), "en-US", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'en-US' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'en-US' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), "en-AU", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'en-AU' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'en-AU' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), "en-GB", true);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'en' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'en' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), "en-GB", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'default' description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), "ja-JA", true);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'default' description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        // getDataObjectsLocal via names
        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), variableNames, "es", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'es' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'es' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), variableNames, "it", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'it' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'it' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), variableNames, "en-US", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'en-US' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'en-US' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), variableNames, "en-AU", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'en-AU' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'en-AU' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), variableNames, "en-GB", true);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'en' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'en' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), variableNames, "en-GB", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'default' description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        // getDataObject
        DataObject dataObject = runtimeService.getDataObject(processInstance.getId(), "stringVar", "es", false);
        assertNotNull(dataObject);
        assertEquals("stringVar", dataObject.getName());
        assertEquals("coca-cola", dataObject.getValue());
        assertEquals("stringVar 'es' Name", dataObject.getLocalizedName());
        assertEquals("stringVar 'es' Description", dataObject.getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObject = runtimeService.getDataObject(processInstance.getId(), "stringVar", "it", false);
        assertNotNull(dataObject);
        assertEquals("stringVar", dataObject.getName());
        assertEquals("coca-cola", dataObject.getValue());
        assertEquals("stringVar 'it' Name", dataObject.getLocalizedName());
        assertEquals("stringVar 'it' Description", dataObject.getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObject = runtimeService.getDataObject(processInstance.getId(), "stringVar", "en-GB", false);
        assertNotNull(dataObject);
        assertEquals("stringVar", dataObject.getName());
        assertEquals("coca-cola", dataObject.getValue());
        assertEquals("stringVar", dataObject.getLocalizedName());
        assertEquals("stringVar 'default' description", dataObject.getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObject = runtimeService.getDataObject(processInstance.getId(), "stringVar", "en-US", false);
        assertNotNull(dataObject);
        assertEquals("stringVar", dataObject.getName());
        assertEquals("coca-cola", dataObject.getValue());
        assertEquals("stringVar 'en-US' Name", dataObject.getLocalizedName());
        assertEquals("stringVar 'en-US' Description", dataObject.getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObject = runtimeService.getDataObject(processInstance.getId(), "stringVar", "en-AU", false);
        assertNotNull(dataObject);
        assertEquals("stringVar", dataObject.getName());
        assertEquals("coca-cola", dataObject.getValue());
        assertEquals("stringVar 'en-AU' Name", dataObject.getLocalizedName());
        assertEquals("stringVar 'en-AU' Description", dataObject.getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObject = runtimeService.getDataObject(processInstance.getId(), "stringVar", "en-GB", true);
        assertNotNull(dataObject);
        assertEquals("stringVar", dataObject.getName());
        assertEquals("coca-cola", dataObject.getValue());
        assertEquals("stringVar 'en' Name", dataObject.getLocalizedName());
        assertEquals("stringVar 'en' Description", dataObject.getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObject = runtimeService.getDataObject(processInstance.getId(), "stringVar", "en-GB", false);
        assertNotNull(dataObject);
        assertEquals("stringVar", dataObject.getName());
        assertEquals("coca-cola", dataObject.getValue());
        assertEquals("stringVar", dataObject.getLocalizedName());
        assertEquals("stringVar 'default' description", dataObject.getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        // getDataObjectLocal
        dataObject = runtimeService.getDataObjectLocal(processInstance.getId(), "stringVar", "es", false);
        assertNotNull(dataObject);
        assertEquals("stringVar", dataObject.getName());
        assertEquals("coca-cola", dataObject.getValue());
        assertEquals("stringVar 'es' Name", dataObject.getLocalizedName());
        assertEquals("stringVar 'es' Description", dataObject.getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObject = runtimeService.getDataObjectLocal(processInstance.getId(), "stringVar", "it", false);
        assertNotNull(dataObject);
        assertEquals("stringVar", dataObject.getName());
        assertEquals("coca-cola", dataObject.getValue());
        assertEquals("stringVar 'it' Name", dataObject.getLocalizedName());
        assertEquals("stringVar 'it' Description", dataObject.getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObject = runtimeService.getDataObjectLocal(processInstance.getId(), "stringVar", "en-US", false);
        assertNotNull(dataObject);
        assertEquals("stringVar", dataObject.getName());
        assertEquals("coca-cola", dataObject.getValue());
        assertEquals("stringVar 'en-US' Name", dataObject.getLocalizedName());
        assertEquals("stringVar 'en-US' Description", dataObject.getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObject = runtimeService.getDataObjectLocal(processInstance.getId(), "stringVar", "en-AU", false);
        assertNotNull(dataObject);
        assertEquals("stringVar", dataObject.getName());
        assertEquals("coca-cola", dataObject.getValue());
        assertEquals("stringVar 'en-AU' Name", dataObject.getLocalizedName());
        assertEquals("stringVar 'en-AU' Description", dataObject.getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObject = runtimeService.getDataObjectLocal(processInstance.getId(), "stringVar", "en-GB", true);
        assertNotNull(dataObject);
        assertEquals("stringVar", dataObject.getName());
        assertEquals("coca-cola", dataObject.getValue());
        assertEquals("stringVar 'en' Name", dataObject.getLocalizedName());
        assertEquals("stringVar 'en' Description", dataObject.getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObject = runtimeService.getDataObjectLocal(processInstance.getId(), "stringVar", "en-GB", false);
        assertNotNull(dataObject);
        assertEquals("stringVar", dataObject.getName());
        assertEquals("coca-cola", dataObject.getValue());
        assertEquals("stringVar", dataObject.getLocalizedName());
        assertEquals("stringVar 'default' description", dataObject.getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        Execution subprocess = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("subprocess1").singleResult();

        dataObjects = runtimeService.getDataObjects(subprocess.getId(), "es", false);
        assertEquals(2, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'es' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'es' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        // getDataObjects
        dataObjects = runtimeService.getDataObjects(subprocess.getId(), "es", false);
        assertEquals(2, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'es' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'es' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar 'es' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'es' Description", dataObjects.get("intVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObjects = runtimeService.getDataObjects(subprocess.getId(), "it", false);
        assertEquals(2, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'it' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'it' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar 'it' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'it' Description", dataObjects.get("intVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObjects = runtimeService.getDataObjects(subprocess.getId(), "en-US", false);
        assertEquals(2, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'en-US' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'en-US' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar 'en-US' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'en-US' Description", dataObjects.get("intVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObjects = runtimeService.getDataObjects(subprocess.getId(), "en-AU", false);
        assertEquals(2, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'en-AU' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'en-AU' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar 'en-AU' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'en-AU' Description", dataObjects.get("intVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObjects = runtimeService.getDataObjects(subprocess.getId(), "en-GB", true);
        assertEquals(2, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'en' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'en' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar 'en' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'en' Description", dataObjects.get("intVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObjects = runtimeService.getDataObjects(subprocess.getId(), "en-GB", false);
        assertEquals(2, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'default' description", dataObjects.get("stringVar").getDescription());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'default' description", dataObjects.get("intVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());
        assertNotNull(dataObjects.get("intVar").getId());

        // getDataObjects via names (from subprocess)

        variableNames.add("intVar");
        dataObjects = runtimeService.getDataObjects(subprocess.getId(), variableNames, "es", false);
        assertEquals(2, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'es' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'es' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar 'es' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'es' Description", dataObjects.get("intVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObjects = runtimeService.getDataObjects(subprocess.getId(), variableNames, "it", false);
        assertEquals(2, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'it' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'it' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar 'it' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'it' Description", dataObjects.get("intVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObjects = runtimeService.getDataObjects(subprocess.getId(), variableNames, "en-US", false);
        assertEquals(2, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'en-US' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'en-US' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar 'en-US' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'en-US' Description", dataObjects.get("intVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObjects = runtimeService.getDataObjects(subprocess.getId(), variableNames, "en-AU", false);
        assertEquals(2, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'en-AU' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'en-AU' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar 'en-AU' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'en-AU' Description", dataObjects.get("intVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObjects = runtimeService.getDataObjects(subprocess.getId(), variableNames, "en-GB", true);
        assertEquals(2, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'en' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'en' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar 'en' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'en' Description", dataObjects.get("intVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObjects = runtimeService.getDataObjects(subprocess.getId(), variableNames, "en-GB", false);
        assertEquals(2, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'default' description", dataObjects.get("stringVar").getDescription());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'default' description", dataObjects.get("intVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());
        assertNotNull(dataObjects.get("intVar").getId());

        // getDataObjectsLocal
        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), "es", false);
        assertEquals(1, dataObjects.size());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar 'es' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'es' Description", dataObjects.get("intVar").getDescription());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), "it", false);
        assertEquals(1, dataObjects.size());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar 'it' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'it' Description", dataObjects.get("intVar").getDescription());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), "en-US", false);
        assertEquals(1, dataObjects.size());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar 'en-US' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'en-US' Description", dataObjects.get("intVar").getDescription());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), "en-AU", false);
        assertEquals(1, dataObjects.size());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar 'en-AU' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'en-AU' Description", dataObjects.get("intVar").getDescription());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), "en-GB", true);
        assertEquals(1, dataObjects.size());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar 'en' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'en' Description", dataObjects.get("intVar").getDescription());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), "en-GB", false);
        assertEquals(1, dataObjects.size());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'default' description", dataObjects.get("intVar").getDescription());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), "ja-JA", true);
        assertEquals(1, dataObjects.size());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'default' description", dataObjects.get("intVar").getDescription());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        // getDataObjectsLocal via names
        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), variableNames, "es", false);
        assertEquals(1, dataObjects.size());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar 'es' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'es' Description", dataObjects.get("intVar").getDescription());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), variableNames, "it", false);
        assertEquals(1, dataObjects.size());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar 'it' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'it' Description", dataObjects.get("intVar").getDescription());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), variableNames, "en-US", false);
        assertEquals(1, dataObjects.size());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar 'en-US' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'en-US' Description", dataObjects.get("intVar").getDescription());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), variableNames, "en-AU", false);
        assertEquals(1, dataObjects.size());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar 'en-AU' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'en-AU' Description", dataObjects.get("intVar").getDescription());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), variableNames, "en-GB", true);
        assertEquals(1, dataObjects.size());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar 'en' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'en' Description", dataObjects.get("intVar").getDescription());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), variableNames, "en-GB", false);
        assertEquals(1, dataObjects.size());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertNull(dataObjects.get("intVar").getValue());
        assertEquals("intVar", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'default' description", dataObjects.get("intVar").getDescription());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        // getDataObject (in subprocess)
        dataObject = runtimeService.getDataObject(subprocess.getId(), "intVar", "es", false);
        assertNotNull(dataObject);
        assertEquals("intVar", dataObject.getName());
        assertNull(dataObject.getValue());
        assertEquals("intVar 'es' Name", dataObject.getLocalizedName());
        assertEquals("intVar 'es' Description", dataObject.getDescription());
        assertEquals("intVarId", dataObject.getDataObjectDefinitionKey());
        assertEquals("int", dataObject.getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObject = runtimeService.getDataObject(subprocess.getId(), "intVar", "it", false);
        assertNotNull(dataObject);
        assertEquals("intVar", dataObject.getName());
        assertNull(dataObject.getValue());
        assertEquals("intVar 'it' Name", dataObject.getLocalizedName());
        assertEquals("intVar 'it' Description", dataObject.getDescription());
        assertEquals("intVarId", dataObject.getDataObjectDefinitionKey());
        assertEquals("int", dataObject.getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObject = runtimeService.getDataObject(subprocess.getId(), "intVar", "en-GB", false);
        assertNotNull(dataObject);
        assertEquals("intVar", dataObject.getName());
        assertNull(dataObject.getValue());
        assertEquals("intVar", dataObject.getLocalizedName());
        assertEquals("intVar 'default' description", dataObject.getDescription());
        assertEquals("intVarId", dataObject.getDataObjectDefinitionKey());
        assertEquals("int", dataObject.getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObject = runtimeService.getDataObject(subprocess.getId(), "intVar", "en-US", false);
        assertNotNull(dataObject);
        assertEquals("intVar", dataObject.getName());
        assertNull(dataObject.getValue());
        assertEquals("intVar 'en-US' Name", dataObject.getLocalizedName());
        assertEquals("intVar 'en-US' Description", dataObject.getDescription());
        assertEquals("intVarId", dataObject.getDataObjectDefinitionKey());
        assertEquals("int", dataObject.getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObject = runtimeService.getDataObject(subprocess.getId(), "intVar", "en-AU", false);
        assertNotNull(dataObject);
        assertEquals("intVar", dataObject.getName());
        assertNull(dataObject.getValue());
        assertEquals("intVar 'en-AU' Name", dataObject.getLocalizedName());
        assertEquals("intVar 'en-AU' Description", dataObject.getDescription());
        assertEquals("intVarId", dataObject.getDataObjectDefinitionKey());
        assertEquals("int", dataObject.getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObject = runtimeService.getDataObject(subprocess.getId(), "intVar", "en-GB", true);
        assertNotNull(dataObject);
        assertEquals("intVar", dataObject.getName());
        assertNull(dataObject.getValue());
        assertEquals("intVar 'en' Name", dataObject.getLocalizedName());
        assertEquals("intVar 'en' Description", dataObject.getDescription());
        assertEquals("intVarId", dataObject.getDataObjectDefinitionKey());
        assertEquals("int", dataObject.getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObject = runtimeService.getDataObject(subprocess.getId(), "intVar", "en-GB", false);
        assertNotNull(dataObject);
        assertEquals("intVar", dataObject.getName());
        assertNull(dataObject.getValue());
        assertEquals("intVar", dataObject.getLocalizedName());
        assertEquals("intVar 'default' description", dataObject.getDescription());
        assertEquals("intVarId", dataObject.getDataObjectDefinitionKey());
        assertEquals("int", dataObject.getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        // getDataObjectLocal (in subprocess)
        dataObject = runtimeService.getDataObjectLocal(subprocess.getId(), "intVar", "es", false);
        assertNotNull(dataObject);
        assertEquals("intVar", dataObject.getName());
        assertNull(dataObject.getValue());
        assertEquals("intVar 'es' Name", dataObject.getLocalizedName());
        assertEquals("intVar 'es' Description", dataObject.getDescription());
        assertEquals("intVarId", dataObject.getDataObjectDefinitionKey());
        assertEquals("int", dataObject.getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObject = runtimeService.getDataObjectLocal(subprocess.getId(), "intVar", "it", false);
        assertNotNull(dataObject);
        assertEquals("intVar", dataObject.getName());
        assertNull(dataObject.getValue());
        assertEquals("intVar 'it' Name", dataObject.getLocalizedName());
        assertEquals("intVar 'it' Description", dataObject.getDescription());
        assertEquals("intVarId", dataObject.getDataObjectDefinitionKey());
        assertEquals("int", dataObject.getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObject = runtimeService.getDataObjectLocal(subprocess.getId(), "intVar", "en-US", false);
        assertNotNull(dataObject);
        assertEquals("intVar", dataObject.getName());
        assertNull(dataObject.getValue());
        assertEquals("intVar 'en-US' Name", dataObject.getLocalizedName());
        assertEquals("intVar 'en-US' Description", dataObject.getDescription());
        assertEquals("intVarId", dataObject.getDataObjectDefinitionKey());
        assertEquals("int", dataObject.getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObject = runtimeService.getDataObjectLocal(subprocess.getId(), "intVar", "en-AU", false);
        assertNotNull(dataObject);
        assertEquals("intVar", dataObject.getName());
        assertNull(dataObject.getValue());
        assertEquals("intVar 'en-AU' Name", dataObject.getLocalizedName());
        assertEquals("intVar 'en-AU' Description", dataObject.getDescription());
        assertEquals("intVarId", dataObject.getDataObjectDefinitionKey());
        assertEquals("int", dataObject.getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObject = runtimeService.getDataObjectLocal(subprocess.getId(), "intVar", "en-GB", true);
        assertNotNull(dataObject);
        assertEquals("intVar", dataObject.getName());
        assertNull(dataObject.getValue());
        assertEquals("intVar 'en' Name", dataObject.getLocalizedName());
        assertEquals("intVar 'en' Description", dataObject.getDescription());
        assertEquals("intVarId", dataObject.getDataObjectDefinitionKey());
        assertEquals("int", dataObject.getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        dataObject = runtimeService.getDataObjectLocal(subprocess.getId(), "intVar", "en-GB", false);
        assertNotNull(dataObject);
        assertEquals("intVar", dataObject.getName());
        assertNull(dataObject.getValue());
        assertEquals("intVar", dataObject.getLocalizedName());
        assertEquals("intVar 'default' description", dataObject.getDescription());
        assertEquals("intVarId", dataObject.getDataObjectDefinitionKey());
        assertEquals("int", dataObject.getType());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());

        // Verify TaskService behavior
        dataObjects = taskService.getDataObjects(task.getId());
        assertEquals(2, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'default' description", dataObjects.get("stringVar").getDescription());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals("intVar", dataObject.getName());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertEquals("intVar 'default' description", dataObjects.get("intVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());
        assertNotNull(dataObjects.get("stringVar").getId());

        // getDataObjects
        dataObjects = taskService.getDataObjects(task.getId());
        assertEquals(2, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'default' description", dataObjects.get("stringVar").getDescription());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals("intVar", dataObject.getName());
        assertEquals("intVar", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'default' description", dataObjects.get("intVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = taskService.getDataObjects(task.getId(), "es", false);
        assertEquals(2, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'es' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'es' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals("intVar", dataObject.getName());
        assertEquals("intVar 'es' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'es' Description", dataObjects.get("intVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = taskService.getDataObjects(task.getId(), "it", false);
        assertEquals(2, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'it' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'it' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals("intVar", dataObject.getName());
        assertEquals("intVar 'it' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'it' Description", dataObjects.get("intVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = taskService.getDataObjects(task.getId(), "en-US", false);
        assertEquals(2, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'en-US' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'en-US' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals("intVar", dataObject.getName());
        assertEquals("intVar 'en-US' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'en-US' Description", dataObjects.get("intVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = taskService.getDataObjects(task.getId(), "en-AU", false);
        assertEquals(2, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'en-AU' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'en-AU' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertEquals("intVar 'en-AU' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'en-AU' Description", dataObjects.get("intVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = taskService.getDataObjects(task.getId(), "en-GB", true);
        assertEquals(2, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'en' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'en' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertEquals("intVar 'en' Name", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'en' Description", dataObjects.get("intVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = taskService.getDataObjects(task.getId(), "en-GB", false);
        assertEquals(2, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'default' description", dataObjects.get("stringVar").getDescription());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals("intVar", dataObjects.get("intVar").getName());
        assertEquals("intVar", dataObjects.get("intVar").getLocalizedName());
        assertEquals("intVar 'default' description", dataObjects.get("intVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("intVarId", dataObjects.get("intVar").getDataObjectDefinitionKey());
        assertEquals("int", dataObjects.get("intVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertEquals(processInstance.getId(), dataObjects.get("intVar").getProcessInstanceId());
        assertEquals(subprocess.getId(), dataObjects.get("intVar").getExecutionId());
        assertNotNull(dataObjects.get("intVar").getId());
        assertNotNull(dataObjects.get("stringVar").getId());

        variableNames = new ArrayList<>();
        variableNames.add("stringVar");

        // getDataObjects via names
        dataObjects = taskService.getDataObjects(task.getId(), variableNames, "es", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'es' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'es' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = taskService.getDataObjects(task.getId(), variableNames, "it", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'it' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'it' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = taskService.getDataObjects(task.getId(), variableNames, "en-US", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'en-US' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'en-US' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = taskService.getDataObjects(task.getId(), variableNames, "en-AU", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'en-AU' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'en-AU' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = taskService.getDataObjects(task.getId(), variableNames, "en-GB", true);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar 'en' Name", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'en' Description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObjects = taskService.getDataObjects(task.getId(), variableNames, "en-GB", false);
        assertEquals(1, dataObjects.size());
        assertEquals("stringVar", dataObjects.get("stringVar").getName());
        assertEquals("coca-cola", dataObjects.get("stringVar").getValue());
        assertEquals("stringVar", dataObjects.get("stringVar").getLocalizedName());
        assertEquals("stringVar 'default' description", dataObjects.get("stringVar").getDescription());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("stringVarId", dataObjects.get("stringVar").getDataObjectDefinitionKey());
        assertEquals("string", dataObjects.get("stringVar").getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        // getDataObject
        dataObject = taskService.getDataObject(task.getId(), "stringVar");
        assertNotNull(dataObject);
        assertEquals("stringVar", dataObject.getName());
        assertEquals("coca-cola", dataObject.getValue());
        assertEquals("stringVar", dataObject.getLocalizedName());
        assertEquals("stringVar 'default' description", dataObject.getDescription());
        assertEquals("stringVarId", dataObject.getDataObjectDefinitionKey());
        assertEquals("string", dataObject.getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObject = taskService.getDataObject(task.getId(), "stringVar", "es", false);
        assertNotNull(dataObject);
        assertEquals("stringVar", dataObject.getName());
        assertEquals("coca-cola", dataObject.getValue());
        assertEquals("stringVar 'es' Name", dataObject.getLocalizedName());
        assertEquals("stringVar 'es' Description", dataObject.getDescription());
        assertEquals("stringVarId", dataObject.getDataObjectDefinitionKey());
        assertEquals("string", dataObject.getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObject = taskService.getDataObject(task.getId(), "stringVar", "it", false);
        assertNotNull(dataObject);
        assertEquals("stringVar", dataObject.getName());
        assertEquals("coca-cola", dataObject.getValue());
        assertEquals("stringVar 'it' Name", dataObject.getLocalizedName());
        assertEquals("stringVar 'it' Description", dataObject.getDescription());
        assertEquals("stringVarId", dataObject.getDataObjectDefinitionKey());
        assertEquals("string", dataObject.getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObject = taskService.getDataObject(task.getId(), "stringVar", "en-GB", false);
        assertNotNull(dataObject);
        assertEquals("stringVar", dataObject.getName());
        assertEquals("coca-cola", dataObject.getValue());
        assertEquals("stringVar", dataObject.getLocalizedName());
        assertEquals("stringVar 'default' description", dataObject.getDescription());
        assertEquals("stringVarId", dataObject.getDataObjectDefinitionKey());
        assertEquals("string", dataObject.getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObject = taskService.getDataObject(task.getId(), "stringVar", "en-US", false);
        assertNotNull(dataObject);
        assertEquals("stringVar", dataObject.getName());
        assertEquals("coca-cola", dataObject.getValue());
        assertEquals("stringVar 'en-US' Name", dataObject.getLocalizedName());
        assertEquals("stringVar 'en-US' Description", dataObject.getDescription());
        assertEquals("stringVarId", dataObject.getDataObjectDefinitionKey());
        assertEquals("string", dataObject.getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObject = taskService.getDataObject(task.getId(), "stringVar", "en-AU", false);
        assertNotNull(dataObject);
        assertEquals("stringVar", dataObject.getName());
        assertEquals("coca-cola", dataObject.getValue());
        assertEquals("stringVar 'en-AU' Name", dataObject.getLocalizedName());
        assertEquals("stringVar 'en-AU' Description", dataObject.getDescription());
        assertEquals("stringVarId", dataObject.getDataObjectDefinitionKey());
        assertEquals("string", dataObject.getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObject = taskService.getDataObject(task.getId(), "stringVar", "en-GB", true);
        assertNotNull(dataObject);
        assertEquals("stringVar", dataObject.getName());
        assertEquals("coca-cola", dataObject.getValue());
        assertEquals("stringVar 'en' Name", dataObject.getLocalizedName());
        assertEquals("stringVar 'en' Description", dataObject.getDescription());
        assertEquals("stringVarId", dataObject.getDataObjectDefinitionKey());
        assertEquals("string", dataObject.getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());

        dataObject = taskService.getDataObject(task.getId(), "stringVar", "en-GB", false);
        assertNotNull(dataObject);
        assertEquals("stringVar", dataObject.getName());
        assertEquals("coca-cola", dataObject.getValue());
        assertEquals("stringVar", dataObject.getLocalizedName());
        assertEquals("stringVar 'default' description", dataObject.getDescription());
        assertEquals("stringVarId", dataObject.getDataObjectDefinitionKey());
        assertEquals("string", dataObject.getType());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getProcessInstanceId());
        assertEquals(processInstance.getId(), dataObjects.get("stringVar").getExecutionId());
        assertNotNull(dataObjects.get("stringVar").getId());
    }

    // Test case for ACT-1839
    @Deployment(resources = { "org/flowable/examples/variables/VariablesTest.testChangeTypeSerializable.bpmn20.xml" })
    public void testChangeTypeSerializable() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("variable-type-change-test");
        assertNotNull(processInstance);
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("Activiti is awesome!", task.getName());
        SomeSerializable myVar = (SomeSerializable) runtimeService.getVariable(processInstance.getId(), "myVar");
        assertEquals("someValue", myVar.getValue());
    }

    public String getVariableInstanceId(String executionId, String name) {
        HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery().processInstanceId(executionId).variableName(name).singleResult();
        return variable.getId();
    }

    // test case for ACT-1082
    @Deployment(resources = { "org/flowable/examples/variables/VariablesTest.testBasicVariableOperations.bpmn20.xml" })
    public void testChangeVariableType() {

        Date now = new Date();
        List<String> serializable = new ArrayList<>();
        serializable.add("one");
        serializable.add("two");
        serializable.add("three");
        byte[] bytes = "somebytes".getBytes();

        // Start process instance with different types of variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("longVar", 928374L);
        variables.put("shortVar", (short) 123);
        variables.put("integerVar", 1234);
        variables.put("stringVar", "coca-cola");
        variables.put("dateVar", now);
        variables.put("nullVar", null);
        variables.put("serializableVar", serializable);
        variables.put("bytesVar", bytes);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("taskAssigneeProcess", variables);

        variables = runtimeService.getVariables(processInstance.getId());
        assertEquals(928374L, variables.get("longVar"));
        assertEquals((short) 123, variables.get("shortVar"));
        assertEquals(1234, variables.get("integerVar"));
        assertEquals("coca-cola", variables.get("stringVar"));
        assertEquals(now, variables.get("dateVar"));
        assertNull(variables.get("nullVar"));
        assertEquals(serializable, variables.get("serializableVar"));
        assertTrue(Arrays.equals(bytes, (byte[]) variables.get("bytesVar")));
        assertEquals(8, variables.size());

        // check if the id of the variable is the same or not

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            String oldSerializableVarId = getVariableInstanceId(processInstance.getId(), "serializableVar");
            String oldLongVar = getVariableInstanceId(processInstance.getId(), "longVar");

            // Change type of serializableVar from serializable to Short
            Map<String, Object> newVariables = new HashMap<>();
            newVariables.put("serializableVar", (short) 222);
            runtimeService.setVariables(processInstance.getId(), newVariables);
            variables = runtimeService.getVariables(processInstance.getId());
            assertEquals((short) 222, variables.get("serializableVar"));

            String newSerializableVarId = getVariableInstanceId(processInstance.getId(), "serializableVar");

            assertEquals(oldSerializableVarId, newSerializableVarId);

            // Change type of a longVar from Long to Short
            newVariables = new HashMap<>();
            newVariables.put("longVar", (short) 123);
            runtimeService.setVariables(processInstance.getId(), newVariables);
            variables = runtimeService.getVariables(processInstance.getId());
            assertEquals((short) 123, variables.get("longVar"));

            String newLongVar = getVariableInstanceId(processInstance.getId(), "longVar");
            assertEquals(oldLongVar, newLongVar);
        }
    }

    // test case for ACT-1428
    @Deployment
    public void testNullVariable() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("taskAssigneeProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        Map<String, String> variables = new HashMap<>();
        variables.put("testProperty", "434");

        formService.submitTaskFormData(task.getId(), variables);
        String resultVar = (String) runtimeService.getVariable(processInstance.getId(), "testProperty");

        assertEquals("434", resultVar);

        task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        // If no variable is given, no variable should be set and script test should throw exception
        processInstance = runtimeService.startProcessInstanceByKey("taskAssigneeProcess");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        variables = new HashMap<>();
        try {
            formService.submitTaskFormData(task.getId(), variables);
            fail("Should throw exception as testProperty is not defined and used in Script task");
        } catch (Exception e) {
            runtimeService.deleteProcessInstance(processInstance.getId(), "intentional exception in script task");

            assertEquals("class org.flowable.engine.common.api.FlowableException", e.getClass().toString());
        }

        // No we put null property, This should be put into the variable. We do not expect exceptions
        processInstance = runtimeService.startProcessInstanceByKey("taskAssigneeProcess");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        variables = new HashMap<>();
        variables.put("testProperty", null);

        try {
            formService.submitTaskFormData(task.getId(), variables);
        } catch (Exception e) {
            fail("Should not throw exception as the testProperty is defined, although null");
        }
        resultVar = (String) runtimeService.getVariable(processInstance.getId(), "testProperty");

        assertNull(resultVar);

        runtimeService.deleteProcessInstance(processInstance.getId(), "intentional exception in script task");
    }

    /**
     * Test added to validate UUID variable type + querying (ACT-1665)
     */
    @Deployment
    public void testUUIDVariableAndQuery() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertNotNull(processInstance);

        // Check UUID variable type query on task
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertNotNull(task);
        UUID randomUUID = UUID.randomUUID();
        taskService.setVariableLocal(task.getId(), "conversationId", randomUUID);

        org.flowable.task.api.Task resultingTask = taskService.createTaskQuery().taskVariableValueEquals("conversationId", randomUUID).singleResult();
        assertNotNull(resultingTask);
        assertEquals(task.getId(), resultingTask.getId());

        randomUUID = UUID.randomUUID();

        // Check UUID variable type query on process
        runtimeService.setVariable(processInstance.getId(), "uuidVar", randomUUID);
        ProcessInstance result = runtimeService.createProcessInstanceQuery().variableValueEquals("uuidVar", randomUUID).singleResult();

        assertNotNull(result);
        assertEquals(processInstance.getId(), result.getId());
    }

}

class CustomType {
    private byte[] value;

    public CustomType(byte[] value) {
        if (value == null) {
            throw new NullPointerException();
        }
        this.value = value;
    }

    public byte[] getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(value);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        CustomType other = (CustomType) obj;
        return Arrays.equals(value, other.value);
    }

}

/**
 * A custom variable type for testing byte array value handling.
 *
 * @author Marcus Klimstra (CGI)
 */
class CustomVariableType implements VariableType {
    public static final CustomVariableType instance = new CustomVariableType();

    @Override
    public String getTypeName() {
        return "CustomVariableType";
    }

    @Override
    public boolean isCachable() {
        return true;
    }

    @Override
    public boolean isAbleToStore(Object value) {
        return value == null || value instanceof CustomType;
    }

    @Override
    public void setValue(Object o, ValueFields valueFields) {
        // ensure calling setBytes multiple times no longer causes any problems
        valueFields.setBytes(new byte[] { 1, 2, 3 });
        valueFields.setBytes(null);
        valueFields.setBytes(new byte[] { 4, 5, 6 });

        byte[] value = (o == null ? null : ((CustomType) o).getValue());
        valueFields.setBytes(value);
    }

    @Override
    public Object getValue(ValueFields valueFields) {
        byte[] bytes = valueFields.getBytes();
        return bytes == null ? null : new CustomType(bytes);
    }

}
