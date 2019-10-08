package com.evbox.everon.ocpp.simulator.station.actions.system;

import com.evbox.everon.ocpp.simulator.station.StationMessageSender;
import com.evbox.everon.ocpp.simulator.station.StationStore;
import com.evbox.everon.ocpp.simulator.station.evse.StateManager;
import com.evbox.everon.ocpp.simulator.station.evse.states.AvailableState;
import com.evbox.everon.ocpp.simulator.station.evse.states.WaitingForPlugState;
import com.evbox.everon.ocpp.v20.message.station.StatusNotificationRequest;
import com.evbox.everon.ocpp.v20.message.station.TransactionData;
import com.evbox.everon.ocpp.v20.message.station.TransactionEventRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents request to release the connector for an evse.
 */
@Slf4j
@Getter
@AllArgsConstructor
public class CancelRemoteStartTransaction implements SystemMessage {

    private final Integer evseId;
    private final Integer connectorId;

    /**
     * Makes the connector specified available again.
     *
     * @param stationStore stores data of station
     * @param stationMessageSender station message sender
     * @param stateManager manage state flow for evse
     */
    @Override
    public void perform(StationStore stationStore, StationMessageSender stationMessageSender, StateManager stateManager) {
        if (WaitingForPlugState.NAME.equals(stateManager.getStateForEvse(evseId).getStateName())) {
            stationMessageSender.sendStatusNotification(evseId, connectorId, StatusNotificationRequest.ConnectorStatus.AVAILABLE);
            stationStore.findEvse(evseId).stopTransaction();
            stationMessageSender.sendTransactionEventEnded(evseId, connectorId, TransactionEventRequest.TriggerReason.EV_CONNECT_TIMEOUT, TransactionData.StoppedReason.TIMEOUT);
            stateManager.setStateForEvse(evseId, new AvailableState());
        }
    }
}
