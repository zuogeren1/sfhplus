package me.zuogeren.slimefunhelperplus.module.newclickgui.style.endfield;

import java.util.function.Consumer;
import me.matl114.gui.GenericScreen;
import me.matl114.versioned.api.VDrawContext;
import me.zuogeren.slimefunhelperplus.hostgui.HostControls;
import me.zuogeren.slimefunhelperplus.hostgui.HostScreens;
import me.zuogeren.slimefunhelperplus.ui.EfCanvas;
import net.minecraft.client.gui.components.EditBox;

/**
 * 一格里"点一下就开始改"的公共输入框。
 *
 * <p>分类页与全配置页原先只能加减或轮换，改不了大跨度数值；这里把模块配置页验证过的那套做法抽出来：
 * 点击只记下目标，原版 {@link EditBox} 一律等到 {@link #tick()} 里增删——
 * 点击与渲染期间往屏幕上加控件会撞进遍历，而程序改值又会触发 responder。
 *
 * <p>两种提交方式：{@code live} 每敲一个字就交出去（数值，解析不了的半截输入由页面自己忽略）；
 * 非 {@code live} 只在 Enter 或点到别处时交一次（文本，避免每敲一个字就写一次配置）。
 */
public final class FieldEditor {
    public static final int FIELD_H = 16;
    public static final int FIELD_PAD = 4;
    public static final int FIELD_TOP = 1;

    private GenericScreen screen;
    private EditBox box;
    private String pendingKey;
    private String boundKey;
    private int pendingX;
    private int pendingY;
    private int pendingW;
    private String seed = "";
    private boolean live;
    private int maxLength = 256;
    private Consumer<String> sink = text -> {};

    /**
     * 打开一格。{@code key} 是这一格的稳定身份（同一条配置在同一行的 key 不变），
     * 页面每帧用 {@link #isOpenFor(String)} 决定要不要让出绘制。
     *
     * <p>{@code x/y/w} 是字段框的矩形（高固定 {@link #FIELD_H}），输入框落在框内缩进 {@link #FIELD_PAD} 的位置；
     * 绘制与命中都用这同一份矩形，点框内任何地方都不会把正在打的内容丢掉。
     */
    public void open(GenericScreen screen, String key, int x, int y, int w, String text, boolean live, Consumer<String> sink) {
        open(screen, key, x, y, w, text, live, live ? 24 : 256, sink);
    }

    /** 支持指定最大长度的重载（例如长路径文本用 {@code 1024}）。 */
    public void open(
            GenericScreen screen, String key, int x, int y, int w,
            String text, boolean live, int maxLength, Consumer<String> sink) {
        this.screen = screen;
        this.pendingKey = key;
        this.boundKey = null;
        this.pendingX = x;
        this.pendingY = y;
        this.pendingW = Math.max(24, w);
        this.seed = text == null ? "" : text;
        this.live = live;
        this.maxLength = Math.max(1, maxLength);
        this.sink = sink == null ? text0 -> {} : sink;
    }

    public boolean isOpen() {
        return pendingKey != null;
    }

    public boolean isOpenFor(String key) {
        return pendingKey != null && pendingKey.equals(key);
    }

    public String key() {
        return pendingKey;
    }

    public String text() {
        return box == null ? seed : box.getValue();
    }

    /** 收起：非即时模式把最后的值交出去，Enter 与点别处走同一条路。 */
    public void close() {
        if (pendingKey == null) {
            return;
        }
        pendingKey = null;
        if (box == null) {
            return;
        }
        if (!live) {
            String value = box.getValue();
            if (!value.equals(seed)) {
                seed = value;
                sink.accept(value);
            }
        }
        // 拆控件要等 tick；这一小段里先把字和按键都掐掉，免得旧框继续接键。
        // 绑定一并作废：同一格紧接着再打开时也走"重建"，不会留着一只没接 responder 的旧框。
        box.setResponder(null);
        box.setVisible(false);
        boundKey = null;
    }

    /**
     * 输入框是否接这一下：接就等于让容器把点击继续派给它，不去挪光标就等于把正在打的内容换回旧值。
     *
     * <p>按字段框（而不是里面那 9px 的输入框）判定：框的下半截也属于这一格，
     * 落在那里若判成"点别处"，就会把正在打的内容换回旧值。
     */
    public boolean contains(double mouseX, double mouseY) {
        return pendingKey != null
                && box != null
                && box.isVisible()
                && mouseX >= pendingX
                && mouseX < pendingX + pendingW
                && mouseY >= pendingY
                && mouseY < pendingY + FIELD_H;
    }

    /** 输入框的颜色跟随主题：默认白字在浅色 panel 上看不见。 */
    public void setTextColor(int color) {
        if (box != null) {
            box.setTextColor(color);
        }
    }

    public void tick() {
        if (pendingKey == null) {
            destroy();
            return;
        }
        if (box != null && pendingKey.equals(boundKey) && screen != null && screen.children().contains(box)) {
            return;
        }
        if (box != null && pendingKey.equals(boundKey)) {
            seed = box.getValue();
        }
        destroy();
        create();
    }

    private void create() {
        if (screen == null) {
            return;
        }
        EditBox created = HostControls.submitEditBox(
                pendingX + FIELD_PAD,
                pendingY + FIELD_PAD,
                Math.max(16, pendingW - FIELD_PAD * 2),
                maxLength,
                this::close);
        HostControls.setValueQuietly(
                created,
                seed,
                live ? text -> {
                    if (box == created) {
                        sink.accept(text);
                    }
                } : null);
        // 输入框有长度上限：回填被截短时以它实际收下的为准，否则点掉时会写回截断值
        this.seed = created.getValue();
        this.box = created;
        this.boundKey = pendingKey;
        HostScreens.addChild(this.screen, created);
        this.screen.setFocused(created);
    }

    private void destroy() {
        if (box == null) {
            return;
        }
        EditBox old = box;
        box = null;
        boundKey = null;
        old.setResponder(null);
        if (screen != null) {
            if (screen.getFocused() == old) {
                screen.setFocused(null);
            }
            HostScreens.removeChild(screen, old);
        }
    }

    /**
     * 字段框：直角、panel 底、1px 边，聚焦时边框内侧再压一圈 2px 的当前位置线。
     *
     * <p>切角在这套外观里表示"可点击"，输入框是往里写东西的格子，所以是直角；
     * 环画在内侧，框的外沿尺寸不随聚焦变，相邻控件不会因为聚焦挪位。
     */
    public static void paintFrame(
            VDrawContext ctx, EfCanvas.Palette p, int x, int y, int w, int h, boolean focused, boolean hovered) {
        EndfieldWidgets.paintFieldFrame(ctx, p, x, y, w, h, focused, hovered);
    }
}