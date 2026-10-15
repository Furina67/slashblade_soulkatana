package cn.blockforge.generated.soulkatana.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 封闭式领域【自闭圆顿裹】的「物理快照索引」，按维度持久化在存档里。
 *
 * <p>核心约束（来自需求文档）：被替换的方块**绝不用内存存 BlockState**，而是物理搬到
 * Y=303 一带的存储区；内存里只留一张「原坐标 &lt;-&gt; 存储坐标」的映射表。这个类就是那张表的
 * 落盘形态——只存成对的 {@code long}（{@link BlockPos#asLong()}），不存方块数据。
 *
 * <p>为什么要落盘：如果服务端在领域展开期间崩溃/被 kill，世界存档里会留着一个巨大的
 * 黑色半球 + 发光球，而内存里的映射表已经没了，地形永远回不来。落盘后，
 * {@code RealPersonDomain} 在 ServerStartedEvent 时读回映射表，把地形原样还原。
 */
public final class DomainSnapshot extends SavedData {

    private static final String DATA_NAME = "soulkatana_domain_snapshots";
    private static final String KEY_DOMAINS = "domains";
    private static final String KEY_OWNER = "owner";
    private static final String KEY_CENTER = "center";
    private static final String KEY_PAIRS = "pairs";

    private final List<Entry> entries = new ArrayList<>();

    private DomainSnapshot() { }

    public static DomainSnapshot get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(DomainSnapshot::load, DomainSnapshot::new, DATA_NAME);
    }

    static DomainSnapshot load(CompoundTag tag) {
        DomainSnapshot data = new DomainSnapshot();
        ListTag list = tag.getList(KEY_DOMAINS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            if (!entryTag.hasUUID(KEY_OWNER)) {
                continue;
            }
            Entry entry = new Entry(entryTag.getUUID(KEY_OWNER), entryTag.getLong(KEY_CENTER));
            long[] pairs = entryTag.getLongArray(KEY_PAIRS);
            for (int p = 0; p + 1 < pairs.length; p += 2) {
                entry.addPacked(pairs[p], pairs[p + 1]);
            }
            data.entries.add(entry);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Entry entry : entries) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID(KEY_OWNER, entry.owner);
            entryTag.putLong(KEY_CENTER, entry.center);
            entryTag.putLongArray(KEY_PAIRS, entry.packedPairs());
            list.add(entryTag);
        }
        tag.put(KEY_DOMAINS, list);
        return tag;
    }

    Entry createEntry(UUID owner, BlockPos center) {
        Entry entry = new Entry(owner, center.asLong());
        entries.add(entry);
        setDirty();
        return entry;
    }

    void removeEntry(Entry entry) {
        if (entries.remove(entry)) {
            setDirty();
        }
    }

    List<Entry> entries() {
        return entries;
    }

    boolean isEmpty() {
        return entries.isEmpty();
    }

    void clear() {
        entries.clear();
        setDirty();
    }

    /** 一次领域的映射表：只存坐标对，方块本体在 Y=303 物理存储区。 */
    static final class Entry {
        final UUID owner;
        final long center;
        /** [原坐标, 存储坐标] 交替；下标 2i / 2i+1。 */
        private long[] pairs = new long[64];
        private int size;

        private Entry(UUID owner, long center) {
            this.owner = owner;
            this.center = center;
        }

        void add(BlockPos origin, BlockPos storage) {
            addPacked(origin.asLong(), storage.asLong());
        }

        private void addPacked(long origin, long storage) {
            if (size * 2 + 2 > pairs.length) {
                pairs = Arrays.copyOf(pairs, Math.max(pairs.length * 2, size * 2 + 2));
            }
            pairs[size * 2] = origin;
            pairs[size * 2 + 1] = storage;
            size++;
        }

        long[] packedPairs() {
            return Arrays.copyOf(pairs, size * 2);
        }

        Map<BlockPos, BlockPos> movedMap() {
            Map<BlockPos, BlockPos> map = new HashMap<>(Math.max(16, size * 2));
            for (int i = 0; i < size; i++) {
                map.put(BlockPos.of(pairs[i * 2]), BlockPos.of(pairs[i * 2 + 1]));
            }
            return map;
        }

        BlockPos centerPos() {
            return BlockPos.of(center);
        }
    }
}
