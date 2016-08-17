package nerd.tuxmobil.fahrplan.congress;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import org.ligi.tracedroid.TraceDroid;
import org.ligi.tracedroid.collecting.TraceDroidMetaInfo;

import de.cketti.mailto.EmailIntentBuilder;

// This class supports translation and configuration via XML files.
// The original TraceDroidEmailSender class is available here:
// https://github.com/ligi/tracedroid
public abstract class TraceDroidEmailSender {

    public static boolean sendStackTraces(@NonNull final Activity context) {
        if (TraceDroid.getStackTraceFiles().length < 1) {
            return false;
        }

        final String emailAddress = context.getString(
                R.string.config_trace_droid_email_address);
        final String dialogTitle = context.getString(
                R.string.trace_droid_dialog_title);
        final String dialogMessage = context.getString(
                R.string.trace_droid_dialog_message);
        final String buttonTitleSend = context.getString(
                R.string.trace_droid_button_title_send);
        final String buttonTitleLater = context.getString(
                R.string.trace_droid_button_title_later);
        final String buttonTitleNo = context.getString(
                R.string.trace_droid_button_title_no);
        final String sendMail = context.getString(
                R.string.trace_droid_app_chooser_title);
        final int maximumStackTracesCount = context.getResources().getInteger(
                R.integer.config_trace_droid_maximum_stack_traces_count);

        new AlertDialog.Builder(context)
                .setTitle(dialogTitle)
                .setMessage(dialogMessage)
                .setPositiveButton(buttonTitleSend, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent emailIntent = getEmailIntent(
                                context, emailAddress, maximumStackTracesCount);
                        if (emailIntent.resolveActivity(context.getPackageManager()) != null) {
                            context.startActivity(Intent.createChooser(emailIntent, sendMail));
                            TraceDroid.deleteStacktraceFiles();
                        } else {
                            String message = context.getString(R.string.trace_droid_no_email_app);
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(buttonTitleNo, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        TraceDroid.deleteStacktraceFiles();
                    }
                })
                .setNeutralButton(buttonTitleLater, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Nothing to do here
                    }
                })
                .show();
        return true;
    }

    @NonNull
    private static Intent getEmailIntent(@NonNull final Activity activity,
                                         @NonNull final String recipient,
                                         int maximumStackTracesCount) {
        return EmailIntentBuilder.from(activity)
                .to(recipient)
                .subject("[TraceDroid Report] " + TraceDroidMetaInfo.getAppPackageName())
                .body(TraceDroid.getStackTraceText(maximumStackTracesCount))
                .build();
    }

}
