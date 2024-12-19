package com.hand.demo.app.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hand.demo.api.dto.InvCountLineDTO;
import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.hzero.boot.apaas.common.userinfo.domain.UserVO;
import org.hzero.boot.apaas.common.userinfo.infra.feign.IamRemoteService;
import org.hzero.mybatis.domian.Condition;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.hand.demo.app.service.InvCountLineService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.hand.demo.domain.entity.InvCountLine;
import com.hand.demo.domain.repository.InvCountLineRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * (InvCountLine)应用服务
 *
 * @author Shafly - 47833
 * @since 2024-12-17 10:43:06
 */
@Service
public class InvCountLineServiceImpl implements InvCountLineService {
    private final InvCountLineRepository invCountLineRepository;
    private final IamRemoteService iamRemoteService;

    @Autowired
    public InvCountLineServiceImpl(InvCountLineRepository invCountLineRepository, IamRemoteService iamRemoteService) {
        this.invCountLineRepository = invCountLineRepository;
        this.iamRemoteService = iamRemoteService;
    }

    @Override
    public Page<InvCountLineDTO> selectList(PageRequest pageRequest, InvCountLine invCountLine) {

        // Convert to DTO
        InvCountLineDTO invCountLineDTO = new InvCountLineDTO();
        BeanUtils.copyProperties(invCountLine, invCountLineDTO);

        // Check if the user admin or not
        invCountLineDTO.setTenantAdminFlag(getUserVO().getTenantAdminFlag() != null);
        return PageHelper.doPageAndSort(pageRequest, () -> invCountLineRepository.selectList(invCountLine));
    }

    @Override
    public void saveData(List<InvCountLine> invCountLines) {
        List<InvCountLine> insertList =
                invCountLines.stream().filter(line -> line.getCountLineId() == null).collect(Collectors.toList());
        List<InvCountLine> updateList =
                invCountLines.stream().filter(line -> line.getCountLineId() != null).collect(Collectors.toList());
        invCountLineRepository.batchInsertSelective(insertList);
        invCountLineRepository.batchUpdateByPrimaryKeySelective(updateList);
    }

    @Override
    public List<InvCountLineDTO> selectListByHeaderId(Long headerId) {
        Condition condition = new Condition(InvCountLine.class);
        // Define the criteria to filter by count_header_id
        condition.createCriteria().andEqualTo("countHeaderId", headerId);
        // Fetch the filtered data
        List<InvCountLine> invCountLines = invCountLineRepository.selectByCondition(condition);
        // return the DTOs
        List<InvCountLineDTO> invCountLineDTOS = new ArrayList<>();
        for (InvCountLine invCountLine : invCountLines) {
            InvCountLineDTO invCountLineDTO = new InvCountLineDTO();
            BeanUtils.copyProperties(invCountLine, invCountLineDTO);
            invCountLineDTOS.add(invCountLineDTO);
        }
        return invCountLineDTOS;
    }

    // TODO: Make this as a common function that can be accessed in Line and Header service
    private UserVO getUserVO() {
        ResponseEntity<String> stringResponse = iamRemoteService.selectSelf();
        ObjectMapper objectMapper = new ObjectMapper();
        // Fix object mapper error
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Set a custom date format that matches the API response
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        objectMapper.setDateFormat(dateFormat);
        UserVO userVO;
        try {
            userVO = objectMapper.readValue(stringResponse.getBody(), UserVO.class);
        } catch (JsonProcessingException e) {
            throw new CommonException("Failed to parse response body to UserVO", e);
        } catch (Exception e) {
            throw new CommonException("Unexpected error occurred", e);
        }
        return userVO;
    }
}

