# Contributing to DevSync Standup Bot

Thank you for your interest in contributing! This document provides guidelines for contributing to the project.

## Code of Conduct

- Be respectful and inclusive
- Welcome newcomers
- Focus on constructive feedback
- Maintain professional communication

## How to Contribute

### Reporting Bugs

1. Check if the bug has already been reported
2. Create a detailed issue with:
   - Clear title
   - Steps to reproduce
   - Expected vs actual behavior
   - Environment details (Java version, OS, etc.)
   - Logs or screenshots

### Suggesting Features

1. Check existing feature requests
2. Create an issue with:
   - Clear description
   - Use case/motivation
   - Proposed solution
   - Alternative approaches

### Pull Requests

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Make your changes
4. Write/update tests
5. Ensure all tests pass: `mvn test`
6. Update documentation
7. Commit with clear messages
8. Push to your fork
9. Create a Pull Request

## Development Setup

1. Install prerequisites:
   - Java 17
   - Maven 3.6+
   - MySQL 8.0+
   - Git

2. Clone repository:
```bash
git clone https://github.com/your-org/standup-bot.git
cd standup-bot
```

3. Setup database:
```sql
CREATE DATABASE standup_bot_dev;
```

4. Configure application:
```bash
cp .env.example .env
# Edit .env with your configuration
```

5. Build project:
```bash
mvn clean install
```

6. Run tests:
```bash
mvn test
```

## Coding Standards

### Java Style Guide

- Follow Google Java Style Guide
- Use meaningful variable names
- Write clear comments
- Keep methods small and focused
- Use Lombok annotations appropriately

### Code Organization

```
service/        # Business logic
controller/     # REST endpoints
repository/     # Data access
model/          # Entities
dto/            # Data transfer objects
config/         # Configuration
exception/      # Exception handling
util/           # Utility classes
```

### Naming Conventions

- Classes: `PascalCase`
- Methods: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Packages: `lowercase`

### Documentation

- Add JavaDoc for public methods
- Include usage examples
- Document complex logic
- Update README for new features

## Testing

### Unit Tests

- Write tests for all business logic
- Use JUnit 5
- Mock external dependencies
- Aim for 80%+ code coverage

Example:
```java
@Test
void testStartStandup() {
    // Given
    StandupRequest request = StandupRequest.builder()
        .userEmail("test@example.com")
        .build();
    
    // When
    StandupResponse response = standupService.startStandup(request);
    
    // Then
    assertNotNull(response);
    assertEquals(1, response.getCurrentStep());
}
```

### Integration Tests

- Test API endpoints
- Test database operations
- Use H2 for test database

## Commit Messages

Format: `type(scope): message`

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Formatting
- `refactor`: Code restructuring
- `test`: Adding tests
- `chore`: Maintenance

Examples:
```
feat(standup): add support for custom questions
fix(zoho): resolve webhook timeout issue
docs(readme): update installation steps
```

## Pull Request Process

1. **Before submitting:**
   - Ensure tests pass
   - Update documentation
   - Follow coding standards
   - Rebase on latest main

2. **PR Description:**
   - Clear title
   - Detailed description
   - Link related issues
   - List changes made
   - Add screenshots if UI changes

3. **Review Process:**
   - Address reviewer comments
   - Keep discussion focused
   - Be open to feedback
   - Make requested changes

4. **After Approval:**
   - Squash commits if requested
   - Maintainer will merge

## Feature Development Workflow

1. Create issue for new feature
2. Discuss approach in issue
3. Get approval from maintainers
4. Create feature branch
5. Implement feature
6. Write tests
7. Update docs
8. Submit PR
9. Address review feedback
10. Merge after approval

## Priority Labels

- `critical`: Security/data loss issues
- `high`: Important features/bugs
- `medium`: Nice-to-have features
- `low`: Minor improvements
- `good-first-issue`: Beginner friendly

## Questions?

- Open a discussion on GitHub
- Ask in pull request comments
- Contact maintainers

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

---

Thank you for contributing! 🎉
