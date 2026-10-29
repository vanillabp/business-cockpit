package io.vanillabp.cockpit.users.model;

/**
 * A person, as it is stored in a user task or a workflow, for example as the assignee. Besides the
 * id it holds the text a search looks at and the text a list sorts by.
 */
public class Person {
  private String id;
  private String fulltext;
  private String sort;

  /**
   * Creates an empty person. The application's mapper fills it.
   */
  public Person() {

  }

  /**
   * The id of the person.
   *
   * @return The id
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the id of the person.
   *
   * @param id The id
   */
  public void setId(
      String id) {
    this.id = id;
  }

  /**
   * The text a search for the person looks at, usually the name.
   *
   * @return The text
   */
  public String getFulltext() {
    return fulltext;
  }

  /**
   * Sets the text a search for the person looks at.
   *
   * @param fulltext The text
   */
  public void setFulltext(
      String fulltext) {
    this.fulltext = fulltext;
  }

  /**
   * The text a list sorts by when it is sorted by this person.
   *
   * @return The text
   */
  public String getSort() {
    return sort;
  }

  /**
   * Sets the text a list sorts by.
   *
   * @param sort The text
   */
  public void setSort(
      String sort) {
    this.sort = sort;
  }
}
