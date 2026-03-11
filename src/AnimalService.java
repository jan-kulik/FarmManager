import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class AnimalService {

    private final AnimalRepository repository;
    private final List<Animal> animals;
    private ItemCatalog catalog;

    public AnimalService(AnimalRepository repository) {
        this.repository = repository;
        this.animals = new ArrayList<>();
        this.repository.loadOrCreate(this.animals);
        sortById();
    }

    public void setCatalog(ItemCatalog catalog) {
        this.catalog = catalog;
    }

    public List<Animal> getAnimals() {
        return animals;
    }

    public Animal getById(int id) {
        for (Animal a : animals) {
            if (a.getId() == id) return a;
        }
        return null;
    }

    public Animal create(AnimalType type, String name) {
        int id = repository.createId();
        Animal animal = createInstance(type, id, name);
        animals.add(animal);
        sortById();
        repository.save(animals);
        return animal;
    }

    public boolean deleteById(int id) {
        for (int i = 0; i < animals.size(); i++) {
            if (animals.get(i).getId() == id) {
                animals.remove(i);
                repository.save(animals);
                return true;
            }
        }
        return false;
    }

    public boolean rename(int id, String newName) {
        Animal a = getById(id);
        if (a == null) return false;
        a.setName(newName);
        repository.save(animals);
        return true;
    }

    public boolean feed(int id, int amount) {
        Animal a = getById(id);
        if (a == null) return false;
        a.feed(amount);
        repository.save(animals);
        return true;
    }

    public void endDayAll(Inventory inventory) {
        for (Animal a : animals) {
            a.endOfDay(inventory);
        }
        repository.save(animals);
    }

    public void save() {
        repository.save(animals);
    }

    private Animal createInstance(AnimalType type, int id, String name) {
        if (type == null) throw new IllegalArgumentException("type is null");

        switch (type) {
            case CHICKEN:
                return new Chicken(id, name);
            case COW:
                return new Cow(id, name);
            case PIG:
                return new Pig(id, name);
            case SHEEP:
                return new Sheep(id, name);
            case BEE:
                return new Bee(id, name);
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    private void sortById() {
        animals.sort(Comparator.comparingInt(Animal::getId));
    }
    // === Tier-Menü === \\

    private static final int PAGE_SIZE = 15;

    // Menu dartstellung aller Tiere
    public void openBrowser(Scanner sc, Inventory inventory, InventoryRepository repo) {
        int page = 0;
        while (true) {
            List<Animal> all = getAnimals();
            all.sort(Comparator.comparing((Animal a) -> a.getType().name()).thenComparing(Animal::getName));

            if (all.isEmpty()) {
                System.out.println("Keine Tiere vorhanden.");
                return;
            }

            int totalPages = (int) Math.ceil((double) all.size() / PAGE_SIZE);
            int from = page * PAGE_SIZE;
            int to = Math.min(from + PAGE_SIZE, all.size());

            System.out.println("=== Tiere (Seite " + (page + 1) + "/" + totalPages + ") ===");
            System.out.println("Anzahl: " + all.size());
            System.out.println(" ");

            String lastType = "";
            for (int i = from; i < to; i++) {
                Animal a = all.get(i);
                String type = a.getType().name();
                if (!type.equals(lastType)) {
                    System.out.println("-- " + type + " --");
                    lastType = type;
                }
                String extra;
                if (a instanceof Bee) {
                    extra = "Volk: " + ((Bee) a).getColonySize() + " Bienen";
                } else {
                    extra = "Hunger: " + Animal.hungerBar(a.getHunger()) + " (" + a.getHunger() + ")";
                }
                System.out.println((i - from + 1) + ") " + a.getName()
                        + "  |  Alter: " + a.getAgeDays() + " Tage  |  " + extra);
            }

            System.out.println(" ");
            if (totalPages > 1) System.out.println("N = Nächste Seite   V = Vorherige Seite");
            System.out.println("Nummer eingeben um Tier auszuwählen");
            System.out.println("0 = Zurück zum Hauptmenü");
            System.out.print("Auswahl: ");
            String input = sc.nextLine().trim();

            if (input.equals("0")) return;
            if (input.equalsIgnoreCase("N")) {
                if (page < totalPages - 1) page++;
                else System.out.println("Bereits auf der letzten Seite.");
                continue;
            }
            if (input.equalsIgnoreCase("V")) {
                if (page > 0) page--;
                else System.out.println("Bereits auf der ersten Seite.");
                continue;
            }

            int choice;
            try { choice = Integer.parseInt(input); }
            catch (NumberFormatException e) { System.out.println("Bitte Zahl eingeben."); continue; }

            int index = from + choice - 1;
            if (choice < 1 || index >= to) { System.out.println("Ungültige Auswahl."); continue; }

            openDetail(sc, all.get(index), inventory, repo);
        }
    }

    // Ist ein Tier ausgewählt, öffnet dieses Menü mit Details und Aktionen (Füttern, Umbenennen, Löschen).
    private void openDetail(Scanner sc, Animal animal, Inventory inventory, InventoryRepository repo) {
        while (true) {
            System.out.println(" ");
            System.out.println("=== " + animal.getName() + " (" + animal.getType() + ") ===");
            System.out.println("ID: " + animal.getId()
                    + "  |  Alter: " + animal.getAgeDays() + " Tage");
            if (animal instanceof Bee) {
                System.out.println("Volk: " + ((Bee) animal).getColonySize() + " Bienen");
                System.out.println("Honig-Produktion: alle 30 Tage automatisch");
            } else {
                System.out.println("Hunger: " + Animal.hungerBar(animal.getHunger())
                        + " (" + animal.getHunger() + "/100)");
            }
            System.out.println(" ");
            if (animal instanceof Bee) {
                System.out.println("1) Volksgröße ändern");
            } else {
                System.out.println("1) Füttern");
            }
            System.out.println("2) Umbenennen");
            System.out.println("3) Löschen");
            System.out.println("0) Zurück");
            System.out.print("Auswahl: ");
            String input = sc.nextLine().trim();

            if (input.equals("0")) return;
            if (input.equals("1")) {
                if (animal instanceof Bee) {
                    System.out.print("Neue Volksgröße: ");
                    try {
                        int size = Integer.parseInt(sc.nextLine().trim());
                        ((Bee) animal).setColonySize(size);
                        save();
                        System.out.println("Volksgröße aktualisiert.");
                    } catch (NumberFormatException e) {
                        System.out.println("Bitte eine Zahl eingeben.");
                    }
                } else {
                    openFeedMenu(sc, animal, inventory, repo);
                }
            } else if (input.equals("2")) {
                System.out.print("Neuer Name: ");
                String newName = sc.nextLine().trim();
                if (newName.isEmpty()) {
                    System.out.println("Name darf nicht leer sein.");
                } else {
                    rename(animal.getId(), newName);
                    System.out.println("Umbenannt zu: " + animal.getName());
                }
            } else if (input.equals("3")) {
                System.out.println("Wirklich löschen? Ja oder Nein?");
                if (sc.nextLine().trim().equalsIgnoreCase("ja")) {
                    deleteById(animal.getId());
                    System.out.println("Tier entfernt.");
                    return;
                }
                System.out.println("Löschen abgebrochen.");
            } else {
                System.out.println("Ungültige Auswahl.");
            }
        }
    }

    // Das Fütterungsmenü zeigt nur die erlaubten Futter an, die im Lager sind.
    private void openFeedMenu(Scanner sc, Animal animal, Inventory inventory, InventoryRepository repo) {
        String[] allowed = animal.getAllowedFeedItems();
        if (allowed.length == 0) {
            System.out.println("Dieses Tier benötigt kein Futter.");
            return;
        }

        System.out.println("=== Füttern: " + animal.getName() + " ===");

        String[] available = new String[allowed.length];
        int count = 0;
        for (int i = 0; i < allowed.length; i++) {
            int amount = inventory.getAmount(allowed[i]);
            if (amount > 0) {
                String displayName = inventory.getDisplayName(allowed[i]);
                System.out.println((count + 1) + ") " + displayName
                        + "  +" + animal.getFeedHungerValue(allowed[i]) + " Hunger"
                        + "  Vorrat: " + amount);
                available[count] = allowed[i];
                count++;
            }
        }

        if (count == 0) {
            System.out.println("Kein passendes Futter im Lager vorhanden.");
            System.out.print("Erlaubte Futtermittel:");
            for (String a : allowed) System.out.print("  " + inventory.getDisplayName(a));
            System.out.println();
            return;
        }

        System.out.println("0) Abbrechen");
        System.out.print("Auswahl: ");
        int choice;
        try { choice = Integer.parseInt(sc.nextLine().trim()); }
        catch (NumberFormatException e) { System.out.println("Ungültige Eingabe."); return; }

        if (choice == 0) return;
        if (choice < 1 || choice > count) { System.out.println("Ungültige Auswahl."); return; }

        boolean ok = animal.feedWithItem(available[choice - 1], inventory);
        if (ok) {
            repo.save(inventory);
            save();
            System.out.println(animal.getName() + " gefüttert mit "
                    + inventory.getDisplayName(available[choice - 1])
                    + ". Hunger jetzt: " + animal.getHunger());
        } else {
            System.out.println("Füttern fehlgeschlagen.");
        }
    }
}
