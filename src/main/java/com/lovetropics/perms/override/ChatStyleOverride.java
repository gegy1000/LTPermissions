package com.lovetropics.perms.override;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public final class ChatStyleOverride implements RoleOverride {
    private final String format;

    public ChatStyleOverride(String format) {
        this.format = format;
    }

    public ITextComponent make(ITextComponent name, String message) {
        return new TranslationTextComponent(this.format, name, message);
    }
}
