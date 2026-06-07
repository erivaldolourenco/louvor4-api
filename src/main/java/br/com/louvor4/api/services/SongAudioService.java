package br.com.louvor4.api.services;

import br.com.louvor4.api.enums.SongAudioType;
import br.com.louvor4.api.shared.dto.Song.SongAudioDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface SongAudioService {
    SongAudioDTO uploadAudio(UUID songId, SongAudioType type, MultipartFile file);
}
