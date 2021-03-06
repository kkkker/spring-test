package com.thoughtworks.rslist.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.rslist.domain.Trade;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RsControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    RsEventRepository rsEventRepository;
    @Autowired
    VoteRepository voteRepository;
    @Autowired
    TradeRepository tradeRepository;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        tradeRepository.deleteAll();
        voteRepository.deleteAll();
        rsEventRepository.deleteAll();
        userRepository.deleteAll();
        userDto =
                UserDto.builder()
                        .voteNum(10)
                        .phone("188888888888")
                        .gender("female")
                        .email("a@b.com")
                        .age(19)
                        .userName("idolice")
                        .build();
    }

    @Test
    public void shouldGetRsEventList() throws Exception {
        UserDto save = userRepository.save(userDto);

        RsEventDto rsEventDto =
                RsEventDto.builder().keyword("无分类").eventName("第一条事件").user(save).build();

        rsEventRepository.save(rsEventDto);

        mockMvc
                .perform(get("/rs/list"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].eventName", is("第一条事件")))
                .andExpect(jsonPath("$[0].keyword", is("无分类")))
                .andExpect(jsonPath("$[0]", not(hasKey("user"))))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldGetOneEvent() throws Exception {
        UserDto save = userRepository.save(userDto);

        RsEventDto rsEventDto =
                RsEventDto.builder().keyword("无分类").eventName("第一条事件").user(save).build();

        rsEventRepository.save(rsEventDto);
        rsEventDto = RsEventDto.builder().keyword("无分类").eventName("第二条事件").user(save).build();
        rsEventRepository.save(rsEventDto);
        mockMvc.perform(get("/rs/1")).andExpect(jsonPath("$.eventName", is("第一条事件")));
        mockMvc.perform(get("/rs/1")).andExpect(jsonPath("$.keyword", is("无分类")));
        mockMvc.perform(get("/rs/2")).andExpect(jsonPath("$.eventName", is("第二条事件")));
        mockMvc.perform(get("/rs/2")).andExpect(jsonPath("$.keyword", is("无分类")));
    }

    @Test
    public void shouldGetErrorWhenIndexInvalid() throws Exception {
        mockMvc
                .perform(get("/rs/4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("invalid index")));
    }

    @Test
    public void shouldGetRsListBetween() throws Exception {
        UserDto save = userRepository.save(userDto);

        RsEventDto rsEventDto =
                RsEventDto.builder().keyword("无分类").eventName("第一条事件").user(save).build();

        rsEventRepository.save(rsEventDto);
        rsEventDto = RsEventDto.builder().keyword("无分类").eventName("第二条事件").user(save).build();
        rsEventRepository.save(rsEventDto);
        rsEventDto = RsEventDto.builder().keyword("无分类").eventName("第三条事件").user(save).build();
        rsEventRepository.save(rsEventDto);
        mockMvc
                .perform(get("/rs/list?start=1&end=2"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].eventName", is("第一条事件")))
                .andExpect(jsonPath("$[0].keyword", is("无分类")))
                .andExpect(jsonPath("$[1].eventName", is("第二条事件")))
                .andExpect(jsonPath("$[1].keyword", is("无分类")));
        mockMvc
                .perform(get("/rs/list?start=2&end=3"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].eventName", is("第二条事件")))
                .andExpect(jsonPath("$[0].keyword", is("无分类")))
                .andExpect(jsonPath("$[1].eventName", is("第三条事件")))
                .andExpect(jsonPath("$[1].keyword", is("无分类")));
        mockMvc
                .perform(get("/rs/list?start=1&end=3"))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].keyword", is("无分类")))
                .andExpect(jsonPath("$[1].eventName", is("第二条事件")))
                .andExpect(jsonPath("$[1].keyword", is("无分类")))
                .andExpect(jsonPath("$[2].eventName", is("第三条事件")))
                .andExpect(jsonPath("$[2].keyword", is("无分类")));
    }

    @Test
    public void shouldAddRsEventWhenUserExist() throws Exception {

        UserDto save = userRepository.save(userDto);

        String jsonValue =
                "{\"eventName\":\"猪肉涨价了\",\"keyword\":\"经济\",\"userId\": " + save.getId() + "}";

        mockMvc
                .perform(post("/rs/event").content(jsonValue).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
        List<RsEventDto> all = rsEventRepository.findAll();
        assertNotNull(all);
        assertEquals(all.size(), 1);
        assertEquals(all.get(0).getEventName(), "猪肉涨价了");
        assertEquals(all.get(0).getKeyword(), "经济");
        assertEquals(all.get(0).getUser().getUserName(), save.getUserName());
        assertEquals(all.get(0).getUser().getAge(), save.getAge());
    }

    @Test
    public void shouldAddRsEventWhenUserNotExist() throws Exception {
        String jsonValue = "{\"eventName\":\"猪肉涨价了\",\"keyword\":\"经济\",\"userId\": 100}";
        mockMvc
                .perform(post("/rs/event").content(jsonValue).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldVoteSuccess() throws Exception {
        UserDto save = userRepository.save(userDto);
        RsEventDto rsEventDto =
                RsEventDto.builder().keyword("无分类").eventName("第一条事件").user(save).build();
        rsEventDto = rsEventRepository.save(rsEventDto);

        String jsonValue =
                String.format(
                        "{\"userId\":%d,\"time\":\"%s\",\"voteNum\":1}",
                        save.getId(), LocalDateTime.now().toString());
        mockMvc.perform(
                post("/rs/vote/{id}", rsEventDto.getId())
                        .content(jsonValue)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        UserDto userDto = userRepository.findById(save.getId()).get();
        RsEventDto newRsEvent = rsEventRepository.findById(rsEventDto.getId()).get();
        assertEquals(userDto.getVoteNum(), 9);
        assertEquals(newRsEvent.getVoteNum(), 1);
        List<VoteDto> voteDtos = voteRepository.findAll();
        assertEquals(voteDtos.size(), 1);
        assertEquals(voteDtos.get(0).getNum(), 1);
    }

    @Test
    void shouldBuyRsEventRank() throws Exception {
        UserDto save = userRepository.save(userDto);
        RsEventDto firstRsEventDto = RsEventDto.builder()
                .keyword("无分类")
                .eventName("第一条事件")
                .user(save)
                .rank(rsEventRepository.findAll().size() + 1)
                .amount(0)
                .build();
        firstRsEventDto = rsEventRepository.save(firstRsEventDto);
        RsEventDto secondRsEventDto = RsEventDto.builder()
                .keyword("无分类")
                .eventName("第二条事件")
                .user(save)
                .rank(rsEventRepository.findAll().size() + 1)
                .amount(0)
                .build();
        secondRsEventDto = rsEventRepository.save(secondRsEventDto);

        ObjectMapper objectMapper = new ObjectMapper();
        Trade trade = Trade.builder()
                .amount(100)
                .rank(1)
                .build();
        String json = objectMapper.writeValueAsString(trade);
        mockMvc.perform(post("/rs/buy/{id}", secondRsEventDto.getId())
                .content(json)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        RsEventDto rsEventDto = rsEventRepository.findByRank(1);
        assertEquals(secondRsEventDto.getId(), rsEventDto.getId());
        assertEquals(trade.getAmount(), rsEventDto.getAmount());
        assertEquals(secondRsEventDto.getVoteNum(), rsEventDto.getVoteNum());
        assertEquals(secondRsEventDto.getUser().getId(), rsEventDto.getUser().getId());
        assertEquals(secondRsEventDto.getEventName(), rsEventDto.getEventName());
        assertEquals(secondRsEventDto.getKeyword(), rsEventDto.getKeyword());

        rsEventDto = rsEventRepository.findByRank(2);
        assertEquals(firstRsEventDto.getId(), rsEventDto.getId());
        assertEquals(firstRsEventDto.getAmount(), rsEventDto.getAmount());
        assertEquals(firstRsEventDto.getVoteNum(), rsEventDto.getVoteNum());
        assertEquals(firstRsEventDto.getUser().getId(), rsEventDto.getUser().getId());
        assertEquals(firstRsEventDto.getEventName(), rsEventDto.getEventName());
        assertEquals(firstRsEventDto.getKeyword(), rsEventDto.getKeyword());

        List<TradeDto> tradeDtoList = tradeRepository.findAll();
        assertEquals(1, tradeDtoList.size());
        assertEquals(trade.getRank(), tradeDtoList.get(0).getRank());
        assertEquals(trade.getAmount(), tradeDtoList.get(0).getAmount());
        assertEquals(secondRsEventDto.getId(), tradeDtoList.get(0).getRsEventDto().getId());
    }

    @Test
    void shouldNotBuyRsEventRankWithWrongRsEventId() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Trade trade = Trade.builder()
                .amount(100)
                .rank(1)
                .build();
        String json = objectMapper.writeValueAsString(trade);
        int wrongId = 100;
        mockMvc.perform(post("/rs/buy/{id}", wrongId)
                .content(json)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldNotBuyRsEventRankByWrongRank() throws Exception {
        UserDto save = userRepository.save(userDto);
        RsEventDto firstRsEventDto = RsEventDto.builder()
                .keyword("无分类")
                .eventName("第一条事件")
                .user(save)
                .rank(rsEventRepository.findAll().size() + 1)
                .amount(0)
                .build();
        rsEventRepository.save(firstRsEventDto);
        RsEventDto secondRsEventDto = RsEventDto.builder()
                .keyword("无分类")
                .eventName("第二条事件")
                .user(save)
                .rank(rsEventRepository.findAll().size() + 1)
                .amount(0)
                .build();
        secondRsEventDto = rsEventRepository.save(secondRsEventDto);

        ObjectMapper objectMapper = new ObjectMapper();
        Trade trade = Trade.builder()
                .amount(100)
                .rank(3)
                .build();
        String json = objectMapper.writeValueAsString(trade);
        mockMvc.perform(post("/rs/buy/{id}", secondRsEventDto.getId())
                .content(json)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        trade = Trade.builder()
                .amount(100)
                .rank(0)
                .build();
        json = objectMapper.writeValueAsString(trade);
        mockMvc.perform(post("/rs/buy/{id}", secondRsEventDto.getId())
                .content(json)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

    }

    @Test
    void shouldNotBuyRsEventRankWithWrongAmount() throws Exception {
        UserDto save = userRepository.save(userDto);
        RsEventDto firstRsEventDto = RsEventDto.builder()
                .keyword("无分类")
                .eventName("第一条事件")
                .user(save)
                .rank(rsEventRepository.findAll().size() + 1)
                .amount(0)
                .build();
        firstRsEventDto = rsEventRepository.save(firstRsEventDto);
        RsEventDto secondRsEventDto = RsEventDto.builder()
                .keyword("无分类")
                .eventName("第二条事件")
                .user(save)
                .rank(rsEventRepository.findAll().size() + 1)
                .amount(0)
                .build();
        secondRsEventDto = rsEventRepository.save(secondRsEventDto);

        ObjectMapper objectMapper = new ObjectMapper();
        Trade trade = Trade.builder()
                .amount(100)
                .rank(1)
                .build();
        String json = objectMapper.writeValueAsString(trade);
        mockMvc.perform(post("/rs/buy/{id}", secondRsEventDto.getId())
                .content(json)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        trade = Trade.builder()
                .amount(100)
                .rank(1)
                .build();
        json = objectMapper.writeValueAsString(trade);
        mockMvc.perform(post("/rs/buy/{id}", firstRsEventDto.getId())
                .content(json)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReplacePurchasedRsEventWhenBuyRank() throws Exception {
        UserDto save = userRepository.save(userDto);
        RsEventDto firstRsEventDto = RsEventDto.builder()
                .keyword("无分类")
                .eventName("第一条事件")
                .user(save)
                .rank(rsEventRepository.findAll().size() + 1)
                .amount(0)
                .build();
        firstRsEventDto = rsEventRepository.save(firstRsEventDto);
        RsEventDto secondRsEventDto = RsEventDto.builder()
                .keyword("无分类")
                .eventName("第二条事件")
                .user(save)
                .rank(rsEventRepository.findAll().size() + 1)
                .amount(0)
                .build();
        secondRsEventDto = rsEventRepository.save(secondRsEventDto);

        ObjectMapper objectMapper = new ObjectMapper();
        Trade trade = Trade.builder()
                .amount(100)
                .rank(1)
                .build();
        String json = objectMapper.writeValueAsString(trade);
        mockMvc.perform(post("/rs/buy/{id}", secondRsEventDto.getId())
                .content(json)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        trade = Trade.builder()
                .amount(101)
                .rank(1)
                .build();
        json = objectMapper.writeValueAsString(trade);
        mockMvc.perform(post("/rs/buy/{id}", firstRsEventDto.getId())
                .content(json)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        RsEventDto rsEventDto = rsEventRepository.findByRank(1);
        assertEquals(firstRsEventDto.getId(), rsEventDto.getId());
        assertEquals(trade.getAmount(), rsEventDto.getAmount());
        assertEquals(firstRsEventDto.getVoteNum(), rsEventDto.getVoteNum());
        assertEquals(firstRsEventDto.getUser().getId(), rsEventDto.getUser().getId());
        assertEquals(firstRsEventDto.getEventName(), rsEventDto.getEventName());
        assertEquals(firstRsEventDto.getKeyword(), rsEventDto.getKeyword());

        assertEquals(1, rsEventRepository.findAll().size());
    }


    @Test
    public void shouldGetRsEventListSortByVoteNumAndPurchased() throws Exception {

        UserDto save = userRepository.save(userDto);
        RsEventDto firstRsEventDto = RsEventDto.builder()
                .keyword("无分类")
                .eventName("第一条事件")
                .user(save)
                .rank(rsEventRepository.findAll().size() + 1)
                .amount(0)
                .build();
        firstRsEventDto = rsEventRepository.save(firstRsEventDto);
        RsEventDto secondRsEventDto = RsEventDto.builder()
                .keyword("无分类")
                .eventName("第二条事件")
                .user(save)
                .rank(rsEventRepository.findAll().size() + 1)
                .amount(0)
                .build();
        secondRsEventDto = rsEventRepository.save(secondRsEventDto);

        RsEventDto thirdRsEventDto = RsEventDto.builder()
                .keyword("无分类")
                .eventName("第三条事件")
                .user(save)
                .rank(rsEventRepository.findAll().size() + 1)
                .amount(0)
                .build();
        thirdRsEventDto = rsEventRepository.save(thirdRsEventDto);

        ObjectMapper objectMapper = new ObjectMapper();
        Trade trade = Trade.builder()
                .amount(100)
                .rank(1)
                .build();
        String json = objectMapper.writeValueAsString(trade);
        mockMvc.perform(post("/rs/buy/{id}", secondRsEventDto.getId())
                .content(json)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());


        String jsonValue = String.format("{\"userId\":%d,\"time\":\"%s\",\"voteNum\":1}",
                save.getId(),
                LocalDateTime.now().toString());
        mockMvc.perform(
                post("/rs/vote/{id}", thirdRsEventDto.getId())
                        .content(jsonValue)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/rs/list"))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].eventName", is("第二条事件")))
                .andExpect(jsonPath("$[1].eventName", is("第三条事件")))
                .andExpect(jsonPath("$[2].eventName", is("第一条事件")));

    }
}
