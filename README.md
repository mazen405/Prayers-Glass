# Prayers Glass
An Android prayer-times home-screen widget with a glass-inspired style.

- Gets prayer times from the AlAdhan API using device location when available.
- Shows the next prayer, time remaining, and five daily prayer times.
- GitHub Actions builds a debug APK on pushes to main.

Build with JDK 17 and Gradle 8.9 using `gradle assembleDebug`.

This is an early MVP. Launcher widgets refresh periodically (configured to 30 minutes), not every minute. Verify times on a real device before relying on them.