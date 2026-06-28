package br.com.louvor4.entitlement.services;

import java.util.UUID;

public interface UserIdProvider {

    UUID getCurrentUserId();
}
