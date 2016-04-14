/*
 * Copyright (C) 2016 The MoKee OpenSource Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class CalculatorWidget extends AppWidgetProvider {
    public final static String PREFERENCE_WIDGET_PREAMBLE = "com.android" +
            ".calculator2.CALC_WIDGET_VALUE_";
    public static final String DIGIT_0 = "com.android.calculator2.0";
    public static final String DIGIT_1 = "com.android.calculator2.1";
    public static final String DIGIT_2 = "com.android.calculator2.2";
    public static final String DIGIT_3 = "com.android.calculator2.3";
    public static final String DIGIT_4 = "com.android.calculator2.4";
    public static final String DIGIT_5 = "com.android.calculator2.5";
    public static final String DIGIT_6 = "com.android.calculator2.6";
    public static final String DIGIT_7 = "com.android.calculator2.7";
    public static final String DIGIT_8 = "com.android.calculator2.8";
    public static final String DIGIT_9 = "com.android.calculator2.9";
    public static final String DOT = "com.android.calculator2.dot";
    public static final String PLUS = "com.android.calculator2.plus";
    public static final String MINUS = "com.android.calculator2.minus";
    public static final String MUL = "com.android.calculator2.mul";
    public static final String DIV = "com.android.calculator2.div";
    public static final String EQUALS = "com.android.calculator2.equals";
    public static final String CLR = "com.android.calculator2.clear";
    public static final String DEL = "com.android.calculator2.delete";

    private boolean mClearText = false;

    private char getDecimal() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        return dfs.getDecimalSeparator();
    }

    private String addDecimal(String equation) {
        if (equation != null) {
            int index = equation.length() - 1;
            boolean foundOperator = false;
            char decimal = getDecimal();

            while (index >= 0) {
                char currChar = equation.charAt(index);

                // If decimal symbol is already present, stop the loop and
                // return back.
                // Two decimal symbols are not permitted
                if (currChar == decimal) {
                    break;
                }
                // If an operator is found, it indicates index moved before
                // the last number entry.
                // Stop the loop and add decimal
                else if (currChar == '*' || currChar == '/' ||
                        currChar == '+' || currChar == '-') {
                    foundOperator = true;
                    break;
                }
                index--;
            }

            // index would be less than zero either when input string is
            // empty or index reached beginning of the string in previous loop
            // foundOperator would be true if an operator was found while
            // traversing the string
            if (index < 0 || foundOperator) {
                equation += String.valueOf(decimal);
            }
        }
        return equation;
    }

    private boolean isOperator (char c) {
        return KeyMaps.isBinary(charToId(c));
    }

    private String addOperator(String equation, char op) {
        if (equation.length() > 0) {
            char lastChar = equation.charAt(equation.length() - 1);
            if (op != '-') {
               // Remove the previous operators if needed
                if (isOperator(lastChar) && equation.length() > 1) {
                    while (isOperator(equation.charAt(equation.length() - 1))) {
                        equation = equation.substring(0, equation.length() - 1);
                        if (equation.length() == 0) {
                            break;
                        }
                    }
                }

                // Append the new operator
                if (equation.length() > 1) {
                    equation += op;
                }
            } else if (lastChar != '-') {
                equation += op;
            }
        } else if (op == '-') {
            equation += op;
        }

        return equation;
    }

    private int charToId(char c) {
        if (Character.isDigit(c)) {
            int i = Character.digit(c, 10);
            return KeyMaps.keyForDigVal(i);
        }
        switch (c) {
            case '.':
                return R.id.dec_point;
            case '-':
                return R.id.op_sub;
            case '+':
                return R.id.op_add;
            case '*':
                return R.id.op_mul;
            case '/':
                return R.id.op_div;
            default:
                return View.NO_ID;
        }
    }

    private void convertExpr(String value, CalculatorExpr expr) {
        for (int i = 0; i < value                   .length(); i++) {
            if (value.charAt(i) == 'E') {
                int j;
                for (j = i + 2; j < value.length(); j++) {
                    if (!Character.isDigit(value.charAt(j))) {
                        break;
                    }
                }
                if (j < value.length()) {
                    expr.addExponent(Integer.parseInt(value.substring(i + 1,
                            j)));
                    i = j - 1;
                } else {
                    expr.addExponent(Integer.parseInt(value.substring(i + 1)));
                    break;
                }
            } else {
                expr.add(charToId(value.charAt(i)));
            }
        }
    }

    private String fmt(String value) {
        double d = Double.parseDouble(value);
        String output;
        if (d == (long) d) {
            output = Long.toString((long) d);
        } else {
            output = Double.toString(d);
        }
        if (output.length() > 9) {
            DecimalFormat df = new DecimalFormat("0.####E0");
            output = df.format(Double.parseDouble(output));
        }
        return output;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int appWidgetId = intent.getIntExtra(AppWidgetManager
                .EXTRA_APPWIDGET_ID, 0);
        String value = getValue(context, appWidgetId);
        if (value.equals(context.getResources().getString(R.string
                .error_syntax))) {
            value = "";
        }

        if (intent.getAction().equals(DIGIT_0)) {
            value += "0";
        } else if (intent.getAction().equals(DIGIT_1)) {
            value += "1";
        } else if (intent.getAction().equals(DIGIT_2)) {
            value += "2";
        } else if (intent.getAction().equals(DIGIT_3)) {
            value += "3";
        } else if (intent.getAction().equals(DIGIT_4)) {
            value += "4";
        } else if (intent.getAction().equals(DIGIT_5)) {
            value += "5";
        } else if (intent.getAction().equals(DIGIT_6)) {
            value += "6";
        } else if (intent.getAction().equals(DIGIT_7)) {
            value += "7";
        } else if (intent.getAction().equals(DIGIT_8)) {
            value += "8";
        } else if (intent.getAction().equals(DIGIT_9)) {
            value += "9";
        } else if (intent.getAction().equals(DOT)) {
            value = addDecimal(value);
        } else if (intent.getAction().equals(DIV)) {
            value = addOperator(value, '/');
        } else if (intent.getAction().equals(MUL)) {
            value = addOperator(value, '*');
        } else if (intent.getAction().equals(MINUS)) {
            value = addOperator(value, '-');
        } else if (intent.getAction().equals(PLUS)) {
            value = addOperator(value, '+');
        } else if (intent.getAction().equals(EQUALS)) {
            try {
                CalculatorExpr expr = new CalculatorExpr();
                convertExpr(value, expr);
                CalculatorExpr.EvalResult result = expr.eval(true);
                value = fmt(result.val.toString(50));
                mClearText = true;
            } catch (CalculatorExpr.SyntaxException | BoundedRational
                    .ZeroDivisionException e) {
                value = context.getResources().getString(R.string
                        .error_syntax);
            }
        } else if (intent.getAction().equals(CLR)) {
            value = "";
            mClearText = false;
        } else if (intent.getAction().equals(DEL)) {
            if (value.length() > 0) {
                value = value.substring(0, value.length() - 1);
            }
        }

        setValue(context, appWidgetId, value);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance
                (context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new
                ComponentName(context, CalculatorWidget.class));
        for (int appWidgetID : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetID);
        }
        super.onReceive(context, intent);
    }

    private static void setValue(Context context, int appWidgetId, String
            newValue) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString
                (PREFERENCE_WIDGET_PREAMBLE + appWidgetId, newValue).commit();
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        for (int appWidgetID : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetID);
        }
    }

    private void updateAppWidget(Context context, AppWidgetManager
            appWidgetManager, int appWidgetId) {
        final RemoteViews remoteViews = new RemoteViews(context
                .getPackageName(), R.layout.widget);

        String value = getValue(context, appWidgetId);

        remoteViews.setTextViewText(R.id.display, value);
        remoteViews.setViewVisibility(R.id.delete, mClearText ? View.GONE :
                View.VISIBLE);
        remoteViews.setViewVisibility(R.id.clear, mClearText ? View.VISIBLE :
                View.GONE);
        setOnClickListeners(context, appWidgetId, remoteViews);

        try {
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        } catch (Exception e) {
        }
    }

    private static String getValue(Context context, int appWidgetId) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString
                (PREFERENCE_WIDGET_PREAMBLE + appWidgetId, "");
    }

    private void setOnClickListeners(Context context, int appWidgetId,
                                     RemoteViews remoteViews) {
        final Intent intent = new Intent(context, CalculatorWidget.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        // The pending intent request code must be unique
        // Not just for these 17 buttons, but for each widget as well
        // Painful T_T Right?
        // So take the id and shift it over 5 bits (enough to store our 17
        // values)
        int shiftedAppWidgetId = appWidgetId << 5;
        // And add our button values (0-16)

        intent.setAction(DIGIT_0);
        remoteViews.setOnClickPendingIntent(R.id.digit_0, PendingIntent
                .getBroadcast(context, shiftedAppWidgetId, intent, 0));

        intent.setAction(DIGIT_1);
        remoteViews.setOnClickPendingIntent(R.id.digit_1, PendingIntent
                .getBroadcast(context, shiftedAppWidgetId + 1, intent, 0));

        intent.setAction(DIGIT_2);
        remoteViews.setOnClickPendingIntent(R.id.digit_2, PendingIntent
                .getBroadcast(context, shiftedAppWidgetId + 2, intent, 0));

        intent.setAction(DIGIT_3);
        remoteViews.setOnClickPendingIntent(R.id.digit_3, PendingIntent
                .getBroadcast(context, shiftedAppWidgetId + 3, intent, 0));

        intent.setAction(DIGIT_4);
        remoteViews.setOnClickPendingIntent(R.id.digit_4, PendingIntent
                .getBroadcast(context, shiftedAppWidgetId + 4, intent, 0));

        intent.setAction(DIGIT_5);
        remoteViews.setOnClickPendingIntent(R.id.digit_5, PendingIntent
                .getBroadcast(context, shiftedAppWidgetId + 5, intent, 0));

        intent.setAction(DIGIT_6);
        remoteViews.setOnClickPendingIntent(R.id.digit_6, PendingIntent
                .getBroadcast(context, shiftedAppWidgetId + 6, intent, 0));

        intent.setAction(DIGIT_7);
        remoteViews.setOnClickPendingIntent(R.id.digit_7, PendingIntent
                .getBroadcast(context, shiftedAppWidgetId + 7, intent, 0));

        intent.setAction(DIGIT_8);
        remoteViews.setOnClickPendingIntent(R.id.digit_8, PendingIntent
                .getBroadcast(context, shiftedAppWidgetId + 8, intent, 0));

        intent.setAction(DIGIT_9);
        remoteViews.setOnClickPendingIntent(R.id.digit_9, PendingIntent
                .getBroadcast(context, shiftedAppWidgetId + 9, intent, 0));

        intent.setAction(DOT);
        remoteViews.setOnClickPendingIntent(R.id.dec_point, PendingIntent
                .getBroadcast(context, shiftedAppWidgetId + 10, intent, 0));

        intent.setAction(DIV);
        remoteViews.setOnClickPendingIntent(R.id.op_div, PendingIntent
                .getBroadcast(context, shiftedAppWidgetId + 11, intent, 0));

        intent.setAction(MUL);
        remoteViews.setOnClickPendingIntent(R.id.op_mul, PendingIntent
                .getBroadcast(context, shiftedAppWidgetId + 12, intent, 0));

        intent.setAction(MINUS);
        remoteViews.setOnClickPendingIntent(R.id.op_sub, PendingIntent
                .getBroadcast(context, shiftedAppWidgetId + 13, intent, 0));

        intent.setAction(PLUS);
        remoteViews.setOnClickPendingIntent(R.id.op_add, PendingIntent
                .getBroadcast(context, shiftedAppWidgetId + 14, intent, 0));

        intent.setAction(EQUALS);
        remoteViews.setOnClickPendingIntent(R.id.equal, PendingIntent
                .getBroadcast(context, shiftedAppWidgetId + 15, intent, 0));

        intent.setAction(DEL);
        remoteViews.setOnClickPendingIntent(R.id.delete, PendingIntent
                .getBroadcast(context, shiftedAppWidgetId + 16, intent, 0));

        intent.setAction(CLR);
        remoteViews.setOnClickPendingIntent(R.id.clear, PendingIntent
                .getBroadcast(context, shiftedAppWidgetId + 17, intent, 0));
    }
}
