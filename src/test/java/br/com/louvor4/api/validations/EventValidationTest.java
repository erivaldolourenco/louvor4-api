package br.com.louvor4.api.validations;

import br.com.louvor4.api.enums.MusicProjectType;
import br.com.louvor4.api.exceptions.ForbiddenException;
import br.com.louvor4.api.models.Event;
import br.com.louvor4.api.models.MusicProject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class EventValidationTest {

    private final EventValidation eventValidation = new EventValidation();

    private Event eventForProjectType(MusicProjectType type) {
        MusicProject project = new MusicProject();
        project.setType(type);
        Event event = new Event();
        event.setMusicProject(project);
        return event;
    }

    @Test
    void requireRepertoireModule_throwsForbiddenWhenProjectIsMedia() {
        Event event = eventForProjectType(MusicProjectType.MEDIA);

        assertThatThrownBy(() -> eventValidation.requireRepertoireModule(event))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void requireRepertoireModule_allowsNonMediaProjects() {
        Event ministryEvent = eventForProjectType(MusicProjectType.MINISTRY);
        Event bandEvent = eventForProjectType(MusicProjectType.BAND);
        Event singerEvent = eventForProjectType(MusicProjectType.SINGER);

        assertDoesNotThrow(() -> eventValidation.requireRepertoireModule(ministryEvent));
        assertDoesNotThrow(() -> eventValidation.requireRepertoireModule(bandEvent));
        assertDoesNotThrow(() -> eventValidation.requireRepertoireModule(singerEvent));
    }
}
