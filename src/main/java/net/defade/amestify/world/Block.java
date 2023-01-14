package net.defade.amestify.world;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.defade.amestify.Main;
import net.defade.amestify.utils.NamespaceID;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Block {
    private static Block[] BLOCKS_BY_STATE_ID;
    private final static Map<String, Block> BLOCKS_BY_NAMESPACE = new HashMap<>();

    private final NamespaceID namespace;
    private final int defaultStateId;
    private final Map<String[], Integer> statesId;

    public Block(String namespace, JsonObject blockObject) {
        this.namespace = NamespaceID.from(namespace);
        this.defaultStateId = blockObject.getAsJsonPrimitive("defaultStateId").getAsInt();
        this.statesId = parseStatesIds(blockObject.getAsJsonObject("states"));
    }

    private Map<String[], Integer> parseStatesIds(JsonObject statesIdsObject) {
        Map<String[], Integer> statesIds = new HashMap<>(statesIdsObject.size());

        for (String state : statesIdsObject.keySet()) {
            statesIds.put(
                    cleanProperties(state).split(","),
                    statesIdsObject.getAsJsonObject(state).getAsJsonPrimitive("stateId").getAsInt()
            );
        }

        return statesIds;
    }

    public int getDefaultStateId() {
        return defaultStateId;
    }

    public int getStateIdForProperties(String[] properties) {
        for (Map.Entry<String[], Integer> propertiesStatesIds : statesId.entrySet()) {
            boolean equals = true;
            for (String propertyKey : propertiesStatesIds.getKey()) {
                boolean contains = false;
                for(String property : properties) {
                    if (propertyKey.equals(property)) {
                        contains = true;
                        break;
                    }
                }

                if (!contains) {
                    equals = false;
                    break;
                }
            }

            if(equals) return propertiesStatesIds.getValue();
        }

        throw new RuntimeException(namespace + " " + "No state id found for properties " + Arrays.toString(properties));
    }

    public static void init() throws IOException {
        InputStream blocksJsonInputStream = Main.class.getClassLoader().getResourceAsStream("blocks.json");
        if(blocksJsonInputStream == null) {
            throw new FileNotFoundException("blocks.json not found");
        }

        int highestBlockStateId = -1;

        JsonObject jsonObject = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
                .fromJson(new InputStreamReader(blocksJsonInputStream), JsonObject.class);
        final Block[] blocks = new Block[jsonObject.size()];

        int index = 0;
        for (String key : jsonObject.keySet()) {
            Block block = new Block(key, jsonObject.get(key).getAsJsonObject());

            blocks[index] = block;
            index++;

            for (Integer stateId : block.statesId.values()) {
                if(highestBlockStateId < stateId) {
                    highestBlockStateId = stateId;
                }
            }
        }

        blocksJsonInputStream.close();

        BLOCKS_BY_STATE_ID = new Block[highestBlockStateId + 1];

        for (Block block : blocks) {
            for (int stateId : block.statesId.values()) {
                BLOCKS_BY_STATE_ID[stateId] = block;
            }

            BLOCKS_BY_NAMESPACE.put(block.namespace.asString(), block);
        }
    }

    public static Block fromStateId(int id) {
        return BLOCKS_BY_STATE_ID[id];
    }

    public static Block fromNamespaceId(String name) {
        return BLOCKS_BY_NAMESPACE.get(name);
    }

    public static Block fromNamespaceId(NamespaceID namespace) {
        return fromNamespaceId(namespace.path());
    }

    private static String cleanProperties(String properties) {
        return properties.replace("[", "").replace("]", "");
    }
}
