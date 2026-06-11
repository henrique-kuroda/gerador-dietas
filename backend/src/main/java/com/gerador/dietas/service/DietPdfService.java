package com.gerador.dietas.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gerador.dietas.domain.DietPlan;
import com.gerador.dietas.domain.Goal;
import com.gerador.dietas.domain.ProfileSnapshot;
import com.gerador.dietas.llm.DietContent;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class DietPdfService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", new Locale("pt", "BR"))
                    .withZone(ZoneId.systemDefault());

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(15, 118, 110));
    private static final Font H2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(15, 23, 42));
    private static final Font H3 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(15, 23, 42));
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(30, 41, 59));
    private static final Font LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(71, 85, 105));
    private static final Font DISCLAIMER = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, new Color(120, 53, 15));

    private final ObjectMapper objectMapper;

    public DietPdfService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public byte[] render(DietPlan plan, ProfileSnapshot profile) {
        DietContent content = objectMapper.convertValue(plan.getContent(), new TypeReference<>() {
        });

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 48, 48, 48, 48);
        PdfWriter.getInstance(doc, out);
        doc.open();

        doc.add(new Paragraph("Plano Alimentar Personalizado", TITLE));
        doc.add(spacing(4));
        doc.add(new Paragraph("Gerado em " + DATE_FMT.format(plan.getCreatedAt()), BODY));
        doc.add(spacing(12));

        if (profile != null) {
            addProfileSection(doc, profile);
            doc.add(spacing(10));
        }
        addMetabolismSection(doc, plan);
        doc.add(spacing(10));
        addSummarySection(doc, content);
        doc.add(spacing(10));
        addMealsSection(doc, content);
        doc.add(spacing(10));
        addMacrosSection(doc, content);
        doc.add(spacing(20));
        addDisclaimer(doc);

        doc.close();
        return out.toByteArray();
    }

    private void addProfileSection(Document doc, ProfileSnapshot profile) {
        doc.add(new Paragraph("Perfil utilizado", H2));
        doc.add(spacing(6));
        PdfPTable table = twoColumnTable();
        addRow(table, "Sexo", profile.getSex().name());
        addRow(table, "Idade", profile.getAge() + " anos");
        addRow(table, "Peso", formatNumber(profile.getWeightKg()) + " kg");
        addRow(table, "Altura", formatNumber(profile.getHeightCm()) + " cm");
        addRow(table, "Nível de atividade", profile.getActivityLevel().name());
        addRow(table, "Objetivo", goalLabel(profile.getGoal()));
        addRow(table, "Refeições/dia", String.valueOf(profile.getMealsPerDay()));
        if (profile.getBodyFatPercent() != null) {
            addRow(table, "% gordura", formatNumber(profile.getBodyFatPercent()) + "%");
        }
        if (profile.getDietaryRestrictions() != null && !profile.getDietaryRestrictions().isBlank()) {
            addRow(table, "Restrições", profile.getDietaryRestrictions());
        }
        doc.add(table);
    }

    private void addMetabolismSection(Document doc, DietPlan plan) {
        doc.add(new Paragraph("Cálculo metabólico", H2));
        doc.add(spacing(6));
        PdfPTable table = twoColumnTable();
        addRow(table, "TMB", plan.getTmb() + " kcal/dia");
        addRow(table, "TDEE", plan.getTdee() + " kcal/dia");
        addRow(table, "Calorias-alvo", plan.getTargetCalories() + " kcal/dia");
        addRow(table, "Fórmula utilizada", plan.getFormulaUsed().name());
        doc.add(table);
    }

    private void addSummarySection(Document doc, DietContent content) {
        doc.add(new Paragraph("Resumo", H2));
        doc.add(spacing(4));
        doc.add(new Paragraph(content.summary(), BODY));
    }

    private void addMealsSection(Document doc, DietContent content) {
        doc.add(new Paragraph("Refeições", H2));
        doc.add(spacing(6));
        for (DietContent.Meal meal : content.meals()) {
            Paragraph header = new Paragraph();
            header.add(new Chunk(meal.name(), H3));
            header.add(new Chunk("   " + meal.calories() + " kcal", LABEL));
            doc.add(header);
            doc.add(spacing(2));

            PdfPTable table = new PdfPTable(new float[]{4, 3, 2});
            table.setWidthPercentage(100);
            headerCell(table, "Alimento");
            headerCell(table, "Porção");
            headerCell(table, "kcal");
            for (DietContent.Item item : meal.items()) {
                bodyCell(table, item.food(), Element.ALIGN_LEFT);
                bodyCell(table, item.portion(), Element.ALIGN_LEFT);
                bodyCell(table, String.valueOf(item.calories()), Element.ALIGN_RIGHT);
            }
            doc.add(table);
            doc.add(spacing(8));
        }
    }

    private void addMacrosSection(Document doc, DietContent content) {
        doc.add(new Paragraph("Macronutrientes (totais do dia)", H2));
        doc.add(spacing(6));
        PdfPTable table = twoColumnTable();
        addRow(table, "Proteínas", content.macros().proteinG() + " g");
        addRow(table, "Carboidratos", content.macros().carbsG() + " g");
        addRow(table, "Gorduras", content.macros().fatG() + " g");
        addRow(table, "Total estimado", content.totalCalories() + " kcal");
        doc.add(table);
    }

    private void addDisclaimer(Document doc) {
        Paragraph p = new Paragraph(
                "Aviso: este plano alimentar foi gerado por inteligência artificial com base nos dados " +
                "informados e tem caráter exclusivamente educativo. Não substitui consulta nem orientação " +
                "de um nutricionista ou médico habilitado.",
                DISCLAIMER
        );
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        doc.add(p);
    }

    private PdfPTable twoColumnTable() {
        PdfPTable table = new PdfPTable(new float[]{2, 5});
        table.setWidthPercentage(100);
        return table;
    }

    private void addRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, LABEL));
        labelCell.setBorderColor(new Color(226, 232, 240));
        labelCell.setPadding(5f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, BODY));
        valueCell.setBorderColor(new Color(226, 232, 240));
        valueCell.setPadding(5f);
        table.addCell(valueCell);
    }

    private void headerCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, LABEL));
        cell.setBackgroundColor(new Color(240, 253, 250));
        cell.setBorderColor(new Color(226, 232, 240));
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private void bodyCell(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BODY));
        cell.setBorderColor(new Color(226, 232, 240));
        cell.setPadding(4f);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private Paragraph spacing(float pts) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(pts);
        return p;
    }

    private String formatNumber(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private String goalLabel(Goal goal) {
        return switch (goal) {
            case AGGRESSIVE_LOSS -> "Perder peso — agressivo (-30%)";
            case LOSE_WEIGHT -> "Perder peso (-20%)";
            case MAINTAIN -> "Manter peso";
            case GAIN_MUSCLE -> "Ganhar massa (+12%)";
            case AGGRESSIVE_GAIN -> "Ganhar massa — agressivo (+20%)";
        };
    }
}
