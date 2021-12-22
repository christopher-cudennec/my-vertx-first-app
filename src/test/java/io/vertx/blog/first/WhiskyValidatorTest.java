package io.vertx.blog.first;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.blog.first.manager.WhiskyValidator;
import io.vertx.blog.first.manager.model.Whisky;
import org.junit.jupiter.api.Test;

class WhiskyValidatorTest {

  private final WhiskyValidator validator = new WhiskyValidator();

  @Test
  void shouldNotValidate() {
    assertThat(validator.validate(new Whisky())).isFalse();
  }

  @Test
  void shouldValidate() {
    assertThat(validator.validate(new Whisky("name", "origin"))).isTrue();
  }
}
