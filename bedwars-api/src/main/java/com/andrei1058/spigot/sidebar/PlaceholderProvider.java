package com.andrei1058.spigot.sidebar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class PlaceholderProvider {

    private final String placeholder;
    private final Supplier<String> provider;

    public PlaceholderProvider(@NotNull String placeholder, @NotNull Supplier<String> provider) {
        this.placeholder = placeholder;
        this.provider = provider;
    }

    @NotNull
    public String getPlaceholder() {
        return placeholder;
    }

    @Nullable
    public String getValue() {
        return provider.get();
    }

    @NotNull
    public Supplier<String> getProvider() {
        return provider;
    }
}
