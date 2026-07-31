package com.andrei1058.bedwars.sidebar;

import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.arena.team.TeamColor;
import org.bukkit.Color;

import java.util.Comparator;
import java.util.UUID;

/** Orders BedWars teams from the red end of the visible spectrum to violet. */
final class TabTeamOrder {

    static final Comparator<ITeam> COMPARATOR = TabTeamOrder::compare;

    private TabTeamOrder() {
    }

    private static int compare(ITeam left, ITeam right) {
        if (left == right) return 0;
        if (left == null) return 1;
        if (right == null) return -1;

        int result = key(left.getColor()).compareTo(key(right.getColor()));
        if (result != 0) return result;

        String leftName = left.getName() == null ? "" : left.getName();
        String rightName = right.getName() == null ? "" : right.getName();
        result = String.CASE_INSENSITIVE_ORDER.compare(leftName, rightName);
        if (result != 0) return result;
        result = leftName.compareTo(rightName);
        if (result != 0) return result;

        return compareIdentity(left.getIdentity(), right.getIdentity());
    }

    private static SpectrumKey key(TeamColor teamColor) {
        if (teamColor == null) return SpectrumKey.UNKNOWN;
        Color color = teamColor.bukkitColor();
        if (color == null) return SpectrumKey.UNKNOWN;
        double red = color.getRed() / 255.0;
        double green = color.getGreen() / 255.0;
        double blue = color.getBlue() / 255.0;
        double maximum = Math.max(red, Math.max(green, blue));
        double minimum = Math.min(red, Math.min(green, blue));
        double range = maximum - minimum;

        if (range == 0.0) {
            return new SpectrumKey(1, 0.0, -maximum);
        }

        double hue;
        if (maximum == red) {
            hue = ((green - blue) / range) % 6.0;
        } else if (maximum == green) {
            hue = ((blue - red) / range) + 2.0;
        } else {
            hue = ((red - green) / range) + 4.0;
        }
        hue *= 60.0;
        if (hue < 0.0) hue += 360.0;
        return new SpectrumKey(0, hue, -maximum);
    }

    private static int compareIdentity(UUID left, UUID right) {
        if (left == right) return 0;
        if (left == null) return 1;
        if (right == null) return -1;
        return left.compareTo(right);
    }

    private record SpectrumKey(int group, double hue, double inverseBrightness)
            implements Comparable<SpectrumKey> {

        private static final SpectrumKey UNKNOWN = new SpectrumKey(2, 0.0, 0.0);

        @Override
        public int compareTo(SpectrumKey other) {
            int result = Integer.compare(group, other.group);
            if (result != 0) return result;
            result = Double.compare(hue, other.hue);
            if (result != 0) return result;
            return Double.compare(inverseBrightness, other.inverseBrightness);
        }
    }
}
