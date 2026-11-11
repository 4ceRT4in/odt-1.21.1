package xyz.frxzenluke.titanreborn.command;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import xyz.frxzenluke.titanreborn.OneDayTitan;

import java.lang.reflect.Method;

/** A six-row chest view of a player's inventory and Titan Reborn Save Inventory. */
final class InvseeScreenHandler extends ScreenHandler {
    private static final int TOP_SLOTS = 54;
    private static final EquipmentSlot[] ARMOR_ORDER = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final ServerPlayerEntity target;
    private final SaveInventory saveInventory;

    InvseeScreenHandler(int syncId, PlayerInventory viewerInventory, ServerPlayerEntity target) {
        super(ScreenHandlerType.GENERIC_9X6, syncId);
        this.target = target;
        this.saveInventory = findSaveInventory(target);

        PlayerInventory targetInventory = target.getInventory();
        // Rows 1-3: main inventory; row 4: hotbar.
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(targetInventory, 9 + row * 9 + column, x(column), y(row)));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(targetInventory, column, x(column), y(3)));
        }

        // Row 5: helmet, chestplate, leggings, boots and offhand.
        for (int column = 0; column < 4; column++) {
            EquipmentSlot equipmentSlot = ARMOR_ORDER[column];
            addSlot(new Slot(targetInventory, 39 - column, x(column), y(4)) {
                @Override
                public boolean canInsert(ItemStack stack) {
                    return target.getPreferredEquipmentSlot(stack) == equipmentSlot;
                }

                @Override
                public int getMaxItemCount() {
                    return 1;
                }
            });
        }
        addSlot(new Slot(targetInventory, 40, x(4), y(4)));

        SimpleInventory fillers = blockedInventory("Unused slot");
        for (int column = 5; column < 9; column++) {
            addSlot(new BlockedSlot(fillers, x(column), y(4)));
        }

        // Row 6: Titan Reborn's eight team or personal Save Inventory slots.
        if (saveInventory == null) {
            SimpleInventory unavailable = blockedInventory("Save Inventory unavailable");
            for (int column = 0; column < 8; column++) {
                addSlot(new BlockedSlot(unavailable, x(column), y(5)));
            }
        } else {
            for (int column = 0; column < 8; column++) {
                addSlot(new SaveInventorySlot(saveInventory.inventory(), column, x(column), y(5)));
            }
        }
        addSlot(new BlockedSlot(fillers, x(8), y(5)));

        // The viewer's inventory occupies the usual slots below the chest.
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(viewerInventory, 9 + row * 9 + column, x(column), 140 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(viewerInventory, column, x(column), 198));
        }
    }

    private static int x(int column) {
        return 8 + column * 18;
    }

    private static int y(int row) {
        return 18 + row * 18;
    }

    private static SimpleInventory blockedInventory(String label) {
        ItemStack barrier = new ItemStack(Items.BARRIER);
        barrier.set(DataComponentTypes.CUSTOM_NAME, Text.literal(label));
        return new SimpleInventory(barrier);
    }

    @Override
    public boolean canUse(PlayerEntity viewer) {
        return target.getServer() != null
                && target.getServer().getPlayerManager().getPlayer(target.getUuid()) == target;
    }

    @Override
    public ItemStack quickMove(PlayerEntity viewer, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(slotIndex);
        if (!slot.hasStack() || !slot.canTakeItems(viewer)) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        boolean moved = slotIndex < TOP_SLOTS
                ? insertItem(stack, TOP_SLOTS, slots.size(), true)
                : insertItem(stack, 0, TOP_SLOTS, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }
        slot.onTakeItem(viewer, stack);
        return original;
    }

    @Override
    public void onClosed(PlayerEntity viewer) {
        super.onClosed(viewer);
        if (saveInventory != null) {
            saveInventory.sync();
        }
    }

    private final class SaveInventorySlot extends Slot {
        private SaveInventorySlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public void markDirty() {
            super.markDirty();
            saveInventory.sync();
        }
    }

    private static final class BlockedSlot extends Slot {
        private BlockedSlot(Inventory inventory, int x, int y) {
            super(inventory, 0, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeItems(PlayerEntity player) {
            return false;
        }

        @Override
        public ItemStack takeStack(int amount) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setStack(ItemStack stack) {
            // Decorative barrier, never a real inventory item.
        }
    }

    private static SaveInventory findSaveInventory(ServerPlayerEntity target) {
        if (!FabricLoader.getInstance().isModLoaded("titanfabric")) {
            return null;
        }
        try {
            Class<?> componentType = Class.forName("net.shirojr.titanfabric.cca.component.ExtendedInventoryComponent");
            Object component = componentType.getMethod("getTeamOrEntity", LivingEntity.class).invoke(null, target);
            Inventory inventory = (Inventory) componentType.getMethod("getInventory").invoke(component);
            if (inventory.size() < 8) {
                OneDayTitan.LOGGER.warn("Titan Reborn Save Inventory has fewer than eight slots");
                return null;
            }
            Method sync = componentType.getMethod("sync");
            return new SaveInventory(inventory, () -> {
                try {
                    sync.invoke(component);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    OneDayTitan.LOGGER.warn("Could not sync Titan Reborn Save Inventory", e);
                }
            });
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            OneDayTitan.LOGGER.warn("Could not open Titan Reborn Save Inventory", e);
            return null;
        }
    }

    private record SaveInventory(Inventory inventory, Runnable syncAction) {
        private void sync() {
            syncAction.run();
        }
    }
}
