package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.ProgramItemType;
import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.*;
import br.com.louvor4.api.repositories.EventRepository;
import br.com.louvor4.api.repositories.EventProgramItemRepository;
import br.com.louvor4.api.shared.dto.Program.*;
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
class ProgramServiceImplTest {

    @Mock
    private EventProgramItemRepository programItemRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private ProgramServiceImpl programService;

    @Test
    void getProgramShouldReturnItemsOrderedByPosition() {
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

        EventProgramItem musicItem = new EventProgramItem();
        musicItem.setId(UUID.randomUUID());
        musicItem.setEvent(event);
        musicItem.setType(ProgramItemType.MUSIC);
        musicItem.setPosition(1000);
        musicItem.setSetlistItem(setlistItem);

        EventProgramItem textItem = new EventProgramItem();
        textItem.setId(UUID.randomUUID());
        textItem.setEvent(event);
        textItem.setType(ProgramItemType.TEXT);
        textItem.setPosition(2000);
        textItem.setTitle("Oração");
        textItem.setDescription("Momento de oração coletiva");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(programItemRepository.findByEventIdOrderByPositionAsc(eventId))
                .thenReturn(List.of(musicItem, textItem));

        List<ProgramItemResponse> result = programService.getProgram(eventId);

        assertEquals(2, result.size());

        ProgramItemResponse first = result.get(0);
        assertEquals(ProgramItemType.MUSIC, first.type());
        assertEquals(1000, first.position());
        assertNotNull(first.music());
        assertEquals("Superman", first.music().title());
        assertEquals("Fruto Sagrado", first.music().artist());
        assertNull(first.title());

        ProgramItemResponse second = result.get(1);
        assertEquals(ProgramItemType.TEXT, second.type());
        assertEquals(2000, second.position());
        assertEquals("Oração", second.title());
        assertEquals("Momento de oração coletiva", second.description());
        assertNull(second.music());
    }

    @Test
    void getProgramShouldThrowWhenEventNotFound() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> programService.getProgram(eventId));
    }

    @Test
    void addTextItemShouldPersistWithPositionAfterMax() {
        UUID eventId = UUID.randomUUID();
        Event event = new Event();
        event.setId(eventId);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(programItemRepository.findMaxPositionByEventId(eventId)).thenReturn(2000);

        programService.addTextItem(eventId, new CreateTextProgramItemRequest("Oração", "Descrição"));

        ArgumentCaptor<EventProgramItem> captor = ArgumentCaptor.forClass(EventProgramItem.class);
        verify(programItemRepository).save(captor.capture());

        EventProgramItem saved = captor.getValue();
        assertEquals(ProgramItemType.TEXT, saved.getType());
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
        when(programItemRepository.findMaxPositionByEventId(eventId)).thenReturn(0);

        programService.addTextItem(eventId, new CreateTextProgramItemRequest("Início", null));

        ArgumentCaptor<EventProgramItem> captor = ArgumentCaptor.forClass(EventProgramItem.class);
        verify(programItemRepository).save(captor.capture());
        assertEquals(1000, captor.getValue().getPosition());
    }

    @Test
    void updateTextItemShouldChangeTitleAndDescription() {
        UUID eventId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        Event event = new Event();
        event.setId(eventId);

        EventProgramItem item = new EventProgramItem();
        item.setId(itemId);
        item.setEvent(event);
        item.setType(ProgramItemType.TEXT);
        item.setTitle("Antigo");

        when(programItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        programService.updateTextItem(eventId, itemId, new UpdateTextProgramItemRequest("Novo", "Nova desc"));

        verify(programItemRepository).save(item);
        assertEquals("Novo", item.getTitle());
        assertEquals("Nova desc", item.getDescription());
    }

    @Test
    void updateTextItemShouldThrowWhenItemNotFound() {
        UUID eventId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        when(programItemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> programService.updateTextItem(eventId, itemId, new UpdateTextProgramItemRequest("X", null)));
    }

    @Test
    void updateTextItemShouldThrowWhenItemBelongsToDifferentEvent() {
        UUID eventId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        Event otherEvent = new Event();
        otherEvent.setId(UUID.randomUUID());

        EventProgramItem item = new EventProgramItem();
        item.setId(itemId);
        item.setEvent(otherEvent);
        item.setType(ProgramItemType.TEXT);

        when(programItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        assertThrows(ValidationException.class,
                () -> programService.updateTextItem(eventId, itemId, new UpdateTextProgramItemRequest("X", null)));
    }

    @Test
    void updateTextItemShouldThrowWhenItemIsNotText() {
        UUID eventId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        Event event = new Event();
        event.setId(eventId);

        EventProgramItem item = new EventProgramItem();
        item.setId(itemId);
        item.setEvent(event);
        item.setType(ProgramItemType.MUSIC);

        when(programItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        assertThrows(ValidationException.class,
                () -> programService.updateTextItem(eventId, itemId, new UpdateTextProgramItemRequest("X", null)));
    }

    @Test
    void deleteTextItemShouldRemoveTextItem() {
        UUID eventId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        Event event = new Event();
        event.setId(eventId);

        EventProgramItem item = new EventProgramItem();
        item.setId(itemId);
        item.setEvent(event);
        item.setType(ProgramItemType.TEXT);

        when(programItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        programService.deleteTextItem(eventId, itemId);

        verify(programItemRepository).delete(item);
    }

    @Test
    void deleteTextItemShouldThrowWhenItemIsMusic() {
        UUID eventId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        Event event = new Event();
        event.setId(eventId);

        EventProgramItem item = new EventProgramItem();
        item.setId(itemId);
        item.setEvent(event);
        item.setType(ProgramItemType.MUSIC);

        when(programItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        assertThrows(ValidationException.class, () -> programService.deleteTextItem(eventId, itemId));
    }

    @Test
    void reorderShouldReassignPositionsInMultiplesOf1000() {
        UUID eventId = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        Event event = new Event();
        event.setId(eventId);

        EventProgramItem item1 = new EventProgramItem();
        item1.setId(id1);
        item1.setEvent(event);
        item1.setPosition(3000);

        EventProgramItem item2 = new EventProgramItem();
        item2.setId(id2);
        item2.setEvent(event);
        item2.setPosition(1000);

        EventProgramItem item3 = new EventProgramItem();
        item3.setId(id3);
        item3.setEvent(event);
        item3.setPosition(2000);

        when(programItemRepository.findByEventIdOrderByPositionAsc(eventId))
                .thenReturn(List.of(item2, item3, item1));

        programService.reorder(eventId, new ReorderProgramRequest(List.of(id1, id2, id3)));

        verify(programItemRepository).saveAll(any());
        assertEquals(1000, item1.getPosition());
        assertEquals(2000, item2.getPosition());
        assertEquals(3000, item3.getPosition());
    }

    @Test
    void reorderShouldThrowWhenOrderedIdsCountDiffersFromProgramCount() {
        UUID eventId = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();

        Event event = new Event();
        event.setId(eventId);

        EventProgramItem item = new EventProgramItem();
        item.setId(id1);
        item.setEvent(event);
        item.setPosition(1000);

        EventProgramItem extra = new EventProgramItem();
        extra.setId(UUID.randomUUID());
        extra.setEvent(event);
        extra.setPosition(2000);

        when(programItemRepository.findByEventIdOrderByPositionAsc(eventId))
                .thenReturn(List.of(item, extra));

        assertThrows(ValidationException.class,
                () -> programService.reorder(eventId, new ReorderProgramRequest(List.of(id1))));
    }

    @Test
    void onSetlistItemAddedShouldCreateMusicItemAtEnd() {
        Event event = new Event();
        event.setId(UUID.randomUUID());

        EventSetlistItem setlistItem = new EventSetlistItem();
        setlistItem.setId(UUID.randomUUID());
        setlistItem.setEvent(event);

        when(programItemRepository.findMaxPositionByEventId(event.getId())).thenReturn(1000);

        programService.onSetlistItemAdded(setlistItem);

        ArgumentCaptor<EventProgramItem> captor = ArgumentCaptor.forClass(EventProgramItem.class);
        verify(programItemRepository).save(captor.capture());

        EventProgramItem saved = captor.getValue();
        assertEquals(ProgramItemType.MUSIC, saved.getType());
        assertEquals(2000, saved.getPosition());
        assertEquals(setlistItem, saved.getSetlistItem());
        assertEquals(event, saved.getEvent());
    }

    @Test
    void onSetlistItemRemovedShouldDeleteCorrespondingProgramItem() {
        UUID setlistItemId = UUID.randomUUID();

        programService.onSetlistItemRemoved(setlistItemId);

        verify(programItemRepository).deleteBySetlistItemId(setlistItemId);
    }
}
