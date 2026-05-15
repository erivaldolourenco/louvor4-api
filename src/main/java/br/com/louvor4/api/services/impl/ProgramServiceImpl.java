package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.ProgramItemType;
import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.EventProgramItem;
import br.com.louvor4.api.models.EventSetlistItem;
import br.com.louvor4.api.repositories.EventRepository;
import br.com.louvor4.api.repositories.EventProgramItemRepository;
import br.com.louvor4.api.services.ProgramService;
import br.com.louvor4.api.shared.dto.Program.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProgramServiceImpl implements ProgramService {

    private static final int POSITION_GAP = 1000;

    private final EventProgramItemRepository programItemRepository;
    private final EventRepository eventRepository;

    public ProgramServiceImpl(EventProgramItemRepository programItemRepository,
                              EventRepository eventRepository) {
        this.programItemRepository = programItemRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    public List<ProgramItemResponse> getProgram(UUID eventId) {
        eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Evento não encontrado."));

        return programItemRepository.findByEventIdOrderByPositionAsc(eventId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addTextItem(UUID eventId, CreateTextProgramItemRequest request) {
        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Evento não encontrado."));

        int nextPosition = nextPosition(eventId);

        var item = new EventProgramItem();
        item.setEvent(event);
        item.setType(ProgramItemType.TEXT);
        item.setPosition(nextPosition);
        item.setTitle(request.title());
        item.setDescription(request.description());

        programItemRepository.save(item);
    }

    @Override
    @Transactional
    public void updateTextItem(UUID eventId, UUID itemId, UpdateTextProgramItemRequest request) {
        var item = programItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item da programação não encontrado."));

        if (!item.getEvent().getId().equals(eventId)) {
            throw new ValidationException("Item não pertence ao evento informado.");
        }
        if (!item.isText()) {
            throw new ValidationException("Apenas itens do tipo TEXT podem ser editados.");
        }

        item.setTitle(request.title());
        item.setDescription(request.description());
        programItemRepository.save(item);
    }

    @Override
    @Transactional
    public void deleteTextItem(UUID eventId, UUID itemId) {
        var item = programItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item da programação não encontrado."));

        if (!item.getEvent().getId().equals(eventId)) {
            throw new ValidationException("Item não pertence ao evento informado.");
        }
        if (!item.isText()) {
            throw new ValidationException("Apenas itens do tipo TEXT podem ser removidos diretamente.");
        }

        programItemRepository.delete(item);
    }

    @Override
    @Transactional
    public void reorder(UUID eventId, ReorderProgramRequest request) {
        List<EventProgramItem> currentItems =
                programItemRepository.findByEventIdOrderByPositionAsc(eventId);

        List<UUID> orderedIds = request.orderedIds();

        if (currentItems.size() != orderedIds.size()) {
            throw new ValidationException(
                    "A lista de IDs deve conter exatamente " + currentItems.size() + " itens.");
        }

        Map<UUID, EventProgramItem> itemById = currentItems.stream()
                .collect(Collectors.toMap(EventProgramItem::getId, i -> i));

        List<EventProgramItem> toSave = new ArrayList<>(orderedIds.size());
        int position = POSITION_GAP;
        for (UUID id : orderedIds) {
            EventProgramItem item = itemById.get(id);
            if (item == null) {
                throw new ValidationException("Item não encontrado na programação do evento: " + id);
            }
            item.setPosition(position);
            toSave.add(item);
            position += POSITION_GAP;
        }

        programItemRepository.saveAll(toSave);
    }

    @Override
    @Transactional
    public void onSetlistItemAdded(EventSetlistItem setlistItem) {
        int nextPosition = nextPosition(setlistItem.getEvent().getId());

        var item = new EventProgramItem();
        item.setEvent(setlistItem.getEvent());
        item.setType(ProgramItemType.MUSIC);
        item.setPosition(nextPosition);
        item.setSetlistItem(setlistItem);

        programItemRepository.save(item);
    }

    @Override
    @Transactional
    public void onSetlistItemRemoved(UUID setlistItemId) {
        programItemRepository.deleteBySetlistItemId(setlistItemId);
    }

    private int nextPosition(UUID eventId) {
        int max = programItemRepository.findMaxPositionByEventId(eventId);
        return max + POSITION_GAP;
    }

    private ProgramItemResponse toResponse(EventProgramItem item) {
        ProgramMusicResponse music = null;
        if (item.isMusic() && item.getSetlistItem() != null) {
            var song = item.getSetlistItem().getSong();
            music = new ProgramMusicResponse(
                    song != null ? song.getId() : null,
                    song != null ? song.getTitle() : null,
                    song != null ? song.getArtist() : null
            );
        }
        return new ProgramItemResponse(
                item.getId(),
                item.getType(),
                item.getPosition(),
                item.isText() ? item.getTitle() : null,
                item.isText() ? item.getDescription() : null,
                music
        );
    }
}
