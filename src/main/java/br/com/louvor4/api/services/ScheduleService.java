package br.com.louvor4.api.services;

import br.com.louvor4.api.models.EventSetlistItem;
import br.com.louvor4.api.shared.dto.Schedule.CreateTextScheduleItemRequest;
import br.com.louvor4.api.shared.dto.Schedule.ReorderScheduleRequest;
import br.com.louvor4.api.shared.dto.Schedule.ScheduleItemResponse;
import br.com.louvor4.api.shared.dto.Schedule.UpdateTextScheduleItemRequest;

import java.util.List;
import java.util.UUID;

public interface ScheduleService {

    List<ScheduleItemResponse> getSchedule(UUID eventId);

    void addTextItem(UUID eventId, CreateTextScheduleItemRequest request);

    void updateTextItem(UUID eventId, UUID itemId, UpdateTextScheduleItemRequest request);

    void deleteTextItem(UUID eventId, UUID itemId);

    void reorder(UUID eventId, ReorderScheduleRequest request);

    void onSetlistItemAdded(EventSetlistItem setlistItem);

    void onSetlistItemRemoved(UUID setlistItemId);
}
