package com.lovetropics.perms.modifier;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public final class ChatStyleModifier implements RoleModifier {
    private final String format;

    public ChatStyleModifier(String format) {
        this.format = format;
    }

    public ITextComponent make(ITextComponent name, String message) {
        return new TranslationTextComponent(this.format, name, message);
    }
}
