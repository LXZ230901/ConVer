package org.batfish.symwork;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.JFactory;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class CommunityToBDD {
//  private static BDDFactory _bddFactory;
//
//  public CommunityToBDD (BDDFactory bddFactory)
//  {
//    _bddFactory = bddFactory;
//  }
//
//
//
//  public BDD convertCommunityBDD(String communityRegex)
//  {
//    BDD communityBDD = _bddFactory.one();
//    Pattern pattern = Pattern.compile("^([0-9]+):([0-9]+)$");
//    Matcher matcher = pattern.matcher(communityRegex);
//    if (matcher.matches())
//    {
//      // concrete
//      return concreteCommunityToBDD(communityRegex);
//    } else {
//      // regex
//
//    }
//    if (commuityString.contains("|") || commuityString.contains("[") )
//    {
//
//    }
//    return communityBDD;
//  }
//
//
//
//  public BDD concreteCommunityToBDD(String community) {
//    String[] parts = community.split(":");
//    int aa = Integer.parseInt(parts[0]);
//    int nn = Integer.parseInt(parts[1]);
//    int combined = (aa << 16) | nn; // Combine AA and NN into a 32-bit number
//
//    BDD result =_bddFactory.one();
//    for (int i = 31; i >= 0; i--) {
//      int bit = (combined >> i) & 1;
//      result = bit == 1 ? result.and(_bddFactory.ithVar(31 - i)) : result.and(_bddFactory.nithVar(31 - i));
//    }
//    return result;
//  }
//
//
//  // Convert a community regex pattern to a BDD representation
//  public static BDD convertCommunityRegexToBDD(String communityRegex) {
//    // Split the community regex into AA and NN parts
//    Pattern pattern = Pattern.compile("^([0-9\\[\\]\\*\\.\\{\\}\\|]+):([0-9\\[\\]\\*\\.\\{\\}\\|]+)$");
//    Matcher matcher = pattern.matcher(communityRegex);
//
//    if (matcher.matches()) {
//      String aaPattern = matcher.group(1);
//      String nnPattern = matcher.group(2);
//
//      BDD aaBDD = regexToBDD(aaPattern, 16, 31); // AA part (regex handled, 16 bits)
//      BDD nnBDD = regexToBDD(nnPattern, 0, 15);  // NN part (regex handled, 16 bits)
//
//      return aaBDD.and(nnBDD); // Combine AA and NN parts
//    } else {
//      throw new IllegalArgumentException("Invalid community regex: " + communityRegex);
//    }
//  }
//
//  // Convert a regex for the AA or NN part to a BDD representation
//  private static BDD regexToBDD(String regexPattern, int startBit, int endBit) {
//    // Initialize BDD to "one" (identity for AND operations)
//    BDD result = _bddFactory.one();
//
//    // Handle OR operation in the regex
//    if (regexPattern.contains("|")) {
//      String[] options = regexPattern.split("\\|");
//      BDD orResult = _bddFactory.zero(); // Initialize as zero for OR
//      for (String option : options) {
//        BDD optionBDD = regexToBDD(option, startBit, endBit); // Recursively process each option
//        orResult = orResult.or(optionBDD); // OR combine the results
//      }
//      return orResult;
//    }
//
//    // Process regex pattern character by character or group by group
//    int currentBit = startBit;
//    for (int i = 0; i < regexPattern.length(); i++) {
//      char c = regexPattern.charAt(i);
//      if (Character.isDigit(c)) {
//        int digit = Character.getNumericValue(c);
//        result = result.and(intToBDD(digit, currentBit, currentBit));
//        currentBit++;
//      } else if (c == '[') {
//        // Character range or set (e.g., [12])
//        int closingBracketIndex = regexPattern.indexOf(']', i);
//        String range = regexPattern.substring(i + 1, closingBracketIndex);
//        BDD rangeBDD = handleRange(range, currentBit);
//        result = result.and(rangeBDD);
//        currentBit++;
//        i = closingBracketIndex;
//      } else if (c == '*') {
//        // Wildcard (matches zero or more digits)
//        BDD wildcardBDD = handleWildcard(currentBit, endBit);
//        result = result.and(wildcardBDD);
//        currentBit = endBit + 1; // Move to end of range
//      } else if (c == '.') {
//        // Match any single character (dot symbol)
//        result = result.and(handleDot(currentBit));
//        currentBit++;
//      } else if (c == '{') {
//        // Repetition range (e.g., {2,3})
//        int closingBraceIndex = regexPattern.indexOf('}', i);
//        String repetition = regexPattern.substring(i + 1, closingBraceIndex);
//        BDD repetitionBDD = handleRepetition(repetition, currentBit, endBit);
//        result = result.and(repetitionBDD);
//        currentBit += getRepetitionLength(repetition);
//        i = closingBraceIndex;
//      } else {
//        throw new UnsupportedOperationException("Unsupported regex character: " + c);
//      }
//    }
//
//    return result;
//  }
//
//  // Convert an integer to BDD representation from startBit to endBit
//  private static BDD intToBDD(int value, int startBit, int endBit) {
//    BDD result = bddFactory.one();
//    for (int i = startBit; i >= endBit; i--) {
//      int bit = (value >> (i - endBit)) & 1;
//      result = bit == 1 ? result.and(bddFactory.ithVar(startBit - i)) : result.and(bddFactory.nithVar(startBit - i));
//    }
//    return result;
//  }
//
//  // Handle range or set (e.g., [12]) and return the corresponding BDD
//  private static BDD handleRange(String range, int bitIndex) {
//    BDD result = bddFactory.zero(); // OR combination
//    for (char c : range.toCharArray()) {
//      int digit = Character.getNumericValue(c);
//      result = result.or(intToBDD(digit, bitIndex, bitIndex));
//    }
//    return result;
//  }
//
//  // Handle wildcard (*) and match any number of digits starting from bitIndex to endBit
//  private static BDD handleWildcard(int startBit, int endBit) {
//    BDD result = bddFactory.one();
//    for (int i = startBit; i <= endBit; i++) {
//      result = result.and(bddFactory.one()); // Match any digit
//    }
//    return result;
//  }
//
//  // Handle dot (.) which matches any single digit
//  private static BDD handleDot(int bitIndex) {
//    return bddFactory.one(); // Matches any digit (0 or 1 in each bit)
//  }
//
//  // Handle repetition range (e.g., {2,3}) and match the specified range of digits
//  private static BDD handleRepetition(String repetition, int startBit, int endBit) {
//    String[] parts = repetition.split(",");
//    int minRepeats = Integer.parseInt(parts[0]);
//    int maxRepeats = Integer.parseInt(parts[1]);
//    BDD result = bddFactory.zero();
//    for (int i = minRepeats; i <= maxRepeats; i++) {
//      result = result.or(handleWildcard(startBit, startBit + i - 1));
//    }
//    return result;
//  }
//
//  // Get the length of a repetition (e.g., {2,3} returns 2 or 3)
//  private static int getRepetitionLength(String repetition) {
//    String[] parts = repetition.split(",");
//    return Integer.parseInt(parts[1]) - Integer.parseInt(parts[0]) + 1;
//  }
}
