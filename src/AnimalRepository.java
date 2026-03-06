import java.io.*;
import java.util.*;

public class AnimalRepository {

    private final String filePath;
    private final DataStore dataStore;

    public AnimalRepository(String filePath, DataStore dataStore) {
        this.filePath = filePath;
        this.dataStore = dataStore;
    }

    public void loadOrCreate(List<Animal> animals) {
        animals.clear();

        File f = new File(filePath);
        if (!f.exists()) {
            save(animals);
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                String t = line.trim();
                if (t.isEmpty()) continue;

                if (firstLine && t.toLowerCase().startsWith("id,")) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;

                String[] parts = t.split(",", -1);
                if (parts.length < 6) continue;

                int id;
                int ageDays;
                int hunger;
                int counter;

                try {
                    id = Integer.parseInt(parts[0].trim());
                    ageDays = Integer.parseInt(parts[3].trim());
                    hunger = Integer.parseInt(parts[4].trim());
                    counter = Integer.parseInt(parts[5].trim());
                } catch (NumberFormatException e) {
                    continue;
                }

                String typeRaw = parts[1].trim();
                String name = parts[2].trim();

                AnimalType type;
                try {
                    type = AnimalType.valueOf(typeRaw);
                } catch (IllegalArgumentException e) {
                    continue;
                }

                Animal animal = createFromType(type, id, name, ageDays, hunger, counter);
                if (animal != null) {
                    animals.add(animal);
                }
            }
        } catch (IOException ignored) {
        }

        int maxId = 0;
        for (Animal a : animals) {
            if (a.getId() > maxId) maxId = a.getId();
        }
        int next = Math.max(getNextAnimalId(), maxId + 1);
        setNextAnimalId(next);
    }

    public void save(List<Animal> animals) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("id,type,name,ageDays,hunger,productionCounterDays");
            writer.newLine();

            List<Animal> copy = new ArrayList<>(animals);
            copy.sort(Comparator.comparingInt(Animal::getId));

            for (Animal a : copy) {
                writer.write(a.toCsvRow());
                writer.newLine();
            }
        } catch (IOException ignored) {
        }
    }

    public int createId() {
        int id = getNextAnimalId();
        setNextAnimalId(id + 1);
        return id;
    }

    private int getNextAnimalId() {
        return dataStore.getInt("nextAnimalId", 1);
    }

    private void setNextAnimalId(int next) {
        dataStore.setInt("nextAnimalId", Math.max(1, next));
    }

    private Animal createFromType(AnimalType type, int id, String name, int ageDays, int hunger, int counter) {
        switch (type) {
            case CHICKEN:
                return new Chicken(id, name, ageDays, hunger, counter);
            default:
                return null;
        }
    }
}