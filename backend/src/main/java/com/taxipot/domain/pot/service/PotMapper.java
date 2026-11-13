package com.taxipot.domain.pot.service;

import com.taxipot.domain.pot.dto.JoinRequestResponse;
import com.taxipot.domain.pot.dto.PotDetailResponse;
import com.taxipot.domain.pot.dto.PotMemberResponse;
import com.taxipot.domain.pot.dto.PotSummaryResponse;
import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.entity.TaxipotMember;
import com.taxipot.domain.pot.repository.TaxipotMemberRepository;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.user.repository.UserRepository;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.time.Kst;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 택시팟 Entity → 응답 DTO. 회원 닉네임·평점은 USERS 를 한 번에 읽어 채운다 */
@Component
@RequiredArgsConstructor
public class PotMapper {

    /** 상세 members 에 넣는 상태 (REQUESTED 대기자는 제외, 명세) */
    static final List<PotMemberStatus> ACTIVE = List.of(PotMemberStatus.JOINED, PotMemberStatus.BOARDED);

    private final UserRepository userRepository;
    private final TaxipotMemberRepository memberRepository;
    private final PotPolicy potPolicy;

    public Map<Long, User> usersById(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(new HashSet<>(ids)).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    /** 상세 조회·생성·수정·탑승 시작 공통 응답. viewerId 는 요청자 (myMemberStatus·openChatUrl 판단) */
    public PotDetailResponse toDetail(Taxipot pot, Long viewerId) {
        List<TaxipotMember> members = memberRepository.findAllByPotIdAndStatusInOrderByIdAsc(pot.getId(), ACTIVE);
        TaxipotMember mine = memberRepository.findByPotIdAndMemberId(pot.getId(), viewerId).orElse(null);

        Set<Long> ids = members.stream().map(TaxipotMember::getMemberId).collect(Collectors.toSet());
        ids.add(pot.getHostId());
        Map<Long, User> users = usersById(ids);
        User host = users.get(pot.getHostId());

        boolean isJoined = mine != null && mine.isActive();
        List<PotMemberResponse> memberResponses = members.stream()
                .sorted(Comparator.comparing((TaxipotMember m) -> !m.isHost()).thenComparing(TaxipotMember::getId))
                .map(m -> toMember(m, users.get(m.getMemberId())))
                .toList();

        return new PotDetailResponse(
                pot.getId(),
                pot.getTitle(),
                pot.getHostId(),
                host == null ? null : host.getNickname(),
                host == null ? null : toDouble(host.getRatingAvg()),
                pot.getDepartureName(),
                toDouble(pot.getDepartureLat()),
                toDouble(pot.getDepartureLng()),
                pot.getDestinationName(),
                toDouble(pot.getDestinationLat()),
                toDouble(pot.getDestinationLng()),
                Kst.toOffset(pot.getDepartureTime()),
                Kst.toOffset(pot.getArrivalDeadline()),
                Kst.toOffset(potPolicy.cancelDeadline(pot.getDepartureTime())),
                pot.getMaxMembers(),
                pot.getCurrentMembers(),
                pot.getGenderOption(),
                pot.getExpectedFare(),
                potPolicy.holdAmount(pot.getExpectedFare(), pot.getMaxMembers()),
                pot.getActualFare(),
                pot.getStatus(),
                pot.getHasCanceled(),
                isJoined ? pot.getOpenChatUrl() : null,   // 오픈채팅 링크는 참여자에게만
                isJoined,
                mine == null ? null : mine.getStatus(),
                pot.getReceiptImgPath() != null,
                pot.getFareClaimStatus(),
                pot.getFareRejectReason(),
                memberResponses,
                Kst.toOffset(pot.getCreatedAt()),
                Kst.toOffset(pot.getUpdatedAt()));
    }

    public PotMemberResponse toMember(TaxipotMember m, User user) {
        return new PotMemberResponse(
                m.getId(),
                m.getMemberId(),
                user == null ? null : user.getNickname(),
                user == null ? null : toDouble(user.getRatingAvg()),
                m.getRole(),
                m.getPreferredSeat(),
                m.getStatus(),
                Kst.toOffset(m.getArrivedAt()),
                Kst.toOffset(m.getJoinedAt()),
                Kst.toOffset(m.getCanceledAt()));
    }

    /** distanceMeters 는 lat·lng 로 조회한 경우만 값 있음 (아니면 null) */
    public PotSummaryResponse toSummary(Taxipot pot, User host, Integer distanceMeters) {
        return new PotSummaryResponse(
                pot.getId(),
                pot.getTitle(),
                pot.getHostId(),
                host == null ? null : host.getNickname(),
                host == null ? null : toDouble(host.getRatingAvg()),
                pot.getDepartureName(),
                toDouble(pot.getDepartureLat()),
                toDouble(pot.getDepartureLng()),
                pot.getDestinationName(),
                Kst.toOffset(pot.getDepartureTime()),
                pot.getMaxMembers(),
                pot.getCurrentMembers(),
                pot.getGenderOption(),
                pot.getExpectedFare(),
                potPolicy.holdAmount(pot.getExpectedFare(), pot.getMaxMembers()),
                Kst.toOffset(pot.getJoinDeadline()),
                pot.getStatus(),
                pot.getHasCanceled(),
                Kst.toOffset(pot.getCreatedAt()),
                distanceMeters);
    }

    /** 입장 요청 응답. requestedAt = 요청 시각(joined_at 에 기록) */
    public JoinRequestResponse toJoinRequest(TaxipotMember m, User user) {
        return new JoinRequestResponse(
                m.getPotId(),
                m.getMemberId(),
                user == null ? null : user.getNickname(),
                user == null ? null : toDouble(user.getRatingAvg()),
                m.getPreferredSeat(),
                m.getStatus(),
                Kst.toOffset(m.getJoinedAt()));
    }

    static Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
