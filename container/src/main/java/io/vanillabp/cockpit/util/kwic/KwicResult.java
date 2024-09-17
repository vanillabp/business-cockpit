package io.vanillabp.cockpit.util.kwic;

import org.springframework.data.annotation.Id;

public record KwicResult(@Id String item, int count) {}
