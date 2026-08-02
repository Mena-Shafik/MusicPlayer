# Android Project Instructions

You are an experienced Android Engineer.

## General

- Use Kotlin only.
- Use Jetpack Compose for all UI.
- Never generate XML layouts unless explicitly requested.
- Use Material 3 components.
- Follow Android and Kotlin official style guides.
- Prefer immutable data structures.
- Keep code clean, readable, and well documented.

## Architecture

- Follow MVVM architecture.
- Follow Clean Architecture principles.
- Keep UI, domain, and data layers separated.
- Business logic belongs in the ViewModel or domain layer.
- Composables should be stateless whenever possible.
- Use Repository pattern.

## Dependency Injection

- Do NOT use Hilt or Dagger.
- Use constructor injection manually.
- Avoid service locators.

## State Management

- Use StateFlow for UI state.
- Use SharedFlow for one-time events.
- UI state should be immutable.
- Avoid mutable state inside composables.

## Coroutines

- Use viewModelScope.
- Never use GlobalScope.
- Handle exceptions properly.
- Use suspend functions when appropriate.

## Jetpack Compose

- Keep composables small.
- Extract reusable composables.
- Every screen should have a @Preview.
- Support dark mode.
- Support dynamic font scaling.
- Add accessibility descriptions.
- Avoid unnecessary recompositions.

## Navigation

- Use Navigation Compose.
- Pass IDs instead of entire objects.
- Keep navigation logic outside composables.

## Networking

- Use Retrofit.
- Use Kotlin Serialization.
- Handle loading, success, and error states.
- Never expose Retrofit models directly to the UI.

## Error Handling

- Never swallow exceptions.
- Return Result where appropriate.
- Display user-friendly error messages.

## Testing

- Generate JUnit tests.
- Mock repositories.
- Cover edge cases.
- Prefer deterministic tests.

## Performance

- Avoid unnecessary recompositions.
- Use remember appropriately.
- Use rememberSaveable when state should survive configuration changes.
- Use LazyColumn for large lists.
- Avoid creating objects during recomposition.

## Code Generation

When generating code:

- Explain important architectural decisions.
- Generate production-ready code.
- Prefer readability over cleverness.
- Do not leave TODOs.
- Do not generate placeholder implementations.
- Include imports.
- Ensure the code compiles.

## Project Conventions

- Target Android API 36.
- Use Gradle Kotlin DSL.
- Use KSP instead of KAPT whenever possible.
- Use Version Catalog (libs.versions.toml).
- Follow package structure:

ui/
    screens/
    components/
    navigation/

domain/
    model/
    repository/
    usecase/

data/
    remote/
    local/
    repository/

- Every feature should contain:
  - Screen
  - ViewModel
  - UiState
  - Repository
  - Preview
  - Tests

- All new screens must include loading, empty, success, and error states.