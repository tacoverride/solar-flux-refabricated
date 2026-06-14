package net.tax.solarflux;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import net.tax.solarflux.mixin.AbstractFurnaceBlockEntityAccessor;

public class SolarPanelBlockEntity extends BlockEntity implements MenuProvider, Container {
	private static final int FURNACE_ENERGY_PER_TICK = 20;
	private static final int FURNACE_BURN_TIME = 20;

	private static final int BLOCK_CHARGING_RANGE = 16;
	private static final int DISPERSIVE_RANGE = 16;
	private static final int TRAVERSAL_RANGE = 16;

	private final SolarPanelStats stats;
	private final NonNullList<ItemStack> upgradeItems = NonNullList.withSize(5, ItemStack.EMPTY);

	private long energyAmount = 0;

	private int currentGeneration = 0;
	private int efficiency = 0;

	public final EnergyStorage energyStorage = new SolarPanelEnergyStorage();

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case 0 -> (int) SolarPanelBlockEntity.this.getEnergy();
				case 1 -> (int) SolarPanelBlockEntity.this.getCapacity();
				case 2 -> SolarPanelBlockEntity.this.currentGeneration;
				case 3 -> SolarPanelBlockEntity.this.getTransfer();
				case 4 -> SolarPanelBlockEntity.this.efficiency;
				case 5 -> SolarPanelBlockEntity.this.stats.tier();
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {
			switch (index) {
				case 0 -> SolarPanelBlockEntity.this.energyAmount = Math.min(value, SolarPanelBlockEntity.this.getCapacity());
				case 2 -> SolarPanelBlockEntity.this.currentGeneration = value;
				case 4 -> SolarPanelBlockEntity.this.efficiency = value;
			}
		}

		@Override
		public int getCount() {
			return 6;
		}
	};

	public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
		super(SolarFluxFabric.SOLAR_PANEL_BLOCK_ENTITY, pos, state);
		this.stats = SolarPanelStats.fromBlock(state.getBlock());
	}

	public static void tick(Level level, BlockPos pos, BlockState state, SolarPanelBlockEntity blockEntity) {
		long capacity = blockEntity.getCapacity();

		if (blockEntity.energyAmount > capacity) {
			blockEntity.energyAmount = capacity;
		}

		boolean generating = level.canSeeSky(pos.above()) && level.isDay();

		if (generating) {
			blockEntity.currentGeneration = blockEntity.getEffectiveGeneration();
			blockEntity.efficiency = blockEntity.getEffectiveEfficiency();

			blockEntity.energyAmount = Math.min(
					blockEntity.energyAmount + blockEntity.currentGeneration,
					capacity
			);

			blockEntity.setChanged();
		} else {
			blockEntity.currentGeneration = 0;
			blockEntity.efficiency = 0;
		}

		blockEntity.powerAdjacentFurnaces(level, pos);
		blockEntity.powerLinkedBlockChargingTarget(level, pos);
		blockEntity.powerNearbyPlayers(level, pos);
		blockEntity.powerTraversalNetwork(level, pos);
		blockEntity.outputEnergy(level, pos);
	}

	private void powerAdjacentFurnaces(Level level, BlockPos pos) {
		if (countUpgrade(SolarFluxFabric.FURNACE_UPGRADE) <= 0) {
			return;
		}

		if (energyAmount < FURNACE_ENERGY_PER_TICK) {
			return;
		}

		for (Direction direction : Direction.values()) {
			if (energyAmount < FURNACE_ENERGY_PER_TICK) {
				return;
			}

			BlockPos targetPos = pos.relative(direction);
			BlockEntity targetBlockEntity = level.getBlockEntity(targetPos);

			if (!(targetBlockEntity instanceof AbstractFurnaceBlockEntity furnaceBlockEntity)) {
				continue;
			}

			ItemStack inputStack = furnaceBlockEntity.getItem(0);

			if (inputStack.isEmpty()) {
				continue;
			}

			AbstractFurnaceBlockEntityAccessor accessor = (AbstractFurnaceBlockEntityAccessor) furnaceBlockEntity;
			accessor.solarflux$setLitTime(FURNACE_BURN_TIME);
			accessor.solarflux$setLitDuration(FURNACE_BURN_TIME);

			energyAmount -= FURNACE_ENERGY_PER_TICK;

			furnaceBlockEntity.setChanged();
			setChanged();
		}
	}

	private void powerLinkedBlockChargingTarget(Level level, BlockPos pos) {
		if (energyAmount <= 0) {
			return;
		}

		ItemStack blockChargingUpgrade = getBlockChargingUpgrade();

		if (blockChargingUpgrade.isEmpty()) {
			return;
		}

		if (!BlockChargingUpgradeItem.isLinked(blockChargingUpgrade)) {
			return;
		}

		String linkedDimension = BlockChargingUpgradeItem.getLinkedDimension(blockChargingUpgrade);
		String currentDimension = level.dimension().location().toString();

		if (!currentDimension.equals(linkedDimension)) {
			return;
		}

		BlockPos linkedPos = BlockChargingUpgradeItem.getLinkedPos(blockChargingUpgrade);

		if (!isWithinBlockChargingRange(pos, linkedPos)) {
			return;
		}

		if (level.getBlockEntity(linkedPos) instanceof SolarPanelBlockEntity) {
			return;
		}

		long remainingTransfer = Math.min(getTransfer(), energyAmount);

		for (Direction direction : Direction.values()) {
			if (remainingTransfer <= 0 || energyAmount <= 0) {
				return;
			}

			EnergyStorage targetStorage = EnergyStorage.SIDED.find(level, linkedPos, direction);

			if (targetStorage == null) {
				continue;
			}

			try (Transaction transaction = Transaction.openOuter()) {
				long inserted = targetStorage.insert(remainingTransfer, transaction);
				long extracted = energyStorage.extract(inserted, transaction);

				if (extracted > 0) {
					transaction.commit();
					remainingTransfer -= extracted;
					setChanged();
					return;
				}
			}
		}
	}

	private void powerNearbyPlayers(Level level, BlockPos pos) {
		if (countUpgrade(SolarFluxFabric.DISPERSIVE_UPGRADE) <= 0) {
			return;
		}

		if (energyAmount <= 0) {
			return;
		}

		AABB rangeBox = new AABB(pos).inflate(DISPERSIVE_RANGE);

		for (Player player : level.getEntitiesOfClass(Player.class, rangeBox)) {
			if (energyAmount <= 0) {
				return;
			}

			chargePlayerInventory(player);
		}
	}

	private void chargePlayerInventory(Player player) {
		Inventory inventory = player.getInventory();

		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (energyAmount <= 0) {
				return;
			}

			ItemStack stack = inventory.getItem(slot);

			if (stack.isEmpty()) {
				continue;
			}

			chargePlayerInventorySlot(player, slot);
		}
	}

	private void chargePlayerInventorySlot(Player player, int slot) {
		Inventory inventory = player.getInventory();
		ItemStack stack = inventory.getItem(slot);

		if (stack.isEmpty()) {
			return;
		}

		PlayerInventoryStorage playerInventoryStorage = PlayerInventoryStorage.of(player);

		EnergyStorage itemStorage = EnergyStorage.ITEM.find(
				stack,
				ContainerItemContext.ofPlayerSlot(player, playerInventoryStorage.getSlot(slot))
		);

		if (itemStorage == null) {
			return;
		}

		long remainingTransfer = Math.min(getTransfer(), energyAmount);

		if (remainingTransfer <= 0) {
			return;
		}

		try (Transaction transaction = Transaction.openOuter()) {
			long inserted = itemStorage.insert(remainingTransfer, transaction);

			if (inserted <= 0) {
				return;
			}

			long extracted = energyStorage.extract(inserted, transaction);

			if (extracted > 0) {
				transaction.commit();
				inventory.setChanged();
				setChanged();
			}
		}
	}

	private void powerTraversalNetwork(Level level, BlockPos pos) {
		if (countUpgrade(SolarFluxFabric.TRAVERSAL_UPGRADE) <= 0) {
			return;
		}

		if (energyAmount <= 0) {
			return;
		}

		long remainingTransfer = Math.min(getTransfer(), energyAmount);

		Queue<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> visited = new HashSet<>();

		visited.add(pos);

		for (Direction direction : Direction.values()) {
			BlockPos startPos = pos.relative(direction);

			if (isValidTraversalNode(level, startPos, pos)) {
				queue.add(startPos);
				visited.add(startPos);
			}
		}

		while (!queue.isEmpty()) {
			if (remainingTransfer <= 0 || energyAmount <= 0) {
				return;
			}

			BlockPos currentPos = queue.remove();

			remainingTransfer -= trySendEnergyToTraversalTarget(level, currentPos, remainingTransfer);

			for (Direction direction : Direction.values()) {
				BlockPos nextPos = currentPos.relative(direction);

				if (visited.contains(nextPos)) {
					continue;
				}

				if (pos.distSqr(nextPos) > TRAVERSAL_RANGE * TRAVERSAL_RANGE) {
					continue;
				}

				if (!isValidTraversalNode(level, nextPos, pos)) {
					continue;
				}

				visited.add(nextPos);
				queue.add(nextPos);
			}
		}
	}

	private boolean isValidTraversalNode(Level level, BlockPos targetPos, BlockPos panelPos) {
		if (targetPos.equals(panelPos)) {
			return false;
		}

		BlockEntity blockEntity = level.getBlockEntity(targetPos);

		if (blockEntity == null) {
			return false;
		}

		if (blockEntity instanceof SolarPanelBlockEntity) {
			return false;
		}

		for (Direction direction : Direction.values()) {
			if (EnergyStorage.SIDED.find(level, targetPos, direction) != null) {
				return true;
			}
		}

		return false;
	}

	private long trySendEnergyToTraversalTarget(Level level, BlockPos targetPos, long maxTransfer) {
		if (maxTransfer <= 0 || energyAmount <= 0) {
			return 0;
		}

		long sent = 0;

		for (Direction direction : Direction.values()) {
			if (maxTransfer <= 0 || energyAmount <= 0) {
				return sent;
			}

			EnergyStorage targetStorage = EnergyStorage.SIDED.find(level, targetPos, direction);

			if (targetStorage == null) {
				continue;
			}

			try (Transaction transaction = Transaction.openOuter()) {
				long inserted = targetStorage.insert(Math.min(maxTransfer, energyAmount), transaction);

				if (inserted <= 0) {
					continue;
				}

				long extracted = energyStorage.extract(inserted, transaction);

				if (extracted > 0) {
					transaction.commit();
					maxTransfer -= extracted;
					sent += extracted;
					setChanged();
				}
			}
		}

		return sent;
	}

	private boolean isWithinBlockChargingRange(BlockPos panelPos, BlockPos linkedPos) {
		return panelPos.distSqr(linkedPos) <= BLOCK_CHARGING_RANGE * BLOCK_CHARGING_RANGE;
	}

	private ItemStack getBlockChargingUpgrade() {
		for (ItemStack stack : upgradeItems) {
			if (stack.is(SolarFluxFabric.BLOCK_CHARGING_UPGRADE)) {
				return stack;
			}
		}

		return ItemStack.EMPTY;
	}

	private void outputEnergy(Level level, BlockPos pos) {
		if (energyAmount <= 0) {
			return;
		}

		long remainingTransfer = Math.min(getTransfer(), energyAmount);

		for (Direction direction : Direction.values()) {
			if (remainingTransfer <= 0) {
				return;
			}

			BlockPos targetPos = pos.relative(direction);

			if (level.getBlockEntity(targetPos) instanceof SolarPanelBlockEntity) {
				continue;
			}

			EnergyStorage targetStorage = EnergyStorage.SIDED.find(level, targetPos, direction.getOpposite());

			if (targetStorage == null) {
				continue;
			}

			try (Transaction transaction = Transaction.openOuter()) {
				long inserted = targetStorage.insert(remainingTransfer, transaction);
				long extracted = energyStorage.extract(inserted, transaction);

				if (extracted > 0) {
					transaction.commit();
					remainingTransfer -= extracted;
				}
			}
		}
	}

	public EnergyStorage getEnergyStorage() {
		return energyStorage;
	}

	@Override
	public Component getDisplayName() {
		return Component.literal("Solar Panel " + toRoman(stats.tier()));
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
		return new SolarPanelMenu(syncId, playerInventory, this, this.data);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putLong("Energy", energyAmount);
		ContainerHelper.saveAllItems(tag, upgradeItems);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		energyAmount = tag.getLong("Energy");
		ContainerHelper.loadAllItems(tag, upgradeItems);

		if (energyAmount > getCapacity()) {
			energyAmount = getCapacity();
		}
	}

	@Override
	public int getContainerSize() {
		return upgradeItems.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : upgradeItems) {
			if (!stack.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return upgradeItems.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack result = ContainerHelper.removeItem(upgradeItems, slot, amount);

		if (!result.isEmpty()) {
			clampEnergyToCapacity();
			setChanged();
		}

		return result;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack result = ContainerHelper.takeItem(upgradeItems, slot);
		clampEnergyToCapacity();
		return result;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		upgradeItems.set(slot, stack);
		clampEnergyToCapacity();
		setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
		upgradeItems.clear();
		clampEnergyToCapacity();
		setChanged();
	}

	public long getEnergy() {
		return energyAmount;
	}

	public void restoreEnergyFromItem(long energy) {
		this.energyAmount = Math.min(Math.max(energy, 0), getCapacity());
		setChanged();
	}

	public void saveUpgradesToItemTag(CompoundTag itemTag) {
		CompoundTag upgradesTag = new CompoundTag();
		ContainerHelper.saveAllItems(upgradesTag, upgradeItems);
		itemTag.put("Upgrades", upgradesTag);
	}

	public void restoreUpgradesFromItemTag(CompoundTag itemTag) {
		if (!itemTag.contains("Upgrades")) {
			return;
		}

		CompoundTag upgradesTag = itemTag.getCompound("Upgrades");
		ContainerHelper.loadAllItems(upgradesTag, upgradeItems);
		clampEnergyToCapacity();
		setChanged();
	}

	public boolean hasUpgrades() {
		for (ItemStack stack : upgradeItems) {
			if (!stack.isEmpty()) {
				return true;
			}
		}

		return false;
	}

	public long getCapacity() {
		int capacityUpgrades = countUpgrade(SolarFluxFabric.CAPACITY_UPGRADE);
		return stats.capacity() * (1L + capacityUpgrades);
	}

	public int getTransfer() {
		int transferUpgrades = countUpgrade(SolarFluxFabric.TRANSFER_RATE_UPGRADE);
		long transfer = stats.transfer() * (1L + transferUpgrades);

		if (transfer > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}

		return (int) transfer;
	}

	public int getCurrentGeneration() {
		return currentGeneration;
	}

	public int getEfficiency() {
		return efficiency;
	}

	private int getEffectiveGeneration() {
		int efficiencyUpgrades = countUpgrade(SolarFluxFabric.EFFICIENCY_UPGRADE);

		long generation = stats.generation() * (100L + efficiencyUpgrades * 25L) / 100L;

		if (generation > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}

		return (int) generation;
	}

	private int getEffectiveEfficiency() {
		int efficiencyUpgrades = countUpgrade(SolarFluxFabric.EFFICIENCY_UPGRADE);
		return 100 + efficiencyUpgrades * 25;
	}

	private int countUpgrade(net.minecraft.world.item.Item item) {
		int count = 0;

		for (ItemStack stack : upgradeItems) {
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}

		return count;
	}

	private void clampEnergyToCapacity() {
		long capacity = getCapacity();

		if (energyAmount > capacity) {
			energyAmount = capacity;
		}
	}

	private static String toRoman(int number) {
		return switch (number) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			case 5 -> "V";
			case 6 -> "VI";
			case 7 -> "VII";
			case 8 -> "VIII";
			default -> String.valueOf(number);
		};
	}

	private class SolarPanelEnergyStorage extends SnapshotParticipant<Long> implements EnergyStorage {
		@Override
		public long insert(long maxAmount, TransactionContext transaction) {
			long inserted = Math.min(maxAmount, getCapacity() - energyAmount);

			if (inserted <= 0) {
				return 0;
			}

			updateSnapshots(transaction);
			energyAmount += inserted;
			return inserted;
		}

		@Override
		public long extract(long maxAmount, TransactionContext transaction) {
			long extracted = Math.min(maxAmount, Math.min(energyAmount, getTransfer()));

			if (extracted <= 0) {
				return 0;
			}

			updateSnapshots(transaction);
			energyAmount -= extracted;
			return extracted;
		}

		@Override
		public long getAmount() {
			return energyAmount;
		}

		@Override
		public long getCapacity() {
			return SolarPanelBlockEntity.this.getCapacity();
		}

		@Override
		protected Long createSnapshot() {
			return energyAmount;
		}

		@Override
		protected void readSnapshot(Long snapshot) {
			energyAmount = snapshot;
		}

		@Override
		protected void onFinalCommit() {
			setChanged();
		}
	}
}