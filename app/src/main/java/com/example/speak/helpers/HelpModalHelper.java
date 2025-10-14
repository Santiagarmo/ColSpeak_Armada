package com.example.speak.helpers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.speak.HelpActivity;
import com.example.speak.R;

import java.util.ArrayList;
import java.util.List;

public class HelpModalHelper {

    public interface TopicProvider {
        List<String> getAllTopics();
    }

    public static void show(Context context, String currentTopic, String level) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_help_topics, null);
            ImageView btnClose = dialogView.findViewById(R.id.btnCloseHelpModal);
            GridLayout grid = dialogView.findViewById(R.id.gridTopics);
            grid.setColumnCount(3);
            grid.setUseDefaultMargins(true);
            grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);

            if ("ALPHABET".equalsIgnoreCase(currentTopic)) {
                addSectionCard(context, grid, "Alfabeto parte 1", "ei", level);
                addSectionCard(context, grid, "Alfabeto parte 2", "i", level);
                addSectionCard(context, grid, "Alfabeto parte 3", "e", level);
                addSectionCard(context, grid, "Alfabeto parte 4", "ai", level);
                addSectionCard(context, grid, "Alfabeto parte 5", "ou", level);
                addSectionCard(context, grid, "Alfabeto parte 6", "ju", level);
                addSectionCard(context, grid, "Alfabeto parte 7", "ar", level);
            } else if ("NUMBERS".equalsIgnoreCase(currentTopic)) {
                // Solo tarjetas de números (dos secciones)
                addSectionCardForTopic(context, grid, "Números 1-10", "NUMBERS", "1-10", level);
                addSectionCardForTopic(context, grid, "Números 11-20", "NUMBERS", "11-20", level);
        } else if ("COLORS".equalsIgnoreCase(currentTopic)) {
            // Solo tarjetas de números
            addSectionCardForTopic(context, grid, "Colores Básicos", "COLORS", "colors", level);

        } else if ("PERSONAL PRONOUNS".equalsIgnoreCase(currentTopic)) {
            // Solo tarjetas de números
            addSectionCardForTopic(context, grid, "Pronombres Singulares", "PERSONAL PRONOUNS", "PRN", level);
        }
            else if ("POSSESSIVE ADJECTIVES".equalsIgnoreCase(currentTopic)) {
                // Solo tarjetas de números
                addSectionCardForTopic(context, grid, "Adjetivos Posesivos", "POSSESSIVE ADJECTIVES", "POS", level);
            }
            else {
                List<String> topics = defaultTopics();
                for (String t : topics) {
                    addTopicCard(context, grid, getReadableTopicTitle(context, t), t, level);
                }
            }

            builder.setView(dialogView);
            AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.show();
            if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        } catch (Exception ignored) {}
    }

    private static void addTopicCard(Context context, GridLayout grid, String titleText, String topicValue, String level) {
        LinearLayout card = buildCard(context, titleText);
        card.setOnClickListener(v -> HelpActivity.startFiltered(context, topicValue, level));
        grid.addView(card);
    }

    private static void addSectionCard(Context context, GridLayout grid, String titleText, String sectionKey, String level) {
        LinearLayout card = buildCard(context, titleText);
        card.setOnClickListener(v -> HelpActivity.startFilteredSection(context, "ALPHABET", level, sectionKey));
        grid.addView(card);
    }

    // Abre una sección por tema explícito (para NUMBERS)
    private static void addSectionCardForTopic(Context context, GridLayout grid, String titleText, String topic, String sectionKey, String level) {
        LinearLayout card = buildCard(context, titleText);
        card.setOnClickListener(v -> HelpActivity.startFilteredSection(context, topic, level, sectionKey));
        grid.addView(card);
    }

    private static LinearLayout buildCard(Context context, String titleText) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        lp.setMargins(12, 12, 12, 12);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        card.setLayoutParams(lp);
        card.setPadding(16, 16, 16, 16);

        float radius = 16f * context.getResources().getDisplayMetrics().density;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#FF9017"));
        bg.setCornerRadius(radius);
        card.setBackground(bg);

        ImageView icon = new ImageView(context);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(72, 72);
        ip.gravity = Gravity.CENTER_HORIZONTAL;
        icon.setLayoutParams(ip);
        icon.setImageResource(R.drawable.summary);

        TextView title = new TextView(context);
        title.setText(titleText);
        title.setTextColor(Color.WHITE);
        title.setTextSize(14);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 8, 0, 0);
        title.setMaxLines(2);

        card.addView(icon);
        card.addView(title);
        return card;
    }

    private static List<String> defaultTopics() {
        List<String> topics = new ArrayList<>();
        topics.add("ALPHABET");
        topics.add("NUMBERS");
        topics.add("COLORS");
        topics.add("PERSONAL PRONOUNS");
        topics.add("POSSESSIVE ADJECTIVES");
        topics.add("PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION");
        topics.add("ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY)");
        return topics;
    }

    private static String getReadableTopicTitle(Context c, String topic) {
        if ("ALPHABET".equals(topic)) return "Alfabeto";
        if ("NUMBERS".equals(topic)) return "Números";
        if ("COLORS".equals(topic)) return "Colores";
        if ("PERSONAL PRONOUNS".equals(topic)) return "Pronombres personales";
        if ("POSSESSIVE ADJECTIVES".equals(topic)) return "Adjetivos posesivos";
        if ("PREPOSITIONS OF PLACE, MOVEMENT AND LOCATION".equals(topic)) return "Prepositions";
        if ("ADJECTIVES (FEELINGS, APPEARANCE, PERSONALITY)".equals(topic)) return "Adjetivos";
        return topic;
    }
}


