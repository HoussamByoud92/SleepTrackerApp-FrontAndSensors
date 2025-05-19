package com.sleeptracker.analysis;

import android.content.Context;
import android.util.Log;

import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.sleeptracker.model.SleepInsight;
import com.sleeptracker.model.SleepSession;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SleepAnalysisManager {
    private static final String TAG = "SleepAnalysisManager";
    private final Context context;
    private final SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public SleepAnalysisManager(Context context) {
        this.context = context;
    }

    /**
     * Analyzes a sleep session and returns the sleep quality score (0-100)
     */
    public int calculateSleepQualityScore(SleepSession session) {
        try {
            // Parse session data
            Date startDate = sdfInput.parse(session.getStart());
            Date endDate = sdfInput.parse(session.getStop());

            if (startDate == null || endDate == null) {
                return 0;
            }

            // Calculate total sleep duration in minutes
            long durationMinutes = TimeUnit.MILLISECONDS.toMinutes(endDate.getTime() - startDate.getTime());

            // Parse events
            List<String> events = parseEvents(session.getEvents());

            // Count different types of events
            int soundEvents = 0;
            int movementEvents = 0;

            for (String event : events) {
                if (event.contains("Sound detected")) {
                    soundEvents++;
                } else if (event.contains("Movement detected")) {
                    movementEvents++;
                }
            }

            // Calculate disturbance ratio (events per hour)
            double hoursSlept = durationMinutes / 60.0;
            double disturbanceRatio = (soundEvents + movementEvents) / hoursSlept;

            // Calculate base score
            int baseScore = 100;

            // Deduct points for disturbances
            int disturbanceDeduction = (int) Math.min(40, disturbanceRatio * 5);

            // Deduct points for suboptimal sleep duration
            int durationDeduction = 0;
            if (durationMinutes < 360) { // Less than 6 hours
                durationDeduction = (int) ((360 - durationMinutes) / 6);
            } else if (durationMinutes > 600) { // More than 10 hours
                durationDeduction = (int) ((durationMinutes - 600) / 12);
            }
            durationDeduction = Math.min(30, durationDeduction);

            // Calculate final score
            int finalScore = baseScore - disturbanceDeduction - durationDeduction;

            // Ensure score is between 0 and 100
            return Math.max(0, Math.min(100, finalScore));

        } catch (ParseException e) {
            Log.e(TAG, "Error parsing dates", e);
            return 0;
        }
    }

    /**
     * Returns a description of the sleep quality based on the score
     */
    public String getSleepQualityDescription(int score) {
        if (score >= 90) {
            return "Excellent";
        } else if (score >= 75) {
            return "Good";
        } else if (score >= 60) {
            return "Fair";
        } else if (score >= 40) {
            return "Poor";
        } else {
            return "Very Poor";
        }
    }

    /**
     * Analyzes sleep stages based on movement patterns
     */
    public PieData analyzeSleepStages(SleepSession session) {
        try {
            // Parse session data
            Date startDate = sdfInput.parse(session.getStart());
            Date endDate = sdfInput.parse(session.getStop());

            if (startDate == null || endDate == null) {
                return createEmptyPieData();
            }

            // Calculate total sleep duration in minutes
            long durationMinutes = TimeUnit.MILLISECONDS.toMinutes(endDate.getTime() - startDate.getTime());

            // Parse events
            List<String> events = parseEvents(session.getEvents());

            // Map to store events by hour
            Map<Integer, Integer> eventsByHour = new HashMap<>();

            // Parse event times and count events per hour
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

            for (String event : events) {
                try {
                    String timeStr = null;

                    if (event.contains("Sound detected at ")) {
                        timeStr = event.substring("Sound detected at ".length(),
                                event.indexOf(" (Amplitude"));
                    } else if (event.contains("Movement detected at ")) {
                        timeStr = event.substring("Movement detected at ".length(),
                                event.indexOf(" (Intensity"));
                    }

                    if (timeStr != null) {
                        Date eventTime = timeFormat.parse(timeStr);
                        if (eventTime != null) {
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(eventTime);
                            int hour = cal.get(Calendar.HOUR_OF_DAY);

                            eventsByHour.put(hour, eventsByHour.getOrDefault(hour, 0) + 1);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing event time: " + event, e);
                }
            }

            // Estimate sleep stages based on movement patterns
            // This is a simplified model - in reality, sleep stages would require EEG data

            // Calculate total sleep time in minutes
            double deepSleepPercent = 0.25;  // Typical deep sleep is 20-25% of total sleep
            double remSleepPercent = 0.20;   // Typical REM sleep is 20-25% of total sleep

            // Adjust percentages based on disturbances
            int totalEvents = events.size();
            if (totalEvents > 0) {
                // More events generally means less deep sleep
                double eventRatio = Math.min(1.0, totalEvents / (durationMinutes / 60.0) / 5.0);
                deepSleepPercent = Math.max(0.10, deepSleepPercent - (eventRatio * 0.15));
                remSleepPercent = Math.max(0.15, remSleepPercent - (eventRatio * 0.05));
            }

            double lightSleepPercent = 1.0 - deepSleepPercent - remSleepPercent;

            // Create pie chart data
            List<PieEntry> entries = new ArrayList<>();
            entries.add(new PieEntry((float)(deepSleepPercent * 100), "Deep Sleep"));
            entries.add(new PieEntry((float)(remSleepPercent * 100), "REM Sleep"));
            entries.add(new PieEntry((float)(lightSleepPercent * 100), "Light Sleep"));

            PieDataSet dataSet = new PieDataSet(entries, "Sleep Stages");

            // Set colors
            int[] colors = new int[]{
                    ColorTemplate.rgb("#304FFE"),  // Deep sleep - dark blue
                    ColorTemplate.rgb("#AA00FF"),  // REM sleep - purple
                    ColorTemplate.rgb("#29B6F6")   // Light sleep - light blue
            };
            dataSet.setColors(colors);

            // Configure dataset
            dataSet.setSliceSpace(3f);
            dataSet.setSelectionShift(5f);
            dataSet.setValueTextSize(12f);
            dataSet.setValueTextColor(ColorTemplate.rgb("#FFFFFF"));

            return new PieData(dataSet);

        } catch (ParseException e) {
            Log.e(TAG, "Error parsing dates", e);
            return createEmptyPieData();
        }
    }

    /**
     * Creates empty pie data for error cases
     */
    private PieData createEmptyPieData() {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(100f, "No Data"));

        PieDataSet dataSet = new PieDataSet(entries, "Sleep Stages");
        dataSet.setColor(ColorTemplate.rgb("#CCCCCC"));

        return new PieData(dataSet);
    }

    /**
     * Generates AI insights based on sleep data
     */
    public List<SleepInsight> generateInsights(SleepSession session) {
        List<SleepInsight> insights = new ArrayList<>();

        try {
            // Parse session data
            Date startDate = sdfInput.parse(session.getStart());
            Date endDate = sdfInput.parse(session.getStop());

            if (startDate == null || endDate == null) {
                insights.add(new SleepInsight("Error", "Could not analyze sleep data due to invalid dates."));
                return insights;
            }

            // Calculate sleep duration
            long durationMinutes = TimeUnit.MILLISECONDS.toMinutes(endDate.getTime() - startDate.getTime());
            long hours = durationMinutes / 60;
            long mins = durationMinutes % 60;

            // Parse events
            List<String> events = parseEvents(session.getEvents());
            int soundEvents = 0;
            int movementEvents = 0;

            for (String event : events) {
                if (event.contains("Sound detected")) {
                    soundEvents++;
                } else if (event.contains("Movement detected")) {
                    movementEvents++;
                }
            }

            // Generate insights based on sleep duration
            if (durationMinutes < 360) { // Less than 6 hours
                insights.add(new SleepInsight("Sleep Duration",
                        "Your sleep duration of " + hours + "h " + mins + "m is below the recommended 7-9 hours. " +
                                "Chronic sleep deprivation can affect mood, cognitive function, and overall health."));
            } else if (durationMinutes > 600) { // More than 10 hours
                insights.add(new SleepInsight("Sleep Duration",
                        "Your sleep duration of " + hours + "h " + mins + "m is longer than average. " +
                                "While occasional long sleep is normal, consistently sleeping more than 9 hours " +
                                "might be associated with certain health conditions."));
            } else {
                insights.add(new SleepInsight("Sleep Duration",
                        "Your sleep duration of " + hours + "h " + mins + "m falls within the recommended range of 7-9 hours."));
            }

            // Generate insights based on disturbances
            double disturbancesPerHour = (soundEvents + movementEvents) / (durationMinutes / 60.0);

            if (disturbancesPerHour > 5) {
                insights.add(new SleepInsight("Sleep Disturbances",
                        "Your sleep was frequently disturbed with " + (soundEvents + movementEvents) +
                                " events detected. Consider improving your sleep environment to reduce noise and movement."));
            } else if (disturbancesPerHour > 2) {
                insights.add(new SleepInsight("Sleep Disturbances",
                        "Your sleep had moderate disturbances with " + (soundEvents + movementEvents) +
                                " events detected. Some disturbances are normal during sleep."));
            } else {
                insights.add(new SleepInsight("Sleep Disturbances",
                        "Your sleep had minimal disturbances with only " + (soundEvents + movementEvents) +
                                " events detected. This suggests a calm sleep environment."));
            }

            // Add insight about sleep quality score
            int sleepScore = calculateSleepQualityScore(session);
            String qualityDesc = getSleepQualityDescription(sleepScore);

            insights.add(new SleepInsight("Sleep Quality",
                    "Your overall sleep quality score is " + sleepScore + " (" + qualityDesc + "). " +
                            "This score considers your sleep duration, disturbances, and movement patterns."));

            return insights;

        } catch (ParseException e) {
            Log.e(TAG, "Error parsing dates", e);
            insights.add(new SleepInsight("Error", "Could not analyze sleep data due to date parsing error."));
            return insights;
        }
    }

    /**
     * Parse events from JSON string
     */
    private List<String> parseEvents(String eventsJson) {
        List<String> eventsList = new ArrayList<>();

        try {
            if (eventsJson != null && !eventsJson.equals("[]")) {
                JSONArray jsonArray = new JSONArray(eventsJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    eventsList.add(jsonArray.getString(i));
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing events JSON", e);
        }

        return eventsList;
    }
}