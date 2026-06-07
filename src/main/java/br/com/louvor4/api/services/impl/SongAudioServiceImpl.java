package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.FileCategory;
import br.com.louvor4.api.enums.SongAudioType;
import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.Song;
import br.com.louvor4.api.models.SongAudio;
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
        Song song = songRepository.getSongById(songId)
                .orElseThrow(() -> new NotFoundException("Música não encontrada."));

        if (file == null || file.isEmpty()) {
            throw new ValidationException("O arquivo não pode estar vazio.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            throw new ValidationException("O arquivo deve ser um áudio válido (audio/*).");
        }

        String audioUrl = storageService.uploadFileWithPrefix(file, FileCategory.SONG_AUDIO, songId.toString());

        SongAudio songAudio = songAudioRepository.findBySong_IdAndType(songId, type)
                .orElseGet(SongAudio::new);
        songAudio.setSong(song);
        songAudio.setType(type);
        songAudio.setAudioUrl(audioUrl);

        SongAudio saved = songAudioRepository.save(songAudio);

        return new SongAudioDTO(songId, saved.getType(), saved.getAudioUrl());
    }
}
