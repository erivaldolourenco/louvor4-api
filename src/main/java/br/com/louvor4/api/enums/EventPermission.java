package br.com.louvor4.api.enums;

public enum EventPermission {
    ADD_SONG,              // pode adicionar música ao repertório
    EDIT_SETLIST,          // pode mudar ordem / tom / bpm
    REMOVE_SONG,           // pode remover música do repertório
    MANAGE_PARTICIPANTS,   // pode adicionar/remover participantes
    EDIT_EVENT,            // pode editar dados do evento
    EDIT_CHORD_SHEET       // pode editar cifra de músicas do evento (se o dono liberou edição colaborativa)
}
