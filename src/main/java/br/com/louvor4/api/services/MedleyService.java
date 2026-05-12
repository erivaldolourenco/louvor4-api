package br.com.louvor4.api.services;

import br.com.louvor4.api.shared.dto.Medley.CreateMedleyRequest;
import br.com.louvor4.api.shared.dto.Medley.MedleyResponse;

import java.util.List;

public interface MedleyService {
    MedleyResponse create(CreateMedleyRequest request);
    List<MedleyResponse> listFromCurrentUser();
}
