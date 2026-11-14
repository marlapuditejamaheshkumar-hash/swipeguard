package com.azentio.swipeguard.risk;

import java.util.List;

public record RiskResult(Decision decision, int score, List<Reason> reasons) { }