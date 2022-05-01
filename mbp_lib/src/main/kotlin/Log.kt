
import java.io.File

fun log(s: String) {
    File("log.txt").appendText(s + System.lineSeparator())
}