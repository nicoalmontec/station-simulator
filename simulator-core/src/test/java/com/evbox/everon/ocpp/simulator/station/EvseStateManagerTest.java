package com.evbox.everon.ocpp.simulator.station;

import com.evbox.everon.ocpp.simulator.station.evse.CableStatus;
import com.evbox.everon.ocpp.simulator.station.evse.Connector;
import com.evbox.everon.ocpp.simulator.station.evse.Evse;
import com.evbox.everon.ocpp.simulator.station.states.*;
import com.evbox.everon.ocpp.simulator.station.subscription.Subscriber;
import com.evbox.everon.ocpp.v20.message.station.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static com.evbox.everon.ocpp.mock.constants.StationConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvseStateManagerTest {

    @Mock
    Evse evseMock;

    @Mock
    StationStore stationStoreMock;

    @Mock
    StationMessageSender stationMessageSenderMock;

    private EvseStateManager evseStateManager;

    @BeforeEach
    void setUp() {

        this.evseStateManager = new EvseStateManager(null, stationStoreMock, stationMessageSenderMock);
        checkStateIs(AvailableState.NAME);
    }

    @Test
    void verifyFullStateFlowPlugThenAuthorize() {
        when(stationStoreMock.findEvse(anyInt())).thenReturn(evseMock);
        when(evseMock.findConnector(anyInt())).thenReturn(new Connector(DEFAULT_CONNECTOR_ID, CableStatus.UNPLUGGED, StatusNotificationRequest.ConnectorStatus.AVAILABLE));

        evseStateManager.cablePlugged(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID);
        checkStateIs(WaitingForAuthorizationState.NAME);

        evseStateManager.authorized(DEFAULT_EVSE_ID, DEFAULT_TOKEN_ID);
        checkStateIs(ChargingState.NAME);

        evseStateManager.authorized(DEFAULT_EVSE_ID, DEFAULT_TOKEN_ID);
        checkStateIs(StoppedState.NAME);

        evseStateManager.cableUnplugged(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID);
        checkStateIs(AvailableState.NAME);
    }

    @Test
    void verifyFullStateFlowAuthorizeThenPlug() {
        when(stationStoreMock.findEvse(anyInt())).thenReturn(evseMock);
        when(evseMock.findConnector(anyInt())).thenReturn(new Connector(DEFAULT_CONNECTOR_ID, CableStatus.UNPLUGGED, StatusNotificationRequest.ConnectorStatus.AVAILABLE));

        evseStateManager.authorized(DEFAULT_EVSE_ID, DEFAULT_TOKEN_ID);
        checkStateIs(WaitingForPlugState.NAME);

        evseStateManager.cablePlugged(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID);
        checkStateIs(ChargingState.NAME);

        evseStateManager.authorized(DEFAULT_EVSE_ID, DEFAULT_TOKEN_ID);
        checkStateIs(StoppedState.NAME);

        evseStateManager.cableUnplugged(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID);
        checkStateIs(AvailableState.NAME);
    }

    @Test
    void verifyStopChargingAndRestart() {
        when(stationStoreMock.findEvse(anyInt())).thenReturn(evseMock);
        when(evseMock.findConnector(anyInt())).thenReturn(new Connector(DEFAULT_CONNECTOR_ID, CableStatus.UNPLUGGED, StatusNotificationRequest.ConnectorStatus.AVAILABLE));

        evseStateManager.cablePlugged(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID);
        checkStateIs(WaitingForAuthorizationState.NAME);

        evseStateManager.authorized(DEFAULT_EVSE_ID, DEFAULT_TOKEN_ID);
        checkStateIs(ChargingState.NAME);

        evseStateManager.authorized(DEFAULT_EVSE_ID, DEFAULT_TOKEN_ID);
        checkStateIs(StoppedState.NAME);

        evseStateManager.authorized(DEFAULT_EVSE_ID, DEFAULT_TOKEN_ID);
        ArgumentCaptor<Subscriber<AuthorizeRequest, AuthorizeResponse>> subscriberCaptor = ArgumentCaptor.forClass(Subscriber.class);

        verify(stationMessageSenderMock, times(3)).sendAuthorizeAndSubscribe(anyString(), anyList(), subscriberCaptor.capture());


        AuthorizeResponse authorizeResponse = new AuthorizeResponse()
                .withIdTokenInfo(new IdTokenInfo().withStatus(IdTokenInfo.Status.ACCEPTED))
                .withEvseId(Collections.singletonList(DEFAULT_EVSE_ID));
        subscriberCaptor.getValue().onResponse(new AuthorizeRequest(), authorizeResponse);

        checkStateIs(ChargingState.NAME);
    }

    @Test
    void verifyAuthorizeAndAuthorize() {
        evseStateManager.authorized(DEFAULT_EVSE_ID, DEFAULT_TOKEN_ID);
        checkStateIs(WaitingForPlugState.NAME);

        evseStateManager.authorized(DEFAULT_EVSE_ID, DEFAULT_TOKEN_ID);
        checkStateIs(AvailableState.NAME);
    }

    @Test
    void verifyPlugAndUnplug() {
        when(stationStoreMock.findEvse(anyInt())).thenReturn(evseMock);
        when(evseMock.findConnector(anyInt())).thenReturn(new Connector(DEFAULT_CONNECTOR_ID, CableStatus.UNPLUGGED, StatusNotificationRequest.ConnectorStatus.AVAILABLE));

        evseStateManager.cablePlugged(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID);
        checkStateIs(WaitingForAuthorizationState.NAME);

        evseStateManager.cableUnplugged(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID);
        checkStateIs(AvailableState.NAME);
    }

    private void checkStateIs(String name) {
        assertThat(evseStateManager.getStateForEvse(DEFAULT_EVSE_ID).getStateName()).isEqualTo(name);
    }

}