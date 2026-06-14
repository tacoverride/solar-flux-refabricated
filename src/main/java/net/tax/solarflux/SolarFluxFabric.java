package net.tax.solarflux;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import team.reborn.energy.api.EnergyStorage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolarFluxFabric implements ModInitializer {
	public static final String MOD_ID = "solarflux";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final MenuType<SolarPanelMenu> SOLAR_PANEL_MENU = Registry.register(
		BuiltInRegistries.MENU,
		new ResourceLocation(MOD_ID, "solar_panel"),
		new MenuType<>(SolarPanelMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final Block SOLAR_PANEL_1 = registerSolarPanel("solar_panel_1");
	public static final Block SOLAR_PANEL_2 = registerSolarPanel("solar_panel_2");
	public static final Block SOLAR_PANEL_3 = registerSolarPanel("solar_panel_3");
	public static final Block SOLAR_PANEL_4 = registerSolarPanel("solar_panel_4");
	public static final Block SOLAR_PANEL_5 = registerSolarPanel("solar_panel_5");
	public static final Block SOLAR_PANEL_6 = registerSolarPanel("solar_panel_6");
	public static final Block SOLAR_PANEL_7 = registerSolarPanel("solar_panel_7");
	public static final Block SOLAR_PANEL_8 = registerSolarPanel("solar_panel_8");

	public static final Item MIRROR = registerItem("mirror");
	public static final Item PHOTOVOLTAIC_CELL_1 = registerItem("photovoltaic_cell_1");
	public static final Item PHOTOVOLTAIC_CELL_2 = registerItem("photovoltaic_cell_2");
	public static final Item PHOTOVOLTAIC_CELL_3 = registerItem("photovoltaic_cell_3");
	public static final Item PHOTOVOLTAIC_CELL_4 = registerItem("photovoltaic_cell_4");
	public static final Item PHOTOVOLTAIC_CELL_5 = registerItem("photovoltaic_cell_5");
	public static final Item PHOTOVOLTAIC_CELL_6 = registerItem("photovoltaic_cell_6");

	public static final Item BLANK_UPGRADE = registerUpgradeItem(
			"blank_upgrade",
			"Base component for upgrade crafting."
	);

	public static final Item CAPACITY_UPGRADE = registerUpgradeItem(
			"capacity_upgrade",
			"Doubles internal energy capacity."
	);

	public static final Item TRANSFER_RATE_UPGRADE = registerUpgradeItem(
			"transfer_rate_upgrade",
			"Doubles output transfer rate."
	);

	public static final Item EFFICIENCY_UPGRADE = registerUpgradeItem(
			"efficiency_upgrade",
			"Increases generation by 25%."
	);

	public static final Item BLOCK_CHARGING_UPGRADE = registerBlockChargingUpgradeItem("block_charging_upgrade");

	public static final Item DISPERSIVE_UPGRADE = registerUpgradeItem(
			"dispersive_upgrade",
			"Disperses energy to nearby players.",
			"Range: 16 blocks."
	);

	public static final Item FURNACE_UPGRADE = registerUpgradeItem(
			"furnace_upgrade",
			"Uses solar energy for furnace behavior."
	);

	public static final Item TRAVERSAL_UPGRADE = registerUpgradeItem(
			"machine_traversal_upgrade",
			"Moves energy through connected machines."
	);

	public static final Item BLAZING_COATING = registerItem("blazing_coating");
	public static final Item ENDER_GLASS = registerItem("ender_glass");
	public static final Item EMERALD_GLASS = registerItem("emerald_glass");

	public static final Item SOLAR_PANEL_1_ITEM = registerSolarPanelItem("solar_panel_1", SOLAR_PANEL_1, 1, 8, 25000);
	public static final Item SOLAR_PANEL_2_ITEM = registerSolarPanelItem("solar_panel_2", SOLAR_PANEL_2, 8, 64, 125000);
	public static final Item SOLAR_PANEL_3_ITEM = registerSolarPanelItem("solar_panel_3", SOLAR_PANEL_3, 32, 256, 425000);
	public static final Item SOLAR_PANEL_4_ITEM = registerSolarPanelItem("solar_panel_4", SOLAR_PANEL_4, 128, 1024, 2000000);
	public static final Item SOLAR_PANEL_5_ITEM = registerSolarPanelItem("solar_panel_5", SOLAR_PANEL_5, 512, 4096, 8000000);
	public static final Item SOLAR_PANEL_6_ITEM = registerSolarPanelItem("solar_panel_6", SOLAR_PANEL_6, 2048, 16384, 32000000);
	public static final Item SOLAR_PANEL_7_ITEM = registerSolarPanelItem("solar_panel_7", SOLAR_PANEL_7, 8192, 65536, 64000000);
	public static final Item SOLAR_PANEL_8_ITEM = registerSolarPanelItem("solar_panel_8", SOLAR_PANEL_8, 32768, 262144, 128000000);

	public static final BlockEntityType<SolarPanelBlockEntity> SOLAR_PANEL_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			new ResourceLocation(MOD_ID, "solar_panel"),
			FabricBlockEntityTypeBuilder.create(
					SolarPanelBlockEntity::new,
					SOLAR_PANEL_1,
					SOLAR_PANEL_2,
					SOLAR_PANEL_3,
					SOLAR_PANEL_4,
					SOLAR_PANEL_5,
					SOLAR_PANEL_6,
					SOLAR_PANEL_7,
					SOLAR_PANEL_8
			).build(null)
	);

	private static Block registerSolarPanel(String name) {
		return Registry.register(
				BuiltInRegistries.BLOCK,
				new ResourceLocation(MOD_ID, name),
				new SolarPanelBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3.0f).noOcclusion())
		);
	}

	private static Item registerSolarPanelItem(String name, Block block, int generation, int transfer, int capacity) {
		return Registry.register(
				BuiltInRegistries.ITEM,
				new ResourceLocation(MOD_ID, name),
				new SolarPanelBlockItem(block, new Item.Properties(), generation, transfer, capacity)
		);
	}

	private static Item registerItem(String name) {
		return Registry.register(
				BuiltInRegistries.ITEM,
				new ResourceLocation(MOD_ID, name),
				new Item(new Item.Properties())
		);
	}

	private static Item registerUpgradeItem(String name, String... tooltipLines) {
		return Registry.register(
				BuiltInRegistries.ITEM,
				new ResourceLocation(MOD_ID, name),
				new SolarFluxUpgradeItem(new Item.Properties(), tooltipLines)
		);
	}

	private static Item registerBlockChargingUpgradeItem(String name) {
		return Registry.register(
				BuiltInRegistries.ITEM,
				new ResourceLocation(MOD_ID, name),
				new BlockChargingUpgradeItem(new Item.Properties())
		);
	}

	@Override
	public void onInitialize() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(entries -> {
			entries.accept(SOLAR_PANEL_1_ITEM);
			entries.accept(SOLAR_PANEL_2_ITEM);
			entries.accept(SOLAR_PANEL_3_ITEM);
			entries.accept(SOLAR_PANEL_4_ITEM);
			entries.accept(SOLAR_PANEL_5_ITEM);
			entries.accept(SOLAR_PANEL_6_ITEM);
			entries.accept(SOLAR_PANEL_7_ITEM);
			entries.accept(SOLAR_PANEL_8_ITEM);

			entries.accept(MIRROR);
			entries.accept(PHOTOVOLTAIC_CELL_1);
			entries.accept(PHOTOVOLTAIC_CELL_2);
			entries.accept(PHOTOVOLTAIC_CELL_3);
			entries.accept(PHOTOVOLTAIC_CELL_4);
			entries.accept(PHOTOVOLTAIC_CELL_5);
			entries.accept(PHOTOVOLTAIC_CELL_6);

			entries.accept(BLANK_UPGRADE);
			entries.accept(CAPACITY_UPGRADE);
			entries.accept(TRANSFER_RATE_UPGRADE);
			entries.accept(EFFICIENCY_UPGRADE);
			entries.accept(BLOCK_CHARGING_UPGRADE);
			entries.accept(DISPERSIVE_UPGRADE);
			entries.accept(FURNACE_UPGRADE);
			entries.accept(TRAVERSAL_UPGRADE);

			entries.accept(BLAZING_COATING);
			entries.accept(ENDER_GLASS);
			entries.accept(EMERALD_GLASS);
		});

		EnergyStorage.SIDED.registerForBlockEntity(
				(blockEntity, direction) -> blockEntity.getEnergyStorage(),
				SOLAR_PANEL_BLOCK_ENTITY
		);

		LOGGER.info("Solar Flux Fabric initialized.");
	}
}