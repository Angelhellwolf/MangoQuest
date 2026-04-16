package me.Cutiemango.MangoQuest.manager;

public class MythicMobInfo
{
	private final String internalName;
	private final String displayName;
	private final String entityType;

	public MythicMobInfo(String internalName, String displayName, String entityType) {
		this.internalName = internalName;
		this.displayName = displayName;
		this.entityType = entityType;
	}

	public String getInternalName() {
		return internalName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getEntityType() {
		return entityType;
	}
}
