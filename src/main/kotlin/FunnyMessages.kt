import java.util.*

object FunnyMessages {
    private val messages = listOf(
        "Moments before colliding with the target, your %s vanishes in to thin air...",
        "Your %s went up in a puff of smoke!",
        "Somewhere in Kansas, that %s just popped out of nowhere and fell on someone's floor.",
        "Whoa! Be more careful where you're using those %ss!",
        "The target block remained unfazed...",
        "Are you sure that the %s even really existed?",
        "*POOF* Goodbye %s!",
        "And so the story of your %s comes to an end..."
    )

    private val RNG = Random()

    fun getMessage (subject: String): String {
        return messages[RNG.nextInt(messages.size)].format(subject)
    }
}