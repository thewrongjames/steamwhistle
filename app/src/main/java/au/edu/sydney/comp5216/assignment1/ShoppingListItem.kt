package au.edu.sydney.comp5216.assignment1

import java.time.ZonedDateTime

data class ShoppingListItem(
    var bought: Boolean,
    var name: String,
    var quantity: String,
    // Using a zoned date time so as to represent an actual point in time, so that the amount of
    // time left won't change if the user changes time zones.
    var due: ZonedDateTime,
)
