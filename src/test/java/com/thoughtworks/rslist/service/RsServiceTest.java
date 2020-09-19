package com.thoughtworks.rslist.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RsServiceTest {
    RsService rsService;

    @Mock
    RsEventRepository rsEventRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    VoteRepository voteRepository;
    @Mock
    TradeRepository tradeRepository;
    LocalDateTime localDateTime;
    Vote vote;

    @BeforeEach
    void setUp() {
        initMocks(this);
        rsService = new RsService(rsEventRepository, userRepository, voteRepository, tradeRepository);
        localDateTime = LocalDateTime.now();
        vote = Vote.builder().voteNum(2).rsEventId(1).time(localDateTime).userId(1).build();
    }

    @Test
    void shouldVoteSuccess() {
        // given

        UserDto userDto =
                UserDto.builder()
                        .voteNum(5)
                        .phone("18888888888")
                        .gender("female")
                        .email("a@b.com")
                        .age(19)
                        .userName("xiaoli")
                        .id(2)
                        .build();
        RsEventDto rsEventDto =
                RsEventDto.builder()
                        .eventName("event name")
                        .id(1)
                        .keyword("keyword")
                        .voteNum(2)
                        .user(userDto)
                        .build();

        when(rsEventRepository.findById(anyInt())).thenReturn(Optional.of(rsEventDto));
        when(userRepository.findById(anyInt())).thenReturn(Optional.of(userDto));
        // when
        rsService.vote(vote, 1);
        // then
        verify(voteRepository).save(VoteDto.builder()
                .num(2)
                .localDateTime(localDateTime)
                .user(userDto)
                .rsEvent(rsEventDto)
                .build());
        verify(userRepository).save(userDto);
        verify(rsEventRepository).save(rsEventDto);
    }

    @Test
    void shouldThrowExceptionWhenUserNotExist() {
        // given
        when(rsEventRepository.findById(anyInt())).thenReturn(Optional.empty());
        when(userRepository.findById(anyInt())).thenReturn(Optional.empty());
        //when&then
        assertThrows(
                RuntimeException.class,
                () -> {
                    rsService.vote(vote, 1);
                });
    }

    @Test
    void shouldBuySuccess() {
        RsEventDto firstRsEventDto = RsEventDto.builder()
                .rank(1)
                .amount(0)
                .build();
        RsEventDto secondRsEventDto = RsEventDto.builder()
                .rank(2)
                .amount(0)
                .build();
        List<RsEventDto> rsEventDtoList = new ArrayList<>();
        rsEventDtoList.add(firstRsEventDto);
        rsEventDtoList.add(secondRsEventDto);

        Trade trade = Trade.builder()
                .amount(100)
                .rank(1)
                .build();
        RsEventDto newFirstRsEventDto = RsEventDto.builder()
                .rank(trade.getRank())
                .amount(trade.getAmount())
                .build();
        RsEventDto newSecondRsEventDto = RsEventDto.builder()
                .rank(trade.getRank())
                .amount(trade.getAmount())
                .build();
        // given
        when(rsEventRepository.findById(anyInt()))
                .thenReturn(Optional.of(secondRsEventDto));
        when(rsEventRepository.findAll()).thenReturn(rsEventDtoList);

        //when
        rsService.buy(trade, 1);

        //then
        verify(tradeRepository).save(TradeDto.builder()
                .amount(trade.getAmount())
                .rank(trade.getRank())
                .rsEventDto(newSecondRsEventDto)
                .build());
        verify(rsEventRepository).save(newSecondRsEventDto);
        verify(rsEventRepository).save(newFirstRsEventDto);
    }

    @Test
    void shouldReturnFalseWhenRsEventNotExist() {
        Trade trade = Trade.builder()
                .amount(100)
                .rank(1)
                .build();
        // given
        when(rsEventRepository.findById(anyInt()))
                .thenReturn(Optional.empty());

        //when
        //then
        assertFalse(rsService.buy(trade, 1));
    }

    @Test
    void shouldReturnFalseWhenRankBiggerThanRange() {
        RsEventDto firstRsEventDto = RsEventDto.builder()
                .rank(1)
                .amount(0)
                .build();
        RsEventDto secondRsEventDto = RsEventDto.builder()
                .rank(2)
                .amount(0)
                .build();
        List<RsEventDto> rsEventDtoList = new ArrayList<>();
        rsEventDtoList.add(firstRsEventDto);
        rsEventDtoList.add(secondRsEventDto);

        int indexBiggerThanRange = rsEventDtoList.size() + 1;

        Trade trade = Trade.builder()
                .amount(100)
                .rank(indexBiggerThanRange)
                .build();
        // given
        when(rsEventRepository.findById(anyInt()))
                .thenReturn(Optional.of(secondRsEventDto));
        when(rsEventRepository.findAll()).thenReturn(rsEventDtoList);

        //when
        //then
        assertFalse(rsService.buy(trade, 1));
    }

    @Test
    void shouldReturnFalseWhenRankLessThanRange() {
        RsEventDto firstRsEventDto = RsEventDto.builder()
                .rank(1)
                .amount(0)
                .build();
        RsEventDto secondRsEventDto = RsEventDto.builder()
                .rank(2)
                .amount(0)
                .build();
        List<RsEventDto> rsEventDtoList = new ArrayList<>();
        rsEventDtoList.add(firstRsEventDto);
        rsEventDtoList.add(secondRsEventDto);

        int indexLessThanRange = 0;

        Trade trade = Trade.builder()
                .amount(100)
                .rank(indexLessThanRange)
                .build();
        // given
        when(rsEventRepository.findById(anyInt()))
                .thenReturn(Optional.of(secondRsEventDto));
        when(rsEventRepository.findAll()).thenReturn(rsEventDtoList);

        //when
        //then
        assertFalse(rsService.buy(trade, 1));
    }

    @Test
    void shouldReturnFalseWhenGivenLessAmount() {
        RsEventDto firstRsEventDto = RsEventDto.builder()
                .rank(1)
                .amount(100)
                .build();
        RsEventDto secondRsEventDto = RsEventDto.builder()
                .rank(2)
                .amount(0)
                .build();
        List<RsEventDto> rsEventDtoList = new ArrayList<>();
        rsEventDtoList.add(firstRsEventDto);
        rsEventDtoList.add(secondRsEventDto);

        int indexLessThanRange = 0;

        Trade trade = Trade.builder()
                .amount(100)
                .rank(firstRsEventDto.getRank())
                .build();
        // given
        when(rsEventRepository.findById(anyInt()))
                .thenReturn(Optional.of(secondRsEventDto));
        when(rsEventRepository.findAll()).thenReturn(rsEventDtoList);

        //when
        //then
        assertFalse(rsService.buy(trade, 1));
    }

    @Test
    void shouldReplacePurchasedRsEventWhenBuyRank() {
        RsEventDto firstRsEventDto = RsEventDto.builder()
                .rank(1)
                .amount(100)
                .build();
        RsEventDto secondRsEventDto = RsEventDto.builder()
                .rank(2)
                .amount(0)
                .build();
        List<RsEventDto> rsEventDtoList = new ArrayList<>();
        rsEventDtoList.add(firstRsEventDto);
        rsEventDtoList.add(secondRsEventDto);

        Trade trade = Trade.builder()
                .amount(101)
                .rank(firstRsEventDto.getRank())
                .build();

        RsEventDto newSecondRsEventDto = RsEventDto.builder()
                .rank(trade.getRank())
                .amount(trade.getAmount())
                .build();
        // given
        when(rsEventRepository.findById(anyInt()))
                .thenReturn(Optional.of(secondRsEventDto));
        when(rsEventRepository.findAll()).thenReturn(rsEventDtoList);

        //when
        rsService.buy(trade, 1);
        //then
        verify(rsEventRepository).deleteById(firstRsEventDto.getId());
        verify(tradeRepository).save(TradeDto.builder()
                .amount(trade.getAmount())
                .rank(trade.getRank())
                .rsEventDto(newSecondRsEventDto)
                .build());
        verify(rsEventRepository).save(newSecondRsEventDto);
    }

}
