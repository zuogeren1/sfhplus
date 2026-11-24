package me.zuogeren.slimefunhelperplus.module.newclickgui.style.endfield.pages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import me.zuogeren.slimefunhelperplus.hostgui.HostControls;
import me.zuogeren.slimefunhelperplus.hostgui.HostEditors;
import me.matl114.gui.GenericScreen;
import me.matl114.hacks.utils.config.EntrySet;
import me.matl114.hacks.utils.config.Holder;
import me.matl114.hacks.utils.config.NBTTypes;
import me.matl114.hacks.utils.config.PrimitiveList;
import me.matl114.hacks.utils.config.PrimitiveMap;
import me.matl114.hacks.utils.config.WrapColor;
import me.matl114.hooks.BaritoneHooks;
import me.matl114.utils.ChatUtils;
import me.matl114.utils.config.ValueAccessor;
import me.matl114.versioned.api.VDrawContext;
import me.zuogeren.slimefunhelperplus.module.newclickgui.NewClickGui;
import me.zuogeren.slimefunhelperplus.module.newclickgui.core.BoundsResolver;
import me.zuogeren.slimefunhelperplus.module.newclickgui.core.RecordFieldOps;
import me.zuogeren.slimefunhelperplus.module.newclickgui.core.ValueCodec;
import me.zuogeren.slimefunhelperplus.module.newclickgui.style.endfield.Ef;
import me.zuogeren.slimefunhelperplus.module.newclickgui.style.endfield.EndfieldColorPalette;
import me.zuogeren.slimefunhelperplus.module.newclickgui.style.endfield.EndfieldTabBar;
import me.zuogeren.slimefunhelperplus.module.newclickgui.style.endfield.EndfieldWidgets;
import me.zuogeren.slimefunhelperplus.module.newclickgui.style.endfield.FieldEditor;
import me.zuogeren.slimefunhelperplus.module.newclickgui.ui.GuiStyles;
import me.zuogeren.slimefunhelperplus.module.newclickgui.ui.Settings;
import me.zuogeren.slimefunhelperplus.ui.EfCanvas;
import me.zuogeren.slimefunhelperplus.ui.EfWidget;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

/**
 * 「Baritone」页：{@link BaritoneHooks} 暴露的整张设置表。
 *
 * <p>这些设置是 Baritone 自己的 Setting，只有 {@code getValue} / {@code setValue}，
 * 没有配置路径、默认值和校验器，所以不复用模块配置页那套按路径分节的表单：
 * 这里一页平铺，控件按值的运行时类型挑。
 *
 * <p>值必须按运行时对象判类型、写回也要写回同一个类型：Baritone 的 Setting 是按泛型读出来的，
 * 把 Double 塞进 Integer 设置，它下一次取值就会抛转型异常。
 */
public class BaritonePage extends GenericScreen {
    private static final int BAR_H = 22;

    /** 顶部分类条：切分类在同一屏里完成。 */
    private final EndfieldTabBar tabBar = new EndfieldTabBar();

    private int barH = BAR_H;
    private static final int FOOT_H = 14;
    private static final int ROW_H = 18;
    private static final int PAD = 14;
    /** 数值槽位的固定宽度：宽度稳定，滑轨才不会随数值跳动。 */
    private static final int TEXT_W = 44;
    /** 行尾重置按钮占的宽度。 */
    private static final int RESET_W = 18;
    /** 行首序号栏的宽度。 */
    private static final int INDEX_W = 22;
    /** 设置项翻译键前缀，与宿主旧界面同一个约定。 */
    private static final String KEY_PREFIX = "widget.click-gui.baritone.";

    private final Settings settings;

    /** Baritone 是否可用：不可用时整页只写一句说明，不画空白表单。 */
    private final boolean available;

    private final List<Entry> entries;

    private int winX;
    private int winY;
    private int winW;
    private int winH;

    private int contentLeft;
    private int contentWidth;
    private int scroll;
    private int totalHeight;

    private final List<Row> rows = new ArrayList<>();

    /** 每项滑杆的取值区间：只在当前值跑出区间时重取。 */
    private final BoundsResolver ranges = new BoundsResolver();

    /** 出厂值：Baritone 的 Setting 自带 defaultValue，拿不到的那些就不给重置入口。 */
    private final Map<String, Object> defaults = new HashMap<>();

    /** 设置项说明的折行结果，按设置名缓存，不每帧查语言表。 */
    private final Map<String, List<String>> tips = new HashMap<>();

    /** 打开中的枚举下拉。 */
    private Entry dropdown;
    private int dropX;
    private int dropY;
    private int dropW;

    /** 数值槽就地手输：开着的这一行让出自己的文本，交给原版输入框画。 */
    private final FieldEditor editor = new FieldEditor();

    /** 颜色项共用调色盘浮层。 */
    private final EndfieldColorPalette palettePanel = new EndfieldColorPalette();

    private Entry hovered;

    /** 正在拖动的滑杆：轨道与区间都按按下那一刻记下的算，拖动中数值变化不会让轨道左右跳。 */
    private Entry dragEntry;
    private int dragTrackX;
    private int dragTrackW;
    private double dragMin;
    private double dragMax;

    /** 一个设置项：Baritone 的设置名与它唯一的读写入口。 */
    private record Entry(String name, ValueAccessor<?> accessor) {}

    /** 一帧内布局出来的一行：序号是它在设置表里的位置，y 已经是屏幕绝对坐标。 */
    private record Row(int index, int y, Entry entry) {}

    public BaritonePage(Settings settings) {
        super(Component.empty(), 0, 0);
        this.settings = settings;
        this.available = isApiSupported();
        this.entries = this.available ? readSettings() : List.of();
        if (this.available) {
            readDefaults();
        }
    }

    /** 出厂值不动，读一次就够；老版本 Baritone 没有这个能力时整页不显示重置。 */
    private void readDefaults() {
        for (Entry entry : this.entries) {
            try {
                Object value = BaritoneHooks.getInstance().getDefaultValue(entry.name());
                if (value instanceof Map<?, ?> rawMap && valueOf(entry) instanceof PrimitiveMap<?, ?>) {
                    value = wrapBlockMapDefault(rawMap);
                }
                if (value != null) {
                    defaults.put(entry.name(), value);
                }
            } catch (Throwable ignored) {
                // 这一项没有出厂值：跳过，不影响其它行
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    private static PrimitiveMap<Holder<Block>, PrimitiveList<Holder<Block>>> wrapBlockMapDefault(Map<?, ?> rawMap) {
        Map<Holder<Block>, PrimitiveList<Holder<Block>>> converted = rawMap.entrySet().stream()
                .filter(e -> e.getKey() instanceof Block && e.getValue() instanceof List<?>)
                .collect(Collectors.toMap(
                        e -> Holder.of(BuiltInRegistries.BLOCK, (Block) e.getKey()),
                        e -> new PrimitiveList<>(
                                NBTTypes.HOLDER_TYPE.cast(),
                                ((List<?>) e.getValue()).stream()
                                        .filter(Block.class::isInstance)
                                        .map(b -> Holder.of(BuiltInRegistries.BLOCK, (Block) b))
                                        .toList(),
                                Holder.of(BuiltInRegistries.BLOCK, null)),
                        (a, b) -> b));
        return new PrimitiveMap<>(
                NBTTypes.HOLDER_TYPE.cast(),
                NBTTypes.PRIMITIVE_LIST_TYPE.cast(),
                converted,
                Holder.of(BuiltInRegistries.BLOCK, null),
                new PrimitiveList<>(NBTTypes.HOLDER_TYPE.cast(), List.of(), Holder.of(BuiltInRegistries.BLOCK, null)));
    }

    // ------------------------------------------------------------------ 数据

    private static boolean isApiSupported() {
        try {
            return BaritoneHooks.getInstance().isBaritoneAPISupported();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** 设置表的顺序就是 Baritone 自己的声明顺序，照原样排，不按名字重排。 */
    private static List<Entry> readSettings() {
        List<Entry> out = new ArrayList<>();
        try {
            Map<String, ValueAccessor<?>> all = BaritoneHooks.getInstance().getAllSettings();
            if (all == null) {
                return out;
            }
            for (Map.Entry<String, ValueAccessor<?>> pair : all.entrySet()) {
                if (pair.getKey() != null && pair.getValue() != null) {
                    out.add(new Entry(pair.getKey(), pair.getValue()));
                }
            }
        } catch (Throwable ignored) {
            // 读不出设置表就按空表处理：页面照样能开，只是没有行
        }
        return out;
    }

    private Object valueOf(Entry entry) {
        try {
            return entry.accessor().getValue();
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** 访问器是按擦除后的裸类型存的，写回也只能走裸类型，再靠 coerce 还原成原来的数值类型。 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void write(Entry entry, Object value) {
        try {
            ((ValueAccessor) entry.accessor()).setValue(value);
        } catch (Throwable ignored) {
            // 写不进去就保持原值，下一帧照旧显示真实状态
        }
    }

    private static boolean integral(Object value) {
        return RecordFieldOps.isIntegral(value);
    }

    /** 滑杆的取值区间：只在当前值跑出区间时按新数量级重算，拖动过程中不会跳档。 */
    private double[] range(Entry entry, double current, boolean whole) {
        return ranges.resolveUnbounded(entry.name(), current, whole);
    }

    private static Object coerce(Object current, double next) {
        return RecordFieldOps.coerceValue(current, next);
    }

    private static String label(Entry entry) {
        return Component.translatableWithFallback(KEY_PREFIX + entry.name(), entry.name())
                .getString();
    }

    /** 设置名之外再没有别的地方能说明它是什么，说明文字沿用宿主 {@code <键名>.tooltips} 的约定。 */
    private List<String> tipLines(Entry entry) {
        return tips.computeIfAbsent(entry.name(), name -> {
            List<String> out = new ArrayList<>();
            for (Component component : ChatUtils.parseTooltipsTranslation(KEY_PREFIX + name + ".tooltips", "")) {
                String text = component == null ? "" : component.getString();
                if (!text.isEmpty()) {
                    out.addAll(Ef.wrap(text, 240));
                }
            }
            return out;
        });
    }

    /** 只读值的展示文本：容器只报类别，别把几千条目的列表摊成一行字。 */
    private static String preview(Object value) {
        if (value == null) {
            return "无值";
        }
        if (value instanceof CharSequence text) {
            String raw = text.toString();
            return raw.length() > 64 ? raw.substring(0, 64) + "..." : raw;
        }
        if (value instanceof Collection<?> || value.getClass().isArray()) {
            return "列表";
        }
        if (value instanceof Map<?, ?>) {
            return "映射";
        }
        if (value instanceof Enum<?> option) {
            return option.name();
        }
        return "不可编辑";
    }

    private static String formatNumber(double value) {
        return ValueCodec.formatNumber(value);
    }

    // ------------------------------------------------------------------ 骨架

    private boolean light() {
        return settings != null && settings.light();
    }

    private EfCanvas.Palette pal() {
        return Ef.palette(light()).withAlpha(NewClickGui.opacity());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init0() {
        super.init0();
        this.x = 0;
        this.y = 0;
        this.scroll = 0;
        // 重建控件会把屏幕上的输入框全清掉，手里那一格跟着作废
        editor.close();
        palettePanel.close();
    }

    @Override
    public void tick() {
        super.tick();
        editor.tick();
    }

    @Override
    protected void refreshWidgets() {
        super.refreshWidgets();
        winW = Math.min(this.width - 8, Math.max(280, Math.round(this.width * 0.78F)));
        winH = Math.min(this.height - 8, Math.max(200, Math.round(this.height * 0.78F)));
        winX = (this.width - winW) / 2;
        winY = (this.height - winH) / 2;
        barH = tabBar.measure(winW, light());

        EfWidget.of(0, 0, this.width, this.height, this::paint)
                .click(this::click)
                .drag(this::drag, () -> dragEntry != null)
                .scroll(this::wheel)
                .addTo(this);
    }

    // ------------------------------------------------------------------ 布局

    private int bodyTop() {
        return winY + barH;
    }

    /** 正文一直铺到页脚上沿：中间留空的那几像素没有任何绘制，会透出背后的世界。 */
    private int bodyHeight() {
        return winH - barH - FOOT_H;
    }

    private int textX() {
        return contentLeft + INDEX_W;
    }

    private int contentRight() {
        return contentLeft + contentWidth;
    }

    /** 控件的右边界：行尾留一列给重置按钮，控件不压到图标上。 */
    private int controlRight() {
        return contentRight() - RESET_W;
    }

    private int sliderWidth() {
        return Math.min(56, Math.max(28, contentWidth / 5));
    }

    /** 标签可用宽度：别让长名字压到滑轨上。 */
    private int labelWidth() {
        return Math.max(40, controlRight() - TEXT_W - 8 - sliderWidth() - 10 - textX());
    }

    /** 右端数值槽的左边：这一格既显示数值，点它也能就地手输。 */
    private int fieldX() {
        return controlRight() - TEXT_W;
    }

    /** 输入框的身份：同一条设置在任何一帧都是同一格。 */
    private static String fieldKey(Entry entry) {
        return "num:" + entry.name();
    }

    /** 字符串与颜色各占一份编辑身份：同一行同一用途的 key 不随帧变化。 */
    private static String textKey(Entry entry) {
        return "text:" + entry.name();
    }

    private static String colorKey(Entry entry) {
        return "color:" + entry.name();
    }

    private void buildRows() {
        rows.clear();
        contentLeft = winX + PAD;
        contentWidth = winW - PAD * 2;

        int y = bodyTop() - scroll;
        for (int i = 0; i < entries.size(); i++) {
            rows.add(new Row(i + 1, y, entries.get(i)));
            y += ROW_H;
        }
        totalHeight = (y + scroll) - bodyTop();
    }

    private int maxScroll() {
        return Math.max(0, totalHeight - bodyHeight() + 8);
    }

    // ------------------------------------------------------------------ 绘制

    private void paint(EfWidget self, VDrawContext ctx, int mouseX, int mouseY) {
        EfCanvas.Palette p = pal();
        buildRows();
        hovered = null;

        paintBar(ctx, p, mouseX, mouseY);
        paintBody(ctx, p, mouseX, mouseY);
        paintDropdown(ctx, p, mouseX, mouseY);
        paintFooter(ctx, p);
        paintFrame(ctx, p);
        paintPalette(ctx, p, mouseX, mouseY);
        paintTooltip(ctx, p, mouseX, mouseY);
    }

    private void paintBar(VDrawContext ctx, EfCanvas.Palette p, int mouseX, int mouseY) {
        // 命令栏就是整条分类条；Baritone 可不可用由正文那一句说明，不再占命令栏
        tabBar.paint(ctx, p, winX, winY, winW, "Baritone", light(), mouseX, mouseY);
    }

    private void paintBody(VDrawContext ctx, EfCanvas.Palette p, int mouseX, int mouseY) {
        int top = bodyTop();
        int height = bodyHeight();
        Ef.fill(ctx, winX, top, winW, height, p.paper());

        ctx.enableScissor(winX, top, winX + winW, top + height);
        try {
            if (!available) {
                paintNote(ctx, p, "这台机器上没有可用的 Baritone API，这一页的设置项读不出来。");
                return;
            }
            if (rows.isEmpty()) {
                paintNote(ctx, p, "Baritone 没有给出任何可以显示的设置项。");
                return;
            }
            for (Row row : rows) {
                if (row.y() + ROW_H <= top || row.y() >= top + height) {
                    continue;
                }
                paintRow(ctx, p, row, mouseX, mouseY);
            }
        } finally {
            ctx.disableScissor();
        }
        Ef.scrollbar(ctx, winX + winW - 4, top + 2, height - 6, scroll, totalHeight, height, p.rule(), p.ink2());
    }

    /** 空页与不可用都只写一句完整的话。 */
    private void paintNote(VDrawContext ctx, EfCanvas.Palette p, String text) {
        int y = bodyTop() + 8;
        for (String line : Ef.wrap(text, contentWidth)) {
            Ef.text(ctx, line, contentLeft, y, p.ink2());
            y += Ef.lineH() + 3;
        }
    }

    private void paintRow(VDrawContext ctx, EfCanvas.Palette p, Row row, int mouseX, int mouseY) {
        int y = row.y();
        boolean hover = mouseY >= y && mouseY < y + ROW_H && mouseX >= contentLeft - 4 && mouseX <= contentRight() + 4;
        if (hover && dropdown == null) {
            hovered = row.entry();
        }
        if (hover) {
            Ef.fill(ctx, contentLeft - 4, y, contentWidth + 8, ROW_H, Ef.mix(p.paper(), p.ink(), 0.04F));
            Ef.fill(ctx, contentLeft - 4, y, 3, ROW_H, p.trace());
        }

        // 行首序号是这一项在设置表里的真实位置
        String index = String.format(Locale.ROOT, "%03d", row.index());
        Ef.tracked(ctx, index, contentLeft, y + 5, p.ink2(), 1);
        Ef.text(ctx, Ef.ellipsis(label(row.entry()), labelWidth()), textX(), y + 5, p.ink());
        Ef.rule(ctx, contentLeft - 4, y + ROW_H - 1, contentWidth + 8, Ef.alpha(p.rule(), 0.55F));

        paintReset(ctx, p, row, mouseX, mouseY);

        Object value = valueOf(row.entry());
        if (value instanceof Boolean flag) {
            paintSwitch(ctx, p, y, flag);
        } else if (value instanceof Number number) {
            double current = number.doubleValue();
            double[] bounds = range(row.entry(), current, integral(value));
            paintSlider(ctx, p, row, mouseX, mouseY, formatNumber(current), current, bounds[0], bounds[1]);
        } else if (value instanceof Enum<?> option) {
            paintEnum(ctx, p, y, dropdown == row.entry(), option.name());
        } else if (value instanceof WrapColor color) {
            paintColor(ctx, p, row.entry(), y, color);
        } else if (value instanceof EntrySet<?> set) {
            paintEntrySet(ctx, p, y, set);
        } else if (value instanceof PrimitiveMap<?, ?> map) {
            paintPrimitiveMap(ctx, p, y, map);
        } else if (value instanceof CharSequence text) {
            paintText(ctx, p, row, text.toString(), mouseX, mouseY);
        } else {
            String shown = Ef.ellipsis(preview(value), Math.min(200, Math.max(80, contentWidth / 2)));
            Ef.text(ctx, shown, controlRight() - Ef.textWidth(shown), y + 5, p.ink2());
        }
    }

    /** 颜色：切角块里一块色块加它的十六进制值，色块开调色盘、十六进制开输入框。 */
    private void paintColor(VDrawContext ctx, EfCanvas.Palette p, Entry entry, int y, WrapColor color) {
        EndfieldWidgets.paintColorRow(
                ctx, p, controlRight(), y, ValueCodec.colorOf(color), editor.isOpenFor(colorKey(entry)), p.cut());
    }

    /** 名单：一个「N 项 ▾」切角块，点开宿主的注册表选择屏。 */
    private void paintEntrySet(VDrawContext ctx, EfCanvas.Palette p, int y, EntrySet<?> set) {
        EndfieldWidgets.paintCountChip(ctx, p, y, controlRight(), set.set().size() + " 项", p.cut(), false);
    }

    /** 映射：一个「N 项 ▾」切角块，点开宿主的方块替换表编辑器。 */
    private void paintPrimitiveMap(VDrawContext ctx, EfCanvas.Palette p, int y, PrimitiveMap<?, ?> map) {
        EndfieldWidgets.paintCountChip(ctx, p, y, controlRight(), map.toPrimitiveMap().size() + " 项", p.cut(), false);
    }

    /** 字符串：与数值槽同一套就地编辑，静态态画一条虚线底边说明这一格能点。 */
    private void paintText(VDrawContext ctx, EfCanvas.Palette p, Row row, String text, int mouseX, int mouseY) {
        int fx = fieldX();
        int fy = row.y() + FieldEditor.FIELD_TOP;
        boolean editing = editor.isOpenFor(fieldKey(row.entry()));
        boolean over = !editing && mouseX >= fx && mouseX < fx + TEXT_W
                && mouseY >= fy && mouseY < fy + FieldEditor.FIELD_H;
        EndfieldWidgets.paintValueSlot(ctx, p, fx, fy, TEXT_W, FieldEditor.FIELD_H, editing, over);
        if (editing) {
            editor.setTextColor(p.ink());
            return;
        }
        String shown = Ef.ellipsis(text, TEXT_W - 8);
        Ef.text(ctx, shown, fx + TEXT_W - 6 - Ef.textWidth(shown), row.y() + 5, p.ink());
    }

    /** 行尾重置：只有拿得到出厂值的项才画，点它写回 {@code defaultValue}。 */
    private void paintReset(VDrawContext ctx, EfCanvas.Palette p, Row row, int mouseX, int mouseY) {
        Object def = defaults.get(row.entry().name());
        if (def == null) {
            return;
        }
        int y = row.y();
        int iconX = contentRight() - 13;
        boolean hot = mouseX >= iconX - 2 && mouseX <= iconX + 12 && mouseY >= y && mouseY < y + 12;
        // 值跑出厂值的那一行，图标铺当前位置色：一眼能看出哪一行动过
        boolean modified = !Objects.equals(valueOf(row.entry()), def);
        if (modified) {
            Ef.cutFill(ctx, iconX - 1, y + 3, 12, 12, p.trace(), p.cut());
        } else if (hot) {
            Ef.cutFill(ctx, iconX - 1, y + 3, 12, 12, p.chip(), p.cut());
        }
        int fg = modified ? p.onTrace() : (hot ? p.ink() : p.ink2());
        Ef.icon(ctx, HostControls.resetIcon(), iconX + 1, y + 4, 9, fg);
    }

    /** 开关块：实心墨块 = 开，虚线框 = 关，点一下切一次。 */
    private void paintSwitch(VDrawContext ctx, EfCanvas.Palette p, int y, boolean on) {
        EndfieldWidgets.paintSwitch(ctx, p, y, ROW_H, controlRight(), on);
    }

    /** 数值滑杆：细线轨道 + 2px 手柄 + 右端数值槽。 */
    private void paintSlider(
            VDrawContext ctx, EfCanvas.Palette p, Row row, int mouseX, int mouseY, String text,
            double value, double min, double max) {
        int y = row.y();
        int right = controlRight();
        int width = sliderWidth();
        int left = right - TEXT_W - 8 - width;
        EndfieldWidgets.paintSliderTrack(ctx, p, left, y + ROW_H / 2, width, value, min, max);

        int fx = fieldX();
        int fy = y + FieldEditor.FIELD_TOP;
        if (editor.isOpenFor(fieldKey(row.entry()))) {
            // 输入框自己画字，这里只把框让出来
            EndfieldWidgets.paintFieldFrame(ctx, p, fx, fy, TEXT_W, FieldEditor.FIELD_H, true, false);
            editor.setTextColor(p.ink());
            return;
        }
        boolean over = mouseX >= fx && mouseX < fx + TEXT_W
                && mouseY >= fy && mouseY < fy + FieldEditor.FIELD_H;
        if (over) {
            EndfieldWidgets.paintFieldFrame(ctx, p, fx, fy, TEXT_W, FieldEditor.FIELD_H, false, true);
        }
        Ef.text(ctx, text, right - Ef.textWidth(text), y + 5, p.ink());
    }

    /** 枚举是一个切角小块，打开时用墨底反白。 */
    private void paintEnum(VDrawContext ctx, EfCanvas.Palette p, int y, boolean opened, String text) {
        String chip = Ef.ellipsis(text, Math.max(60, contentWidth / 3)) + "   ▾";
        int width = Ef.textWidth(chip);
        int left = controlRight() - width - 10;
        Ef.cutFill(ctx, left, y + 3, width + 10, 11, opened ? p.ink() : p.chip(), p.cut());
        Ef.text(ctx, chip, left + 5, y + 5, opened ? p.paper() : p.ink());
    }

    /** 下拉面板的上沿：放不下就翻到行上方，绘制与命中共用同一个判断。 */
    private int dropTop(int height) {
        int top = dropY;
        if (top + height > winY + winH - FOOT_H) {
            top = Math.max(winY + BAR_H, dropY + ROW_H - height);
        }
        return top;
    }

    private void paintDropdown(VDrawContext ctx, EfCanvas.Palette p, int mouseX, int mouseY) {
        if (dropdown == null) {
            return;
        }
        Object[] options = enumOptions(dropdown);
        if (options.length == 0) {
            dropdown = null;
            return;
        }
        int rowH = 13;
        int height = options.length * rowH + 4;
        int top = dropTop(height);
        Ef.fill(ctx, dropX, top, dropW, height, p.panel());
        Ef.cutOutline(ctx, dropX, top, dropW, height, p.ink(), p.cut());

        Object current = valueOf(dropdown);
        for (int i = 0; i < options.length; i++) {
            int oy = top + 2 + i * rowH;
            boolean hover = mouseX >= dropX && mouseX <= dropX + dropW && mouseY >= oy && mouseY < oy + rowH;
            boolean active = options[i] == current;
            if (hover) {
                Ef.fill(ctx, dropX + 1, oy, dropW - 2, rowH, Ef.mix(p.panel(), p.ink(), 0.08F));
            }
            if (active) {
                Ef.fill(ctx, dropX + 1, oy, 2, rowH, p.trace());
            }
            Ef.text(ctx, Ef.ellipsis(enumLabel(options[i]), dropW - 14), dropX + 8, oy + 3, active ? p.ink() : p.ink2());
        }
    }

    private Object[] enumOptions(Entry entry) {
        Object value = valueOf(entry);
        return value instanceof Enum<?> option ? option.getDeclaringClass().getEnumConstants() : new Object[0];
    }

    private static String enumLabel(Object option) {
        return option instanceof Enum<?> value ? value.name() : String.valueOf(option);
    }

    private void paintFooter(VDrawContext ctx, EfCanvas.Palette p) {
        String left = available ? entries.size() + " 项设置  │  已开启 " + enabledCount() + " 项" : "Baritone 不可用";
        String hint = null;
        if (available) {
            hint = "点开关切换 │ 拖滑杆改数值 │ 点数字手输 │ 右键重置";
            int room = winW - 16 - Ef.textWidth(left);
            if (Ef.textWidth(hint) > room) {
                // 窄窗先省掉"点开关切换"：右侧提示压到左侧统计上比少一句提示更糟
                hint = "拖滑杆 │ 数字手输 │ 右键重置";
            }
        }
        EndfieldWidgets.paintFooterBar(ctx, p, winX, winY, winW, winH, FOOT_H, left, hint);
    }

    /** 页脚统计：真的去读，不按「应该开着」估算。 */
    private int enabledCount() {
        int count = 0;
        for (Entry entry : entries) {
            if (Boolean.TRUE.equals(valueOf(entry))) {
                count++;
            }
        }
        return count;
    }

    private void paintFrame(VDrawContext ctx, EfCanvas.Palette p) {
        EndfieldWidgets.paintWindowFrame(ctx, p, winX, winY, winW, winH);
    }

    private void paintPalette(VDrawContext ctx, EfCanvas.Palette p, int mouseX, int mouseY) {
        if (!(palettePanel.target() instanceof Entry target)) {
            return;
        }
        if (palettePanel.paint(ctx, p, winX, winY, winW, winH, ROW_H,
                ValueCodec.colorOf(valueOf(target)), mouseX, mouseY)) {
            hovered = null;
        }
    }

    private void paintTooltip(VDrawContext ctx, EfCanvas.Palette p, int mouseX, int mouseY) {
        if (hovered == null) {
            return;
        }
        List<String> lines = tipLines(hovered);
        if (lines.isEmpty()) {
            return;
        }
        Ef.tooltip(ctx, p, lines, mouseX, mouseY, this.width, this.height);
    }

    // ------------------------------------------------------------------ 交互

    private boolean click(EfWidget self, double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        // 输入框是原版控件：落在它上面的这一下要还给输入框，否则光标都挪不动
        if (editor.contains(mouseX, mouseY)) {
            return false;
        }
        // 点到别处就收起：输入框挂在旧行坐标上会盖到别的设置
        editor.close();
        // 上一次拖动在这里收尾：重新点击就重新判定
        dragEntry = null;
        buildRows();

        if (dropdown != null) {
            Object[] options = enumOptions(dropdown);
            int rowH = 13;
            int top = dropTop(options.length * rowH + 4);
            if (mx >= dropX && mx <= dropX + dropW && my >= top && my <= top + options.length * rowH + 4) {
                int index = (my - top - 2) / rowH;
                if (index >= 0 && index < options.length) {
                    write(dropdown, options[index]);
                }
            }
            dropdown = null;
            return true;
        }

        if (palettePanel.target() instanceof Entry target) {
            if (palettePanel.click(winX, winY, winW, winH, ROW_H, ValueCodec.colorOf(valueOf(target)),
                    mx, my, button, rgb -> write(target, ValueCodec.withRgb(valueOf(target), rgb)))) {
                return true;
            }
            palettePanel.close();
            if (my < winY || my >= winY + barH) {
                return true;
            }
        }

        if (my >= winY && my < winY + barH) {
            String hit = tabBar.click(winX, winY, winW, "Baritone", light(), mx, my);
            if (EndfieldTabBar.THEME.equals(hit)) {
                if (settings != null) {
                    settings.light(!settings.light());
                }
                return true;
            }
            if (hit != null && !"Baritone".equals(hit)) {
                GuiStyles.switchTab(this, hit, settings);
                return true;
            }
            return false;
        }

        if (my < bodyTop() || my > bodyTop() + bodyHeight()) {
            return false;
        }
        for (Row row : rows) {
            if (my < row.y() || my >= row.y() + ROW_H) {
                continue;
            }
            return hit(row, mx, my, button);
        }
        return false;
    }

    private boolean hit(Row row, int mouseX, int mouseY, int button) {
        Entry entry = row.entry();
        if (defaults.containsKey(entry.name())) {
            // 行尾图标与右键都能恢复默认：图标是看得见的入口，右键是配置页一直有的手势
            boolean icon = button == 0
                    && mouseX >= contentRight() - 15
                    && mouseX <= contentRight() - 1
                    && mouseY >= row.y()
                    && mouseY < row.y() + 12;
            if (icon || button == 1) {
                write(entry, defaults.get(entry.name()));
                return true;
            }
        }
        Object value = valueOf(entry);
        if (value instanceof Boolean flag) {
            if (mouseX >= controlRight() - 24) {
                write(entry, !flag);
                return true;
            }
            return false;
        }
        if (value instanceof Number number) {
            double current = number.doubleValue();
            double[] bounds = range(entry, current, integral(value));
            int fx = fieldX();
            // 数值槽与滑轨的命中区不重叠：槽从右端 TEXT_W 起，滑轨到它左边 8px 为止
            if (button == 0 && mouseX >= fx && mouseX <= fx + TEXT_W) {
                openField(row, entry, value, current);
                return true;
            }
            int width = sliderWidth();
            int left = controlRight() - TEXT_W - 8 - width;
            if (button == 0 && mouseX >= left - 3 && mouseX <= left + width + 3) {
                dragEntry = entry;
                dragTrackX = left;
                dragTrackW = width;
                dragMin = bounds[0];
                dragMax = bounds[1];
                applyRatio(entry, value, (mouseX - left) / (double) width);
                return true;
            }
            return false;
        }
        if (value instanceof Enum<?>) {
            if (button == 0) {
                dropdown = entry;
                dropW = Math.min(180, Math.max(120, contentWidth / 3));
                dropX = controlRight() - dropW;
                dropY = row.y() + ROW_H;
                return true;
            }
            return false;
        }
        if (value instanceof WrapColor color) {
            return hitColor(row, entry, color, mouseX, mouseY, button);
        }
        if (value instanceof EntrySet<?> set) {
            String label = set.set().size() + " 项   ▾";
            int width = Ef.textWidth(label) + 10;
            if (button == 0 && mouseX >= controlRight() - width && mouseX <= controlRight()
                    && mouseY >= row.y() + 3 && mouseY < row.y() + 15) {
                openEntrySet(entry, set);
                return true;
            }
            return false;
        }
        if (value instanceof PrimitiveMap<?, ?> map) {
            String label = map.toPrimitiveMap().size() + " 项   ▾";
            int width = Ef.textWidth(label) + 10;
            if (button == 0 && mouseX >= controlRight() - width && mouseX <= controlRight()
                    && mouseY >= row.y() + 3 && mouseY < row.y() + 15) {
                openPrimitiveMap(entry);
                return true;
            }
            return false;
        }
        if (value instanceof CharSequence text) {
            int fx = fieldX();
            if (button == 0 && mouseX >= fx && mouseX <= fx + TEXT_W) {
                openTextField(row, entry, text.toString());
                return true;
            }
            return false;
        }
        return false;
    }

    /**
     * 字符串就地编辑：非即时模式，Enter 或点到别处才交一次。
     *
     * <p>Baritone 的字符串设置（{@code prefix}、{@code schematicFallbackExtension}）改动会立刻影响
     * 它自己的行为，逐字写回等于每敲一个字母就重配一次。
     */
    private void openTextField(Row row, Entry entry, String current) {
        editor.open(this, textKey(entry), fieldX(), row.y() + FieldEditor.FIELD_TOP, TEXT_W, current, false,
                text -> write(entry, text));
    }

    /**
     * 颜色命中：色块开调色盘，十六进制槽按 {@link EndfieldWidgets#colorGeometry} 开就地输入。
     */
    private boolean hitColor(Row row, Entry entry, WrapColor current, int mouseX, int mouseY, int button) {
        if (button != 0 || mouseY < row.y() + FieldEditor.FIELD_TOP
                || mouseY >= row.y() + FieldEditor.FIELD_TOP + FieldEditor.FIELD_H) {
            return false;
        }
        EndfieldWidgets.ColorGeometry box = EndfieldWidgets.colorGeometry(controlRight());
        if (mouseX >= box.chipX() && mouseX < box.chipX() + box.swatchHitW()) {
            palettePanel.open(entry, box.swatchX(), box.swatchW(), row.y());
            return true;
        }
        if (mouseX >= box.hexFieldX() && mouseX < box.hexFieldX() + box.hexFieldW()) {
            palettePanel.close();
            editor.open(this, colorKey(entry), box.hexFieldX(), row.y() + FieldEditor.FIELD_TOP, box.hexFieldW(),
                    ValueCodec.hexOf(ValueCodec.colorOf(current)), true, 16, text -> {
                        WrapColor parsed = ValueCodec.tryParseWrapColor(text);
                        if (parsed != null) {
                            write(entry, parsed);
                        }
                    });
            return true;
        }
        return false;
    }

    /** 名单：打开宿主的注册表选择屏。 */
    private void openEntrySet(Entry entry, EntrySet<?> set) {
        HostEditors.openEntrySet(
                set, next -> write(entry, next), NewClickGui.popupFactory(settings), "Baritone 名单选择屏打开失败");
    }

    /** 映射：打开宿主的方块替换表编辑器（单次点击直接进入列表编辑界面）。 */
    private void openPrimitiveMap(Entry entry) {
        HostEditors.openPrimitiveMapAccessor(
                entry.accessor(), entry.name(), NewClickGui.popupFactory(settings), "Baritone 替换表打开失败");
    }

    /**
     * 点数值槽：就地手输，逐字写回；"-"、"." 这类半截输入解析不了就保持原值。
     *
     * <p>写回类型取按下那一刻的运行时类型，经 {@link #coerce} 还原：Baritone 的 Setting
     * 按泛型取值，类型不对下一次取值就会抛异常。
     */
    private void openField(Row row, Entry entry, Object current, double value) {
        editor.open(
                this,
                fieldKey(entry),
                fieldX(),
                row.y() + FieldEditor.FIELD_TOP,
                TEXT_W,
                formatNumber(value),
                true,
                text -> {
                    double next;
                    try {
                        next = Double.parseDouble(text.trim().replace(',', '.'));
                    } catch (NumberFormatException ignored) {
                        return;
                    }
                    // "NaN"/"Infinity" 也解析得出来，但写进设置就是坏值
                    if (!Double.isFinite(next)) {
                        return;
                    }
                    Object coerced = coerce(current, next);
                    if (coerced != null) {
                        write(entry, coerced);
                    }
                });
    }

    private void applyRatio(Entry entry, Object current, double ratio) {
        double clamped = Math.max(0.0D, Math.min(1.0D, ratio));
        double next = dragMin + clamped * (dragMax - dragMin);
        Object value = coerce(current, next);
        if (value != null) {
            write(entry, value);
        }
    }

    /** 拖动中：轨道矩形与区间按按下那一刻记下的算。 */
    private boolean drag(EfWidget self, double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragEntry == null) {
            return false;
        }
        applyRatio(dragEntry, valueOf(dragEntry), (mouseX - dragTrackX) / (double) dragTrackW);
        return true;
    }

    private boolean wheel(EfWidget self, double mouseX, double mouseY, double amount) {
        if (mouseY < bodyTop() || mouseY > bodyTop() + bodyHeight()) {
            return false;
        }
        // 滚动会挪动行，下拉、调色盘与输入框跟着旧坐标走就会错位，直接收起来
        dropdown = null;
        palettePanel.close();
        editor.close();
        buildRows();
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) (Math.signum(amount) * 18)));
        return true;
    }
}