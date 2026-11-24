package me.zuogeren.slimefunhelperplus.module.newclickgui.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import me.matl114.hacks.utils.config.BoundedPrimitiveFlagMap;
import me.matl114.hacks.utils.config.BoundedPrimitiveMap;
import me.matl114.hacks.utils.config.EnumPrimitiveList;
import me.matl114.hacks.utils.config.Holder;
import me.matl114.hacks.utils.config.ItemStackTemplate;
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
import me.matl114.hacks.utils.config.WrapColor;
import me.matl114.hacks.utils.config.WrapEnum;
import me.matl114.hacks.utils.config.WrapRGBA;
import me.matl114.managers.config.ConfigEnum;
import me.matl114.managers.config.DoubleRef;
import me.matl114.managers.config.FloatRef;
import me.matl114.managers.config.IntRef;
import me.matl114.managers.config.ListRef;
import me.matl114.managers.config.LongRef;
import me.matl114.managers.config.Ref;
import me.matl114.managers.config.StringRef;
import me.matl114.managers.input.MultiKeyBind;
import me.zuogeren.slimefunhelperplus.hostgui.StringChoicesRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

/**
 * 跨风格共用的配置值格式化、颜色/数值/文本解析与枚举/路径文案工具。
 *
 * <p>集中收拢 {@code #RRGGBB} 解析、小数保留两位、枚举翻译键与文本族写回逻辑，
 * 避免不同页面或未来新主题对同一个宿主 {@link Ref} 产生两套读写语义。
 */
public final class ValueCodec {
    private ValueCodec() {}

    /** 点分键名取最后一段（如 {@code a.b.enable} → {@code enable}）。 */
    public static String leaf(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        String[] parts = key.split("\\.");
        return parts[parts.length - 1];
    }

    /** 路径数组剥掉末尾字段名作为分节键；单段或空数组返回空串（主设置节）。 */
    public static String sectionKey(String[] path) {
        if (path == null || path.length <= 1) {
            return "";
        }
        return String.join(".", Arrays.copyOf(path, path.length - 1));
    }

    public static String sectionTitle(String key) {
        if (key == null || key.isEmpty()) {
            return "主设置";
        }
        return Component.translatableWithFallback("config.index." + key, leaf(key)).getString();
    }

    public static String labelText(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        return Component.translatableWithFallback(key, key).getString();
    }

    /** 整数不带小数点，浮点保留最多两位并去掉末尾零。 */
    public static String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0E-6D) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    /** 解析用户输入数字；半截输入（"-"、"."）或 NaN/Infinity 返回 {@code null}。 */
    public static Double tryParseNumber(String text) {
        if (text == null) {
            return null;
        }
        try {
            double value = Double.parseDouble(text.trim().replace(',', '.'));
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static double numericOf(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0D;
    }

    public static boolean isNumericRef(Ref<?> ref) {
        return ref instanceof IntRef
                || ref instanceof LongRef
                || ref instanceof DoubleRef
                || ref instanceof FloatRef;
    }

    public static double stepOf(Ref<?> ref) {
        if (ref instanceof IntRef || ref instanceof LongRef) {
            return 1.0D;
        }
        double magnitude = Math.abs(numericOf(ref == null ? null : ref.getValue()));
        return magnitude >= 10.0D ? 1.0D : (magnitude >= 1.0D ? 0.5D : 0.1D);
    }

    /** 按 {@link Ref} 真实类型写回数值：整型取整，浮点保留两位，保证步进、拖动与手输同口径。 */
    public static void setNumeric(Ref<?> ref, double value) {
        if (ref instanceof IntRef intRef) {
            intRef.set((int) Math.round(value));
        } else if (ref instanceof LongRef longRef) {
            longRef.set(Math.round(value));
        } else if (ref instanceof DoubleRef doubleRef) {
            doubleRef.set(Math.round(value * 100.0D) / 100.0D);
        } else if (ref instanceof FloatRef floatRef) {
            floatRef.set((float) (Math.round(value * 100.0D) / 100.0D));
        }
    }

    public static int colorOf(Object value) {
        if (value instanceof WrapColor color && color.color() != null) {
            return color.asRGB();
        }
        if (value instanceof WrapRGBA rgba && rgba.textColor() != null) {
            return rgba.textColor().getValue();
        }
        if (value instanceof TextColor tc) {
            return tc.getValue();
        }
        if (value instanceof OptionalPrimitive<?> opt && opt.getValue() instanceof TextColor tc) {
            return tc.getValue();
        }
        return 0xFFFFFF;
    }

    public static int alphaOf(Object value) {
        return value instanceof WrapRGBA rgba ? Math.max(0, Math.min(255, rgba.alpha())) : 255;
    }

    public static String hexOf(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    /** 只认 {@code #} 加六位十六进制；其它前缀或半截十六进制返回 {@code false}。 */
    public static boolean fullHex(String value) {
        if (value == null || value.length() != 7 || value.charAt(0) != '#') {
            return false;
        }
        for (int i = 1; i < value.length(); i++) {
            if (Character.digit(value.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 解析颜色输入：以 {@code #} 开头必须满六位，否则按命名色交由 {@link WrapColor} 解析；
     * 半截输入或非法名称返回 {@code null}，不覆盖原配置。
     */
    public static WrapColor tryParseWrapColor(String text) {
        if (text == null) {
            return null;
        }
        String value = text.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty() || (value.startsWith("#") && !fullHex(value))) {
            return null;
        }
        try {
            return new WrapColor(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static TextColor tryParseTextColor(String text) {
        WrapColor wrap = tryParseWrapColor(text);
        return wrap == null ? null : wrap.color();
    }

    public static Integer tryParseAlpha(String text) {
        Double number = tryParseNumber(text);
        if (number == null) {
            return null;
        }
        int rounded = (int) Math.round(number);
        return Math.max(0, Math.min(255, rounded));
    }

    /** 把调色盘选中的 RGB 写回任意颜色类值（{@link WrapColor}、{@link WrapRGBA} 或 {@link OptionalPrimitive}{@code <TextColor>}）。 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Object withRgb(Object current, int rgb) {
        TextColor next = TextColor.fromRgb(rgb & 0xFFFFFF);
        if (current instanceof WrapColor) {
            return new WrapColor(next);
        }
        if (current instanceof WrapRGBA rgba) {
            return rgba.withTextColor(next);
        }
        if (current instanceof OptionalPrimitive optional && optional.getValue() instanceof TextColor) {
            return optional.withValue(next);
        }
        return new WrapColor(next);
    }

    public static String enumType(Enum<?> value) {
        if (value instanceof ConfigEnum configEnum) {
            return configEnum.getConfigEnumType().replace('_', '-');
        }
        return value.getDeclaringClass().getSimpleName().toLowerCase(Locale.ROOT);
    }

    public static String enumLabel(Object option) {
        if (option instanceof Enum<?> e) {
            String key = "configenum." + enumType(e) + "." + e.name().toLowerCase(Locale.ROOT);
            return Component.translatableWithFallback(key, e.name()).getString();
        }
        if (option instanceof ConfigEnum configEnum) {
            Enum<?> e = configEnum.cast();
            String key = "configenum." + enumType(e) + "." + e.name().toLowerCase(Locale.ROOT);
            return Component.translatableWithFallback(key, e.name()).getString();
        }
        return String.valueOf(option);
    }

    /** 取出当前枚举（或记录分量/可选值内层枚举，解开 {@link WrapEnum}；{@code part >= 100} 为可选记录分量）。 */
    public static Object enumValueOf(Object raw, int part, int optionalPart) {
        Object value = raw;
        if (part == optionalPart) {
            value = RecordFieldOps.optionalRawInner(raw);
        } else if (part >= 100 && raw instanceof OptionalPrimitive<?> opt) {
            Object[] parts = RecordFieldOps.recordParts(opt.getValue());
            int index = part - 100;
            value = parts != null && index < parts.length ? parts[index] : null;
        } else if (part >= 0) {
            Object[] parts = RecordFieldOps.recordParts(raw);
            value = parts != null && part < parts.length ? parts[part] : null;
        }
        return value instanceof WrapEnum<?> wrap ? wrap.value : value;
    }

    /** 列出下拉候选：依次检查字符串注册表、{@link StringEnum} 候选、枚举常量数组。 */
    public static Object[] optionsFor(Object raw, String fullPath, int part, int optionalPart) {
        if (part == -1) {
            List<String> choices = StringChoicesRegistry.ofPath(fullPath);
            if (!choices.isEmpty()) {
                return choices.toArray();
            }
            if (raw instanceof StringEnum choice) {
                return choice.values().toArray();
            }
        }
        Object value = enumValueOf(raw, part, optionalPart);
        return value instanceof Enum<?> e ? e.getDeclaringClass().getEnumConstants() : new Object[0];
    }

    /** 提取文本编辑框里显示的完整原文（不裁剪、不输出对象身份串）。 */
    public static String editableTextOf(Object raw) {
        if (raw == null) {
            return "";
        }
        if (raw instanceof Regex regex) {
            return regex.regex();
        }
        if (raw instanceof RegistryRegex<?> reg) {
            return reg.getParent().regex();
        }
        if (raw instanceof WeakRegistryRegex<?> weak) {
            return weak.parent().regex();
        }
        if (raw instanceof StringFormat format) {
            return format.formatString();
        }
        if (raw instanceof ItemStackTemplate template) {
            return template.itemId().toString();
        }
        if (raw instanceof Holder<?> holder) {
            return holder.asString();
        }
        if (raw instanceof WeakHolder<?> weak) {
            return weak.asString();
        }
        return raw.toString();
    }

    /**
     * 把文本编辑草稿写回 {@link Ref}：按值原本类型处理正则、注册表正则、物品模板、注册表条目、格式化串或纯字符串。
     *
     * @return 是否已成功尝试写回（即使解析失败导致不落盘，文本编辑态也视为已处理）
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean commitText(Ref<?> ref, String rawDraft) {
        if (ref == null || rawDraft == null) {
            return false;
        }
        String next = rawDraft.trim();
        Object current = ref.getValue();
        if (current instanceof Regex) {
            try {
                ((Ref) ref).setValue(new Regex(next));
                return true;
            } catch (RuntimeException ignored) {
                return false;
            }
        }
        if (current instanceof RegistryRegex<?> reg) {
            try {
                ((Ref) ref).setValue(reg.withParent(new Regex(next)));
                return true;
            } catch (RuntimeException ignored) {
                return false;
            }
        }
        if (current instanceof WeakRegistryRegex<?> weak) {
            try {
                ((Ref) ref).setValue(weak.withParent(new Regex(next)));
                return true;
            } catch (RuntimeException ignored) {
                return false;
            }
        }
        if (current instanceof ItemStackTemplate template) {
            Identifier id = Identifier.tryParse(next);
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                template.setType(id);
                ((Ref) ref).setValue(template);
            }
            return true;
        }
        if (current instanceof Holder<?>) {
            Holder.parse(next).ifSuccess(parsed -> ((Ref) ref).setValue(parsed));
            return true;
        }
        if (current instanceof WeakHolder<?>) {
            WeakHolder.parse(next).ifSuccess(parsed -> ((Ref) ref).setValue(parsed));
            return true;
        }
        if (current instanceof StringFormat format) {
            ((Ref) ref).setValue(format.withFormatString(next));
            return true;
        }
        if (ref instanceof StringRef stringRef) {
            stringRef.set(next);
            return true;
        }
        ((Ref) ref).setValue(next);
        return true;
    }

    public static String keyBindText(MultiKeyBind bind) {
        if (bind == null || bind.isEmpty()) {
            return "未绑定";
        }
        String text = bind.getKeyStr();
        return text == null || text.isEmpty() ? "未绑定" : text;
    }

    /** 只读行或列表/值展示用文本：按值类型做安全可读格式化。 */
    public static String previewValueText(Object value) {
        if (value == null) {
            return "-";
        }
        if (value instanceof Float || value instanceof Double) {
            return formatNumber(((Number) value).doubleValue());
        }
        if (value instanceof Boolean flag) {
            return flag ? "开" : "关";
        }
        if (value instanceof Enum<?> || value instanceof ConfigEnum) {
            return enumLabel(value);
        }
        if (value instanceof MultiKeyBind bind) {
            return keyBindText(bind);
        }
        if (value instanceof Regex regex) {
            return regex.regex();
        }
        if (value instanceof RegistryRegex<?> reg) {
            return reg.getParent().regex();
        }
        if (value instanceof WeakRegistryRegex<?> weak) {
            return weak.parent().regex();
        }
        if (value instanceof StringFormat format) {
            return format.formatString();
        }
        if (value instanceof Holder<?> holder) {
            return holder.asString();
        }
        if (value instanceof WeakHolder<?> weak) {
            return weak.asString();
        }
        if (value instanceof WrapColor || value instanceof TextColor) {
            return hexOf(colorOf(value));
        }
        if (value instanceof WrapRGBA rgba) {
            return hexOf(colorOf(rgba)) + " (" + rgba.alpha() + ")";
        }
        if (value instanceof Collection<?> || value instanceof PrimitivePairList<?, ?>
                || value instanceof EnumPrimitiveList<?, ?> || value.getClass().isArray()) {
            return "列表";
        }
        if (value instanceof Map<?, ?> || value instanceof BoundedPrimitiveMap<?, ?>) {
            return "映射";
        }
        return String.valueOf(value);
    }

    /** 取集合/键值/枚举映射类配置的条目数，不匹配时返回 -1。 */
    public static int listCountOf(Ref<?> ref, Object raw) {
        if (ref instanceof ListRef listRef) {
            return listRef.get().size();
        }
        if (raw instanceof RegexList regexList) {
            return regexList.patterns().size();
        }
        if (raw instanceof PrimitiveList<?> list) {
            return list.list().size();
        }
        if (raw instanceof PrimitivePairList<?, ?> pairList) {
            return pairList.list().size();
        }
        if (raw instanceof PrimitiveMap<?, ?> map) {
            return map.toPrimitiveMap().size();
        }
        if (raw instanceof BoundedPrimitiveFlagMap<?> flags) {
            return RecordFieldOps.flagCount(flags);
        }
        if (raw instanceof BoundedPrimitiveMap<?, ?> map) {
            return map.toMap().size();
        }
        if (raw instanceof EnumPrimitiveList<?, ?> enumList) {
            int[] count = {0};
            enumList.forEach((k, v) -> count[0]++);
            return count[0];
        }
        return -1;
    }
}
