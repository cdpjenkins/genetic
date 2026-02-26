# Plan: Add UUID to Individual

## Summary

Add a type 4 (random) UUID field to `Individual` that is:
- Serialized in JSON representation
- Stored as a column in the Dudes database table
- Generated fresh on every new Individual (including via mutation)

---

## Design: uuid as constructor parameter with default

```kotlin
data class Individual(
    val genome: List<Shape>,
    val bounds: BoundsRectangle,
    val generation: Int = 1,
    val uuid: UUID = UUID.randomUUID()
)
```

**Why this works:**
- All existing call sites omit `uuid` → each new Individual gets a fresh random UUID automatically
- `mutate()` calls `Individual(newGenome, bounds, generation + 1)` with no uuid arg → mutated Individual gets a new UUID
- Jackson-Kotlin serializes it to JSON; when deserializing, uses the uuid value from JSON (backward-compatible: old JSON without `uuid` generates a fresh one via the default)
- `uuid` is part of data class `equals()` — the existing round-trip test still passes because uuid is read back from JSON correctly

---

## Files to Change

| File | Change |
|------|--------|
| `Individual.kt` | Add `val uuid: UUID = UUID.randomUUID()` constructor param; add `java.util.UUID` import |
| `DudeDao.kt` | Add `ALTER TABLE Dudes ADD COLUMN IF NOT EXISTS uuid VARCHAR(36)` in `createTables()`; update `insertDude` SQL + binding; add `uuid` field to `DudeRow` |
| `JSONTest.kt` | Add: uuid field present in JSON; uuid preserved through round-trip |

---

## TDD Steps

### Step 1 — RED: uuid appears in serialised JSON

```kotlin
@Test
fun `uuid field is included in serialised Individual`() {
    val json = serialise(individual)
    json shouldContain "\"uuid\""
}
```

Expected error: json does not contain `"uuid"`.

### Step 2 — GREEN: Add uuid to Individual

Add `val uuid: UUID = UUID.randomUUID()` to the Individual constructor.
Add `import java.util.UUID`.

Run tests — new test passes; existing round-trip test still passes.

### Step 3 — RED: uuid is preserved through JSON round-trip

```kotlin
@Test
fun `uuid is preserved through JSON round-trip`() {
    val json = serialise(individual)
    val deserialized = json.deserialise<Individual>()
    deserialized.uuid shouldBe individual.uuid
}
```

Expected to be GREEN immediately (uuid is a constructor param, Jackson handles it).

### Step 4 — RED: mutated Individual gets a different uuid

```kotlin
@Test
fun `mutated Individual gets a different uuid`() {
    val mutated = individual.mutate(testEvolverSettings)
    mutated.uuid shouldNotBe individual.uuid
}
```

Expected to be GREEN immediately (mutation creates new Individual with new UUID.randomUUID()).

### Step 5 — Database: uuid stored in Dudes table

In `createTables()`, add after the Dudes CREATE TABLE:

```sql
ALTER TABLE Dudes ADD COLUMN IF NOT EXISTS uuid VARCHAR(36);
```

Update `insertDude`:

```sql
INSERT INTO Dudes (name, generation, fitness, timeInMillis, genomeSize, createdTimestamp, uuid)
VALUES(:name, :generation, :fitness, :timeInMillis, :genomeSize, :createdTimestamp, :uuid)
```

Add `.bind("uuid", dude.uuid.toString())`.

Update `DudeRow`:

```kotlin
data class DudeRow(
    var name: String,
    var generation: Int,
    var individual: String?,
    var fitness: Int? = null,
    var timeInMillis: Long? = null,
    var genomeSize: Int? = null,
    var createdTimestamp: Long? = null,
    var uuid: String? = null
)
```

---

## Backward Compatibility

- Old JSON without `uuid`: Jackson uses `UUID.randomUUID()` default on deserialization — safe
- Existing DB rows: `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` is idempotent; old rows get `null` uuid
