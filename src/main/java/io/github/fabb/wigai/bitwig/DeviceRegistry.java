package io.github.fabb.wigai.bitwig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.fabb.wigai.common.Logger;

import java.io.InputStream;
import java.util.*;

/**
 * Registry of known Bitwig and third-party device UUIDs, loaded from the bundled JSON resource.
 *
 * Two fast lookup maps are maintained for Bitwig-native devices (the only ones insertable
 * via the Controller API):
 *   byId   : uuid string  → DeviceInfo
 *   byName : lower-cased name → uuid string
 *
 * All categories (including CLAP/VST2/VST3) are kept in allByCategory for listing purposes.
 */
public class DeviceRegistry {

    private static final Set<String> BITWIG_NATIVE_CATEGORIES =
            Set.of("bitwig_audio_fx", "bitwig_instruments");

    public record DeviceInfo(String name, String category, String id) {
        public boolean isBitwigNative() {
            return BITWIG_NATIVE_CATEGORIES.contains(category);
        }
    }

    private final Map<String, DeviceInfo> byId;
    private final Map<String, String> byName;
    private final Map<String, List<DeviceInfo>> allByCategory;

    public DeviceRegistry(Logger logger) {
        Map<String, DeviceInfo> idMap = new LinkedHashMap<>();
        Map<String, String> nameMap = new LinkedHashMap<>();
        Map<String, List<DeviceInfo>> categoryMap = new LinkedHashMap<>();

        try (InputStream is = DeviceRegistry.class.getResourceAsStream("/bitwig-device-uuids-full.json")) {
            if (is == null) {
                logger.warn("DeviceRegistry: bitwig-device-uuids-full.json not found on classpath");
            } else {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(is);
                JsonNode devicesNode = root.get("devices");
                devicesNode.fields().forEachRemaining(categoryEntry -> {
                    String category = categoryEntry.getKey();
                    List<DeviceInfo> list = new ArrayList<>();
                    categoryEntry.getValue().fields().forEachRemaining(deviceEntry -> {
                        String name = deviceEntry.getKey();
                        String id = deviceEntry.getValue().asText().trim();
                        DeviceInfo info = new DeviceInfo(name, category, id);
                        list.add(info);
                        if (BITWIG_NATIVE_CATEGORIES.contains(category)) {
                            idMap.put(id, info);
                            nameMap.put(name.toLowerCase(Locale.ROOT), id);
                        }
                    });
                    categoryMap.put(category, Collections.unmodifiableList(list));
                });
                int total = categoryMap.values().stream().mapToInt(List::size).sum();
                logger.info("DeviceRegistry: loaded " + idMap.size()
                        + " insertable Bitwig-native devices, " + total + " total across all categories");
            }
        } catch (Exception e) {
            logger.error("DeviceRegistry: failed to load device registry", e);
        }

        this.byId = Collections.unmodifiableMap(idMap);
        this.byName = Collections.unmodifiableMap(nameMap);
        this.allByCategory = Collections.unmodifiableMap(categoryMap);
    }

    /** Look up a Bitwig-native device by its exact UUID string. */
    public Optional<DeviceInfo> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /** Look up a Bitwig-native device by name (case-insensitive). */
    public Optional<DeviceInfo> findByName(String name) {
        String id = byName.get(name.toLowerCase(Locale.ROOT));
        if (id == null) return Optional.empty();
        return Optional.ofNullable(byId.get(id));
    }

    /** All Bitwig-native devices keyed by UUID. */
    public Map<String, DeviceInfo> getBitwigNativeDevices() {
        return byId;
    }

    /** All devices across every category, including CLAP/VST2/VST3 (reference only). */
    public Map<String, List<DeviceInfo>> getAllByCategory() {
        return allByCategory;
    }

    /** Returns true if the given UUID belongs to an insertable Bitwig-native device. */
    public boolean isInsertable(String id) {
        return byId.containsKey(id);
    }
}
