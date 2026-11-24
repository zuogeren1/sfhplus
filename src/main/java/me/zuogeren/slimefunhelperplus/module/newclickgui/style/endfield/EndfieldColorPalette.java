package me.zuogeren.slimefunhelperplus.module.newclickgui.style.endfield;

import java.awt.Color;
import java.util.function.IntConsumer;
import me.matl114.versioned.api.VDrawContext;
import me.zuogeren.slimefunhelperplus.module.newclickgui.core.ValueCodec;
import me.zuogeren.slimefunhelperplus.ui.EfCanvas;

/**
 * 终末地风格页面共用的内嵌调色盘浮层。
 *
 * <p>铺在色块下方（放不下则翻到上方），按 {@value #CELL}px 格子铺饱和度/明度方块与色相条，
 * 绘制与点击命中共用同一份 {@link Box} 坐标，点哪格就取那一格中心对应颜色。
 */
public final class EndfieldColorPalette {
    private static final int CELL = 4;
    private static final int SV_W = 104;
    private static final int SV_H = 64;
    private static final int HUE_H = 12;
    private static final int GAP = 4;
    private static final int PAD = 8;
    private static final int HEAD = 14;

    private Object target;
    private int anchorX;
    private int anchorW;
    private int rowY;

    public record Box(int x, int y, int w, int h, int svX, int svY, int hueX, int hueY) {
        public boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        }
    }

    public void open(Object target, int anchorX, int anchorW, int rowY) {
        this.target = target;
        this.anchorX = anchorX;
        this.anchorW = anchorW;
        this.rowY = rowY;
    }

    public void close() {
        this.target = null;
    }

    public boolean isOpen() {
        return target != null;
    }

    public Object target() {
        return target;
    }

    public Box box(int winX, int winY, int winW, int winH, int rowH) {
        int headW = Ef.trackedWideWidth("// COLOR", 1, 1.2F) + GAP * 2 + Ef.textWidth("#RRGGBB");
        int w = PAD * 2 + Math.max(SV_W, headW);
        int h = PAD * 2 + HEAD + SV_H + GAP + HUE_H;
        int x = Math.max(winX + 4, Math.min(winX + winW - 4 - w, anchorX + anchorW - w));
        int y = rowY + rowH + 2;
        if (y + h > winY + winH - 4) {
            y = rowY - h - 2;
        }
        y = Math.max(winY + 4, Math.min(winY + winH - 4 - h, y));
        int svX = x + PAD;
        int svY = y + PAD + HEAD;
        return new Box(x, y, w, h, svX, svY, svX, svY + SV_H + GAP);
    }

    /** 绘制调色盘；鼠标悬停在调色盘上时返回 {@code true}，调用方据此屏蔽底下行提示。 */
    public boolean paint(
            VDrawContext ctx, EfCanvas.Palette p, int winX, int winY, int winW, int winH, int rowH,
            int rgb, int mouseX, int mouseY) {
        if (target == null) {
            return false;
        }
        Box box = box(winX, winY, winW, winH, rowH);
        float[] hsb = hsbOf(rgb);

        int cut = Ef.CUT_PANEL;
        Ef.cutFill(ctx, box.x(), box.y(), box.w(), box.h(), p.panel(), cut);
        Ef.fill(ctx, box.x(), box.y(), box.w(), 1, p.ink());
        Ef.fill(ctx, box.x(), box.y(), 1, box.h(), p.ink());
        Ef.fill(ctx, box.x() + box.w() - 1, box.y(), 1, box.h() - cut, p.ink());
        Ef.fill(ctx, box.x(), box.y() + box.h() - 1, box.w() - cut, 1, p.ink());
        for (int i = 0; i < cut; i++) {
            Ef.fill(ctx, box.x() + box.w() - cut + i, box.y() + box.h() - 1 - i, 1, 1, p.ink());
        }
        Ef.trackedWide(ctx, "// COLOR", box.x() + PAD, box.y() + PAD - 3, p.ink2(), 1, 1.2F);
        String hex = ValueCodec.hexOf(rgb);
        Ef.text(ctx, hex, box.x() + box.w() - PAD - Ef.textWidth(hex), box.y() + PAD - 3, p.ink());

        int cols = SV_W / CELL;
        int rows = SV_H / CELL;
        for (int row = 0; row < rows; row++) {
            float v = 1.0F - (row + 0.5F) / rows;
            for (int col = 0; col < cols; col++) {
                float s = (col + 0.5F) / cols;
                Ef.fill(ctx, box.svX() + col * CELL, box.svY() + row * CELL,
                        CELL, CELL, opaque(Color.HSBtoRGB(hsb[0], s, v)));
            }
        }
        markCell(ctx, p.ink(), box.svX(), box.svY(), cols, rows, CELL,
                (int) (hsb[1] * cols), (int) ((1.0F - hsb[2]) * rows));

        for (int col = 0; col < cols; col++) {
            float hue = (col + 0.5F) / cols;
            Ef.fill(ctx, box.hueX() + col * CELL, box.hueY(), CELL, HUE_H,
                    opaque(Color.HSBtoRGB(hue, 1.0F, 1.0F)));
        }
        markCell(ctx, p.ink(), box.hueX(), box.hueY(), cols, 1, HUE_H,
                (int) (hsb[0] * cols), 0);

        return box.contains(mouseX, mouseY);
    }

    /** 处理调色盘点击：点在面板内返回 {@code true}（左键采色回调）；点在面板外不关闭由调用方决定后续。 */
    public boolean click(
            int winX, int winY, int winW, int winH, int rowH, int currentRgb,
            int mouseX, int mouseY, int button, IntConsumer onPick) {
        if (target == null) {
            return false;
        }
        Box box = box(winX, winY, winW, winH, rowH);
        if (!box.contains(mouseX, mouseY)) {
            return false;
        }
        if (button != 0) {
            return true;
        }
        float[] hsb = hsbOf(currentRgb);
        int cols = SV_W / CELL;
        if (mouseY >= box.hueY() && mouseY < box.hueY() + HUE_H) {
            int col = cellAt(mouseX - box.hueX(), cols);
            if (col >= 0) {
                onPick.accept(Color.HSBtoRGB((col + 0.5F) / cols, hsb[1], hsb[2]));
            }
            return true;
        }
        int rows = SV_H / CELL;
        if (mouseX < box.svX() || mouseX >= box.svX() + SV_W
                || mouseY < box.svY() || mouseY >= box.svY() + SV_H) {
            return true;
        }
        int col = cellAt(mouseX - box.svX(), cols);
        int row = cellAt(mouseY - box.svY(), rows);
        if (col >= 0 && row >= 0) {
            onPick.accept(Color.HSBtoRGB(hsb[0], (col + 0.5F) / cols, 1.0F - (row + 0.5F) / rows));
        }
        return true;
    }

    private static void markCell(VDrawContext ctx, int color, int x, int y, int cols, int rows, int cellH,
            int col, int row) {
        int c = Math.max(0, Math.min(cols - 1, col));
        int r = Math.max(0, Math.min(rows - 1, row));
        int cx = x + c * CELL;
        int cy = y + r * CELL;
        int h = rows > 1 ? CELL : cellH;
        Ef.fill(ctx, cx, cy, CELL, 1, color);
        Ef.fill(ctx, cx, cy + h - 1, CELL, 1, color);
        Ef.fill(ctx, cx, cy, 1, h, color);
        Ef.fill(ctx, cx + CELL - 1, cy, 1, h, color);
    }

    private static int cellAt(int offset, int cells) {
        int cell = offset / CELL;
        return offset >= 0 && cell < cells ? cell : -1;
    }

    private static float[] hsbOf(int rgb) {
        return Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
    }

    private static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }
}
