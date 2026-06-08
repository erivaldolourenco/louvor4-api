package br.com.louvor4.api.services;

import br.com.louvor4.api.enums.AudioType;
import br.com.louvor4.api.shared.dto.Audio.AudioFileDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface AudioFileService {
    AudioFileDTO uploadSongAudio(UUID songId, AudioType type, MultipartFile file);
    AudioFileDTO uploadMedleyAudio(UUID medleyId, AudioType type, MultipartFile file);
}
