package com.fooddelivery.user.service;

import com.fooddelivery.user.entity.User;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService {

    private final Keycloak keycloak;
    private final com.fooddelivery.user.config.KeycloakConfig keycloakConfig;

    public String createKeycloakUser(User user, String rawPassword) {
        try {
            UsersResource usersResource = keycloak.realm(keycloakConfig.getRealm()).users();

            UserRepresentation kcUser = new UserRepresentation();
            kcUser.setUsername(user.getEmail());
            kcUser.setEmail(user.getEmail());
            kcUser.setFirstName(user.getFullName());
            kcUser.setEnabled(true);
            kcUser.setEmailVerified(true);

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(rawPassword);
            credential.setTemporary(false);
            kcUser.setCredentials(Collections.singletonList(credential));

            Response response = usersResource.create(kcUser);

            if (response.getStatus() == 201) {
                String locationHeader = response.getHeaderString("Location");
                String keycloakUserId = locationHeader.substring(locationHeader.lastIndexOf('/') + 1);

                assignRole(user, keycloakUserId);
                return keycloakUserId;
            } else {
                log.error("Failed to create Keycloak user. Status: {}", response.getStatus());
                return null;
            }
        } catch (Exception e) {
            log.error("Keycloak user creation failed: {}", e.getMessage());
            return null;
        }
    }

    private void assignRole(User user, String keycloakUserId) {
        try {
            var realmResource = keycloak.realm(keycloakConfig.getRealm());
            var role = realmResource.roles().get(user.getRole().name()).toRepresentation();
            realmResource.users().get(keycloakUserId).roles().realmLevel().add(List.of(role));
        } catch (Exception e) {
            log.warn("Could not assign role {} to Keycloak user {}: {}",
                    user.getRole().name(), keycloakUserId, e.getMessage());
        }
    }
}
