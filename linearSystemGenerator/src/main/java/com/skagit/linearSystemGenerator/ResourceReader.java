package com.skagit.linearSystemGenerator;

/** <pre>
Here’s a complete, runnable Java example showing how to safely read a text file from the classpath.
This works whether the file is inside your resources folder, packaged in a JAR, or on the classpath at runtime.
</pre>
*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ResourceReader {

	/**
	 * Reads a text file from the classpath and returns its content as a String.
	 *
	 * @param resourcePath Path relative to the classpath root (e.g.,
	 *                     "data/example.txt")
	 * @return File content as a String
	 * @throws IOException if the resource cannot be read
	 */
	public static String readResource(final String resourcePath) throws IOException {
		// Use the current class's ClassLoader to find the resource
		try (InputStream inputStream = ResourceReader.class.getClassLoader().getResourceAsStream(resourcePath)) {
			if (inputStream == null)
				throw new IOException("Resource not found: " + resourcePath);

			// Read the file content using BufferedReader
			final StringBuilder content = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					content.append(line).append(System.lineSeparator());
				}
			}
			return content.toString();
		}
	}

	public static void main(final String[] args) {
		final String resourcePath = "com/skagit/linearSystemGenerator/insults.txt"; // Example path in resources folder

		try {
			final String fileContent = readResource(resourcePath);
			System.out.println("File content:\n" + fileContent);
		} catch (final IOException e) {
			System.err.println("Error reading resource: " + e.getMessage());
		}
	}
}
/**
 * <pre>
How to set it up:

Place your file in src/main/resources/data/example.txt(for Maven/Gradle projects).
When you build, it will be copied into the classpath automatically.
Run the program - it will find the file inside the JAR or classpath.

Path rules:

Use relative paths like"data/example.txt"(no leading/ when using ClassLoader.getResourceAsStream).
If you use YourClass.class.getResourceAsStream("/data/example.txt"), the leading / means absolute
path from classpath root.

Advantages of this approach:

Works both in IDE and when packaged into a JAR.
Handles UTF-8 encoding explicitly.
Includes robust error handling for missing resources.
 *
 * </pre>
 */