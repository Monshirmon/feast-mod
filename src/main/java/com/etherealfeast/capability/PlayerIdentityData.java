package com.etherealfeast.capability;

import com.etherealfeast.EtherealFeast;
import com.etherealfeast.item.BaiWeiItem;
import com.etherealfeast.network.ModNetwork;
import com.etherealfeast.network.SyncIdentityPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class PlayerIdentityData {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, EtherealFeast.MOD_ID);

    public static final Supplier<AttachmentType<IdentityData>> IDENTITY_DATA =
            ATTACHMENT_TYPES.register("identity_data",
                    () -> AttachmentType.<IdentityData>builder(() -> new IdentityData())
                            .serialize(IdentityData::new)
                            .build());

    public static void register() {
    }

    public static IdentityData get(Player player) {
        return player.getData(IDENTITY_DATA.get());
    }

    public static void sync(ServerPlayer player) {
        IdentityData data = get(player);
        PacketDistributor.sendToPlayer(player, new SyncIdentityPacket(data.serializeNBT()));
    }

    public static class IdentityData implements IAttachmentSerializer<CompoundTag, IdentityData> {
        private static final int[] EXP_THRESHOLDS = {0, 1000, 3000, 6000, 10000};
        private static final String KEY_TYPE = "IdentityType";
        private static final String KEY_EXP = "FeastExp";
        private static final String KEY_LEVEL = "FeastLevel";
        private static final String KEY_DAMAGED = "IsDamaged";
        private static final String KEY_BOUND = "IsBound";

        private BaiWeiItem.IdentityType identityType = BaiWeiItem.IdentityType.SOLO;
        private int feastExp = 0;
        private int feastLevel = 1;
        private boolean isDamaged = false;
        private boolean bound = false;

        public IdentityData() {
        }

        public IdentityData(CompoundTag tag) {
            deserializeNBT(tag);
        }

        public BaiWeiItem.IdentityType getIdentityType() {
            return identityType;
        }

        public int getFeastExp() {
            return feastExp;
        }

        public int getFeastLevel() {
            return feastLevel;
        }

        public boolean isDamaged() {
            return isDamaged;
        }

        public boolean isBound() {
            return bound;
        }

        public void bindIdentity(BaiWeiItem.IdentityType type) {
            this.identityType = type;
            this.feastLevel = 1;
            this.feastExp = 0;
            this.isDamaged = false;
            this.bound = true;
        }

        public void setItemDamaged(boolean damaged) {
            this.isDamaged = damaged;
        }

        public void addExp(int amount) {
            this.feastExp += amount;
            checkLevelUp();
        }

        public void setExp(int amount) {
            this.feastExp = amount;
        }

        public void setLevel(int level) {
            this.feastLevel = level;
        }

        public int getExpForNextLevel() {
            if (feastLevel >= EXP_THRESHOLDS.length) return -1;
            return EXP_THRESHOLDS[feastLevel];
        }

        public int getCurrentLevelThreshold() {
            return EXP_THRESHOLDS[feastLevel - 1];
        }

        public static int getThresholdForLevel(int level) {
            if (level <= 0 || level > EXP_THRESHOLDS.length) return -1;
            return EXP_THRESHOLDS[level - 1];
        }

        private void checkLevelUp() {
            while (feastLevel < EXP_THRESHOLDS.length && feastExp >= EXP_THRESHOLDS[feastLevel]) {
                feastLevel++;
            }
        }

        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString(KEY_TYPE, identityType.id);
            tag.putInt(KEY_EXP, feastExp);
            tag.putInt(KEY_LEVEL, feastLevel);
            tag.putBoolean(KEY_DAMAGED, isDamaged);
            tag.putBoolean(KEY_BOUND, bound);
            return tag;
        }

        @Override
        public IdentityData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            IdentityData data = new IdentityData();
            data.deserializeNBT(tag);
            return data;
        }

        @Override
        public CompoundTag write(IdentityData attachment, HolderLookup.Provider provider) {
            return attachment.serializeNBT();
        }

        public void deserializeNBT(CompoundTag tag) {
            if (tag.contains(KEY_TYPE)) {
                String typeStr = tag.getString(KEY_TYPE);
                for (BaiWeiItem.IdentityType t : BaiWeiItem.IdentityType.values()) {
                    if (t.id.equals(typeStr)) {
                        this.identityType = t;
                        break;
                    }
                }
            }
            this.feastExp = tag.getInt(KEY_EXP);
            this.feastLevel = tag.getInt(KEY_LEVEL);
            if (this.feastLevel < 1) this.feastLevel = 1;
            if (this.feastLevel > EXP_THRESHOLDS.length) this.feastLevel = EXP_THRESHOLDS.length;
            this.isDamaged = tag.getBoolean(KEY_DAMAGED);
            this.bound = tag.getBoolean(KEY_BOUND);
        }
    }

    @EventBusSubscriber(modid = EtherealFeast.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
    public static class Events {
        @SubscribeEvent
        public static void onPlayerClone(PlayerEvent.Clone event) {
            Player oldPlayer = event.getOriginal();
            Player newPlayer = event.getEntity();

            IdentityData oldData = get(oldPlayer);
            IdentityData newData = get(newPlayer);

            newData.bindIdentity(oldData.getIdentityType());
            newData.setExp(oldData.getFeastExp());
            newData.setLevel(oldData.getFeastLevel());

            // On death, mark as damaged
            newData.setItemDamaged(true);
        }

        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                sync(player);
            }
        }
    }
}
