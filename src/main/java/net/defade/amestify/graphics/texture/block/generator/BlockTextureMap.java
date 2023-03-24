package net.defade.amestify.graphics.texture.block.generator;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.defade.amestify.world.Block;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class BlockTextureMap {
    private final Map<String, List<String>> placeholders = new HashMap<>();
    private final BlockTextureData[] stateIdToTexture = new BlockTextureData[Block.getStatesIdAmount()];

    public BlockTextureData[] init() throws IOException {
        InputStream blockTexturesJsonInputStream = BlockTextureMap.class.getClassLoader().getResourceAsStream("textures/block-textures.json");
        if(blockTexturesJsonInputStream == null) {
            throw new FileNotFoundException("textures/block-textures.json not found");
        }

        JsonObject jsonObject = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
                .fromJson(new InputStreamReader(blockTexturesJsonInputStream), JsonObject.class);
        JsonObject placeholdersObject = jsonObject.getAsJsonObject("placeholders");

        for (Map.Entry<String, JsonElement> placeholderEntry : placeholdersObject.entrySet()) {
            placeholders.put(placeholderEntry.getKey(), placeholderEntry.getValue().getAsJsonArray().asList()
                    .stream().map(JsonElement::getAsString).toList());
        }

        JsonObject blocksTextureObject = jsonObject.getAsJsonObject("texture_map");
        List<BlockTextureHolder> unparsedBlocks = new ArrayList<>();
        for (Map.Entry<String, JsonElement> blockTextureEntry : blocksTextureObject.entrySet()) {
            List<String> blocksInfo = new ArrayList<>();
            if(blockTextureEntry.getValue().isJsonPrimitive()) { // It is directly a texture
                blocksInfo.addAll(Arrays.asList("texture", blockTextureEntry.getValue().getAsString()));
            } else {
                for(JsonElement blockInfo : blockTextureEntry.getValue().getAsJsonArray()) {
                    blocksInfo.add(blockInfo.getAsString());
                }
            }

            unparsedBlocks.add(new BlockTextureHolder(blockTextureEntry.getKey(), blocksInfo));
        }
        blockTexturesJsonInputStream.close();

        for (BlockTextureHolder unparsedBlock : unparsedBlocks) {
            parseBlock(unparsedBlock);
        }

        return stateIdToTexture;
    }

    private void parseBlock(BlockTextureHolder blockTextureHolder) throws IOException {
        for(String placeholder : placeholders.keySet()) {
            if(blockTextureHolder.containsPlaceholder(placeholder)) {
                for(String replace : placeholders.get(placeholder)) { // Recursively replace the placeholders
                    parseBlock(blockTextureHolder.replace(placeholder, replace));
                }

                return; // Don't execute the following code everytime we replace a placeholder but only once
            }
        }

        for(int stateId : parseBlockStates(blockTextureHolder.blockName())) {
            Queue<String> textureInfo = new LinkedList<>(blockTextureHolder.properties());
            String texture = parseBlockImage(textureInfo);

            boolean isGrass = false;
            boolean isFoliage = false;
            boolean isWater = false;
            boolean isIgnored = texture == null;

            for(String remainingProperty : textureInfo) {
                if(remainingProperty.startsWith("tint=")) {
                    switch (remainingProperty.substring("tint=".length())) {
                        case "none":
                            break;
                        case "grass":
                            isGrass = true;
                            break;
                        case "foliage":
                            isFoliage = true;
                            break;
                        case "water":
                            isWater = true;
                            break;
                        default:
                            throw new IOException("Malformed block: " + textureInfo);
                    }
                }
            }

            stateIdToTexture[stateId] = new BlockTextureData(texture, isGrass, isFoliage, isWater, isIgnored);
        }
    }

    private List<Integer> parseBlockStates(String states) {
        List<Integer> blockStates = new ArrayList<>();
        String[] split = states.split(",");
        String blockName = "minecraft:" + split[0];

        if(!states.contains("*")) {
            if(split.length == 1) {
                blockStates.add(Block.fromNamespaceId(blockName).getDefaultStateId());
            } else {
                blockStates.add(Block.fromNamespaceId(blockName).getStateIdForProperties(split));
            }
        } else {
            Block block = Block.fromNamespaceId(blockName);
            for (Map.Entry<String[], Integer> blockStatesEntry : block.getStatesId().entrySet()) {
                boolean equals = true;

                for(String blockStateProperty : blockStatesEntry.getKey()) {
                    String[] blockProperties = blockStateProperty.split(",");
                    for(String blockProperty : blockProperties) {
                        String[] splitBlockProperty = blockProperty.split("=");

                        for(String state : split) {
                            if(state.equals(split[0])) continue;

                            String[] splitState = state.split("=");
                            if(splitBlockProperty[0].equals(splitState[0]) && !splitState[1].equals("*") && !splitBlockProperty[1].equals(splitState[1])) {
                                equals = false;
                                break;
                            }
                        }
                    }
                }

                if(equals) blockStates.add(blockStatesEntry.getValue());
            }
        }

        return blockStates;
    }

    private static String parseBlockImage(Queue<String> textureInfo) throws IOException {
        String operation = textureInfo.remove();

        switch (operation) {
            case "transparent" -> {
                return null;
            }

            case "texture" -> {
                return textureInfo.remove();
            }

            default -> throw new IOException("Malformed block: " + textureInfo);
        }
    }
}
