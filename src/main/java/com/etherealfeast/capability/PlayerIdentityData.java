package com.etherealfeast.capability;

import com.etherealfeast.EtherealFeast;
import com.etherealfeast.item.BaiWeiItem;
import com.etherealfeast.network.SyncIdentityPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class PlayerIdentityData {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, EtherealFeast.MOD_ID);

    private static final AccessorySerializer ACCESSORY_SERIALIZER = new AccessorySerializer();

    public static final Supplier<AttachmentType<AccessorySlot>> ACCESSORY =
            ATTACHMENT_TYPES.register("accessory",
                    () -> AttachmentType.<AccessorySlot>builder(AccessorySlot::new)
                            .serialize(ACCESSORY_SERIALIZER)
                            .build());

    private static final IdentityDataSerializer ID_SERIALIZER = new IdentityDataSerializer();
    public static final Supplier<AttachmentType<IdentityData>> IDENTITY_DATA =
            ATTACHMENT_TYPES.register("identity_data",
                    () -> AttachmentType.<IdentityData>builder(() -> new IdentityData())
                            .serialize(ID_SERIALIZER)
                            .build());

    public static AccessorySlot getAccessory(Player player) {
        return player.getData(ACCESSORY.get());
    }

    public static void sync(ServerPlayer player) {
        AccessorySlot slot = getAccessory(player);
        CompoundTag tag = slot.hasBaiWei() ? slot.getNbt() : new CompoundTag();
        // Also update legacy IdentityData for client display
        IdentityData id = get(player);
        id.deserializeNBT(tag);
        PacketDistributor.sendToPlayer(player, new SyncIdentityPacket(tag));
    }

    public static class AccessorySlot {
        private ItemStack item = ItemStack.EMPTY;

        public ItemStack getItem() { return item; }

        public void setItem(ItemStack stack) { this.item = stack.copy(); }

        public boolean hasBaiWei() {
            return !item.isEmpty() && item.getItem() instanceof BaiWeiItem;
        }

        public CompoundTag getNbt() {
            if (!hasBaiWei()) return new CompoundTag();
            return item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        }

        private void modifyNbt(java.util.function.Consumer<CompoundTag> modifier) {
            CompoundTag tag = getNbt();
            modifier.accept(tag);
            item.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }

        /** Called on client from sync packet to update NBT */
        public void loadNbtFromSync(CompoundTag tag) {
            if (!tag.isEmpty() && hasBaiWei()) {
                item.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
        }

        public BaiWeiItem.IdentityType getIdentityType() {
            String s = getNbt().getString("IdentityType");
            return s.equals("team") ? BaiWeiItem.IdentityType.TEAM : BaiWeiItem.IdentityType.SOLO;
        }

        public int getFeastLevel() { return Math.max(1, Math.min(getNbt().getInt("FeastLevel"), 6)); }
        public int getFeastExp() { return getNbt().getInt("FeastExp"); }
        public boolean isDamaged() { return getNbt().getBoolean("IsDamaged"); }
        public boolean isBound() { return hasBaiWei() && getNbt().getBoolean("IsBound"); }

        public void bindIdentity(BaiWeiItem.IdentityType type) {
            modifyNbt(tag -> {
                tag.putString("IdentityType", type.id);
                tag.putInt("FeastLevel", 1);
                tag.putInt("FeastExp", 0);
                tag.putBoolean("IsDamaged", false);
                tag.putBoolean("IsBound", true);
            });
        }

        public void addExp(int amount) {
            modifyNbt(tag -> {
                int newExp = tag.getInt("FeastExp") + amount;
                tag.putInt("FeastExp", newExp);
                int[] t = {0, 3000, 8000, 14000, 20000, 30000};
                int lv = Math.max(1, tag.getInt("FeastLevel"));
                while (lv < t.length && newExp >= t[lv]) lv++;
                tag.putInt("FeastLevel", lv);
            });
        }

        public void setExp(int amount) { modifyNbt(tag -> tag.putInt("FeastExp", amount)); }
        public void setLevel(int level) { modifyNbt(tag -> tag.putInt("FeastLevel", Math.max(1, Math.min(level, 6)))); }
        public void setDamaged(boolean d) { modifyNbt(tag -> tag.putBoolean("IsDamaged", d)); }

        public int getExpForNextLevel() {
            int[] t = {0, 3000, 8000, 14000, 20000, 30000};
            int lv = getFeastLevel();
            return lv >= t.length ? -1 : t[lv];
        }

        public int getCurrentLevelThreshold() {
            int[] t = {0, 3000, 8000, 14000, 20000, 30000};
            int lv = getFeastLevel();
            return lv <= 0 ? 0 : t[lv - 1];
        }

        public static int getThresholdForLevel(int level) {
            int[] t = {0, 3000, 8000, 14000, 20000, 30000};
            return (level <= 0 || level > t.length) ? -1 : t[level - 1];
        }
    }

    public static IdentityData get(Player player) {
        return player.getData(IDENTITY_DATA.get());
    }

    private static class AccessorySerializer implements IAttachmentSerializer<CompoundTag, AccessorySlot> {
        public AccessorySlot read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            AccessorySlot slot = new AccessorySlot();
            if (tag.contains("item"))
                slot.setItem(ItemStack.parseOptional(provider, tag.getCompound("item")));
            return slot;
        }
        public CompoundTag write(AccessorySlot slot, HolderLookup.Provider provider) {
            CompoundTag tag = new CompoundTag();
            if (!slot.item.isEmpty()) {
                Tag saved = slot.item.save(provider);
                tag.put("item", saved);
            }
            return tag;
        }
    }

    public static class IdentityData {
        private BaiWeiItem.IdentityType identityType = BaiWeiItem.IdentityType.SOLO;
        private int feastExp, feastLevel = 1;
        private boolean isDamaged, bound;

        public BaiWeiItem.IdentityType getIdentityType() { return identityType; }
        public int getFeastExp() { return feastExp; }
        public int getFeastLevel() { return feastLevel; }
        public boolean isDamaged() { return isDamaged; }
        public boolean isBound() { return bound; }
        public void bindIdentity(BaiWeiItem.IdentityType t) { identityType = t; feastLevel = 1; feastExp = 0; isDamaged = false; bound = true; }
        public void setItemDamaged(boolean d) { isDamaged = d; }
        public void addExp(int a) { feastExp += a; }
        public void setExp(int a) { feastExp = a; }
        public void setLevel(int l) { feastLevel = l; }
        public int getExpForNextLevel() { int[] t = {0, 3000, 8000, 14000, 20000, 30000}; return feastLevel >= t.length ? -1 : t[feastLevel]; }
        public int getCurrentLevelThreshold() { int[] t = {0, 3000, 8000, 14000, 20000, 30000}; return feastLevel <= 0 ? 0 : t[feastLevel-1]; }
        public static int getThresholdForLevel(int l) { int[] t = {0, 3000, 8000, 14000, 20000, 30000}; return l <= 0 || l > t.length ? -1 : t[l-1]; }
        public CompoundTag serializeNBT() { CompoundTag t = new CompoundTag(); t.putString("IdentityType", identityType.id); t.putInt("FeastExp", feastExp); t.putInt("FeastLevel", feastLevel); t.putBoolean("IsDamaged", isDamaged); t.putBoolean("IsBound", bound); return t; }
        public void deserializeNBT(CompoundTag t) { identityType = "team".equals(t.getString("IdentityType")) ? BaiWeiItem.IdentityType.TEAM : BaiWeiItem.IdentityType.SOLO; feastExp = t.getInt("FeastExp"); feastLevel = Math.max(1, t.getInt("FeastLevel")); isDamaged = t.getBoolean("IsDamaged"); bound = t.getBoolean("IsBound"); }
    }

    private static class IdentityDataSerializer implements IAttachmentSerializer<CompoundTag, IdentityData> {
        public IdentityData read(IAttachmentHolder h, CompoundTag t, HolderLookup.Provider p) { IdentityData d = new IdentityData(); d.deserializeNBT(t); return d; }
        public CompoundTag write(IdentityData d, HolderLookup.Provider p) { return d.serializeNBT(); }
    }

    public static class Events {
        public static void register() {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(new Object() {
                @SubscribeEvent
                public void onPlayerClone(PlayerEvent.Clone event) {
                    AccessorySlot old = getAccessory(event.getOriginal());
                    if (old.hasBaiWei()) {
                        AccessorySlot newSlot = getAccessory(event.getEntity());
                        newSlot.setItem(old.getItem());
                        newSlot.setDamaged(true);
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
