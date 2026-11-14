package com.azentio.swipeguard.alert;

import java.util.List;

public record AlertPage(List<AlertView> content, int page, int size, long totalElements) { }