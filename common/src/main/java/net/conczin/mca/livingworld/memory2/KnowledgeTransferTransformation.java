package net.conczin.mca.livingworld.memory2;

import java.util.List;
import java.util.UUID;

/** Immutable bounded process snapshot for wording transformations in one rumor lineage. */
public record KnowledgeTransferTransformation(List<Step> steps) {
    public KnowledgeTransferTransformation {
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("at least one transformation step is required");
        }
        if (steps.size() > KnowledgeTransferTransformationPolicy.MAX_TRANSFORMATIONS) {
            throw new IllegalArgumentException("transformation budget exceeded");
        }
        steps = List.copyOf(steps);
    }

    public int transformationsUsed() {
        return steps.size();
    }

    public String currentStatement() {
        return steps.getLast().transformedStatement();
    }

    public enum Kind {
        OMIT_TRAILING_SENTENCE
    }

    public record Step(
            Kind kind,
            String sourceStatement,
            String transformedStatement,
            UUID speakerNpcId,
            UUID listenerNpcId,
            UUID sourceSemanticEntryId,
            UUID evidenceEventId,
            long gameTime
    ) {
        public Step {
            if (kind == null) throw new IllegalArgumentException("kind is required");
            if (sourceStatement == null || sourceStatement.isBlank()) {
                throw new IllegalArgumentException("sourceStatement is required");
            }
            if (transformedStatement == null || transformedStatement.isBlank()) {
                throw new IllegalArgumentException("transformedStatement is required");
            }
            if (speakerNpcId == null || listenerNpcId == null || sourceSemanticEntryId == null || evidenceEventId == null) {
                throw new IllegalArgumentException("transformation identity fields are required");
            }
            if (speakerNpcId.equals(listenerNpcId)) {
                throw new IllegalArgumentException("speaker and listener must differ");
            }
            if (gameTime < 0L) throw new IllegalArgumentException("gameTime must be non-negative");
        }
    }
}
