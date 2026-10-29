package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.models.Event;
import br.com.louvor4.api.models.MusicProject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EventNotificationDataTest {

    @Test
    void dataJson_includesProjectImageWhenPresent() {
        Event event = eventWithImage("https://bucket.s3.amazonaws.com/project.png");

        assertEquals(
                "{\"eventId\":\"" + event.getId() + "\",\"projectImageUrl\":\"https://bucket.s3.amazonaws.com/project.png\"}",
                EventNotificationData.dataJson(event));
    }

    @Test
    void dataJson_onlyEventIdWhenProjectHasNoImage() {
        Event event = eventWithImage("  ");

        assertNull(EventNotificationData.projectImageUrl(event));
        assertEquals("{\"eventId\":\"" + event.getId() + "\"}", EventNotificationData.dataJson(event));
    }

    private Event eventWithImage(String image) {
        MusicProject project = new MusicProject();
        project.setProfileImage(image);
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setMusicProject(project);
        return event;
    }
}
