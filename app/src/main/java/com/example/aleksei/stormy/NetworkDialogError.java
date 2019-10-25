package com.example.aleksei.stormy;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

public class NetworkDialogError extends DialogFragment
{
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Context context = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(R.string.error_title)
                .setMessage("I need internet access to do my job, try turning off airplane mode and check your wifi status!");

        builder.setPositiveButton(R.string.error_button_ok_text, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                getActivity().finish();
                System.exit(0);

            }
        });

        return builder.create();
    }
}
