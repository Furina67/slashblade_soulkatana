package cn.blockforge.generated.soulkatana.integration;

import cn.blockforge.generated.soulkatana.SoulKatana;
import cn.blockforge.generated.soulkatana.SoulKatanaConfig;
import cn.blockforge.generated.soulkatana.SoulKatanaIdentity;
import cn.blockforge.generated.soulkatana.SoulKatanaNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 「化身」(Identity, modid=identity) 软依赖桥 —— 变身闸门。
 *
 * 需求（魂刀_真人.txt）：装载 Identity 时，获得/切换化身必须主手持「魂刀·真人」；
 * 未装载 Identity 时本模组照常运行，只缺失这部分联动。开关见
 * config/soulkatana-server.toml 的 identity_require_soul_blade（布尔值）。
 *
 * 实现（操作说明.md 首选方案）：挂 Identity 官方提供的变身审核钩子
 * {@code draylar.identity.api.event.IdentitySwapCallback}——所有变身路径
 * （快捷键界面 /identity 命令 / 击杀解锁 / 死亡还原）最终都汇聚到
 * PlayerEntityDataMixin#updateIdentity，其开头即调用该回调，
 * 返回 EventResult.interruptFalse() 即否决本次变身（服务端权威，无需 Mixin）。
 * 同时挂 {@code UnlockIdentityCallback} 拦住"得到化身"（击杀解锁）。
 *
 * 软依赖纪律：
 *  - 本类不 import 任何 draylar/dev.architectury 类，全部反射 + JDK 动态代理，
 *    编译期与运行期都不需要对方在 classpath 上；
 *  - 注册动作发生在 FMLCommonSetupEvent，且先判 ModList.isLoaded("identity")；
 *  - 任何一步反射失败只打日志并放弃联动，绝不让本模组崩溃。
 *
 * 放行规则：
 *  1) to == null（变回玩家本人）永远放行——绝不把玩家锁在生物形态；
 *  2) 本玩家有挂起的[无为转变]选择时，这次 swap 视为"选中的化身"，
 *     转交给 IdleTransformation 施加到被击中的实体上，并否决玩家自身变身；
 *  3) 配置关闭时放行；
 *  4) 主手是「魂刀·真人」时放行，否则否决。
 */
@Mod.EventBusSubscriber(modid = SoulKatana.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class IdentityInterop {
    private static final Logger LOGGER = LoggerFactory.getLogger("SoulKatana|IdentityInterop");

    public static final String IDENTITY_MODID = "identity";
    private static final String SWAP_INTERFACE = "draylar.identity.api.event.IdentitySwapCallback";
    private static final String UNLOCK_INTERFACE = "draylar.identity.api.event.UnlockIdentityCallback";
    private static final String EVENT_RESULT_CLASS = "dev.architectury.event.EventResult";

    /** Identity 在场且回调注册成功（setup 阶段尝试；失败时首用懒重试）。 */
    private static volatile boolean wired;

    /** 最近一次注册失败的原因（供 /soulkatana status 自查展示）。 */
    private static volatile String lastWireError;

    /** 挂接尝试节流时间戳（毫秒）。 */
    private static volatile long lastAttemptMs;

    // EventResult 的静态工厂（反射缓存）
    private static Object passResult;
    private static Object denyResult;

    // 动态代理监听器必须持有强引用，防止被 GC
    private static Object swapListener;
    private static Object unlockListener;

    private IdentityInterop() { }

    /** Identity 已装载（与是否挂接成功无关，供状态自查展示）。 */
    public static boolean isIdentityLoaded() {
        return ModList.get().isLoaded(IDENTITY_MODID);
    }

    /** Identity 已装载且审核钩子挂接成功；setup 期失败时在服务端线程懒重试一次。 */
    public static boolean isAvailable() {
        if (wired) {
            return true;
        }
        if (!isIdentityLoaded()) {
            return false;
        }
        attemptWire();
        return wired;
    }

    /** 联动挂接是否已成功（/soulkatana status 用）。 */
    public static boolean isWired() {
        return wired;
    }

    /** 最近一次挂接失败原因（成功过则为 null）。 */
    public static String lastWireError() {
        return lastWireError;
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!isIdentityLoaded()) {
            LOGGER.info("[魂刀] 未检测到「化身」(identity)，跳过变身联动（模组其余功能不受影响）。");
            return;
        }
        attemptWire();
    }

    /** 幂等的回调挂接：setup 阶段与首次施放时共用，失败只留日志不崩溃。 */
    private static synchronized void attemptWire() {
        if (wired || !isIdentityLoaded()) {
            return;
        }
        // 失败节流：最多每 10 秒重试一次，避免每次右键都刷一遍异常日志
        long now = System.currentTimeMillis();
        if (now - lastAttemptMs < 10_000L) {
            return;
        }
        lastAttemptMs = now;
        try {
            resolveEventResult();
            swapListener = registerListener(SWAP_INTERFACE, "swap", IdentityInterop::dispatchSwap);
            unlockListener = registerListener(UNLOCK_INTERFACE, "unlock", IdentityInterop::dispatchUnlock);
            wired = true;
            lastWireError = null;
            LOGGER.info("[魂刀] 已接管「化身」变身入口：获得/切换化身需要主手持「魂刀·真人」，"
                    + "[无为转变]将弹出化身选择界面。");
        } catch (Throwable t) {
            wired = false;
            lastWireError = t.getClass().getSimpleName() + ": " + t.getMessage();
            LOGGER.warn("[魂刀] 挂接「化身」变身审核钩子失败（{}），本次暂不做变身联动，"
                    + "施放[无为转变]将退回随机转变。", lastWireError, t);
        }
    }

    // ------------------------------------------------------------------
    // 回调逻辑（参数按官方映射的运行时类型接收；identity 类一律以 Object 处理）
    // ------------------------------------------------------------------

    /** IdentitySwapCallback.swap(ServerPlayer, @Nullable LivingEntity to) -> EventResult */
    private static Object dispatchSwap(Object player, Object to) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return passResult;
        }
        // 1) 变回玩家本人：永远放行；若此时有挂起的无为转变，视为玩家取消选择
        if (to == null) {
            boolean hadPending = IdleTransformation.hasPending(serverPlayer);
            IdleTransformation.clearPending(serverPlayer);
            if (hadPending) {
                // 取消也是"选择完毕"：服务端驱动关闭化身选择界面
                SoulKatanaNetwork.sendCloseIdentityMenu(serverPlayer);
            }
            return passResult;
        }
        // 2) 无为转变：玩家刚在化身选择界面里点中的那个化身，施加到被击中的实体上
        if (IdleTransformation.hasPending(serverPlayer)) {
            if (to instanceof LivingEntity formEntity) {
                IdleTransformation.applyPending(serverPlayer, formEntity);
            } else {
                IdleTransformation.clearPending(serverPlayer);
            }
            // 选择已被消费（无论施加成功还是目标丢失）：自动关闭界面
            SoulKatanaNetwork.sendCloseIdentityMenu(serverPlayer);
            return denyResult; // 否决玩家自身变身
        }
        // 3) 配置关闭：完全恢复 Identity 原行为
        if (!SoulKatanaConfig.identityRequireSoulBlade()) {
            return passResult;
        }
        // 4) 主手必须持「魂刀·真人」才允许获得/切换化身（静默否决，不给提示）
        ItemStack main = serverPlayer.getMainHandItem();
        if (SoulKatanaIdentity.isRealPerson(main)) {
            return passResult;
        }
        return denyResult;
    }

    /** UnlockIdentityCallback.unlock(ServerPlayer, IdentityType) -> EventResult（"得到化身"同样受闸门管）。 */
    private static Object dispatchUnlock(Object player, Object identityType) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return passResult;
        }
        if (IdleTransformation.hasPending(serverPlayer)) {
            // 无为转变选择过程中顺带触发的解锁不拦截
            return passResult;
        }
        if (!SoulKatanaConfig.identityRequireSoulBlade()) {
            return passResult;
        }
        if (SoulKatanaIdentity.isRealPerson(serverPlayer.getMainHandItem())) {
            return passResult;
        }
        return denyResult;
    }

    /**
     * 让客户端弹出 Identity 自带的化身选择界面（IdentityScreen，无参构造）。
     * 选择结果会以一次 swap 回调回到服务端，由 dispatchSwap 转交给无为转变。
     */
    public static void openIdentityMenu(ServerPlayer player) {
        SoulKatanaNetwork.sendOpenIdentityMenu(player);
    }

    // ------------------------------------------------------------------
    // 反射工具
    // ------------------------------------------------------------------

    private static void resolveEventResult() throws Exception {
        Class<?> eventResult = Class.forName(EVENT_RESULT_CLASS);
        passResult = eventResult.getMethod("pass").invoke(null);
        denyResult = eventResult.getMethod("interruptFalse").invoke(null);
    }

    /** 取 iface.EVENT，把 JDK 动态代理监听器注册进 Architectury 事件容器。 */
    private static Object registerListener(String interfaceName, String callbackMethod,
                                           ListenerBody body) throws Exception {
        Class<?> iface = Class.forName(interfaceName);
        Object event = iface.getField("EVENT").get(null);

        Object listener = Proxy.newProxyInstance(
                iface.getClassLoader(), new Class<?>[]{iface},
                (InvocationHandler) (proxy, method, args) -> {
                    if (method.getName().equals(callbackMethod) && args != null) {
                        Object player = args.length > 0 ? args[0] : null;
                        Object second = args.length > 1 ? args[1] : null;
                        return body.handle(player, second);
                    }
                    // toString/hashCode/equals 等
                    if ("toString".equals(method.getName())) {
                        return "soulkatana:" + interfaceName;
                    }
                    return null;
                });

        // 关键：Method 必须取自 architectury 公开导出的 dev.architectury.event.Event 接口，
        // 而不是事件对象的运行时类。运行时类是 EventFactory$EventImpl——非公开内部类，
        // 跨 JPMS 模块以它为声明类反射调用 register 必抛 IllegalAccessException
        // （旧实现在这里 event.getClass().getMethods() 拿到的是内部类的覆写版本）。
        // 以公开接口为声明类的 Method 走虚分派落到同一实现，等价且可访问
        // （与 resolveEventResult 里反射调用 EventResult.pass()/interruptFalse() 同理）。
        Method register = findRegisterOnPublicType(event);
        if (register == null) {
            throw new NoSuchMethodException("Event#register(T) not found on public supertypes of "
                    + event.getClass().getName());
        }
        register.invoke(event, listener);
        return listener;
    }

    /** architectury 事件容器接口（公开，register 即声明于此）。 */
    private static final String EVENT_INTERFACE = "dev.architectury.event.Event";

    /**
     * 在事件对象的所有【公开】父类型（优先 EVENT_INTERFACE，其次实现接口与父类接口）上
     * 查找实例方法 register(单参)。任何一步拿不到都返回 null，由调用方抛错并降级。
     */
    private static Method findRegisterOnPublicType(Object event) {
        try {
            Class<?> eventIface = Class.forName(EVENT_INTERFACE);
            Method m = pickRegister(eventIface);
            if (m != null && eventIface.isInstance(event)) {
                return m;
            }
        } catch (Throwable expectedIfRenamed) {
            // architectury 版本里没有该接口名：走通用扫描
        }
        for (Class<?> c = event.getClass(); c != null; c = c.getSuperclass()) {
            for (Class<?> it : c.getInterfaces()) {
                if (!java.lang.reflect.Modifier.isPublic(it.getModifiers())) {
                    continue;
                }
                Method m = pickRegister(it);
                if (m != null) {
                    return m;
                }
            }
        }
        return null;
    }

    /** 从公开类型上取非静态、单参、名为 register 的公开方法（擦除后签名为 register(Object)）。 */
    private static Method pickRegister(Class<?> type) {
        for (Method m : type.getMethods()) {
            if (m.getName().equals("register") && !m.isSynthetic()
                    && m.getParameterCount() == 1 && !java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                return m;
            }
        }
        return null;
    }

    @FunctionalInterface
    private interface ListenerBody {
        Object handle(Object player, Object second);
    }
}
