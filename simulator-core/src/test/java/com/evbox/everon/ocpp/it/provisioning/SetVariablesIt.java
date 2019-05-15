package com.evbox.everon.ocpp.it.provisioning;

import com.evbox.everon.ocpp.mock.StationSimulatorSetUp;
import com.evbox.everon.ocpp.simulator.configuration.SimulatorConfiguration.StationConfiguration;
import com.evbox.everon.ocpp.simulator.message.ActionType;
import com.evbox.everon.ocpp.simulator.message.Call;
import com.evbox.everon.ocpp.simulator.station.component.ocppcommctrlr.HeartbeatIntervalVariableAccessor;
import com.evbox.everon.ocpp.simulator.station.component.ocppcommctrlr.OCPPCommCtrlrComponent;
import com.evbox.everon.ocpp.v20.message.centralserver.SetVariableDatum;
import com.evbox.everon.ocpp.v20.message.centralserver.SetVariablesRequest;
import com.evbox.everon.ocpp.v20.message.centralserver.SetVariablesResponse;
import org.junit.jupiter.api.Test;

import static com.evbox.everon.ocpp.mock.constants.StationConstants.DEFAULT_CALL_ID;
import static com.evbox.everon.ocpp.mock.constants.StationConstants.STATION_ID;
import static com.evbox.everon.ocpp.mock.constants.VariableConstants.BASIC_AUTH_PASSWORD_VARIABLE_NAME;
import static com.evbox.everon.ocpp.mock.constants.VariableConstants.SECURITY_COMPONENT_NAME;
import static com.evbox.everon.ocpp.mock.factory.SetVariablesCreator.createSetVariablesRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class SetVariablesIt extends StationSimulatorSetUp {

    @Test
    void shouldReplyToSetVariablesRequest() {

        int expectedNumberOfVariables = 1;

        stationSimulatorRunner.run();

        SetVariablesRequest setVariablesRequest = createSetVariablesRequest(
                "ReservationFeature",
                "ReserveConnectorZeroSupported",
                "true",
                SetVariableDatum.AttributeType.TARGET);

        Call call = new Call(DEFAULT_CALL_ID, ActionType.SET_VARIABLES, setVariablesRequest);

        ocppMockServer.waitUntilConnected();

        SetVariablesResponse response =
                ocppServerClient.findStationSender(STATION_ID).sendMessage(call.toJson(), SetVariablesResponse.class);

        await().untilAsserted(() -> {
            assertThat(response.getSetVariableResult().size()).isEqualTo(expectedNumberOfVariables);
            ocppMockServer.verify();
        });
    }

    @Test
    void shouldSetHeartbeatIntervalWithSetVariablesRequest() {

        int newHeartbeatInterval = 120;

        stationSimulatorRunner.run();

        SetVariablesRequest setVariablesRequest = createSetVariablesRequest(
                OCPPCommCtrlrComponent.NAME,
                HeartbeatIntervalVariableAccessor.NAME,
                String.valueOf(newHeartbeatInterval),
                SetVariableDatum.AttributeType.ACTUAL);

        Call call = new Call(DEFAULT_CALL_ID, ActionType.SET_VARIABLES, setVariablesRequest);

        ocppMockServer.waitUntilConnected();

        ocppServerClient.findStationSender(STATION_ID).sendMessage(call.toJson());

        await().untilAsserted(() -> {
            int heartbeatInterval = stationSimulatorRunner.getStation(STATION_ID).getStateView().getHeartbeatInterval();
            assertThat(heartbeatInterval).isEqualTo(newHeartbeatInterval);
            ocppMockServer.verify();
        });
    }

    @Test
    void shouldSetBasicAuthPassword() {

        String expectedPassword = "abc";

        stationSimulatorRunner.run();

        SetVariablesRequest setVariablesRequest = createSetVariablesRequest(
                SECURITY_COMPONENT_NAME,
                BASIC_AUTH_PASSWORD_VARIABLE_NAME,
                expectedPassword,
                SetVariableDatum.AttributeType.ACTUAL);

        Call call = new Call(DEFAULT_CALL_ID, ActionType.SET_VARIABLES, setVariablesRequest);

        ocppMockServer.waitUntilConnected();

        ocppServerClient.findStationSender(STATION_ID).sendMessage(call.toJson());

        await().untilAsserted(() -> {
            StationConfiguration configuration = stationSimulatorRunner.getStation(STATION_ID).getConfiguration();

            assertThat(configuration.getPassword()).isEqualTo(expectedPassword);

            ocppMockServer.verify();
        });
    }

}
