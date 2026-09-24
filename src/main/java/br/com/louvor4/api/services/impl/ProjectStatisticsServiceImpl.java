package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.EventParticipantStatus;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.repositories.EventParticipantRepository;
import br.com.louvor4.api.repositories.projections.InviteStatsProjection;
import br.com.louvor4.api.repositories.projections.MemberScheduleStatsProjection;
import br.com.louvor4.api.services.ProjectStatisticsService;
import br.com.louvor4.api.shared.dto.Statistics.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProjectStatisticsServiceImpl implements ProjectStatisticsService {

    private static final int DEFAULT_RANGE_MONTHS = 6;
    private static final int MAX_RANGE_MONTHS = 24;
    private static final String NO_SKILL_NAME = "Sem função";

    private final EventParticipantRepository eventParticipantRepository;

    public ProjectStatisticsServiceImpl(EventParticipantRepository eventParticipantRepository) {
        this.eventParticipantRepository = eventParticipantRepository;
    }

    @Override
    public ProjectScheduleStatsResponse getScheduleStats(UUID projectId, String from, String to) {
        MonthRange range = parseRange(from, to);

        List<MemberScheduleStatsProjection> rows = eventParticipantRepository.countByMemberSkillAndMonth(
                projectId,
                EventParticipantStatus.ACCEPTED,
                range.start().atDay(1).atStartOfDay(),
                range.end().plusMonths(1).atDay(1).atStartOfDay());

        Map<UUID, List<MemberScheduleStatsProjection>> byMember = rows.stream()
                .collect(Collectors.groupingBy(MemberScheduleStatsProjection::getMemberId, LinkedHashMap::new, Collectors.toList()));

        List<MemberScheduleStatsDTO> members = byMember.values().stream()
                .map(this::toMemberScheduleStats)
                .sorted(Comparator.comparingLong(MemberScheduleStatsDTO::total).reversed()
                        .thenComparing(MemberScheduleStatsDTO::firstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        return new ProjectScheduleStatsResponse(projectId, range.start().toString(), range.end().toString(), members);
    }

    @Override
    public ProjectInviteStatsResponse getInviteStats(UUID projectId, String from, String to) {
        MonthRange range = parseRange(from, to);

        List<InviteStatsProjection> rows = eventParticipantRepository.countInvitesByMemberMonthAndStatus(
                projectId,
                range.start().atDay(1).atStartOfDay(),
                range.end().plusMonths(1).atDay(1).atStartOfDay());

        Map<YearMonth, List<InviteStatsProjection>> rowsByMonth = rows.stream()
                .collect(Collectors.groupingBy(r -> YearMonth.of(r.getEventYear(), r.getEventMonth())));

        List<MonthInviteStatsDTO> byMonth = range.months().stream()
                .map(ym -> new MonthInviteStatsDTO(ym.toString(), toInviteCounts(rowsByMonth.getOrDefault(ym, List.of()))))
                .toList();

        Map<UUID, List<InviteStatsProjection>> rowsByMember = rows.stream()
                .collect(Collectors.groupingBy(InviteStatsProjection::getMemberId, LinkedHashMap::new, Collectors.toList()));

        List<MemberInviteStatsDTO> byMember = rowsByMember.values().stream()
                .map(memberRows -> {
                    InviteStatsProjection first = memberRows.get(0);
                    return new MemberInviteStatsDTO(
                            first.getMemberId(),
                            first.getUserId(),
                            first.getFirstName(),
                            first.getLastName(),
                            first.getProfileImage(),
                            toInviteCounts(memberRows));
                })
                .sorted(Comparator.comparingLong((MemberInviteStatsDTO m) -> m.counts().sent()).reversed()
                        .thenComparing(MemberInviteStatsDTO::firstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        return new ProjectInviteStatsResponse(
                projectId,
                range.start().toString(),
                range.end().toString(),
                toInviteCounts(rows),
                byMonth,
                byMember);
    }

    private MemberScheduleStatsDTO toMemberScheduleStats(List<MemberScheduleStatsProjection> memberRows) {
        MemberScheduleStatsProjection first = memberRows.get(0);

        Map<YearMonth, List<MemberScheduleStatsProjection>> rowsByMonth = memberRows.stream()
                .collect(Collectors.groupingBy(r -> YearMonth.of(r.getEventYear(), r.getEventMonth()), TreeMap::new, Collectors.toList()));

        List<MonthScheduleStatsDTO> byMonth = rowsByMonth.entrySet().stream()
                .map(entry -> new MonthScheduleStatsDTO(
                        entry.getKey().toString(),
                        sumTotal(entry.getValue()),
                        toSkillCounts(entry.getValue())))
                .toList();

        return new MemberScheduleStatsDTO(
                first.getMemberId(),
                first.getUserId(),
                first.getFirstName(),
                first.getLastName(),
                first.getProfileImage(),
                sumTotal(memberRows),
                toSkillCounts(memberRows),
                byMonth);
    }

    private List<SkillCountDTO> toSkillCounts(List<MemberScheduleStatsProjection> rows) {
        Map<Optional<UUID>, List<MemberScheduleStatsProjection>> bySkill = rows.stream()
                .collect(Collectors.groupingBy(r -> Optional.ofNullable(r.getSkillId())));

        return bySkill.entrySet().stream()
                .map(entry -> {
                    MemberScheduleStatsProjection first = entry.getValue().get(0);
                    boolean hasSkill = entry.getKey().isPresent();
                    return new SkillCountDTO(
                            entry.getKey().orElse(null),
                            hasSkill ? first.getSkillName() : NO_SKILL_NAME,
                            hasSkill && first.getSkillIconKey() != null ? first.getSkillIconKey().name() : null,
                            sumTotal(entry.getValue()));
                })
                .sorted(Comparator.comparingLong(SkillCountDTO::total).reversed()
                        .thenComparing(SkillCountDTO::skillName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private long sumTotal(List<MemberScheduleStatsProjection> rows) {
        return rows.stream().mapToLong(MemberScheduleStatsProjection::getTotal).sum();
    }

    private InviteCountsDTO toInviteCounts(List<InviteStatsProjection> rows) {
        long accepted = 0, declined = 0, pending = 0;
        for (InviteStatsProjection row : rows) {
            switch (row.getStatus()) {
                case ACCEPTED -> accepted += row.getTotal();
                case DECLINED -> declined += row.getTotal();
                case PENDING -> pending += row.getTotal();
            }
        }
        long sent = accepted + declined + pending;
        double acceptanceRate = sent == 0 ? 0.0 : (double) accepted / sent;
        return new InviteCountsDTO(sent, accepted, declined, pending, acceptanceRate);
    }

    private MonthRange parseRange(String from, String to) {
        YearMonth end = isBlank(to) ? YearMonth.now() : parseYearMonth(to, "to");
        YearMonth start = isBlank(from) ? end.minusMonths(DEFAULT_RANGE_MONTHS - 1) : parseYearMonth(from, "from");

        if (start.isAfter(end)) {
            throw new ValidationException("O parâmetro 'from' deve ser anterior ou igual a 'to'.");
        }
        if (ChronoUnit.MONTHS.between(start, end) + 1 > MAX_RANGE_MONTHS) {
            throw new ValidationException("O período máximo permitido é de " + MAX_RANGE_MONTHS + " meses.");
        }
        return new MonthRange(start, end);
    }

    private YearMonth parseYearMonth(String value, String paramName) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Formato inválido para '" + paramName + "'. Use YYYY-MM (ex: 2026-02)");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record MonthRange(YearMonth start, YearMonth end) {
        List<YearMonth> months() {
            List<YearMonth> months = new ArrayList<>();
            for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
                months.add(ym);
            }
            return months;
        }
    }
}
