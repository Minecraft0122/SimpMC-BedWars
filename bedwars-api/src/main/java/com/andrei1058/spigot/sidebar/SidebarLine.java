package com.andrei1058.spigot.sidebar;

import org.jetbrains.annotations.NotNull;

public class SidebarLine {

    private final String line;

    public SidebarLine() {
        this("");
    }

    public SidebarLine(String line) {
        this.line = line == null ? "" : line;
    }

    @NotNull
    public String getLine() {
        return line;
    }
}
