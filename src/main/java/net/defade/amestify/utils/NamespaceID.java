package net.defade.amestify.utils;

import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a <a href="https://minecraft.gamepedia.com/Namespaced_ID">namespaced ID</a>
 */
public final class NamespaceID implements CharSequence {
    private static final String legalLetters = "[0123456789abcdefghijklmnopqrstuvwxyz_-]+";
    private static final String legalPathLetters = "[0123456789abcdefghijklmnopqrstuvwxyz./_-]+";

    private final String domain;
    private final String path;
    private final String full;

    public static @NotNull NamespaceID from(@NotNull String namespace) {
        final int index = namespace.indexOf(':');
        final String domain;
        final String path;
        if (index < 0) {
            domain = "minecraft";
            path = namespace;
            namespace = "minecraft:" + namespace;
        } else {
            domain = namespace.substring(0, index);
            path = namespace.substring(index + 1);
        }
        return new NamespaceID(namespace, domain, path);
    }

    public static @NotNull NamespaceID from(@NotNull String domain, @NotNull String path) {
        return from(domain + ":" + path);
    }

    private NamespaceID(String full, String domain, String path) {
        this.full = full;
        this.domain = domain;
        this.path = path;
        assert !domain.contains(".") && !domain.contains("/") : "Domain cannot contain a dot nor a slash character (" + full + ")";
        assert domain.matches(legalLetters) : "Illegal character in domain (" + full + "). Must match " + legalLetters;
        assert path.matches(legalPathLetters) : "Illegal character in path (" + full + "). Must match " + legalPathLetters;
    }

    public @NotNull String domain() {
        return domain;
    }

    public @NotNull String path() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NamespaceID that = (NamespaceID) o;
        return full.equals(that.full);
    }

    @Override
    public int hashCode() {
        return full.hashCode();
    }

    @Override
    public int length() {
        return full.length();
    }

    @Override
    public char charAt(int index) {
        return full.charAt(index);
    }

    @Override
    public @NotNull CharSequence subSequence(int start, int end) {
        return full.subSequence(start, end);
    }

    @Override
    public @NotNull String toString() {
        return full;
    }

    @Pattern("[a-z0-9_\\-.]+")
    public @NotNull String namespace() {
        return this.domain;
    }

    public @NotNull String value() {
        return this.path;
    }

    public @NotNull String asString() {
        return this.full;
    }
}
