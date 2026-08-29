package dev.ilamparithi.aournalpp.backup

import dev.ilamparithi.aournalpp.backup.model.ConflictResolutionPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class ConflictPolicyTest {

    @Test
    fun testPolicyResolution() {
        assertEquals(ConflictResolutionPolicy.KEEP_NEWER, ConflictResolutionPolicy.fromId("keep_newer"))
        assertEquals(ConflictResolutionPolicy.OVERWRITE_LOCAL, ConflictResolutionPolicy.fromId("overwrite_local"))
        assertEquals(ConflictResolutionPolicy.SKIP_CONFLICTS, ConflictResolutionPolicy.fromId("skip_conflicts"))
        assertEquals(ConflictResolutionPolicy.KEEP_NEWER, ConflictResolutionPolicy.fromId("invalid_fallback"))
    }
}
