package kr.shacon.edi;

/**
 * Used when writing records to a file. Some record definitions require data to be left or right
 * padded. By default the {@link Marshaller} will left pad using spaces.
 * JRecordBind comes with 4 built-in padders that left or right pad using spaces or zeros.
 * If you need a different padding, you can specify it by implementing this interface. <br>
 * The default padder is specified in the "main" tag of the record definition, and each element can have its own specific padder
 */
public interface Padder {

  /**
   * Pads a string to the given length
   *
   * @param input  the string to pad
   * @param length the length of the final string
   * @return the padded string
   */
  String pad(String input, int length);

  String unpad(String input);

}
