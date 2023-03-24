package net.defade.amestify.graphics.texture.block.generator;

import java.util.List;

public record BlockTextureHolder(String blockName, List<String> properties) {
    public boolean containsPlaceholder(String placeholder) {
        for (String property : properties) {
            if (property.contains("${" + placeholder + "}")) {
                return true;
            }
        }

        return blockName.contains("${" + placeholder + "}");
    }

    public BlockTextureHolder replace(String placeholder, String replace) {
        return new BlockTextureHolder(
                blockName.replace("${" + placeholder + "}", replace),
                properties.stream().map(property -> property.replace("${" + placeholder + "}", replace)).toList()
        );
    }
}
