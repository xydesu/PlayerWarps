package dev.revivalo.playerwarps.warp;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.category.Category;
import dev.revivalo.playerwarps.category.CategoryManager;
import dev.revivalo.playerwarps.util.ItemUtil;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class Warp implements ConfigurationSerializable {
    private UUID warpID;
    private UUID owner;
    private String ownerName;
    private WarpStatus status;
    private boolean verificationNeeded;
    private String name;
    private String displayName;
    private String password;
    private String description;
    private String stars;
    private Location location;
    private String worldName;
    private int rating;
    private int visits;
    private int todayVisits;
    private int admission;
    private long dateCreated;
    private long lastActivity;
    private long featuredTimestamp;
    private Set<UUID> reviewers;
    private Set<UUID> blockedPlayers = new HashSet<>();
    private Category category;
    private ItemStack menuItem;

    public Warp(Map<String, Object> map) {
        if (map.containsKey("owner-id")) {
            setOwner(UUID.fromString((String) map.get("owner-id")));
        }

        for (String key : map.keySet()) {
            final Object value = map.get(key);
            switch (key){
                case "uuid": warpID = UUID.fromString((String) value);
                case "name": setName((String) value); break;
                case "display-name": setDisplayName((String) value); break;
                case "owner-id": setOwner(UUID.fromString((String) value)); break;
                case "need-verification": setVerificationNeeded((boolean) value); break;
                case "loc":
                    setLocation(parseLocation(value));
                    break;
                case "lore": setDescription((String) value); break;
                case "type": // category in old versions
                case "category": setCategory(CategoryManager.getCategoryFromName((String) value)); break;
                case "item":
                    if (value instanceof String) {
                        org.bukkit.OfflinePlayer offlinePlayer = owner != null ? PlayerWarpsPlugin.get().getServer().getOfflinePlayer(owner) : null;
                        setMenuItem(ItemUtil.getItem((String) value, offlinePlayer).build());
                    } else {
                        setMenuItem((ItemStack) value);
                    }
                    break;
                case "ratings": setRating((int) value); break;
                case "reviewers": setReviewers(((List<String>) value).stream().map(UUID::fromString).collect(Collectors.toCollection(HashSet::new))); break;
                case "blocked-players": setBlockedPlayers(((List<String>) value).stream().map(UUID::fromString).collect(Collectors.toCollection(HashSet::new))); break;
                case "visits": setVisits((int) value); break;
                case "status": setStatus(WarpStatus.valueOf((String) value)); break;
                case "password": setPassword(String.valueOf(value)); break;
                case "admission": setAdmission(Integer.parseInt(String.valueOf(value))); break;
                case "date-created": setDateCreated(Long.parseLong(String.valueOf(value))); break;
                case "last-activity": setLastActivity(Long.parseLong(String.valueOf(value))); break;
                case "featured": setFeaturedTimestamp(Long.parseLong(String.valueOf(value))); break;
            }

            if (owner != null) {
                OfflinePlayer offlinePlayer = PlayerWarpsPlugin.get().getServer().getOfflinePlayer(owner);
                setOwnerName(offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown");
            } else {
                setOwnerName("Unknown");
            }
        }

        if (reviewers != null) stars = TextUtil.createRatingFormat(this);
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        return new HashMap<String, Object>() {{
            put("uuid", getWarpID().toString());
            put("name", getName());
            put("display-name", getDisplayName());
            put("owner-id", getOwner().toString());
            if (getLocation() != null) {
                put("loc", new HashMap<String, Object>() {{
                    String wName = (getLocation().getWorld() != null) ? getLocation().getWorld().getName() : Warp.this.worldName;
                    if (wName == null) wName = "unknown";
                    put("world", wName);
                    put("x", getLocation().getX());
                    put("y", getLocation().getY());
                    put("z", getLocation().getZ());
                    put("yaw", getLocation().getYaw());
                    put("pitch", getLocation().getPitch());
                }});
            }
            put("lore", getDescription());
            put("item", getMenuItem());
            put("ratings", getRating());
            put("need-verification", isVerificationNeeded());
            put("reviewers", getReviewers().stream().map(UUID::toString).collect(Collectors.toList()));
            put("blocked-players", getBlockedPlayers().stream().map(UUID::toString).collect(Collectors.toList()));
            put("category", getCategory() == null ? "all" : getCategory().getType());
            put("password", getPassword());
            put("visits", getVisits());
            put("status", getStatus().name());
            put("admission", getAdmission());
            put("date-created", getDateCreated());
            put("last-activity", getLastActivity());
            put("featured", getFeaturedTimestamp());
        }};
    }

//    public SkullBuilder getItem() {
//        if (tempItem == null) {
//            PlayerWarpsPlugin.get().getLogger().info("Temp item");
//            tempItem = ItemBuilder.skull().owner(PlayerWarpsPlugin.get().getServer().getOfflinePlayer(owner));
//            //tempItem = ItemUtil.getItem(Config.DEFAULT_WARP_ITEM.asString(), owner);
//        }
//
//        return tempItem;
//    }

    public float getConvertedRating() {
        return (float) getRating() / getReviewers().size();
    }

    public boolean isPasswordProtected(){
        return getStatus() == WarpStatus.PASSWORD_PROTECTED;
    }

    public boolean isVerified() {
        return !verificationNeeded;
    }

    public boolean isAccessible(){
        return isVerified() && getStatus() != WarpStatus.CLOSED;
    }

    public boolean canManage(Player player){
        return PermissionUtil.hasPermission(player, PermissionUtil.Permission.MANAGE_OTHERS)
            || Objects.equals(player.getUniqueId(), getOwner());
    }

    public boolean isBlocked(UUID playerUuid) {
        return blockedPlayers.contains(playerUuid);
    }

    public void block(OfflinePlayer player) {
        blockedPlayers.add(player.getUniqueId());
    }

    public void unblock(OfflinePlayer player) {
        blockedPlayers.remove(player.getUniqueId());
    }

    public boolean isBlocked(OfflinePlayer player) {
        return isBlocked(player.getUniqueId());
    }

    public boolean isOwner(Player player) {
        return Objects.equals(owner, player.getUniqueId());
    }

    public UUID getWarpID() {
        return warpID;
    }

    public void setWarpID(UUID warpID) {
        this.warpID = warpID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.displayName = name;
    }

    public String getDisplayName() {
        return displayName == null ? name : TextUtil.colorize(displayName);
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPassword() {
        return password;
    }

    public boolean validatePassword(String passwordToValidate) {
        return this.password.equals(passwordToValidate);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        if (location == null) {
            return;
        }

        if (location.getWorld() == null) {
            return;
        }

        if (PlayerWarpsPlugin.get().getServer().getWorld(location.getWorld().getName()) == null) {
            return;
        }

        this.location = location;
    }

    public int getRating() {
        return rating;
    }

    public double getReview() {
        return (double) rating / (getReviewers().isEmpty() ? 1 : getReviewers().size());
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public Set<UUID> getReviewers() {
        return reviewers;
    }

    public void setReviewers(Set<UUID> reviewers) {
        this.reviewers = reviewers;
    }

    public Set<UUID> getBlockedPlayers() {
        return blockedPlayers == null ? Collections.emptySet() : blockedPlayers;
    }

    public void setBlockedPlayers(Set<UUID> blockedPlayers) {
        this.blockedPlayers = blockedPlayers;
    }

    public int getVisits() {
        return visits;
    }

    public void setVisits(int visits) {
        this.visits = visits;
    }

    public int getTodayVisits() {
        return todayVisits;
    }

    public void setTodayVisits(int todayVisits) {
        this.todayVisits = todayVisits;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public boolean hasAdmission() {
        return admission > 0;
    }

    public int getAdmission() {
        return admission;
    }

    public void setAdmission(int admission) {
        this.admission = admission;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WarpStatus getStatus() {
        return status;
    }

    public void setStatus(WarpStatus status) {
        this.status = status;
    }

    public long getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(long dateCreated) {
        this.dateCreated = dateCreated;
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }

    public ItemStack getMenuItem() {
        return menuItem;
    }

    public void setMenuItem(ItemStack menuItem) {
        this.menuItem = menuItem;
    }

    public String getStars() {
        return stars;
    }

    public void setStars(String stars) {
        this.stars = stars;
    }

    @Override
    public String toString(){return warpID.toString();}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Warp warp = (Warp) o;
        return Objects.equals(warpID, warp.warpID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(warpID);
    }

    public boolean isVerificationNeeded() {
        return verificationNeeded;
    }

    public void setVerificationNeeded(boolean verificationNeeded) {
        this.verificationNeeded = verificationNeeded;
    }

    public long getFeaturedTimestamp() {
        return featuredTimestamp;
    }

    public void setFeaturedTimestamp(long featuredTimestamp) {
        this.featuredTimestamp = featuredTimestamp;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    private Location parseLocation(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof Location) {
            return (Location) value;
        }

        Map<String, Object> mapLoc = null;
        if (value instanceof org.bukkit.configuration.ConfigurationSection) {
            mapLoc = ((org.bukkit.configuration.ConfigurationSection) value).getValues(false);
        } else if (value instanceof Map) {
            mapLoc = (Map<String, Object>) value;
        }

        if (mapLoc != null) {
            org.bukkit.World world = null;
            String parsedWorldName = null;
            
            if (mapLoc.containsKey("world") && mapLoc.get("world") != null) {
                parsedWorldName = String.valueOf(mapLoc.get("world"));
                world = PlayerWarpsPlugin.get().getServer().getWorld(parsedWorldName);
            } 
            if (world == null && mapLoc.containsKey("world-uid") && mapLoc.get("world-uid") != null) {
                try {
                    world = PlayerWarpsPlugin.get().getServer().getWorld(UUID.fromString(String.valueOf(mapLoc.get("world-uid"))));
                    if (world != null) {
                        parsedWorldName = world.getName();
                    }
                } catch (Exception ignored) {}
            }
            if (world == null && mapLoc.containsKey("world_key") && mapLoc.get("world_key") != null) {
                String wk = String.valueOf(mapLoc.get("world_key"));
                if (parsedWorldName == null) {
                    parsedWorldName = wk.contains(":") ? wk.split(":")[1] : wk;
                }
                world = PlayerWarpsPlugin.get().getServer().getWorld(wk);
                if (world == null && wk.contains(":")) {
                    world = PlayerWarpsPlugin.get().getServer().getWorld(wk.split(":")[1]);
                }
            }

            this.worldName = parsedWorldName;

            double x = mapLoc.containsKey("x") ? ((Number) mapLoc.get("x")).doubleValue() : 0;
            double y = mapLoc.containsKey("y") ? ((Number) mapLoc.get("y")).doubleValue() : 0;
            double z = mapLoc.containsKey("z") ? ((Number) mapLoc.get("z")).doubleValue() : 0;
            float yaw = mapLoc.containsKey("yaw") ? ((Number) mapLoc.get("yaw")).floatValue() : 0;
            float pitch = mapLoc.containsKey("pitch") ? ((Number) mapLoc.get("pitch")).floatValue() : 0;
            Location loc = new Location(world, x, y, z, yaw, pitch);
            return loc;
        }
        return null;
    }
}