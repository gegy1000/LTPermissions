package com.lovetropics.perms.keycloak;

import com.mojang.authlib.GameProfile;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;
import java.util.stream.Stream;

public class KeycloakService {

    private static Keycloak INSTANCE;
    final static String serverUrl = "https://identity.lovetropics.org";
    final static String realm = "LoveTropics";
    final static String clientId = "lt-minecraft-query";
    final static String clientSecret = "secret";

    public KeycloakService() {

    }

    public void getRolesForPlayer(final GameProfile gameProfile) {
        getInstance().realm(realm)
                .users()
                .list()
                .forEach(userRepresentation -> {
                    System.out.println(userRepresentation.getUsername());
                });
    }

    public static boolean hasAccessToLt24(final GameProfile gameProfile) {

        final List<UserRepresentation> search = getInstance().realm(realm)
                .users().search(".tommi.");

        //final List<String> stringStream = getInstance().realm(realm).users().search(".tommi.").get(0).getGroups();

        getInstance().realm(realm).users().get("10d21d80-08d1-4857-aa9b-5bdbc9bfdb1e").groups().stream()
                .map(GroupRepresentation::getName)
                .forEach(s -> System.out.println(s));

        //.map(GroupRepresentation::getName).findFirst().map(Object::toString).orElse("NO_GROUP")

        getInstance().realm(realm)
                .users()
                .list()
                .forEach(userRepresentation -> {
                    final String username = userRepresentation.getUsername();
                    final List<String> realmRoles = userRepresentation.getRealmRoles();
                    //join realmRoles

                    final String roles = realmRoles == null ? "" : String.join(", ", realmRoles);
                    System.out.println(username + " has roles: " + roles);

                });


        return true;
    }

    public static Keycloak getInstance() {
        if (INSTANCE == null) {
            INSTANCE = createInstance();
        }
        return INSTANCE;
    }

    private static Keycloak createInstance() {
        if (INSTANCE == null) {
            INSTANCE = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .grantType(OAuth2Constants.PASSWORD)
                    .realm("master")
                    .clientId("admin-cli")
//                    .clientSecret(clientSecret)
                    .resteasyClient(ResteasyClientBuilder.newClient())
                    .username(clientId)
                    .password(clientSecret)
                    .build();
        }
        return INSTANCE;
    }
}