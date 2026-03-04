# Codebase Walkthrough

This document walks through the genetic project linearly, from entry point to persistence.

## What the project does

The project evolves geometric shapes that approximate a target image. It is inspired by Roger Johansson's "Genetic Programming: Evolution of Mona Lisa". Starting from a black canvas, the system randomly mutates a collection of overlapping shapes and keeps changes that move the result closer to the target. Given enough generations, the shapes converge on a recognisable approximation of the original image.

---

## 1. Entry point: `EvolverMain.kt`

`GeneticEvolverApplicationCommand` is a [Clikt](https://ajalt.github.io/clikt/) CLI command. When the program starts, Clikt parses two inputs:

- `name` – a positional argument that identifies the evolution project (used to store and retrieve data)
- `--master-image` – an optional path to the target image file
- `SECRET` – read from the environment variable of the same name

```
fun main(args: Array<String>) = GeneticEvolverApplicationCommand().main(args)
```

`run()` calls `GeneticEvolverApplication.create(name, secret, masterImageFileName)` and then `application.start()`.

---

## 2. Bootstrapping: `GeneticEvolverApplication.create()`

This companion object factory method wires everything together before handing back a running application.

### Step 1 – Connect to DudeStore

```kotlin
val dudeStoreClient = DudeStoreClient("https://genetic-dude.herokuapp.com", secret)
```

The remote DudeStore service is where evolved individuals are stored and shared across machines. The client talks HTTP to it.

### Step 2 – Load settings

Settings are loaded in priority order:

1. Local file `evolver-settings-{name}.json` (if it exists)
2. Remote DudeStore `GET /dudes/{name}/settings`
3. `EvolverSettings.default(name)` if neither exists

The resolved settings are written to `written-evolver-settings-{name}.json` so you can inspect what was actually used.

`EvolverSettings` controls every tuning parameter for evolution:

| Field | Purpose |
|---|---|
| `initialGenomeSize` | How many shapes to start with |
| `maxGenomeSize` | Upper limit on number of shapes |
| `minAlpha` / `maxAlpha` | Transparency range for new shapes |
| `colourMutateAmount` | Standard deviation for colour mutation |
| `pointMutateRange` | Range for position mutation |
| `newShapeProbabilityFactor` | How likely a mutation adds a shape vs mutates existing ones |
| `avgShapesToMutate` | Average number of shapes mutated per generation |
| `saveToS3` | Whether to persist to S3 |
| `saveToFilesystem` | Whether to persist to disk |
| `weights` | Weighted distribution of shape types |

### Step 3 – Load master image

The target image is loaded from the local file path (if supplied) or from the DudeStore. If neither is available the process crashes with a clear error. The image becomes an `IntArray` of packed pixel values used throughout fitness calculations.

### Step 4 – Load or create the initial individual

`dudeStoreClient.getLatestDude(name)` checks if there is already an evolved individual for this project name. If so, evolution resumes from that point. Otherwise `makeIndividual()` creates one from scratch using `EvolverSettings.initialGenomeSize` random shapes.

### Step 5 – Attach listeners

Listeners are notified every time a mutation improves fitness:

- **`GUIEvolverListener`** – updates the Swing window (skipped in headless environments)
- **`FilePersistenceListener`** – saves PNG, JSON, and SVG to disk every 10 generations (only if `saveToFilesystem` is true)
- **`DudeStoreClientListener`** – posts the improved individual to the remote DudeStore (always active)
- **`S3PersistenceListener`** – saves to S3 every 10 generations (only if `saveToS3` is true)

---

## 3. The evolution loop: `GeneticEvolverApplication.start()`

`start()` launches a background thread that loops forever:

```kotlin
while (true) {
    evolver.mutateOnce()

    if (shouldDownloadNewIndividual()) {
        val latestIndividual = dudeStoreClient.getLatestDude(name)
        evolver.supplyNewIndividual(latestIndividual)
    }
}
```

The `shouldDownloadNewIndividual` flag is set by the `DudeStoreClientListener` when a POST to the remote service fails (e.g. because another machine posted the same generation number first). In that case the loop pauses, downloads whatever the remote service has, replaces the local individual, and continues.

---

## 4. One mutation step: `Evolver.mutateOnce()`

This is the core of the genetic algorithm:

```kotlin
fun mutateOnce() {
    val newIndividual = mutator.mutate(individual)
    newIndividual.drawAndCalculateFitness(masterPixels)
    if (newIndividual.fitness < individual.fitness) {
        individual = newIndividual
        listeners.forEach { it.notify(individual) }
    }
}
```

1. **Mutate** – create a candidate by mutating the current individual
2. **Render & score** – draw the candidate to a `BufferedImage` and calculate its fitness
3. **Accept or discard** – if the candidate scores better (lower pixel distance), replace the current individual and notify all listeners; otherwise discard it

`Mutator` is a thin wrapper that delegates to `Individual.mutate(settings)`.

---

## 5. The individual: `Individual`

An `Individual` is a `data class` holding:

- `genome: List<Shape>` – the ordered list of shapes painted back-to-front
- `bounds: BoundsRectangle` – the canvas size
- `generation: Int` – incremented by 1 on every accepted mutation
- `uuid: UUID` – unique identifier for this specific state

Non-serialised fields (`@JsonIgnore`) that are recalculated at runtime:
- `bufferedImage` – the rendered result
- `fitness` – the pixel distance score

### Rendering: `draw(g: Graphics2D)`

The canvas starts black, then each shape in the genome is drawn in order. Shapes are semi-transparent so they layer on top of each other to create complex colour blends.

### Fitness: `drawAndCalculateFitness(masterPixels)`

After rendering, every pixel in the result is compared to the corresponding pixel in the master image:

```kotlin
total += ((dr*dr + dg*dg + db*db) shr 6)
```

`dr`, `dg`, `db` are the red, green, and blue channel differences. Squaring them penalises large differences more than small ones. The right-shift by 6 (÷64) keeps the total in a manageable range. Lower fitness means a closer match.

### Mutation: `mutate(settings)`

Two things can happen in a mutation:

1. **Add a shape** – with a probability that decreases as the genome fills toward `maxGenomeSize`:
   ```kotlin
   val addShapeProbability = (headRoom / maxGenomeSize) * newShapeProbabilityFactor
   ```
   A new random shape is appended to the end of the genome.

2. **Mutate existing shapes** – each shape independently has a chance of being mutated, calibrated so that on average `avgShapesToMutate` shapes are changed per generation.

Either way, `mutate` returns a new `Individual` with `generation + 1` and a fresh UUID. The original is unchanged.

---

## 6. Shapes: `Shape`

`Shape` is a sealed interface with five implementations. All are `data class` or extend `GeneralPathShape`, making them naturally immutable and easy to copy for mutation.

```kotlin
sealed interface Shape {
    fun draw(g: Graphics2D)
    fun mutate(evolverSettings: EvolverSettings): Shape
    fun maybeMutate(mutateProbability: Double, evolverSettings: EvolverSettings): Shape
}
```

Jackson uses `@JsonTypeInfo` with a `"type"` property to serialise and deserialise the correct subtype.

| Type | Geometry |
|---|---|
| `Circle` | Filled circle: centre point, radius |
| `RectangleShape` | Filled rectangle: two corner points |
| `QuadCurveShape` | Filled shape from a quadratic bezier path |
| `PolygonShape` | Filled polygon from a list of vertices |
| `StrokedCubicCurveShape` | Stroked cubic bezier curve with configurable width |

Each shape's `mutate()` creates a new instance with Gaussian-perturbed positions and colours, keeping values clamped within the canvas bounds and colour range.

`GeneralPathShape` is an abstract base for `QuadCurveShape` and `PolygonShape`. It builds the `java.awt.geom.GeneralPath` lazily so it is only computed once per instance.

### Value objects used by shapes

- `Point(x, y)` – a 2D position. `mutate()` adds a Gaussian offset clamped to `bounds`.
- `Colour(red, green, blue, alpha)` – RGBA colour. `mutate()` adds Gaussian noise to each channel, clamped to 0–255 (and `minAlpha`–`maxAlpha` for alpha).
- `BoundsRectangle(minX, minY, maxX, maxY)` – immutable canvas boundary. The width and height are derived properties.
- `Radius` – a type alias for `Int` with an extension `mutate()`.
- `StrokeWidth` – an inline value class for `Float` with a `mutate()` that respects `minStrokeWidth` / `maxStrokeWidth` settings.

---

## 7. Creating shapes from scratch: `Genesis.kt`

`makeIndividual()` creates the very first individual by calling `spawnRandomShape()` `initialGenomeSize` times.

`spawnRandomShape()` uses a `WeightedSelector` to pick a shape type according to the configured weights, then delegates to the appropriate spawner function (e.g. `spawnRandomCircle()`). Each spawner picks random points within the bounds and a random colour within the alpha range.

`WeightedSelector` accepts a list of `Weight(name, weight)` pairs. It builds a partial-sums array and picks a random position in that array to select a type proportionally to its weight.

---

## 8. DudeStore web service: `WebMain.kt`

DudeStore is a separate deployable HTTP service that acts as shared storage. Its `main()` reads `PORT`, `SECRET`, and `JDBC_DATABASE_URL` from the environment, constructs a `DudeStoreApplication`, and starts a Netty server.

### Authentication

Most write endpoints and the master-image GET are protected by `SecretAuthFilter`, which checks for `?secret=<value>` in the query string and returns 401 if it is wrong.

### Routes

| Method | Path | Description |
|---|---|---|
| `POST` | `/dudes/{name}` | Store an evolved individual |
| `GET` | `/dudes/{name}/latest` | Retrieve latest individual as JSON, PNG, or SVG |
| `GET` | `/dudes/{name}/latest/summary` | Retrieve lightweight metadata |
| `GET` | `/dudes/{name}/{generation}` | Retrieve a specific generation |
| `GET` | `/dudes` | List all project names and generation counts |
| `POST` | `/dudes/{name}/settings` | Create evolver settings |
| `PUT` | `/dudes/{name}/settings` | Update evolver settings |
| `GET` | `/dudes/{name}/settings` | Read evolver settings |
| `POST` | `/dudes/{name}/master-image` | Upload target image |
| `GET` | `/dudes/{name}/master-image` | Download target image |
| `POST` | `/setup` | Create database tables |
| `POST` | `/recreate` | Drop and recreate schema |

### Retrieving individuals

`getDudeLatest` and `getDudeByGeneration` use `resolveIndividual()`, which first checks the database JSONB column and falls back to S3 if that column is null:

```kotlin
fun resolveIndividual(dude: Dude?): Individual? {
    if (dude == null) return null
    return dude.individual ?: s3Persister?.readFromS3(dude.name, dude.generation)
}
```

This allows the database to store only metadata (fitness, genome size, timestamp) while the full JSON lives in S3.

### Conflict handling

`ExceptionHandlingFilter` catches `AlreadyExistsException` and returns HTTP 409 Conflict. This is how two machines racing to post the same generation number are handled gracefully.

---

## 9. Database layer: `DudeDao`

`DudeDao` wraps a JDBI `Jdbi` instance and executes plain SQL. There are three tables:

**`Dudes`** – primary key `(name, generation)`

| Column | Type | Notes |
|---|---|---|
| `name` | VARCHAR | Project name |
| `generation` | INT | Generation number |
| `individual` | JSONB | Full serialised individual (may be null if stored in S3) |
| `fitness` | INTEGER | Score |
| `timeInMillis` | INTEGER | Time to compute this generation |
| `genomeSize` | INTEGER | Number of shapes |
| `createdTimestamp` | BIGINT | Wall-clock time |
| `uuid` | VARCHAR | UUID of this individual |

**`EvolverSettings`** – primary key `name`, stores settings as JSONB.

**`MasterImages`** – primary key `name`, stores the target image as raw bytes.

`insertDude` catches `UnableToExecuteStatementException` and rethrows as `AlreadyExistsException` when the cause is PostgreSQL error code `23505` (unique constraint violation). Everything else propagates normally.

`latestDude` queries `ORDER BY generation DESC LIMIT 1` to find the highest generation.

---

## 10. S3 persistence: `DudeStoreS3Persister`

When S3 is enabled, every individual is also stored in S3 at:

```
{name}/json/dude_{generation:010d}.json.bz2
```

The JSON is compressed with BZip2 (Apache Commons Compress) before upload. On read, the code tries the `.json.bz2` key first, then falls back to `.json` for legacy uncompressed files.

The evolver-side `S3PersistenceListener` and `FilePersistenceListener` only persist every 10 generations (`individual.generation % 10 == 0`) to limit I/O volume.

---

## 11. HTTP client: `DudeStoreClient`

The evolver uses `DudeStoreClient` (OkHttp) to talk to the remote DudeStore. Key methods:

- `getLatestDude(name)` – GET the current best individual as JSON
- `postDude(individual, name)` – POST a new individual
- `getSettings(name)` / `postSettings` / `putSettings` – settings lifecycle
- `getMasterImage(name)` / `postMasterImage` – target image lifecycle

---

## 12. Local file persistence: `FilePersister`

When `saveToFilesystem` is true, every 10 generations the evolver writes three files to `output/{name}/`:

- `json/cow_{generation:010d}.json` – serialised individual
- `png/cow_{generation:010d}.png` – rendered image
- `svg/cow_{generation:010d}.svg` – vector rendering via `SvgRenderer`

`SvgRenderer` uses Apache Batik's `SVGGraphics2D` to produce an SVG by calling `individual.draw(g)` with the SVG graphics context instead of a raster one.

---

## 13. Visualisation: `GUI` and `GUIEvolverListener`

`GUI` is a Swing `JFrame` that shows the master image and the current evolved image side by side, with a label showing the current stats. It is created only when the JVM is not running headless.

`GUIEvolverListener.notify()` calls `gui.updateUiWithNewIndividual()`, which uses `SwingUtilities.invokeLater` to update the labels on the EDT without blocking the evolution thread.

---

## 14. JSON serialisation: `JSON.kt`

All serialisation uses Jackson via `jacksonObjectMapper()`. `Individual` and all `Shape` subtypes are plain data classes, so Jackson handles them with no custom code except for the `@JsonTypeInfo` / `@JsonSubTypes` annotations on `Shape` that encode and decode the concrete type.

`@JsonIgnoreProperties(ignoreUnknown = true)` on `EvolverSettings` means older settings files without newer fields (like `minStrokeWidth`) can be loaded without errors.

---

## 15. Tests

**Unit tests** (`src/test/kotlin/`):

- `EvolverTest` – verifies that `mutateOnce` accepts improving mutations, discards worse ones, and notifies listeners only on improvement.
- `WeightedSelectorTest` – verifies weighted random selection with various weight distributions.
- `IndividualUUIDTest` – verifies UUID assignment and that the `UUIDGenerationStrategy` is injectable for deterministic testing.
- `JSONTest` – round-trip serialisation tests for all shape types and edge cases like optional fields and unknown properties.
- `EvolverSettingsTest` – verifies default settings values.

**Integration tests** (`src/intTest/kotlin/`):

- `PostgresDudeDaoIT` – uses TestContainers to spin up a real PostgreSQL instance and exercises all DAO operations including unique constraint violations.
- `WebMainIT` – uses TestContainers for PostgreSQL and LocalStack (S3 emulation) to test the full HTTP API, including JSON/PNG/SVG responses, authentication, conflict handling, and S3 compression round-trips.
