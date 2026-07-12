package br.com.louvor4.api.validations;

import br.com.louvor4.api.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChordSheetValidationTest {

    private final ChordSheetValidation validation = new ChordSheetValidation();

    @Test
    void shouldAcceptRegularSectionWithChordWithinTextLength() {
        String json = """
                {"schemaVersion":1,"sections":[{"type":"verse","label":"Verso 1","lines":[
                {"text":"Deus e bom","chords":[{"position":0,"chord":"G"},{"position":5,"chord":"Am7"}]}
                ]}]}
                """;

        assertThatCode(() -> validation.validate(json)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectRegularSectionWhenChordPositionExceedsTextLength() {
        String json = """
                {"schemaVersion":1,"sections":[{"type":"verse","label":"Verso 1","lines":[
                {"text":"Deus","chords":[{"position":10,"chord":"G"}]}
                ]}]}
                """;

        assertThatThrownBy(() -> validation.validate(json))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Posição de acorde inválida");
    }

    @Test
    void shouldAcceptChordSequenceSectionWithSequentialPositionsOverEmptyText() {
        String json = """
                {"schemaVersion":1,"sections":[{"type":"chord_sequence","label":"","lines":[
                {"text":"","chords":[
                    {"position":0,"chord":"G"},
                    {"position":1,"chord":"D"},
                    {"position":2,"chord":"Em"},
                    {"position":3,"chord":"C"}
                ],"repeat":null}
                ]}]}
                """;

        assertThatCode(() -> validation.validate(json)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectChordSequenceWhenPositionsSkipAValue() {
        String json = """
                {"schemaVersion":1,"sections":[{"type":"chord_sequence","label":"","lines":[
                {"text":"","chords":[{"position":0,"chord":"G"},{"position":2,"chord":"D"}]}
                ]}]}
                """;

        assertThatThrownBy(() -> validation.validate(json))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("sequência contínua");
    }

    @Test
    void shouldRejectChordSequenceWithDuplicatedPosition() {
        String json = """
                {"schemaVersion":1,"sections":[{"type":"chord_sequence","label":"","lines":[
                {"text":"","chords":[{"position":0,"chord":"G"},{"position":0,"chord":"D"}]}
                ]}]}
                """;

        assertThatThrownBy(() -> validation.validate(json))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("duplicada");
    }

    @Test
    void shouldRejectChordSequenceWithInvalidChordName() {
        String json = """
                {"schemaVersion":1,"sections":[{"type":"chord_sequence","label":"","lines":[
                {"text":"","chords":[{"position":0,"chord":"H7"}]}
                ]}]}
                """;

        assertThatThrownBy(() -> validation.validate(json))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Acorde inválido");
    }
}
