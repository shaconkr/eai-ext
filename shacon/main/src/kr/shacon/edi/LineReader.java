package kr.shacon.edi;

import java.io.Reader;

/**
 * When the {@link Unmarshaller} reads a text file, it reads it via a LineReader.<br>
 * Unless specified, the default implementation will be used, {@link SimpleLineReader}, which will read each line and pass it to the {@link Unmarshaller}<br>
 * Sometimes, text files contains lines you're not interested to, or you want to do some preprocessing before parsing them: in such cases, it's useful to provide your own implementation of LineReader
 *
 * @see SimpleLineReader
 */
public interface LineReader {

  /**
   * Reads a line from a Reader. This is where you want to add your customization
   *
   * @param reader            a reader, reading from a fixed-length file
   * @param defaultPadder     the padder to optionally use when reading a line
   * @param propertyDelimiter the delimiter used to delimit properties on a line
   * @param recordLength      the total expected length of a line
   * @param lineSeparator     the character sequence used to mark the end of a line
   * @return a string, or null if there are no more lines to read
   */
  String readLine(Reader reader, Padder defaultPadder, String propertyDelimiter, int recordLength, String lineSeparator);

}
