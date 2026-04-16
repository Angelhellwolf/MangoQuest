package me.Cutiemango.MangoQuest.manager;

import me.Cutiemango.MangoQuest.ConfigSettings;
import me.Cutiemango.MangoQuest.DebugHandler;
import me.Cutiemango.MangoQuest.I18n;
import me.Cutiemango.MangoQuest.Main;
import me.Cutiemango.MangoQuest.QuestUtil;
import me.Cutiemango.MangoQuest.data.QuestPlayerData;
import me.Cutiemango.MangoQuest.model.Quest;
import me.Cutiemango.MangoQuest.objects.RequirementType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class RequirementManager
{
	public static Optional<String> meetRequirementWith(Player p, EnumMap<RequirementType, Object> requirements, boolean sendMsg) {
		QuestPlayerData pd = QuestUtil.getData(p);

		List<String> failMsg = new ArrayList<>();

		for (RequirementType t : requirements.keySet()) {
			Object value = requirements.get(t);
			switch (t) {
				case PERMISSION:
					if (!(value instanceof List)) {
						if (sendMsg)
							DebugHandler.log(5, "[Requirements] Requirement type is PERMISSION, but the value is not a list.");
						break;
					}
					List<String> permissions = (List<String>) value;
					for (String perm : permissions) {
						if (!p.hasPermission(perm)) {
							failMsg.add(I18n.locMsg("Requirements.NotMeet.Permission"));
							break;
						}
					}
					break;
				case QUEST:
					for (String s : (List<String>) value) {
						Quest q = QuestUtil.getQuest(s);
						if (!pd.hasFinished(q))
							failMsg.add(I18n.locMsg("Requirements.NotMeet.Quest", q.getQuestName()));
					}
					break;
				case LEVEL:
					int level = (Integer) value;
					if (p.getLevel() < level)
						failMsg.add(I18n.locMsg("Requirements.NotMeet.Level", Integer.toString(level)));
					break;
				case MONEY:
					if (Main.getHooker().hasEconomyEnabled()) {
						double money = (Double) value;
						if (!Main.getHooker().getEconomy().hasAccount(p) || Main.getHooker().getEconomy().getBalance(p) < money)
							failMsg.add(I18n.locMsg("Requirements.NotMeet.Money", Double.toString(QuestUtil.cut(money))));
					}
					break;
				case ITEM:
					if (!(value instanceof List)) {
						if (sendMsg)
							DebugHandler.log(5, "[Requirements] Requirement type is ITEM, but the value is not a list.");
						break;
					}
					if (ConfigSettings.USE_WEAK_ITEM_CHECK) {
						for (ItemStack reqItem : (List<ItemStack>) value) {
							int need = reqItem.getAmount();
							for (ItemStack owned : p.getInventory().getContents()) {
								if (owned == null || owned.getType() == Material.AIR)
									continue;
								if (QuestValidater.weakItemCheck(reqItem, owned, false)) {
									need -= Math.min(need, reqItem.getAmount());
									if (need == 0) break;
								}
							}
							if (need > 0) {
								failMsg.add(I18n.locMsg("Requirements.NotMeet.Item", QuestUtil.getItemName(reqItem), Integer.toString(reqItem.getAmount())));
								if (sendMsg) {
									DebugHandler.log(5, "[Requirements] User has failed requirement: " + t.toString());
									DebugHandler
											.log(5, "[Requirements] Did not found enough (or any) %s in user's inventory.", QuestUtil.getItemName(reqItem));
								}
							}
						}
					} else {
						for (ItemStack reqItem : (List<ItemStack>) value) {
							if (!p.getInventory().containsAtLeast(reqItem, reqItem.getAmount())) {
								failMsg.add(I18n.locMsg("Requirements.NotMeet.Item", QuestUtil.getItemName(reqItem), Integer.toString(reqItem.getAmount())));
								if (sendMsg) {
									DebugHandler.log(5, "[Requirements] User has failed requirement: " + t.toString());
									DebugHandler
											.log(5, "[Requirements] Did not found enough (or any) %s in user's inventory.", QuestUtil.getItemName(reqItem));
								}
							}
						}
					}
					break;
				case SKILLAPI_CLASS:
					String classID = (String) value;
					if (classID.equalsIgnoreCase("none"))
						break;

					if (Main.getHooker().hasSkillAPIEnabled()) {
						if (!Main.getHooker().isSkillAPIClassRegistered(classID))
							failMsg.add(I18n.locMsg("Requirements.NotMeet.NoClass") + classID);
						else if (!Main.getHooker().playerHasSkillAPIClass(p, classID, (Boolean) requirements.get(RequirementType.ALLOW_DESCENDANT))) {
							failMsg.add(I18n.locMsg("Requirements.NotMeet.RPGClass", Main.getHooker().getSkillAPIClassPrefix(classID)));
							if (sendMsg)
								DebugHandler.log(5, "[Requirements] User has failed requirement: " + t.toString());
						}
					}
					break;
				case SKILLAPI_LEVEL:
					int lvl = (Integer) value;
					if (lvl == 0)
						break;

					else if (Main.getHooker().hasSkillAPIEnabled())
						if (!Main.getHooker().playerHasSkillAPILevel(p, lvl))
							failMsg.add(I18n.locMsg("Requirements.NotMeet.RPGLevel", Integer.toString(lvl)));
					break;
				case QRPG_CLASS:
					classID = (String) value;
					if (classID.equalsIgnoreCase("none"))
						break;

					if (Main.getHooker().hasQuantumRPGEnabled()) {
						if (!Main.getHooker().isQuantumRPGClassRegistered(classID))
							failMsg.add(I18n.locMsg("Requirements.NotMeet.NoClass") + classID);
						else if (!Main.getHooker().playerHasQuantumRPGClass(p, classID, (Boolean) requirements.get(RequirementType.ALLOW_DESCENDANT))) {
							failMsg.add(I18n.locMsg("Requirements.NotMeet.RPGClass", Main.getHooker().getQuantumRPGClassName(classID)));
							if (sendMsg)
								DebugHandler.log(5, "[Requirements] User has failed requirement: " + t.toString());
						}
					}
					break;
				case QRPG_LEVEL:
					lvl = (Integer) value;
					if (lvl == 0)
						break;
					else if (Main.getHooker().hasQuantumRPGEnabled()) {
						if (!Main.getHooker().playerHasQuantumRPGLevel(p, lvl)) {
							failMsg.add(I18n.locMsg("Requirements.NotMeet.RPGLevel", Integer.toString(lvl)));
							if (sendMsg)
								DebugHandler.log(5, "[Requirements] User has failed requirement: " + t.toString());
						}
					}
					break;
				case FRIEND_POINT:
					HashMap<Integer, Integer> map = (HashMap<Integer, Integer>) value;

					for (Integer id : map.keySet()) {
						if (pd.getNPCfp(id) < map.get(id))
							failMsg.add(I18n.locMsg("Requirements.NotMeet.FriendPoint"));
					}
					break;
			}
		}

		if (failMsg.isEmpty())
			return Optional.empty();
		return Optional.of(String.join("\n", failMsg));
	}

}
