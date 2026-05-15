package br.com.louvor4.api.services;

import br.com.louvor4.api.shared.dto.Medley.CreateMedleyRequest;
import br.com.louvor4.api.shared.dto.Medley.MedleyResponse;
import br.com.louvor4.api.shared.dto.Medley.UpdateMedleyRequest;

import java.util.List;
import java.util.UUID;

public interface MedleyService {
    MedleyResponse create(CreateMedleyRequest request);
    List<MedleyResponse> listFromCurrentUser();
    MedleyResponse update(UUID medleyId, UpdateMedleyRequest request);
    void delete(UUID medleyId);
}
