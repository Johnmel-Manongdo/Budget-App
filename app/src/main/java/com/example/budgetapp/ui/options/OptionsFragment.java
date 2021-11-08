package com.example.budgetapp.ui.options;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.example.budgetapp.exceptions.NumberRangeException;
import com.example.budgetapp.ui.main.ViewPagerAdapter;
import com.example.budgetapp.util.CurrencyHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.Executor;

import com.example.budgetapp.R;
import com.example.budgetapp.ui.options.categories.CustomCategoriesActivity;
import com.example.budgetapp.ui.signin.SignInActivity;
import com.example.budgetapp.firebase.FirebaseElement;
import com.example.budgetapp.firebase.FirebaseObserver;
import com.example.budgetapp.firebase.viewmodel_factories.UserProfileViewModelFactory;
import com.example.budgetapp.firebase.models.User;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class OptionsFragment extends PreferenceFragmentCompat {
    User user;
    ArrayList<Preference> preferences = new ArrayList<>();
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private TextView errorTextView;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.app_preferences);

        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(getActivity(), gso);

        Field[] fields = R.string.class.getFields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getName().startsWith("pref_key")) {
                try {
                    preferences.add(findPreference(getString((int) fields[i].get(null))));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        for (Preference preference : preferences) {
            preference.setEnabled(false);
        }

        UserProfileViewModelFactory.getModel(getUid(), getActivity()).observe(this, new FirebaseObserver<FirebaseElement<User>>() {

            @Override
            public void onChanged(FirebaseElement<User> element) {
                if (!element.hasNoError()) return;
                OptionsFragment.this.user = element.getElement();
                dataUpdated();
            }
        });


        Preference logoutPreference = findPreference(getString(R.string.pref_key_logout));
        logoutPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                createLogoutDialog(getActivity());
                return true;
            }
        });

        Preference customCategoriesPreference = findPreference(getString(R.string.pref_key_custom_categories));
        customCategoriesPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                getActivity().startActivity(new Intent(getActivity(), CustomCategoriesActivity.class));
                return true;
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
    }

    private void dataUpdated() {
        for (Preference preference : preferences) {
            preference.setEnabled(true);
        }

        Preference currencyPreference = findPreference(getString(R.string.pref_key_currency));
        currencyPreference.setSummary(user.currency.symbol);
        currencyPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setTitle("Set Currency");
                View layout = getLayoutInflater().inflate(R.layout.set_currency_dialog, null);

                TextInputEditText currencyEditText = layout.findViewById(R.id.currency_edittext);
                currencyEditText.setText(user.currency.symbol);

                alert.setView(layout);
                alert.setNegativeButton("Cancel", null);

                alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        user.currency.symbol = currencyEditText.getText().toString();
                        saveUser(user);
                    }
                });
                alert.create().show();
                return true;
            }
        });


        {
            Preference counterTypePreference = findPreference(getString(R.string.pref_key_counter_type));
            View layout = getLayoutInflater().inflate(R.layout.set_counter_type_dialog, null);
            RadioGroup radioGroup = layout.findViewById(R.id.radio_group);
            counterTypePreference.setSummary(((RadioButton) radioGroup.getChildAt(user.userSettings.homeCounterType)).getText());
            counterTypePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    View layout = getLayoutInflater().inflate(R.layout.set_counter_type_dialog, null);
                    RadioGroup radioGroup = layout.findViewById(R.id.radio_group);
                    AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                    alert.setTitle("Set type:");
                    ((RadioButton) radioGroup.getChildAt(user.userSettings.homeCounterType)).setChecked(true);
                    alert.setView(layout);
                    alert.setNegativeButton("Cancel", null);
                    alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            int index = radioGroup.indexOfChild(layout.findViewById(radioGroup.getCheckedRadioButtonId()));
                            user.userSettings.homeCounterType = index;
                            saveUser(user);
                        }
                    });
                    alert.create().show();
                    return true;
                }
            });

        }

        {
            Preference counterTypePreference = findPreference(getString(R.string.pref_key_counter_period));
            View layout = getLayoutInflater().inflate(R.layout.set_counter_period_dialog, null);
            RadioGroup radioGroup = layout.findViewById(R.id.radio_group);
            counterTypePreference.setSummary(((RadioButton) radioGroup.getChildAt(user.userSettings.homeCounterPeriod)).getText());
            counterTypePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                    alert.setTitle("Set period:");
                    View layout = getLayoutInflater().inflate(R.layout.set_counter_period_dialog, null);
                    RadioGroup radioGroup = layout.findViewById(R.id.radio_group);
                    ((RadioButton) radioGroup.getChildAt(user.userSettings.homeCounterPeriod)).setChecked(true);
                    alert.setView(layout);
                    alert.setNegativeButton("Cancel", null);
                    alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            int index = radioGroup.indexOfChild(layout.findViewById(radioGroup.getCheckedRadioButtonId()));
                            user.userSettings.homeCounterPeriod = index;
                            saveUser(user);
                        }
                    });
                    alert.create().show();
                    return true;
                }
            });

        }

        Preference limitPreference = findPreference(getString(R.string.pref_key_limit));
        limitPreference.setSummary(CurrencyHelper.formatCurrency(user.currency, user.userSettings.limit));
        limitPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setTitle("Set limit:");
                View layout = getLayoutInflater().inflate(R.layout.set_limit_dialog, null);
                TextInputEditText editText = layout.findViewById(R.id.edittext);
                CurrencyHelper.setupAmountEditText(editText, user);
                alert.setView(layout);
                alert.setNegativeButton("Cancel", null);
                alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        user.userSettings.limit = CurrencyHelper.convertAmountStringToLong(editText.getText().toString());
                        saveUser(user);
                    }
                });
                AlertDialog alertDialog = alert.create();
                alertDialog.show();
                return true;
            }
        });
    }

    private String getDayString(int dayOfWeek) {
        switch (dayOfWeek) {
            case 0:
                return "Monday";
            case 1:
                return "Tuesday";
            case 2:
                return "Wednesday";
            case 3:
                return "Thursday";
            case 4:
                return "Friday";
            case 5:
                return "Saturday";
            case 6:
                return "Sunday";
        }
        return "";
    }

    private void createLogoutDialog(Context context) {
        new android.app.AlertDialog.Builder(context)
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        mGoogleSignInClient.signOut().addOnCompleteListener(getActivity(), new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                mAuth.signOut();
                                Intent intent = new Intent(getActivity(), SignInActivity.class);
                                Toast.makeText(getActivity(), "Logout Successful", Toast.LENGTH_SHORT).show();
                                startActivity(intent);
                            }
                        });
                        dialog.dismiss();
                    }

                })

                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();

                    }
                })
                .create().show();
    }

    private void saveUser(User user) {
        UserProfileViewModelFactory.saveModel(getUid(), user);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

    }

    public String getUid() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

}


