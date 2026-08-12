package net.conczin.mca.livingworld.context;

/** Fixed-size read-only influence derived from a PersonalitySocialSnapshot. */
public record PersonalitySocialInfluence(
        PersonalityDialogueStyle personalityStyle,
        NpcPairDisposition pairDisposition
) {
    public static final PersonalitySocialInfluence NEUTRAL = new PersonalitySocialInfluence(
            PersonalityDialogueStyle.NEUTRAL,
            NpcPairDisposition.NEUTRAL
    );

    public PersonalitySocialInfluence {
        personalityStyle = personalityStyle == null ? PersonalityDialogueStyle.NEUTRAL : personalityStyle;
        pairDisposition = pairDisposition == null ? NpcPairDisposition.NEUTRAL : pairDisposition;
    }
}
