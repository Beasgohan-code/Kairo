package com.kairo.app.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.kairo.app.MainActivity;
import com.kairo.app.R;

/** Home-screen widget that opens the Kairo workspace. */
public class KairoAppWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.kairo_widget);
            views.setTextViewText(R.id.widget_title, "Kairo");
            views.setTextViewText(R.id.widget_subtitle, "Private AI workspace");
            views.setTextViewText(R.id.widget_action, "Open");

            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pending = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_root, pending);
            views.setOnClickPendingIntent(R.id.widget_action, pending);
            appWidgetManager.updateAppWidget(id, views);
        }
    }
}
