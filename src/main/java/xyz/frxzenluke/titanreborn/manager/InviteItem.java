package xyz.frxzenluke.titanreborn.manager;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Provides the invite sword even when TitanFabric is not installed. */
public final class InviteItem {
    private static final Identifier LEGEND_SWORD = Identifier.of("titanfabric", "legend_sword");
    private static final String MARKER = "onedaytitan_invite";

    private InviteItem() {
    }

    public static ItemStack create() {
        Item item = Registries.ITEM.getOrEmpty(LEGEND_SWORD).orElse(Items.IRON_SWORD);
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§8» §5Invite"));
        NbtCompound data = new NbtCompound();
        data.putBoolean(MARKER, true);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
        return stack;
    }

    public static boolean isInviteSword(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data != null && data.copyNbt().getBoolean(MARKER)) {
            return true;
        }
        return Registries.ITEM.getOrEmpty(LEGEND_SWORD)
                .map(item -> stack.isOf(item))
                .orElse(false);
    }
}
