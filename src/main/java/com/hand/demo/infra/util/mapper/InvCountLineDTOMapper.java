package com.hand.demo.infra.util.mapper;

import com.hand.demo.api.dto.InvCountLineDTO;
import com.hand.demo.domain.entity.InvCountLine;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InvCountLineDTOMapper {
    InvCountLineDTO toDto(InvCountLine invCountLine);

    List<InvCountLineDTO> toDtoList(List<InvCountLine> invCountLines);
}
