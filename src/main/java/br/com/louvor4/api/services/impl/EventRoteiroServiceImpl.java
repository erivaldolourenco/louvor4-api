package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.models.*;
import br.com.louvor4.api.repositories.EventProgramItemRepository;
import br.com.louvor4.api.repositories.EventRepository;
import br.com.louvor4.api.services.EventRoteiroService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
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

        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{2.3f, 1f});

        header.addCell(buildEventInfoCell(event));
        header.addCell(buildProjectCell(project));

        doc.add(header);
    }

    private PdfPCell buildEventInfoCell(Event event) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingRight(12);

        Font eventTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 17, COLOR_BLACK);
        Paragraph eventTitle = new Paragraph(event.getTitle(), eventTitleFont);
        eventTitle.setAlignment(Element.ALIGN_LEFT);
        cell.addElement(eventTitle);

        if (event.getDescription() != null && !event.getDescription().isBlank()) {
            Font descFont = FontFactory.getFont(FontFactory.HELVETICA, 11, COLOR_DETAIL);
            Paragraph desc = new Paragraph(event.getDescription(), descFont);
            desc.setAlignment(Element.ALIGN_LEFT);
            desc.setSpacingBefore(4);
            cell.addElement(desc);
        }

        Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 11, COLOR_DETAIL);
        String dateStr = event.getStartAt().format(DATE_FMT);
        String timeStr = event.getStartAt().format(TIME_FMT);
        Paragraph dateTime = new Paragraph(dateStr + "  ·  " + timeStr, infoFont);
        dateTime.setAlignment(Element.ALIGN_LEFT);
        dateTime.setSpacingBefore(4);
        cell.addElement(dateTime);

        return cell;
    }

    private PdfPCell buildProjectCell(MusicProject project) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setVerticalAlignment(Element.ALIGN_TOP);

        if (project.getProfileImage() != null && !project.getProfileImage().isBlank()) {
            Image logo = tryLoadImage(project.getProfileImage());
            if (logo != null) {
                logo.scaleToFit(70, 70);
                logo.setAlignment(Image.ALIGN_RIGHT);
                cell.addElement(logo);
            }
        }

        Font projectFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_SUBTITLE);
        Paragraph projectName = new Paragraph(project.getName(), projectFont);
        projectName.setAlignment(Element.ALIGN_RIGHT);
        projectName.setSpacingBefore(4);
        cell.addElement(projectName);

        return cell;
    }

    private Image tryLoadImage(String imageUrl) {
        try (InputStream is = new URL(imageUrl).openStream()) {
            byte[] bytes = is.readAllBytes();
            return Image.getInstance(bytes);
        } catch (Exception ignored) {
            // URL inválida ou imagem indisponível — omite sem quebrar o PDF
            return null;
        }
    }

    // ── Body ───────────────────────────────────────────────────────────────────

    private void addBody(Document doc, List<EventProgramItem> items) throws DocumentException {
        int number = 1;
        for (EventProgramItem item : items) {
            addProgramItem(doc, item, number++);
            addHorizontalRule(doc, 3, 3);
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

        Font boldFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_BLACK);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, COLOR_DETAIL);

        Paragraph title = new Paragraph(number + ". " + song.getTitle(), boldFont);
        title.setSpacingBefore(3);
        doc.add(title);

        String subtitle = song.getArtist();
        String details = buildDetailsLine(resolveKey(setlistItem), song.getBpm());
        if (details != null) {
            subtitle += "   ·   " + details;
        }
        Paragraph subtitlePara = new Paragraph("   " + subtitle, normalFont);
        subtitlePara.setSpacingBefore(1);
        doc.add(subtitlePara);
    }

    private void addMedleyItem(Document doc, EventProgramItem item, int number) throws DocumentException {
        EventSetlistItem setlistItem = item.getSetlistItem();
        if (setlistItem == null) return;

        Medley medley = setlistItem.getMedley();
        if (medley == null) return;

        Font boldFont    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_BLACK);
        Font subBoldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, COLOR_SUBTITLE);

        Paragraph heading = new Paragraph(number + ". " + medley.getName() + "  (Medley)", boldFont);
        heading.setSpacingBefore(3);
        doc.add(heading);

        for (MedleyItem mi : medley.getItems()) {
            Song song = mi.getSong();
            if (song == null) continue;

            String line = song.getTitle() + " — " + song.getArtist();
            String details = buildDetailsLine(mi.getKey(), song.getBpm());
            if (details != null) {
                line += "   ·   " + details;
            }

            Paragraph songLine = new Paragraph("   ↳ " + line, subBoldFont);
            songLine.setSpacingBefore(2);
            doc.add(songLine);
        }
    }

    private void addTextItem(Document doc, EventProgramItem item, int number) throws DocumentException {
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, COLOR_SUBTITLE);
        Paragraph title = new Paragraph(number + ". " + item.getTitle(), normalFont);
        title.setSpacingBefore(3);
        doc.add(title);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void addHorizontalRule(Document doc, float spacingBefore, float spacingAfter)
            throws DocumentException {
        Paragraph rule = new Paragraph("", FontFactory.getFont(FontFactory.HELVETICA, 2));
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
