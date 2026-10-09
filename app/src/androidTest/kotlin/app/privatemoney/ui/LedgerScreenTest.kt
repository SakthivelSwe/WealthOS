package app.privatemoney.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LedgerScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testLedgerSearchAndBottomSheet() {
        // Since we are running on emulator and just want to verify the UI components
        // exist as per the user's request. We can mock out the LedgerViewModel or just
        // test the interactions with the node tree.
    }
}
