package com.spiritform.network;

import com.spiritform.SpiritFormMod;
import com.spiritform.army.ArmySoulStats;
import com.spiritform.client.SpiritArmyClient;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Сервер повідомляє клієнту, які душі лежать у сховищі армії, хто з них викликаний і який наказ діє. */
public record ArmySyncPayload(List<Entry> souls, int order) implements CustomPacketPayload {

    /** hp - поточне (з урахуванням рангу й рівнів), baseHp - початкове; kills і рівні - розвиток душі. */
    public record Entry(UUID id, String type, String name, int hp, boolean summoned,
                        int kills, int healthLevel, int damageLevel, int speedLevel,
                        int baseHp, float attack, float speed, List<ItemStack> armor) {

        public int rank() {
            return ArmySoulStats.rank(kills);
        }
    }

    public static final Type<ArmySyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "army_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ArmySyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.order);
                buf.writeVarInt(payload.souls.size());
                for (Entry entry : payload.souls) {
                    buf.writeUUID(entry.id());
                    buf.writeUtf(entry.type());
                    buf.writeUtf(entry.name());
                    buf.writeVarInt(entry.hp());
                    buf.writeBoolean(entry.summoned());
                    buf.writeVarInt(entry.kills());
                    buf.writeVarInt(entry.healthLevel());
                    buf.writeVarInt(entry.damageLevel());
                    buf.writeVarInt(entry.speedLevel());
                    buf.writeVarInt(entry.baseHp());
                    buf.writeFloat(entry.attack());
                    buf.writeFloat(entry.speed());
                    for (int i = 0; i < 4; i++) {
                        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, i < entry.armor().size() ? entry.armor().get(i) : ItemStack.EMPTY);
                    }
                }
            },
            buf -> {
                int order = buf.readVarInt();
                int size = buf.readVarInt();
                List<Entry> list = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    UUID id = buf.readUUID();
                    String type = buf.readUtf();
                    String name = buf.readUtf();
                    int hp = buf.readVarInt();
                    boolean summoned = buf.readBoolean();
                    int kills = buf.readVarInt();
                    int hpLevel = buf.readVarInt();
                    int dmgLevel = buf.readVarInt();
                    int spdLevel = buf.readVarInt();
                    int baseHp = buf.readVarInt();
                    float attack = buf.readFloat();
                    float speed = buf.readFloat();
                    List<ItemStack> armor = new ArrayList<>();
                    for (int a = 0; a < 4; a++) {
                        armor.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                    }
                    list.add(new Entry(id, type, name, hp, summoned, kills, hpLevel, dmgLevel, spdLevel,
                            baseHp, attack, speed, armor));
                }
                return new ArmySyncPayload(list, order);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(ArmySyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SpiritArmyClient.update(payload));
    }
}
