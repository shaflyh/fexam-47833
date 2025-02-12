package com.hand.demo.app.service.impl;

import com.hand.demo.api.dto.InvCountHeaderDTO;
import com.hand.demo.api.dto.InvCountLineDTO;
import com.hand.demo.api.dto.InvStockSummaryDTO;
import com.hand.demo.app.service.InvBatchService;
import com.hand.demo.app.service.InvMaterialService;
import com.hand.demo.domain.entity.*;
import com.hand.demo.infra.util.Utils;
import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.hzero.mybatis.domian.Condition;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.hand.demo.app.service.InvCountLineService;
import org.springframework.stereotype.Service;
import com.hand.demo.domain.repository.InvCountLineRepository;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final InvMaterialService materialService;
    private final InvBatchService batchService;
    private final Utils utils;

    @Autowired
    public InvCountLineServiceImpl(InvCountLineRepository invCountLineRepository, InvMaterialService materialService,
                                   InvBatchService batchService, Utils utils) {
        this.invCountLineRepository = invCountLineRepository;
        this.materialService = materialService;
        this.batchService = batchService;
        this.utils = utils;
    }

    @Override
    public Page<InvCountLineDTO> selectList(PageRequest pageRequest, InvCountLine invCountLine) {

        // Convert to DTO
        InvCountLineDTO invCountLineDTO = new InvCountLineDTO();
        BeanUtils.copyProperties(invCountLine, invCountLineDTO);

        // Check if the user admin or not
        invCountLineDTO.setTenantAdminFlag(utils.getUserVO().getTenantAdminFlag() != null);
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
    public List<InvCountLineDTO> selectListByHeader(InvCountHeaderDTO invCountHeaderDTO) {
        Long headerId = invCountHeaderDTO.getCountHeaderId();
        Condition condition = new Condition(InvCountLine.class);
        // Define the criteria to filter by count_header_id
        condition.createCriteria().andEqualTo("countHeaderId", headerId);
        // Fetch the filtered data
        List<InvCountLine> invCountLines = invCountLineRepository.selectByCondition(condition);
        // convert to the DTOs
        List<InvCountLineDTO> invCountLineDTOS = convertLinesToDTOList(invCountLines);

        // Add the required field for reporting
        // TODO: prevent query in a loop
        invCountLineDTOS.forEach(line -> {
            if (line.getMaterialId() != null) {
                InvMaterial material = materialService.selectById(line.getMaterialId());
                line.setItemCode(material.getMaterialCode());
                line.setItemName(material.getMaterialName());
            }
            if (line.getBatchId() != null) {
                InvBatch batch = batchService.selectById(line.getBatchId());
                line.setBatchCode(batch.getBatchCode());
            }
            line.setSupervisorIds(invCountHeaderDTO.getSupervisorIds());
        });

        return invCountLineDTOS;
    }

    @Override
    public List<InvCountLine> fetchExistingLines(List<InvCountLine> lineList) {
        // Extract the set of IDs from the update list
        Set<Long> lineIds = lineList.stream().map(InvCountLine::getCountLineId).collect(Collectors.toSet());

        // Convert the set of IDs to a comma-separated string for the repository query
        String idString = lineIds.stream().map(String::valueOf).collect(Collectors.joining(","));

        // Fetch the existing lines from the repository using the comma-separated IDs
        return invCountLineRepository.selectByIds(idString);
    }

    @Override
    public List<InvCountLine> resultSyncBatchUpdate(List<InvCountLine> invCountLines) {
        return invCountLineRepository.batchUpdateOptional(invCountLines,
                InvCountLine.FIELD_UNIT_QTY,
                InvCountLine.FIELD_UNIT_DIFF_QTY,
                InvCountLine.FIELD_REMARK);
    }

    @Override
    public List<InvCountLine> batchInsert(List<InvCountLine> invCountLines) {
        return invCountLineRepository.batchInsertSelective(invCountLines);
    }

    /**
     * Generates a list of InvCountLine objects by summarizing stock quantities based on grouping.
     *
     * @param summarizedStocks The list of InvStock items to process.
     * @param header           The InvCountHeaderDTO containing counting information.
     * @return A list of InvCountLine objects representing each group.
     */
    @Override
    public List<InvCountLine> generateInvLines(List<InvStockSummaryDTO> summarizedStocks, InvCountHeaderDTO header) {
        AtomicInteger lineNumber = new AtomicInteger(1);
        return summarizedStocks.stream()
                .map(stock -> generateInvCountLine(lineNumber.getAndIncrement(), header, stock))
                .collect(Collectors.toList());
    }

    /**
     * Creates a single InvCountLine object based on the provided stock summary and header information.
     *
     * @param lineNumber Line number for the generated line.
     * @param header     Header-level information for the counting process.
     * @param stock      Stock summary data used to populate the line details.
     * @return A fully populated InvCountLine object.
     */
    private InvCountLine generateInvCountLine(int lineNumber, InvCountHeaderDTO header, InvStockSummaryDTO stock) {
        InvCountLine line = new InvCountLine();
        line.setLineNumber(lineNumber);
        line.setTenantId(header.getTenantId());
        line.setCountHeaderId(header.getCountHeaderId());
        line.setWarehouseId(header.getWarehouseId());
        line.setCounterIds(header.getCounterIds());
        line.setSnapshotUnitQty(stock.getTotalQuantity());
        line.setUnitCode(stock.getUnitCode());
        line.setMaterialId(stock.getMaterialId());
        line.setBatchId(stock.getBatchId());
        return line;
    }

    private List<InvCountLineDTO> convertLinesToDTOList(List<InvCountLine> lineList) {
        return lineList.stream().map(line -> {
            InvCountLineDTO dto = new InvCountLineDTO();
            BeanUtils.copyProperties(line, dto); // Copy properties from entity to DTO
            return dto;
        }).collect(Collectors.toList());
    }
}

