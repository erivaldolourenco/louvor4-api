package br.com.louvor4.api.services;

import br.com.louvor4.api.models.User;

public interface AccountDeletionService {
    void deleteAccount(User user);
    void requestDeletion(String email);
    void confirmDeletion(String token);
}
