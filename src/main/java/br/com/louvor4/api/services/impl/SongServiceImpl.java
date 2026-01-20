package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.mapper.SongMapper;
import br.com.louvor4.api.models.Song;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.repositories.SongRepository;
import br.com.louvor4.api.repositories.UserRepository;
import br.com.louvor4.api.services.SongService;
import br.com.louvor4.api.shared.dto.Song.SongDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SongServiceImpl implements SongService {

    private final SongRepository songRepository;
    private final SongMapper songMapper;
    private final CurrentUserProvider currentUserProvider;

    public SongServiceImpl(SongRepository songRepository, SongMapper songMapper, CurrentUserProvider currentUserProvider) {
        this.songRepository = songRepository;
        this.songMapper = songMapper;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public SongDTO create(SongDTO createDto) {
        User creator = currentUserProvider.get();
        Song song = songMapper.toEntity(createDto);
        song.setUser(creator);
        Song saved = songRepository.save(song);
        return songMapper.toDto(saved);
    }

    @Override
    public List<SongDTO> getSongsFromUser() {
        User user = currentUserProvider.get();
        List<Song> songs = songRepository.getSongByUser_Id(user.getId());
        return songMapper.toDtoList(songs);
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

        Song saved = songRepository.save(song);
        return songMapper.toDto(saved);
    }
}
