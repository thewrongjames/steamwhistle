package au.edu.sydney.comp5216.assignment1

import android.content.Intent
import androidx.activity.result.ActivityResult
import android.app.Activity.RESULT_OK
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import android.util.Log

data class ShoppingListItem(
    var bought: Boolean,
    var name: String,
    var quantity: String,
    // Using a zoned date time so as to represent an actual point in time, so that the amount of
    // time left won't change if the user changes time zones.
    var due: ZonedDateTime,
) {
    companion object {
        private const val DESERIALISE_TAG = "deserialiseFromIntent"

        private const val POSITION_SERIALISATION_NAME = "position"
        private const val BOUGHT_SERIALISATION_NAME = "bought"
        private const val NAME_SERIALISATION_NAME = "name"
        private const val QUANTITY_SERIALISATION_NAME = "quantity"
        private const val DUE_SERIALISATION_NAME = "due"

        /**
         * Place the details of an [item] at a [position] onto the given [intent], so that they can
         * later be extracted with [deserialiseFromIntent]. Use a negative position to
         * indicate the addition of a new item.
         */
        fun serialiseOntoIntent(item: ShoppingListItem, position: Int, intent: Intent) {
            intent.putExtra(POSITION_SERIALISATION_NAME, position)
            intent.putExtra(BOUGHT_SERIALISATION_NAME, item.bought)
            intent.putExtra(NAME_SERIALISATION_NAME, item.name)
            intent.putExtra(QUANTITY_SERIALISATION_NAME, item.quantity)
            intent.putExtra(DUE_SERIALISATION_NAME, item.due.toString())
        }

        /**
         * Extract the details of an item at a position from the given activity [result]. Designed
         * for use with [serialiseOntoIntent]. A negative position indicates that the item is a new
         * item to be added to the list. A null pair indicates that it could not be extracted.
         */
        fun deserialiseFromIntent(intent: Intent): Pair<ShoppingListItem, Int>? {
            val position = intent.getIntExtra(POSITION_SERIALISATION_NAME, -1)
            val bought = intent.getBooleanExtra(BOUGHT_SERIALISATION_NAME, false)

            val name = intent.getStringExtra(NAME_SERIALISATION_NAME)
            val quantity = intent.getStringExtra(QUANTITY_SERIALISATION_NAME)
            val dueString = intent.getStringExtra(DUE_SERIALISATION_NAME)

            // The java-primitive extras won't be null if they don't exist, they will just have
            // their default values, so we need to check them manually. The strings, however, will
            // be null if they don't exist
            if (
                !intent.hasExtra(POSITION_SERIALISATION_NAME)
                || !intent.hasExtra(BOUGHT_SERIALISATION_NAME)
                || name == null || quantity == null || dueString == null
            ) {
                Log.i(DESERIALISE_TAG, "Failed: some values not found")
                return null
            }

            val due: ZonedDateTime
            try {
                due = ZonedDateTime.parse(dueString)
            } catch (error: DateTimeParseException) {
                Log.i(DESERIALISE_TAG, "Failed: could not parse due ZonedDateTime")
                return null
            }

            return Pair(
                ShoppingListItem(bought, name, quantity, due),
                position
            )
        }
    }
}
