package me.Cutiemango.MangoQuest.manager;

import me.Cutiemango.MangoQuest.DebugHandler;
import me.Cutiemango.MangoQuest.I18n;
import me.Cutiemango.MangoQuest.Main;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Level;

public class PluginHooker
{
	public PluginHooker(Main main) {
		plugin = main;
	}

	private final Main plugin;

	private Economy economy;
	private boolean citizens;
	private Plugin mythicMobs;
	private Plugin shopkeepers;
	private Plugin skillAPI;
	private Plugin quantumRPG;
	private Object mythicMobsAPIHelper;

	public void hookPlugins() {
		economy = null;
		citizens = false;
		mythicMobs = null;
		shopkeepers = null;
		skillAPI = null;
		quantumRPG = null;
		mythicMobsAPIHelper = null;

		if (plugin.getServer().getPluginManager().isPluginEnabled("Citizens")) {
			citizens = true;
			QuestChatManager.logCmd(Level.INFO, I18n.locMsg("PluginHooker.CitizensHooked"));
		} else {
			QuestChatManager.logCmd(Level.SEVERE, I18n.locMsg("PluginHooker.PluginNotHooked"));
			QuestChatManager.logCmd(Level.SEVERE, I18n.locMsg("PluginHooker.CitizensNotHooked1"));
			QuestChatManager.logCmd(Level.SEVERE, I18n.locMsg("PluginHooker.CitizensNotHooked2"));
			QuestChatManager.logCmd(Level.SEVERE, I18n.locMsg("PluginHooker.PleaseInstall"));
		}

		if (plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
			QuestChatManager.logCmd(Level.INFO, I18n.locMsg("PluginHooker.VaultHooked"));
		} else {
			QuestChatManager.logCmd(Level.SEVERE, I18n.locMsg("PluginHooker.PluginNotHooked"));
			QuestChatManager.logCmd(Level.SEVERE, I18n.locMsg("PluginHooker.VaultNotHooked"));
			QuestChatManager.logCmd(Level.SEVERE, I18n.locMsg("PluginHooker.PleaseInstall"));
		}

		mythicMobs = hookOptionalPlugin("MythicMobs", "PluginHooker.MythicMobsHooked");
		shopkeepers = hookOptionalPlugin("Shopkeepers", "PluginHooker.ShopkeepersHooked");
		skillAPI = hookOptionalPlugin("SkillAPI", "PluginHooker.SkillAPIHooked");
		quantumRPG = hookOptionalPlugin("QuantumRPG", "PluginHooker.QuantumRPGHooked");

		RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServicesManager().getRegistration(Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
			QuestChatManager.logCmd(Level.INFO, I18n.locMsg("PluginHooker.EconomyHooked"));
		} else {
			QuestChatManager.logCmd(Level.SEVERE, I18n.locMsg("PluginHooker.EconomyNotHooked"));
		}
	}

	private Plugin hookOptionalPlugin(String pluginName, String messageKey) {
		Plugin hooked = plugin.getServer().getPluginManager().getPlugin(pluginName);
		if (hooked != null && hooked.isEnabled()) {
			QuestChatManager.logCmd(Level.INFO, I18n.locMsg(messageKey));
			return hooked;
		}
		DebugHandler.log(1, pluginName + " not hooked.");
		return null;
	}

	public boolean hasEconomyEnabled() {
		return economy != null;
	}

	public boolean hasMythicMobEnabled() {
		return mythicMobs != null;
	}

	public boolean hasCitizensEnabled() {
		return citizens;
	}

	public boolean hasShopkeepersEnabled() {
		return shopkeepers != null;
	}

	public boolean hasSkillAPIEnabled() {
		return skillAPI != null;
	}

	public boolean hasQuantumRPGEnabled() {
		return quantumRPG != null;
	}

	public Economy getEconomy() {
		return economy;
	}

	public MythicMobInfo getMythicMob(String id) {
		if (!hasMythicMobEnabled())
			return null;
		Object helper = getMythicMobsAPIHelper();
		Object mob = helper == null ? null : invoke(helper, "getMythicMob", id);
		return toMythicMobInfo(mob, id);
	}

	public boolean isMythicMob(Entity entity) {
		if (!hasMythicMobEnabled())
			return false;
		Object helper = getMythicMobsAPIHelper();
		Object result = helper == null ? null : invoke(helper, "isMythicMob", entity);
		if (result instanceof Boolean)
			return (Boolean) result;
		return getMythicMobId(entity) != null;
	}

	public String getMythicMobId(Entity entity) {
		if (!hasMythicMobEnabled())
			return null;
		Object helper = getMythicMobsAPIHelper();
		Object activeMob = helper == null ? null : invoke(helper, "getMythicMobInstance", entity);
		activeMob = unwrapOptional(activeMob);
		if (activeMob == null)
			return null;
		Object type = firstValue(activeMob, "getType", "getMobType");
		MythicMobInfo info = toMythicMobInfo(type == null ? activeMob : type, null);
		return info == null ? null : info.getInternalName();
	}

	public boolean isShopkeeper(Entity entity) {
		if (!hasShopkeepersEnabled())
			return false;
		Object registry = getShopkeeperRegistry();
		Object result = registry == null ? null : invoke(registry, "isShopkeeper", entity);
		return result instanceof Boolean && (Boolean) result;
	}

	public boolean openShopkeeper(Entity entity, Player player) {
		if (!hasShopkeepersEnabled())
			return false;
		Object registry = getShopkeeperRegistry();
		Object shopkeeper = registry == null ? null : invoke(registry, "getShopkeeperByEntity", entity);
		if (shopkeeper == null)
			return false;
		invoke(shopkeeper, "openTradingWindow", player);
		return true;
	}

	public boolean isSkillAPIClassRegistered(String classId) {
		if (classId == null || classId.equalsIgnoreCase("none"))
			return true;
		Class<?> clazz = findClass("com.sucy.skill.SkillAPI");
		Object result = clazz == null ? null : invokeStatic(clazz, "isClassRegistered", classId);
		return result instanceof Boolean && (Boolean) result;
	}

	public String getSkillAPIClassName(String classId) {
		Object skillClass = getSkillAPIClass(classId);
		String name = stringFrom(firstValue(skillClass, "getName", "getPrefix", "getId"));
		return name == null ? classId : name;
	}

	public String getSkillAPIClassPrefix(String classId) {
		Object skillClass = getSkillAPIClass(classId);
		String prefix = stringFrom(firstValue(skillClass, "getPrefix", "getName", "getId"));
		return prefix == null ? classId : prefix;
	}

	public boolean playerHasSkillAPIClass(Player player, String classId, boolean allowDescendant) {
		if (!hasSkillAPIEnabled() || classId == null || classId.equalsIgnoreCase("none"))
			return true;
		Object reqClass = getSkillAPIClass(classId);
		if (reqClass == null)
			return false;
		Object playerData = getSkillAPIPlayerData(player);
		Object mainClass = playerData == null ? null : invoke(playerData, "getMainClass");
		Object userClass = mainClass == null ? null : invoke(mainClass, "getData");
		if (userClass == null)
			return false;
		Object exact = invoke(playerData, "isExactClass", reqClass);
		if (exact instanceof Boolean && (Boolean) exact)
			return true;
		return allowDescendant && hasParentClass(userClass, classId);
	}

	public boolean playerHasSkillAPILevel(Player player, int level) {
		if (!hasSkillAPIEnabled() || level == 0)
			return true;
		Object playerData = getSkillAPIPlayerData(player);
		Object mainClass = playerData == null ? null : invoke(playerData, "getMainClass");
		Object currentLevel = mainClass == null ? null : invoke(mainClass, "getLevel");
		return numberValue(currentLevel) >= level;
	}

	public void giveSkillAPIExp(Player player, int amount) {
		if (!hasSkillAPIEnabled())
			return;
		Object playerData = getSkillAPIPlayerData(player);
		Class<?> expSourceClass = findClass("com.sucy.skill.api.enums.ExpSource");
		Object expSource = expSourceClass != null && expSourceClass.isEnum() ? Enum.valueOf((Class<Enum>) expSourceClass, "COMMAND") : null;
		if (playerData != null && expSource != null)
			invoke(playerData, "giveExp", amount, expSource);
	}

	public boolean isQuantumRPGClassRegistered(String classId) {
		if (classId == null || classId.equalsIgnoreCase("none"))
			return true;
		return getQuantumRPGClass(classId) != null;
	}

	public String getQuantumRPGClassName(String classId) {
		Object rpgClass = getQuantumRPGClass(classId);
		String name = stringFrom(firstValue(rpgClass, "getName", "getId"));
		return name == null ? classId : name;
	}

	public boolean playerHasQuantumRPGClass(Player player, String classId, boolean allowDescendant) {
		if (!hasQuantumRPGEnabled() || classId == null || classId.equalsIgnoreCase("none"))
			return true;
		Object reqClass = getQuantumRPGClass(classId);
		if (reqClass == null)
			return false;
		String userClassId = getQuantumRPGPlayerClassId(player);
		if (userClassId == null)
			return false;
		if (classId.equalsIgnoreCase(userClassId))
			return true;
		return allowDescendant && hasParentClass(getQuantumRPGClass(userClassId), classId);
	}

	public boolean playerHasQuantumRPGLevel(Player player, int level) {
		if (!hasQuantumRPGEnabled() || level == 0)
			return true;
		Object classData = getQuantumRPGClassData(player);
		return numberValue(classData == null ? null : invoke(classData, "getLevel")) >= level;
	}

	public void giveQuantumRPGExp(Player player, int amount) {
		if (!hasQuantumRPGEnabled())
			return;
		Object classData = getQuantumRPGClassData(player);
		if (classData != null)
			invoke(classData, "addExp", amount);
	}

	public NPC getNPC(String id) {
		if (!hasCitizensEnabled() || !QuestValidater.validateInteger(id))
			return null;
		return CitizensAPI.getNPCRegistry().getById(Integer.parseInt(id));
	}

	public NPC getNPC(int id) {
		if (!hasCitizensEnabled())
			return null;
		return CitizensAPI.getNPCRegistry().getById(id);
	}

	private Object getMythicMobsAPIHelper() {
		if (mythicMobsAPIHelper != null)
			return mythicMobsAPIHelper;
		Class<?> mythicClass = findClass("io.lumine.mythic.bukkit.MythicBukkit", "io.lumine.xikage.mythicmobs.MythicMobs");
		Object mythicInstance = mythicClass == null ? null : invokeStatic(mythicClass, "inst");
		mythicMobsAPIHelper = mythicInstance == null ? null : invoke(mythicInstance, "getAPIHelper");
		return mythicMobsAPIHelper;
	}

	private Object getShopkeeperRegistry() {
		Class<?> apiClass = findClass("com.nisovin.shopkeepers.api.ShopkeepersAPI");
		return apiClass == null ? null : invokeStatic(apiClass, "getShopkeeperRegistry");
	}

	private Object getSkillAPIClass(String classId) {
		if (classId == null || classId.equalsIgnoreCase("none"))
			return null;
		Class<?> clazz = findClass("com.sucy.skill.SkillAPI");
		return clazz == null ? null : invokeStatic(clazz, "getClass", classId);
	}

	private Object getSkillAPIPlayerData(Player player) {
		Class<?> clazz = findClass("com.sucy.skill.SkillAPI");
		if (clazz == null)
			return null;
		Object hasData = invokeStatic(clazz, "hasPlayerData", player);
		if (hasData instanceof Boolean && !(Boolean) hasData)
			return null;
		return invokeStatic(clazz, "getPlayerData", player);
	}

	private Object getQuantumRPGClass(String classId) {
		if (!hasQuantumRPGEnabled() || classId == null || classId.equalsIgnoreCase("none"))
			return null;
		Object classManager = getQuantumRPGClassManager();
		return classManager == null ? null : invoke(classManager, "getClassById", classId);
	}

	private Object getQuantumRPGClassManager() {
		Object cache = invoke(quantumRPG, "getModuleCache");
		return cache == null ? null : invoke(cache, "getClassManager");
	}

	private Object getQuantumRPGClassData(Player player) {
		Object userManager = invoke(quantumRPG, "getUserManager");
		Object user = userManager == null ? null : invoke(userManager, "getOrLoadUser", player);
		Object profile = user == null ? null : invoke(user, "getActiveProfile");
		return profile == null ? null : invoke(profile, "getClassData");
	}

	private String getQuantumRPGPlayerClassId(Player player) {
		Object classData = getQuantumRPGClassData(player);
		return stringFrom(classData == null ? null : invoke(classData, "getClassId"));
	}

	private MythicMobInfo toMythicMobInfo(Object value, String fallbackId) {
		Object mob = unwrapOptional(value);
		if (mob == null)
			return null;
		String internalName = stringFrom(firstValue(mob, "getInternalName", "getId", "getName"));
		String displayName = stringFrom(firstValue(mob, "getDisplayName", "getName", "getInternalName", "getId"));
		String entityType = stringFrom(firstValue(mob, "getEntityType", "getType"));
		if (internalName == null)
			internalName = fallbackId;
		if (displayName == null)
			displayName = internalName;
		if (internalName == null)
			return null;
		return new MythicMobInfo(internalName, displayName, entityType);
	}

	private boolean hasParentClass(Object userClass, String classId) {
		Object current = userClass;
		for (int i = 0; current != null && i < 32; i++) {
			Object parent = invoke(current, "getParent");
			if (parent == null)
				return false;
			if (matchesClass(parent, classId))
				return true;
			current = parent;
		}
		return false;
	}

	private boolean matchesClass(Object rpgClass, String classId) {
		String id = stringFrom(firstValue(rpgClass, "getId", "getName", "getInternalName", "getPrefix"));
		return id != null && id.equalsIgnoreCase(classId);
	}

	private Object firstValue(Object target, String... methodNames) {
		if (target == null)
			return null;
		for (String methodName : methodNames) {
			Object value = invoke(target, methodName);
			value = unwrapOptional(value);
			if (value != null)
				return value;
		}
		return null;
	}

	private static Class<?> findClass(String... names) {
		for (String name : names) {
			try {
				return Class.forName(name);
			}
			catch (ClassNotFoundException ignored) {
			}
		}
		return null;
	}

	private static Object invokeStatic(Class<?> clazz, String methodName, Object... args) {
		return invokeMethod(clazz, null, methodName, args);
	}

	private static Object invoke(Object target, String methodName, Object... args) {
		if (target == null)
			return null;
		return invokeMethod(target.getClass(), target, methodName, args);
	}

	private static Object invokeMethod(Class<?> clazz, Object target, String methodName, Object... args) {
		Method method = findMethod(clazz, methodName, args);
		if (method == null)
			return null;
		try {
			return unwrapOptional(method.invoke(target, args));
		}
		catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
			DebugHandler.log(4, "[PluginHooker] Failed to invoke " + clazz.getName() + "#" + methodName + ": " + e.getMessage());
			return null;
		}
	}

	private static Method findMethod(Class<?> clazz, String methodName, Object... args) {
		for (Method method : clazz.getMethods()) {
			if (!method.getName().equals(methodName) || method.getParameterCount() != args.length)
				continue;
			Class<?>[] parameterTypes = method.getParameterTypes();
			boolean compatible = true;
			for (int i = 0; i < parameterTypes.length; i++) {
				if (!isCompatible(parameterTypes[i], args[i])) {
					compatible = false;
					break;
				}
			}
			if (compatible)
				return method;
		}
		return null;
	}

	private static boolean isCompatible(Class<?> parameterType, Object arg) {
		if (arg == null)
			return !parameterType.isPrimitive();
		Class<?> argType = arg.getClass();
		if (!parameterType.isPrimitive())
			return parameterType.isAssignableFrom(argType);
		if (parameterType == int.class)
			return argType == Integer.class;
		if (parameterType == long.class)
			return argType == Long.class;
		if (parameterType == double.class)
			return argType == Double.class;
		if (parameterType == float.class)
			return argType == Float.class;
		if (parameterType == boolean.class)
			return argType == Boolean.class;
		if (parameterType == byte.class)
			return argType == Byte.class;
		if (parameterType == short.class)
			return argType == Short.class;
		if (parameterType == char.class)
			return argType == Character.class;
		return false;
	}

	private static Object unwrapOptional(Object value) {
		if (value instanceof Optional<?>)
			return ((Optional<?>) value).orElse(null);
		return value;
	}

	private static String stringFrom(Object value) {
		Object unwrapped = unwrapOptional(value);
		return unwrapped == null ? null : String.valueOf(unwrapped);
	}

	private static double numberValue(Object value) {
		if (value instanceof Number)
			return ((Number) value).doubleValue();
		if (value == null)
			return 0;
		try {
			return Double.parseDouble(String.valueOf(value));
		}
		catch (NumberFormatException ignored) {
			return 0;
		}
	}
}
