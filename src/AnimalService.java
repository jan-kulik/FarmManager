import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AnimalService {

    private final AnimalRepository repository;
    private final List<Animal> animals;

    public AnimalService(AnimalRepository repository) {
        this.repository = repository;
        this.animals = new ArrayList<>();
        this.repository.loadOrCreate(this.animals);
        sortById();
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

    public void endDayAll(Inventory inventory, Balance balance) {
        for (Animal a : animals) {
            a.endOfDay(inventory, balance);
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

            // add more z.B.:
            // case COW:
            //     return new Cow(id, name);
            // case SHEEP:
            //     return new Sheep(id, name);

            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    private void sortById() {
        animals.sort(Comparator.comparingInt(Animal::getId));
    }
}