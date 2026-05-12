package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.ScheduleItemType;
import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.*;
import br.com.louvor4.api.repositories.EventRepository;
import br.com.louvor4.api.repositories.EventScheduleItemRepository;
import br.com.louvor4.api.shared.dto.Schedule.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    @Mock
    private EventScheduleItemRepository scheduleItemRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    @Test
    void getScheduleShouldReturnItemsOrderedByPosition() {
        UUID eventId = UUID.randomUUID();
        Event event = new Event();
        event.setId(eventId);

        Song song = new Song();
        song.setId(UUID.randomUUID());
        song.setTitle("Superman");
        song.setArtist("Fruto Sagrado");

        EventSetlistItem setlistItem = new EventSetlistItem();
        setlistItem.setId(UUID.randomUUID());
        setlistItem.setSong(song);

        EventScheduleItem musicItem = new EventScheduleItem();
        musicItem.setId(UUID.randomUUID());
        musicItem.setEvent(event);
        musicItem.setType(ScheduleItemType.MUSIC);
        musicItem.setPosition(1000);
        musicItem.setSetlistItem(setlistItem);

        EventScheduleItem textItem = new EventScheduleItem();
        textItem.setId(UUID.randomUUID());
        textItem.setEvent(event);
        textItem.setType(ScheduleItemType.TEXT);
        textItem.setPosition(2000);
        textItem.setTitle("Oração");
        textItem.setDescription("Momento de oração coletiva");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(scheduleItemRepository.findByEventIdOrderByPositionAsc(eventId))
                .thenReturn(List.of(musicItem, textItem));

        List<ScheduleItemResponse> result = scheduleService.getSchedule(eventId);

        assertEquals(2, result.size());

        ScheduleItemResponse first = result.get(0);
        assertEquals(ScheduleItemType.MUSIC, first.type());
        assertEquals(1000, first.position());
        assertNotNull(first.music());
        assertEquals("Superman", first.music().title());
        assertEquals("Fruto Sagrado", first.music().artist());
        assertNull(first.title());

        ScheduleItemResponse second = result.get(1);
        assertEquals(ScheduleItemType.TEXT, second.type());
        assertEquals(2000, second.position());
        assertEquals("Oração", second.title());
        assertEquals("Momento de oração coletiva", second.description());
        assertNull(second.music());
    }

    @Test
    void getScheduleShouldThrowWhenEventNotFound() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> scheduleService.getSchedule(eventId));
    }

    @Test
    void addTextItemShouldPersistWithPositionAfterMax() {
        UUID eventId = UUID.randomUUID();
        Event event = new Event();
        event.setId(eventId);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(scheduleItemRepository.findMaxPositionByEventId(eventId)).thenReturn(2000);

        scheduleService.addTextItem(eventId, new CreateTextScheduleItemRequest("Oração", "Descrição"));

        ArgumentCaptor<EventScheduleItem> captor = ArgumentCaptor.forClass(EventScheduleItem.class);
        verify(scheduleItemRepository).save(captor.capture());

        EventScheduleItem saved = captor.getValue();
        assertEquals(ScheduleItemType.TEXT, saved.getType());
        assertEquals(3000, saved.getPosition());
        assertEquals("Oração", saved.getTitle());
        assertEquals("Descrição", saved.getDescription());
    }

    @Test
    void addTextItemShouldUse1000AsFirstPositionWhenNoItems() {
        UUID eventId = UUID.randomUUID();
        Event event = new Event();
        event.setId(eventId);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(scheduleItemRepository.findMaxPositionByEventId(eventId)).thenReturn(0);

        scheduleService.addTextItem(eventId, new CreateTextScheduleItemRequest("Início", null));

        ArgumentCaptor<EventScheduleItem> captor = ArgumentCaptor.forClass(EventScheduleItem.class);
        verify(scheduleItemRepository).save(captor.capture());
        assertEquals(1000, captor.getValue().getPosition());
    }

    @Test
    void updateTextItemShouldChangeTitleAndDescription() {
        UUID eventId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        Event event = new Event();
        event.setId(eventId);

        EventScheduleItem item = new EventScheduleItem();
        item.setId(itemId);
        item.setEvent(event);
        item.setType(ScheduleItemType.TEXT);
        item.setTitle("Antigo");

        when(scheduleItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        scheduleService.updateTextItem(eventId, itemId, new UpdateTextScheduleItemRequest("Novo", "Nova desc"));

        verify(scheduleItemRepository).save(item);
        assertEquals("Novo", item.getTitle());
        assertEquals("Nova desc", item.getDescription());
    }

    @Test
    void updateTextItemShouldThrowWhenItemNotFound() {
        UUID eventId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        when(scheduleItemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> scheduleService.updateTextItem(eventId, itemId, new UpdateTextScheduleItemRequest("X", null)));
    }

    @Test
    void updateTextItemShouldThrowWhenItemBelongsToDifferentEvent() {
        UUID eventId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        Event otherEvent = new Event();
        otherEvent.setId(UUID.randomUUID());

        EventScheduleItem item = new EventScheduleItem();
        item.setId(itemId);
        item.setEvent(otherEvent);
        item.setType(ScheduleItemType.TEXT);

        when(scheduleItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        assertThrows(ValidationException.class,
                () -> scheduleService.updateTextItem(eventId, itemId, new UpdateTextScheduleItemRequest("X", null)));
    }

    @Test
    void updateTextItemShouldThrowWhenItemIsNotText() {
        UUID eventId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        Event event = new Event();
        event.setId(eventId);

        EventScheduleItem item = new EventScheduleItem();
        item.setId(itemId);
        item.setEvent(event);
        item.setType(ScheduleItemType.MUSIC);

        when(scheduleItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        assertThrows(ValidationException.class,
                () -> scheduleService.updateTextItem(eventId, itemId, new UpdateTextScheduleItemRequest("X", null)));
    }

    @Test
    void deleteTextItemShouldRemoveTextItem() {
        UUID eventId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        Event event = new Event();
        event.setId(eventId);

        EventScheduleItem item = new EventScheduleItem();
        item.setId(itemId);
        item.setEvent(event);
        item.setType(ScheduleItemType.TEXT);

        when(scheduleItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        scheduleService.deleteTextItem(eventId, itemId);

        verify(scheduleItemRepository).delete(item);
    }

    @Test
    void deleteTextItemShouldThrowWhenItemIsMusic() {
        UUID eventId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        Event event = new Event();
        event.setId(eventId);

        EventScheduleItem item = new EventScheduleItem();
        item.setId(itemId);
        item.setEvent(event);
        item.setType(ScheduleItemType.MUSIC);

        when(scheduleItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        assertThrows(ValidationException.class, () -> scheduleService.deleteTextItem(eventId, itemId));
    }

    @Test
    void reorderShouldReassignPositionsInMultiplesOf1000() {
        UUID eventId = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        Event event = new Event();
        event.setId(eventId);

        EventScheduleItem item1 = new EventScheduleItem();
        item1.setId(id1);
        item1.setEvent(event);
        item1.setPosition(3000);

        EventScheduleItem item2 = new EventScheduleItem();
        item2.setId(id2);
        item2.setEvent(event);
        item2.setPosition(1000);

        EventScheduleItem item3 = new EventScheduleItem();
        item3.setId(id3);
        item3.setEvent(event);
        item3.setPosition(2000);

        when(scheduleItemRepository.findByEventIdOrderByPositionAsc(eventId))
                .thenReturn(List.of(item2, item3, item1));

        scheduleService.reorder(eventId, new ReorderScheduleRequest(List.of(id1, id2, id3)));

        verify(scheduleItemRepository).saveAll(any());
        assertEquals(1000, item1.getPosition());
        assertEquals(2000, item2.getPosition());
        assertEquals(3000, item3.getPosition());
    }

    @Test
    void reorderShouldThrowWhenOrderedIdsCountDiffersFromScheduleCount() {
        UUID eventId = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();

        Event event = new Event();
        event.setId(eventId);

        EventScheduleItem item = new EventScheduleItem();
        item.setId(id1);
        item.setEvent(event);
        item.setPosition(1000);

        EventScheduleItem extra = new EventScheduleItem();
        extra.setId(UUID.randomUUID());
        extra.setEvent(event);
        extra.setPosition(2000);

        when(scheduleItemRepository.findByEventIdOrderByPositionAsc(eventId))
                .thenReturn(List.of(item, extra));

        assertThrows(ValidationException.class,
                () -> scheduleService.reorder(eventId, new ReorderScheduleRequest(List.of(id1))));
    }

    @Test
    void onSetlistItemAddedShouldCreateMusicItemAtEnd() {
        Event event = new Event();
        event.setId(UUID.randomUUID());

        EventSetlistItem setlistItem = new EventSetlistItem();
        setlistItem.setId(UUID.randomUUID());
        setlistItem.setEvent(event);

        when(scheduleItemRepository.findMaxPositionByEventId(event.getId())).thenReturn(1000);

        scheduleService.onSetlistItemAdded(setlistItem);

        ArgumentCaptor<EventScheduleItem> captor = ArgumentCaptor.forClass(EventScheduleItem.class);
        verify(scheduleItemRepository).save(captor.capture());

        EventScheduleItem saved = captor.getValue();
        assertEquals(ScheduleItemType.MUSIC, saved.getType());
        assertEquals(2000, saved.getPosition());
        assertEquals(setlistItem, saved.getSetlistItem());
        assertEquals(event, saved.getEvent());
    }

    @Test
    void onSetlistItemRemovedShouldDeleteCorrespondingScheduleItem() {
        UUID setlistItemId = UUID.randomUUID();

        scheduleService.onSetlistItemRemoved(setlistItemId);

        verify(scheduleItemRepository).deleteBySetlistItemId(setlistItemId);
    }
}
