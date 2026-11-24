package me.zuogeren.slimefunhelperplus.module.newclickgui.core;

import java.util.Collections;
import java.util.List;
import me.matl114.hacks.api.BaseModule.WrapperConfigRef;
import me.matl114.hacks.utils.config.BoundedPrimitiveMap;
import me.matl114.hacks.utils.config.EntityTypeEntrySet;
import me.matl114.hacks.utils.config.EntrySet;
import me.matl114.hacks.utils.config.EnumPrimitiveList;
import me.matl114.hacks.utils.config.GuiSetting;
import me.matl114.hacks.utils.config.Holder;
import me.matl114.hacks.utils.config.ItemStackTemplate;
import me.matl114.hacks.utils.config.LabelVec2;
import me.matl114.hacks.utils.config.LabelVec3;
import me.matl114.hacks.utils.config.OptionalPrimitive;
import me.matl114.hacks.utils.config.PrimitiveList;
import me.matl114.hacks.utils.config.PrimitiveMap;
import me.matl114.hacks.utils.config.PrimitivePairList;
import me.matl114.hacks.utils.config.Regex;
import me.matl114.hacks.utils.config.RegexList;
import me.matl114.hacks.utils.config.RegistryRegex;
import me.matl114.hacks.utils.config.StringEnum;
import me.matl114.hacks.utils.config.StringFormat;
import me.matl114.hacks.utils.config.WeakHolder;
import me.matl114.hacks.utils.config.WeakRegistryRegex;
import me.matl114.hacks.utils.config.WidgetPos;
import me.matl114.hacks.utils.config.WrapColor;
import me.matl114.hacks.utils.config.WrapRGBA;
import me.matl114.managers.config.EnumRef;
import me.matl114.managers.config.FlagRef;
import me.matl114.managers.config.KeyBindRef;
import me.matl114.managers.config.ListRef;
import me.matl114.managers.config.Ref;
import me.matl114.managers.config.StringRef;
import me.matl114.utils.config.AttrKeyValue;
import me.matl114.utils.config.ValueAccessor;
import me.zuogeren.slimefunhelperplus.hostgui.HostControls;
import me.zuogeren.slimefunhelperplus.hostgui.StringChoicesRegistry;

/**
 * 跨数据源、跨主题统一的配置条目描述与控件类型分派契约。
 *
 * <p>把宿主的三种异构来源（模块 {@link WrapperConfigRef}、全配置 {@link Ref}、Baritone {@link ValueAccessor}）
 * 收敛成同一套读写/恢复默认/控件类别模型，供现有页面与新增皮肤主题直接复用。
 */
public interface ConfigEntry {

    /** 统一配置控件类别，供任意风格按语义挑选自绘控件。 */
    enum ControlKind {
        BOOLEAN,
        NUMBER,
        ENUM_OR_CHOICE,
        KEYBIND,
        COLOR,
        WRAP_RGBA,
        TEXT,
        STRING_FORMAT,
        ITEM_TEMPLATE,
        REGISTRY_REGEX,
        OPTIONAL,
        ENTRY_SET,
        ENTITY_TYPE_SET,
        BOUNDED_FLAG_MAP,
        GUI_SETTING,
        LIST_OR_MAP_POPUP,
        WIDGET_POS,
        LABEL_VEC,
        RECORD_FIELDS,
        RECORD_BOOLEANS,
        READ_ONLY
    }

    String fullPath();

    String sectionKey();

    String label();

    Object rawValue();

    Ref<?> ref();

    void writeValue(Object next);

    boolean hasDefault();

    boolean isChanged();

    void resetToDefault();

    default List<?> validators() {
        Ref<?> r = ref();
        if (r == null) {
            return Collections.emptyList();
        }
        AttrKeyValue<?> kv = HostControls.keyValue(r, fullPath());
        return kv == null ? Collections.emptyList() : kv.getValidators();
    }

    default ControlKind controlKind() {
        return classify(ref(), rawValue(), fullPath());
    }

    /** 根据 {@link Ref} 类型、当前运行时值与字符串候选表判定应呈现的控件类别。 */
    static ControlKind classify(Ref<?> ref, Object raw, String fullPath) {
        if (ref instanceof FlagRef || raw instanceof Boolean) {
            return ControlKind.BOOLEAN;
        }
        if (ValueCodec.isNumericRef(ref) || raw instanceof Number) {
            return ControlKind.NUMBER;
        }
        if (ref instanceof KeyBindRef) {
            return ControlKind.KEYBIND;
        }
        if (raw instanceof WrapRGBA) {
            return ControlKind.WRAP_RGBA;
        }
        if (raw instanceof WrapColor) {
            return ControlKind.COLOR;
        }
        if (ref instanceof EnumRef<?> || raw instanceof StringEnum || raw instanceof Enum<?>) {
            return ControlKind.ENUM_OR_CHOICE;
        }
        if (!StringChoicesRegistry.ofPath(fullPath).isEmpty()) {
            return ControlKind.ENUM_OR_CHOICE;
        }
        if (raw instanceof StringFormat) {
            return ControlKind.STRING_FORMAT;
        }
        if (raw instanceof ItemStackTemplate) {
            return ControlKind.ITEM_TEMPLATE;
        }
        if (raw instanceof RegistryRegex<?> || raw instanceof WeakRegistryRegex<?>) {
            return ControlKind.REGISTRY_REGEX;
        }
        if (ref instanceof StringRef
                || raw instanceof Regex
                || raw instanceof Holder<?>
                || raw instanceof WeakHolder<?>
                || raw instanceof CharSequence) {
            return ControlKind.TEXT;
        }
        if (raw instanceof OptionalPrimitive<?>) {
            return ControlKind.OPTIONAL;
        }
        if (raw instanceof EntrySet<?>) {
            return ControlKind.ENTRY_SET;
        }
        if (raw instanceof EntityTypeEntrySet) {
            return ControlKind.ENTITY_TYPE_SET;
        }
        if (raw instanceof BoundedPrimitiveMap<?, ?>) {
            return ControlKind.BOUNDED_FLAG_MAP;
        }
        if (raw instanceof GuiSetting) {
            return ControlKind.GUI_SETTING;
        }
        if (ref instanceof ListRef
                || raw instanceof RegexList
                || raw instanceof PrimitiveList<?>
                || raw instanceof PrimitivePairList<?, ?>
                || raw instanceof PrimitiveMap<?, ?>
                || raw instanceof EnumPrimitiveList<?, ?>) {
            return ControlKind.LIST_OR_MAP_POPUP;
        }
        if (raw instanceof WidgetPos) {
            return ControlKind.WIDGET_POS;
        }
        if (raw instanceof LabelVec2 || raw instanceof LabelVec3) {
            return ControlKind.LABEL_VEC;
        }
        if (RecordFieldOps.booleanParts(raw) != null) {
            return ControlKind.RECORD_BOOLEANS;
        }
        if (RecordFieldOps.recordParts(raw) != null) {
            return ControlKind.RECORD_FIELDS;
        }
        return ControlKind.READ_ONLY;
    }

    /** 适配模块页与各专页使用的 {@link WrapperConfigRef}。 */
    static ConfigEntry fromWrapper(WrapperConfigRef<?> wrapper, String label) {
        String key = wrapper.keyName();
        String section = ValueCodec.sectionKey(wrapper.path());
        Ref<?> ref = wrapper.ref();
        return new RefConfigEntry(key, section, label, ref);
    }

    /** 适配全配置页按路径解析出来的 {@link Ref}。 */
    static ConfigEntry fromConfigRef(String fullPath, String sectionKey, String label, Ref<?> ref) {
        return new RefConfigEntry(fullPath, sectionKey, label, ref);
    }

    /** 适配 Baritone {@link ValueAccessor} 设置条目。 */
    static ConfigEntry fromAccessor(String name, String label, ValueAccessor<?> accessor, Object defaultValue) {
        return new AccessorConfigEntry(name, label, accessor, defaultValue);
    }

    final class RefConfigEntry implements ConfigEntry {
        private final String fullPath;
        private final String sectionKey;
        private final String label;
        private final Ref<?> ref;

        RefConfigEntry(String fullPath, String sectionKey, String label, Ref<?> ref) {
            this.fullPath = fullPath == null ? "" : fullPath;
            this.sectionKey = sectionKey == null ? "" : sectionKey;
            this.label = label == null ? "" : label;
            this.ref = ref;
        }

        @Override
        public String fullPath() {
            return fullPath;
        }

        @Override
        public String sectionKey() {
            return sectionKey;
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public Object rawValue() {
            return ref == null ? null : ref.getValue();
        }

        @Override
        public Ref<?> ref() {
            return ref;
        }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public void writeValue(Object next) {
            if (ref != null) {
                ((Ref) ref).setValue(next);
            }
        }

        @Override
        public boolean hasDefault() {
            return ref != null && ref.hasDefaultValue();
        }

        @Override
        public boolean isChanged() {
            return ref != null && ref.isValueDifferent();
        }

        @Override
        public void resetToDefault() {
            if (ref != null && ref.hasDefaultValue()) {
                writeValue(ref.getDefaultValue());
            }
        }
    }

    final class AccessorConfigEntry implements ConfigEntry {
        private final String name;
        private final String label;
        private final ValueAccessor<?> accessor;
        private final Object defaultValue;

        AccessorConfigEntry(String name, String label, ValueAccessor<?> accessor, Object defaultValue) {
            this.name = name == null ? "" : name;
            this.label = label == null ? "" : label;
            this.accessor = accessor;
            this.defaultValue = defaultValue;
        }

        @Override
        public String fullPath() {
            return name;
        }

        @Override
        public String sectionKey() {
            return "";
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public Object rawValue() {
            try {
                return accessor == null ? null : accessor.getValue();
            } catch (Throwable ignored) {
                return null;
            }
        }

        @Override
        public Ref<?> ref() {
            return null;
        }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public void writeValue(Object next) {
            if (accessor == null) {
                return;
            }
            try {
                ((ValueAccessor) accessor).setValue(next);
            } catch (Throwable ignored) {
                // 类型不匹配时保持原值
            }
        }

        @Override
        public boolean hasDefault() {
            return defaultValue != null;
        }

        @Override
        public boolean isChanged() {
            return defaultValue != null && !java.util.Objects.equals(rawValue(), defaultValue);
        }

        @Override
        public void resetToDefault() {
            if (defaultValue != null) {
                writeValue(defaultValue);
            }
        }
    }
}
