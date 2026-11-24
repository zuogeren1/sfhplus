package me.zuogeren.slimefunhelperplus.module.custommodel.ui;

import java.util.List;
import me.matl114.renderer.text.TextDrawer;
import me.matl114.versioned.api.VDrawContext;
import me.zuogeren.slimefunhelperplus.ui.EfCanvas;
import net.minecraft.resources.Identifier;

/**
 * {@code custommodel} 模块的自绘外观入口：复用 {@link EfCanvas} 底层原语，但动效与字体优化独立固定开启，
 * 不跟 {@code newclickgui} 的开关联动。
 */
public final class Ef {
    private Ef() {}

    /** custommodel 屏不跟 newclickgui 的配置联动。 */
    private static final boolean ANIMATIONS = true;

    /** 同上，固定开。 */
    private static final boolean FONT_OPTIMIZE = true;

    public static final int SIGNAL = EfCanvas.SIGNAL;
    public static final int ON_SIGNAL = EfCanvas.ON_SIGNAL;

    public static final int BAR_BG = EfCanvas.BAR_BG;
    public static final int BAR_INK = EfCanvas.BAR_INK;
    public static final int BAR_INK2 = EfCanvas.BAR_INK2;
    public static final int BAR_RULE = EfCanvas.BAR_RULE;

    public static final int TRI_A = EfCanvas.TRI_A;
    public static final int TRI_C = EfCanvas.TRI_C;

    public static final int CUT = EfCanvas.CUT;
    public static final int CUT_WIDE = EfCanvas.CUT_WIDE;
    public static final int CUT_PANEL = EfCanvas.CUT_PANEL;

    public static final EfCanvas.Palette DARK = EfCanvas.DARK;
    public static final EfCanvas.Palette LIGHT = EfCanvas.LIGHT;

    public static EfCanvas.Palette palette(boolean light) {
        return EfCanvas.palette(light);
    }

    public static int mix(int a, int b, float t) {
        return EfCanvas.mix(a, b, t);
    }

    public static int alpha(int color, float factor) {
        return EfCanvas.alpha(color, factor);
    }

    public static float snap(float t) {
        return EfCanvas.snap(t);
    }

    public static float mech(float t) {
        return EfCanvas.mech(t);
    }

    public static float ease(float t) {
        return EfCanvas.ease(t);
    }

    public static float progress(long startMillis, float durationMillis) {
        return EfCanvas.progress(startMillis, durationMillis, ANIMATIONS);
    }

    public static void fill(VDrawContext ctx, int x, int y, int w, int h, int color) {
        EfCanvas.fill(ctx, x, y, w, h, color);
    }

    public static void wipe(VDrawContext ctx, int x, int y, int w, int h, int base, int accent, float progress) {
        EfCanvas.wipe(ctx, x, y, w, h, base, accent, progress);
    }

    public static void wipeCut(
            VDrawContext ctx, int x, int y, int w, int h, int base, int accent, float progress,
            int cut, EfCanvas.Corner corner) {
        EfCanvas.wipeCut(ctx, x, y, w, h, base, accent, progress, cut, corner);
    }

    public static void cutFill(VDrawContext ctx, int x, int y, int w, int h, int color, int cut) {
        EfCanvas.cutFill(ctx, x, y, w, h, color, cut);
    }

    public static void cutFill(
            VDrawContext ctx, int x, int y, int w, int h, int color, int cut, EfCanvas.Corner corner) {
        EfCanvas.cutFill(ctx, x, y, w, h, color, cut, corner);
    }

    public static void icon(VDrawContext ctx, Identifier sprite, int x, int y, int size, int color) {
        EfCanvas.icon(ctx, sprite, x, y, size, color);
    }

    public static void image(VDrawContext ctx, Identifier texture, int x, int y, int w, int h) {
        EfCanvas.image(ctx, texture, x, y, w, h);
    }

    public static void cutOutline(VDrawContext ctx, int x, int y, int w, int h, int color, int cut) {
        EfCanvas.cutOutline(ctx, x, y, w, h, color, cut);
    }

    public static void cutOutline(
            VDrawContext ctx, int x, int y, int w, int h, int color, int cut, EfCanvas.Corner corner) {
        EfCanvas.cutOutline(ctx, x, y, w, h, color, cut, corner);
    }

    public static void dashedOutline(VDrawContext ctx, int x, int y, int w, int h, int color) {
        EfCanvas.dashedOutline(ctx, x, y, w, h, color);
    }

    public static void rule(VDrawContext ctx, int x, int y, int w, int color) {
        EfCanvas.rule(ctx, x, y, w, color);
    }

    public static void scrollbar(
            VDrawContext ctx, int x, int y, int h, int offset, int total, int visible, int track, int thumb) {
        EfCanvas.scrollbar(ctx, x, y, h, offset, total, visible, track, thumb);
    }

    public static void tooltip(
            VDrawContext ctx, EfCanvas.Palette palette, List<String> lines,
            int mouseX, int mouseY, int screenW, int screenH) {
        EfCanvas.tooltip(ctx, palette, lines, mouseX, mouseY, screenW, screenH, FONT_OPTIMIZE);
    }

    public static TextDrawer font() {
        return EfCanvas.font();
    }

    public static int lineH() {
        return EfCanvas.lineH(FONT_OPTIMIZE);
    }

    public static int textWidth(String text) {
        return EfCanvas.textWidth(text);
    }

    public static void text(VDrawContext ctx, String text, int x, int y, int color) {
        EfCanvas.text(ctx, text, x, y, color);
    }

    public static int trackedWidth(String text, int tracking) {
        return EfCanvas.trackedWidth(text, tracking, FONT_OPTIMIZE);
    }

    public static void tracked(VDrawContext ctx, String text, int x, int y, int color, int tracking) {
        EfCanvas.tracked(ctx, text, x, y, color, tracking, FONT_OPTIMIZE);
    }

    public static int wideWidth(String text, float stretch) {
        return EfCanvas.wideWidth(text, stretch);
    }

    public static int trackedWideWidth(String text, int tracking, float stretch) {
        return EfCanvas.trackedWideWidth(text, tracking, stretch, FONT_OPTIMIZE);
    }

    public static void trackedWide(
            VDrawContext ctx, String text, int x, int y, int color, int tracking, float stretch) {
        EfCanvas.trackedWide(ctx, text, x, y, color, tracking, stretch, FONT_OPTIMIZE);
    }

    public static void wide(VDrawContext ctx, String text, int x, int y, int color, float stretch) {
        EfCanvas.wide(ctx, text, x, y, color, stretch);
    }

    public static List<String> wrap(String text, int maxWidth) {
        return EfCanvas.wrap(text, maxWidth);
    }

    public static String ellipsis(String text, int maxWidth) {
        return EfCanvas.ellipsis(text, maxWidth);
    }

    public static String ellipsisWide(String text, int maxWidth, int tracking, float stretch) {
        return EfCanvas.ellipsisWide(text, maxWidth, tracking, stretch, FONT_OPTIMIZE);
    }
}
