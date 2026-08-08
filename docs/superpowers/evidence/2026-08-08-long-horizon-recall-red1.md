# Semantic Long-Horizon RED 1

- Behavioral tests-only commit: `b13ac7a84cccc2ad5c50826db935129168f7cb95`
- Subsequent commits before CI observation are documentation-only.
- No `common/src/main/**` production file has changed since base `b09924d7297775baabf577ca50dbcb65c22f0516`.
- Expected failing test: `SemanticMemoryRetrieverTest.contextProviderRecallsOldDurableSemanticMemoryAfterNewerEligibleWindowAndReload`.
- Expected reason: newest-only 32-candidate truncation starves an old persisted high-durability eligible Semantic record before final ranking.
