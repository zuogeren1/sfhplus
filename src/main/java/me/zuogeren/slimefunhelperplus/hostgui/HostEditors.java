package me.zuogeren.slimefunhelperplus.hostgui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import me.matl114.gui.GenericScreen;
import me.matl114.gui.basic.ButtonAction;
import me.matl114.gui.basic.DrawableWidget;
import me.matl114.gui.basic.ExecutableWidget;
import me.matl114.gui.basic.SubScreenWidget;
import me.matl114.gui.basic.TooltipHandler;
import me.matl114.gui.elements.IconElement;
import me.matl114.gui.interfaces.ElementFactory;
import me.matl114.gui.presets.choices.EntityTypeEntrySetScreen;
import me.matl114.gui.presets.choices.RegistryChooseScreen;
import me.matl114.gui.presets.choices.RegistrySelectScreen;
import me.matl114.hacks.InvTasks;
import me.matl114.hacks.modules.combat.TargetSelector;
import me.matl114.hacks.modules.task.ConfigManager;
import me.matl114.hacks.utils.config.EntityTypeEntrySet;
import me.matl114.hacks.utils.config.EntrySet;
import me.matl114.hacks.utils.config.NBTTypes;
import me.matl114.hacks.utils.config.PrimitiveList;
import me.matl114.hacks.utils.config.PrimitiveMap;
import me.matl114.hacks.utils.config.Regex;
import me.matl114.hacks.utils.config.RegexList;
import me.matl114.hacks.utils.config.RegistryRegex;
import me.matl114.hacks.utils.config.StringFormat;
import me.matl114.hacks.utils.config.WeakRegistryRegex;
import me.matl114.hacks.utils.gui.ClassicClickGuiScreen;
import me.matl114.managers.config.ListRef;
import me.matl114.managers.config.NBTType;
import me.matl114.managers.config.Ref;
import me.matl114.utils.Debug;
import me.matl114.utils.config.AttrKeyValue;
import me.matl114.utils.config.BaseAttrKeyValue;
import me.matl114.utils.config.ValueAccessor;
import me.matl114.utils.config.WrapperFactory;
import me.matl114.utils.config.kv.TypeConvertAttrKeyValue;
import me.matl114.utils.config.kv.WrapperAttrKeyValue;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 宿主弹窗与复杂类型编辑器的统一调度入口。
 *
 * <p>收拢列表/键值表/有界开关表/注册表集合/实体集合/快照与老界面回退的打开逻辑，
 * 避免每个配置屏各自重复包 {@link WrapperAttrKeyValue} 或直接拼宿主预设屏构造参数。
 */
public final class HostEditors {
    private static final int DEFAULT_EDITOR_W = 300;
    private static final int DEFAULT_EDITOR_H = 20;

    private HostEditors() {}

    /** 打开注册表多选屏，确认时按原注册表重建 {@link EntrySet} 回传。 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void openEntrySet(
            EntrySet<?> set, Consumer<EntrySet<?>> onConfirm, ElementFactory factory, String errorTag) {
        if (set == null) {
            return;
        }
        try {
            HostScreens.open(new RegistrySelectScreen(
                    set.registry(),
                    set.set(),
                    (Consumer<Set>) selected -> {
                        if (selected != null && onConfirm != null) {
                            onConfirm.accept(new EntrySet(set.registry(), selected));
                        }
                    },
                    factory));
        } catch (Throwable e) {
            Debug.info("[SFH+] " + errorTag + ": " + e);
        }
    }

    /** 打开实体类型分组集合编辑屏，确认时整份回传。 */
    public static void openEntityTypeSet(
            EntityTypeEntrySet set, Consumer<EntityTypeEntrySet> onConfirm, ElementFactory factory) {
        try {
            HostScreens.open(new EntityTypeEntrySetScreen(set, onConfirm, factory));
        } catch (Throwable e) {
            Debug.info("[SFH+] 实体集合编辑屏打开失败: " + e);
        }
    }

    /**
     * 打开宿主 {@code generateValueWidget} 产出的列表、键值表、枚举列表或有界开关表编辑器。
     *
     * <p>若宿主控件本身包着一只全宽「点击编辑列表」按钮，这里直接替用户点开下级大编辑屏，
     * 省去中间再弹一个单按钮小窗的多余点击；其余复合控件照常包进居中宿主屏。
     */
    public static void openValueWidgetScreen(
            Ref<?> ref, String keyName, ElementFactory factory, String errorTag) {
        if (ref == null) {
            return;
        }
        try {
            openAttrWidget(HostControls.keyValue(ref, keyName), factory, errorTag);
        } catch (Throwable e) {
            Debug.info("[SFH+] " + errorTag + ": " + e);
        }
    }

    /**
     * 为只有 {@link ValueAccessor}、没有 {@link Ref} 的 {@link PrimitiveMap}（如 Baritone 方块替换表）打开列表编辑器。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void openPrimitiveMapAccessor(
            ValueAccessor<?> accessor, String keyName, ElementFactory factory, String errorTag) {
        if (accessor == null) {
            return;
        }
        try {
            Object current = accessor.getValue();
            if (!(current instanceof PrimitiveMap<?, ?> map)) {
                return;
            }
            BaseAttrKeyValue<PrimitiveMap<Object, Object>> attr =
                    PrimitiveMap.TYPE.createAttrKeyValue(keyName, (PrimitiveMap<Object, Object>) map);
            attr.addListener(updated -> {
                if (updated != null) {
                    ((ValueAccessor) accessor).setValue(updated);
                }
            });
            openAttrWidget(attr, factory, errorTag);
        } catch (Throwable e) {
            Debug.info("[SFH+] " + errorTag + ": " + e);
        }
    }

    private static void openAttrWidget(AttrKeyValue<?> source, ElementFactory factory, String errorTag) {
        DrawableWidget widget = source.generateValueWidget(factory, 0, 0, DEFAULT_EDITOR_W, DEFAULT_EDITOR_H);
        if (widget == null) {
            return;
        }
        if (widget instanceof SubScreenWidget sub) {
            ExecutableWidget opener = null;
            int childCount = 0;
            for (DrawableWidget child : sub.childrenRenderOrder()) {
                childCount++;
                if (childCount > 2) {
                    opener = null;
                    break;
                }
                if (child instanceof ExecutableWidget exec
                        && exec.getTextureWidth() > exec.getTextureHeight()) {
                    opener = exec;
                }
            }
            if (opener != null) {
                Screen before = HostScreens.current();
                if (opener.onMouseClicked(opener.getX() + 1, opener.getY() + 1, 0)
                        && HostScreens.current() != before) {
                    return;
                }
            }
        }
        HostScreens.open(new WidgetHostScreen(widget, DEFAULT_EDITOR_W, DEFAULT_EDITOR_H));
    }

    /** 判断正则过滤项此刻能否展开注册表实时预览（{@code WeakRegistryRegex} 需已进入世界才有连接注册表）。 */
    public static boolean canPreviewRegistryRegex(Object raw) {
        if (raw instanceof RegistryRegex<?> reg) {
            return reg.getRegistry() != null;
        }
        if (raw instanceof WeakRegistryRegex<?> weak) {
            return weak.resolveRegistry().isPresent();
        }
        return false;
    }

    /**
     * 打开注册表正则过滤的实时预览选择屏（支持 {@link RegistryRegex}、{@code EntityTypeRegex} 与 {@link WeakRegistryRegex}）。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void openRegistryRegexPreview(Ref<?> ref, String keyName, ElementFactory factory) {
        if (ref == null) {
            return;
        }
        try {
            Object raw = ref.getValue();
            if (raw instanceof RegistryRegex<?> reg) {
                Registry<Object> registry = (Registry<Object>) reg.getRegistry();
                if (registry == null) {
                    return;
                }
                openRegexPreviewScreen(
                        (AttrKeyValue<Object>) (AttrKeyValue<?>) HostControls.keyValue(ref, keyName),
                        registry,
                        val -> ((RegistryRegex) val).getParent(),
                        (val, next) -> ((RegistryRegex) val).withParent(next),
                        (val, entry) -> ((RegistryRegex) val).test(entry),
                        val -> ((RegistryRegex) val).getRules(),
                        factory);
                return;
            }
            if (raw instanceof WeakRegistryRegex<?> weak) {
                Registry<Object> registry = (Registry<Object>) weak.resolveRegistry().orElse(null);
                if (registry == null) {
                    return;
                }
                openRegexPreviewScreen(
                        (AttrKeyValue<Object>) (AttrKeyValue<?>) HostControls.keyValue(ref, keyName),
                        registry,
                        val -> ((WeakRegistryRegex) val).parent(),
                        (val, next) -> ((WeakRegistryRegex) val).withParent(next),
                        (val, entry) -> ((WeakRegistryRegex) val).test(entry),
                        val -> ((WeakRegistryRegex) val).getRules(),
                        factory);
            }
        } catch (Throwable e) {
            Debug.info("[SFH+] 正则预览屏打开失败: " + e);
        }
    }

    private static <T, W> void openRegexPreviewScreen(
            AttrKeyValue<W> original,
            Registry<T> registry,
            Function<W, Regex> getParent,
            java.util.function.BiFunction<W, Regex, W> withParent,
            BiPredicate<W, T> tester,
            Function<W, List<Component>> rulesGetter,
            ElementFactory factory) {
        AttrKeyValue<W> originalCopy = original.copy();
        W seed = originalCopy.get();
        AttrKeyValue<Regex> regexWrapper = new TypeConvertAttrKeyValue<>(
                originalCopy,
                WrapperFactory.of(next -> withParent.apply(seed, next), getParent),
                Regex.TYPE);
        HostScreens.open(new RegistryChooseScreen<>(
                registry,
                ignored -> original.setInput(originalCopy.getInput()),
                factory) {
            private boolean listenerRegistered;

            @Override
            protected void refreshWidgets() {
                super.refreshWidgets();
                // SelectSubScreen 在 refreshWidgets() 才创建，放在构造初始化块里会在进屏前空指针
                selectSubScreen.modifiable(false);
                selectSubScreen.filter(entry -> originalCopy.isValidate()
                        && tester.test(originalCopy.get(), entry.getC()));
                if (!listenerRegistered) {
                    listenerRegistered = true;
                    originalCopy.addListener(ignored -> {
                        if (selectSubScreen != null) {
                            selectSubScreen.updateFilterList();
                        }
                    });
                }
                SubScreenWidget border = selectSubScreen.getScrollableBorder();
                border.clearChildren();
                border.addDrawableChild(regexWrapper.generateValueWidget(factory, 0, -20, border.width(), 20));
                border.addDrawableChild(factory.createElement(
                        IconElement.fixedGui(me.matl114.gui.Constants.EDITOR_SPRITE, ButtonAction.run(() -> {}))
                                .withTooltips(TooltipHandler.of(rulesGetter.apply(original.get()))),
                        null,
                        null,
                        border.width(),
                        -20,
                        20,
                        20));
            }

            @Override
            protected boolean canConfirm(me.matl114.gui.basic.ElementHandler elementHandler) {
                return originalCopy.isValidate();
            }
        });
    }

    /** 打开宿主样式/颜色代码说明页。 */
    public static void openFormattingWiki() {
        StringFormat.openWikiColorString();
    }

    /**
     * 打开列表类配置编辑器（支持 {@link PrimitiveList}、{@link RegexList}、{@link ListRef}）。
     *
     * <p>{@code PrimitiveList}/{@code RegexList} 与宿主 {@code openListModifyScreen} 要求的 {@code List} 类型不一致，
     * {@code ListRef} 的默认 keyValue 只写快照副本；这里统一用 {@link WrapperAttrKeyValue} 做双向转换与实时写回。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void openListEditor(Ref<?> ref, String keyName, ElementFactory factory) {
        if (ref == null) {
            return;
        }
        try {
            AttrKeyValue<Object> source = (AttrKeyValue<Object>) (AttrKeyValue<?>) HostControls.keyValue(ref, keyName);
            Object raw = ref.getValue();
            if (raw instanceof PrimitiveList<?> list) {
                NBTType<Object> elementType = (NBTType<Object>) list.elementType();
                WrapperFactory<List<Object>, PrimitiveList<Object>> wrapperFactory = WrapperFactory.of(
                        next -> new PrimitiveList<>(elementType, next, list.defaultPrimitive()),
                        PrimitiveList::list);
                AttrKeyValue<List<Object>> converted = new WrapperAttrKeyValue<>(
                        (AttrKeyValue<PrimitiveList<Object>>) (AttrKeyValue<?>) source, wrapperFactory);
                NBTTypes.openListModifyScreen(
                        factory, converted, elementType, list::createNewElement, DEFAULT_EDITOR_W, DEFAULT_EDITOR_H);
                return;
            }
            if (raw instanceof RegexList) {
                WrapperFactory<List<Pattern>, RegexList> wrapperFactory =
                        WrapperFactory.of(RegexList::new, RegexList::patterns);
                AttrKeyValue<List<Pattern>> converted = new WrapperAttrKeyValue<>(
                        (AttrKeyValue<RegexList>) (AttrKeyValue<?>) source, wrapperFactory);
                NBTTypes.openListModifyScreen(
                        factory, converted, NBTTypes.REGEX_TYPE, () -> Pattern.compile(""), DEFAULT_EDITOR_W, DEFAULT_EDITOR_H);
                return;
            }
            if (ref instanceof ListRef listRef) {
                WrapperFactory<List<String>, List<String>> wrapperFactory = WrapperFactory.of(
                        next -> {
                            listRef.setValue(new ArrayList<>(next));
                            return new ArrayList<>(next);
                        },
                        ArrayList::new);
                AttrKeyValue<List<String>> live = new WrapperAttrKeyValue<>(
                        (AttrKeyValue<List<String>>) (AttrKeyValue<?>) source, wrapperFactory);
                NBTTypes.openListModifyScreen(
                        factory, live, NBTTypes.STRING_TYPE, () -> "", DEFAULT_EDITOR_W, DEFAULT_EDITOR_H);
            }
        } catch (Throwable e) {
            Debug.info("[SFH+] 列表编辑器打开失败: " + e);
        }
    }

    /** 打开宿主物品编辑器，保存时回调编辑后的物品栈。 */
    public static void openItemStackEditor(ItemStack current, Consumer<ItemStack> onEdited) {
        if (current == null || current.isEmpty()) {
            return;
        }
        InvTasks.openEditScreen(current, onEdited);
    }

    /** 回退打开宿主单一分类页（尚未有新风格专页的顶部分类走此兜底）。 */
    public static void openClassicPage(
            String key, Function<Screen, DrawableWidget> page, ElementFactory factory) {
        if (page == null) {
            return;
        }
        Map<String, Function<Screen, DrawableWidget>> one = new LinkedHashMap<>();
        one.put(key, page);
        HostScreens.open(new ClassicClickGuiScreen(one, factory));
    }

    /** 打开宿主配置快照保存/读取界面。 */
    public static void openSnapshot(ConfigManager manager, ElementFactory factory, boolean saving) {
        if (manager != null) {
            manager.openSnapshotScreen(factory, saving);
        }
    }

    /** 打开宿主好友名单编辑界面。 */
    public static void openFriends(TargetSelector selector, ElementFactory factory) {
        if (selector != null) {
            selector.openEditFriendsScreen(factory);
        }
    }

    /** 把单个宿主 {@link DrawableWidget} 居中放进独立屏。 */
    private static final class WidgetHostScreen extends GenericScreen {
        private final DrawableWidget editor;
        private final int editorW;
        private final int editorH;

        WidgetHostScreen(DrawableWidget editor, int editorW, int editorH) {
            super(Component.empty(), 0, 0);
            this.editor = editor;
            this.editorW = editorW;
            this.editorH = editorH;
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        protected void refreshWidgets() {
            super.refreshWidgets();
            editor.setX((this.width - editorW) / 2);
            editor.setY((this.height - editorH) / 2);
            this.addRenderableWidget(editor);
        }
    }
}
