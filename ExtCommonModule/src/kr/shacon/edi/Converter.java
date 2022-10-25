package kr.shacon.edi;

/**
 * Used to convert a String to an appropriate object and an object to its String representation. Must be stateless.<br>
 * <p>
 * See {@link kr.shacon.edi.converters} for the bundled ones.
 */
public interface Converter {

  /**
   * Converts a String into an object, e.g. String 19790518 to Date May 18th, 1979
   *
   * @param value the string to convert
   * @return the converted object
   */
  Object convert(String value);

  /**
   * Converts an object into string, e.g. Date May 18th, 1979 to String 19790518
   *
   * @param value the object to convert
   * @return a string rapresentation of the object
   */
  String toString(Object value);

}
