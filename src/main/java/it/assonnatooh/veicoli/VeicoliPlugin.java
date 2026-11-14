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
                "vehicles." + vehicleConfigKey(vehicleName) + ".body-custom-model-data", 22));
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
            spawned.setCustomName(ChatColor*
