package com.chaeulso.punish.model;

import java.util.UUID;

/**
 * Model class encapsulating all metadata of a punishment record.
 */
public class Punishment {
    public enum Type {
        BAN, KICK, MUTE, FREEZE, JAIL, WARN
    }

    private final String punishmentId;
    private final UUID uuid;
    private final String username;
    private final UUID staffUuid;
    private final String staffName;
    private final String reason;
    private final Type type;
    private final long issued;
    private final long expires;
    private final String timezone;
    private final String server;

    private boolean active = true;
    private boolean removed = false;
    private String removedBy;
    private String removedReason;
    private long removedDate;

    public Punishment(String punishmentId, UUID uuid, String username, UUID staffUuid, String staffName,
                      String reason, Type type, long issued, long expires, String timezone, String server) {
        this.punishmentId = punishmentId;
        this.uuid = uuid;
        this.username = username;
        this.staffUuid = staffUuid;
        this.staffName = staffName;
        this.reason = reason;
        this.type = type;
        this.issued = issued;
        this.expires = expires;
        this.timezone = timezone;
        this.server = server;
    }

    public String getPunishmentId() { return punishmentId; }
    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }
    public UUID getStaffUuid() { return staffUuid; }
    public String getStaffName() { return staffName; }
    public String getReason() { return reason; }
    public Type getType() { return type; }
    public long getIssued() { return issued; }
    public long getExpires() { return expires; }
    public String getTimezone() { return timezone; }
    public String getServer() { return server; }

    public boolean isActive() {
        if (expires > 0 && System.currentTimeMillis() > expires) {
            active = false;
        }
        return active;
    }

    public void setActive(boolean active) { this.active = active; }
    public boolean isRemoved() { return removed; }
    public void setRemoved(boolean removed) { this.removed = removed; }
    public String getRemovedBy() { return removedBy; }
    public void setRemovedBy(String removedBy) { this.removedBy = removedBy; }
    public String getRemovedReason() { return removedReason; }
    public void setRemovedReason(String removedReason) { this.removedReason = removedReason; }
    public long getRemovedDate() { return removedDate; }
    public void setRemovedDate(long removedDate) { this.removedDate = removedDate; }

    public boolean isPermanent() {
        return expires <= 0;
    }
}
