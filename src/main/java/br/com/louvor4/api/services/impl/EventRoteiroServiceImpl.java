package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.models.*;
import br.com.louvor4.api.repositories.EventProgramItemRepository;
import br.com.louvor4.api.repositories.EventRepository;
import br.com.louvor4.api.services.EventRoteiroService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class EventRoteiroServiceImpl implements EventRoteiroService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static final Color COLOR_BLACK      = new Color(20, 20, 20);
    private static final Color COLOR_SUBTITLE   = new Color(50, 50, 50);
    private static final Color COLOR_DETAIL     = new Color(90, 90, 90);
    private static final Color COLOR_SEPARATOR  = new Color(190, 190, 190);

    private final EventRepository eventRepository;
    private final EventProgramItemRepository programItemRepository;

    public EventRoteiroServiceImpl(EventRepository eventRepository,
                                   EventProgramItemRepository programItemRepository) {
        this.eventRepository = eventRepository;
        this.programItemRepository = programItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Result generatePdf(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Evento não encontrado."));

        List<EventProgramItem> items = programItemRepository.findByEventIdOrderByPositionAsc(eventId);

        try {
            byte[] pdf = buildPdf(event, items);
            return new Result(pdf, event.getTitle());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PDF do roteiro.", e);
        }
    }

    // ── PDF builder ────────────────────────────────────────────────────────────

    private byte[] buildPdf(Event event, List<EventProgramItem> items) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 45, 45, 50, 70);
        PdfWriter writer = PdfWriter.getInstance(doc, baos);

        byte[] footerLogo = loadFooterLogo();
        if (footerLogo != null) {
            writer.setPageEvent(new FooterLogoEvent(footerLogo));
        }

        doc.open();

        addHeader(doc, event);
        addHorizontalRule(doc, 14, 14);

        addBody(doc, items);

        doc.close();
        return baos.toByteArray();
    }

    private byte[] loadFooterLogo() {
        try (InputStream is = getClass().getResourceAsStream("/images/logo.png")) {
            if (is == null) return null;
            return is.readAllBytes();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static class FooterLogoEvent extends PdfPageEventHelper {
        private final byte[] logoBytes;

        FooterLogoEvent(byte[] logoBytes) {
            this.logoBytes = logoBytes;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                Image logo = Image.getInstance(logoBytes);
                logo.scaleToFit(80, 35);
                float x = (document.right() + document.left() - logo.getScaledWidth()) / 2;
                float y = document.bottom() - 45;
                logo.setAbsolutePosition(x, y);
                writer.getDirectContent().addImage(logo);
            } catch (Exception ignored) {}
        }
    }

    // ── Header ─────────────────────────────────────────────────────────────────

    private void addHeader(Document doc, Event event) throws DocumentException {
        MusicProject project = event.getMusicProject();

        // Logo
        if (project.getProfileImage() != null && !project.getProfileImage().isBlank()) {
            tryAddLogo(doc, project.getProfileImage());
        }

        // Project name
        Font projectFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, COLOR_BLACK);
        Paragraph projectName = new Paragraph(project.getName(), projectFont);
        projectName.setAlignment(Element.ALIGN_CENTER);
        projectName.setSpacingBefore(8);
        doc.add(projectName);

        // Event title
        Font eventTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, COLOR_SUBTITLE);
        Paragraph eventTitle = new Paragraph(event.getTitle(), eventTitleFont);
        eventTitle.setAlignment(Element.ALIGN_CENTER);
        eventTitle.setSpacingBefore(6);
        doc.add(eventTitle);

        // Description
        if (event.getDescription() != null && !event.getDescription().isBlank()) {
            Font descFont = FontFactory.getFont(FontFactory.HELVETICA, 11, COLOR_DETAIL);
            Paragraph desc = new Paragraph(event.getDescription(), descFont);
            desc.setAlignment(Element.ALIGN_CENTER);
            desc.setSpacingBefore(3);
            doc.add(desc);
        }

        // Date · Time
        Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 11, COLOR_DETAIL);
        String dateStr = event.getStartAt().format(DATE_FMT);
        String timeStr = event.getStartAt().format(TIME_FMT);
        Paragraph dateTime = new Paragraph(dateStr + "  ·  " + timeStr, infoFont);
        dateTime.setAlignment(Element.ALIGN_CENTER);
        dateTime.setSpacingBefore(3);
        doc.add(dateTime);
    }

    private void tryAddLogo(Document doc, String imageUrl) {
        try (InputStream is = new URL(imageUrl).openStream()) {
            byte[] bytes = is.readAllBytes();
            Image logo = Image.getInstance(bytes);
            logo.scaleToFit(90, 90);
            logo.setAlignment(Image.ALIGN_CENTER);
            doc.add(logo);
        } catch (Exception ignored) {
            // URL inválida ou imagem indisponível — omite sem quebrar o PDF
        }
    }

    // ── Body ───────────────────────────────────────────────────────────────────

    private void addBody(Document doc, List<EventProgramItem> items) throws DocumentException {
        int number = 1;
        for (EventProgramItem item : items) {
            addProgramItem(doc, item, number++);
            addHorizontalRule(doc, 8, 8);
        }
    }

    private void addProgramItem(Document doc, EventProgramItem item, int number) throws DocumentException {
        if (item.isMusic()) {
            addMusicItem(doc, item, number);
        } else if (item.isMedley()) {
            addMedleyItem(doc, item, number);
        } else if (item.isText()) {
            addTextItem(doc, item, number);
        }
    }

    private void addMusicItem(Document doc, EventProgramItem item, int number) throws DocumentException {
        EventSetlistItem setlistItem = item.getSetlistItem();
        if (setlistItem == null) return;

        Song song = setlistItem.getSong();
        if (song == null) return;

        Font boldFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, COLOR_BLACK);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11, COLOR_DETAIL);

        Paragraph title = new Paragraph(number + ". " + song.getTitle(), boldFont);
        title.setSpacingBefore(4);
        doc.add(title);

        Paragraph artist = new Paragraph("   " + song.getArtist(), normalFont);
        artist.setSpacingBefore(2);
        doc.add(artist);

        String details = buildDetailsLine(resolveKey(setlistItem), song.getBpm());
        if (details != null) {
            Paragraph detailsPara = new Paragraph("   " + details, normalFont);
            detailsPara.setSpacingBefore(2);
            doc.add(detailsPara);
        }
    }

    private void addMedleyItem(Document doc, EventProgramItem item, int number) throws DocumentException {
        EventSetlistItem setlistItem = item.getSetlistItem();
        if (setlistItem == null) return;

        Medley medley = setlistItem.getMedley();
        if (medley == null) return;

        Font boldFont    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, COLOR_BLACK);
        Font subBoldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_SUBTITLE);
        Font subFont     = FontFactory.getFont(FontFactory.HELVETICA, 11, COLOR_DETAIL);

        Paragraph heading = new Paragraph(number + ". " + medley.getName() + "  (Medley)", boldFont);
        heading.setSpacingBefore(4);
        doc.add(heading);

        for (MedleyItem mi : medley.getItems()) {
            Song song = mi.getSong();
            if (song == null) continue;

            Paragraph songLine = new Paragraph("   ↳ " + song.getTitle() + " — " + song.getArtist(), subBoldFont);
            songLine.setSpacingBefore(4);
            doc.add(songLine);

            String details = buildDetailsLine(mi.getKey(), song.getBpm());
            if (details != null) {
                Paragraph detailsLine = new Paragraph("      " + details, subFont);
                detailsLine.setSpacingBefore(1);
                doc.add(detailsLine);
            }
        }
    }

    private void addTextItem(Document doc, EventProgramItem item, int number) throws DocumentException {
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 13, COLOR_SUBTITLE);
        Paragraph title = new Paragraph(number + ". " + item.getTitle(), normalFont);
        title.setSpacingBefore(4);
        doc.add(title);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void addHorizontalRule(Document doc, float spacingBefore, float spacingAfter)
            throws DocumentException {
        Paragraph rule = new Paragraph();
        rule.setSpacingBefore(spacingBefore);
        rule.add(new Chunk(new LineSeparator(0.5f, 100f, COLOR_SEPARATOR, Element.ALIGN_CENTER, -2)));
        rule.setSpacingAfter(spacingAfter);
        doc.add(rule);
    }

    private String resolveKey(EventSetlistItem item) {
        if (item.getKey() != null) return item.getKey();
        return item.getSong() != null ? item.getSong().getKey() : null;
    }

    private String buildDetailsLine(String key, Integer bpm) {
        StringBuilder sb = new StringBuilder();
        if (key != null && !key.isBlank()) {
            sb.append("Tom: ").append(key);
        }
        if (bpm != null) {
            if (!sb.isEmpty()) sb.append("  ·  ");
            sb.append("BPM: ").append(bpm);
        }
        return sb.isEmpty() ? null : sb.toString();
    }
}
