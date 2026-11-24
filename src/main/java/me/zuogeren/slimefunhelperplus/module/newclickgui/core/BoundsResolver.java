package me.zuogeren.slimefunhelperplus.module.newclickgui.core;

import java.util.HashMap;
import java.util.Map;
import me.matl114.utils.config.NumberClamp;

/**
 * 滑杆数值上下界的推导与屏幕内稳定缓存。
 *
 * <p>拖动滑杆期间若每帧按当前值重推区间，数值一跨数量级就会导致轨道比例跳档；
 * 所以缺省侧区间一旦推导出来就缓存在同一屏幕会话里，仅当当前值真的越界时再按新数量级重算。
 */
public final class BoundsResolver {
    private final Map<Object, double[]> derivedBounds = new HashMap<>();

    public void clear() {
        derivedBounds.clear();
    }

    /** 结合校验器里的 {@link NumberClamp} 解析闭区间；缺失的一侧按缓存的数量级兜底。 */
    public double[] resolve(Object cacheKey, Iterable<?> validators, double current) {
        Double fixedMin = null;
        Double fixedMax = null;
        if (validators != null) {
            for (Object validator : validators) {
                if (validator instanceof NumberClamp<?> clamp) {
                    if (clamp.min() != null) {
                        double value = clamp.min().doubleValue();
                        fixedMin = fixedMin == null ? value : Math.max(fixedMin, value);
                    }
                    if (clamp.max() != null) {
                        double value = clamp.max().doubleValue();
                        fixedMax = fixedMax == null ? value : Math.min(fixedMax, value);
                    }
                }
            }
        }
        double[] derived = derivedBounds.get(cacheKey);
        boolean escaped = derived == null
                || (fixedMin == null && current < derived[0])
                || (fixedMax == null && current > derived[1]);
        if (escaped) {
            derived = deriveBounds(current, fixedMin, fixedMax);
            derivedBounds.put(cacheKey, derived);
        }
        double min = fixedMin != null ? fixedMin : derived[0];
        double max = fixedMax != null ? fixedMax : derived[1];
        if (max <= min) {
            max = min + 1.0D;
        }
        return new double[] {min, max};
    }

    /** 解析无上下界元数据的条目范围（供 Baritone 等设置使用）：整数小值给 0..100，小数给 0..1。 */
    public double[] resolveUnbounded(Object cacheKey, double current, boolean whole) {
        double[] cached = derivedBounds.get(cacheKey);
        if (cached == null || current < cached[0] || current > cached[1]) {
            cached = deriveUnbounded(current, whole);
            derivedBounds.put(cacheKey, cached);
        }
        return cached;
    }

    private static double[] deriveBounds(double current, Double fixedMin, Double fixedMax) {
        double min;
        double max;
        if (fixedMin == null && fixedMax == null) {
            double magnitude = Math.max(1.0D, Math.abs(current));
            double top = Math.pow(10.0D, Math.ceil(Math.log10(magnitude)));
            max = top;
            min = current < 0.0D ? -top : 0.0D;
        } else if (fixedMin == null) {
            max = fixedMax;
            min = max <= 10.0D ? max - 10.0D : 0.0D;
        } else if (fixedMax == null) {
            min = fixedMin;
            max = min + Math.max(1.0D, Math.abs(min) * 0.5D);
        } else {
            min = fixedMin;
            max = fixedMax;
        }
        return new double[] {min, max};
    }

    private static double[] deriveUnbounded(double current, boolean whole) {
        double abs = Math.abs(current);
        double top;
        if (abs < 1.0D) {
            top = whole ? 100.0D : 1.0D;
        } else {
            top = Math.pow(10.0D, Math.ceil(Math.log10(abs)));
        }
        return current < 0.0D ? new double[] {-top, top} : new double[] {0.0D, top};
    }
}
