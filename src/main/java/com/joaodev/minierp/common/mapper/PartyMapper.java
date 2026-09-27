package com.joaodev.minierp.common.mapper;

import com.joaodev.minierp.common.dto.PartyResponse;
import com.joaodev.minierp.common.entity.Party;
import java.util.function.Supplier;

public final class PartyMapper {

    private PartyMapper() {
    }

    public static <R extends PartyResponse> R toResponse(Party party, Supplier<R> responseFactory) {
        R response = responseFactory.get();
        response.setId(party.getId());
        response.setName(party.getName());
        response.setDocument(party.getDocument());
        response.setEmail(party.getEmail());
        response.setPhone(party.getPhone());
        response.setActive(party.isActive());
        response.setVersion(party.getVersion());
        response.setCreatedAt(party.getCreatedAt());
        response.setUpdatedAt(party.getUpdatedAt());
        return response;
    }
}
