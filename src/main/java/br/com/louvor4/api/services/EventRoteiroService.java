package br.com.louvor4.api.services;

import java.util.UUID;

public interface EventRoteiroService {

    Result generatePdf(UUID eventId);

    record Result(byte[] pdf, String eventTitle) {}
}
