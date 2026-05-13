package components.page

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

internal val DeadlineDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT)

internal fun formatDeadlineDateInput(value: String): String {
    val digits = value.filter(Char::isDigit).take(8)
    val builder = StringBuilder()

    digits.forEachIndexed { index, char ->
        if (index == 2 || index == 4) {
            builder.append('.')
        }
        builder.append(char)
    }

    return builder.toString()
}

internal fun isDeadlineDateValid(
    value: String,
    minimumDate: LocalDate = LocalDate.now().plusMonths(3)
): Boolean {
    val parsedDate = parseDeadlineDate(value) ?: return false
    return !parsedDate.isBefore(minimumDate)
}

internal fun parseDeadlineDate(value: String): LocalDate? {
    if (value.length != 10) return null
    return runCatching {
        LocalDate.parse(value, DeadlineDateFormatter)
    }.getOrNull()
}
