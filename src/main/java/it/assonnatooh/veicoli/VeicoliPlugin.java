package it.assonnatooh.veicoli;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class VeicoliPlugin extends JavaPlugin implements Listener {
    private static final int[] GEAR_LIMITS = {15, 30, 55, 75, 110};
    private NamespacedKey vehicleKey;
    private NamespacedKey fuelKey;
    private NamespacedKey gearKey;
    private NamespacedKey wheelKey;
    private NamespacedKey wheelParentKey;
    private final Map<UUID, List<UUID>> vehicleWheels = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        vehicleKey = new NamespacedKey(this, "vehicle");
        fuelKey = new NamespacedKey(this, "fuel");
        gearKey = new NamespacedKey(this, "gear");
        wheelKey = new NamespacedKey(this, "vehicle_wheel");
        wheelParentKey = new NamespacedKey(this, "wheel_parent");
        Bukkit.getPluginManager().registerEvents(this, this);
        startVehicleTask();
        getLogger().info("VeicoliPlugin Minecraft attivo.");
    }

    @Override
    public void onDisable() {
        for (List<UUID> wheels : vehicleWheels.values()) {
            for (UUID wheelId : wheels) {
                Entity wheel = Bukkit.getEntity(wheelId);
                if (wheel != null) wheel.remove();
            }
        }
        vehicleWheels.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("garage")) return handleGarage(sender, args);
        if (command.getName().equalsIgnoreCase("adminveicoli")) return handleAdmin(sender, args);
        return false;
    }

    private boolean handleGarage(CommandSender sender, String[] args) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage("Questo comando può essere usato solo in gioco.");
            return true;
        }
        Player target = viewer;
        if (args.length > 0) {
            if (!viewer.hasPermission("veicoliplugin.admin")) {
                viewer.sendMessage(ChatColor.RED + "Non hai il permesso per aprire il garage di altri giocatori.");
                return true;
            }
            target = findOnlinePlayer(args[0]);
            if (target == null) {
                viewer.sendMessage(ChatColor.RED + "Giocatore non trovato o non online.");
                return true;
            }
        }
        openGarage(viewer, target.getUniqueId(), target.getName());
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("veicoliplugin.admin")) {
            sender.sendMessage(ChatColor.RED + "Non hai il permesso.");
            return true;
        }
        if (args.length < 3 || !(args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("remove"))) {
            sender.sendMessage(ChatColor.YELLOW + "Uso: /adminveicoli <give|remove> <nick> <nome veicolo>");
            return true;
        }
        Player target = findOnlinePlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Giocatore non trovato o non online.");
            return true;
        }
        String vehicleName = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim();
        if (vehicleName.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Specifica il nome del veicolo.");
            return true;
        }
        List<String> garage = getGarage(target.getUniqueId());
        if (args[0].equalsIgnoreCase("give")) {
            garage.add(vehicleName);
            setGarage(target.getUniqueId(), garage);
            sender.sendMessage(ChatColor.GREEN + "Aggiunto " + vehicleName + " al garage di " + target.getName() + ".");
            target.sendMessage(ChatColor.GREEN + "Hai ricevuto un veicolo: " + vehicleName + ". Usa /garage.");
        } else {
            boolean removed = garage.removeIf(name -> name.equalsIgnoreCase(vehicleName));
            setGarage(target.getUniqueId(), garage);
            sender.sendMessage(removed
                    ? ChatColor.GREEN + "Rimosso " + vehicleName + " dal garage di " + target.getName() + "."
                    : ChatColor.RED + "Quel veicolo non è nel garage del giocatore.");
            refreshOpenGarages(target.getUniqueId());
        }
        return true;
    }

    private Player findOnlinePlayer(String name) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name)) return player;
        }
        return null;
    }

    private List<String> getGarage(UUID owner) {
        return new ArrayList<>(getConfig().getStringList("garages." + owner));
    }

    private void setGarage(UUID owner, List<String> vehicles) {
        getConfig().set("garages." + owner, vehicles);
        saveConfig();
    }

    private void openGarage(Player viewer, UUID owner, String ownerName) {
        List<String> vehicles = getGarage(owner);
        int size = Math.max(9, Math.min(54, ((vehicles.size() + 8) / 9) * 9));
        GarageHolder holder = new GarageHolder(owner);
        Inventory inventory = Bukkit.createInventory(holder, size, ChatColor.DARK_GRAY + "Garage: " + ownerName);
        holder.inventory = inventory;
        for (int i = 0; i < vehicles.size() && i < size; i++) {
            inventory.setItem(i, createGarageItem(vehicles.get(i)));
        }
        if (vehicles.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "Garage vuoto");
            empty.setItemMeta(meta);
            inventory.setItem(4, empty);
        }
        viewer.openInventory(inventory);
    }

    private ItemStack createGarageItem(String vehicleName) {
        ItemStack item = new ItemStack(Material.LEATHER_HORSE_ARMOR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + vehicleName);
        meta.setLore(List.of(
                ChatColor.GRAY + "Click sinistro: fai apparire il veicolo",
                ChatColor.GRAY + "Lo staff può rimuoverlo con click destro"
        ));
        meta.setCustomModelData(getConfig().getInt(
                "vehicles." + vehicleConfigKey(vehicleName) + ".body-custom-model-data", 20));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onGarageClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GarageHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;
        List<String> vehicles = getGarage(holder.owner);
        if (slot >= vehicles.size()) return;
        String vehicle = vehicles.get(slot);
        if (event.isRightClick()) {
            if (!viewer.hasPermission("veicoliplugin.admin")) {
                viewer.sendMessage(ChatColor.RED + "Solo lo staff può rimuovere veicoli da questa GUI.");
                return;
            }
            vehicles.remove(slot);
            setGarage(holder.owner, vehicles);
            viewer.sendMessage(ChatColor.GREEN + "Rimosso " + vehicle + " dal garage.");
            refreshOpenGarages(holder.owner);
            if (viewer.getOpenInventory().getTopInventory().getHolder() instanceof GarageHolder) {
                openGarageForUuid(viewer, holder.owner);
            }
            return;
        }
        if (!viewer.getUniqueId().equals(holder.owner)) {
            viewer.sendMessage(ChatColor.YELLOW + "Garage in sola consultazione: usa click destro per rimuovere (staff).");
            return;
        }
        spawnVehicle(viewer, vehicle);
    }

    private void refreshOpenGarages(UUID ownerId) {
        String ownerName = Bukkit.getOfflinePlayer(ownerId).getName();
        if (ownerName == null) ownerName = "Giocatore";
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof GarageHolder holder
                    && holder.owner.equals(ownerId)) {
                openGarage(player, ownerId, ownerName);
            }
        }
    }

    private void openGarageForUuid(Player viewer, UUID owner) {
        String ownerName = Bukkit.getOfflinePlayer(owner).getName();
        openGarage(viewer, owner, ownerName == null ? "Giocatore" : ownerName);
    }

    private void spawnVehicle(Player player, String vehicleName) {
        Entity current = player.getVehicle();
        if (isPluginVehicle(current)) removeVehicleAndWheels((Horse) current);
        Horse horse = player.getWorld().spawn(player.getLocation(), Horse.class, spawned -> {
            spawned.setAdult();
            spawned.setTamed(true);
            spawned.setOwner(player);
            spawned.setInvulnerable(true);
            spawned.setPersistent(true);
            // Il cavallo è solo un supporto invisibile per guidare il veicolo.
            spawned.setInvisible(true);
            spawned.setCustomName(ChatColor.GOLD + vehicleName);
            spawned.setCustomNameVisible(false);
            spawned.getPersistentDataContainer().set(vehicleKey, PersistentDataType.STRING, vehicleName);
            spawned.getPersistentDataContainer().set(fuelKey, PersistentDataType.INTEGER, 100);
            spawned.getPersistentDataContainer().set(gearKey, PersistentDataType.INTEGER, 1);
            if (spawned.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
                spawned.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.30);
            }
            HorseInventory inventory = spawned.getInventory();
            inventory.setSaddle(new ItemStack(Material.SADDLE));
            ItemStack armor = new ItemStack(Material.LEATHER_HORSE_ARMOR);
            ItemMeta meta = armor.getItemMeta();
            meta.setCustomModelData(getConfig().getInt(
                    "vehicles." + vehicleConfigKey(vehicleName) + ".body-custom-model-data", 20));
            meta.setDisplayName(ChatColor.GOLD + vehicleName);
            armor.setItemMeta(meta);
            inventory.setArmor(armor);
        });
        createWheelDisplays(horse);
        horse.addPassenger(player);
        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + vehicleName + " è pronto. Premi F per cambiare marcia.");
    }

    private void createWheelDisplays(Horse horse) {
        removeWheelDisplays(horse.getUniqueId());
        String vehicleName = horse.getPersistentDataContainer()
                .getOrDefault(vehicleKey, PersistentDataType.STRING, "AMG Cross");
        int wheelCmd = getConfig().getInt(
                "vehicles." + vehicleConfigKey(vehicleName) + ".wheel-custom-model-data", 240);
        ItemStack wheelItem = new ItemStack(Material.LEATHER_HORSE_ARMOR);
        ItemMeta meta = wheelItem.getItemMeta();
        meta.setCustomModelData(wheelCmd);
        meta.setDisplayName(ChatColor.GOLD + "Ruota veicolo");
        wheelItem.setItemMeta(meta);

        double offset = getConfig().getDouble("vehicles.amg-cross.wheel-side-offset", 0.42);
        List<UUID> wheels = new ArrayList<>(2);
        for (int side : new int[]{-1, 1}) {
            ItemDisplay display = horse.getWorld().spawn(horse.getLocation(), ItemDisplay.class, spawned -> {
                spawned.setItemStack(wheelItem);
                spawned.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                spawned.setGravity(false);
                spawned.setInvulnerable(true);
                spawned.setPersistent(false);
                spawned.getPersistentDataContainer().set(wheelKey, PersistentDataType.BYTE, (byte) 1);
                spawned.getPersistentDataContainer().set(wheelParentKey, PersistentDataType.STRING,
                        horse.getUniqueId().toString());
            });
            wheels.add(display.getUniqueId());
        }
        vehicleWheels.put(horse.getUniqueId(), wheels);
        updateWheelPositions(horse, offset);
    }

    private void updateWheelPositions(Horse horse, double offset) {
        List<UUID> wheels = vehicleWheels.get(horse.getUniqueId());
        if (wheels == null || wheels.size() != 2) {
            createWheelDisplays(horse);
            return;
        }
        float yaw = horse.getLocation().getYaw();
        double radians = Math.toRadians(yaw);
        double rightX = Math.cos(radians);
        double rightZ = -Math.sin(radians);
        double[] sides = {-1.0, 1.0};
        for (int index = 0; index < 2; index++) {
            Entity wheel = Bukkit.getEntity(wheels.get(index));
            if (!(wheel instanceof ItemDisplay display) || !display.isValid()) {
                createWheelDisplays(horse);
                return;
            }
            double side = sides[index] * offset;
            display.teleport(horse.getLocation().clone().add(rightX * side, -0.55, rightZ * side));
        }
    }

    private void removeWheelDisplays(UUID horseId) {
        List<UUID> wheels = vehicleWheels.remove(horseId);
        if (wheels == null) return;
        for (UUID wheelId : wheels) {
            Entity wheel = Bukkit.getEntity(wheelId);
            if (wheel != null) wheel.remove();
        }
    }

    private void removeVehicleAndWheels(Horse horse) {
        removeWheelDisplays(horse.getUniqueId());
        horse.remove();
    }

    private boolean isPluginVehicle(Entity entity) {
        return entity instanceof Horse horse
                && horse.getPersistentDataContainer().has(vehicleKey, PersistentDataType.STRING);
    }

    @EventHandler
    public void onGearKey(PlayerSwapHandItemsEvent event) {
        Entity vehicle = event.getPlayer().getVehicle();
        if (!isPluginVehicle(vehicle)) return;
        event.setCancelled(true);
        Horse horse = (Horse) vehicle;
        int gear = horse.getPersistentDataContainer().getOrDefault(gearKey, PersistentDataType.INTEGER, 1);
        gear = gear >= 5 ? 1 : gear + 1;
        horse.getPersistentDataContainer().set(gearKey, PersistentDataType.INTEGER, gear);
        event.getPlayer().sendActionBar(hudText(horse, gear));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Entity vehicle = event.getPlayer().getVehicle();
        if (isPluginVehicle(vehicle)) removeVehicleAndWheels((Horse) vehicle);
    }

    private void startVehicleTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Entity entity = player.getVehicle();
                    if (!isPluginVehicle(entity)) continue;
                    Horse horse = (Horse) entity;
                    int gear = horse.getPersistentDataContainer().getOrDefault(gearKey, PersistentDataType.INTEGER, 1);
                    int fuel = horse.getPersistentDataContainer().getOrDefault(fuelKey, PersistentDataType.INTEGER, 100);
                    updateWheelPositions(horse, getConfig().getDouble("vehicles.amg-cross.wheel-side-offset", 0.42));
                    Vector velocity = horse.getVelocity();
                    double speedKmh = velocity.clone().setY(0).length() * 72.0;
                    double maxKmh = GEAR_LIMITS[Math.max(0, Math.min(4, gear - 1))];
                    if (speedKmh > maxKmh) {
                        Vector horizontal = velocity.clone().setY(0).normalize().multiply(maxKmh / 72.0);
                        horse.setVelocity(new Vector(horizontal.getX(), velocity.getY(), horizontal.getZ()));
                        speedKmh = maxKmh;
                    }
                    if (fuel > 0 && horse.getTicksLived() % 1200 == 0) {
                        fuel--;
                        horse.getPersistentDataContainer().set(fuelKey, PersistentDataType.INTEGER, fuel);
                    }
                    if (fuel <= 0) {
                        Vector stopped = horse.getVelocity();
                        horse.setVelocity(new Vector(stopped.getX() * 0.6, stopped.getY(), stopped.getZ() * 0.6));
                    }
                    player.sendActionBar(ChatColor.WHITE + "Velocità: " + Math.round(speedKmh) + " km/h"
                            + ChatColor.GRAY + " | " + ChatColor.WHITE + "Carburante: " + fuel + "%"
                            + ChatColor.GRAY + " | " + ChatColor.WHITE + "Marcia: " + gear);
                }
            }
        }.runTaskTimer(this, 1L, 1L);
    }

    private String hudText(Horse horse, int gear) {
        int fuel = horse.getPersistentDataContainer().getOrDefault(fuelKey, PersistentDataType.INTEGER, 100);
        int speed = (int) Math.round(horse.getVelocity().clone().setY(0).length() * 72.0);
        return ChatColor.WHITE + "Velocità: " + speed + " km/h | Carburante: " + fuel + "% | Marcia: " + gear;
    }

    private String vehicleConfigKey(String vehicleName) {
        return vehicleName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private static final class GarageHolder implements InventoryHolder {
        private final UUID owner;
        private Inventory inventory;

        private GarageHolder(UUID owner) {
            this.owner = owner;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
