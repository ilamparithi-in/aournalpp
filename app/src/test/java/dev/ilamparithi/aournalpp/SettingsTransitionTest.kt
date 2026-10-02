package dev.ilamparithi.aournalpp

import dev.ilamparithi.aournalpp.ui.animation.SpringSlideTransition
import dev.ilamparithi.aournalpp.ui.settings.SettingsCategory
import dev.ilamparithi.aournalpp.ui.settings.SettingsTransitionHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTransitionTest {

    @Test
    fun testTwoPaneCategoryTransitionDirection() {
        // Navigating down the category hierarchy should slide forward
        assertTrue(
            SettingsTransitionHelper.isForwardCategoryTransition(
                initial = SettingsCategory.FILES_STORAGE,
                target = SettingsCategory.APPEARANCE_CANVAS
            )
        )
        assertTrue(
            SettingsTransitionHelper.isForwardCategoryTransition(
                initial = SettingsCategory.TOOLBAR,
                target = SettingsCategory.INPUT_STYLUS
            )
        )
        assertTrue(
            SettingsTransitionHelper.isForwardCategoryTransition(
                initial = SettingsCategory.DISPLAY_KEYBOARD,
                target = SettingsCategory.ABOUT
            )
        )

        // Navigating up the category hierarchy should slide backward
        assertFalse(
            SettingsTransitionHelper.isForwardCategoryTransition(
                initial = SettingsCategory.APPEARANCE_CANVAS,
                target = SettingsCategory.FILES_STORAGE
            )
        )
        assertFalse(
            SettingsTransitionHelper.isForwardCategoryTransition(
                initial = SettingsCategory.ABOUT,
                target = SettingsCategory.SYSTEM_MAINTENANCE
            )
        )
        assertFalse(
            SettingsTransitionHelper.isForwardCategoryTransition(
                initial = SettingsCategory.INPUT_STYLUS,
                target = SettingsCategory.TOOLBAR
            )
        )
    }

    @Test
    fun testCompactCategoryTransitionDirection() {
        // Entering a detail screen from the category index (null -> Category) is forward
        assertTrue(
            SettingsTransitionHelper.isForwardCompactTransition(
                initial = null,
                target = SettingsCategory.FILES_STORAGE
            )
        )
        assertTrue(
            SettingsTransitionHelper.isForwardCompactTransition(
                initial = null,
                target = SettingsCategory.ABOUT
            )
        )

        // Backing out from a detail screen to the category index (Category -> null) is backward
        assertFalse(
            SettingsTransitionHelper.isForwardCompactTransition(
                initial = SettingsCategory.FILES_STORAGE,
                target = null
            )
        )
        assertFalse(
            SettingsTransitionHelper.isForwardCompactTransition(
                initial = SettingsCategory.ABOUT,
                target = null
            )
        )

        // Navigating directly between sections in compact mode follows category order
        assertTrue(
            SettingsTransitionHelper.isForwardCompactTransition(
                initial = SettingsCategory.FILES_STORAGE,
                target = SettingsCategory.TOOLBAR
            )
        )
        assertFalse(
            SettingsTransitionHelper.isForwardCompactTransition(
                initial = SettingsCategory.TOOLBAR,
                target = SettingsCategory.FILES_STORAGE
            )
        )
    }

    @Test
    fun testSettingsCategorySpringSlideTransitionSpecs() {
        // Verify standard spring spec instantiation for SettingsCategory
        val standardSpec = SpringSlideTransition.createSpec<SettingsCategory>(
            isForward = true,
            reduceAnimations = false,
            isRapid = false
        )
        assertNotNull(standardSpec)

        // Verify rapid spring spec instantiation
        val rapidSpec = SpringSlideTransition.createSpec<SettingsCategory>(
            isForward = false,
            reduceAnimations = false,
            isRapid = true
        )
        assertNotNull(rapidSpec)

        // Verify reduced motion fallback spec instantiation
        val reducedSpec = SpringSlideTransition.createSpec<SettingsCategory>(
            isForward = true,
            reduceAnimations = true,
            isRapid = false
        )
        assertNotNull(reducedSpec)

        // Verify nullable category spec for compact mode
        val compactSpec = SpringSlideTransition.createSpec<SettingsCategory?>(
            isForward = true,
            reduceAnimations = false,
            isRapid = false
        )
        assertNotNull(compactSpec)
    }
}
