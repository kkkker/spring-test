package com.thoughtworks.rslist.service;

import com.thoughtworks.rslist.domain.RsEvent;
import com.thoughtworks.rslist.domain.Trade;
import com.thoughtworks.rslist.domain.Vote;
import com.thoughtworks.rslist.dto.RsEventDto;
import com.thoughtworks.rslist.dto.TradeDto;
import com.thoughtworks.rslist.dto.UserDto;
import com.thoughtworks.rslist.dto.VoteDto;
import com.thoughtworks.rslist.repository.RsEventRepository;
import com.thoughtworks.rslist.repository.TradeRepository;
import com.thoughtworks.rslist.repository.UserRepository;
import com.thoughtworks.rslist.repository.VoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RsService {
    final RsEventRepository rsEventRepository;
    final UserRepository userRepository;
    final VoteRepository voteRepository;
    final TradeRepository tradeRepository;

    public RsService(RsEventRepository rsEventRepository,
                     UserRepository userRepository,
                     VoteRepository voteRepository,
                     TradeRepository tradeRepository) {
        this.rsEventRepository = rsEventRepository;
        this.userRepository = userRepository;
        this.voteRepository = voteRepository;
        this.tradeRepository = tradeRepository;
    }

    public List<RsEvent> getResponseRsEvents(List<RsEvent> rsEvents, Integer start, Integer end) {
        rsEvents.sort(Comparator.comparing(RsEvent::getRank));
        if (start == null || end == null) {
            return rsEvents;
        }
        return rsEvents.subList(start - 1, end);
    }

    public void vote(Vote vote, int rsEventId) {
        Optional<RsEventDto> rsEventDto = rsEventRepository.findById(rsEventId);
        Optional<UserDto> userDto = userRepository.findById(vote.getUserId());
        if (!rsEventDto.isPresent()
                || !userDto.isPresent()
                || vote.getVoteNum() > userDto.get().getVoteNum()) {
            throw new RuntimeException();
        }
        VoteDto voteDto = VoteDto.builder()
                .localDateTime(vote.getTime())
                .num(vote.getVoteNum())
                .rsEvent(rsEventDto.get())
                .user(userDto.get())
                .build();
        voteRepository.save(voteDto);
        UserDto user = userDto.get();
        user.setVoteNum(user.getVoteNum() - vote.getVoteNum());
        userRepository.save(user);
        RsEventDto rsEvent = rsEventDto.get();
        rsEvent.setVoteNum(rsEvent.getVoteNum() + vote.getVoteNum());
        rsEventRepository.save(rsEvent);
        List<RsEventDto> rsEventDtoList = rsEventRepository.findAll();
        updateRsEventRank(rsEventDtoList);
    }

    public boolean buy(Trade trade, int id) {
        Optional<RsEventDto> optionalRsEventDto = rsEventRepository.findById(id);
        if (!optionalRsEventDto.isPresent()) {
            return false;
        }
        RsEventDto newRsEventDto = optionalRsEventDto.get();
        List<RsEventDto> rsEventDtoList = rsEventRepository.findAll();
        if (rsEventDtoList.size() < trade.getRank() || trade.getRank() <= 0) {
            return false;
        }
        rsEventDtoList.sort(Comparator.comparing(RsEventDto::getRank));
        RsEventDto oldRsEventDto = rsEventDtoList.get(trade.getRank() - 1);
        if (trade.getAmount() <= oldRsEventDto.getAmount()) {
            return false;
        }
        if (oldRsEventDto.getAmount() > 0) {
            rsEventRepository.deleteById(oldRsEventDto.getId());
            rsEventDtoList.remove(oldRsEventDto);
        }
        rsEventDtoList.remove(newRsEventDto);
        newRsEventDto.setAmount(trade.getAmount());
        newRsEventDto.setRank(trade.getRank());
        rsEventDtoList.add(trade.getRank() - 1, newRsEventDto);
        updateRsEventRank(rsEventDtoList);
        tradeRepository.save(TradeDto.builder()
                .amount(trade.getAmount())
                .rank(trade.getRank())
                .rsEventDto(newRsEventDto)
                .build());
        return true;
    }

    private void updateRsEventRank(List<RsEventDto> rsEventDtoList) {
        List<RsEventDto> purchasedRsEvents = rsEventDtoList.stream()
                .filter(rsEventDto -> rsEventDto.getAmount() > 0)
                .sorted(Comparator.comparing(RsEventDto::getRank))
                .collect(Collectors.toList());

        List<RsEventDto> rsEvents = rsEventDtoList.stream()
                .filter(rsEventDto -> rsEventDto.getAmount() <= 0)
                .sorted(Comparator.comparing(RsEventDto::getVoteNum).reversed())
                .collect(Collectors.toList());
        for (RsEventDto purchasedRsEvent : purchasedRsEvents) {
            rsEvents.add(purchasedRsEvent.getRank() - 1, purchasedRsEvent);
        }
        rsEvents.forEach(rsEventDto -> {
            rsEventDto.setRank(rsEvents.indexOf(rsEventDto) + 1);
            rsEventRepository.save(rsEventDto);
        });
    }
}
