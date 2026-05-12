package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.ScheduleItemType;
import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.EventScheduleItem;
import br.com.louvor4.api.models.EventSetlistItem;
import br.com.louvor4.api.repositories.EventRepository;
import br.com.louvor4.api.repositories.EventScheduleItemRepository;
import br.com.louvor4.api.services.ScheduleService;
import br.com.louvor4.api.shared.dto.Schedule.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    private static final int POSITION_GAP = 1000;

    private final EventScheduleItemRepository scheduleItemRepository;
    private final EventRepository eventRepository;

    public ScheduleServiceImpl(EventScheduleItemRepository scheduleItemRepository,
                               EventRepository eventRepository) {
        this.scheduleItemRepository = scheduleItemRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    public List<ScheduleItemResponse> getSchedule(UUID eventId) {
        eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Evento não encontrado."));

        return scheduleItemRepository.findByEventIdOrderByPositionAsc(eventId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addTextItem(UUID eventId, CreateTextScheduleItemRequest request) {
        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Evento não encontrado."));

        int nextPosition = nextPosition(eventId);

        var item = new EventScheduleItem();
        item.setEvent(event);
        item.setType(ScheduleItemType.TEXT);
        item.setPosition(nextPosition);
        item.setTitle(request.title());
        item.setDescription(request.description());

        scheduleItemRepository.save(item);
    }

    @Override
    @Transactional
    public void updateTextItem(UUID eventId, UUID itemId, UpdateTextScheduleItemRequest request) {
        var item = scheduleItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item da programação não encontrado."));

        if (!item.getEvent().getId().equals(eventId)) {
            throw new ValidationException("Item não pertence ao evento informado.");
        }
        if (!item.isText()) {
            throw new ValidationException("Apenas itens do tipo TEXT podem ser editados.");
        }

        item.setTitle(request.title());
        item.setDescription(request.description());
        scheduleItemRepository.save(item);
    }

    @Override
    @Transactional
    public void deleteTextItem(UUID eventId, UUID itemId) {
        var item = scheduleItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item da programação não encontrado."));

        if (!item.getEvent().getId().equals(eventId)) {
            throw new ValidationException("Item não pertence ao evento informado.");
        }
        if (!item.isText()) {
            throw new ValidationException("Apenas itens do tipo TEXT podem ser removidos diretamente.");
        }

        scheduleItemRepository.delete(item);
    }

    @Override
    @Transactional
    public void reorder(UUID eventId, ReorderScheduleRequest request) {
        List<EventScheduleItem> currentItems =
                scheduleItemRepository.findByEventIdOrderByPositionAsc(eventId);

        List<UUID> orderedIds = request.orderedIds();

        if (currentItems.size() != orderedIds.size()) {
            throw new ValidationException(
                    "A lista de IDs deve conter exatamente " + currentItems.size() + " itens.");
        }

        Map<UUID, EventScheduleItem> itemById = currentItems.stream()
                .collect(Collectors.toMap(EventScheduleItem::getId, i -> i));

        List<EventScheduleItem> toSave = new ArrayList<>(orderedIds.size());
        int position = POSITION_GAP;
        for (UUID id : orderedIds) {
            EventScheduleItem item = itemById.get(id);
            if (item == null) {
                throw new ValidationException("Item não encontrado na programação do evento: " + id);
            }
            item.setPosition(position);
            toSave.add(item);
            position += POSITION_GAP;
        }

        scheduleItemRepository.saveAll(toSave);
    }

    @Override
    @Transactional
    public void onSetlistItemAdded(EventSetlistItem setlistItem) {
        int nextPosition = nextPosition(setlistItem.getEvent().getId());

        var item = new EventScheduleItem();
        item.setEvent(setlistItem.getEvent());
        item.setType(ScheduleItemType.MUSIC);
        item.setPosition(nextPosition);
        item.setSetlistItem(setlistItem);

        scheduleItemRepository.save(item);
    }

    @Override
    @Transactional
    public void onSetlistItemRemoved(UUID setlistItemId) {
        scheduleItemRepository.deleteBySetlistItemId(setlistItemId);
    }

    private int nextPosition(UUID eventId) {
        int max = scheduleItemRepository.findMaxPositionByEventId(eventId);
        return max + POSITION_GAP;
    }

    private ScheduleItemResponse toResponse(EventScheduleItem item) {
        ScheduleMusicResponse music = null;
        if (item.isMusic() && item.getSetlistItem() != null) {
            var song = item.getSetlistItem().getSong();
            music = new ScheduleMusicResponse(
                    song != null ? song.getId() : null,
                    song != null ? song.getTitle() : null,
                    song != null ? song.getArtist() : null
            );
        }
        return new ScheduleItemResponse(
                item.getId(),
                item.getType(),
                item.getPosition(),
                item.isText() ? item.getTitle() : null,
                item.isText() ? item.getDescription() : null,
                music
        );
    }
}
