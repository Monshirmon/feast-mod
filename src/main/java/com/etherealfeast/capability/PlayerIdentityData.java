package com.etherealfeast.capability;

import com.etherealfeast.EtherealFeast;
import com.etherealfeast.item.BaiWeiItem;
import com.etherealfeast.network.SyncIdentityPacket;
import com.etherealfeast.registry.ModItems;
import com.etherealfeast.taste.TasteType;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PlayerIdentityData {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, EtherealFeast.MOD_ID);

    private static final IdentityDataSerializer ID_SERIALIZER = new IdentityDataSerializer();

    /** Client-side display data, synced from server */
    public static final Supplier<AttachmentType<IdentityData>> IDENTITY_DATA =
            ATTACHMENT_TYPES.register("identity_data",
                    () -> AttachmentType.<IdentityData>builder(() -> new IdentityData())
                            .serialize(ID_SERIALIZER)
                            .build());

    // ==================== Curios-based helpers ====================

    /** Get the ItemStack from the Curios "cookbook" slot. Works on both sides. */
    public static ItemStack getCookbookStack(Player player) {
        Optional<top.theillusivec4.curios.api.type.capability.ICuriosItemHandler> curios =
                CuriosApi.getCuriosInventory(player);
        if (curios.isEmpty()) return ItemStack.EMPTY;
        return curios.get().getStacksHandler("cookbook")
                .map(h -> h.getStacks().getStackInSlot(0))
                .orElse(ItemStack.EMPTY);
    }

    /** Check if the player has a valid BaiWei item equipped in the cookbook slot */
    public static boolean hasBaiWeiEquipped(Player player) {
        ItemStack stack = getCookbookStack(player);
        return !stack.isEmpty() && stack.getItem() instanceof BaiWeiItem;
    }

    /** Read CustomData tag from the cookbook Curios slot. Returns empty tag if not equipped. */
    private static CompoundTag getCurioTag(Player player) {
        ItemStack stack = getCookbookStack(player);
        if (!stack.isEmpty() && stack.getItem() instanceof BaiWeiItem) {
            return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        }
        return new CompoundTag();
    }

    /** Modify CustomData on the cookbook stack in Curios slot (server only). Syncs automatically after modification. */
    private static void modifyCurioTag(ServerPlayer player, Consumer<CompoundTag> modifier) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            handler.getStacksHandler("cookbook").ifPresent(stacksHandler -> {
                ItemStack stack = stacksHandler.getStacks().getStackInSlot(0);
                if (!stack.isEmpty() && stack.getItem() instanceof BaiWeiItem) {
                    CustomData cd = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                    CompoundTag tag = cd.copyTag();
                    modifier.accept(tag);
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    stacksHandler.getStacks().setStackInSlot(0, stack);
                }
            });
        });
        sync(player);
    }

    // ==================== Read methods (work on both sides) ====================

    public static boolean isBound(Player player) {
        return hasBaiWeiEquipped(player) && getCurioTag(player).getBoolean("IsBound");
    }

    public static BaiWeiItem.IdentityType getIdentityType(Player player) {
        String s = getCurioTag(player).getString("IdentityType");
        return "team".equals(s) ? BaiWeiItem.IdentityType.TEAM : BaiWeiItem.IdentityType.SOLO;
    }

    public static int getFeastLevel(Player player) {
        return Math.max(1, Math.min(getCurioTag(player).getInt("FeastLevel"), 6));
    }

    public static int getFeastExp(Player player) {
        return getCurioTag(player).getInt("FeastExp");
    }

    public static boolean isDamaged(Player player) {
        return getCurioTag(player).getBoolean("IsDamaged");
    }

    public static int getExpForNextLevel(Player player) {
        int[] t = {0, 3000, 8000, 14000, 20000, 30000};
        int lv = getFeastLevel(player);
        return lv >= t.length ? -1 : t[lv];
    }

    public static int getCurrentLevelThreshold(Player player) {
        int[] t = {0, 3000, 8000, 14000, 20000, 30000};
        int lv = getFeastLevel(player);
        return lv <= 0 ? 0 : t[lv - 1];
    }

    public static int getThresholdForLevel(int level) {
        int[] t = {0, 3000, 8000, 14000, 20000, 30000};
        return (level <= 0 || level > t.length) ? -1 : t[level - 1];
    }

    // ==================== Write methods (server only) ====================

    /** Equip an ItemStack into the Curios cookbook slot */
    public static void equipCookbook(ServerPlayer player, ItemStack stack) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            handler.getStacksHandler("cookbook").ifPresent(stacksHandler -> {
                stacksHandler.getStacks().setStackInSlot(0, stack.copy());
            });
        });
    }

    /** Initialize binding data on the equipped cookbook */
    public static void bindIdentity(ServerPlayer player, BaiWeiItem.IdentityType type) {
        final String[][] tastes = generateLikesAndDislikes();
        modifyCurioTag(player, tag -> {
            tag.putString("IdentityType", type.id);
            tag.putInt("FeastLevel", 1);
            tag.putInt("FeastExp", 0);
            tag.putBoolean("IsDamaged", false);
            tag.putBoolean("IsBound", true);
            tag.putString("TasteLikes", String.join(",", tastes[0]));
            tag.putString("TasteDislikes", String.join(",", tastes[1]));
        });
    }

    /** Generate 2 likes + 2 dislikes, guaranteed no overlap (4 distinct tastes from 6) */
    private static String[][] generateLikesAndDislikes() {
        List<TasteType> all = new ArrayList<>(List.of(TasteType.values()));
        Collections.shuffle(all, new Random());
        return new String[][] {
            { all.get(0).id, all.get(1).id },
            { all.get(2).id, all.get(3).id }
        };
    }

    /** Get player's preferred taste IDs */
    public static List<String> getTasteLikes(Player player) {
        String s = getCurioTag(player).getString("TasteLikes");
        if (s.isEmpty()) return List.of();
        return Arrays.asList(s.split(","));
    }

    /** Get player's disliked taste IDs */
    public static List<String> getTasteDislikes(Player player) {
        String s = getCurioTag(player).getString("TasteDislikes");
        if (s.isEmpty()) return List.of();
        return Arrays.asList(s.split(","));
    }

    public static void addExp(ServerPlayer player, int amount) {
        modifyCurioTag(player, tag -> {
            int newExp = tag.getInt("FeastExp") + amount;
            tag.putInt("FeastExp", newExp);
            int[] t = {0, 3000, 8000, 14000, 20000, 30000};
            int lv = Math.max(1, tag.getInt("FeastLevel"));
            while (lv < t.length && newExp >= t[lv]) lv++;
            tag.putInt("FeastLevel", lv);
        });
    }

    public static void setExp(ServerPlayer player, int amount) {
        modifyCurioTag(player, tag -> tag.putInt("FeastExp", amount));
    }

    public static void setLevel(ServerPlayer player, int level) {
        modifyCurioTag(player, tag -> tag.putInt("FeastLevel", Math.max(1, Math.min(level, 6))));
    }

    public static void setDamaged(ServerPlayer player, boolean damaged) {
        modifyCurioTag(player, tag -> tag.putBoolean("IsDamaged", damaged));
    }

    // ==================== Sync ====================

    /** Sync identity data to client for HUD rendering */
    public static void sync(ServerPlayer player) {
        ItemStack stack = getCookbookStack(player);
        CompoundTag tag;
        if (!stack.isEmpty() && stack.getItem() instanceof BaiWeiItem) {
            tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        } else {
            tag = new CompoundTag();
        }
        // Update client-side IdentityData
        IdentityData id = player.getData(IDENTITY_DATA.get());
        id.deserializeNBT(tag);
        PacketDistributor.sendToPlayer(player, new SyncIdentityPacket(tag));
    }

    // ==================== IdentityData (client-side display) ====================

    /** Get client-side IdentityData attachment for HUD/etc */
    public static IdentityData get(Player player) {
        return player.getData(IDENTITY_DATA.get());
    }

    public static class IdentityData {
        private BaiWeiItem.IdentityType identityType = BaiWeiItem.IdentityType.SOLO;
        private int feastExp, feastLevel = 1;
        private boolean isDamaged, bound;
        private List<String> tasteLikes = List.of();
        private List<String> tasteDislikes = List.of();

        public BaiWeiItem.IdentityType getIdentityType() { return identityType; }
        public int getFeastExp() { return feastExp; }
        public int getFeastLevel() { return feastLevel; }
        public boolean isDamaged() { return isDamaged; }
        public boolean isBound() { return bound; }
        public List<String> getTasteLikes() { return tasteLikes; }
        public List<String> getTasteDislikes() { return tasteDislikes; }

        public void bindIdentity(BaiWeiItem.IdentityType t) {
            identityType = t; feastLevel = 1; feastExp = 0; isDamaged = false; bound = true;
        }
        public void setItemDamaged(boolean d) { isDamaged = d; }
        public void addExp(int a) { feastExp += a; }
        public void setExp(int a) { feastExp = a; }
        public void setLevel(int l) { feastLevel = l; }
        public int getExpForNextLevel() {
            int[] t = {0, 3000, 8000, 14000, 20000, 30000};
            return feastLevel >= t.length ? -1 : t[feastLevel];
        }
        public int getCurrentLevelThreshold() {
            int[] t = {0, 3000, 8000, 14000, 20000, 30000};
            return feastLevel <= 0 ? 0 : t[feastLevel - 1];
        }
        public static int getThresholdForLevel(int l) {
            int[] t = {0, 3000, 8000, 14000, 20000, 30000};
            return l <= 0 || l > t.length ? -1 : t[l - 1];
        }

        public CompoundTag serializeNBT() {
            CompoundTag t = new CompoundTag();
            t.putString("IdentityType", identityType.id);
            t.putInt("FeastExp", feastExp);
            t.putInt("FeastLevel", feastLevel);
            t.putBoolean("IsDamaged", isDamaged);
            t.putBoolean("IsBound", bound);
            if (!tasteLikes.isEmpty()) t.putString("TasteLikes", String.join(",", tasteLikes));
            if (!tasteDislikes.isEmpty()) t.putString("TasteDislikes", String.join(",", tasteDislikes));
            return t;
        }
        public void deserializeNBT(CompoundTag t) {
            identityType = "team".equals(t.getString("IdentityType"))
                    ? BaiWeiItem.IdentityType.TEAM : BaiWeiItem.IdentityType.SOLO;
            feastExp = t.getInt("FeastExp");
            feastLevel = Math.max(1, t.getInt("FeastLevel"));
            isDamaged = t.getBoolean("IsDamaged");
            bound = t.getBoolean("IsBound");
            String likesStr = t.getString("TasteLikes");
            tasteLikes = likesStr.isEmpty() ? List.of() : Arrays.asList(likesStr.split(","));
            String dislikesStr = t.getString("TasteDislikes");
            tasteDislikes = dislikesStr.isEmpty() ? List.of() : Arrays.asList(dislikesStr.split(","));
        }
    }

    private static class IdentityDataSerializer implements IAttachmentSerializer<CompoundTag, IdentityData> {
        public IdentityData read(IAttachmentHolder h, CompoundTag t, HolderLookup.Provider p) {
            IdentityData d = new IdentityData();
            if (!t.isEmpty()) d.deserializeNBT(t);
            return d;
        }
        public CompoundTag write(IdentityData d, HolderLookup.Provider p) {
            return d.serializeNBT();
        }
    }

    // ==================== Events ====================

    public static class Events {
        public static void register() {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(new Object() {
                @SubscribeEvent(priority = EventPriority.LOWEST)
                public void onPlayerClone(PlayerEvent.Clone event) {
                    if (event.getEntity() instanceof ServerPlayer newPlayer
                            && event.getOriginal() instanceof ServerPlayer original) {

                        // Read from attachment – survives Curios inventory clearing
                        IdentityData oldData = original.getData(IDENTITY_DATA.get());

                        // Clear any stale Curios data on the new player
                        equipCookbook(newPlayer, ItemStack.EMPTY);

                        if (oldData.isBound()) {
                            // Create new damaged BaiWei of the same identity type
                            Item item = oldData.getIdentityType() == BaiWeiItem.IdentityType.SOLO
                                    ? ModItems.BAIWEI_DUZHUO.get()
                                    : ModItems.BAIWEI_GONGXIANG.get();
                            equipCookbook(newPlayer, new ItemStack(item));

                            // Restore preserved data with damaged flag
                            modifyCurioTag(newPlayer, tag -> {
                                tag.putString("IdentityType", oldData.getIdentityType().id);
                                tag.putInt("FeastLevel", oldData.getFeastLevel());
                                tag.putInt("FeastExp", oldData.getFeastExp());
                                tag.putBoolean("IsDamaged", true);
                                tag.putBoolean("IsBound", true);
                                tag.putString("TasteLikes", String.join(",", oldData.getTasteLikes()));
                                tag.putString("TasteDislikes", String.join(",", oldData.getTasteDislikes()));
                            });
                        }
                    }
                }
                @SubscribeEvent
                public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
                    if (event.getEntity() instanceof ServerPlayer p) sync(p);
                }
            });
        }
    }
}
