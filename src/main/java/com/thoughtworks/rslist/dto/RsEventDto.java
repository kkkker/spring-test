package com.thoughtworks.rslist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.List;

@Entity
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "rsEvent")
public class RsEventDto {
  @Id @GeneratedValue private int id;
  private String eventName;
  private String keyword;
  private int voteNum;
  @ManyToOne private UserDto user;
  private int rank;
  private int amount;

  @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "rsEventDto")
  private List<TradeDto> tradeDtoList;
}
