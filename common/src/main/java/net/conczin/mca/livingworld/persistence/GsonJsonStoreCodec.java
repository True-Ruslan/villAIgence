package net.conczin.mca.livingworld.persistence;

import com.google.gson.Gson;

import java.util.Objects;

/** Gson adapter kept outside the dependency-free recovery primitive. */
public final class GsonJsonStoreCodec<T> implements JsonStoreRecovery.Codec<T> {
    private final Gson gson;
    private final Class<T> type;

    public GsonJsonStoreCodec(Gson gson, Class<T> type) {
        this.gson = Objects.requireNonNull(gson, "gson");
        this.type = Objects.requireNonNull(type, "type");
    }

    @Override
    public T decode(String raw) {
        return gson.fromJson(raw, type);
    }

    @Override
    public String encode(T value) {
        return gson.toJson(value);
    }
}
