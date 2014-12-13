package tk.mygod.text;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.PersistableBundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TtsSpan;
import junit.framework.Assert;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Stack;

/**
 * @author Mygod
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class SsmlDroid {
    private static class Tag {
        public Tag(String name) {
            Name = name;
        }
        public Tag(TtsSpan span, String name, int pos) {
            this(name);
            Span = span;
            Position = pos;
        }

        public TtsSpan Span;
        public String Name;
        public int Position;
    }

    private static class Mapping {
        public Mapping(int s, int t) {
            SsmlPosition = s;
            TextPosition = t;
        }

        public int SsmlPosition, TextPosition;
    }

    public static class Parser extends DefaultHandler {
        private static final String[]
                months = "january,february,march,april,may,june,july,august,september,october,november,december"
                    .split(","),
                weekdays = "sunday,monday,tuesday,wednesday,thursday,friday,saturday".split(",");
        private Stack<Tag> treeStack = new Stack<Tag>();
        private Locator locator;
        private Field theCurrentLine, theCurrentColumn;
        private ArrayList<Mapping> mappings = new ArrayList<Mapping>();
        private int lineNumber, offset;
        private String source;
        private Html.TagHandler customHandler;
        private XMLReader reader;
        private boolean ignoreSingleLineBreaks;
        public SpannableStringBuilder Result = new SpannableStringBuilder();

        Parser(String src, Html.TagHandler handler, XMLReader reader, boolean ignoreSingleLineBreaks) {
            source = src;
            customHandler = handler;
            this.reader = reader;
            this.ignoreSingleLineBreaks = ignoreSingleLineBreaks;
        }

        private void addMapping(int s, int t) {
            int size = mappings.size();
            if (size > 0) { // time & space improvement
                Mapping last = mappings.get(size - 1);
                if (s == last.SsmlPosition && t == last.TextPosition) return;
            }
            mappings.add(new Mapping(s, t));
        }

        private static int parseInt(String value, String[] patterns, int directOffset, int patternOffset)
                throws SAXException {
            try {
                return Integer.parseInt(value) + directOffset;
            } catch (NumberFormatException ignored) { }
            if (patterns != null) {
                int length = patterns.length;
                for (int i = 0; i < length; ++i) if (patterns[i].equalsIgnoreCase(value)) return i + patternOffset;
            }
            throw new SAXNotRecognizedException("Unrecognized input: " + value);
        }

        @Override
        public void setDocumentLocator(Locator l) {
            Locator rollback = locator;
            try {
                Class c = (locator = l).getClass();
                (theCurrentLine = c.getDeclaredField("theCurrentLine")).setAccessible(true);
                (theCurrentColumn = c.getDeclaredField("theCurrentColumn")).setAccessible(true);
            } catch (NoSuchFieldException exc) {
                exc.printStackTrace();
                locator = rollback;
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            PersistableBundle bundle = new PersistableBundle();
            String temp, name = localName.toLowerCase();
            if ("cardinal".equals(name) || "ordinal".equals(name)) {
                if ((temp = attributes.getValue("number")) == null)
                    throw new AttributeMissingException(name + "/@number");
                bundle.putString(TtsSpan.ARG_NUMBER, temp);
            } else if ("date".equals(name)) {
                boolean month = (temp = attributes.getValue("month")) != null;
                if (month) bundle.putInt(TtsSpan.ARG_MONTH, parseInt(temp, months, 1, 0));
                if ((temp = attributes.getValue("year")) != null)
                    bundle.putInt(TtsSpan.ARG_YEAR, parseInt(temp, null, 0, 0));
                else if (!month) throw new AttributeMissingException("date/@month | @year");
                boolean day = (temp = attributes.getValue("day")) != null;
                if (day) bundle.putInt(TtsSpan.ARG_DAY, parseInt(temp, null, 0, 0));
                else if (!month) throw new AttributeMissingException("date/@month | @day");
                if ((temp = attributes.getValue("weekday")) != null)
                    bundle.putInt(TtsSpan.ARG_WEEKDAY, parseInt(temp, weekdays, 0, 1));
                else if (!day) throw new AttributeMissingException("date/@day | @weekday");
            } else if ("decimal".equals(name) || "money".equals(name)) {
                if ((temp = attributes.getValue("integer_part")) == null)
                    throw new AttributeMissingException(name + "/@integer_part");
                bundle.putString(TtsSpan.ARG_INTEGER_PART, temp);
                if ((temp = attributes.getValue("fractional_part")) == null)
                    throw new AttributeMissingException(name + "/@fractional_part");
                bundle.putString(TtsSpan.ARG_FRACTIONAL_PART, temp);
                if ((temp = attributes.getValue("currency")) != null) bundle.putString(TtsSpan.ARG_CURRENCY, temp);
                else if ("money".equals(name)) throw new AttributeMissingException("money/@fractional_part");
                if ((temp = attributes.getValue("quantity")) != null) bundle.putString(TtsSpan.ARG_QUANTITY, temp);
            } else if ("digits".equals(name)) {
                if ((temp = attributes.getValue("digits")) != null) bundle.putString(TtsSpan.ARG_DIGITS, temp);
            } else if ("electronic".equals(name)) {
                boolean notSet = true;
                if ((temp = attributes.getValue("protocol")) != null) {
                    bundle.putString(TtsSpan.ARG_PROTOCOL, temp);
                    notSet = false;
                }
                if ((temp = attributes.getValue("username")) != null) {
                    bundle.putString(TtsSpan.ARG_USERNAME, temp);
                    notSet = false;
                }
                if ((temp = attributes.getValue("password")) != null) {
                    bundle.putString(TtsSpan.ARG_PASSWORD, temp);
                    notSet = false;
                }
                if ((temp = attributes.getValue("domain")) != null) {
                    bundle.putString(TtsSpan.ARG_DOMAIN, temp);
                    notSet = false;
                }
                if ((temp = attributes.getValue("port")) != null) {
                    bundle.putInt(TtsSpan.ARG_PORT, parseInt(temp, null, 0, 0));
                    notSet = false;
                }
                if ((temp = attributes.getValue("path")) != null) {
                    bundle.putString(TtsSpan.ARG_PATH, temp);
                    notSet = false;
                }
                if ((temp = attributes.getValue("query_string")) != null) {
                    bundle.putString(TtsSpan.ARG_QUERY_STRING, temp);
                    notSet = false;
                }
                if ((temp = attributes.getValue("fragment_id")) != null) {
                    bundle.putString(TtsSpan.ARG_FRAGMENT_ID, temp);
                    notSet = false;
                }
                if (notSet) throw new AttributeMissingException("electronic/@protocol | @username | @password | " +
                        "@domain | @port | @path | @query_string | @fragment_id");
            } else if ("fraction".equals(name)) {
                if ((temp = attributes.getValue("numerator")) == null)
                    throw new AttributeMissingException("fraction/@numerator");
                bundle.putString(TtsSpan.ARG_NUMERATOR, temp);
                if ((temp = attributes.getValue("denominator")) == null)
                    throw new AttributeMissingException("fraction/@denominator");
                bundle.putString(TtsSpan.ARG_DENOMINATOR, temp);
                if ((temp = attributes.getValue("integer_part")) != null)
                    bundle.putString(TtsSpan.ARG_INTEGER_PART, temp);
            } else if ("measure".equals(name)) {
                if ((temp = attributes.getValue("number")) == null) {
                    boolean integer_part = (temp = attributes.getValue("integer_part")) != null;
                    if (integer_part) bundle.putString(TtsSpan.ARG_INTEGER_PART, temp);
                    if ((temp = attributes.getValue("fractional_part")) == null) {
                        if ((temp = attributes.getValue("numerator")) == null)
                            throw new AttributeMissingException("measure/@numerator");
                        bundle.putString(TtsSpan.ARG_NUMERATOR, temp);
                        if ((temp = attributes.getValue("denominator")) == null)
                            throw new AttributeMissingException("measure/@denominator");
                        bundle.putString(TtsSpan.ARG_DENOMINATOR, temp);
                    } else {
                        bundle.putString(TtsSpan.ARG_FRACTIONAL_PART, temp);
                        if (!integer_part) throw new AttributeMissingException("measure/@integer_part");
                    }
                } else bundle.putString(TtsSpan.ARG_NUMBER, temp);
                if ((temp = attributes.getValue("unit")) == null)
                    throw new AttributeMissingException("measure/@unit");
                bundle.putString(TtsSpan.ARG_UNIT, temp);
            } else if ("telephone".equals(name)) {
                if ((temp = attributes.getValue("number_parts")) == null)
                    throw new AttributeMissingException("telephone/@number_parts");
                bundle.putString(TtsSpan.ARG_NUMBER_PARTS, temp);
                if ((temp = attributes.getValue("country_code")) != null)
                    bundle.putString(TtsSpan.ARG_COUNTRY_CODE, temp);
                if ((temp = attributes.getValue("extension")) != null) bundle.putString(TtsSpan.ARG_EXTENSION, temp);
            } else if ("text".equals(name)) {
                if ((temp = attributes.getValue("text")) != null) bundle.putString(TtsSpan.ARG_TEXT, temp);
            } else if ("time".equals(name)) {
                if ((temp = attributes.getValue("hours")) == null) throw new AttributeMissingException("time/@hours");
                bundle.putInt(TtsSpan.ARG_HOURS, parseInt(temp, null, 0, 0));
                if ((temp = attributes.getValue("minutes")) == null)
                    throw new AttributeMissingException("time/@minutes");
                bundle.putInt(TtsSpan.ARG_MINUTES, parseInt(temp, null, 0, 0));
            } else if ("verbatim".equals(name)) {
                if ((temp = attributes.getValue("verbatim")) == null)
                    throw new AttributeMissingException("verbatim/@verbatim");
                bundle.putString(TtsSpan.ARG_VERBATIM, temp);
            } else {
                if (customHandler != null) customHandler.handleTag(true, localName, Result, reader);
                treeStack.push(new Tag(name));
                return;
            }
            if ((temp = attributes.getValue("gender")) != null)
                bundle.putString(TtsSpan.ARG_GENDER, "android." + temp);
            if ((temp = attributes.getValue("animacy")) != null)
                bundle.putString(TtsSpan.ARG_ANIMACY, "android." + temp);
            if ((temp = attributes.getValue("multiplicity")) != null)
                bundle.putString(TtsSpan.ARG_MULTIPLICITY, "android." + temp);
            if ((temp = attributes.getValue("case")) != null)
                bundle.putString(TtsSpan.ARG_CASE, "android." + temp);
            treeStack.push(new Tag(new TtsSpan("android.name." + name, bundle), name, Result.length()));
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            Tag tag = treeStack.pop();
            if (tag.Span != null)
                Result.setSpan(tag.Span, tag.Position, Result.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            else if (customHandler != null) customHandler.handleTag(false, localName, Result, reader);
        }

        @Override
        public void characters(char ch[], int start, int length) throws SAXException {
            Assert.assertNotNull(locator);
            Assert.assertEquals(start, 0);
            int line, column;
            try {
                line = (Integer) theCurrentLine.get(locator);
                column = (Integer) theCurrentColumn.get(locator);
            } catch (IllegalAccessException exc) {
                throw new SAXException(exc);
            }
            while (lineNumber < line) {
                if (source.charAt(offset) != '\n') offset = source.indexOf('\n', offset);
                if (offset++ < 0 || offset > source.length()) throw new SAXException("Line number overflow.");
                ++lineNumber;
            }
            int i = offset + column - 1, j = Result.length();
            addMapping(i - length, j);
            addMapping(i, j + length);
            for (i = 0; i < length; i++) Result.append(ignoreSingleLineBreaks && i > 0 && i < ch.length - 1 &&
                    ch[i - 1] != '\n' && ch[i] == '\n' && ch[i + 1] != '\n' ? ' ' : ch[i]);
        }

        /**
         * Get SSML offset from text offset. Takes O(log n) where n is the number of tags. Thread-safe.
         * @param textOffset Text offset.
         * @param preferLeft If there is an tag at the specified offset, go as left as possible.
         *                   Otherwise, go as right as possible.
         * @return SSML offset.
         */
        public int getSsmlOffset(int textOffset, boolean preferLeft) {
            int l = 0, r = mappings.size();
            while (l < r) {
                int mid = (l + r) >> 1;
                int pos = mappings.get(mid).TextPosition;
                if (textOffset < pos || textOffset == pos && preferLeft) r = mid;
                else l = mid + 1;
            }
            Mapping mapping = mappings.get(preferLeft ? l : l - 1);
            return mapping.SsmlPosition + textOffset - mapping.TextPosition;
        }
    }

    private SsmlDroid() {
        throw new AssertionError();
    }

    public static Parser fromSsml(String source, boolean ignoreSingleLineBreaks, Html.TagHandler customHandler)
            throws SAXException, IOException {
        XMLReader reader = XMLReaderFactory.createXMLReader("org.ccil.cowan.tagsoup.Parser");
        Parser result = new Parser(source, customHandler, reader, ignoreSingleLineBreaks);
        reader.setContentHandler(result);
        reader.parse(new InputSource(new StringReader(source)));
        return result;
    }
}