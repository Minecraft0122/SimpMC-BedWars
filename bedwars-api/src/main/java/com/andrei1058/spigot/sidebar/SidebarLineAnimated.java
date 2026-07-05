package com.andrei1058.spigot.sidebar;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class SidebarLineAnimated extends SidebarLine {

    private final List<String> frames;
    private int frameIndex = 0;

    public SidebarLineAnimated(String[] frames) {
        this(Arrays.asList(frames));
    }

    public SidebarLineAnimated(@NotNull Collection<String> frames) {
        this.frames = new ArrayList<>(frames);
    }

    @Override
    @NotNull
    public String getLine() {
        if (frames.isEmpty()) {
            return "";
        }
        String frame = frames.get(frameIndex);
        frameIndex++;
        if (frameIndex >= frames.size()) {
            frameIndex = 0;
        }
        return frame == null ? "" : frame;
    }
}
