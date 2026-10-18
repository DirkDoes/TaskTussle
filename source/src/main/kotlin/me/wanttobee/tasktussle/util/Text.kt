package me.wanttobee.tasktussle.util

internal fun wrapLore(text: String, maxLength: Int = 42): List<String> {
	val lines = mutableListOf<String>()
	var line = ""
	for (word in text.trim().split(Regex("""\s+""")).filter(String::isNotEmpty)) {
		if (line.isEmpty() || line.length + word.length + 1 <= maxLength) line = if (line.isEmpty()) word else "$line $word"
		else { lines += line; line = word }
	}
	if (line.isNotEmpty()) lines += line
	return lines
}
