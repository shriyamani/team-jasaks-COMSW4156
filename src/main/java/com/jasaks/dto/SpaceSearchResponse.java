package com.jasaks.dto;

import com.jasaks.model.Space;
import java.math.BigDecimal;

public record SpaceSearchResponse(
        Space request,
        BigDecimal estPrice
) {
}
