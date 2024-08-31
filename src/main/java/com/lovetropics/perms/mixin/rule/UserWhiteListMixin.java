package com.lovetropics.perms.mixin.rule;

import com.lovetropics.perms.keycloak.KeycloakService;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.players.UserWhiteList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;

@Mixin(UserWhiteList.class)
public class UserWhiteListMixin {

    @Inject(method = "isWhiteListed", at = @At("HEAD"), cancellable = true)
    public void isWhiteListed(final GameProfile gameProfile, final CallbackInfoReturnable<Boolean> cir) throws URISyntaxException {
//        cir.setReturnValue(false);
        System.out.println("chekin yo whitelist he he he he");

        final boolean hasAccess = KeycloakService.hasAccessToLt24(gameProfile);

        if (!hasAccess) {
            System.out.println("User " + gameProfile.getName() + " does not have access to the server");
        }


        cir.setReturnValue(hasAccess);

    }
}
