#!/usr/bin/env python3
"""Apply the reviewed E1 identity-replay guard as one exact source replacement."""

from pathlib import Path

TARGET = Path("common/src/main/java/net/conczin/mca/block/TombstoneBlock.java")

OLD_IMPORT = "import java.util.Optional;\nimport java.util.function.Function;"
NEW_IMPORT = "import java.util.Optional;\nimport java.util.UUID;\nimport java.util.function.Function;"

OLD_METHOD = """        public Optional<Entity> createEntity(Level world, boolean remove) {
            try {
                return entityData.flatMap(data -> EntityType.create(withoutActiveEffects(data.nbt), world));
            } finally {
                if (remove) {
                    setEntity(null);
                }
            }
        }
"""

NEW_METHOD = """        public Optional<Entity> createEntity(Level world, boolean remove) {
            Optional<Entity> created = entityData.flatMap(
                    data -> EntityType.create(withoutActiveEffects(data.nbt), world)
            );
            if (remove
                    && created.isPresent()
                    && hasExistingIdentity(world, created.get().getUUID())) {
                return Optional.empty();
            }
            if (remove) {
                setEntity(null);
            }
            return created;
        }

        private boolean hasExistingIdentity(Level world, UUID uuid) {
            if (!(world instanceof ServerLevel serverLevel)) {
                return false;
            }
            for (ServerLevel level : serverLevel.getServer().getAllLevels()) {
                Entity existing = level.getEntity(uuid);
                if (existing != null && !existing.isRemoved()) {
                    return true;
                }
            }
            return false;
        }
"""


def replace_exact(source: str, old: str, new: str, label: str) -> str:
    count = source.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one {label} match, found {count}")
    return source.replace(old, new, 1)


def main() -> None:
    source = TARGET.read_text(encoding="utf-8")
    source = replace_exact(source, OLD_IMPORT, NEW_IMPORT, "UUID import anchor")
    source = replace_exact(source, OLD_METHOD, NEW_METHOD, "createEntity method")
    TARGET.write_text(source, encoding="utf-8")


if __name__ == "__main__":
    main()
