package com.example.botguard.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "bots")
public class Bot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String botName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBotName() { return botName; }
    public void setBotName(String botName) { this.botName = botName; }
}
