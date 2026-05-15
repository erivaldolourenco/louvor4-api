package br.com.louvor4.api.services;

import br.com.louvor4.api.models.EventSetlistItem;
import br.com.louvor4.api.shared.dto.Program.CreateTextProgramItemRequest;
import br.com.louvor4.api.shared.dto.Program.ReorderProgramRequest;
import br.com.louvor4.api.shared.dto.Program.ProgramItemResponse;
import br.com.louvor4.api.shared.dto.Program.UpdateTextProgramItemRequest;

import java.util.List;
import java.util.UUID;

public interface ProgramService {

    List<ProgramItemResponse> getProgram(UUID eventId);

    void addTextItem(UUID eventId, CreateTextProgramItemRequest request);

    void updateTextItem(UUID eventId, UUID itemId, UpdateTextProgramItemRequest request);

    void deleteTextItem(UUID eventId, UUID itemId);

    void reorder(UUID eventId, ReorderProgramRequest request);

    void onSetlistItemAdded(EventSetlistItem setlistItem);

    void onSetlistItemRemoved(UUID setlistItemId);
}
