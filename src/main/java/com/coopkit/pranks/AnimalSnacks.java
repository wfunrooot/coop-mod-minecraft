package com.coopkit.pranks;

import com.coopkit.CoopKit;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.food.FoodProperties;

import java.util.Map;
import static java.util.Map.entry;

public final class AnimalSnacks {
    private static final String PET_NAME = "CoopKitPetName";
    private static final String ANIMAL_DATA = "CoopKitAnimal";
    private AnimalSnacks() {}

    public static void register() {
        Map<EntityType<?>, Item> snacks = Map.ofEntries(
                snack(EntityType.CHICKEN, "held_chicken", 2),
                snack(EntityType.PIG, "held_pig", 3),
                snack(EntityType.COW, "held_cow", 4),
                snack(EntityType.SHEEP, "held_sheep", 4),
                snack(EntityType.MOOSHROOM, "held_mooshroom", 4),
                snack(EntityType.RABBIT, "held_rabbit", 2),
                snack(EntityType.BAT, "held_bat", 1),
                snack(EntityType.CAT, "held_cat", 2),
                snack(EntityType.PARROT, "held_parrot", 2),
                snack(EntityType.HORSE, "held_horse", 4),
                snack(EntityType.DONKEY, "held_donkey", 4),
                snack(EntityType.MULE, "held_mule", 4),
                snack(EntityType.CAMEL, "held_camel", 4),
                snack(EntityType.BEE, "held_bee", 1),
                snack(EntityType.GOAT, "held_goat", 3),
                snack(EntityType.WOLF, "held_wolf", 3),
                snack(EntityType.LLAMA, "held_llama", 4),
                snack(EntityType.TRADER_LLAMA, "held_trader_llama", 4));

        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            Item snack = snacks.get(entity.getType());
            if (snack == null || hand != InteractionHand.MAIN_HAND || player.isSpectator()
                    || !player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()
                    || !canPickUp(entity)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                ItemStack stack = new ItemStack(snack);
                CompoundTag data = entity.saveWithoutId(new CompoundTag());
                stack.getOrCreateTag().put(ANIMAL_DATA, data);
                if (isPet(entity)) {
                    stack.getOrCreateTag().putString(PET_NAME, entity.getName().getString());
                }
                if (entity.hasCustomName()) stack.setHoverName(entity.getName());
                player.setItemInHand(hand, stack);
                entity.discard();
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        });
    }

    private static boolean canPickUp(Entity entity) {
        if (!entity.isAlive() || entity.isVehicle() || entity.isPassenger()) return false;
        if (entity instanceof Mob mob && mob.isLeashed()) return false;
        if (entity instanceof Rabbit rabbit && rabbit.getVariant() == Rabbit.Variant.EVIL) return false;
        if (entity instanceof AbstractHorse horse && horse.isSaddled()) return false;
        if (entity instanceof AbstractChestedHorse horse && horse.hasChest()) return false;
        return !(entity instanceof Horse horse && horse.isWearingArmor());
    }

    private static boolean isPet(Entity entity) {
        return (entity instanceof TamableAnimal animal && animal.isTame())
                || (entity instanceof AbstractHorse horse && horse.isTamed());
    }

    private static Map.Entry<EntityType<?>, Item> snack(EntityType<?> type, String name, int nutrition) {
        FoodProperties food = new FoodProperties.Builder().nutrition(nutrition)
                .saturationMod(0.3F).alwaysEat().build();
        Item item = Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(CoopKit.MODID, name),
                new SnackItem(type, new Item.Properties().stacksTo(1).food(food)));
        return entry(type, item);
    }

    private static final class SnackItem extends Item {
        private final EntityType<?> type;

        private SnackItem(EntityType<?> type, Properties properties) {
            super(properties);
            this.type = type;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (player.isShiftKeyDown()) return InteractionResultHolder.pass(player.getItemInHand(hand));
            return super.use(level, player, hand);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            var player = context.getPlayer();
            if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
            var stack = context.getItemInHand();
            var tag = stack.getTag();
            if (tag == null || !tag.contains(ANIMAL_DATA, Tag.TAG_COMPOUND)) {
                if (!context.getLevel().isClientSide) {
                    player.displayClientMessage(Component.translatable("coopkit.animal.legacy"), true);
                }
                return InteractionResult.FAIL;
            }
            if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;

            var pos = context.getClickedPos().relative(context.getClickedFace());
            if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), stack)
                    || level.isOutsideBuildHeight(pos)) return InteractionResult.FAIL;

            Entity animal = type.create(level);
            if (animal == null) return InteractionResult.FAIL;
            animal.load(tag.getCompound(ANIMAL_DATA).copy());
            animal.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, animal.getYRot(), animal.getXRot());
            animal.setDeltaMovement(0, 0, 0);
            animal.fallDistance = 0;
            for (ServerLevel world : level.getServer().getAllLevels()) {
                if (world.getEntity(animal.getUUID()) != null) return InteractionResult.FAIL;
            }
            if (!level.getWorldBorder().isWithinBounds(animal.getBoundingBox()) || !level.noCollision(animal)
                    || !level.getEntities(animal, animal.getBoundingBox(), Entity::isPickable).isEmpty()
                    || !level.addFreshEntity(animal)) {
                player.displayClientMessage(Component.translatable("coopkit.animal.no_space"), true);
                return InteractionResult.FAIL;
            }
            if (player.getAbilities().instabuild) {
                player.setItemInHand(context.getHand(), ItemStack.EMPTY);
            } else {
                stack.shrink(1);
            }
            return InteractionResult.CONSUME;
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
