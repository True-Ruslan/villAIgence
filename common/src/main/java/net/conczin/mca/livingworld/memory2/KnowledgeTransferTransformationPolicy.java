package net.conczin.mca.livingworld.memory2;

import java.util.Optional;

/** Pure deterministic policy for the first bounded social-information transformation primitive. */
final class KnowledgeTransferTransformationPolicy {
    static final int MAX_TRANSFORMATIONS = 1;

    private KnowledgeTransferTransformationPolicy() {
    }

    static Optional<String> omitTrailingSentence(String sourceStatement) {
        String normalized = SemanticMemoryIngestionAdapter.normalizeAndLimitStatement(sourceStatement);
        if (normalized.isBlank()) return Optional.empty();

        int boundary = -1;
        for (int index = 0; index + 1 < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if ((current == '.' || current == '!' || current == '?')
                    && normalized.charAt(index + 1) == ' ') {
                boundary = index;
            }
        }
        if (boundary < 0 || boundary + 2 >= normalized.length()) return Optional.empty();

        String retained = normalized.substring(0, boundary + 1).strip();
        return retained.isBlank() || retained.equals(normalized)
                ? Optional.empty()
                : Optional.of(retained);
    }

    static boolean valid(
            KnowledgeTransferTransformation transformation,
            KnowledgeTransferProvenance provenance
    ) {
        if (transformation == null
                || transformation.steps() == null
                || transformation.steps().size() != 1
                || !KnowledgeTransferProvenancePolicy.valid(provenance)) {
            return false;
        }

        KnowledgeTransferTransformation.Step step = transformation.steps().getFirst();
        if (step == null || step.kind() != KnowledgeTransferTransformation.Kind.OMIT_TRAILING_SENTENCE) {
            return false;
        }

        String source = SemanticMemoryIngestionAdapter.normalizeAndLimitStatement(step.sourceStatement());
        String transformed = SemanticMemoryIngestionAdapter.normalizeAndLimitStatement(step.transformedStatement());
        if (!source.equals(step.sourceStatement())
                || !transformed.equals(step.transformedStatement())
                || !source.equals(provenance.origin().statement())) {
            return false;
        }
        if (!omitTrailingSentence(source).filter(transformed::equals).isPresent()) {
            return false;
        }

        return provenance.hops().stream().anyMatch(hop ->
                step.speakerNpcId().equals(hop.speakerNpcId())
                        && step.listenerNpcId().equals(hop.listenerNpcId())
                        && step.sourceSemanticEntryId().equals(hop.speakerSemanticEntryId())
                        && step.evidenceEventId().equals(hop.evidenceEventId())
                        && step.gameTime() == hop.gameTime()
        );
    }

    static boolean matchesCurrentStatement(
            KnowledgeTransferProvenance provenance,
            KnowledgeTransferTransformation transformation,
            String statement
    ) {
        if (!KnowledgeTransferProvenancePolicy.valid(provenance)) return false;
        String normalized = SemanticMemoryIngestionAdapter.normalizeAndLimitStatement(statement);
        if (normalized.isBlank()) return false;
        if (transformation == null) return normalized.equals(provenance.origin().statement());
        return valid(transformation, provenance)
                && normalized.equals(transformation.currentStatement());
    }
}
