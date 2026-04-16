package me.Cutiemango.MangoQuest.versions;

import me.Cutiemango.MangoQuest.I18n;
import me.Cutiemango.MangoQuest.Main;
import me.Cutiemango.MangoQuest.QuestUtil;
import me.Cutiemango.MangoQuest.manager.QuestChatManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.ItemTag;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Item;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;

public class ModernVersionHandler implements VersionHandler
{
	private final NamespacedKey guiItemKey = new NamespacedKey(Main.getInstance(), "gui_item");

	@Override
	public void sendTitle(Player p, Integer fadeIn, Integer stay, Integer fadeOut, String title, String subtitle) {
		p.sendTitle(
				QuestChatManager.translateColor(title == null ? "" : title),
				QuestChatManager.translateColor(subtitle == null ? "" : subtitle),
				fadeIn,
				stay,
				fadeOut
		);
	}

	@Override
	public void openBook(Player p, TextComponent... texts) {
		ArrayList<BaseComponent[]> pages = new ArrayList<>();
		for (TextComponent text : texts)
			pages.add(new BaseComponent[] { text });

		ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
		BookMeta meta = (BookMeta) book.getItemMeta();
		if (meta == null)
			return;

		meta.spigot().setPages(pages.toArray(new BaseComponent[][] {}));
		meta.setAuthor("MangoQuest");
		meta.setTitle("MangoQuest");
		book.setItemMeta(meta);

		p.openBook(book);
	}

	@Override
	public TextComponent textFactoryConvertLocation(String name, Location loc, boolean isFinished) {
		if (loc == null)
			return new TextComponent("");

		ItemStack item = new ItemStack(Material.PAINTING);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(name);
			String displayMsg = I18n.locMsg("QuestJourney.NPCLocDisplay",
					loc.getWorld().getName(),
					Integer.toString(loc.getBlockX()),
					Integer.toString(loc.getBlockY()),
					Integer.toString(loc.getBlockZ()));
			meta.setLore(QuestUtil.createList(displayMsg));
			item.setItemMeta(meta);
		}

		TextComponent text = new TextComponent(isFinished ? QuestChatManager.finishedObjectFormat(name) : name);
		text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, buildHoverItem(item)));
		return text;
	}

	@Override
	public TextComponent textFactoryConvertItem(final ItemStack item, boolean finished) {
		String displayText = QuestUtil.translate(item);

		if (finished)
			displayText = QuestChatManager.finishedObjectFormat(displayText);
		else
			displayText = ChatColor.BLACK + displayText;

		TextComponent text = new TextComponent(displayText);
		if (item != null && item.getType() != Material.AIR)
			text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, buildHoverItem(item)));
		return text;
	}

	@Override
	public boolean hasTag(Player p, String s) {
		return p.getScoreboardTags().contains(s);
	}

	@Override
	public ItemStack addGUITag(ItemStack item) {
		if (item == null)
			return null;
		ItemStack copy = item.clone();
		ItemMeta meta = copy.getItemMeta();
		if (meta == null)
			return copy;
		meta.getPersistentDataContainer().set(guiItemKey, PersistentDataType.BYTE, (byte) 1);
		copy.setItemMeta(meta);
		return copy;
	}

	@Override
	public boolean hasGUITag(ItemStack item) {
		if (item == null || !item.hasItemMeta())
			return false;
		ItemMeta meta = item.getItemMeta();
		if (meta == null)
			return false;
		PersistentDataContainer container = meta.getPersistentDataContainer();
		return container.has(guiItemKey, PersistentDataType.BYTE);
	}

	@Override
	public void playNPCEffect(Player p, Location location) {
		Location effectLocation = location.clone().add(0, 2, 0);
		p.spawnParticle(Particle.NOTE, effectLocation, 1, 0, 0, 0, 1);
	}

	private Item buildHoverItem(ItemStack item) {
		ItemTag tag = createItemTag(item);
		return new Item(item.getType().getKey().toString(), item.getAmount(), tag);
	}

	private ItemTag createItemTag(ItemStack item) {
		if (!item.hasItemMeta())
			return null;
		ItemMeta meta = item.getItemMeta();
		if (meta == null)
			return null;
		try {
			String tag = meta.getAsString();
			if (tag == null || tag.isEmpty())
				return null;
			return ItemTag.ofNbt(tag);
		}
		catch (RuntimeException ignored) {
			return null;
		}
	}
}
