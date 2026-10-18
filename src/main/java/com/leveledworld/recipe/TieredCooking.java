package com.leveledworld.recipe;

import com.leveledworld.LeveledWorld;
import com.leveledworld.item.TierItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCookingSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Receitas de cozimento (fornalha, defumador e fogueira) que passam o tier do ingrediente para o resultado:
 * carne crua Rara vira carne assada Rara. Substituem as receitas vanilla das carnes (data/minecraft/recipe).
 */
public final class TieredCooking {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, LeveledWorld.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<Smelting>> SMELTING =
            SERIALIZERS.register("tiered_smelting", () -> new SimpleCookingSerializer<>(Smelting::new, 200));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<Smoking>> SMOKING =
            SERIALIZERS.register("tiered_smoking", () -> new SimpleCookingSerializer<>(Smoking::new, 100));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<Campfire>> CAMPFIRE =
            SERIALIZERS.register("tiered_campfire_cooking", () -> new SimpleCookingSerializer<>(Campfire::new, 100));

    public static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }

    private static ItemStack withTier(ItemStack result, SingleRecipeInput input) {
        TierItems.copyTier(input.item(), result);
        return result;
    }

    public static final class Smelting extends SmeltingRecipe {
        public Smelting(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float xp, int time) {
            super(group, category, ingredient, result, xp, time);
        }

        @Override
        public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
            return withTier(super.assemble(input, registries), input);
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return SMELTING.get();
        }
    }

    public static final class Smoking extends SmokingRecipe {
        public Smoking(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float xp, int time) {
            super(group, category, ingredient, result, xp, time);
        }

        @Override
        public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
            return withTier(super.assemble(input, registries), input);
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return SMOKING.get();
        }
    }

    public static final class Campfire extends CampfireCookingRecipe {
        public Campfire(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float xp, int time) {
            super(group, category, ingredient, result, xp, time);
        }

        @Override
        public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
            return withTier(super.assemble(input, registries), input);
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return CAMPFIRE.get();
        }
    }

    private TieredCooking() {}
}
