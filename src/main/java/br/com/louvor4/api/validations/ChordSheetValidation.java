package br.com.louvor4.api.validations;

import br.com.louvor4.api.exceptions.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

@Component
public class ChordSheetValidation {

    private static final Set<Integer> SUPPORTED_SCHEMA_VERSIONS = Set.of(1);
    private static final Pattern CHORD_PATTERN = Pattern.compile("^[A-G](#|b)?[^/\\s]*(/[A-G](#|b)?)?$");
    private static final String CHORD_SEQUENCE_TYPE = "chord_sequence";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void validate(String chordSheetJson) {
        if (chordSheetJson == null || chordSheetJson.isBlank()) {
            throw new ValidationException("JSON de cifra não pode ser vazio.");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(chordSheetJson);
        } catch (JsonProcessingException e) {
            throw new ValidationException("JSON de cifra mal formado.");
        }

        if (!root.hasNonNull("schemaVersion") || !root.get("schemaVersion").isInt()) {
            throw new ValidationException("schemaVersion é obrigatório e deve ser um número inteiro.");
        }

        int schemaVersion = root.get("schemaVersion").asInt();
        if (!SUPPORTED_SCHEMA_VERSIONS.contains(schemaVersion)) {
            throw new ValidationException("schemaVersion não reconhecida: " + schemaVersion);
        }

        JsonNode sections = root.path("sections");
        if (!sections.isArray()) {
            throw new ValidationException("sections deve ser uma lista.");
        }

        for (JsonNode section : sections) {
            boolean isChordSequence = CHORD_SEQUENCE_TYPE.equals(section.path("type").asText(""));

            JsonNode lines = section.path("lines");
            if (!lines.isArray()) {
                throw new ValidationException("lines deve ser uma lista dentro de cada section.");
            }

            for (JsonNode line : lines) {
                String text = line.path("text").asText("");
                JsonNode chords = line.path("chords");
                if (!chords.isArray()) {
                    continue;
                }

                Set<Integer> chordSequencePositions = isChordSequence ? new TreeSet<>() : null;

                for (JsonNode chordNode : chords) {
                    if (!chordNode.hasNonNull("position") || !chordNode.get("position").isInt()) {
                        throw new ValidationException("Posição de acorde é obrigatória e deve ser um número inteiro.");
                    }

                    int position = chordNode.get("position").asInt();

                    if (isChordSequence) {
                        if (position < 0) {
                            throw new ValidationException("Posição de acorde inválida: " + position + ".");
                        }
                        if (!chordSequencePositions.add(position)) {
                            throw new ValidationException("Posição de acorde duplicada em chord_sequence: " + position + ".");
                        }
                    } else if (position < 0 || position > text.length()) {
                        throw new ValidationException(
                                "Posição de acorde inválida: " + position + " para linha com " + text.length() + " caracteres.");
                    }

                    String chord = chordNode.path("chord").asText("");
                    if (!CHORD_PATTERN.matcher(chord).matches()) {
                        throw new ValidationException("Acorde inválido: " + chord);
                    }
                }

                if (isChordSequence && !isContiguousFromZero(chordSequencePositions)) {
                    throw new ValidationException(
                            "Posições de chord_sequence devem formar uma sequência contínua começando em 0: " + chordSequencePositions);
                }
            }
        }
    }

    private boolean isContiguousFromZero(Set<Integer> positions) {
        int expected = 0;
        for (int position : positions) {
            if (position != expected) {
                return false;
            }
            expected++;
        }
        return true;
    }
}
