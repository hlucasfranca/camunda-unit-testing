import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.camunda.bpm.extension.process_test_coverage.junit.rules.TestCoverageProcessEngineRule;
import org.camunda.bpm.extension.process_test_coverage.junit.rules.TestCoverageProcessEngineRuleBuilder;
import org.camunda.bpm.scenario.ProcessScenario;
import org.camunda.bpm.scenario.Scenario;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;
import static org.mockito.Mockito.*;

@Deployment(resources = {"process.bpmn", "subprocesso.bpmn"})
public class CalculadoraTest {
    public static final String PROCESS_KEY = "primeiro-projeto-camunda-process";
    public static final String TASK_FORMULARIO_CALCULADORA = "Task_FormularioCalculadora";
    public static final String OPERACAO = "operacao";
    public static final String A = "a";
    public static final String B = "b";
    @Rule
    @ClassRule
    public static TestCoverageProcessEngineRule rule = TestCoverageProcessEngineRuleBuilder.create()
            .assertClassCoverageAtLeast(0.2)
            .build();
    @Mock
    private ProcessScenario processo;
    @Mock
    ProcessScenario subprocesso;
    @Before
    public void defaultScenario() {
        MockitoAnnotations.openMocks(this);
        Mocks.register("subprocesso", subprocesso);

        when(processo.waitsAtUserTask("Task_Resultado")).thenReturn(task -> {
            task.complete();
        });
    }
    @Test
    public void deveTestarSoma() {

        when(processo.waitsAtUserTask(TASK_FORMULARIO_CALCULADORA))
                .thenReturn(task -> {
                    task.complete(withVariables(OPERACAO, "soma", A, 1, B, 2));
                });

        when(processo.runsCallActivity("Activity_0glriyd")).thenReturn(Scenario.use(subprocesso));

        when(subprocesso.waitsAtMessageIntermediateThrowEvent("Event_EnviaMensagem"))
                .thenReturn(externalTaskDelegate -> externalTaskDelegate.complete(withVariables("codigo_retorno", "erro_1")))
                .thenReturn(externalTaskDelegate -> externalTaskDelegate.complete(withVariables("codigo_retorno", "erro_2")))
                .thenReturn(externalTaskDelegate -> externalTaskDelegate.complete(withVariables("codigo_retorno", "erro_2")))
                .thenReturn(externalTaskDelegate -> externalTaskDelegate.complete(withVariables("codigo_retorno", "erro_1")))
                .thenReturn(externalTaskDelegate -> externalTaskDelegate.complete(withVariables("codigo_retorno", "erro_3")))
                .thenReturn(externalTaskDelegate -> externalTaskDelegate.complete(withVariables("codigo_retorno", "erro_3")))
                .thenReturn(externalTaskDelegate -> externalTaskDelegate.complete(withVariables("codigo_retorno", "erro_2")));

        when(subprocesso.waitsAtEventBasedGateway("Gateway_Retorno")).thenReturn(eventBasedGatewayDelegate -> {
            String processInstanceId = eventBasedGatewayDelegate.getProcessInstance().getRootProcessInstanceId();
            ProcessInstance processInstance = runtimeService().createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            runtimeService().createMessageCorrelation("Message_3nhg1qm").processInstanceId(processInstance.getProcessInstanceId()).correlate();
        });

        when(subprocesso.waitsAtTimerIntermediateEvent("Event_TimerRetentativa")).thenReturn(processInstanceDelegate -> {});

        ProcessInstance instance = Scenario.run(processo)
                .startByKey(PROCESS_KEY)
//                .fromBefore("Activity_0glriyd")
                .execute()
                .instance(processo);

        Object objResultado = historyService()
                .createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("resultado")
                .singleResult()
                .getValue();

        Long l = Long.parseLong(objResultado.toString());

        assertThat(l).isEqualTo(3);

        Object contagemRetentativas = historyService().createHistoricVariableInstanceQuery()
                .variableName("contagem_retentativas")
                .singleResult()
                .getValue();

        assertThat(contagemRetentativas).isEqualTo(Map.of("erro_1", 2, "erro_2", 4, "erro_3", 2));

        verify(processo, never()).hasCompleted("Activity_1yq1ul7");
    }

//    @Test
//    public void deveTestarSubtracao() {
//
//        //Happy-Path
//        when(testDeliveryProcess.waitsAtUserTask(TASK_FORMULARIO_CALCULADORA))
//                .thenReturn(task -> {
//                    task.complete(withVariables(OPERACAO, "subtracao", A, 99, B, 88));
//                });
//
//        ProcessInstance instance = Scenario.run(testDeliveryProcess)
//                .startByKey(PROCESS_KEY)
//                .execute().instance(testDeliveryProcess);
//
//
//        Object objResultado = historyService().createHistoricVariableInstanceQuery().processInstanceId(instance.getId()).variableName("resultado").singleResult().getValue();
//        Long l = Long.parseLong(objResultado.toString());
//
//        assertThat(l).isEqualTo(11);
//    }
//
//    @Test
//    public void deveTestarDivisao() {
//
//        //Happy-Path
//        when(testDeliveryProcess.waitsAtUserTask(TASK_FORMULARIO_CALCULADORA))
//                .thenReturn(task -> {
//                    task.complete(withVariables(OPERACAO, "divisao", A, 99, B, 3));
//                });
//
//        Scenario execute = Scenario.run(testDeliveryProcess)
//                .startByKey(PROCESS_KEY)
//                .execute();
//
//        ProcessInstance instance = execute.instance(testDeliveryProcess);
//
//
//        Object objResultado = historyService().createHistoricVariableInstanceQuery().processInstanceId(instance.getId()).variableName("resultado").singleResult().getValue();
//        Long l = Long.parseLong(objResultado.toString());
//
//        assertThat(l).isEqualTo(33);
//    }
//
//    @Test
//    public void deveTestarMultiplicacao() {
//
//        //Happy-Path
//        when(testDeliveryProcess.waitsAtUserTask(TASK_FORMULARIO_CALCULADORA))
//                .thenReturn(task -> {
//                    task.complete(withVariables(OPERACAO, "multiplicacao", A, 3, B, 3));
//                });
//
//        ProcessInstance instance = Scenario.run(testDeliveryProcess)
//                .startByKey(PROCESS_KEY)
//                .execute().instance(testDeliveryProcess);
//
//
//        Object objResultado = historyService().createHistoricVariableInstanceQuery().processInstanceId(instance.getId()).variableName("resultado").singleResult().getValue();
//        Long l = Long.parseLong(objResultado.toString());
//
//        assertThat(l).isEqualTo(9);
//    }
}
