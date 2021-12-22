package io.vertx.blog.first.manager;

import io.vertx.blog.first.manager.model.Whisky;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhiskyValidator {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(WhiskyValidator.class);

  private final Validator validator;

  public WhiskyValidator() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  public boolean validate(Whisky whisky) {
    var violations = validator.validate(whisky);
    final var valid = violations.isEmpty();
    if (!valid) {
      LOGGER.warn("Object invalid: {}", violations);
    }
    return valid;
  }
}
