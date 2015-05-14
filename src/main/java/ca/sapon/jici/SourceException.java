package ca.sapon.jici;

/**
 *
 */
public class SourceException extends RuntimeException {
    private static final long serialVersionUID = 1;

    public SourceException(String error, String source, String offender, int index) {
        super(generateMessage(error, source, offender, index));
    }

    private static String generateMessage(String error, String source, String offender, int index) {
        final int line = findLine(source, index);
        // find start and end of line containing the offender
        int start = index, end = index - 1;
        while (--start >= 0 && !isLineTerminator(source.charAt(start))) {
        }
        while (++end < source.length() && !isLineTerminator(source.charAt(end))) {
        }
        source = source.substring(start + 1, end);
        index -= start;
        // build the error message with source and cursor lines
        final StringBuilder builder = new StringBuilder()
                .append("\"").append(error).append("\"")
                .append(" caused by ").append(offender)
                .append(" at line: ").append(line).append(" index: ").append(index)
                .append(" in \n").append(source).append('\n');
        for (int i = 0; i < index - 1; i++) {
            builder.append(' ');
        }
        builder.append('^');
        return builder.toString();
    }

    private static int findLine(String source, int index) {
        int line = 0;
        for (int i = 0; i <= index; i++) {
            final char c = source.charAt(i);
            if (isLineTerminator(c)) {
                i = consumeLineTerminator(source, i);
                line++;
            }
        }
        return line;
    }

    private static int consumeLineTerminator(String source, int i) {
        final char c = source.charAt(i);
        if (c == '\n') {
            // LF
            i++;
        } else if (c == '\r') {
            // CR
            if (++i < source.length() && source.charAt(i) == '\n') {
                // CR + LF
                ++i;
            }
        }
        return i;
    }

    private static boolean isLineTerminator(char c) {
        return c == '\n' || c == '\r';
    }
}
