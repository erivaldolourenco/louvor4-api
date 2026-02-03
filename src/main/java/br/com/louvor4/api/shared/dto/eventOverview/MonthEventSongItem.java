package br.com.louvor4.api.shared.dto.eventOverview;

import java.util.UUID;

public record MonthEventSongItem(UUID songId,
                                 String title,
                                 String artist,
                                 String key,
                                 String addedBy) {
}
