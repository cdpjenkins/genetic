# TODOs

## TODO

- HTTP client - Get integration tests to use the dudestore-client
- HTTP client - Stop using threads in dudestore-client... there ought to be an option involving coroutines, surely...

- JSON - use kotlinx serialisation, not Jackson

- GUI - React app to interact with the above
- GUI - Proper auth

- Logging - log to a file
- Logging - log to an external service

- Modules - split codebase into modules. Note that I'm not sure if this is worth the hassle.
  - dudestore
  - dudestore client
  - evolver
  - individual-model
 
## Doing
- Config - dude config parameters in DB
- Config - API to change said parameters (note the need to inform the evolver about the change)

## Done
- Logging - use kotlin-logging instead of using SLF4J directly
