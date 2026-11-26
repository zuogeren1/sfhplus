package cn.net.rms.confluxmap.core.waypoint;

import cn.net.rms.confluxmap.core.model.DimensionId;
import java.util.UUID;

/**
 * conflux 内部路径点（编译期桩，不参与打包）。
 *
 * <p>字段名逐字照 0.1.16 反编译（`public final UUID id; public String name; ...`）——
 * 保存过滤要按 {@code group} 判断，指纹要读其余字段。真类在 common jar 里。
 */
public final class Waypoint {
    public final UUID id = null;
    public String name;
    public DimensionId dimensionId;
    public double x;
    public double y;
    public double z;
    public int colorArgb;
    public String group;
    public boolean visible;
    public boolean crossDimensionVisible;
    public Double crossDimensionY;
    public Waypoint.Type type;
    public String iconItemId = "";
    public String markerLabel = "";
    public final long createdAtEpochMs = 0L;

    /**
     * 真类是 `Waypoint$Type`：0.1.16 只有 NORMAL / DEATH，0.1.18 末尾追加了 TEMPORARY
     * （那类临时点在他们自己构造保存快照时就被丢掉了；api 层的 WaypointType 仍只有前两个值）。
     */
    public enum Type {
        NORMAL,
        DEATH,
        TEMPORARY
    }
}
