package kr.shacon.edi.padders;

public abstract class AbstractLeftPadder extends AbstractPadder {

  public AbstractLeftPadder(char padChar) {
    super(padChar);
  }

  public String pad(String input, int totalLength) {
    if (input == null) {
      return buildPad(null, totalLength);
    }
    return buildPad(input, totalLength) + input;
  }

  public String unpad(String input) {
    if (input == null || input.isEmpty()) {
      return "";
    }

    int inputIdx = 0;
    while (inputIdx < input.length() && input.charAt(inputIdx) == padChar) {
      inputIdx++;
    }

    return input.substring(inputIdx);
  }
}
