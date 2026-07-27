package com.etherealfeast.capability;

import com.etherealfeast.EtherealFeast;
import com.etherealfeast.item.BaiWeiItem;
import com.etherealfeast.network.SyncIdentityPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Accessory slot for BaiWei items, stored as an ItemStack in player data.
 * The ItemStack NBT holds: identity, level, exp, damaged.
 */
public class PlayerIdentityData {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, EtherealFeast.MOD_ID);

    private static final AccessorySerializer ACCESSORY_SERIALIZER = new AccessorySerializer();

    /** Holds the BaiWei item stack in the accessory slot */
    public static final Supplier<AttachmentType<AccessorySlot>> ACCESSORY =
            ATTACHMENT_TYPES.register("accessory",
                    () -> AttachmentType.<AccessorySlot>builder(AccessorySlot::new)
                            .serialize(ACCESSORY_SERIALIZER)
                            .build());

    // Legacy: also keep IdentityData for backward compat
    private static final IdentityDataSerializer SERIALIZER = new IdentityDataSerializer();
    public static final Supplier<AttachmentType<IdentityData>> IDENTITY_DATA =
            ATTACHMENT_TYPES.register("identity_data",
                    () -> AttachmentType.<IdentityData>builder(() -> new IdentityData())
                            .serialize(SERIALIZER)
                            .build());

    // === Accessory Slot ===

    public static AccessorySlot getAccessory(Player player) {
        return player.getData(ACCESSORY.get());
    }

    public static void sync(ServerPlayer player) {
        AccessorySlot slot = getAccessory(player);
        PacketDistributor.sendToPlayer(player,
                new SyncIdentityPacket(slot.getItem().save(player.registryAccess())));
    }

    public static class AccessorySlot {
        private ItemStack item = ItemStack.EMPTY;

        public ItemStack getItem() { return item; }

        public void setItem(ItemStack stack) {
            this.item = stack.copy();
        }

        public boolean hasBaiWei() {
            return !item.isEmpty() && item.getItem() instanceof BaiWeiItem;
        }

        public CompoundTag getNbt() {
            if (!hasBaiWei()) return new CompoundTag();
            CompoundTag tag = item.getOrCreateTag();
            return tag;
        }

        // NBT helpers
        public BaiWeiItem.IdentityType getIdentityType() {
            String s = getNbt().getString("IdentityType");
            if (s.isEmpty()) return BaiWeiItem.IdentityType.SOLO;
            return s.equals("team") ? BaiWeiItem.IdentityType.TEAM : BaiWeiItem.IdentityType.SOLO;
        }

        public int getFeastLevel() {
            int lv = getNbt().getInt("FeastLevel");
            return Math.max(1, Math.min(lv, 6));
        }

        public int getFeastExp() {
            return getNbt().getInt("FeastExp");
        }

        public boolean isDamaged() {
            return getNbt().getBoolean("IsDamaged");
        }

        public boolean isBound() {
            return hasBaiWei() && getNbt().getBoolean("IsBound");
        }

        public void bindIdentity(BaiWeiItem.IdentityType type) {
            CompoundTag tag = getNbt();
            tag.putString("IdentityType", type.id);
            tag.putInt("FeastLevel", 1);
            tag.putInt("FeastExp", 0);
            tag.putBoolean("IsDamaged", false);
            tag.putBoolean("IsBound", true);
        }

        public void addExp(int amount) {
            CompoundTag tag = getNbt();
            tag.putInt("FeastExp", getFeastExp() + amount);
            checkLevelUp();
        }

        public void setExp(int amount) { getNbt().putInt("FeastExp", amount); }

        public void setLevel(int level) { getNbt().putInt("FeastLevel", Math.max(1, Math.min(level, 6))); }

        public void setDamaged(boolean d) { getNbt().putBoolean("IsDamaged", d); }

        public int getExpForNextLevel() {
            int lv = getFeastLevel();
            int[] t = {0, 3000, 8000, 14000, 20000, 30000};
            if (lv >= t.length) return -1;
            return t[lv];
        }

        public int getCurrentLevelThreshold() {
            int lv = getFeastLevel();
            int[] t = {0, 3000, 8000, 14000, 20000, 30000};
            if (lv <= 0) return 0;
            return t[lv - 1];
        }

        public static int getThresholdForLevel(int level) {
            int[] t = {0, 3000, 8000, 14000, 20000, 30000};
            if (level <= 0 || level > t.length) return -1;
            return t[level - 1];
        }

        private void checkLevelUp() {
            int[] t = {0, 3000, 8000, 14000, 20000, 30000};
            while (getFeastLevel() < t.length && getFeastExp() >= t[getFeastLevel()]) {
                setLevel(getFeastLevel() + 1);
            }
        }
    }

    // === Legacy IdentityData (kept for data migration) ===

    public static IdentityData get(Player player) {
        return player.getData(IDENTITY_DATA.get());
    }

    private static class AccessorySerializer implements IAttachmentSerializer<CompoundTag, AccessorySlot> {
        @Override
        public AccessorySlot read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            AccessorySlot slot = new AccessorySlot();
            if (tag.contains("item")) {
                slot.setItem(ItemStack.parseOptional(provider, tag.getCompound("item")));
            }
            return slot;
        }

        @Override
        public CompoundTag write(AccessorySlot slot, HolderLookup.Provider provider) {
            CompoundTag tag = new CompoundTag();
            if (!slot.item.isEmpty()) {
                tag.put("item", slot.item.save(provider));
            }
            return tag;
        }
    }

    public static class IdentityData {
        // ... (unchanged from before, kept for data migration)
        private BaiWeiItem.IdentityType identityType = BaiWeiItem.IdentityType.SOLO;
        private int feastExp = 0;
        private int feastLevel = 1;
        private boolean isDamaged = false;
        private boolean bound = false;

        public BaiWeiItem.IdentityType getIdentityType() { return identityType; }
        public int getFeastExp() { return feastExp; }
        public int getFeastLevel() { return feastLevel; }
        public boolean isDamaged() { return isDamaged; }
        public boolean isBound() { return bound; }
        public void bindIdentity(BaiWeiItem.IdentityType type) { this.identityType = type; this.feastLevel = 1; this.feastExp = 0; this.isDamaged = false; this.bound = true; }
        public void setItemDamaged(boolean d) { this.isDamaged = d; }
        public void addExp(int a) { feastExp += a; }
        public void setExp(int a) { feastExp = a; }
        public void setLevel(int l) { feastLevel = l; }
        public int getExpForNextLevel() { int l = feastLevel; int[] t = {0, 3000, 8000, 14000, 20000, 30000}; return l >= t.length ? -1 : t[l]; }
        public int getCurrentLevelThreshold() { int l = feastLevel; int[] t = {0, 3000, 8000, 14000, 20000, 30000}; return l <= 0 ? 0 : t[l-1]; }
        public static int getThresholdForLevel(int l) { int[] t = {0, 3000, 8000, 14000, 20000, 30000}; return l <= 0 || l > t.length ? -1 : t[l-1]; }

        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("IdentityType", identityType.id);
            tag.putInt("FeastExp", feastExp);
            tag.putInt("FeastLevel", feastLevel);
            tag.putBoolean("IsDamaged", isDamaged);
            tag.putBoolean("IsBound", bound);
            return tag;
        }
        public void deserializeNBT(CompoundTag tag) {
            if (tag.contains("IdentityType")) {
                String s = tag.getString("IdentityType");
                identityType = s.equals("team") ? BaiWeiItem.IdentityType.TEAM : BaiWeiItem.IdentityType.SOLO;
            }
            feastExp = tag.getInt("FeastExp");
            feastLevel = tag.getInt("FeastLevel");
            if (feastLevel < 1) feastLevel = 1;
            isDamaged = tag.getBoolean("IsDamaged");
            bound = tag.getBoolean("IsBound");
        }
    }

    private static class IdentityDataSerializer implements IAttachmentSerializer<CompoundTag, IdentityData> {
        @Override
        public IdentityData read(IAttachmentHolder h, CompoundTag t, HolderLookup.Provider p) { IdentityData d = new IdentityData(); d.deserializeNBT(t); return d; }
        @Override
        public CompoundTag write(IdentityData d, HolderLookup.Provider p) { return d.serializeNBT(); }
    }

    /** Game events */
    public static class Events {
        public static void register() {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(new Object() {
                @SubscribeEvent
                public void onPlayerClone(PlayerEvent.Clone event) {
                    Player oldP = event.getOriginal();
                    Player newP = event.getEntity();
                    AccessorySlot old = getAccessory(oldP);
                    if (old.hasBaiWei()) {
                        getAccessory(newP).setItem(old.getItem());
                        old.setDamaged(true);
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
