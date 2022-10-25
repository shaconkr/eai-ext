package kr.shacon.edi;

import java.io.IOException;
import java.io.Reader;

/**
 * The default {@link LineReader} implementation. It will read all lines from a Reader,
 * using given line separator to understand when a line ends
 */
public class SimpleLineReader implements LineReader {

  public String readLine(Reader reader, Padder defaultPadder, String propertyDelimiter, int recordLength, String lineSeparator) {
    char[] lineseparatorChars = lineSeparator.toCharArray();
    int matchedPos = 0;
    int inInt;
    char inChar;
    StringBuilder outLine = new StringBuilder();
    try {
      while ((inInt = reader.read()) > 0) {
        inChar = (char) inInt;
        outLine.append(inChar);
        if (inChar == lineseparatorChars[matchedPos]) {
          matchedPos++;
          if (matchedPos == lineseparatorChars.length) {
            // We have a line end
            return outLine.replace(outLine.length() - lineseparatorChars.length, outLine.length(), "").toString();
          }
        } else {
          if (matchedPos > 0) {
            matchedPos = 0;
          }
        }
      }
      if (outLine.length() > 0) {
        return outLine.toString();
      }
      return null;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
