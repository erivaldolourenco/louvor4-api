package br.com.louvor4.api.services;

import br.com.louvor4.api.models.User;

public interface GoogleAuthService {
    User authenticate(String idToken);
}
