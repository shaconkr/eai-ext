package kr.shacon.edi.padders;

public abstract class AbstractRightPadder extends AbstractPadder {

  public AbstractRightPadder(char padChar) {
    super(padChar);
  }

  public String pad(String input, int totalLength) {
    if (input == null) {
      return buildPad(null, totalLength);
    }
    return input + buildPad(input, totalLength);
  }

  public String unpad(String input) {
    if (input == null || input.isEmpty()) {
      return "";
    }

    int inputIdx = input.length() - 1;
    while (inputIdx >= 0 && input.charAt(inputIdx) == padChar) {
      inputIdx--;
    }

    return input.substring(0, inputIdx + 1);
  }
}
