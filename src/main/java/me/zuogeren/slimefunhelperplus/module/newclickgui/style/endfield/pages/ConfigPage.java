package me.zuogeren.slimefunhelperplus.module.newclickgui.style.endfield.pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import me.matl114.gui.GenericScreen;
import me.matl114.hacks.api.BaseModule.WrapperConfigRef;
import me.matl114.managers.config.Config;
import me.matl114.managers.config.ConfigEnum;
import me.matl114.managers.config.DoubleRef;
import me.matl114.managers.config.EnumRef;
import me.matl114.managers.config.FlagRef;
import me.matl114.managers.config.FloatRef;
import me.matl114.managers.config.IntRef;
import me.matl114.managers.config.KeyBindRef;
import me.matl114.managers.config.LongRef;
import me.matl114.managers.config.MapRef;
import me.matl114.managers.config.Ref;
import me.matl114.managers.config.StringRef;
import me.matl114.managers.input.KeyCode;
import me.matl114.managers.input.MultiKeyBind;
import me.matl114.hacks.utils.config.EntrySet;
import me.matl114.hacks.utils.config.Holder;
import me.matl114.hacks.utils.config.ItemStackTemplate;
import me.matl114.hacks.utils.config.OptionalPrimitive;
import me.matl114.hacks.utils.config.PrimitiveList;
import me.matl114.hacks.utils.config.PrimitiveMap;
import me.matl114.hacks.utils.config.RegexList;
import me.matl114.hacks.utils.config.BoundedPrimitiveMap;
import me.matl114.hacks.utils.config.EntityTypeEntrySet;
import me.matl114.hacks.utils.config.EnumPrimitiveList;
import me.matl114.hacks.utils.config.GuiSetting;
import me.matl114.hacks.utils.config.LabelVec2;
import me.matl114.hacks.utils.config.LabelVec3;
import me.matl114.hacks.utils.config.PrimitivePairList;
import me.matl114.hacks.utils.config.RegistryRegex;
import me.matl114.hacks.utils.config.StringEnum;
import me.matl114.hacks.utils.config.Vec2;
import me.matl114.hacks.utils.config.Vec3;
import me.matl114.hacks.utils.config.WeakHolder;
import me.matl114.hacks.utils.config.WeakRegistryRegex;
import me.matl114.hacks.utils.config.WidgetPos;
import me.matl114.managers.config.ListRef;
import me.matl114.hacks.utils.config.Regex;
import me.matl114.hacks.utils.config.StringFormat;
import me.matl114.hacks.utils.config.WrapColor;
import me.matl114.hacks.utils.config.WrapEnum;
import me.matl114.hacks.utils.config.WrapRGBA;
import me.zuogeren.slimefunhelperplus.hostgui.HostControls;
import me.zuogeren.slimefunhelperplus.hostgui.HostEditors;
import me.zuogeren.slimefunhelperplus.hostgui.HostScreens;
import me.zuogeren.slimefunhelperplus.hostgui.StringChoicesRegistry;
import me.matl114.versioned.api.VDrawContext;
import me.zuogeren.slimefunhelperplus.module.newclickgui.NewClickGui;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

/**
 * 「全配置」页：把宿主每一份配置文件的可见设置摊成一张总表。
 *
 * <p>数据不是 {@code WrapperConfigRef}（那是模块的视角），而是 {@link Config#getVisiblePaths()}：
 * 一条可见路径就一定能通过 {@link Config#get(String...)} 解析回 {@link Ref}，
 * 和宿主 {@code ConfigMultiLevelList} 走的是同一条解析路。
 * 解析不出来的路径仍要列出来，只是画成只读行——总表的价值一半在"这里有什么"，不是"这里能改什么"。
 *
 * <p>可写的有四类：开关翻转、数值就地输入（± 只是步进）、枚举下拉、文本就地编辑；右键恢复默认。
 * 都走 {@link Ref} 自己的写入口，所以校验器、更新监听、脏标记全部沿用宿主那一套，
 * 不在这里复制一份配置语义。
 */
public class ConfigPage extends GenericScreen {
    private static final int BAR_H = 22;

    /** 顶部分类条：切分类在同一屏里完成。 */
    private final EndfieldTabBar tabBar = new EndfieldTabBar();

    private int barH = BAR_H;
    private static final int SEARCH_H = 22;
    private static final int FOOT_H = 14;
    private static final int PAD = 12;
    private static final int HEAD_H = 30;
    private static final int SECTION_H = 22;
    private static final int ROW_H = 18;
    private static final int RAIL_CHIP_H = 15;
    private static final int RAIL_STEP = 19;
    /** 数值行两侧加减键的边长。 */
    private static final int BTN = 13;
    /** 值槽（就地编辑的那一格）的宽度：右沿正好让到减号左边，与 ± 两键不重叠。 */
    private static final int VALUE_W = 56;
    /** 枚举下拉里一行候选的高度与最大可见行数。 */
    private static final int DROP_ROW_H = 13;
    private static final int DROP_MAX_ROWS = 12;
    private static final int ARROW_W = 14;
    private static final int OPTIONAL_RECORD_BASE = 100;

    /** GUI 设置入口块上的字：与模块配置页同一个词。 */
    private static final String GUI_SETTING_CHIP = "详细设置";
    private static final String TITLE = "全配置";
    private static final String LATIN = "CONFIG";

    private final Settings settings;

    /** 配置根清单：每份配置文件一项，顺序沿用注册表。 */
    private final List<Config> roots = new ArrayList<>();

    /** 每份配置解析出来的条目：Ref 是活的，值每帧现读，只有路径与 Ref 关系是缓存。 */
    private final Map<Config, List<Item>> itemCache = new IdentityHashMap<>();

    private Config selected;
    private String query = "";
    private int railScroll;
    private int itemScroll;

    /** 搜索结果的缓存：拼音转换不便宜，同一个词每帧对全表重算会拖慢界面。 */
    private Config shownConfig;
    private String shownQuery = "";
    private List<Item> shownCache;

    /** 原版输入框，只负责收字与画字；框体由 paintSearch 画。 */
    private EditBox searchBox;

    /** 行内编辑：同一时刻只有一格在改，原版输入框由它在 tick 里增删。 */
    private final FieldEditor editor = new FieldEditor();

    /** 打开中的枚举下拉：记的是 key，列表重建后同一条路径仍是同一下拉。 */
    private String dropdownKey;

    /** 下拉盖住的是记录里的第几格；不是记录型时为 -1。 */
    private int dropdownPart = -1;
    private int dropScroll;

    /** 打开中的调色盘浮层（WrapColor / WrapRGBA / 可选 TextColor 共用）。 */
    private final EndfieldColorPalette palettePanel = new EndfieldColorPalette();

    /** 行内辅助按钮悬停信息（每帧重算，优先于普通项说明）。 */
    private StringFormat formatArgumentHover;
    private StringFormat editorHover;
    private boolean itemTemplateHover;
    private String regexPreviewHover;

    /** 正在等按键的那一条：记路径，列表重建后仍是同一项。 */
    private String keyTarget;

    private int winX;
    private int winY;
    private int winW;
    private int winH;

    /** 左侧配置根那一列的宽度。 */
    private int railW;

    /** 这一帧布局出来的行，绘制与命中共用；坐标为屏幕绝对坐标。 */
    private final List<Line> lines = new ArrayList<>();

    /** 内容总高，用来算滚动范围。 */
    private int listHeight;

    /** 这一帧鼠标停在哪一条设置上，悬停说明框用。 */
    private Item hovered;

    /** 一条可见路径：{@code ref} 可能为 null（路径解析不出 Ref，只能只读展示）。 */
    private record Item(String path, String section, Ref<?> ref) {}

    /** 分节标题行（item 为 null）或一条设置行。 */
    private record Line(int y, int height, String section, Item item) {}

    /** 一块矩形：值槽与下拉面板共用，绘制与命中取同一份几何。 */
    private record Rect(int x, int y, int w, int h) {}

    public ConfigPage(Settings settings) {
        super(Component.empty(), 0, 0);
        this.settings = settings;
    }

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

    // ------------------------------------------------------------------ 生命周期

    @Override
    protected void init0() {
        super.init0();
        this.x = 0;
        this.y = 0;
        this.railScroll = 0;
        this.itemScroll = 0;
        // 行要按新尺寸重排：贴行的输入框与下拉都按旧坐标算，先收起来（文本顺手提交）
        editor.close();
        dropdownKey = null;
        dropdownPart = -1;
        dropScroll = 0;
        palettePanel.close();
        this.roots.clear();
        this.itemCache.clear();
        for (Config config : Config.REGISTRY.stream().toList()) {
            if (config != null) {
                roots.add(config);
            }
        }
        // 配置可能被插件晚一步注册，选中的根失效时要重新落一个
        if (selected == null || !roots.contains(selected)) {
            selected = roots.isEmpty() ? null : roots.get(0);
        }
    }

    @Override
    protected void refreshWidgets() {
        super.refreshWidgets();
        winW = Math.min(this.width - 8, Math.max(320, Math.round(this.width * 0.78F)));
        winH = Math.min(this.height - 8, Math.max(220, Math.round(this.height * 0.80F)));
        winX = (this.width - winW) / 2;
        winY = (this.height - winH) / 2;
        barH = tabBar.measure(winW, light());
        railW = Math.max(96, Math.min(160, winW / 4));

        EfWidget.of(0, 0, this.width, this.height, this::paint)
                .click(this::click)
                .scroll(this::wheel)
                .keys(this::captureKey)
                .addTo(this);

        // 搜索框交给原版 EditBox：输入法、退格、粘贴都由 MC 自己处理
        int boxX = winX + 8;
        int prefixW = Ef.trackedWidth("//", 1) + 4;
        EditBox box = HostControls.editBox(
                boxX + 4 + prefixW,
                searchTop() + 6,
                Math.max(24, searchWidth() - 8 - prefixW),
                48,
                "搜索配置项");
        HostControls.setValueQuietly(box, query, text -> {
            query = text;
            itemScroll = 0;
        });
        this.searchBox = box;
        this.addRenderableWidget(box);
    }

    /**
     * 输入框是原版控件，增删不能发生在点击与渲染的遍历里，
     * 所以统一放在这个每 tick 调一次的位置。
     */
    @Override
    public void tick() {
        super.tick();
        editor.tick();
    }

    // ------------------------------------------------------------------ 几何

    private int searchTop() {
        return winY + barH;
    }

    private int bodyTop() {
        return searchTop() + SEARCH_H;
    }

    private int bodyBottom() {
        return winY + winH - FOOT_H;
    }

    private int bodyHeight() {
        return Math.max(1, bodyBottom() - bodyTop());
    }

    private int railRowsTop() {
        return bodyTop() + 34;
    }

    private int itemsX() {
        return winX + railW + PAD;
    }

    private int itemsRight() {
        return winX + winW - PAD;
    }

    private int itemsWidth() {
        return Math.max(40, itemsRight() - itemsX());
    }

    /** 设置列表的起点：标题块在它上面。 */
    private int listTop() {
        return bodyTop() + 6 + HEAD_H;
    }

    private int listViewH() {
        return Math.max(1, bodyBottom() - listTop());
    }

    private int searchWidth() {
        return Math.min(240, Math.max(120, winW / 3));
    }

    // ------------------------------------------------------------------ 数据

    /** 一份配置的可见设置项：按 {@code getVisiblePaths()} 的顺序，路径与 Ref 解析一次就缓存。 */
    private List<Item> itemsOf(Config config) {
        if (config == null) {
            return List.of();
        }
        List<Item> cached = itemCache.get(config);
        if (cached != null) {
            return cached;
        }
        List<Item> out = new ArrayList<>();
        for (String path : config.getVisiblePaths()) {
            if (path == null || path.isBlank()) {
                continue;
            }
            String[] cut = Config.cutToPath(path);
            if (cut.length == 0 || cut[cut.length - 1].isBlank()) {
                continue;
            }
            Ref<?> ref = config.get(cut);
            // 分组节点（MapRef）自身不是一条可编辑的设置，它的子项各自在可见路径里
            if (ref instanceof MapRef) {
                continue;
            }
            String section = cut.length > 1 ? String.join(".", Arrays.copyOf(cut, cut.length - 1)) : "";
            out.add(new Item(path, section, ref));
        }
        itemCache.put(config, out);
        return out;
    }

    private List<Item> all() {
        return itemsOf(selected);
    }

    /**
     * 搜索后的清单：路径按原样匹配，中文名交给宿主的 {@link FilterService#nameMatch}，
     * 所以能用全拼与首字母搜（「pengyou」也能命中「朋友的酒」）。
     *
     * <p>结果按「配置文件 + 搜索词」缓存：拼音转换不便宜，全配置页有近千条，
     * 每帧重算会把界面拖慢，只有换了配置文件或搜索词才重算。
     */
    private List<Item> shown() {
        List<Item> items = all();
        String q = query.trim();
        if (q.isEmpty()) {
            return items;
        }
        if (shownCache != null && shownConfig == selected && shownQuery.equals(q)) {
            return shownCache;
        }
        String needle = q.toLowerCase(Locale.ROOT);
        List<Item> out = new ArrayList<>();
        for (Item item : items) {
            if (item.path().toLowerCase(Locale.ROOT).contains(needle)
                    || HostControls.nameMatch(labelOf(item), needle)) {
                out.add(item);
            }
        }
        shownConfig = selected;
        shownQuery = q;
        shownCache = out;
        return out;
    }

    private static int sectionsOf(List<Item> items) {
        Set<String> seen = new LinkedHashSet<>();
        for (Item item : items) {
            seen.add(item.section());
        }
        return seen.size();
    }

    private static String labelOf(Item item) {
        if (item.ref() == null) {
            // 解析不出 Ref 的路径连翻译都没有意义，直接报路径
            return item.path();
        }
        return Component.translatableWithFallback(item.path(), leaf(item.path())).getString();
    }

    private static String sectionTitle(String section) {
        return ValueCodec.sectionTitle(section);
    }

    private static String rootName(Config config) {
        if (config == null) {
            return "—";
        }
        String name = config.getConfigName();
        String key = config.getTranslationKey();
        return Component.translatableWithFallback(key == null ? name : key, name).getString();
    }

    /** 配置根的拉丁标识：注册表里的路径，没有注册就退回配置名。 */
    private static String codeOf(Config config) {
        if (config == null) {
            return "";
        }
        if (config.getRegistryKey() != null) {
            return config
                    .getRegistryKey()
                    .identifier()
                    .getPath()
                    .replace('-', ' ')
                    .replace('_', ' ')
                    .toUpperCase(Locale.ROOT);
        }
        return config.getConfigName().toUpperCase(Locale.ROOT);
    }

    private static String leaf(String path) {
        return ValueCodec.leaf(path);
    }

    // ------------------------------------------------------------------ 值

    private static boolean isNumeric(Ref<?> ref) {
        return ValueCodec.isNumericRef(ref);
    }

    /** 这一页真正给了控件的类型；其余只显示当前值。 */
    private static boolean writable(Ref<?> ref) {
        Object val = ref.getValue();
        return ref instanceof FlagRef
                || isNumeric(ref)
                || ref instanceof EnumRef<?>
                || ref instanceof StringRef
                || ref instanceof KeyBindRef
                || val instanceof WrapColor
                || val instanceof WrapRGBA
                || val instanceof Regex
                || val instanceof RegistryRegex<?>
                || val instanceof WeakRegistryRegex<?>
                || val instanceof StringFormat
                || val instanceof OptionalPrimitive<?>
                || val instanceof EntrySet<?>
                || ref instanceof ListRef
                || val instanceof RegexList
                || val instanceof PrimitiveList<?>
                || val instanceof PrimitivePairList<?, ?>
                || val instanceof PrimitiveMap<?, ?>
                || val instanceof EnumPrimitiveList<?, ?>
                || val instanceof ItemStackTemplate
                || val instanceof Holder<?>
                || val instanceof WeakHolder<?>
                || val instanceof StringEnum
                || val instanceof WidgetPos
                || val instanceof GuiSetting
                || val instanceof EntityTypeEntrySet
                || val instanceof BoundedPrimitiveMap<?, ?>
                || val instanceof LabelVec2
                || val instanceof LabelVec3
                || booleanParts(val) != null
                || recordParts(val) != null;
    }

    /** 值框里写一段文本的那几类：注册表条目写「注册表|条目」。 */
    private static boolean textValueRow(Ref<?> ref) {
        return ref.getValue() instanceof Holder<?> || ref.getValue() instanceof WeakHolder<?>;
    }

    /** 值框里的原文：与宿主存盘格式一致，写回时再解析。 */
    private static String textValueRowOf(Ref<?> ref) {
        return ValueCodec.editableTextOf(ref.getValue());
    }

    /** 按 Ref 找回它的配置路径：计数块的编辑屏要用它当键名。 */
    private String itemPath(Ref<?> ref) {
        for (Line line : lines) {
            if (line.item() != null && line.item().ref() == ref) {
                return line.item().path();
            }
        }
        return "";
    }

    /** 计数块那几类：值本身是一串条目或映射，点开交给宿主的编辑屏。 */
    private static boolean countRow(Ref<?> ref) {
        Object val = ref.getValue();
        return ref instanceof ListRef
                || val instanceof RegexList
                || val instanceof PrimitiveList<?>
                || val instanceof PrimitivePairList<?, ?>
                || val instanceof PrimitiveMap<?, ?>
                || val instanceof EnumPrimitiveList<?, ?>
                || val instanceof BoundedPrimitiveMap<?, ?>;
    }

    /** 计数块上的文字：条目数。 */
    private static int countOf(Ref<?> ref) {
        int count = ValueCodec.listCountOf(ref, ref.getValue());
        return Math.max(0, count);
    }

    private static Object[] recordParts(Object value) {
        return RecordFieldOps.recordParts(value);
    }

    private static int rowParts(Ref<?> ref) {
        return ref == null ? 1 : RecordFieldOps.rowParts(ref.getValue());
    }

    private static boolean[] booleanParts(Object value) {
        return RecordFieldOps.booleanParts(value);
    }

    private static String[] componentNames(Object value) {
        return RecordFieldOps.componentNames(value);
    }

    private static Object withPart(Object value, int index, Object next) {
        return RecordFieldOps.withPart(value, index, next);
    }

    private static Object withNumericPart(Object value, int index, double next) {
        return RecordFieldOps.withNumericPart(value, index, next);
    }

    /** 可选值：行尾一个开关决定这一项是否生效，左边是内层值自己的控件。 */
    private static boolean optionalRow(Ref<?> ref) {
        return ref.getValue() instanceof OptionalPrimitive<?>;
    }

    /** 可选值的内层：没启用或取不到时返回 null。 */
    private static Object optionalInner(Ref<?> ref) {
        return RecordFieldOps.optionalPresentInner(ref.getValue());
    }

    /** 就地文本编辑的那几类：值本身就是一段可读写的字符串，不需要另开编辑器。 */
    private static boolean textEditable(Ref<?> ref) {
        return ref instanceof StringRef
                || ref.getValue() instanceof Regex
                || ref.getValue() instanceof StringFormat;
    }

    /** 文本类配置的原文：显示会截断，回填与写回都必须拿完整值。 */
    private static String editableTextOf(Ref<?> ref) {
        return ValueCodec.editableTextOf(ref.getValue());
    }

    private static double numericOf(Ref<?> ref) {
        return ValueCodec.numericOf(ref.getValue());
    }

    private static double stepOf(Ref<?> ref) {
        return ValueCodec.stepOf(ref);
    }

    private static void setNumber(Ref<?> ref, double value) {
        ValueCodec.setNumeric(ref, value);
    }

    private static void nudge(Ref<?> ref, double delta) {
        setNumber(ref, numericOf(ref) + delta);
    }

    private static void commitNumber(Ref<?> ref, String text) {
        Double value = ValueCodec.tryParseNumber(text);
        if (value != null) {
            setNumber(ref, value);
        }
    }

    private static void commitText(Ref<?> ref, String text) {
        ValueCodec.commitText(ref, text);
    }

    private static void setOptionalPresent(Ref<?> ref, boolean present) {
        RecordFieldOps.setOptionalPresent(ref, present);
    }

    private static void writeOptionalNumber(Ref<?> ref, double value) {
        RecordFieldOps.writeOptionalNumber(ref, value);
    }

    private static void writeOptionalOption(Ref<?> ref, Object option) {
        RecordFieldOps.writeOptionalOption(ref, option);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void commitColor(Ref<?> ref, String text) {
        TextColor nextColor = ValueCodec.tryParseTextColor(text);
        if (nextColor == null) {
            return;
        }
        Object current = ref.getValue();
        if (current instanceof WrapColor) {
            ((Ref) ref).setValue(new WrapColor(nextColor));
        } else if (current instanceof WrapRGBA rgba) {
            ((Ref) ref).setValue(rgba.withTextColor(nextColor));
        } else if (current instanceof OptionalPrimitive<?> opt && opt.getValue() instanceof TextColor) {
            RecordFieldOps.writeOptionalTextColor(ref, nextColor.getValue());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void commitAlpha(Ref<?> ref, String text) {
        Integer alpha = ValueCodec.tryParseAlpha(text);
        if (alpha != null && ref.getValue() instanceof WrapRGBA rgba) {
            ((Ref) ref).setValue(rgba.withAlpha(alpha));
        }
    }

    /** 文本行的原文：显示会截断，回填与写回都必须拿完整值。 */
    private static String textValueOf(Ref<?> ref) {
        try {
            Object value = ref.getValue();
            return value == null ? "" : String.valueOf(value);
        } catch (Throwable e) {
            return "";
        }
    }

    private static String enumLabel(Object value) {
        return ValueCodec.enumLabel(value);
    }

    /** 枚举取值可能没注册完成，读不到就画一个占位，别让整页因为一格坏掉。 */
    private static String enumText(Ref<?> ref) {
        try {
            Object value = ref.getValue();
            if (value instanceof StringEnum choice) {
                return choice.value();
            }
            return value == null ? "—" : enumLabel(value);
        } catch (Throwable e) {
            return "—";
        }
    }

    /**
     * 枚举的候选：取当前取值的 {@link ConfigEnum#getMap()}，也就是它的声明顺序。
     * 类型还没注册完成时取 map 会抛，这里兜住并退回空表（芯片保持只读）。
     */
    private static List<ConfigEnum> enumOptions(Ref<?> ref) {
        return enumOptions(ref, -1);
    }

    /**
     * 枚举的候选：{@code part} 为记录分量下标，{@code -1} 表示整值。
     *
     * <p>记录里的枚举（{@code PredictArgument} 的 {@code mode}）要取那一格自己的枚举类，
     * 拿整条记录去问 {@code getMap()} 只会得到空表——表现就是「点了没反应」。
     */
    private static List<ConfigEnum> enumOptions(Ref<?> ref, int part) {
        try {
            Object value = ref.getValue();
            if (value instanceof StringEnum) {
                // 有限字符串候选不是 ConfigEnum：候选走另一条路，见 stringEnumOptions
                return List.of();
            }
            if (part >= OPTIONAL_RECORD_BASE) {
                Object inner = optionalInner(ref);
                Object[] parts = recordParts(inner);
                int index = part - OPTIONAL_RECORD_BASE;
                value = parts != null && index < parts.length ? parts[index] : null;
            } else if (part >= 0) {
                Object[] parts = recordParts(value);
                value = parts != null && part < parts.length ? parts[part] : null;
            } else if (value instanceof OptionalPrimitive<?>) {
                // 可选值：候选取内层枚举，不是外层那个 OptionalPrimitive
                value = optionalInner(ref);
            }
            if (value instanceof ConfigEnum configEnum) {
                Map<String, ConfigEnum> map = configEnum.getMap();
                if (map != null) {
                    return new ArrayList<>(map.values());
                }
            }
        } catch (Throwable e) {
            // 注册未完成：这一次没有可选项
        }
        return List.of();
    }

    /** 有限字符串候选：值就是候选本身，不需要再包一层枚举。 */
    private static List<String> stringEnumOptions(Ref<?> ref) {
        Object value = ref.getValue();
        return value instanceof StringEnum choice ? choice.values() : List.of();
    }

    /** 当前取值：只用来在下拉里标出当前项，取不到就当没有当前项。 */
    private static ConfigEnum currentEnum(Ref<?> ref) {
        return currentEnum(ref, -1);
    }

    /** 当前取值：{@code part} 为记录分量下标，{@code -1} 表示整值。 */
    private static ConfigEnum currentEnum(Ref<?> ref, int part) {
        try {
            Object value = ref.getValue();
            if (part >= OPTIONAL_RECORD_BASE) {
                Object inner = optionalInner(ref);
                Object[] parts = recordParts(inner);
                int index = part - OPTIONAL_RECORD_BASE;
                value = parts != null && index < parts.length ? parts[index] : null;
            } else if (part >= 0) {
                Object[] parts = recordParts(value);
                value = parts != null && part < parts.length ? parts[part] : null;
            } else if (value instanceof OptionalPrimitive<?>) {
                value = optionalInner(ref);
            }
            return value instanceof ConfigEnum configEnum ? configEnum : null;
        } catch (Throwable e) {
            return null;
        }
    }

    /** 下拉里要画的候选文字：依次支持路径字符串候选表、有限字符串候选与枚举。 */
    private static List<String> optionLabels(Item item, int part) {
        Ref<?> ref = item.ref();
        if (part < 0) {
            List<String> pathChoices = StringChoicesRegistry.ofPath(item.path());
            if (!pathChoices.isEmpty()) {
                return pathChoices;
            }
            List<String> strings = stringEnumOptions(ref);
            if (!strings.isEmpty()) {
                return strings;
            }
        }
        List<ConfigEnum> enums = enumOptions(ref, part);
        List<String> out = new ArrayList<>(enums.size());
        for (ConfigEnum option : enums) {
            out.add(enumLabel(option));
        }
        return out;
    }

    /** 当前项在候选里的标签：只用来标出哪一行是当前值。 */
    private static String currentOptionLabel(Item item, int part) {
        Ref<?> ref = item.ref();
        if (part < 0) {
            List<String> pathChoices = StringChoicesRegistry.ofPath(item.path());
            if (!pathChoices.isEmpty() && ref instanceof StringRef str) {
                return str.get();
            }
            Object value = ref.getValue();
            if (value instanceof StringEnum choice) {
                return choice.value();
            }
        }
        ConfigEnum current = currentEnum(ref, part);
        return current == null ? null : enumLabel(current);
    }

    /** 写回下拉里选中的那一项：字符串候选、记录分量与普通枚举各走各的写入口。 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void selectOption(Item item, int index, int part) {
        Ref<?> ref = item.ref();
        if (part < 0) {
            List<String> pathChoices = StringChoicesRegistry.ofPath(item.path());
            if (!pathChoices.isEmpty() && ref instanceof StringRef str) {
                if (index >= 0 && index < pathChoices.size()) {
                    str.set(pathChoices.get(index));
                }
                return;
            }
            List<String> strings = stringEnumOptions(ref);
            if (!strings.isEmpty()) {
                if (index >= 0 && index < strings.size()) {
                    Object value = ref.getValue();
                    if (value instanceof StringEnum choice) {
                        ((Ref) ref).setValue(new StringEnum(strings.get(index), choice.values()));
                    }
                }
                return;
            }
        }
        List<ConfigEnum> enums = enumOptions(ref, part);
        if (index >= 0 && index < enums.size()) {
            if (part >= OPTIONAL_RECORD_BASE) {
                RecordFieldOps.writeOptionalRecordPart(ref, part - OPTIONAL_RECORD_BASE, enums.get(index));
            } else if (part >= 0) {
                ((Ref) ref).setValue(withPart(ref.getValue(), part, enums.get(index)));
            } else {
                selectEnum(ref, enums.get(index));
            }
        }
    }

    /** 写回选中的枚举：走 Ref 自己的写入口，校验器与更新监听照旧生效。 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void selectEnum(Ref<?> ref, ConfigEnum value) {
        if (ref.getValue() instanceof OptionalPrimitive<?>) {
            writeOptionalOption(ref, value);
            return;
        }
        if (recordParts(ref.getValue()) != null) {
            // 记录型：下拉身份里带着分量号，按它换掉那一格
            int part = recordPartOf(ref, dropdownKey);
            if (part >= 0) {
                ((Ref) ref).setValue(withPart(ref.getValue(), part, value));
            }
            return;
        }
        try {
            ((Ref) ref).setValue(value);
        } catch (Throwable e) {
            // 类型不符或校验器拒绝：保持原值
        }
    }

    private static String formatNumber(double value) {
        return ValueCodec.formatNumber(value);
    }

    private static String valueText(Ref<?> ref) {
        if (ref == null) {
            return "—";
        }
        try {
            Object value = ref.getValue();
            if (value == null) {
                return "—";
            }
            if (value instanceof Float || value instanceof Double) {
                return formatNumber(((Number) value).doubleValue());
            }
            if (value instanceof Boolean flag) {
                return flag ? "开" : "关";
            }
            if (value instanceof ConfigEnum) {
                return enumLabel(value);
            }
            if (value instanceof MultiKeyBind bind) {
                String keys = bind.isEmpty() ? "" : bind.getKeyStr();
                return keys.isEmpty() ? "未绑定" : keys;
            }
            // 文本族取原文：它们的 toString 是类名，摆到行里等于什么都没说
            if (value instanceof Regex regex) {
                return regex.regex();
            }
            if (value instanceof StringFormat format) {
                return format.formatString();
            }
            String text = String.valueOf(value);
            return text.length() > 40 ? text.substring(0, 40) + "..." : text;
        } catch (Throwable e) {
            return "—";
        }
    }

    // ------------------------------------------------------------------ 布局

    private void buildLines() {
        lines.clear();
        List<Item> shown = shown();
        int y = listTop() - itemScroll;
        String current = null;
        for (Item item : shown) {
            if (!item.section().equals(current)) {
                current = item.section();
                lines.add(new Line(y, SECTION_H, current, null));
                y += SECTION_H;
            }
            int height = ROW_H * rowParts(item.ref());
            lines.add(new Line(y, height, current, item));
            y += height;
        }
        listHeight = (y + itemScroll) - listTop();
    }

    private int maxItemScroll() {
        return Math.max(0, listHeight + 8 - listViewH());
    }

    private int maxRailScroll() {
        int view = Math.max(1, bodyBottom() - railRowsTop());
        return Math.max(0, roots.size() * RAIL_STEP + 4 - view);
    }

    /** 控件区的左边界：绘制标签宽度和点击判定共用，两边不会错开。 */
    private int controlLeft(Item item) {
        int right = itemsRight();
        Ref<?> ref = item.ref();
        if (ref instanceof FlagRef) {
            return right - 20;
        }
        if (isNumeric(ref)) {
            return right - BTN * 2 - 4 - VALUE_W;
        }
        if (ref instanceof EnumRef<?>) {
            return right - enumWidth(ref);
        }
        if (ref instanceof KeyBindRef bindRef) {
            return right - (Ef.textWidth(keyText(bindRef)) + 12);
        }
        Object raw = ref.getValue();
        if (raw instanceof WrapColor) {
            return EndfieldWidgets.colorGeometry(right).chipX();
        }
        if (raw instanceof WrapRGBA) {
            return EndfieldWidgets.rgbaGeometry(right).color().chipX();
        }
        if (raw instanceof OptionalPrimitive<?> opt) {
            if (opt.isPresent() && opt.getValue() instanceof TextColor) {
                return EndfieldWidgets.colorGeometry(right - 28).chipX();
            }
            if (opt.isPresent() && opt.getValue() instanceof CharSequence) {
                return optionalTextSlot(right - 28, 0).x();
            }
            return right - 28 - readOnlyWidth() / 2;
        }
        return right - readOnlyWidth();
    }

    private int enumWidth(Ref<?> ref) {
        return Ef.textWidth(enumText(ref) + " ▾") + 10;
    }

    private int readOnlyWidth() {
        return Math.min(180, Math.max(40, itemsWidth() / 3));
    }

    /**
     * 值槽：点一下就能原地改的那一格。
     *
     * <p>数值让开 ± 两键，槽右沿正好落在减号左边；文本没有键要让，占住只读区原本那一段。
     * 绘制、悬停与命中共用这一份几何，不会出现"看着在格子里却点不到"。
     */
    private Rect valueSlot(Item item, int rowY) {
        Ref<?> ref = item.ref();
        int w = isNumeric(ref) ? VALUE_W : readOnlyWidth();
        return new Rect(controlLeft(item), rowY + FieldEditor.FIELD_TOP, w, FieldEditor.FIELD_H);
    }

    private boolean inValueSlot(Item item, int rowY, int mx, int my) {
        Rect slot = valueSlot(item, rowY);
        return mx >= slot.x() && mx < slot.x() + slot.w() && my >= slot.y() && my < slot.y() + slot.h();
    }

    private Rect optionalTextSlot(int innerRight, int rowY) {
        int w = Math.max(60, Math.min(160, readOnlyWidth()));
        return new Rect(innerRight - w, rowY + FieldEditor.FIELD_TOP, w, FieldEditor.FIELD_H);
    }

    /** 带右侧按钮行的文本框矩形（物品模板 / 注册表正则共用）。 */
    private Rect trailingBtnSlot(int right, int rowY, int trailingCount) {
        int tailW = trailingCount * (BTN + 2);
        int w = Math.max(48, readOnlyWidth() - tailW);
        return new Rect(right - tailW - w, rowY + FieldEditor.FIELD_TOP, w, FieldEditor.FIELD_H);
    }

    /**
     * 枚举下拉的矩形：贴在行下方，放不下翻到行上方，最后夹进窗口。
     *
     * <p>宽度跟着候选文本走但不超过设置列，右沿对到 {@link #itemsRight()}，与芯片同一条线；
     * 绘制与命中都用这一份，翻上翻下不会各算一套。
     */
    private Rect dropdownRect(Line line, int count) {
        int w = Math.min(itemsWidth(), Math.max(120, enumWidth(line.item().ref())));
        int visible = Math.min(count, DROP_MAX_ROWS);
        int h = visible * DROP_ROW_H + 4;
        int x = Math.max(winX + 4, Math.min(winX + winW - 4 - w, itemsRight() - w));
        // 记录与可选记录的下拉：面板贴到具体那一子行下面
        int rowOffset = dropdownPart >= OPTIONAL_RECORD_BASE
                ? (dropdownPart - OPTIONAL_RECORD_BASE + 1)
                : Math.max(0, dropdownPart);
        int y = line.y() + rowOffset * ROW_H + ROW_H + 2;
        if (y + h > winY + winH - 4) {
            y = line.y() + rowOffset * ROW_H - h - 2;
        }
        y = Math.max(winY + 4, Math.min(winY + winH - 4 - h, y));
        return new Rect(x, y, w, h);
    }

    // ------------------------------------------------------------------ 绘制

    private void paint(EfWidget self, VDrawContext ctx, int mouseX, int mouseY) {
        EfCanvas.Palette p = pal();
        buildLines();
        hovered = null;
        formatArgumentHover = null;
        editorHover = null;
        itemTemplateHover = false;
        regexPreviewHover = null;
        // 输入框默认白字，浅色主题的 panel 底上看不见
        editor.setTextColor(p.ink());

        paintBar(ctx, p, mouseX, mouseY);
        paintSearch(ctx, p);
        paintBody(ctx, p, mouseX, mouseY);
        paintFooter(ctx, p);
        paintFrame(ctx, p);
        // 下拉与调色盘是浮层：越过正文裁剪与页脚，所以画在帧之后；说明框仍盖在它们上面
        paintDropdown(ctx, p, mouseX, mouseY);
        paintPalette(ctx, p, mouseX, mouseY);
        paintTip(ctx, p, mouseX, mouseY);
    }

    private void paintBar(VDrawContext ctx, EfCanvas.Palette p, int mouseX, int mouseY) {
        // 命令栏就是整条分类条：当前分类高亮，点别的分类当场换页
        tabBar.paint(ctx, p, winX, winY, winW, "Config", light(), mouseX, mouseY);
    }

    private void paintSearch(VDrawContext ctx, EfCanvas.Palette p) {
        int y = searchTop();
        Ef.fill(ctx, winX, y, winW, SEARCH_H, p.panel());
        Ef.rule(ctx, winX, y + SEARCH_H - 1, winW, p.rule());

        int boxX = winX + 8;
        int boxW = searchWidth();
        boolean focused = searchBox != null && searchBox.isFocused();
        Ef.fill(ctx, boxX, y + 4, boxW, 13, p.paper());
        Ef.cutOutline(ctx, boxX, y + 4, boxW, 13, focused ? p.ink() : p.rule(), p.cut());
        if (focused) {
            // 聚焦时下沿换 2px 的当前位置线，颜色跟主题走
            Ef.fill(ctx, boxX + 1, y + 15, boxW - 2, 2, p.trace());
        }
        Ef.tracked(ctx, "//", boxX + 4, y + 6, p.ink2(), 1);
        if (searchBox != null) {
            searchBox.setTextColor(p.ink());
        }

        int avail = winX + winW - 8 - (boxX + boxW + 12);
        if (avail > 48) {
            String right = Ef.ellipsis("当前配置  │  " + rootName(selected), avail);
            Ef.text(ctx, right, winX + winW - 8 - Ef.textWidth(right), y + 7, p.ink2());
        }
    }

    private void paintBody(VDrawContext ctx, EfCanvas.Palette p, int mouseX, int mouseY) {
        int top = bodyTop();
        int height = bodyHeight();
        Ef.fill(ctx, winX, top, winW, height, p.paper());
        Ef.fill(ctx, winX + railW - 1, top, 1, height, p.rule());

        ctx.enableScissor(winX, top, winX + winW, top + height);
        try {
            paintRail(ctx, p, mouseX, mouseY);
            paintItems(ctx, p, mouseX, mouseY);
        } finally {
            ctx.disableScissor();
        }

        Ef.scrollbar(
                ctx, winX + winW - 4, top + 2, height - 6, itemScroll, listHeight, listViewH(), p.rule(), p.ink2());
    }

    private void paintRail(VDrawContext ctx, EfCanvas.Palette p, int mouseX, int mouseY) {
        int x = winX;
        int chipX = x + 8;
        int chipW = Math.max(40, railW - 16);
        int y = railRowsTop() - railScroll;
        for (Config config : roots) {
            if (y + RAIL_CHIP_H >= bodyTop() && y <= bodyBottom()) {
                boolean current = config == selected;
                boolean hover = mouseX >= chipX && mouseX <= chipX + chipW && mouseY >= y && mouseY < y + RAIL_CHIP_H;
                int bg = current ? p.signal() : (hover ? Ef.mix(p.chip(), p.ink(), 0.12F) : p.chip());
                int fg = current ? p.onSignal() : p.ink();
                Ef.cutFill(ctx, chipX, y, chipW, RAIL_CHIP_H, bg, p.cutWide());

                String number = String.valueOf(itemsOf(config).size());
                int numberW = Ef.textWidth(number);
                // 切角会啃掉右下角，右侧数字必须让开切角宽度
                int nameW = Math.max(20, chipW - 10 - numberW - p.cutWide());
                Ef.text(ctx, Ef.ellipsis(rootName(config), nameW), chipX + 5, y + 3, fg);
                Ef.text(ctx, number, chipX + chipW - p.cutWide() - 4 - numberW, y + 3, current ? p.onSignal() : p.ink2());
            }
            y += RAIL_STEP;
        }

        // 表头最后画：左列滚动时从它底下穿过去，不用再套一层裁剪
        Ef.fill(ctx, x, bodyTop(), Math.max(1, railW - 1), railRowsTop() - bodyTop(), p.paper());
        Ef.trackedWide(ctx, "// CONFIGS", x + 8, bodyTop() + 6, p.ink2(), 1, p.wide());
        int titleY = bodyTop() + 6 + Ef.lineH() + 2;
        Ef.text(ctx, "配置文件", x + 8, titleY, p.ink());
        String count = roots.size() + " 个";
        Ef.text(ctx, count, x + railW - 8 - Ef.textWidth(count), titleY, p.ink2());
        Ef.rule(ctx, x + 8, bodyTop() + 32, Math.max(1, railW - 16), p.rule());
    }

    private void paintItems(VDrawContext ctx, EfCanvas.Palette p, int mouseX, int mouseY) {
        int ix = itemsX();
        int right = itemsRight();

        if (selected == null) {
            Ef.text(ctx, "宿主还没有注册配置文件，这里暂时没有可编辑的设置。", ix, listTop() + 4, p.ink2());
            return;
        }

        for (Line line : lines) {
            if (line.y() + line.height() < bodyTop() || line.y() > bodyBottom()) {
                continue;
            }
            if (line.item() == null) {
                paintSection(ctx, p, line);
            } else {
                paintRow(ctx, p, line, mouseX, mouseY);
            }
        }

        if (lines.isEmpty()) {
            String empty = query.trim().isEmpty()
                    ? "这份配置里还没有可显示的设置。"
                    : "没有匹配「" + query.trim() + "」的配置项，换个词试试。";
            Ef.text(ctx, empty, ix, listTop() + 4, p.ink2());
        }

        // 标题块最后画：设置项滚上来时从它底下穿过去，不用再套一层裁剪
        int top = bodyTop() + 6;
        Ef.fill(ctx, winX + railW, bodyTop(), winW - railW, listTop() - bodyTop(), p.paper());
        Ef.trackedWide(ctx, Ef.ellipsisWide("// " + codeOf(selected), itemsWidth(), 1, p.wide()), ix, top, p.ink2(), 1, p.wide());
        int titleY = top + Ef.lineH() + 2;
        List<Item> items = all();
        String meta = items.size() + " 项  │  " + sectionsOf(items) + " 组";
        int metaX = right - Ef.textWidth(meta);
        int titleW = Math.max(40, metaX - ix - 10);
        Ef.text(ctx, Ef.ellipsis(rootName(selected), titleW), ix, titleY, p.ink());
        Ef.text(ctx, meta, metaX, titleY, p.ink2());
        Ef.rule(ctx, ix, top + HEAD_H - 8, itemsWidth(), p.rule());
    }

    private void paintSection(VDrawContext ctx, EfCanvas.Palette p, Line line) {
        int ix = itemsX();
        String section = line.section();
        String mark = section.isEmpty() ? "MAIN" : leaf(section).replace('-', ' ').toUpperCase(Locale.ROOT);
        String eyebrow = "// " + mark;
        Ef.trackedWide(ctx, eyebrow, ix, line.y() + 7, p.ink2(), 1, p.wide());
        int titleX = ix + Math.max(72, Ef.trackedWideWidth(eyebrow, 1, p.wide()) + 10);
        int titleW = Math.max(20, itemsWidth() - (titleX - ix));
        Ef.text(ctx, Ef.ellipsis(sectionTitle(section), titleW), titleX, line.y() + 7, p.ink());
        Ef.rule(ctx, ix, line.y() + SECTION_H - 5, itemsWidth(), p.rule());
    }

    private void paintRow(VDrawContext ctx, EfCanvas.Palette p, Line line, int mouseX, int mouseY) {
        int ix = itemsX();
        int right = itemsRight();
        int y = line.y();
        Item item = line.item();
        Ref<?> ref = item.ref();

        boolean hover = mouseY >= Math.max(y, listTop())
                && mouseY < y + line.height()
                && mouseX >= ix - 4
                && mouseX <= right + 4;
        if (hover) {
            hovered = item;
            Ef.fill(ctx, ix - 4, y, itemsWidth() + 8, line.height(), Ef.mix(p.paper(), p.ink(), 0.05F));
            Ef.fill(ctx, ix - 4, y, 3, line.height(), p.trace());
        }

        int labelW = Math.max(24, controlLeft(item) - 8 - ix);
        Ef.text(ctx, Ef.ellipsis(labelOf(item), labelW), ix, y + 5, p.ink());
        Ef.rule(ctx, ix - 4, y + line.height() - 1, itemsWidth() + 8, Ef.alpha(p.rule(), 0.55F));

        if (ref instanceof FlagRef flag) {
            int sw = 20;
            int sh = 9;
            int sx = right - sw;
            int sy = y + (ROW_H - sh) / 2;
            if (flag.get()) {
                Ef.fill(ctx, sx, sy, sw, sh, p.ink());
                Ef.fill(ctx, sx + sw - 7, sy + 2, 5, 5, p.paper());
            } else {
                Ef.dashedOutline(ctx, sx, sy, sw, sh, p.ink2());
                Ef.fill(ctx, sx + 2, sy + 2, 5, 5, p.ink2());
            }
        } else if (isNumeric(ref)) {
            int minusX = right - BTN * 2 - 4;
            int plusX = right - BTN;
            paintValue(ctx, p, item, y, mouseX, mouseY);
            paintStep(ctx, p, minusX, y, "-", mouseX, mouseY);
            paintStep(ctx, p, plusX, y, "+", mouseX, mouseY);
        } else if (ref instanceof EnumRef<?>) {
            String text = enumText(ref) + " ▾";
            int width = enumWidth(ref);
            int chipX = right - width;
            boolean open = enumKey(item).equals(dropdownKey);
            boolean hot = open || (mouseX >= chipX && mouseX <= right && mouseY >= y + 3 && mouseY < y + 15);
            Ef.cutFill(ctx, chipX, y + 3, width, 12, hot ? Ef.mix(p.chip(), p.ink(), 0.14F) : p.chip(), p.cut());
            Ef.text(ctx, text, chipX + 5, y + 5, p.ink());
        } else if (ref.getValue() instanceof EntrySet<?> set) {
            paintEntrySet(ctx, p, y, right, ref, set);
        } else if (textValueRow(ref)) {
            paintTextValueRow(ctx, p, item, y, mouseX, mouseY);
        } else if (countRow(ref)) {
            paintCount(ctx, p, y, right, ref);
        } else if (booleanParts(ref.getValue()) != null) {
            paintBooleanRecord(ctx, p, y, right, ref);
        } else if (recordParts(ref.getValue()) != null) {
            paintRecord(ctx, p, y, right, ref, mouseX, mouseY);
        } else if (ref.getValue() instanceof WidgetPos pos) {
            paintWidgetPos(ctx, p, y, right, ref, pos, mouseX, mouseY);
        } else if (ref.getValue() instanceof GuiSetting) {
            paintGuiSetting(ctx, p, y, right);
        } else if (ref.getValue() instanceof StringEnum choice) {
            paintStringEnum(ctx, p, y, right, ref, choice);
        } else if (ref.getValue() instanceof EntityTypeEntrySet set) {
            paintCountChip(ctx, p, y, right, set.set().size() + " 项");
        } else if (ref.getValue() instanceof ItemStackTemplate template) {
            paintItemTemplate(ctx, p, y, right, item, template, mouseX, mouseY);
        } else if (ref.getValue() instanceof RegistryRegex<?> || ref.getValue() instanceof WeakRegistryRegex<?>) {
            paintRegistryRegex(ctx, p, y, right, item, mouseX, mouseY);
        } else if (ref.getValue() instanceof StringFormat format) {
            paintStringFormat(ctx, p, y, right, item, format, mouseX, mouseY);
        } else if (ref instanceof StringRef && !StringChoicesRegistry.ofPath(item.path()).isEmpty()) {
            paintStringChoices(ctx, p, y, item, ((StringRef) ref).get(), mouseX, mouseY);
        } else if (ref.getValue() instanceof LabelVec2 label2) {
            paintLabelVec(ctx, p, y, right, ref, label2.xLabel(), label2.yLabel(), null,
                    label2.data().x(), label2.data().y(), 0, mouseX, mouseY);
        } else if (ref.getValue() instanceof LabelVec3 label3) {
            paintLabelVec(ctx, p, y, right, ref, label3.xLabel(), label3.yLabel(), label3.zLabel(),
                    label3.data().x(), label3.data().y(), label3.data().z(), mouseX, mouseY);
        } else if (optionalRow(ref)) {
            paintOptional(ctx, p, y, right, item, mouseX, mouseY);
        } else if (ref instanceof KeyBindRef bindRef) {
            paintKeyBind(ctx, p, y, right, bindRef, item);
        } else if (ref.getValue() instanceof WrapColor color) {
            paintColor(ctx, p, y, right, item, color);
        } else if (ref.getValue() instanceof WrapRGBA rgba) {
            paintRgba(ctx, p, y, right, item, rgba, mouseX, mouseY);
        } else if (textEditable(ref)) {
            paintValue(ctx, p, item, y, mouseX, mouseY);
        } else {
            String text = Ef.ellipsis(valueText(ref), readOnlyWidth());
            Ef.text(ctx, text, right - Ef.textWidth(text), y + 5, p.ink2());
        }
    }

    /** 值框：物品模板与注册表条目共用，静态态画虚线底边。 */
    private void paintTextValueRow(VDrawContext ctx, EfCanvas.Palette p, Item item, int y, int mouseX, int mouseY) {
        Rect slot = valueSlot(item, y);
        boolean editing = editor.isOpenFor(editorKey(item));
        boolean hot = !editing && mouseX >= slot.x() && mouseX < slot.x() + slot.w()
                && mouseY >= slot.y() && mouseY < slot.y() + slot.h();
        EndfieldWidgets.paintValueSlot(ctx, p, slot.x(), slot.y(), slot.w(), slot.h(), editing, hot);
        if (editing) {
            return;
        }
        String text = textValueRowOf(item.ref());
        String shown = Ef.ellipsis(text, slot.w() - 8);
        Ef.text(ctx, shown, slot.x() + slot.w() - 6 - Ef.textWidth(shown), y + 5, p.ink());
    }

    /** 计数块：一个「N 项 ▾」切角块，点开宿主的列表/键值编辑屏。 */
    private void paintCount(VDrawContext ctx, EfCanvas.Palette p, int y, int right, Ref<?> ref) {
        EndfieldWidgets.paintCountChip(ctx, p, y, right, countOf(ref) + " 项", p.cut(), false);
    }

    /**
     * 一条记录里的几格：每格占一行，行名用字段名；数值就地编辑，枚举点开下拉。
     *
     * <p>行高由 {@link #rowParts} 按分量数算，绘制与布局取同一个数，下一行不会压上来。
     */
    private void paintRecord(VDrawContext ctx, EfCanvas.Palette p, int y, int right, Ref<?> ref, int mouseX, int mouseY) {
        Object[] parts = recordParts(ref.getValue());
        if (parts == null) {
            return;
        }
        String[] names = componentNames(ref.getValue());
        for (int i = 0; i < parts.length; i++) {
            int partY = y + i * ROW_H;
            Object part = parts[i];
            if (part instanceof Number number) {
                double numeric = number.doubleValue();
                String text = formatNumber(numeric);
                Rect slot = new Rect(right - VALUE_W, partY + FieldEditor.FIELD_TOP, VALUE_W, FieldEditor.FIELD_H);
                boolean editing = editor.isOpenFor(recordKey(ref, i));
                boolean hot = !editing && mouseX >= slot.x() && mouseX < slot.x() + slot.w()
                        && mouseY >= slot.y() && mouseY < slot.y() + slot.h();
                EndfieldWidgets.paintValueSlot(ctx, p, slot.x(), slot.y(), slot.w(), slot.h(), editing, hot);
                if (!editing) {
                    Ef.text(ctx, text, slot.x() + slot.w() - 6 - Ef.textWidth(text), partY + 5, p.ink());
                }
            } else if (part instanceof ConfigEnum configEnum) {
                String text = enumLabel(configEnum) + " ▾";
                int width = Ef.textWidth(text) + 10;
                boolean open = recordKey(ref, i).equals(dropdownKey);
                Ef.cutFill(ctx, right - width, partY + 3, width, 12,
                        open ? Ef.mix(p.chip(), p.ink(), 0.14F) : p.chip(), p.cut());
                Ef.text(ctx, text, right - width + 5, partY + 5, p.ink());
            }
            paintPartName(ctx, p, partY, right - VALUE_W - 12, names[i]);
        }
    }

    /**
     * 一条记录里的几格开关：每格占一行，行名用字段名，开关贴着行尾。
     *
     * <p>行高由 {@link #rowParts} 按分量数算，绘制与布局取同一个数，下一行不会压上来。
     */
    private void paintBooleanRecord(VDrawContext ctx, EfCanvas.Palette p, int y, int right, Ref<?> ref) {
        boolean[] bools = booleanParts(ref.getValue());
        if (bools == null) {
            return;
        }
        String[] names = componentNames(ref.getValue());
        for (int i = 0; i < bools.length; i++) {
            int partY = y + i * ROW_H;
            paintPartName(ctx, p, partY, right - 28, names[i]);
            paintSwitch(ctx, p, partY, right, bools[i]);
        }
    }

    /** 分量行名：右对齐贴着控件，宽度按可用空间截断，长字段名不会压到行名上。 */
    private void paintPartName(VDrawContext ctx, EfCanvas.Palette p, int y, int rightEdge, String name) {
        EndfieldWidgets.paintPartName(ctx, p, y, itemsX(), rightEdge, name, p.ink2());
    }

    /** 行尾那个小开关：开是实心块加纸色缺口，关是虚线框加小方块。 */
    private void paintSwitch(VDrawContext ctx, EfCanvas.Palette p, int y, int right, boolean on) {
        EndfieldWidgets.paintSwitch(ctx, p, y, ROW_H, right, on);
    }

    /**
     * 位置配置：行尾一个 type 切换块，下面两行滑杆。
     *
     * <p>{@code type} 决定哪两个字段生效（0 = 百分比、1 = 绝对长度），
     * 所以只画当前生效的那一对——四个字段全摆出来会让人以为都能调。
     */
    private void paintWidgetPos(
            VDrawContext ctx, EfCanvas.Palette p, int y, int right, Ref<?> ref, WidgetPos pos,
            int mouseX, int mouseY) {
        boolean percent = pos.getType() == 0;
        String mode = percent ? "百分比" : "绝对";
        int modeW = Ef.textWidth(mode) + 10;
        int modeX = right - modeW;
        boolean hot = mouseX >= modeX && mouseX < modeX + modeW && mouseY >= y + 3 && mouseY < y + 15;
        Ef.cutFill(ctx, modeX, y + 3, modeW, 12, hot ? p.ink() : p.chip(), p.cut());
        Ef.text(ctx, mode, modeX + 5, y + 5, hot ? p.paper() : p.ink());

        Vec2 pair = percent ? pos.toPercentage() : pos.toLength();
        String[] names = percent ? new String[] {"X 比例", "Y 比例"} : new String[] {"X 长度", "Y 长度"};
        double[] values = {pair.x(), pair.y()};
        // 滑杆与数值要让开行尾那个 type 块：两者都右对齐到 itemsRight() 会叠在一起
        int sliderRight = right - modeW - 6;
        for (int i = 0; i < 2; i++) {
            int rowY = y + i * ROW_H;
            String text = formatNumber(values[i]);
            int width = Math.max(VALUE_W, Ef.textWidth(text) + 12);
            Rect slot = new Rect(sliderRight - width, rowY + FieldEditor.FIELD_TOP, width, FieldEditor.FIELD_H);
            paintPartName(ctx, p, rowY, slot.x() - 8, names[i]);
            boolean editing = editor.isOpenFor(widgetPosKey(ref, i));
            boolean slotHot = !editing && mouseX >= slot.x() && mouseX < slot.x() + slot.w()
                    && mouseY >= slot.y() && mouseY < slot.y() + slot.h();
            EndfieldWidgets.paintValueSlot(ctx, p, slot.x(), slot.y(), slot.w(), slot.h(), editing, slotHot);
            if (!editing) {
                Ef.text(ctx, text, slot.x() + slot.w() - 6 - Ef.textWidth(text), rowY + 5, p.ink());
            }
        }
    }

    /** 位置配置里某一格的身份：绘制、命中与编辑共用。 */
    private static String widgetPosKey(Ref<?> ref, int part) {
        return "widgetpos:" + System.identityHashCode(ref) + ":" + part;
    }

    /** GUI 设置：行里一个自绘入口块，点开本界面自己的详细页。 */
    private void paintGuiSetting(VDrawContext ctx, EfCanvas.Palette p, int y, int right) {
        int width = Ef.textWidth(GUI_SETTING_CHIP) + 10;
        int x = right - width;
        Ef.cutFill(ctx, x, y + 3, width, 12, p.chip(), p.cut());
        Ef.text(ctx, GUI_SETTING_CHIP, x + 5, y + 5, p.ink());
    }

    /** 计数块：一个「N 项 ▾」切角块，点开宿主的编辑屏。 */
    private void paintCountChip(VDrawContext ctx, EfCanvas.Palette p, int y, int right, String text) {
        EndfieldWidgets.paintCountChip(ctx, p, y, right, text, p.cut(), false);
    }

    /**
     * 带标签的向量：每格占一行，行名取记录里自带的标签。
     *
     * <p>标签是数据的一部分（{@code LabelVec2} 的 {@code xLabel}），不是字段名，
     * 所以不能走 {@link #componentNames}——那个读的是记录分量名，会画成「X LABEL」。
     */
    private void paintLabelVec(
            VDrawContext ctx, EfCanvas.Palette p, int y, int right, Ref<?> ref,
            String xLabel, String yLabel, String zLabel, double x, double y2, double z,
            int mouseX, int mouseY) {
        String[] labels = {xLabel, yLabel, zLabel};
        double[] values = {x, y2, z};
        int count = zLabel == null ? 2 : 3;
        for (int i = 0; i < count; i++) {
            int partY = y + i * ROW_H;
            String text = formatNumber(values[i]);
            int width = Math.max(VALUE_W, Ef.textWidth(text) + 12);
            Rect slot = new Rect(right - width, partY + FieldEditor.FIELD_TOP, width, FieldEditor.FIELD_H);
            paintPartName(ctx, p, partY, slot.x() - 8, labelText(labels[i]));
            boolean editing = editor.isOpenFor(labelVecKey(ref, i));
            boolean hot = !editing && mouseX >= slot.x() && mouseX < slot.x() + slot.w()
                    && mouseY >= slot.y() && mouseY < slot.y() + slot.h();
            EndfieldWidgets.paintValueSlot(ctx, p, slot.x(), slot.y(), slot.w(), slot.h(), editing, hot);
            if (!editing) {
                Ef.text(ctx, text, slot.x() + slot.w() - 6 - Ef.textWidth(text), partY + 5, p.ink());
            }
        }
    }

    /** 标签是翻译键：取不到就原样显示，别让一格坏掉整行。 */
    private static String labelText(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        return Component.translatableWithFallback(key, key).getString();
    }

    /** 带标签向量里某一格的身份：绘制、命中与编辑共用。 */
    private static String labelVecKey(Ref<?> ref, int part) {
        return "labelvec:" + System.identityHashCode(ref) + ":" + part;
    }

    /** 有限字符串候选：与枚举同一套下拉，值框显示当前项。 */
    private void paintStringEnum(VDrawContext ctx, EfCanvas.Palette p, int y, int right, Ref<?> ref, StringEnum choice) {
        String text = choice.value() + " ▾";
        int width = Ef.textWidth(text) + 10;
        int x = right - width;
        boolean open = enumKeyOf(ref).equals(dropdownKey);
        Ef.cutFill(ctx, x, y + 3, width, 12, open ? Ef.mix(p.chip(), p.ink(), 0.14F) : p.chip(), p.cut());
        Ef.text(ctx, text, x + 5, y + 5, p.ink());
    }

    /** 记录里某一格的身份：绘制、命中与编辑共用。 */
    private static String recordKey(Ref<?> ref, int part) {
        return "record:" + System.identityHashCode(ref) + ":" + part;
    }

    /** 当前打开的下拉属于记录里的第几格；不是记录型返回 -1。 */
    private static int recordPartOf(Ref<?> ref, String key) {
        String prefix = "record:" + System.identityHashCode(ref) + ":";
        if (key == null || !key.startsWith(prefix)) {
            return -1;
        }
        try {
            return Integer.parseInt(key.substring(prefix.length()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * 名单：一个「N 项 ▾」切角块，点开宿主的注册表选择屏。
     *
     * <p>增删要遍历注册表、要按 id 反查条目，自绘一份等于重写宿主的 {@code RegistrySelectScreen}；
     * 这里只摆入口，编辑能力借宿主的——与模块配置页同一个思路。
     */
    private void paintEntrySet(VDrawContext ctx, EfCanvas.Palette p, int y, int right, Ref<?> ref, EntrySet<?> set) {
        EndfieldWidgets.paintCountChip(ctx, p, y, right, set.set().size() + " 项", Ef.CUT, false);
    }

    /**
     * 可选值：行尾一个开关决定这一项是否生效，左边是内层值自己的控件。
     *
     * <p>没启用时值区写一块「未启用」，留空会被当成坏掉的控件。
     * 内层可以是数值（就地编辑）或枚举（下拉），与模块配置页同一套画法。
     */
    private void paintOptional(
            VDrawContext ctx, EfCanvas.Palette p, int y, int right, Item item, int mouseX, int mouseY) {
        Ref<?> ref = item.ref();
        OptionalPrimitive<?> optional = (OptionalPrimitive<?>) ref.getValue();
        EndfieldWidgets.paintSwitch(ctx, p, y, ROW_H, right, optional.isPresent());

        int innerRight = right - 28;
        Object inner = optionalInner(ref);
        if (inner == null) {
            String off = "未启用";
            int offW = Ef.textWidth(off) + 10;
            Ef.dashedOutline(ctx, innerRight - offW, y + 3, offW, 11, p.ink2());
            Ef.text(ctx, off, innerRight - offW + 5, y + 5, p.ink2());
            return;
        }
        if (inner instanceof ConfigEnum configEnum) {
            String text = enumLabel(configEnum) + " ▾";
            int width = Ef.textWidth(text) + 10;
            boolean open = optionalKey(ref).equals(dropdownKey);
            Ef.cutFill(ctx, innerRight - width, y + 3, width, 12,
                    open ? Ef.mix(p.chip(), p.ink(), 0.14F) : p.chip(), p.cut());
            Ef.text(ctx, text, innerRight - width + 5, y + 5, p.ink());
            return;
        }
        if (inner instanceof Number number) {
            double numeric = number.doubleValue();
            String text = formatNumber(numeric);
            int width = Math.max(VALUE_W, Ef.textWidth(text) + 12);
            Rect slot = new Rect(innerRight - width, y + FieldEditor.FIELD_TOP, width, FieldEditor.FIELD_H);
            boolean editing = editor.isOpenFor(editorKey(ref, optionalKey(ref)));
            boolean hot = !editing && inRect(slot, mouseX, mouseY);
            EndfieldWidgets.paintValueSlot(ctx, p, slot.x(), slot.y(), slot.w(), slot.h(), editing, hot);
            if (!editing) {
                Ef.text(ctx, text, slot.x() + slot.w() - 6 - Ef.textWidth(text), y + 5, p.ink());
            }
            return;
        }
        if (inner instanceof Boolean flag) {
            paintSwitch(ctx, p, y, innerRight, flag);
            return;
        }
        if (inner instanceof TextColor) {
            EndfieldWidgets.paintColorRow(
                    ctx, p, innerRight, y, ValueCodec.colorOf(optional), editor.isOpenFor(editorKey(item)), Ef.CUT);
            return;
        }
        if (inner instanceof CharSequence text) {
            Rect slot = optionalTextSlot(innerRight, y);
            paintSimpleSlot(ctx, p, slot, editor.isOpenFor(editorKey(item)), text.toString(), mouseX, mouseY);
            return;
        }
        Object[] parts = recordParts(inner);
        if (parts != null) {
            String on = "已启用";
            int onW = Ef.textWidth(on) + 10;
            Ef.cutFill(ctx, innerRight - onW, y + 3, onW, 12, p.chip(), p.cut());
            Ef.text(ctx, on, innerRight - onW + 5, y + 5, p.ink2());
            String[] names = componentNames(inner);
            for (int i = 0; i < parts.length; i++) {
                int partY = y + (i + 1) * ROW_H;
                int partIdx = OPTIONAL_RECORD_BASE + i;
                Object part = parts[i];
                if (part instanceof Number number) {
                    String text = formatNumber(number.doubleValue());
                    Rect slot = new Rect(right - VALUE_W, partY + FieldEditor.FIELD_TOP, VALUE_W, FieldEditor.FIELD_H);
                    boolean editing = editor.isOpenFor(recordKey(ref, partIdx));
                    EndfieldWidgets.paintValueSlot(
                            ctx, p, slot.x(), slot.y(), slot.w(), slot.h(), editing, !editing && inRect(slot, mouseX, mouseY));
                    if (!editing) {
                        Ef.text(ctx, text, slot.x() + slot.w() - 6 - Ef.textWidth(text), partY + 5, p.ink());
                    }
                } else if (part instanceof ConfigEnum configEnum) {
                    String text = enumLabel(configEnum) + " ▾";
                    int width = Ef.textWidth(text) + 10;
                    boolean open = recordKey(ref, partIdx).equals(dropdownKey);
                    Ef.cutFill(ctx, right - width, partY + 3, width, 12,
                            open ? Ef.mix(p.chip(), p.ink(), 0.14F) : p.chip(), p.cut());
                    Ef.text(ctx, text, right - width + 5, partY + 5, p.ink());
                }
                paintPartName(ctx, p, partY, right - VALUE_W - 12, names[i]);
            }
            return;
        }
        boolean[] bools = booleanParts(inner);
        if (bools != null) {
            String on = "已启用";
            int onW = Ef.textWidth(on) + 10;
            Ef.cutFill(ctx, innerRight - onW, y + 3, onW, 12, p.chip(), p.cut());
            Ef.text(ctx, on, innerRight - onW + 5, y + 5, p.ink2());
            String[] names = componentNames(inner);
            for (int i = 0; i < bools.length; i++) {
                int partY = y + (i + 1) * ROW_H;
                paintPartName(ctx, p, partY, right - 28, names[i]);
                paintSwitch(ctx, p, partY, right, bools[i]);
            }
        }
    }

    /** 按键绑定：等待按键时整块换成强调色，其余时候是描边块。 */
    private void paintKeyBind(VDrawContext ctx, EfCanvas.Palette p, int y, int right, KeyBindRef ref, Item item) {
        boolean listening = keyTarget != null && keyTarget.equals(item.path());
        String text = listening ? "按任意键…" : keyText(ref);
        int width = Ef.textWidth(text) + 12;
        int x = right - width;
        if (listening) {
            Ef.cutFill(ctx, x, y + 3, width, 12, p.trace(), Ef.CUT);
            Ef.text(ctx, text, x + 6, y + 5, p.onTrace());
        } else {
            Ef.cutOutline(ctx, x, y + 3, width, 12, p.rule(), Ef.CUT);
            Ef.text(ctx, text, x + 6, y + 5, p.ink());
        }
    }

    private static String keyText(KeyBindRef ref) {
        MultiKeyBind bind = ref.get();
        if (bind == null || bind.isEmpty()) {
            return "未绑定";
        }
        String keys = bind.getKeyStr();
        return keys == null || keys.isEmpty() ? "未绑定" : keys;
    }

    /** 颜色：切角块里一块色块加它的十六进制值，色块开调色盘、十六进制开输入框。 */
    private void paintColor(VDrawContext ctx, EfCanvas.Palette p, int y, int right, Item item, WrapColor color) {
        int rgb = ValueCodec.colorOf(color);
        EndfieldWidgets.paintColorRow(ctx, p, right, y, rgb, editor.isOpenFor(editorKey(item)), Ef.CUT);
    }

    /** RGBA 颜色：色块 + #RRGGBB + 右侧 A:0..255 不透明度槽。 */
    private void paintRgba(
            VDrawContext ctx, EfCanvas.Palette p, int y, int right, Item item, WrapRGBA rgba,
            int mouseX, int mouseY) {
        EndfieldWidgets.RgbaGeometry geo = EndfieldWidgets.rgbaGeometry(right);
        boolean editingAlpha = editor.isOpenFor(alphaKey(item.ref()));
        boolean hotAlpha = !editingAlpha && mouseX >= geo.alphaFieldX()
                && mouseX < geo.alphaFieldX() + geo.alphaFieldW()
                && mouseY >= y + FieldEditor.FIELD_TOP && mouseY < y + FieldEditor.FIELD_TOP + FieldEditor.FIELD_H;
        EndfieldWidgets.paintRgbaRow(
                ctx, p, right, y, ValueCodec.colorOf(rgba), rgba.alpha(),
                editor.isOpenFor(editorKey(item)), editingAlpha, hotAlpha, Ef.CUT);
    }

    /** 带候选值的字符串：左边值框手输，右端箭头展开注册候选下拉。 */
    private void paintStringChoices(
            VDrawContext ctx, EfCanvas.Palette p, int y, Item item, String current, int mouseX, int mouseY) {
        Rect slot = valueSlot(item, y);
        boolean editing = editor.isOpenFor(editorKey(item));
        boolean open = choicesKey(item).equals(dropdownKey);
        if (editing) {
            EndfieldWidgets.paintFieldFrame(ctx, p, slot.x(), slot.y(), slot.w(), slot.h(), true, false);
            return;
        }
        boolean hot = inRect(slot, mouseX, mouseY);
        EndfieldWidgets.paintFieldFrame(ctx, p, slot.x(), slot.y(), slot.w(), slot.h(), open, hot);
        boolean blank = current == null || current.isEmpty();
        String label = blank ? "空" : current;
        String shown = Ef.ellipsis(label, slot.w() - FieldEditor.FIELD_PAD * 2 - ARROW_W);
        Ef.text(ctx, shown, slot.x() + FieldEditor.FIELD_PAD, slot.y() + FieldEditor.FIELD_PAD,
                blank ? p.ink2() : p.ink());
        String arrow = "▾";
        Ef.text(ctx, arrow, slot.x() + slot.w() - ARROW_W + (ARROW_W - Ef.textWidth(arrow)) / 2,
                slot.y() + FieldEditor.FIELD_PAD, open ? p.ink() : p.ink2());
    }

    /** 物品模板：值框编辑 id，行尾按钮开宿主物品编辑器。 */
    private void paintItemTemplate(
            VDrawContext ctx, EfCanvas.Palette p, int y, int right, Item item, ItemStackTemplate template,
            int mouseX, int mouseY) {
        int btnX = right - BTN;
        Rect slot = trailingBtnSlot(right, y, 1);
        paintSimpleSlot(ctx, p, slot, editor.isOpenFor(editorKey(item)), template.itemId().toString(), mouseX, mouseY);
        boolean hot = mouseX >= btnX && mouseX < btnX + BTN && mouseY >= y + 2 && mouseY < y + 2 + BTN;
        if (hot) {
            Ef.cutFill(ctx, btnX, y + 2, BTN, BTN, p.chip(), Ef.CUT);
            itemTemplateHover = true;
        }
        Ef.icon(ctx, HostControls.editorIcon(), btnX, y + 2, BTN, hot ? p.ink() : p.ink2());
    }

    /** 注册表正则：值框编辑正则原文，行尾按钮开实时匹配的注册表预览屏。 */
    private void paintRegistryRegex(
            VDrawContext ctx, EfCanvas.Palette p, int y, int right, Item item, int mouseX, int mouseY) {
        int btnX = right - BTN;
        Rect slot = trailingBtnSlot(right, y, 1);
        paintSimpleSlot(ctx, p, slot, editor.isOpenFor(editorKey(item)), editableTextOf(item.ref()), mouseX, mouseY);
        boolean available = HostEditors.canPreviewRegistryRegex(item.ref().getValue());
        boolean hot = mouseX >= btnX && mouseX < btnX + BTN && mouseY >= y + 2 && mouseY < y + 2 + BTN;
        if (hot && available) {
            Ef.cutFill(ctx, btnX, y + 2, BTN, BTN, p.chip(), Ef.CUT);
        }
        int fg = available ? (hot ? p.ink() : p.ink2()) : Ef.alpha(p.ink2(), 0.45F);
        Ef.icon(ctx, HostControls.listTagIcon(), btnX, y + 2, BTN, fg);
        if (hot) {
            regexPreviewHover = available ? "预览匹配列表：按正则实时筛选注册表条目" : "需先进入世界才能预览该注册表";
        }
    }

    /** 格式化串：值框 + 占位符说明图标 + 颜色代码百科图标。 */
    private void paintStringFormat(
            VDrawContext ctx, EfCanvas.Palette p, int y, int right, Item item, StringFormat format,
            int mouseX, int mouseY) {
        int cursor = right;
        int editX = -1;
        int argX = -1;
        if (format.colorString()) {
            editX = cursor - BTN;
            cursor = editX - 2;
        }
        if (!format.formattingArgument().isEmpty()) {
            argX = cursor - BTN;
            cursor = argX - 2;
        }
        int tailCount = (editX >= 0 ? 1 : 0) + (argX >= 0 ? 1 : 0);
        Rect slot = trailingBtnSlot(right, y, tailCount);
        paintSimpleSlot(ctx, p, slot, editor.isOpenFor(editorKey(item)), format.formatString(), mouseX, mouseY);
        if (argX >= 0) {
            boolean hot = mouseX >= argX && mouseX < argX + BTN && mouseY >= y + 2 && mouseY < y + 2 + BTN;
            if (hot) {
                Ef.cutFill(ctx, argX, y + 2, BTN, BTN, p.chip(), Ef.CUT);
                formatArgumentHover = format;
            }
            Ef.icon(ctx, HostControls.formattingIcon(), argX, y + 2, BTN, hot ? p.ink() : p.ink2());
        }
        if (editX >= 0) {
            boolean hot = mouseX >= editX && mouseX < editX + BTN && mouseY >= y + 2 && mouseY < y + 2 + BTN;
            if (hot) {
                Ef.cutFill(ctx, editX, y + 2, BTN, BTN, p.chip(), Ef.CUT);
                editorHover = format;
            }
            Ef.icon(ctx, HostControls.editorIcon(), editX, y + 2, BTN, hot ? p.ink() : p.ink2());
        }
    }

    private void paintSimpleSlot(
            VDrawContext ctx, EfCanvas.Palette p, Rect slot, boolean editing, String raw, int mouseX, int mouseY) {
        boolean hot = !editing && inRect(slot, mouseX, mouseY);
        EndfieldWidgets.paintValueSlot(ctx, p, slot.x(), slot.y(), slot.w(), slot.h(), editing, hot);
        if (editing) {
            return;
        }
        boolean blank = raw == null || raw.isEmpty();
        String shown = Ef.ellipsis(blank ? "空" : raw, slot.w() - 8);
        Ef.text(ctx, shown, slot.x() + slot.w() - 6 - Ef.textWidth(shown), slot.y() + 4, blank ? p.ink2() : p.ink());
    }

    private static boolean inRect(Rect rect, int mx, int my) {
        return mx >= rect.x() && mx < rect.x() + rect.w() && my >= rect.y() && my < rect.y() + rect.h();
    }

    private void paintPalette(VDrawContext ctx, EfCanvas.Palette p, int mouseX, int mouseY) {
        if (!(palettePanel.target() instanceof Item target) || target.ref() == null) {
            return;
        }
        if (palettePanel.paint(ctx, p, winX, winY, winW, winH, ROW_H,
                ValueCodec.colorOf(target.ref().getValue()), mouseX, mouseY)) {
            hovered = null;
        }
    }

    /**
     * 值槽：编辑中只画框（字由输入框自己画），其余时候值右对齐到槽内沿。
     *
     * <p>悬停补一圈 1px 的框——这一格能点开，是点之前就该看出来的事。
     * 值右对齐到槽内沿而不是行尾，框出现时字不会横向跳一格。
     */
    private void paintValue(VDrawContext ctx, EfCanvas.Palette p, Item item, int rowY, int mouseX, int mouseY) {
        Ref<?> ref = item.ref();
        Rect slot = valueSlot(item, rowY);
        boolean editing = editor.isOpenFor(editorKey(item));
        boolean hot = !editing
                && mouseX >= slot.x()
                && mouseX < slot.x() + slot.w()
                && mouseY >= slot.y()
                && mouseY < slot.y() + slot.h();
        EndfieldWidgets.paintValueSlot(ctx, p, slot.x(), slot.y(), slot.w(), slot.h(), editing, hot);
        if (editing) {
            return;
        }
        boolean blank = false;
        String text;
        if (textEditable(ref)) {
            // 文本族（字符串 / 正则 / 格式化串）取的是原文：String.valueOf 会输出类名
            text = editableTextOf(ref);
            blank = text.isEmpty();
        } else {
            text = valueText(ref);
        }
        String shown = Ef.ellipsis(blank ? "空" : text, slot.w() - 8);
        Ef.text(ctx, shown, slot.x() + slot.w() - 6 - Ef.textWidth(shown), rowY + 5, blank ? p.ink2() : p.ink());
    }

    /**
     * 枚举下拉：panel 底、切角边，一行一个候选，当前项左侧压一条 3px 的当前位置线。
     *
     * <p>画在帧之后：它是浮层，可以越过正文裁剪与页脚；指在面板上时不再顺带出底下那一行的说明。
     */
    private void paintDropdown(VDrawContext ctx, EfCanvas.Palette p, int mouseX, int mouseY) {
        Line line = lineOfKey(dropdownKey);
        if (line == null) {
            dropdownKey = null;
            dropdownPart = -1;
            dropScroll = 0;
            return;
        }
        int part = dropdownPartOf(line.item().ref(), dropdownKey);
        List<String> labels = optionLabels(line.item(), part);
        if (labels.isEmpty()) {
            dropdownKey = null;
            dropdownPart = -1;
            dropScroll = 0;
            return;
        }
        int visible = Math.min(labels.size(), DROP_MAX_ROWS);
        dropScroll = Math.max(0, Math.min(labels.size() - visible, dropScroll));
        Rect box = dropdownRect(line, labels.size());
        Ef.fill(ctx, box.x(), box.y(), box.w(), box.h(), p.panel());
        Ef.cutOutline(ctx, box.x(), box.y(), box.w(), box.h(), p.ink(), p.cut());

        String current = currentOptionLabel(line.item(), part);
        for (int i = 0; i < visible; i++) {
            int index = dropScroll + i;
            int oy = box.y() + 2 + i * DROP_ROW_H;
            boolean hot = mouseX >= box.x() && mouseX <= box.x() + box.w() && mouseY >= oy && mouseY < oy + DROP_ROW_H;
            boolean active = labels.get(index).equals(current);
            if (hot) {
                Ef.fill(ctx, box.x() + 1, oy, box.w() - 2, DROP_ROW_H, Ef.mix(p.panel(), p.ink(), 0.08F));
            }
            if (active) {
                Ef.fill(ctx, box.x() + 1, oy, 3, DROP_ROW_H, p.trace());
            }
            Ef.text(ctx, Ef.ellipsis(labels.get(index), box.w() - 14), box.x() + 8, oy + 3,
                    active ? p.ink() : p.ink2());
        }
        if (labels.size() > visible) {
            int trackX = box.x() + box.w() - 3;
            int trackH = box.h() - 4;
            Ef.fill(ctx, trackX, box.y() + 2, 2, trackH, Ef.mix(p.panel(), p.ink(), 0.12F));
            int thumbH = Math.max(8, trackH * visible / labels.size());
            int thumbY = box.y() + 2 + (trackH - thumbH) * dropScroll / Math.max(1, labels.size() - visible);
            Ef.fill(ctx, trackX, thumbY, 2, thumbH, p.ink2());
        }

        if (mouseX >= box.x() && mouseX <= box.x() + box.w() && mouseY >= box.y() && mouseY <= box.y() + box.h()) {
            hovered = null;
        }
    }

    /** 数值行两端的加减键：切角是"能按"的形状标记，悬停时补底色。 */
    private void paintStep(VDrawContext ctx, EfCanvas.Palette p, int x, int y, String glyph, int mouseX, int mouseY) {
        boolean hot = mouseX >= x && mouseX < x + BTN && mouseY >= y + 2 && mouseY < y + 2 + BTN;
        if (hot) {
            Ef.cutFill(ctx, x, y + 2, BTN, BTN, p.chip(), p.cut());
        }
        Ef.cutOutline(ctx, x, y + 2, BTN, BTN, hot ? p.ink() : p.rule(), p.cut());
        Ef.text(ctx, glyph, x + 4, y + 4, hot ? p.ink() : p.ink2());
    }

    private void paintFooter(VDrawContext ctx, EfCanvas.Palette p) {
        String left = roots.size() + " 个配置  │  当前 " + all().size() + " 项设置";
        if (!query.trim().isEmpty()) {
            left = left + "  │  命中 " + shown().size() + " 项";
        }
        EndfieldWidgets.paintFooterBar(ctx, p, winX, winY, winW, winH, FOOT_H, left, "滚轮滚动  │  右键恢复默认");
    }

    private void paintFrame(VDrawContext ctx, EfCanvas.Palette p) {
        EndfieldWidgets.paintWindowFrame(ctx, p, winX, winY, winW, winH);
    }

    /** 悬停说明：按钮说明优先，其次显示完整路径与恢复默认提示。 */
    private void paintTip(VDrawContext ctx, EfCanvas.Palette p, int mouseX, int mouseY) {
        if (formatArgumentHover != null) {
            List<String> lines = new ArrayList<>();
            for (Component tip : StringFormat.generateTooltipsForArgument(formatArgumentHover.formattingArgument())) {
                lines.addAll(Ef.wrap(tip.getString(), 240));
            }
            formatArgumentHover = null;
            Ef.tooltip(ctx, p, lines, mouseX, mouseY, this.width, this.height);
            return;
        }
        if (itemTemplateHover) {
            itemTemplateHover = false;
            Ef.tooltip(ctx, p, List.of("打开物品编辑屏：附魔、名字、数量等组件在那里改"), mouseX, mouseY,
                    this.width, this.height);
            return;
        }
        if (regexPreviewHover != null) {
            String tip = regexPreviewHover;
            regexPreviewHover = null;
            Ef.tooltip(ctx, p, List.of(tip), mouseX, mouseY, this.width, this.height);
            return;
        }
        if (editorHover != null) {
            List<Component> lines = editorHover.generateColorStringPreview();
            editorHover = null;
            Ef.tooltipStyled(ctx, p, lines, mouseX, mouseY, this.width, this.height);
            return;
        }
        if (hovered == null) {
            return;
        }
        List<String> tip = new ArrayList<>();
        // 只写给人看的名字：翻译键取不到时才退回路径，内部键不该占一行
        tip.addAll(Ef.wrap(labelOf(hovered), 240));
        Ref<?> ref = hovered.ref();
        if (ref == null) {
            tip.add("这条路径解析不出配置项，只能看。");
        } else {
            if (!writable(ref)) {
                tip.add("这一类项在这里只能看。");
            }
            if (ref.hasDefaultValue()) {
                tip.add("右键恢复默认值");
            }
        }
        Ef.tooltip(ctx, p, tip, mouseX, mouseY, this.width, this.height);
    }

    // ------------------------------------------------------------------ 交互

    /** 行内编辑的身份：数值与文本各占一份，同一行同一用途的 key 不随帧变化。 */
    private static String editorKey(Item item) {
        return editorKey(item.ref(), item.path());
    }

    private static String editorKey(Ref<?> ref, String path) {
        Object val = ref.getValue();
        String kind = val instanceof WrapColor
                        || val instanceof WrapRGBA
                        || (val instanceof OptionalPrimitive<?> opt && opt.getValue() instanceof TextColor)
                ? "color:"
                : (textEditable(ref) ? "text:" : "num:");
        return kind + path;
    }

    private static String alphaKey(Ref<?> ref) {
        return "alpha:" + System.identityHashCode(ref);
    }

    /** 可选值内层枚举的下拉身份。 */
    private static String optionalKey(Ref<?> ref) {
        return "optional:" + System.identityHashCode(ref);
    }

    /** 枚举下拉的身份：芯片、候选与命中都认这一条路径。 */
    private static String enumKey(Item item) {
        return "enum:" + item.path();
    }

    /** 带候选字符串配置的下拉身份。 */
    private static String choicesKey(Item item) {
        return "choices:" + item.path();
    }

    /** 有限字符串候选的下拉身份：与枚举分开，两者候选来源不同。 */
    private static String enumKeyOf(Ref<?> ref) {
        return "stringenum:" + System.identityHashCode(ref);
    }

    /**
     * 按键捕获：等按键的那一条把下一次按键写进配置。
     *
     * <p>Esc 取消而不是绑成 Esc——绑一个"取消键"上去，这一项就再也改不回来了。
     */
    private boolean captureKey(EfWidget self, int keyCode, int scanCode, int modifiers) {
        if (keyTarget == null) {
            return false;
        }
        if (keyCode == KeyCode.KEY_ESCAPE) {
            keyTarget = null;
            return true;
        }
        String name = KeyCode.getNameForKey(keyCode);
        if (name != null) {
            Item target = itemOfPath(keyTarget);
            if (target != null && target.ref() instanceof KeyBindRef bindRef) {
                bindRef.set(new MultiKeyBind(new String[] {name}, false, false));
            }
            keyTarget = null;
        }
        return true;
    }

    /** 按路径找回当前列表里的那一项：列表重建后对象会换，路径不会。 */
    private Item itemOfPath(String path) {
        for (Line line : lines) {
            if (line.item() != null && line.item().path().equals(path)) {
                return line.item();
            }
        }
        return null;
    }

    private boolean click(EfWidget self, double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;

        // 这一下是给输入框的：容器会把它继续派下去，这里挪走焦点就打不进去了
        if (editor.contains(mouseX, mouseY)) {
            return false;
        }
        // 点到别处：文本这一类要在这里提交，之后再按新的目标处理这一下
        editor.close();
        buildLines();

        if (dropdownKey != null) {
            boolean picked = clickDropdown(mx, my, button);
            dropdownKey = null;
            dropdownPart = -1;
            dropScroll = 0;
            if (picked) {
                return true;
            }
            // 面板外的一击只关面板，不穿透到下面的控件；命令栏是例外，照旧切页
            return my >= winY && my < winY + barH && clickTabBar(mx, my);
        }
        if (palettePanel.target() instanceof Item target && target.ref() != null) {
            if (palettePanel.click(winX, winY, winW, winH, ROW_H, ValueCodec.colorOf(target.ref().getValue()),
                    mx, my, button, rgb -> writePaletteColor(target.ref(), rgb))) {
                return true;
            }
            palettePanel.close();
            return my >= winY && my < winY + barH && clickTabBar(mx, my);
        }
        if (my >= winY && my < winY + barH) {
            return clickTabBar(mx, my);
        }
        // 搜索框那一行留给原版输入框，否则点不进焦点
        if (my >= searchTop() && my < bodyTop()) {
            return false;
        }
        if (my < bodyTop() || my >= bodyBottom()) {
            return false;
        }
        if (mx >= winX && mx < winX + railW) {
            return button == 0 && clickRail(mx, my);
        }
        return clickItems(mx, my, button);
    }

    /** 按 key 找回布局里的那一行：被筛掉或滚出可视区的行没有可贴的位置，也就算找不到。 */
    private Line lineOfKey(String key) {
        if (key == null) {
            return null;
        }
        for (Line line : lines) {
            if (line.item() == null || !dropdownKeyMatches(line.item(), key)) {
                continue;
            }
            if (line.y() + ROW_H < bodyTop() || line.y() > bodyBottom()) {
                return null;
            }
            return line;
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void writePaletteColor(Ref<?> ref, int rgb) {
        ((Ref) ref).setValue(ValueCodec.withRgb(ref.getValue(), rgb));
    }

    private static int dropdownPartOf(Ref<?> ref, String key) {
        int part = recordPartOf(ref, key);
        return part >= 0 ? part : -1;
    }

    /** 这一行的下拉身份是否就是 {@code key}。 */
    private static boolean dropdownKeyMatches(Item item, String key) {
        if (key == null) {
            return false;
        }
        if (key.equals(enumKey(item))
                || key.equals(choicesKey(item))
                || key.equals(enumKeyOf(item.ref()))
                || key.equals(optionalKey(item.ref()))) {
            return true;
        }
        return recordPartOf(item.ref(), key) >= 0;
    }

    /** 下拉里的一击：落在候选行上才写回，面板空白处只收面板。 */
    private boolean clickDropdown(int mx, int my, int button) {
        if (button != 0) {
            return false;
        }
        Line line = lineOfKey(dropdownKey);
        if (line == null) {
            return false;
        }
        int part = dropdownPartOf(line.item().ref(), dropdownKey);
        List<String> labels = optionLabels(line.item(), part);
        if (labels.isEmpty()) {
            return false;
        }
        int visible = Math.min(labels.size(), DROP_MAX_ROWS);
        Rect box = dropdownRect(line, labels.size());
        if (mx < box.x() || mx > box.x() + box.w()) {
            return false;
        }
        int top = box.y() + 2;
        int row = (my - top) / DROP_ROW_H;
        int index = dropScroll + row;
        if (my < top || row < 0 || row >= visible || index < 0 || index >= labels.size()) {
            return false;
        }
        selectOption(line.item(), index, part);
        return true;
    }

    /** 分类条：点当前项什么都不做，点别的项当场换页，点右侧切深浅。 */
    private boolean clickTabBar(int mx, int my) {
        String hit = tabBar.click(winX, winY, winW, "Config", light(), mx, my);
        if (EndfieldTabBar.THEME.equals(hit)) {
            if (settings != null) {
                settings.light(!settings.light());
            }
            return true;
        }
        if (hit == null || "Config".equals(hit)) {
            return false;
        }
        GuiStyles.switchTab(this, hit, settings);
        return true;
    }

    private boolean clickRail(int mx, int my) {
        if (my < railRowsTop()) {
            return false;
        }
        int offset = my - railRowsTop() + railScroll;
        int index = offset / RAIL_STEP;
        if (index < 0 || index >= roots.size() || offset % RAIL_STEP >= RAIL_CHIP_H) {
            return false;
        }
        int chipX = winX + 8;
        int chipW = Math.max(40, railW - 16);
        if (mx < chipX || mx > chipX + chipW) {
            return false;
        }
        Config target = roots.get(index);
        if (target != selected) {
            selected = target;
            itemScroll = 0;
        }
        return true;
    }

    private boolean clickItems(int mx, int my, int button) {
        if (my < listTop()) {
            return false;
        }
        for (Line line : lines) {
            if (line.item() == null || my < line.y() || my >= line.y() + line.height()) {
                continue;
            }
            return apply(line.item(), line.y(), mx, my, button);
        }
        return false;
    }

    /**
     * 点开一格的输入框：这里只登记目标与几何，原版控件要等 tick 才建。
     *
     * <p>数值逐字写回（解析不了的半截输入由 {@link #commitNumber} 忽略）；文本等 Enter 或点到别处
     * 才交一次——每敲一个字就写一次配置，宿主的更新监听会被打成筛子。
     */
    private void openEditor(Item item, int rowY) {
        Ref<?> ref = item.ref();
        Rect slot = valueSlot(item, rowY);
        boolean color = ref.getValue() instanceof WrapColor;
        boolean text = textEditable(ref);
        String seed;
        if (color) {
            seed = ValueCodec.hexOf(ValueCodec.colorOf(ref.getValue()));
        } else if (text) {
            seed = editableTextOf(ref);
        } else {
            seed = valueText(ref);
        }
        editor.open(this, editorKey(item), slot.x(), slot.y(), slot.w(), seed, !text, text ? 1024 : 24, value -> {
            if (color) {
                commitColor(ref, value);
            } else if (text) {
                commitText(ref, value);
            } else {
                commitNumber(ref, value);
            }
        });
    }

    /**
     * 打开宿主的注册表选择屏。
     *
     * <p>回调里重建一份 {@code EntrySet} 再写回：宿主按 {@code (registry, set)} 判等，
     * 原地改那个 {@code set} 不会触发更新监听。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void openEntrySet(Ref<?> ref, EntrySet<?> set) {
        HostEditors.openEntrySet(
                set, next -> ((Ref) ref).setValue(next), NewClickGui.popupFactory(settings), "名单选择屏打开失败");
    }

    /**
     * 打开宿主的列表 / 键值 / 有界表编辑屏。
     *
     * <p>具体类型包装与实时写回适配器统一收口在 {@link HostEditors}。
     */
    private void openCountEditor(Ref<?> ref, String path) {
        Object raw = ref.getValue();
        if (raw instanceof PrimitiveMap<?, ?>
                || raw instanceof PrimitivePairList<?, ?>
                || raw instanceof EnumPrimitiveList<?, ?>
                || raw instanceof BoundedPrimitiveMap<?, ?>) {
            HostEditors.openValueWidgetScreen(ref, path, NewClickGui.popupFactory(settings), "列表或映射编辑器打开失败");
            return;
        }
        HostEditors.openListEditor(ref, path, NewClickGui.popupFactory(settings));
    }

    /** 物品模板与注册表条目的就地编辑：非即时模式，Enter 或点到别处才交一次。 */
    private void openTextValueEditor(Item item, int rowY) {
        Ref<?> ref = item.ref();
        Rect slot = valueSlot(item, rowY);
        editor.open(this, editorKey(item), slot.x(), slot.y(), slot.w(), textValueRowOf(ref), false,
                text -> commitTextValue(ref, text));
    }

    /**
     * 值框写回：物品模板只换类型（数据组件原样保留），注册表条目按「注册表|条目」解析。
     *
     * <p>解析不出来就不落配置：打到一半的 id 不该把这一项变成空气。
     */
    private static void commitTextValue(Ref<?> ref, String text) {
        ValueCodec.commitText(ref, text);
    }

    /** 位置行的一击：行尾切 type，两行数值槽各管一格。 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean applyWidgetPos(Ref<?> ref, WidgetPos pos, int rowY, int mx, int my, int button) {
        if (button != 0) {
            return false;
        }
        int right = itemsRight();
        String mode = pos.getType() == 0 ? "百分比" : "绝对";
        int modeW = Ef.textWidth(mode) + 10;
        if (mx >= right - modeW && mx <= right && my >= rowY + 3 && my < rowY + 15) {
            ((Ref) ref).setValue(pos.withType((pos.getType() + 1) % 2));
            return true;
        }
        int index = Math.max(0, Math.min(1, (my - rowY) / ROW_H));
        Vec2 pair = pos.getType() == 0 ? pos.toPercentage() : pos.toLength();
        double current = index == 0 ? pair.x() : pair.y();
        String text = formatNumber(current);
        int width = Math.max(VALUE_W, Ef.textWidth(text) + 12);
        int slotX = right - modeW - 6 - width;
        int slotY = rowY + index * ROW_H + FieldEditor.FIELD_TOP;
        if (mx >= slotX && mx < slotX + width && my >= slotY && my < slotY + FieldEditor.FIELD_H) {
            openWidgetPosEditor(ref, pos, index, slotX, slotY, width, current);
            return true;
        }
        return false;
    }

    /** 位置配置里某一格的数值编辑：几何与绘制共用同一份。 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void openWidgetPosEditor(
            Ref<?> ref, WidgetPos pos, int part, int x, int y, int w, double current) {
        editor.open(this, widgetPosKey(ref, part), x, y, w, formatNumber(current), true, value -> {
            try {
                double next = Double.parseDouble(value.trim().replace(',', '.'));
                if (!Double.isFinite(next)) {
                    return;
                }
                Vec2 pair = pos.getType() == 0 ? pos.toPercentage() : pos.toLength();
                Vec2 updated = part == 0 ? new Vec2(next, pair.y()) : new Vec2(pair.x(), next);
                ((Ref) ref).setValue(pos.getType() == 0 ? pos.withPercentage(updated) : pos.withLength(updated));
            } catch (NumberFormatException ignored) {
                // 半截输入不是错误，继续等下一个字
            }
        });
    }

    /** 带标签向量的一击：每格一个数值槽，点开就地编辑。 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean applyLabelVec(
            Ref<?> ref, int rowY, int mx, int my, int button, int count, double x, double y2, double z) {
        if (button != 0) {
            return false;
        }
        int right = itemsRight();
        double[] values = {x, y2, z};
        int index = Math.max(0, Math.min(count - 1, (my - rowY) / ROW_H));
        String text = formatNumber(values[index]);
        int width = Math.max(VALUE_W, Ef.textWidth(text) + 12);
        int slotX = right - width;
        int slotY = rowY + index * ROW_H + FieldEditor.FIELD_TOP;
        if (mx >= slotX && mx < slotX + width && my >= slotY && my < slotY + FieldEditor.FIELD_H) {
            openLabelVecEditor(ref, index, slotX, slotY, width, values[index]);
            return true;
        }
        return false;
    }

    /** 带标签向量里某一格的数值编辑：几何与绘制共用同一份。 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void openLabelVecEditor(Ref<?> ref, int part, int x, int y, int w, double current) {
        editor.open(this, labelVecKey(ref, part), x, y, w, formatNumber(current), true, value -> {
            try {
                double next = Double.parseDouble(value.trim().replace(',', '.'));
                if (!Double.isFinite(next)) {
                    return;
                }
                Object raw = ref.getValue();
                if (raw instanceof LabelVec2 label2) {
                    Vec2 data = part == 0
                            ? new Vec2(next, label2.data().y())
                            : new Vec2(label2.data().x(), next);
                    ((Ref) ref).setValue(label2.withData(data));
                } else if (raw instanceof LabelVec3 label3) {
                    Vec3 data = switch (part) {
                        case 0 -> new Vec3(next, label3.data().y(), label3.data().z());
                        case 1 -> new Vec3(label3.data().x(), next, label3.data().z());
                        default -> new Vec3(label3.data().x(), label3.data().y(), next);
                    };
                    ((Ref) ref).setValue(label3.withData(data));
                }
            } catch (NumberFormatException ignored) {
                // 半截输入不是错误，继续等下一个字
            }
        });
    }

    /** 打开实体类型集合编辑屏。 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void openEntityTypeSet(Ref<?> ref, EntityTypeEntrySet set) {
        HostEditors.openEntityTypeSet(set, next -> ((Ref) ref).setValue(next), NewClickGui.popupFactory(settings));
    }

    /** 打开 GUI 设置的详细页：本界面自己的屏，不是宿主那一套。 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void openGuiSetting(Ref<?> ref) {
        try {
            WrapperConfigRef<GuiSetting> target =
                    new WrapperConfigRef<>((Ref<GuiSetting>) ref, null, new String[] {"gui-setting"});
            HostScreens.open(new GuiSettingPage(target, settings));
        } catch (Throwable e) {
            me.matl114.utils.Debug.info("[SFH+] GUI 设置页打开失败: " + e);
        }
    }

    /** 记录里某一格的数值编辑：几何与绘制共用同一份。 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void openRecordEditor(Ref<?> ref, int part, int rowY, double current) {
        editor.open(this, recordKey(ref, part), itemsRight() - VALUE_W,
                rowY + part * ROW_H + FieldEditor.FIELD_TOP, VALUE_W, formatNumber(current), true, value -> {
                    Double next = ValueCodec.tryParseNumber(value);
                    if (next != null) {
                        ((Ref) ref).setValue(withNumericPart(ref.getValue(), part, next));
                    }
                });
    }

    /** 可选记录里某一格的数值编辑：几何与绘制共用同一份。 */
    private void openOptionalRecordEditor(Ref<?> ref, int subIndex, int rowY, double current) {
        int partIdx = OPTIONAL_RECORD_BASE + subIndex;
        int partY = rowY + (subIndex + 1) * ROW_H;
        editor.open(this, recordKey(ref, partIdx), itemsRight() - VALUE_W,
                partY + FieldEditor.FIELD_TOP, VALUE_W, formatNumber(current), true, value -> {
                    Double next = ValueCodec.tryParseNumber(value);
                    if (next != null) {
                        RecordFieldOps.writeOptionalRecordNumber(ref, subIndex, next);
                    }
                });
    }

    /** 可选值内层的数值编辑：几何由调用方算好，与绘制共用同一份。 */
    private void openOptionalEditor(Ref<?> ref, int rowY, int x, int w, double current) {
        editor.open(this, editorKey(ref, optionalKey(ref)), x, rowY + FieldEditor.FIELD_TOP, w,
                formatNumber(current), true, value -> {
                    Double next = ValueCodec.tryParseNumber(value);
                    if (next != null) {
                        writeOptionalNumber(ref, next);
                    }
                });
    }

    /** RGB / RGBA / 可选颜色共用的色块与十六进制点击命中。 */
    private boolean hitColorBox(Item item, int anchorRight, int mx, int my, int button, int rowY) {
        if (button != 0 || my < rowY + FieldEditor.FIELD_TOP || my >= rowY + FieldEditor.FIELD_TOP + FieldEditor.FIELD_H) {
            return false;
        }
        Ref<?> ref = item.ref();
        EndfieldWidgets.ColorGeometry box = EndfieldWidgets.colorGeometry(anchorRight);
        if (mx >= box.chipX() && mx < box.chipX() + box.swatchHitW()) {
            palettePanel.open(item, box.swatchX(), box.swatchW(), rowY);
            return true;
        }
        if (mx >= box.hexFieldX() && mx < box.hexFieldX() + box.hexFieldW()) {
            palettePanel.close();
            editor.open(this, editorKey(item), box.hexFieldX(), rowY + FieldEditor.FIELD_TOP, box.hexFieldW(),
                    ValueCodec.hexOf(ValueCodec.colorOf(ref.getValue())), true, 16, text -> commitColor(ref, text));
            return true;
        }
        return false;
    }

    private boolean hitRgba(Item item, WrapRGBA rgba, int right, int mx, int my, int button, int rowY) {
        if (button != 0 || my < rowY + FieldEditor.FIELD_TOP || my >= rowY + FieldEditor.FIELD_TOP + FieldEditor.FIELD_H) {
            return false;
        }
        EndfieldWidgets.RgbaGeometry geo = EndfieldWidgets.rgbaGeometry(right);
        if (mx >= geo.alphaFieldX() && mx < geo.alphaFieldX() + geo.alphaFieldW()) {
            palettePanel.close();
            editor.open(this, alphaKey(item.ref()), geo.alphaFieldX(), rowY + FieldEditor.FIELD_TOP,
                    geo.alphaFieldW(), Integer.toString(rgba.alpha()), true, 8,
                    text -> commitAlpha(item.ref(), text));
            return true;
        }
        return hitColorBox(item, geo.alphaFieldX() - 4, mx, my, button, rowY);
    }

    private boolean apply(Item item, int rowY, int mx, int my, int button) {
        Ref<?> ref = item.ref();
        if (ref == null) {
            return false;
        }
        if (button == 1) {
            if (ref.hasDefaultValue()) {
                ref.resetValue();
                return true;
            }
            return false;
        }
        int right = itemsRight();
        if (ref instanceof FlagRef flag) {
            int sx = right - 20;
            if (mx >= sx - 2 && mx <= sx + 22) {
                flag.toggle();
                return true;
            }
            return false;
        }
        if (isNumeric(ref)) {
            boolean band = my >= rowY + 2 && my < rowY + 2 + BTN;
            int minusX = right - BTN * 2 - 4;
            int plusX = right - BTN;
            if (band && mx >= minusX && mx < minusX + BTN) {
                nudge(ref, -stepOf(ref));
                return true;
            }
            if (band && mx >= plusX && mx < plusX + BTN) {
                nudge(ref, stepOf(ref));
                return true;
            }
            // 两个键之外还有值槽：槽右沿正好让到减号左边，点谁是谁
            if (inValueSlot(item, rowY, mx, my)) {
                openEditor(item, rowY);
                return true;
            }
            return false;
        }
        if (ref instanceof EnumRef<?>) {
            int chipX = right - enumWidth(ref);
            if (mx >= chipX && mx <= right && my >= rowY + 3 && my < rowY + 15) {
                dropdownKey = enumKey(item);
                return true;
            }
            return false;
        }
        if (textValueRow(ref)) {
            if (button == 0 && inValueSlot(item, rowY, mx, my)) {
                openTextValueEditor(item, rowY);
                return true;
            }
            return false;
        }
        if (countRow(ref)) {
            String label = countOf(ref) + " 项   ▾";
            int width = Ef.textWidth(label) + 10;
            if (button == 0 && mx >= right - width && mx <= right && my >= rowY + 3 && my < rowY + 15) {
                openCountEditor(ref, itemPath(ref));
                return true;
            }
            return false;
        }
        if (booleanParts(ref.getValue()) != null) {
            // 开关只吃行尾那一段：与单值开关同一块命中区，留白里的点击不算
            if (button != 0 || mx < right - 24 || mx > right) {
                return false;
            }
            boolean[] bools = booleanParts(ref.getValue());
            int index = Math.max(0, Math.min(bools.length - 1, (my - rowY) / ROW_H));
            ((Ref) ref).setValue(withPart(ref.getValue(), index, !bools[index]));
            return true;
        }
        if (ref.getValue() instanceof WidgetPos pos) {
            return applyWidgetPos(ref, pos, rowY, mx, my, button);
        }
        if (ref.getValue() instanceof GuiSetting) {
            if (button != 0) {
                return false;
            }
            int width = Ef.textWidth(GUI_SETTING_CHIP) + 10;
            if (mx >= right - width && mx <= right && my >= rowY + 3 && my < rowY + 15) {
                openGuiSetting(ref);
                return true;
            }
            return false;
        }
        if (ref.getValue() instanceof StringEnum choice) {
            if (button != 0) {
                return false;
            }
            String text = choice.value() + " ▾";
            int width = Ef.textWidth(text) + 10;
            if (mx >= right - width && mx <= right && my >= rowY + 3 && my < rowY + 15) {
                dropdownKey = enumKeyOf(ref);
                return true;
            }
            return false;
        }
        if (ref.getValue() instanceof EntityTypeEntrySet set) {
            if (button != 0) {
                return false;
            }
            String label = set.set().size() + " 项   ▾";
            int width = Ef.textWidth(label) + 10;
            if (mx >= right - width && mx <= right && my >= rowY + 3 && my < rowY + 15) {
                openEntityTypeSet(ref, set);
                return true;
            }
            return false;
        }
        if (ref.getValue() instanceof ItemStackTemplate template) {
            if (button != 0) {
                return false;
            }
            int btnX = right - BTN;
            if (mx >= btnX && mx < btnX + BTN && my >= rowY + 2 && my < rowY + 2 + BTN) {
                HostEditors.openItemStackEditor(template.itemStack(), edited -> {
                    template.setItemStack(edited);
                    ((Ref) ref).setValue(template);
                });
                return true;
            }
            Rect slot = trailingBtnSlot(right, rowY, 1);
            if (inRect(slot, mx, my)) {
                editor.open(this, editorKey(item), slot.x(), slot.y(), slot.w(),
                        template.itemId().toString(), false, 1024, text -> commitTextValue(ref, text));
                return true;
            }
            return false;
        }
        if (ref.getValue() instanceof RegistryRegex<?> || ref.getValue() instanceof WeakRegistryRegex<?>) {
            if (button != 0) {
                return false;
            }
            int btnX = right - BTN;
            if (mx >= btnX && mx < btnX + BTN && my >= rowY + 2 && my < rowY + 2 + BTN) {
                HostEditors.openRegistryRegexPreview(ref, item.path(), NewClickGui.popupFactory(settings));
                return true;
            }
            Rect slot = trailingBtnSlot(right, rowY, 1);
            if (inRect(slot, mx, my)) {
                editor.open(this, editorKey(item), slot.x(), slot.y(), slot.w(),
                        editableTextOf(ref), false, 1024, text -> commitTextValue(ref, text));
                return true;
            }
            return false;
        }
        if (ref.getValue() instanceof StringFormat format) {
            if (button != 0) {
                return false;
            }
            int cursor = right;
            int editX = -1;
            int argX = -1;
            if (format.colorString()) {
                editX = cursor - BTN;
                cursor = editX - 2;
            }
            if (!format.formattingArgument().isEmpty()) {
                argX = cursor - BTN;
            }
            if (argX >= 0 && mx >= argX && mx < argX + BTN && my >= rowY + 2 && my < rowY + 2 + BTN) {
                return true;
            }
            if (editX >= 0 && mx >= editX && mx < editX + BTN && my >= rowY + 2 && my < rowY + 2 + BTN) {
                HostEditors.openFormattingWiki();
                return true;
            }
            int tailCount = (editX >= 0 ? 1 : 0) + (argX >= 0 ? 1 : 0);
            Rect slot = trailingBtnSlot(right, rowY, tailCount);
            if (inRect(slot, mx, my)) {
                editor.open(this, editorKey(item), slot.x(), slot.y(), slot.w(),
                        format.formatString(), false, 1024, text -> commitText(ref, text));
                return true;
            }
            return false;
        }
        if (ref instanceof StringRef && !StringChoicesRegistry.ofPath(item.path()).isEmpty()) {
            if (button != 0) {
                return false;
            }
            Rect slot = valueSlot(item, rowY);
            if (inRect(slot, mx, my)) {
                if (mx >= slot.x() + slot.w() - ARROW_W) {
                    dropdownKey = choicesKey(item);
                    dropScroll = 0;
                } else {
                    openEditor(item, rowY);
                }
                return true;
            }
            return false;
        }
        if (ref.getValue() instanceof LabelVec2 label2) {
            return applyLabelVec(ref, rowY, mx, my, button, 2,
                    label2.data().x(), label2.data().y(), 0);
        }
        if (ref.getValue() instanceof LabelVec3 label3) {
            return applyLabelVec(ref, rowY, mx, my, button, 3,
                    label3.data().x(), label3.data().y(), label3.data().z());
        }
        if (recordParts(ref.getValue()) != null) {
            Object[] parts = recordParts(ref.getValue());
            int index = Math.max(0, Math.min(parts.length - 1, (my - rowY) / ROW_H));
            Object part = parts[index];
            if (part instanceof Number number) {
                if (button == 0 && mx >= right - VALUE_W && mx <= right && my >= rowY + index * ROW_H + 3
                        && my < rowY + index * ROW_H + 15) {
                    openRecordEditor(ref, index, rowY, number.doubleValue());
                    return true;
                }
                return false;
            }
            if (part instanceof ConfigEnum && button == 0) {
                dropdownKey = recordKey(ref, index);
                dropdownPart = index;
                return true;
            }
            return false;
        }
        if (ref.getValue() instanceof EntrySet<?> set) {
            String label = set.set().size() + " 项   ▾";
            int width = Ef.textWidth(label) + 10;
            if (button == 0 && mx >= right - width && mx <= right && my >= rowY + 3 && my < rowY + 15) {
                openEntrySet(ref, set);
                return true;
            }
            return false;
        }
        if (optionalRow(ref)) {
            OptionalPrimitive<?> optional = (OptionalPrimitive<?>) ref.getValue();
            Object inner = optionalInner(ref);
            if (my < rowY + ROW_H) {
                // 行尾开关：只切「这一项是否生效」，不碰内层值
                if (mx >= right - 22 && mx <= right + 2) {
                    setOptionalPresent(ref, !optional.isPresent());
                    return true;
                }
                if (inner == null) {
                    return false;
                }
                int innerRight = right - 28;
                if (inner instanceof ConfigEnum) {
                    String text = enumLabel((ConfigEnum) inner) + " ▾";
                    int width = Ef.textWidth(text) + 10;
                    if (mx >= innerRight - width && mx <= innerRight && my >= rowY + 3 && my < rowY + 15) {
                        dropdownKey = optionalKey(ref);
                        dropScroll = 0;
                        return true;
                    }
                    return false;
                }
                if (inner instanceof Number number) {
                    String text = formatNumber(number.doubleValue());
                    int width = Math.max(VALUE_W, Ef.textWidth(text) + 12);
                    if (mx >= innerRight - width && mx < innerRight && my >= rowY + 3 && my < rowY + 15) {
                        openOptionalEditor(ref, rowY, innerRight - width, width, number.doubleValue());
                        return true;
                    }
                    return false;
                }
                if (inner instanceof Boolean flag) {
                    if (button == 0 && mx >= innerRight - 24 && mx <= innerRight) {
                        RecordFieldOps.writeOptionalBoolean(ref, !flag);
                        return true;
                    }
                    return false;
                }
                if (inner instanceof TextColor) {
                    return hitColorBox(item, innerRight, mx, my, button, rowY);
                }
                if (inner instanceof CharSequence text) {
                    Rect slot = optionalTextSlot(innerRight, rowY);
                    if (button == 0 && inRect(slot, mx, my)) {
                        editor.open(this, editorKey(item), slot.x(), slot.y(), slot.w(),
                                text.toString(), false, 1024,
                                draft -> RecordFieldOps.writeOptionalString(ref, draft.trim()));
                        return true;
                    }
                    return false;
                }
                return false;
            }
            if (inner == null) {
                return false;
            }
            int subIndex = (my - rowY - ROW_H) / ROW_H;
            Object[] optParts = recordParts(inner);
            if (optParts != null && subIndex >= 0 && subIndex < optParts.length) {
                int partY = rowY + (subIndex + 1) * ROW_H;
                Object subPart = optParts[subIndex];
                if (subPart instanceof Number number) {
                    if (button == 0 && mx >= right - VALUE_W && mx <= right && my >= partY + 3 && my < partY + 15) {
                        openOptionalRecordEditor(ref, subIndex, rowY, number.doubleValue());
                        return true;
                    }
                    return false;
                }
                if (subPart instanceof ConfigEnum && button == 0) {
                    int partIdx = OPTIONAL_RECORD_BASE + subIndex;
                    dropdownKey = recordKey(ref, partIdx);
                    dropdownPart = partIdx;
                    dropScroll = 0;
                    return true;
                }
                return false;
            }
            boolean[] optBools = booleanParts(inner);
            if (optBools != null && subIndex >= 0 && subIndex < optBools.length) {
                if (button == 0 && mx >= right - 24 && mx <= right) {
                    RecordFieldOps.writeOptionalRecordPart(ref, subIndex, !optBools[subIndex]);
                    return true;
                }
            }
            return false;
        }
        if (ref instanceof KeyBindRef bindRef) {
            int width = Ef.textWidth(keyText(bindRef)) + 12;
            if (mx >= right - width && mx <= right && my >= rowY + 3 && my < rowY + 15) {
                if (button == 0) {
                    keyTarget = item.path();
                }
                return true;
            }
            return false;
        }
        if (ref.getValue() instanceof WrapColor) {
            return hitColorBox(item, right, mx, my, button, rowY);
        }
        if (ref.getValue() instanceof WrapRGBA rgba) {
            return hitRgba(item, rgba, right, mx, my, button, rowY);
        }
        if (textEditable(ref)) {
            if (inValueSlot(item, rowY, mx, my)) {
                openEditor(item, rowY);
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean wheel(EfWidget self, double mouseX, double mouseY, double amount) {
        if (dropdownKey != null) {
            Line line = lineOfKey(dropdownKey);
            if (line != null) {
                int part = dropdownPartOf(line.item().ref(), dropdownKey);
                List<String> labels = optionLabels(line.item(), part);
                int visible = Math.min(labels.size(), DROP_MAX_ROWS);
                if (labels.size() > visible) {
                    int step = (int) Math.signum(amount);
                    dropScroll = Math.max(0, Math.min(labels.size() - visible, dropScroll - step));
                    return true;
                }
            }
        }
        int my = (int) mouseY;
        if (my < bodyTop() || my >= bodyBottom()) {
            return false;
        }
        // 滚动会挪行：贴行的输入框与下拉都按旧坐标算，先收起来（文本顺手提交）
        editor.close();
        dropdownKey = null;
        dropdownPart = -1;
        dropScroll = 0;
        palettePanel.close();
        buildLines();
        int step = (int) (Math.signum(amount) * 18.0D);
        if (mouseX >= winX && mouseX < winX + railW) {
            railScroll = Math.max(0, Math.min(maxRailScroll(), railScroll - step));
        } else {
            itemScroll = Math.max(0, Math.min(maxItemScroll(), itemScroll - step));
        }
        return true;
    }
}