package kr.shacon.edi.padders;

import kr.shacon.edi.Padder;

public abstract class AbstractPadder implements Padder {

  protected final char padChar;

  public AbstractPadder(char padChar) {
    this.padChar = padChar;
  }

  protected String buildPad(String string, int totalLength) {
    int lengthOfString = lengthOf(string);
    // PITEST note: the following conditional will generate a surviving mutation.
    // That's because it's just an optimization to avoid creating a StringBuilder
    // So the code works even if the `if` is removed/negated, it'll just be slower
    int padSize = totalLength - lengthOfString;
    if (padSize <= 0) {
      return "";
    }
    StringBuilder sb = new StringBuilder(padSize);
    while (padSize-- > 0) {
      sb.append(padChar);
    }
    return sb.toString();
  }

  private int lengthOf(String string) {
    if (string == null) {
      return 0;
    }
    return string.length();
  }

}
