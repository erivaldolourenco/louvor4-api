package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.SongAudioType;
import br.com.louvor4.api.repositories.SongAudioRepository;
import br.com.louvor4.api.repositories.SongRepository;
import br.com.louvor4.api.services.SongAudioService;
import br.com.louvor4.api.services.StorageService;
import br.com.louvor4.api.shared.dto.Song.SongAudioDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class SongAudioServiceImpl implements SongAudioService {

    private final SongRepository songRepository;
    private final SongAudioRepository songAudioRepository;
    private final StorageService storageService;

    public SongAudioServiceImpl(SongRepository songRepository,
                                SongAudioRepository songAudioRepository,
                                StorageService storageService) {
        this.songRepository = songRepository;
        this.songAudioRepository = songAudioRepository;
        this.storageService = storageService;
    }

    @Override
    public SongAudioDTO uploadAudio(UUID songId, SongAudioType type, MultipartFile file) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
