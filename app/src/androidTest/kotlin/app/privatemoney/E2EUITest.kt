package app.privatemoney

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class E2EUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testPlansForms() {
        composeTestRule.onNodeWithText("Plans").performClick()
        composeTestRule.onNodeWithContentDescription("Add Plan Item").performClick()
        composeTestRule.onNodeWithText("Add Goal").performClick()
        composeTestRule.onNodeWithText("Goal Name").performTextInput("New House")
        composeTestRule.onNodeWithText("Target Amount").performTextInput("500000")
        composeTestRule.onNodeWithText("Save").performClick()
    }

    @Test
    fun testLedgerEnhancements() {
        composeTestRule.onNodeWithText("Ledger").performClick()
        composeTestRule.onNodeWithText("Search transactions...").performTextInput("Groceries")
    }

    @Test
    fun testImportUI() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Import Statement").performClick()
        composeTestRule.onNodeWithText("Supported formats: CSV, XLSX, PDF").assertExists()
    }
}
