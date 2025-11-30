package com.xshards;

import java.time.LocalDate;

public class StreakData {
    private int streakDays;
    private LocalDate lastLoginDate;
    private double multiplier;
    private String stage; // "easy", "mid", "hard"

    public StreakData() {
        this.streakDays = 0;
        this.lastLoginDate = null;
        this.multiplier = 1.0;
        this.stage = "easy";
    }

    public StreakData(int streakDays, LocalDate lastLoginDate, double multiplier, String stage) {
        this.streakDays = streakDays;
        this.lastLoginDate = lastLoginDate;
        this.multiplier = multiplier;
        this.stage = stage;
    }

    public int getStreakDays() {
        return streakDays;
    }

    public void setStreakDays(int streakDays) {
        this.streakDays = streakDays;
    }

    public LocalDate getLastLoginDate() {
        return lastLoginDate;
    }

    public void setLastLoginDate(LocalDate lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }
}
