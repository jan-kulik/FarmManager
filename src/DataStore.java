import java.io.*;
import java.util.*;
import java.nio.file.*;

// Simpler key=value Datenspeicher der in eine Textdatei schreibt.
// Schreibt erst in eine .tmp-Datei und benennt sie dann um damit
// bei einem Absturz keine kaputten Dateien entstehen koennen.

// !!! Den Speicher mechanismus haben wir von StackOverflow!!! keine ganz eigene Logik!!! Deshalb nicht darauf eingegangeb in der Dokumentation!!!!
public class DataStore {
    private final String filePath;
    private final Map<String,String> data = new LinkedHashMap<>();

    public DataStore(String filePath) {
        this.filePath = filePath;
        load();
    }

    public boolean hasKey(String key) {
        if(key ==null) return false;
        return data.containsKey(key);
    }

    public String getString(String key, String defaultValue) {
        if(key == null) return defaultValue;
        return data.getOrDefault(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String v = getString(key, null);
        if(v == null) return defaultValue;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e){
            return defaultValue;
        }
    }

    public double getDouble(String key, double defaultValue) {
        String v = getString(key, null);
        if(v == null) return defaultValue;
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e){
            return defaultValue;
        }
    }

    public void set(String key, String value) {
        if(key == null || key.isBlank()) return;
        if(value == null) value = "";
        data.put(key.trim(), value.trim());
    }

    public void saveNow() {
        save();
    }

    public void setInt(String key, int value) {
        set(key, String.valueOf(value));
    }

    public void setDouble(String key, double value) {
        set(key, String.valueOf(value));
    }

    private void load(){
        Path path = Paths.get(filePath);
        if(!Files.exists(path)) {
            save(); // Datei neu anlegen wenn noch nicht vorhanden
            return;
        }

        // Format: key=value, Kommentare mit # werden ignoriert
        try ( BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String t = line.trim();
                if(t.isEmpty() || t.startsWith("#")) continue;
                int idx = t.indexOf('=');
                if(idx < 0) continue;
                String k = t.substring(0, idx).trim();
                String v = t.substring(idx+1).trim();
                data.put(k, v);
            }
        } catch (IOException ignored) {
        }
    }

    private void save() {
        // erstmal in .tmp schreiben, dann umbenennen
        Path temp = Paths.get(filePath + ".tmp");
        Path target = Paths.get(filePath);

        try(BufferedWriter writer = Files.newBufferedWriter(temp)) {
            for( Map.Entry<String, String> e : data.entrySet()) {
                writer.write(e.getKey() + "=" + e.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            return;
        }
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            try{
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
            }
        }
    }
}
