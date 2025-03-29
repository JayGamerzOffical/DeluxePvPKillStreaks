
package org.JayGamerz;

public final class ColorManager {
    private ColorManager() {
        throw new UnsupportedOperationException("This class is not meant to be instantiated.");
    }

    public static String colorize(String textToTranslate) {
        char altColorChar = '&';
        StringBuilder b = new StringBuilder();
        char[] mess = textToTranslate.toCharArray();
        boolean color = false;
        boolean hashtag = false;
        boolean doubleTag = false;
        int i = 0;

        while(i < mess.length) {
            char c = mess[i];
            if (doubleTag) {
                doubleTag = false;
                int max = i + 3;
                if (max <= mess.length) {
                    boolean match = true;

                    for(int n = i; n < max; ++n) {
                        char tmp = mess[n];
                        if ((tmp < '0' || tmp > '9') && (tmp < 'a' || tmp > 'f') && (tmp < 'A' || tmp > 'F')) {
                            match = false;
                            break;
                        }
                    }

                    if (match) {
                        b.append('§');
                        b.append('x');

                        while(i < max) {
                            char tmp = mess[i];
                            b.append('§');
                            b.append(tmp);
                            b.append('§');
                            b.append(tmp);
                            ++i;
                        }
                        continue;
                    }
                }

                b.append('&');
                b.append("##");
            }

            if (hashtag) {
                hashtag = false;
                if (c == '#') {
                    doubleTag = true;
                    ++i;
                    continue;
                }

                int max = i + 6;
                if (max <= mess.length) {
                    boolean match = true;

                    for(int n = i; n < max; ++n) {
                        char tmp = mess[n];
                        if ((tmp < '0' || tmp > '9') && (tmp < 'a' || tmp > 'f') && (tmp < 'A' || tmp > 'F')) {
                            match = false;
                            break;
                        }
                    }

                    if (match) {
                        b.append('§');
                        b.append('x');

                        while(i < max) {
                            b.append('§');
                            b.append(mess[i]);
                            ++i;
                        }
                        continue;
                    }
                }

                b.append('&');
                b.append('#');
            }

            if (color) {
                color = false;
                if (c == '#') {
                    hashtag = true;
                    ++i;
                    continue;
                }

                if (c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c == 'r' || c >= 'k' && c <= 'o' || c >= 'A' && c <= 'F' || c == 'R' || c >= 'K' && c <= 'O') {
                    b.append('§');
                    b.append(c);
                    ++i;
                    continue;
                }

                b.append('&');
            }

            if (c == '&') {
                color = true;
                ++i;
            } else {
                b.append(c);
                ++i;
            }
        }

        if (color) {
            b.append('&');
        } else if (hashtag) {
            b.append('&');
            b.append('#');
        } else if (doubleTag) {
            b.append('&');
            b.append("##");
        }

        return b.toString();
    }
}
