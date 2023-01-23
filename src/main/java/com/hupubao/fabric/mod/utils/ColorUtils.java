package com.hupubao.fabric.mod.utils;

import java.awt.*;
import java.util.Set;

public class ColorUtils {


    public static Color findClosestPaletteColorTo(Color color, Set<Color> colorSet) {
        java.util.List<PaletteColor> colors = colorSet.stream().map(ColorUtils::convert).toList();

        int closestColor = -1;
        int closestDistance = Integer.MAX_VALUE;
        for (final PaletteColor paletteColor : colors) {
            final int distance = paletteColor.distanceTo(color.getRGB());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestColor = paletteColor.asInt();
            }
        }
        return new Color(closestColor);
    }


    private static PaletteColor convert(Color color) {
        return new PaletteColor(color.getRGB());
    }

    private static final class PaletteColor {
        private final int r;
        private final int g;
        private final int b;
        private final int color;

        public PaletteColor(final int color) {
            this.r = ((color & 0xff000000) >>> 24);
            this.g = ((color & 0x00ff0000) >>> 16);
            this.b = ((color & 0x0000ff00) >>> 8);
            this.color = color;
        }

        public int distanceTo(final int color) {
            final int deltaR = this.r - ((color & 0xff000000) >>> 24);
            final int deltaG = this.g - ((color & 0x00ff0000) >>> 16);
            final int deltaB = this.b - ((color & 0x0000ff00) >>> 8);
            return (deltaR * deltaR) + (deltaG * deltaG) + (deltaB * deltaB);
        }

        public int asInt() {
            return this.color;
        }
    }
}
