# Testing
- Run `./gradlew test` to run unit tests
- Run `./gradlew intTest` to run integration tests
- Tests use kotest for assertions

# Running
- Main class: `com.cdpjenkins.genetic.evolver.EvolverMain`
- Run with `./gradlew run` or run the main class directly

# Architecture
- Uses ports and adapters architecture
- Handlers deal with HTTP requests or event logic, not business logic
- Commands handle business logic and should not directly access the database

# Persistence
- DudeStore: Remote persistence for individuals and settings
- S3: Optional persistence controlled by `EvolverSettings.saveToS3`
- Filesystem: Optional persistence controlled by `EvolverSettings.saveToFilesystem`
- FilePersister writes to `output/` directory every 10 generations
