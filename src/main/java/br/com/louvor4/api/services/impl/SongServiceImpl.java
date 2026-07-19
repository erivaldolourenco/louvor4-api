package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.enums.AudioType;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.mapper.SongMapper;
import br.com.louvor4.api.models.AudioFile;
import br.com.louvor4.api.models.Song;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.repositories.AudioFileRepository;
import br.com.louvor4.api.repositories.EventSetlistItemRepository;
import br.com.louvor4.api.repositories.EventSongRepository;
import br.com.louvor4.api.repositories.MedleyItemRepository;
import br.com.louvor4.api.repositories.SongRepository;
import br.com.louvor4.api.services.SongService;
import br.com.louvor4.api.shared.dto.Song.ChordSheetDTO;
import br.com.louvor4.api.shared.dto.Song.ChordSheetEditPermissionDTO;
import br.com.louvor4.api.shared.dto.Song.SongDTO;
import br.com.louvor4.api.shared.dto.Song.SongLyricsDTO;
import br.com.louvor4.api.validations.ChordSheetValidation;
import br.com.louvor4.entitlement.services.EntitlementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SongServiceImpl implements SongService {

    private final SongRepository songRepository;
    private final AudioFileRepository audioFileRepository;
    private final MedleyItemRepository medleyItemRepository;
    private final EventSongRepository eventSongRepository;
    private final EventSetlistItemRepository eventSetlistItemRepository;
    private final SongMapper songMapper;
    private final CurrentUserProvider currentUserProvider;
    private final EntitlementService entitlementService;
    private final ChordSheetValidation chordSheetValidation;

    public SongServiceImpl(SongRepository songRepository,
                           AudioFileRepository audioFileRepository,
                           MedleyItemRepository medleyItemRepository,
                           EventSongRepository eventSongRepository,
                           EventSetlistItemRepository eventSetlistItemRepository,
                           SongMapper songMapper,
                           CurrentUserProvider currentUserProvider,
                           EntitlementService entitlementService,
                           ChordSheetValidation chordSheetValidation) {
        this.songRepository = songRepository;
        this.audioFileRepository = audioFileRepository;
        this.medleyItemRepository = medleyItemRepository;
        this.eventSongRepository = eventSongRepository;
        this.eventSetlistItemRepository = eventSetlistItemRepository;
        this.songMapper = songMapper;
        this.currentUserProvider = currentUserProvider;
        this.entitlementService = entitlementService;
        this.chordSheetValidation = chordSheetValidation;
    }

    @Override
    public SongDTO create(SongDTO createDto) {
        User creator = currentUserProvider.get();
        long current = songRepository.countByUser_Id(creator.getId());
        entitlementService.enforceLimit(creator.getId(), "max_songs", current);
        Song song = songMapper.toEntity(createDto);
        song.setUser(creator);
        Song saved = songRepository.save(song);
        return songMapper.toDto(saved);
    }

    @Override
    public List<SongDTO> getSongsFromUser() {
        User user = currentUserProvider.get();
        List<Song> songs = songRepository.getSongByUser_Id(user.getId());
        return songs.stream().map(song -> {
            String referenceAudioUrl = audioFileRepository
                    .findBySong_IdAndType(song.getId(), AudioType.REFERENCE)
                    .map(AudioFile::getAudioUrl)
                    .orElse(null);
            return new SongDTO(
                    song.getId(),
                    song.getTitle(),
                    song.getArtist(),
                    song.getKey(),
                    song.getBpm(),
                    song.getYouTubeUrl(),
                    song.getNotes(),
                    referenceAudioUrl
            );
        }).toList();
    }

    @Override
    public SongDTO update(SongDTO updateDto) {
        if (updateDto == null || updateDto.id() == null) {
            throw new ValidationException("Id da música é obrigatório para atualizar.");
        }

        Song song = songRepository.getSongById(updateDto.id())
                .orElseThrow(() -> new ValidationException("Música não encontrada."));

        song.setTitle(updateDto.title());
        song.setArtist(updateDto.artist());
        song.setKey(updateDto.key());
        song.setBpm(updateDto.bpm());
        song.setYouTubeUrl(updateDto.youTubeUrl());
        song.setNotes(updateDto.notes());

        Song saved = songRepository.save(song);
        return songMapper.toDto(saved);
    }

    @Override
    public SongDTO get(UUID songId) {
        Song song = songRepository.getSongById(songId)
                .orElseThrow(() -> new ValidationException("Música não encontrada."));

        String referenceAudioUrl = audioFileRepository
                .findBySong_IdAndType(song.getId(), AudioType.REFERENCE)
                .map(AudioFile::getAudioUrl)
                .orElse(null);

        return new SongDTO(
                song.getId(),
                song.getTitle(),
                song.getArtist(),
                song.getKey(),
                song.getBpm(),
                song.getYouTubeUrl(),
                song.getNotes(),
                referenceAudioUrl
        );
    }

    @Override
    @Transactional
    public void delete(UUID songId) {
        User currentUser = currentUserProvider.get();

        Song song = songRepository.getSongById(songId)
                .orElseThrow(() -> new ValidationException("Música não encontrada."));

        if (!currentUser.getId().equals(song.getUser().getId())) {
            throw new ValidationException("Você não tem permissão para excluir esta música.");
        }

        boolean usedInMedley = medleyItemRepository.existsBySong_Id(songId);
        boolean usedInEventSong = eventSongRepository.existsBySongId(songId);
        boolean usedInEventSetlist = eventSetlistItemRepository.existsBySong_Id(songId);

        if (usedInMedley || usedInEventSong || usedInEventSetlist) {
            throw new ValidationException(
                    "Não é possível excluir esta música: ela está sendo usada em um medley ou em um evento."
            );
        }

        audioFileRepository.deleteBySong_Id(songId);
        songRepository.delete(song);
    }

    @Override
    public SongLyricsDTO getLyrics(UUID songId) {
        Song song = songRepository.getSongById(songId)
                .orElseThrow(() -> new ValidationException("Música não encontrada."));

        return new SongLyricsDTO(song.getId(), song.getLyrics());
    }

    @Override
    public SongLyricsDTO updateLyrics(UUID songId, String lyrics) {
        User currentUser = currentUserProvider.get();

        Song song = songRepository.getSongById(songId)
                .orElseThrow(() -> new ValidationException("Música não encontrada."));

        if (!currentUser.getId().equals(song.getUser().getId())) {
            throw new ValidationException("Você não tem permissão para editar a letra desta música.");
        }

        song.setLyrics(lyrics);
        Song saved = songRepository.save(song);
        return new SongLyricsDTO(saved.getId(), saved.getLyrics());
    }

    @Override
    public ChordSheetDTO getChordSheet(UUID songId) {
        Song song = songRepository.getSongById(songId)
                .orElseThrow(() -> new ValidationException("Música não encontrada."));

        return new ChordSheetDTO(song.getId(), song.getChordSheetJson(), song.isEditChordSheetPermission());
    }

    @Override
    public ChordSheetDTO updateChordSheet(UUID songId, String chordSheetJson) {
        User currentUser = currentUserProvider.get();

        Song song = songRepository.getSongById(songId)
                .orElseThrow(() -> new ValidationException("Música não encontrada."));

        boolean isOwner = currentUser.getId().equals(song.getUser().getId());
        boolean canEdit = isOwner || Boolean.TRUE.equals(song.isEditChordSheetPermission());
        if (!canEdit) {
            throw new ValidationException("Você não tem permissão para editar a cifra desta música.");
        }

        chordSheetValidation.validate(chordSheetJson);

        song.setChordSheetJson(chordSheetJson);
        Song saved = songRepository.save(song);
        return new ChordSheetDTO(saved.getId(), saved.getChordSheetJson(), saved.isEditChordSheetPermission());
    }

    @Override
    public void deleteChordSheet(UUID songId) {
        User currentUser = currentUserProvider.get();

        Song song = songRepository.getSongById(songId)
                .orElseThrow(() -> new ValidationException("Música não encontrada."));

        if (!currentUser.getId().equals(song.getUser().getId())) {
            throw new ValidationException("Você não tem permissão para remover a cifra desta música.");
        }

        song.setChordSheetJson(null);
        songRepository.save(song);
    }

    @Override
    public ChordSheetDTO importChordSheet(UUID songId, String chordSheetJson) {
        return updateChordSheet(songId, chordSheetJson);
    }

    @Override
    public ChordSheetEditPermissionDTO updateChordSheetEditPermission(UUID songId, boolean editPermission) {
        User currentUser = currentUserProvider.get();

        Song song = songRepository.getSongById(songId)
                .orElseThrow(() -> new ValidationException("Música não encontrada."));

        if (!currentUser.getId().equals(song.getUser().getId())) {
            throw new ValidationException("Você não tem permissão para alterar a permissão de edição da cifra desta música.");
        }

        song.setEditChordSheetPermission(editPermission);
        Song saved = songRepository.save(song);
        return new ChordSheetEditPermissionDTO(saved.isEditChordSheetPermission());
    }
}
