package net.tax.solarflux;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SolarPanelMenu extends AbstractContainerMenu {
	private static final int UPGRADE_SLOT_START = 0;
	private static final int UPGRADE_SLOT_END = 5;

	private static final int PLAYER_INVENTORY_START = 5;
	private static final int PLAYER_INVENTORY_END = 32;

	private static final int HOTBAR_START = 32;
	private static final int HOTBAR_END = 41;

	private final ContainerData data;

	public SolarPanelMenu(int syncId, Inventory playerInventory) {
		this(syncId, playerInventory, new SimpleContainer(5), new SimpleContainerData(6));
	}

	public SolarPanelMenu(int syncId, Inventory playerInventory, Container upgradeInventory, ContainerData data) {
		super(SolarFluxFabric.SOLAR_PANEL_MENU, syncId);
		this.data = data;

		addDataSlots(data);

		for (int i = 0; i < 5; i++) {
			this.addSlot(new UpgradeSlot(upgradeInventory, i, 9 + i * 18, 61));
		}

		addPlayerInventory(playerInventory);
		addPlayerHotbar(playerInventory);
	}

	private void addPlayerInventory(Inventory playerInventory) {
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 98 + row * 18));
			}
		}
	}

	private void addPlayerHotbar(Inventory playerInventory) {
		for (int column = 0; column < 9; column++) {
			this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 156));
		}
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack originalStack = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);

		if (slot != null && slot.hasItem()) {
			ItemStack stackInSlot = slot.getItem();
			originalStack = stackInSlot.copy();

			if (index >= UPGRADE_SLOT_START && index < UPGRADE_SLOT_END) {
				if (!this.moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
					return ItemStack.EMPTY;
				}
			}

			else if (index >= PLAYER_INVENTORY_START && index < HOTBAR_END) {
				if (isUpgradeItem(stackInSlot) && !alreadyHasUpgradeType(stackInSlot, -1)) {
					if (!this.moveItemStackTo(stackInSlot, UPGRADE_SLOT_START, UPGRADE_SLOT_END, false)) {
						return ItemStack.EMPTY;
					}
				}

				else if (index >= PLAYER_INVENTORY_START && index < PLAYER_INVENTORY_END) {
					if (!this.moveItemStackTo(stackInSlot, HOTBAR_START, HOTBAR_END, false)) {
						return ItemStack.EMPTY;
					}
				} else if (index >= HOTBAR_START && index < HOTBAR_END) {
					if (!this.moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
						return ItemStack.EMPTY;
					}
				}
			}

			if (stackInSlot.isEmpty()) {
				slot.set(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}

			if (stackInSlot.getCount() == originalStack.getCount()) {
				return ItemStack.EMPTY;
			}

			slot.onTake(player, stackInSlot);
		}

		return originalStack;
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	public int getEnergy() {
		return data.get(0);
	}

	public int getCapacity() {
		return data.get(1);
	}

	public int getCurrentGeneration() {
		return data.get(2);
	}

	public int getTransfer() {
		return data.get(3);
	}

	public int getEfficiency() {
		return data.get(4);
	}

	public int getTier() {
		return data.get(5);
	}

	public int getEnergyBarHeight() {
		int capacity = getCapacity();

		if (capacity <= 0) {
			return 0;
		}

		return (int) ((long) getEnergy() * 48L / capacity);
	}

	public int getGenerationBarHeight() {
		if (getEfficiency() <= 0) {
			return 0;
		}

		return (int) ((long) getEfficiency() * 48L / 100L);
	}

	private static boolean isUpgradeItem(ItemStack stack) {
		return stack.is(SolarFluxFabric.CAPACITY_UPGRADE)
				|| stack.is(SolarFluxFabric.TRANSFER_RATE_UPGRADE)
				|| stack.is(SolarFluxFabric.EFFICIENCY_UPGRADE)
				|| stack.is(SolarFluxFabric.BLOCK_CHARGING_UPGRADE)
				|| stack.is(SolarFluxFabric.DISPERSIVE_UPGRADE)
				|| stack.is(SolarFluxFabric.FURNACE_UPGRADE)
				|| stack.is(SolarFluxFabric.TRAVERSAL_UPGRADE);
	}

	private boolean alreadyHasUpgradeType(ItemStack stack, int ignoredUpgradeSlotIndex) {
		for (int i = UPGRADE_SLOT_START; i < UPGRADE_SLOT_END; i++) {
			if (i == ignoredUpgradeSlotIndex) {
				continue;
			}

			ItemStack existingStack = this.slots.get(i).getItem();

			if (!existingStack.isEmpty() && existingStack.is(stack.getItem())) {
				return true;
			}
		}

		return false;
	}

	private class UpgradeSlot extends Slot {
		private final int upgradeSlotIndex;

		public UpgradeSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
			this.upgradeSlotIndex = slot;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return isUpgradeItem(stack) && !alreadyHasUpgradeType(stack, upgradeSlotIndex);
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}
	}
}