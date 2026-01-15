package io.vanillabp.cockpit.devshell.simulator.businesscockpit.model;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Page<O> {

    private Long totalElements;

    private Integer totalPages;

    private Integer number;

    private Integer size;

    private List<O> pageObjects;

}
