package com.thoughtworks.rslist.service;

import com.thoughtworks.rslist.domain.Trade;
import com.thoughtworks.rslist.domain.Vote;
import com.thoughtworks.rslist.dto.RsEventDto;
import com.thoughtworks.rslist.dto.UserDto;
import com.thoughtworks.rslist.dto.VoteDto;
import com.thoughtworks.rslist.repository.RsEventRepository;
import com.thoughtworks.rslist.repository.UserRepository;
import com.thoughtworks.rslist.repository.VoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class RsService {
    final RsEventRepository rsEventRepository;
    final UserRepository userRepository;
    final VoteRepository voteRepository;

    public RsService(RsEventRepository rsEventRepository, UserRepository userRepository, VoteRepository voteRepository) {
        this.rsEventRepository = rsEventRepository;
        this.userRepository = userRepository;
        this.voteRepository = voteRepository;
    }

    public void vote(Vote vote, int rsEventId) {
        Optional<RsEventDto> rsEventDto = rsEventRepository.findById(rsEventId);
        Optional<UserDto> userDto = userRepository.findById(vote.getUserId());
        if (!rsEventDto.isPresent()
                || !userDto.isPresent()
                || vote.getVoteNum() > userDto.get().getVoteNum()) {
            throw new RuntimeException();
        }
        VoteDto voteDto =
                VoteDto.builder()
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
        rsEventDtoList.add(trade.getRank() - 1, newRsEventDto);
        updateRankAfter(trade.getRank() - 1, rsEventDtoList);
        return true;
    }

    private void updateRankAfter(int index, List<RsEventDto> rsEventDtoList) {
      for (int i = index; i < rsEventDtoList.size(); i++) {
        RsEventDto rsEventDto = rsEventDtoList.get(i);
        rsEventDto.setRank(i + 1);
        rsEventRepository.save(rsEventDto);
      }
    }
}
