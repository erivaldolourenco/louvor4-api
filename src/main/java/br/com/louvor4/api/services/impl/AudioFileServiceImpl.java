package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.AudioType;
import br.com.louvor4.api.enums.FileCategory;
import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.AudioFile;
import br.com.louvor4.api.models.Medley;
import br.com.louvor4.api.models.Song;
import br.com.louvor4.api.repositories.AudioFileRepository;
import br.com.louvor4.api.repositories.MedleyRepository;
import br.com.louvor4.api.repositories.SongRepository;
import br.com.louvor4.api.services.AudioFileService;
import br.com.louvor4.api.services.StorageService;
import br.com.louvor4.api.shared.dto.Audio.AudioFileDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class AudioFileServiceImpl implements AudioFileService {

    private final SongRepository songRepository;
    private final MedleyRepository medleyRepository;
    private final AudioFileRepository audioFileRepository;
    private final StorageService storageService;

    public AudioFileServiceImpl(SongRepository songRepository,
                                MedleyRepository medleyRepository,
                                AudioFileRepository audioFileRepository,
                                StorageService storageService) {
        this.songRepository = songRepository;
        this.medleyRepository = medleyRepository;
        this.audioFileRepository = audioFileRepository;
        this.storageService = storageService;
    }

    @Override
    @Transactional
    public AudioFileDTO uploadSongAudio(UUID songId, AudioType type, MultipartFile file) {
        Song song = songRepository.getSongById(songId)
                .orElseThrow(() -> new NotFoundException("Música não encontrada."));

        validateFile(file);

        AudioFile audioFile = audioFileRepository.findBySong_IdAndType(songId, type)
                .orElseGet(AudioFile::new);
        audioFile.setSong(song);
        audioFile.setType(type);

        String audioUrl = storageService.uploadFileWithPrefix(file, FileCategory.SONG_AUDIO, songId.toString());
        audioFile.setAudioUrl(audioUrl);

        AudioFile saved = audioFileRepository.save(audioFile);

        return new AudioFileDTO(saved.getSong().getId(), null, saved.getType(), saved.getAudioUrl());
    }

    @Override
    @Transactional
    public AudioFileDTO uploadMedleyAudio(UUID medleyId, AudioType type, MultipartFile file) {
        Medley medley = medleyRepository.findById(medleyId)
                .orElseThrow(() -> new NotFoundException("Medley não encontrado."));

        validateFile(file);

        AudioFile audioFile = audioFileRepository.findByMedley_IdAndType(medleyId, type)
                .orElseGet(AudioFile::new);
        audioFile.setMedley(medley);
        audioFile.setType(type);

        String audioUrl = storageService.uploadFileWithPrefix(file, FileCategory.MEDLEY_AUDIO, medleyId.toString());
        audioFile.setAudioUrl(audioUrl);

        AudioFile saved = audioFileRepository.save(audioFile);

        return new AudioFileDTO(null, saved.getMedley().getId(), saved.getType(), saved.getAudioUrl());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("O arquivo não pode estar vazio.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            throw new ValidationException("O arquivo deve ser um áudio válido (audio/*).");
        }
    }
}
