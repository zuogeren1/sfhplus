package me.zuogeren.slimefunhelperplus.module.newclickgui.core;

import java.lang.reflect.RecordComponent;
import me.matl114.hacks.utils.config.BoundedPrimitiveFlagMap;
import me.matl114.hacks.utils.config.LabelVec2;
import me.matl114.hacks.utils.config.LabelVec3;
import me.matl114.hacks.utils.config.OptionalPrimitive;
import me.matl114.hacks.utils.config.WidgetPos;
import me.matl114.hacks.utils.config.WrapEnum;
import me.matl114.managers.config.ConfigEnum;
import me.matl114.managers.config.Ref;
import net.minecraft.network.chat.TextColor;

/**
 * 跨页面、跨风格共用的复合记录（{@code record}）、可选值（{@link OptionalPrimitive}）与数值类型还原工具。
 *
 * <p>以前这套反射分派在模块页与全配置页各写了一份，甚至出现布尔记录上限 4 vs 8 的口径漂移；
 * 统一收敛到这里，保证行高计算、分量拆解与写回语义全局一致。
 */
public final class RecordFieldOps {
    private static final int MIN_RECORD_PARTS = 2;
    private static final int MAX_RECORD_PARTS = 8;

    private RecordFieldOps() {}

    /**
     * 一条记录里的几格：全是数值或枚举才拆成逐格控件（2~8 格），混着其它类型交回只读或专用控件。
     */
    public static Object[] recordParts(Object value) {
        if (value == null || !value.getClass().isRecord()) {
            return null;
        }
        RecordComponent[] parts = value.getClass().getRecordComponents();
        if (parts.length < MIN_RECORD_PARTS || parts.length > MAX_RECORD_PARTS) {
            return null;
        }
        Object[] out = new Object[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                Object part = parts[i].getAccessor().invoke(value);
                if (!(part instanceof Number) && !(part instanceof Enum<?>)) {
                    return null;
                }
                out[i] = part;
            }
        } catch (Throwable e) {
            return null;
        }
        return out;
    }

    /**
     * 一条记录里的几格开关：全是布尔（2~8 格）才拆成多行开关，混着其它类型交给其它路径。
     */
    public static boolean[] booleanParts(Object value) {
        if (value == null || !value.getClass().isRecord()) {
            return null;
        }
        RecordComponent[] parts = value.getClass().getRecordComponents();
        if (parts.length < MIN_RECORD_PARTS || parts.length > MAX_RECORD_PARTS) {
            return null;
        }
        boolean[] out = new boolean[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                Object part = parts[i].getAccessor().invoke(value);
                if (!(part instanceof Boolean flag)) {
                    return null;
                }
                out[i] = flag;
            }
        } catch (Throwable e) {
            return null;
        }
        return out;
    }

    /** 这一行占几格（高度倍数）：记录按分量数铺开，已启用的记录型可选值在首行开关下方展开各分量，其余一格。 */
    public static int rowParts(Object value) {
        if (value instanceof OptionalPrimitive<?> optional && optional.isPresent()) {
            Object inner = optional.getValue();
            Object[] optParts = recordParts(inner);
            if (optParts != null) {
                return 1 + optParts.length;
            }
            boolean[] optBools = booleanParts(inner);
            if (optBools != null) {
                return 1 + optBools.length;
            }
        }
        Object[] parts = recordParts(value);
        if (parts != null) {
            return parts.length;
        }
        boolean[] bools = booleanParts(value);
        if (bools != null) {
            return bools.length;
        }
        if (value instanceof WidgetPos) {
            return 2;
        }
        if (value instanceof LabelVec2) {
            return 2;
        }
        if (value instanceof LabelVec3) {
            return 3;
        }
        return 1;
    }

    /** 记录里每一格的字段名：驼峰拆成空格隔开的大写词（{@code ticksLater} → {@code TICKS LATER}）。 */
    public static String[] componentNames(Object value) {
        RecordComponent[] parts = value.getClass().getRecordComponents();
        String[] names = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            names[i] = spacedUpper(parts[i].getName());
        }
        return names;
    }

    public static String spacedUpper(String raw) {
        StringBuilder out = new StringBuilder(raw.length() + 4);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(c));
        }
        return out.toString();
    }

    /** 换掉记录里的第 {@code index} 个分量并构造新记录实例。 */
    public static Object withPart(Object value, int index, Object next) {
        try {
            RecordComponent[] parts = value.getClass().getRecordComponents();
            Object[] args = new Object[parts.length];
            for (int i = 0; i < parts.length; i++) {
                args[i] = i == index ? next : parts[i].getAccessor().invoke(value);
            }
            Class<?>[] types = new Class<?>[parts.length];
            for (int i = 0; i < parts.length; i++) {
                types[i] = parts[i].getType();
            }
            return value.getClass().getDeclaredConstructor(types).newInstance(args);
        } catch (Throwable e) {
            return value;
        }
    }

    /** 按记录分量的声明数值类型还原后替换该格。 */
    public static Object withNumericPart(Object value, int index, double next) {
        try {
            RecordComponent[] parts = value.getClass().getRecordComponents();
            if (index >= 0 && index < parts.length) {
                return withPart(value, index, coerce(parts[index].getType(), next));
            }
        } catch (Throwable ignored) {
            // 读不出记录结构就保持原值
        }
        return value;
    }

    /** 按声明的数值 {@link Class} 还原（如 {@code int}/{@code float}/{@code long}）。 */
    public static Object coerce(Class<?> type, double value) {
        if (type == double.class || type == Double.class) {
            return value;
        }
        if (type == float.class || type == Float.class) {
            return (float) value;
        }
        if (type == int.class || type == Integer.class) {
            return (int) Math.round(value);
        }
        if (type == long.class || type == Long.class) {
            return Math.round(value);
        }
        if (type == short.class || type == Short.class) {
            return (short) Math.round(value);
        }
        if (type == byte.class || type == Byte.class) {
            return (byte) Math.round(value);
        }
        return value;
    }

    /** 按运行时值对象还原数值类型（供 Baritone 这类无声明类型元数据条目用）；认不出时返回 {@code null}。 */
    public static Object coerceValue(Object current, double next) {
        if (current instanceof Integer) {
            return (int) Math.round(next);
        }
        if (current instanceof Long) {
            return Math.round(next);
        }
        if (current instanceof Short) {
            return (short) Math.round(next);
        }
        if (current instanceof Byte) {
            return (byte) Math.round(next);
        }
        if (current instanceof Float) {
            return (float) next;
        }
        if (current instanceof Double) {
            return next;
        }
        return null;
    }

    public static boolean isIntegral(Object value) {
        return value instanceof Integer
                || value instanceof Long
                || value instanceof Short
                || value instanceof Byte;
    }

    /** 取可选值的内层对象（不判 {@code isPresent()}）。 */
    public static Object optionalRawInner(Object raw) {
        return raw instanceof OptionalPrimitive<?> optional ? optional.getValue() : null;
    }

    /** 取已启用可选值的内层对象（自动解开 {@link WrapEnum}）；未启用时返回 {@code null}。 */
    public static Object optionalPresentInner(Object raw) {
        if (!(raw instanceof OptionalPrimitive<?> optional) || !optional.isPresent()) {
            return null;
        }
        Object inner = optional.getValue();
        return inner instanceof WrapEnum<?> wrap ? wrap.value : inner;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void setOptionalPresent(Ref<?> ref, boolean present) {
        if (ref == null) {
            return;
        }
        Object raw = ref.getValue();
        if (raw instanceof OptionalPrimitive optional) {
            ((Ref) ref).setValue(optional.withPresent(present));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void writeOptionalNumber(Ref<?> ref, double value) {
        if (ref == null) {
            return;
        }
        Object raw = ref.getValue();
        if (!(raw instanceof OptionalPrimitive optional)) {
            return;
        }
        Object inner = optional.getValue();
        if (!(inner instanceof Number number)) {
            return;
        }
        ((Ref) ref).setValue(optional.withValue(coerce(number.getClass(), value)));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void writeOptionalOption(Ref<?> ref, Object option) {
        if (ref == null) {
            return;
        }
        Object raw = ref.getValue();
        if (!(raw instanceof OptionalPrimitive optional)) {
            return;
        }
        Object next = optional.getValue() instanceof WrapEnum<?> ? new WrapEnum<>((ConfigEnum) option) : option;
        ((Ref) ref).setValue(optional.withValue(next));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void writeOptionalBoolean(Ref<?> ref, boolean value) {
        if (ref == null) {
            return;
        }
        Object raw = ref.getValue();
        if (raw instanceof OptionalPrimitive optional && optional.getValue() instanceof Boolean) {
            ((Ref) ref).setValue(optional.withValue(value));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void writeOptionalString(Ref<?> ref, String next) {
        if (ref == null || next == null) {
            return;
        }
        Object raw = ref.getValue();
        if (raw instanceof OptionalPrimitive optional && optional.getValue() instanceof CharSequence) {
            ((Ref) ref).setValue(optional.withValue(next));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void writeOptionalTextColor(Ref<?> ref, int rgb) {
        if (ref == null) {
            return;
        }
        Object raw = ref.getValue();
        if (raw instanceof OptionalPrimitive optional && optional.getValue() instanceof TextColor) {
            ((Ref) ref).setValue(optional.withValue(TextColor.fromRgb(rgb & 0xFFFFFF)));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void writeOptionalRecordNumber(Ref<?> ref, int partIndex, double value) {
        if (ref == null) {
            return;
        }
        Object raw = ref.getValue();
        if (!(raw instanceof OptionalPrimitive optional)) {
            return;
        }
        Object inner = optional.getValue();
        if (recordParts(inner) != null) {
            ((Ref) ref).setValue(optional.withValue(withNumericPart(inner, partIndex, value)));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void writeOptionalRecordPart(Ref<?> ref, int partIndex, Object next) {
        if (ref == null) {
            return;
        }
        Object raw = ref.getValue();
        if (!(raw instanceof OptionalPrimitive optional)) {
            return;
        }
        Object inner = optional.getValue();
        if (inner != null && inner.getClass().isRecord()) {
            ((Ref) ref).setValue(optional.withValue(withPart(inner, partIndex, next)));
        }
    }

    /** 有界开关表里当前打开的项数（与宿主编辑屏计数口径一致）。 */
    public static int flagCount(BoundedPrimitiveFlagMap<?> flags) {
        if (flags == null) {
            return 0;
        }
        int count = 0;
        for (Object value : flags.toMap().values()) {
            if (Boolean.TRUE.equals(value)) {
                count++;
            }
        }
        return count;
    }
}
