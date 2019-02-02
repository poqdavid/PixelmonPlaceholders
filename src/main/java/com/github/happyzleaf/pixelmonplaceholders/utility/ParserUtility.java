package com.github.happyzleaf.pixelmonplaceholders.utility;

import com.github.happyzleaf.pixelmonplaceholders.PPConfig;
import com.pixelmonmod.pixelmon.api.pokemon.ISpecType;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonSpec;
import com.pixelmonmod.pixelmon.api.pokemon.SpecFlag;
import com.pixelmonmod.pixelmon.api.world.WeatherType;
import com.pixelmonmod.pixelmon.battles.attacks.AttackBase;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.HiddenPower;
import com.pixelmonmod.pixelmon.entities.npcs.registry.DropItemRegistry;
import com.pixelmonmod.pixelmon.entities.npcs.registry.PokemonDropInformation;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.BaseStats;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.Moveset;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.StatsType;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.evolution.Evolution;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.evolution.conditions.*;
import com.pixelmonmod.pixelmon.enums.EnumSpecies;
import com.pixelmonmod.pixelmon.enums.EnumType;
import com.pixelmonmod.pixelmon.items.heldItems.HeldItem;
import me.rojo8399.placeholderapi.NoValueException;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.entity.Entity;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.*;

public class ParserUtility {
	private static HashMap<EnumSpecies, PokemonDropInformation> pokemonDrops;
	private static Field mainDrop, rareDrop, optDrop1, optDrop2;
	private static Field friendship, level, type, weather;
	
	static {
		try {
			Field pokemonDropsField = DropItemRegistry.class.getDeclaredField("pokemonDrops");
			pokemonDropsField.setAccessible(true);
			pokemonDrops = (HashMap<EnumSpecies, PokemonDropInformation>) pokemonDropsField.get(null);
			mainDrop = PokemonDropInformation.class.getDeclaredField("mainDrop");
			mainDrop.setAccessible(true);
			rareDrop = PokemonDropInformation.class.getDeclaredField("rareDrop");
			rareDrop.setAccessible(true);
			optDrop1 = PokemonDropInformation.class.getDeclaredField("optDrop1");
			optDrop1.setAccessible(true);
			optDrop2 = PokemonDropInformation.class.getDeclaredField("optDrop2");
			optDrop2.setAccessible(true);
			
			friendship = FriendshipCondition.class.getDeclaredField("friendship");
			friendship.setAccessible(true);
			level = LevelCondition.class.getDeclaredField("level");
			level.setAccessible(true);
			type = MoveTypeCondition.class.getDeclaredField("type");
			type.setAccessible(true);
			weather = WeatherCondition.class.getDeclaredField("weather");
			weather.setAccessible(true);
			
		} catch (IllegalAccessException | NoSuchFieldException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param from inclusive
	 * @param to   exclusive
	 */
	public static <T> T[] copyOfRange(T[] original, int from, int to) {
		if (original.length == to - from) {
			try {
				return (T[]) original.getClass().newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			return Arrays.copyOfRange(original, from, to);
		}
	}
	
	public static Object parsePokedexInfo(EnumSpecies pokemon, String[] values) throws NoValueException {
		if (values.length == 0) {
			return pokemon.name;
		}
		
		BaseStats stats = pokemon.getBaseStats();
		
		switch (values[0]) {
			case "name":
				return pokemon.name;
			case "catchrate":
				return stats.catchRate;
			case "nationalid":
				return stats.nationalPokedexNumber;
			case "rarity":
				throw new NoValueException("rarity has been disabled for now");
				/*if (values.length == 2) {
//					int rarity;
					switch (values[1]) {
						case "day":
							rarity = stats.rarity.day;
							break;
						case "night":
							rarity = stats.rarity.night;
							break;
						case "dawndusk":
							rarity = stats.rarity.dawndusk;
							break;
						default:
							throw new NoValueException();
					}
					// return rarity <= 0 ? EnumPokemon.legendaries.contains(pokemon.name) ? 0 : 1 : rarity;
				}
				break;*/
			case "postevolutions":
				return asReadableList(values, 1, stats.evolutions.stream().map(evolution -> evolution.to.name).toArray());
			case "preevolutions":
				return asReadableList(values, 1, stats.preEvolutions);
			case "evolutions":
				//WHAT AM I DOING
				return asReadableList(values, 1, ArrayUtils.addAll(ArrayUtils.add(ArrayUtils.addAll(new Object[]{}, stats.preEvolutions), pokemon.name), stats.evolutions.stream().map(evolution -> evolution.to.name).toArray()));
			case "ability":
				if (values.length > 1) {
					String value1 = values[1];
					int ability = value1.equals("1") ? 0 : value1.equals("2") ? 1 : value1.equalsIgnoreCase("h") ? 2 : -1;
					if (ability != -1) {
						String result = stats.abilities[ability];
						return result == null ? PPConfig.noneText : result;
					}
				} else {
					throw new NoValueException("Not enough arguments.");
				}
			case "abilities":
				return asReadableList(values, 1, stats.abilities);
			case "biomes": // TODO add
				throw new NoValueException("biomes have been disabled for now");
				//return asReadableList(values, 2, Arrays.stream(stats.biomeIDs).map(id -> Biome.getBiome(id).getBiomeName()).toArray());
			case "spawnlocations":
				return asReadableList(values, 1, stats.spawnLocations);
			case "doesevolve":
				return stats.evolutions.size() != 0;
			case "evolutionscount":
				return stats.evolutions.size();
			case "evolution":
				if (values.length > 1) {
					int evolution;
					try {
						evolution = Integer.parseInt(values[1]) - 1;
					} catch (NumberFormatException e) {
						throw new NoValueException();
					}
					if (stats.evolutions.size() <= 0) {
						throw new NoValueException();
					}
					if (stats.evolutions.size() <= evolution) {
						return "Does not evolve.";
					} else {
						Evolution evol = stats.evolutions.get(evolution);
						if (values.length < 3) {
							return stats.evolutions.get(evolution).to.name;
						} else {
							String choice = values[2];
							if (choice.equals("list")) { //TODO write better
								List<String> conditions = new ArrayList<>();
								for (Map.Entry<String, EvoParser> entry : evoParsers.entrySet()) {
									for (EvoCondition cond : evol.conditions) {
										if (cond.getClass().equals(entry.getValue().clazz)) {
											conditions.add(entry.getKey());
										}
									}
								}
								return asReadableList(values, 3, conditions.toArray());
							} else {
								try {
									EvoParser parser = evoParsers.get(values[2]);
									if (parser == null) throw new NoValueException();
									EvoCondition cond = null;
									for (EvoCondition c : evol.conditions) {
										if (c.getClass().equals(parser.clazz)) {
											cond = c;
											break;
										}
									}
									if (cond == null) {
										throw new NoValueException(String.format("The condition %s isn't valid.", values[2]));
									}
									try {
										//noinspection unchecked
										return parser.parse(cond, values, 3);
									} catch (IllegalAccessException e) {
										e.printStackTrace();
									}
								} catch (NoValueException e) {
									return PPConfig.evolutionNotAvailableText;
								}
							}
							
						}
					}
				}
				break;
			case "type":
				return asReadableList(values, 1, stats.getTypeList().toArray());
			case "basestats":
				if (values.length > 1) {
					switch (values[1]) {
						case "hp":
							return stats.stats.get(StatsType.HP);
						case "atk":
							return stats.stats.get(StatsType.Attack);
						case "def":
							return stats.stats.get(StatsType.Defence);
						case "spa":
							return stats.stats.get(StatsType.SpecialAttack);
						case "spd":
							return stats.stats.get(StatsType.SpecialDefence);
						case "spe":
							return stats.stats.get(StatsType.Speed);
						case "yield":
							if (values.length > 2) {
								switch (values[2]) {
									case "hp":
										return stats.evYields.get(StatsType.HP);
									case "atk":
										return stats.evYields.get(StatsType.Attack);
									case "def":
										return stats.evYields.get(StatsType.Defence);
									case "spa":
										return stats.evYields.get(StatsType.SpecialAttack);
									case "spd":
										return stats.evYields.get(StatsType.SpecialDefence);
									case "spe":
										return stats.evYields.get(StatsType.Speed);
								}
							}
							break;
						case "yields":
							return stats.evYields.values().stream().mapToInt(value -> value).sum();
					}
				}
				break;
			case "drops":
				if (values.length > 1) {
					PokemonDropInformation drops = pokemonDrops.get(pokemon);
					if (drops == null) {
						return PPConfig.noneText;
					} else {
						try {
							switch (values[1]) {
								case "main":
									return getItemStackInfo((ItemStack) mainDrop.get(drops));
								case "rare":
									return getItemStackInfo((ItemStack) rareDrop.get(drops));
								case "optional1":
									return getItemStackInfo((ItemStack) optDrop1.get(drops));
								case "optional2":
									return getItemStackInfo((ItemStack) optDrop2.get(drops));
							}
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						}
					}
				}
				break;
			case "egggroups":
				return asReadableList(values, 1, stats.eggGroups);
			case "texturelocation":
				return "pixelmon:sprites/pokemon/" + String.format("%03d", stats.nationalPokedexNumber);
			case "move":
				if (values.length > 1) {
					try {
						Object[] attacks = getAllAttackNames(stats);
						int attack = Integer.parseInt(values[1]) - 1;
						if (attack >= 0 && attack < attacks.length) {
							return attacks[attack];
						} else {
							return PPConfig.moveNotAvailableText;
						}
					} catch (NumberFormatException ignored) {
					}
				}
				break;
			case "moves":
				return asReadableList(values, 1, getAllAttackNames(stats));
		}
		throw new NoValueException();
	}
	
	public static Object[] getAllAttackNames(BaseStats stats) {
		return stats.getAllMoves().stream().map(attack -> attack.baseAttack.getLocalizedName()).toArray();
	}
	
	public static Object parsePokemonInfo(Entity owner, Pokemon pokemon, String[] values) throws NoValueException {
		if (pokemon == null) {
			return PPConfig.teamMemberNotAvailableText;
		}
		
		if (PPConfig.disableEggInfo && pokemon.isEgg()) {
			if (values.length != 1 || !values[0].equals("texturelocation")) {
				return PPConfig.disabledEggText;
			}
		}
		
		if (values.length > 0) {
			switch (values[0]) {
				case "nickname":
					return pokemon.getDisplayName();
				case "exp":
					return formatBigNumbers(pokemon.getExperience());
				case "level":
					return pokemon.getLevel();
				case "exptolevelup":
					return formatBigNumbers(pokemon.getExperienceToLevelUp());
				case "stats":
					if (values.length > 1) {
						switch (values[1]) {
							case "hp":
								return pokemon.getStats().hp;
							case "atk":
								return pokemon.getStats().attack;
							case "def":
								return pokemon.getStats().defence;
							case "spa":
								return pokemon.getStats().specialAttack;
							case "spd":
								return pokemon.getStats().specialDefence;
							case "spe":
								return pokemon.getStats().speed;
							case "ivs":
								if (values.length > 2) {
									switch (values[2]) {
										case "hp":
											return pokemon.getStats().ivs.hp;
										case "atk":
											return pokemon.getStats().ivs.attack;
										case "def":
											return pokemon.getStats().ivs.defence;
										case "spa":
											return pokemon.getStats().ivs.specialAttack;
										case "spd":
											return pokemon.getStats().ivs.specialDefence;
										case "spe":
											return pokemon.getStats().ivs.speed;
										case "total":
											return Arrays.stream(pokemon.getStats().ivs.getArray()).sum();
										case "totalpercentage":
											return formatDouble((Arrays.stream(pokemon.getStats().ivs.getArray()).sum()) * 100 / 186d);
									}
								}
								break;
							case "evs":
								if (values.length > 2) {
									switch (values[2]) {
										case "hp":
											return pokemon.getStats().evs.hp;
										case "atk":
											return pokemon.getStats().evs.attack;
										case "def":
											return pokemon.getStats().evs.defence;
										case "spa":
											return pokemon.getStats().evs.specialAttack;
										case "spd":
											return pokemon.getStats().evs.specialDefence;
										case "spe":
											return pokemon.getStats().evs.speed;
										case "total":
											return Arrays.stream(pokemon.getStats().evs.getArray()).sum();
										case "totalpercentage":
											return formatDouble(Arrays.stream(pokemon.getStats().evs.getArray()).sum() * 100 / 510d);
									}
								}
								break;
						}
					}
					break;
				case "helditem":
					return pokemon.getHeldItem() == ItemStack.EMPTY ? PPConfig.noneText : pokemon.getHeldItem().getDisplayName();
				case "pos":
					if (values.length > 1) {
						EntityPixelmon entity = pokemon.getPixelmonIfExists();
						BlockPos pos = entity == null ? ((net.minecraft.entity.Entity) owner).getPosition() : entity.getPosition();
						switch (values[1]) {
							case "x":
								return pos.getX();
							case "y":
								return pos.getY();
							case "z":
								return pos.getZ();
						}
					}
					break;
				case "moveset":
					Moveset moveset = pokemon.getMoveset();
					try {
						int moveIndex = Integer.parseInt(values[1]) - 1;
						if (moveIndex < 0 || moveIndex >= 4) {
							throw new NoValueException("The attack index must be between 0 and 4.");
						}
						return moveset.get(moveIndex);
					} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
						return asReadableList(values, 1, moveset.attacks);
					}
				case "friendship":
					return formatBigNumbers(pokemon.getFriendship());
				case "ability":
					if (values.length == 1) {
						return pokemon.getAbility().getName();
					}
					break;
				case "ball":
					return pokemon.getCaughtBall().getItem().getLocalizedName();
				//case "possibledrops":
				//	return asReadableList(pokeValues, 1, DropItemRegistry.getDropsForPokemon(pokemon).stream().map(ParserUtility::getItemStackInfo).toArray());
				case "nature":
					return pokemon.getNature();
				case "gender":
					return pokemon.getGender().name();
				case "growth":
					return pokemon.getGrowth().name();
				case "shiny":
					return pokemon.isShiny();
				case "hiddenpower":
					return HiddenPower.getHiddenPowerType(pokemon.getStats().ivs);
				case "texturelocation":
					return getSprite(pokemon);
				case "customtexture":
					return getCustomTexture(pokemon);
				case "form":
					return pokemon.getForm();
				case "extraspecs":
					if (values.length > 1) {
						ISpecType spec = PokemonSpec.getSpecForKey(values[1]);
						if (spec instanceof SpecFlag) { //Could move to SpecType<Boolean> but let's imply this is the standard for boolean-based specs.
							return ((SpecFlag) spec).matches(pokemon);
						} else {
							throw new NoValueException("Spec not supported.");
						}
					}
					throw new NoValueException("Not enough arguments.");
				case "aura":
					return getAuraID(pokemon);
			}
		}
		
		return parsePokedexInfo(pokemon.getSpecies(), values);
	}
	
	private static Object getCustomTexture(Pokemon pokemon) {
		String custom = pokemon.getCustomTexture();
		if (custom == null || custom.isEmpty()) {
			return PPConfig.noneText;
		}
		return custom;
	}
	
	private static Key<Value<String>> PARTICLE_ID;
	
	static {
		try {
			PARTICLE_ID = (Key<Value<String>>) Class.forName("de.randombyte.entityparticles.data.EntityParticlesKeys").getField("PARTICLE_ID").get(null);
		} catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException | ClassCastException e) {
			PARTICLE_ID = null;
		}
	}
	
	private static Object getAuraID(Pokemon pokemon) {
		EntityPixelmon entity = pokemon.getOrSpawnPixelmon(null);
		Object result = PARTICLE_ID == null ? PPConfig.noneText : ((Entity) entity).get(PARTICLE_ID).map(p -> (Object) p).orElse(PPConfig.noneText);
		entity.unloadEntity();
		return result;
	}
	
	private static String getSprite(Pokemon pokemon) {
		return getSprite(pokemon.getSpecies(), pokemon.isShiny(), pokemon.isEgg(), pokemon.getEggCycles(), pokemon.getForm());
	}
	
	// Based on com.pixelmonmod.pixelmon.client.render.tileEntities.RenderTileEntityTradingMachine#getSpriteFromID
	private static String getSprite(EnumSpecies pokemon, boolean isShiny, boolean isEgg, int eggCycles, int variant) {
		String basePath = "textures/sprites/pokemon/";
		if (isShiny) {
			basePath = "textures/sprites/shinypokemon/";
		} else if (isEgg) {
			basePath = "textures/sprites/eggs/";
		}
		
		if (isEgg) {
			String eggType = "egg";
			if (pokemon == EnumSpecies.Togepi) {
				eggType = "togepi";
			} else if (pokemon == EnumSpecies.Manaphy) {
				eggType = "manaphy";
			}
			
			if (eggCycles > 10) { //show un-cracked
				return basePath + eggType + "1.png";
			} else if (eggCycles > 5) { //show crack 1
				return basePath + eggType + "2.png";
			} else { //show crack 2
				return basePath + eggType + "3.png";
			}
		} else {
			return basePath + pokemon.getNationalPokedexNumber() + pokemon.getFormEnum(variant).getSpriteSuffix();
		}
	}
	
	/**
	 * @param index The index in the array values where the method should start
	 */
	public static Object asReadableList(String[] values, int index, Object[] data) {
		String separator = ", ";
		if (values.length == index + 1) {
			separator = values[index].replaceAll("--", " ");
		}
		String list = "";
		for (int i = 0; i < data.length; i++) {
			Object d = data[i];
			if (i == 0) {
				list = d.toString();
			} else {
				list = list.concat(separator + d.toString());
			}
		}
		return list.isEmpty() ? PPConfig.noneText : list;
	}
	
	public static String normalizeText(String text) {
		return text.substring(1) + text.substring(1, text.length() - 1);
	}
	
	public static String formatBigNumbers(int number) {
		if (number < 1000) {
			return String.valueOf(number);
		} else if (number < 1000000) {
			return String.valueOf((double) Math.round(number / 100) / 10) + "k";
		} else if (number < 1000000000) {
			return String.valueOf((double) Math.round(number / 100000) / 10) + "m";
		} else {
			return String.valueOf((double) Math.round(number / 100000000) / 10) + "b";
		}
	}
	
	private static DecimalFormat formatter = new DecimalFormat();
	
	static {
		formatter.setMaximumFractionDigits(PPConfig.maxFractionDigits);
		formatter.setMinimumFractionDigits(PPConfig.minFractionDigits);
	}
	
	public static String formatDouble(double number) {
		return formatter.format(number);
	}
	
	public static Object getItemStackInfo(@Nullable ItemStack is) {
		return is == null || is.getCount() == 0 ? PPConfig.noneText : is.getCount() + " " + is.getDisplayName();
	}
	
	private static Map<String, EvoParser> evoParsers = new HashMap<>();
	
	static {
		evoParsers.put("biome", new EvoParser<BiomeCondition>(BiomeCondition.class) {
			@Override
			public Object parse(BiomeCondition condition, String[] values, int index) { //TODO test
				return asReadableList(values, index, condition.biomes.stream().map(biome -> ForgeRegistries.BIOMES.getValues().stream().filter(b -> biome.equalsIgnoreCase(b.biomeName)).findFirst().get()).toArray());
			}
		});
		evoParsers.put("chance", new EvoParser<ChanceCondition>(ChanceCondition.class) {
			@Override
			public Object parse(ChanceCondition condition, String[] values, int index) {
				return formatDouble(condition.chance);
			}
		});
		evoParsers.put("stone", new EvoParser<EvoRockCondition>(EvoRockCondition.class) {
			@Override
			public Object parse(EvoRockCondition condition, String[] values, int index) throws NoValueException {
				if (values.length > index) {
					if (values[index].equals("biome")) {
						return asReadableList(values, index + 1, Arrays.stream(condition.evolutionRock.biomes).map(biome -> biome.biomeName).toArray());
					} else {
						throw new NoValueException();
					}
				}
				return condition.evolutionRock;
			}
		});
		evoParsers.put("friendship", new EvoParser<FriendshipCondition>(FriendshipCondition.class) {
			@Override
			public Object parse(FriendshipCondition condition, String[] values, int index) throws IllegalAccessException {
				int f = friendship.getInt(condition);
				return f == -1 ? 220 : f;
			}
		});
		evoParsers.put("gender", new EvoParser<GenderCondition>(GenderCondition.class) {
			@Override
			public Object parse(GenderCondition condition, String[] values, int index) {
				return asReadableList(values, index, condition.genders/*.stream().filter(gender -> gender != Gender.None)*/.toArray());
			}
		});
		evoParsers.put("helditem", new EvoParser<HeldItemCondition>(HeldItemCondition.class) {
			@Override
			public Object parse(HeldItemCondition condition, String[] values, int index) { //TODO test
				return ((HeldItem) ForgeRegistries.ITEMS.getValue(new ResourceLocation(condition.item.itemID))).getLocalizedName();
			}
		});
		evoParsers.put("altitude", new EvoParser<HighAltitudeCondition>(HighAltitudeCondition.class) {
			@Override
			public Object parse(HighAltitudeCondition condition, String[] values, int index) {
				return formatDouble(condition.minAltitude);
			}
		});
		evoParsers.put("level", new EvoParser<LevelCondition>(LevelCondition.class) {
			@Override
			public Object parse(LevelCondition condition, String[] values, int index) throws IllegalAccessException {
				return level.getInt(condition);
			}
		});
		evoParsers.put("move", new EvoParser<MoveCondition>(MoveCondition.class) {
			@Override
			public Object parse(MoveCondition condition, String[] values, int index) { //TODO test
				return AttackBase.getAttackBase(condition.attackIndex).map(attackBase -> (Object) attackBase.getLocalizedName()).orElse(PPConfig.noneText);
			}
		});
		evoParsers.put("movetype", new EvoParser<MoveTypeCondition>(MoveTypeCondition.class) {
			@Override
			public Object parse(MoveTypeCondition condition, String[] values, int index) throws IllegalAccessException {
				return ((EnumType) type.get(condition)).getLocalizedName();
			}
		});
//		evoParsers.put("partyalolan", new EvoParser<PartyAlolanCondition>(PartyAlolanCondition.class) {
//			@Override
//			public Object parse(PartyAlolanCondition condition, String[] values, int index) throws NoValueException, IllegalAccessException {
//				throw new NoValueException();
//			}
//		});
		evoParsers.put("party", new EvoParser<PartyCondition>(PartyCondition.class) {
			@Override
			public Object parse(PartyCondition condition, String[] values, int index) throws NoValueException {
				if (values.length > index) {
					if (values[index].equals("pokemon")) {
						return asReadableList(values, index + 1, condition.withPokemon.toArray());
					} else if (values[index].equals("type")) {
						return asReadableList(values, index + 1, condition.withTypes.toArray());
					}
				}
				throw new NoValueException();
			}
		});
		//TODO StatRatioCondition
		evoParsers.put("time", new EvoParser<TimeCondition>(TimeCondition.class) { //TODO fix
			@Override
			public Object parse(TimeCondition condition, String[] values, int index) throws NoValueException {
				return normalizeText(condition.time.name());
			}
		});
		evoParsers.put("weather", new EvoParser<WeatherCondition>(WeatherCondition.class) {
			@Override
			public Object parse(WeatherCondition condition, String[] values, int index) throws IllegalAccessException {
				return normalizeText(((WeatherType) weather.get(condition)).name());
			}
		});
	}
	
	public static abstract class EvoParser<T extends EvoCondition> {
		public Class<T> clazz;
		
		public EvoParser(Class<T> clazz) {
			this.clazz = clazz;
		}
		
		public abstract Object parse(T condition, String[] values, int index) throws NoValueException, IllegalAccessException;
	}
}
