package cn.blockforge.generated.soulkatana;

import cn.blockforge.generated.soulkatana.entity.CrescentSlashEntity;
import cn.blockforge.generated.soulkatana.entity.DomainSlashEntity;
import cn.blockforge.generated.soulkatana.entity.ShrineEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class SoulKatanaEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SoulKatana.MODID);

    /** [解]：白色月牙波动（推进实体，接触判定：碰到实体造成 8 点伤害，每实体每发至多一次）。 */
    public static final RegistryObject<EntityType<CrescentSlashEntity>> CRESCENT_SLASH =
            ENTITY_TYPES.register("crescent_slash", () -> EntityType.Builder
                    .<CrescentSlashEntity>of(CrescentSlashEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).clientTrackingRange(64).updateInterval(1)
                    .build(SoulKatana.MODID + ":crescent_slash"));

    /** 领域斩击风暴的贴图斩击（纯视觉，短寿命）；也被 [捌] 复用为命中火花。 */
    public static final RegistryObject<EntityType<DomainSlashEntity>> DOMAIN_SLASH =
            ENTITY_TYPES.register("domain_slash", () -> EntityType.Builder
                    .<DomainSlashEntity>of(DomainSlashEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).clientTrackingRange(64).updateInterval(1)
                    .build(SoulKatana.MODID + ":domain_slash"));

    /** 伏魔御厨子神龛：领域锚点，不入存档（领域是纯内存状态）。 */
    public static final RegistryObject<EntityType<ShrineEntity>> SHRINE =
            ENTITY_TYPES.register("shrine", () -> EntityType.Builder
                    .<ShrineEntity>of(ShrineEntity::new, MobCategory.MISC)
                    .sized(9.0F, 9.0F).clientTrackingRange(128).updateInterval(20)
                    .build(SoulKatana.MODID + ":shrine"));

    private SoulKatanaEntities() { }
}
