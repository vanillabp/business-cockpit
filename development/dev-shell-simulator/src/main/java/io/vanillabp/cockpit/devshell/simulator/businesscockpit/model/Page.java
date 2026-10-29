package io.vanillabp.cockpit.devshell.simulator.businesscockpit.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * One page of a longer list, together with the numbers the official API reports about it.
 *
 * @param <O> The type of the objects on the page
 */
@Getter
@Builder
public class Page<O> {

  private Long totalElements;

  private Integer totalPages;

  private Integer number;

  private Integer size;

  private List<O> pageObjects;

  /**
   * Creates a page. Use {@link #builder()} instead.
   *
   * @param totalElements The number of objects on all pages
   * @param totalPages The number of pages
   * @param number The number of this page, starting at 0
   * @param size The number of objects a page holds
   * @param pageObjects The objects of this page
   */
  Page(
      final Long totalElements,
      final Integer totalPages,
      final Integer number,
      final Integer size,
      final List<O> pageObjects) {

    this.totalElements = totalElements;
    this.totalPages = totalPages;
    this.number = number;
    this.size = size;
    this.pageObjects = pageObjects;

  }

}
