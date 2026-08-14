package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.enums.AudioType;
import br.com.louvor4.api.exceptions.ForbiddenException;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.AudioFile;
import br.com.louvor4.api.models.Medley;
import br.com.louvor4.api.models.MedleyItem;
import br.com.louvor4.api.models.Song;
import br.com.louvor4.api.models.SongCategory;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.repositories.AudioFileRepository;
import br.com.louvor4.api.repositories.MedleyRepository;
import br.com.louvor4.api.repositories.SongCategoryRepository;
import br.com.louvor4.api.repositories.SongRepository;
import br.com.louvor4.api.services.MedleyService;
import br.com.louvor4.api.shared.dto.Medley.CreateMedleyItemRequest;
import br.com.louvor4.api.shared.dto.Medley.CreateMedleyRequest;
import br.com.louvor4.api.shared.dto.Medley.MedleyItemResponse;
import br.com.louvor4.api.shared.dto.Medley.MedleyResponse;
import br.com.louvor4.api.shared.dto.Medley.UpdateMedleyRequest;
import br.com.louvor4.api.shared.dto.Song.SongCategoryDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MedleyServiceImpl implements MedleyService {

    private final MedleyRepository medleyRepository;
    private final SongRepository songRepository;
    private final SongCategoryRepository songCategoryRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AudioFileRepository audioFileRepository;

    public MedleyServiceImpl(MedleyRepository medleyRepository,
                             SongRepository songRepository,
                             SongCategoryRepository songCategoryRepository,
                             CurrentUserProvider currentUserProvider,
                             AudioFileRepository audioFileRepository) {
        this.medleyRepository = medleyRepository;
        this.songRepository = songRepository;
        this.songCategoryRepository = songCategoryRepository;
        this.currentUserProvider = currentUserProvider;
        this.audioFileRepository = audioFileRepository;
    }

    @Override
    @Transactional
    public MedleyResponse create(CreateMedleyRequest request) {
        if (request == null) {
            throw new ValidationException("Dados do medley são obrigatórios.");
        }

        User owner = currentUserProvider.get();
        validateRequestItems(request.items());

        List<UUID> songIds = request.items().stream()
                .map(CreateMedleyItemRequest::songId)
                .toList();

        Map<UUID, Song> songsById = songRepository.findAllById(songIds)
                .stream()
                .collect(Collectors.toMap(Song::getId, Function.identity()));

        if (songsById.size() != new HashSet<>(songIds).size()) {
            throw new ValidationException("Uma ou mais músicas informadas não foram encontradas.");
        }

        List<MedleyItem> medleyItems = new ArrayList<>();
        for (CreateMedleyItemRequest itemRequest : request.items()) {
            Song song = songsById.get(itemRequest.songId());
            if (song == null || song.getUser() == null || !owner.getId().equals(song.getUser().getId())) {
                throw new ForbiddenException("Você só pode usar músicas cadastradas por você.");
            }

            MedleyItem medleyItem = new MedleyItem();
            medleyItem.setSong(song);
            medleyItem.setSequence(itemRequest.sequence());
            medleyItem.setKey(itemRequest.key());
            medleyItem.setNotes(itemRequest.notes());
            medleyItems.add(medleyItem);
        }

        Medley medley = new Medley();
        medley.setUser(owner);
        medley.setName(request.name());
        medley.setDescription(request.description());
        medley.setItems(medleyItems);

        for (MedleyItem medleyItem : medleyItems) {
            medleyItem.setMedley(medley);
        }

        Medley saved = medleyRepository.save(medley);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public MedleyResponse update(UUID medleyId, UpdateMedleyRequest request) {
        User owner = currentUserProvider.get();

        Medley medley = medleyRepository.findMedleyById(medleyId)
                .orElseThrow(() -> new ValidationException("Medley não encontrado."));

        validateRequestItems(request.items());

        List<UUID> songIds = request.items().stream()
                .map(CreateMedleyItemRequest::songId)
                .toList();

        Map<UUID, Song> songsById = songRepository.findAllById(songIds)
                .stream()
                .collect(Collectors.toMap(Song::getId, Function.identity()));

        if (songsById.size() != new HashSet<>(songIds).size()) {
            throw new ValidationException("Uma ou mais músicas informadas não foram encontradas.");
        }

        List<MedleyItem> updatedItems = new ArrayList<>();
        for (CreateMedleyItemRequest itemRequest : request.items()) {
            Song song = songsById.get(itemRequest.songId());
            if (song == null || song.getUser() == null || !owner.getId().equals(song.getUser().getId())) {
                throw new ForbiddenException("Você só pode usar músicas cadastradas por você.");
            }

            MedleyItem medleyItem = new MedleyItem();
            medleyItem.setSong(song);
            medleyItem.setSequence(itemRequest.sequence());
            medleyItem.setKey(itemRequest.key());
            medleyItem.setNotes(itemRequest.notes());
            medleyItem.setMedley(medley);
            updatedItems.add(medleyItem);
        }

        medley.setName(request.name());
        medley.setDescription(request.description());
        medley.getItems().clear();
        medley.getItems().addAll(updatedItems);

        Medley saved = medleyRepository.save(medley);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public MedleyResponse assignCategories(UUID medleyId, Set<UUID> categoryIds) {
        Medley medley = medleyRepository.findMedleyById(medleyId)
                .orElseThrow(() -> new ValidationException("Medley não encontrado."));

        User owner = currentUserProvider.get();
        Set<UUID> ids = categoryIds == null ? Set.of() : categoryIds;
        Set<SongCategory> categories = new HashSet<>(songCategoryRepository.findAllById(ids));

        boolean allBelongToOwner = categories.stream()
                .allMatch(c -> c.getUser().getId().equals(owner.getId()));
        if (categories.size() != ids.size() || !allBelongToOwner) {
            throw new ValidationException("Uma ou mais categorias informadas não existem ou não pertencem a você.");
        }

        medley.setCategories(categories);
        Medley saved = medleyRepository.save(medley);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID medleyId) {
        Medley medley = medleyRepository.findMedleyById(medleyId)
                .orElseThrow(() -> new ValidationException("Medley não encontrado."));

        medleyRepository.delete(medley);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedleyResponse> listFromCurrentUser() {
        User owner = currentUserProvider.get();
        List<Medley> medleys = medleyRepository.findByUser_Id(owner.getId());

        List<UUID> medleyIds = medleys.stream().map(Medley::getId).toList();
        Map<UUID, String> audioUrlByMedleyId = audioFileRepository
                .findByMedley_IdInAndType(medleyIds, AudioType.REFERENCE)
                .stream()
                .collect(Collectors.toMap(af -> af.getMedley().getId(), AudioFile::getAudioUrl));

        return medleys.stream()
                .map(m -> toResponse(m, audioUrlByMedleyId.get(m.getId())))
                .toList();
    }

    private void validateRequestItems(List<CreateMedleyItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new ValidationException("Medley deve conter ao menos um item.");
        }

        Set<Integer> sequenceSet = new HashSet<>();
        for (CreateMedleyItemRequest item : items) {
            if (!sequenceSet.add(item.sequence())) {
                throw new ValidationException("Sequência dos itens do medley não pode repetir.");
            }
        }
    }

    private MedleyResponse toResponse(Medley medley) {
        return toResponse(medley, null);
    }

    private MedleyResponse toResponse(Medley medley, String referenceAudioUrl) {
        List<MedleyItemResponse> itemResponses = medley.getItems()
                .stream()
                .sorted(Comparator.comparing(MedleyItem::getSequence))
                .map(item -> new MedleyItemResponse(
                        item.getId(),
                        item.getSong().getId(),
                        item.getSong().getTitle(),
                        item.getSong().getArtist(),
                        item.getSong().getYouTubeUrl(),
                        item.getKey(),
                        item.getNotes(),
                        item.getSequence()
                ))
                .toList();

        Set<SongCategoryDTO> categoryDtos = medley.getCategories().stream()
                .map(SongCategoryDTO::fromEntity)
                .collect(Collectors.toSet());

        return new MedleyResponse(
                medley.getId(),
                medley.getName(),
                medley.getDescription(),
                medley.getNotes(),
                medley.getCreatedAt(),
                medley.getUpdatedAt(),
                itemResponses,
                referenceAudioUrl,
                categoryDtos
        );
    }
}
