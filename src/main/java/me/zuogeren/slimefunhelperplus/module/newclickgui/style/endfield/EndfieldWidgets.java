package me.zuogeren.slimefunhelperplus.module.newclickgui.style.endfield;

import me.matl114.versioned.api.VDrawContext;
import me.zuogeren.slimefunhelperplus.module.newclickgui.core.ValueCodec;
import me.zuogeren.slimefunhelperplus.ui.EfCanvas;

/**
 * 终末地主题各个配置页共用的标准行控件与窗框画法。
 *
 * <p>收拢开关、滑杆轨道、虚线底边、字段编辑框、计数芯片、颜色芯片与页脚/外框，
 * 避免模块配置页、全配置页、Baritone 页各自重复实现导致像素细节漂移。
 */
public final class EndfieldWidgets {
    private static final int RING = 2;
    private static final int RING_INSET = 1;

    private EndfieldWidgets() {}

    /** 字段框：直角、panel 底、1px 边；聚焦时内侧压一圈 2px 强调线，不改变外沿占位。 */
    public static void paintFieldFrame(
            VDrawContext ctx, EfCanvas.Palette p, int x, int y, int w, int h, boolean focused, boolean hovered) {
        Ef.fill(ctx, x, y, w, h, p.panel());
        if (focused) {
            Ef.fill(ctx, x + RING_INSET, y + RING_INSET, w - RING_INSET * 2, RING, p.trace());
            Ef.fill(ctx, x + RING_INSET, y + h - RING_INSET - RING, w - RING_INSET * 2, RING, p.trace());
            Ef.fill(ctx, x + RING_INSET, y + RING_INSET, RING, h - RING_INSET * 2, p.trace());
            Ef.fill(ctx, x + w - RING_INSET - RING, y + RING_INSET, RING, h - RING_INSET * 2, p.trace());
            return;
        }
        int edge = hovered ? p.ink2() : p.rule();
        Ef.fill(ctx, x, y, w, 1, edge);
        Ef.fill(ctx, x, y + h - 1, w, 1, edge);
        Ef.fill(ctx, x, y, 1, h, edge);
        Ef.fill(ctx, x + w - 1, y, 1, h, edge);
    }

    /** 可编辑槽位的静态底边：2px 实 1px 空的短划，避免与整行分隔线贴成一条实线。 */
    public static void paintDottedRule(VDrawContext ctx, EfCanvas.Palette p, int x, int y, int w) {
        for (int i = 0; i < w; i += 3) {
            Ef.fill(ctx, x + i, y, Math.min(2, w - i), 1, p.rule());
        }
    }

    /** 行内文本/数值值槽的背景态：编辑画聚焦框，悬停画 1px 提示框，静态画虚线底边。 */
    public static void paintValueSlot(
            VDrawContext ctx, EfCanvas.Palette p, int x, int y, int w, int h, boolean editing, boolean hovered) {
        if (editing) {
            paintFieldFrame(ctx, p, x, y, w, h, true, false);
        } else if (hovered) {
            paintFieldFrame(ctx, p, x, y, w, h, false, true);
        } else {
            paintDottedRule(ctx, p, x, y + h - 2, w);
        }
    }

    /** 开关：开为实心墨块与右侧纸色缺口，关为虚线框与左侧小方块。 */
    public static void paintSwitch(VDrawContext ctx, EfCanvas.Palette p, int y, int rowH, int right, boolean on) {
        int sw = 20;
        int sh = 9;
        int sy = y + (rowH - sh) / 2;
        if (on) {
            Ef.fill(ctx, right - sw, sy, sw, sh, p.ink());
            Ef.fill(ctx, right - 9, sy + 2, 5, 5, p.paper());
        } else {
            Ef.dashedOutline(ctx, right - sw, sy, sw, sh, p.ink2());
            Ef.fill(ctx, right - sw + 2, sy + 2, 5, 5, p.ink2());
        }
    }

    /** 多分量行内的字段短名：右对齐贴着控件左侧，按剩余宽度截断以免盖住左侧主名。 */
    public static void paintPartName(
            VDrawContext ctx, EfCanvas.Palette p, int y, int minLeft, int rightEdge, String name, int ink) {
        if (name == null || name.isEmpty()) {
            return;
        }
        int available = Math.max(0, Math.min(150, rightEdge - minLeft - 8));
        if (available < 18) {
            return;
        }
        String shown = Ef.ellipsis(name, available);
        Ef.tracked(ctx, shown, rightEdge - Ef.trackedWidth(shown, 1), y + 5, ink, 1);
    }

    /** 滑杆轨道与 2px 竖条手柄。 */
    public static void paintSliderTrack(
            VDrawContext ctx, EfCanvas.Palette p, int trackX, int lineY, int trackW,
            double value, double min, double max) {
        Ef.fill(ctx, trackX, lineY, trackW, 1, p.rule());
        double ratio = max > min ? (value - min) / (max - min) : 0.0D;
        int knob = (int) (trackW * Math.max(0.0D, Math.min(1.0D, ratio)));
        Ef.fill(ctx, trackX, lineY, knob, 1, p.ink());
        Ef.fill(ctx, trackX + knob - 1, lineY - 3, 2, 7, p.ink());
    }

    /** 「N 项 ▾」计数切角块。 */
    public static void paintCountChip(
            VDrawContext ctx, EfCanvas.Palette p, int y, int right, String text, int cut, boolean open) {
        String label = text + "   ▾";
        int width = Ef.textWidth(label) + 10;
        int x = right - width;
        Ef.cutFill(ctx, x, y + 3, width, 12, open ? p.ink() : p.chip(), cut);
        Ef.text(ctx, label, x + 5, y + 5, open ? p.paper() : p.ink());
    }

    /** RGB 颜色行几何：切角底、色块（含到输入框之间的点击归属）、#RRGGBB 输入框。 */
    public record ColorGeometry(
            int chipX, int chipW,
            int swatchX, int swatchW, int swatchHitW,
            int hexFieldX, int hexFieldW, int hexTextX) {}

    /** RGBA 颜色行几何：左侧色块 + #RRGGBB 与 {@link ColorGeometry} 一致，最右多一格 {@code A:255} 透明度槽。 */
    public record RgbaGeometry(
            ColorGeometry color,
            int alphaFieldX, int alphaFieldW) {}

    public static ColorGeometry colorGeometry(int right) {
        int pad = 2;
        int swatch = 12;
        int gap = 5;
        int hexW = Ef.textWidth("#RRGGBB") + 2;
        int chipW = pad + swatch + gap + hexW + FieldEditor.FIELD_PAD;
        int chipX = right - chipW;
        int hexTextX = chipX + pad + swatch + gap;
        int hexFieldX = hexTextX - FieldEditor.FIELD_PAD;
        int hexFieldW = hexW + FieldEditor.FIELD_PAD * 2;
        return new ColorGeometry(chipX, chipW, chipX + pad, swatch, hexFieldX - chipX, hexFieldX, hexFieldW, hexTextX);
    }

    public static RgbaGeometry rgbaGeometry(int right) {
        int alphaW = Math.max(36, Ef.textWidth("A:255") + FieldEditor.FIELD_PAD * 2 + 2);
        int alphaX = right - alphaW;
        return new RgbaGeometry(colorGeometry(alphaX - 4), alphaX, alphaW);
    }

    /** 绘制标准 RGB 颜色行（色块 + #RRGGBB；手输时色块保留实时预览、右侧让出给输入框）。 */
    public static void paintColorRow(
            VDrawContext ctx, EfCanvas.Palette p, int right, int y, int rgb, boolean editingHex, int cut) {
        ColorGeometry box = colorGeometry(right);
        if (editingHex) {
            Ef.fill(ctx, box.swatchX(), y + 3, box.swatchW(), 12, 0xFF000000 | (rgb & 0xFFFFFF));
            paintFieldFrame(ctx, p, box.hexFieldX(), y + FieldEditor.FIELD_TOP, box.hexFieldW(),
                    FieldEditor.FIELD_H, true, false);
            return;
        }
        paintColorChip(ctx, p, box.chipX(), box.chipW(), y, box.swatchX(), box.swatchW(), box.hexTextX(), rgb, cut);
    }

    /** 绘制 RGBA 颜色行：色块 + #RRGGBB + 右侧 A:0..255 不透明度槽。 */
    public static void paintRgbaRow(
            VDrawContext ctx, EfCanvas.Palette p, int right, int y, int rgb, int alpha,
            boolean editingHex, boolean editingAlpha, boolean hotAlpha, int cut) {
        RgbaGeometry geo = rgbaGeometry(right);
        ColorGeometry box = geo.color();
        if (editingHex) {
            Ef.fill(ctx, box.swatchX(), y + 3, box.swatchW(), 12, 0xFF000000 | (rgb & 0xFFFFFF));
            paintFieldFrame(ctx, p, box.hexFieldX(), y + FieldEditor.FIELD_TOP, box.hexFieldW(),
                    FieldEditor.FIELD_H, true, false);
        } else {
            paintColorChip(ctx, p, box.chipX(), box.chipW(), y, box.swatchX(), box.swatchW(), box.hexTextX(), rgb, cut);
        }
        paintValueSlot(ctx, p, geo.alphaFieldX(), y + FieldEditor.FIELD_TOP, geo.alphaFieldW(),
                FieldEditor.FIELD_H, editingAlpha, hotAlpha);
        if (!editingAlpha) {
            String text = "A:" + Math.max(0, Math.min(255, alpha));
            Ef.text(ctx, text, geo.alphaFieldX() + geo.alphaFieldW() - FieldEditor.FIELD_PAD - Ef.textWidth(text),
                    y + 5, p.ink());
        }
    }

    /** 颜色行切角块：背景块 + 12x12 实色预览块 + {@code #RRGGBB}。 */
    public static void paintColorChip(
            VDrawContext ctx, EfCanvas.Palette p, int chipX, int chipW, int y,
            int swatchX, int swatchW, int hexX, int rgb, int cut) {
        Ef.cutFill(ctx, chipX, y + 3, chipW, 12, p.chip(), cut);
        Ef.fill(ctx, swatchX, y + 3, swatchW, 12, 0xFF000000 | (rgb & 0xFFFFFF));
        Ef.text(ctx, ValueCodec.hexOf(rgb), hexX, y + 5, p.ink());
    }

    /** 1px 实线窗口外框。 */
    public static void paintWindowFrame(VDrawContext ctx, EfCanvas.Palette p, int x, int y, int w, int h) {
        int edge = p.ink();
        Ef.fill(ctx, x, y, w, 1, edge);
        Ef.fill(ctx, x, y + h - 1, w, 1, edge);
        Ef.fill(ctx, x, y, 1, h, edge);
        Ef.fill(ctx, x + w - 1, y, 1, h, edge);
    }

    /** 页脚底栏与分隔线，以及两侧提示文本（任意一侧传 null 则省略）。 */
    public static void paintFooterBar(
            VDrawContext ctx, EfCanvas.Palette p, int winX, int winY, int winW, int winH, int footH,
            String left, String hint) {
        int y = winY + winH - footH;
        Ef.fill(ctx, winX, y, winW, footH, p.panel());
        Ef.rule(ctx, winX, y, winW, p.rule());
        if (left != null && !left.isEmpty()) {
            Ef.text(ctx, left, winX + 8, y + 3, p.ink2());
        }
        if (hint != null && !hint.isEmpty()) {
            Ef.text(ctx, hint, winX + winW - 8 - Ef.textWidth(hint), y + 3, p.ink2());
        }
    }
}
