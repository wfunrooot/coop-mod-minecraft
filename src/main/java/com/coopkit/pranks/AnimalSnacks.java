package com.coopkit.pranks;

import com.coopkit.CoopKit;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;

import java.util.Map;
import static java.util.Map.entry;

public final class AnimalSnacks {
    private static final String PET_NAME = "CoopKitPetName";
    private AnimalSnacks() {}

    public static void register() {
        Map<EntityType<?>, Item> snacks = Map.ofEntries(
                entry(EntityType.CHICKEN, snack("held_chicken", 2)),
                entry(EntityType.PIG, snack("held_pig", 3)),
                entry(EntityType.COW, snack("held_cow", 4)),
                entry(EntityType.SHEEP, snack("held_sheep", 4)),
                entry(EntityType.MOOSHROOM, snack("held_mooshroom", 4)),
                entry(EntityType.RABBIT, snack("held_rabbit", 2)),
                entry(EntityType.BAT, snack("held_bat", 1)),
                entry(EntityType.CAT, snack("held_cat", 2)),
                entry(EntityType.PARROT, snack("held_parrot", 2)),
                entry(EntityType.HORSE, snack("held_horse", 4)),
                entry(EntityType.DONKEY, snack("held_donkey", 4)),
                entry(EntityType.MULE, snack("held_mule", 4)),
                entry(EntityType.CAMEL, snack("held_camel", 4)),
                entry(EntityType.BEE, snack("held_bee", 1)),
                entry(EntityType.GOAT, snack("held_goat", 3)),
                entry(EntityType.WOLF, snack("held_wolf", 3)),
                entry(EntityType.LLAMA, snack("held_llama", 4)),
                entry(EntityType.TRADER_LLAMA, snack("held_trader_llama", 4)));

        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            Item snack = snacks.get(entity.getType());
            if (snack == null || hand != InteractionHand.MAIN_HAND || player.isSpectator()
                    || !player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()
                    || !canPickUp(entity)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                ItemStack stack = new ItemStack(snack);
                if (isPet(entity)) {
                    stack.getOrCreateTag().putString(PET_NAME, entity.getName().getString());
                    if (entity.hasCustomName()) stack.setHoverName(entity.getName());
                }
                player.setItemInHand(hand, stack);
                entity.discard();
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        });
    }

    private static boolean canPickUp(Entity entity) {
        if (!entity.isAlive() || entity.isVehicle() || entity.isPassenger()) return false;
        if (entity instanceof Rabbit rabbit && rabbit.getVariant() == Rabbit.Variant.EVIL) return false;
        if (entity instanceof AbstractHorse horse && horse.isSaddled()) return false;
        if (entity instanceof AbstractChestedHorse horse && horse.hasChest()) return false;
        return !(entity instanceof Horse horse && horse.isWearingArmor());
    }

    private static boolean isPet(Entity entity) {
        return (entity instanceof TamableAnimal animal && animal.isTame())
                || (entity instanceof AbstractHorse horse && horse.isTamed());
    }

    private static Item snack(String name, int nutrition) {
        FoodProperties food = new FoodProperties.Builder().nutrition(nutrition)
                .saturationMod(0.3F).alwaysEat().build();
        return Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(CoopKit.MODID, name),
                new SnackItem(new Item.Properties().stacksTo(1).food(food)));
    }

    private static final class SnackItem extends Item {
        private SnackItem(Properties properties) {
            super(properties);
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
            var tag = stack.getTag();
            String pet = tag == null ? "" : tag.getString(PET_NAME);
            ItemStack remaining = super.finishUsingItem(stack, level, eater);
            if (!pet.isEmpty() && eater instanceof ServerPlayer player && player.getServer() != null) {
                int variant = 1 + player.getRandom().nextInt(3);
                player.getServer().getPlayerList().broadcastSystemMessage(Component.translatable(
                        "coopkit.msg.pet_eaten." + variant, pet, player.getDisplayName()), false);
            }
            return remaining;
        }
    }
}
