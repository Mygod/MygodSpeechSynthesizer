package tk.mygod.speech.synthesizer;

import android.content.Context;
import android.view.MenuItem;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Locale;

/**
 * This thing is so complex that I have to split it into a separate class to make things clear.
 * @author Mygod
 */
final class StyleIdParser {
    public String Tag, Toast;
    public int Selection;

    public StyleIdParser(Context context, MenuItem item, CharSequence selection) {
        boolean attribute = false;
        switch (item.getGroupId() | item.getItemId()) {
            case R.id.action_tts_cardinal:
                Tag = "cardinal number=\"\"";
                Toast = context.getString(R.string.action_tts_number_toast);
                break;
            case R.id.action_tts_date:
                try {
                    Calendar calendar = Calendar.getInstance();
                    Locale locale = Locale.getDefault();
                    String str = selection.toString();
                    int i = 0;
                    while (i < 4) try {
                        calendar.setTime(DateFormat.getDateInstance(i++, locale).parse(str));
                        break;
                    } catch (ParseException ignore) { }
                    if (i >= 4) throw new Exception();
                    Tag = String.format("date year=\"%s\" month=\"%s\" day=\"%s\" weekday=\"%s\"",
                            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.DAY_OF_WEEK));
                } catch (Exception ignore) {
                    Tag = "date year=\"\" month=\"\" day=\"\" weekday=\"\"";
                    Toast = context.getString(R.string.action_tts_date_toast);
                }
                break;
            case R.id.action_tts_decimal:
                try {   // stolen from TtsSpan.DecimalBuilder.setArgumentsFromDouble(double, int, int)
                    NumberFormat formatter = NumberFormat.getInstance(Locale.US);
                    formatter.setMinimumFractionDigits(0);
                    formatter.setMaximumFractionDigits(15);
                    formatter.setGroupingUsed(false);
                    String str = formatter.format(Double.parseDouble(selection.toString()));
                    int i = str.indexOf('.');
                    Tag = String.format("decimal integer_part=\"%s\" fractional_part=\"%s\"",
                            i < 0 ? str : str.substring(0, i), i < 0 ? "" : str.substring(i + 1));
                } catch (NumberFormatException ignore) {
                    Tag = "decimal integer_part=\"\" fractional_part=\"\"";
                    Toast = context.getString(R.string.action_tts_decimal_toast);
                }
                break;
            case R.id.action_tts_digits:
                Tag = "digits digits=\"\"";
                Toast = context.getString(selection.length() == 0
                        ? R.string.action_tts_digits_toast_empty : R.string.action_tts_digits_toast);
                break;
            case R.id.action_tts_electronic:
                try {
                    URI uri = new URI(selection.toString());
                    StringBuilder tagBuilder = new StringBuilder("electronic");
                    String temp = uri.getScheme();
                    if (temp != null) tagBuilder.append(String.format(" protocol=\"%s\"", temp));
                    if ((temp = uri.getRawUserInfo()) != null) {
                        int i = temp.indexOf(':');
                        if (i < 0) tagBuilder.append(String.format(" username=\"%s\"", temp));
                        else tagBuilder.append(String.format(" username=\"%s\" password=\"%s\"", temp.indexOf(0, i),
                                temp.indexOf(i + 1)));
                    }
                    if ((temp = uri.getHost()) != null) tagBuilder.append(String.format(" domain=\"%s\"", temp));
                    int port = uri.getPort();
                    if (port >= 0) tagBuilder.append(String.format(" port=\"%s\"", port));
                    if ((temp = uri.getRawPath()) != null) tagBuilder.append(String.format(" path=\"%s\"", temp));
                    if ((temp = uri.getRawQuery()) != null)
                        tagBuilder.append(String.format(" query_string=\"%s\"", temp));
                    if ((temp = uri.getRawFragment()) != null)
                        tagBuilder.append(String.format(" fragment_id=\"%s\"", temp));
                    Tag = tagBuilder.toString();
                } catch (URISyntaxException ignore) {
                    Tag = "electronic protocol=\"\" username=\"\" password=\"\" domain=\"\" port=\"\" path=\"\" " +
                            "query_string=\"\" fragment_id=\"\"";
                    Toast = context.getString(R.string.action_tts_electronic_toast);
                }
                break;
            case R.id.action_tts_fraction:
                Tag = "fraction numerator=\"\" denominator=\"\" integer_part=\"\"";
                Toast = context.getString(R.string.action_tts_fraction_toast);
                break;
            case R.id.action_tts_measure:
                Tag = "measure number=\"\" integer_part=\"\" fractional_part=\"\" numerator=\"\" denominator=\"\" " +
                        "unit=\"\"";
                Toast = context.getString(R.string.action_tts_measure_toast);
                break;
            case R.id.action_tts_money:
                Tag = "money integer_part=\"\" fractional_part=\"\" currency=\"\" quantity=\"\"";
                Toast = context.getString(R.string.action_tts_money_toast);
                break;
            case R.id.action_tts_ordinal:
                Tag = "ordinal number=\"\"";
                Toast = context.getString(R.string.action_tts_number_toast);
                break;
            case R.id.action_tts_telephone:
                Tag = "telephone number_parts=\"\" country_code=\"\" extension=\"\"";
                Toast = context.getString(R.string.action_tts_telephone_toast);
                break;
            case R.id.action_tts_text:
                Tag = "text text=\"\"";
                Toast = context.getString(selection.length() == 0
                        ? R.string.action_tts_text_toast_empty : R.string.action_tts_text_toast);
                break;
            case R.id.action_tts_time:
                try {
                    Calendar calendar = Calendar.getInstance();
                    Locale locale = Locale.getDefault();
                    String str = selection.toString();
                    int i = 0;
                    while (i < 4) try {
                        calendar.setTime(DateFormat.getTimeInstance(i++, locale).parse(str));
                        break;
                    } catch (ParseException ignore) { }
                    if (i >= 4) throw new Exception();
                    Tag = String.format("time hours=\"%s\" minutes=\"%s\"",
                            calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
                } catch (Exception ignore) {
                    Tag = "time hours=\"\" minutes=\"\"";
                    Toast = context.getString(R.string.action_tts_time_toast);
                }
                break;
            case R.id.action_tts_verbatim:
                Tag = "verbatim verbatim=\"\"";
                Toast = context.getString(R.string.action_tts_verbatim_toast);
                break;
            case R.id.action_tts_generic_attributes_gender:
                Tag = String.format(" gender=\"%s\"", item.getTitleCondensed());
                attribute = true;
                break;
            case R.id.action_tts_generic_attributes_animacy:
                Tag = String.format(" animacy=\"%s\"", item.getTitleCondensed());
                attribute = true;
                break;
            case R.id.action_tts_generic_attributes_multiplicity:
                Tag = String.format(" multiplicity=\"%s\"", item.getTitleCondensed());
                attribute = true;
                break;
            case R.id.action_tts_generic_attributes_case:
                Tag = String.format(" case=\"%s\"", item.getTitleCondensed());
                attribute = true;
                break;
            case R.id.action_tts_earcon:
                Tag = "earcon";
                break;
            default:    // unknown stuff clicked
                return;
        }
        if (attribute) Selection = Tag.length();
        else {
            Selection = Tag.indexOf("\"\"") + 2;
            if (selection.length() == 0) {
                Tag = String.format("<%s />", Tag);
                if (Selection < 2) Selection = Tag.length();
            } else {
                if (Selection < 2) Selection = Tag.length() + 2;
                int i = Tag.indexOf(' ');
                Tag = String.format("<%s>%s</%s>", Tag, selection, i < 0 ? Tag : Tag.substring(0, i));
            }
        }
    }
}
