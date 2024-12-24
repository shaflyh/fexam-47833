package com.hand.demo.api.controller.v1;

import com.hand.demo.api.dto.InvCountHeaderDTO;
import com.hand.demo.api.dto.InvCountInfoDTO;
import com.hand.demo.api.dto.WorkflowEventDTO;
import io.choerodon.core.domain.Page;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.mybatis.pagehelper.annotation.SortDefault;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.mybatis.pagehelper.domain.Sort;
import io.choerodon.swagger.annotation.Permission;
import io.swagger.annotations.ApiOperation;
import org.hzero.boot.platform.lov.annotation.ProcessLovValue;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.base.BaseController;
import org.hzero.core.cache.ProcessCacheValue;
import org.hzero.core.util.Results;
import org.hzero.mybatis.helper.SecurityTokenHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.hand.demo.app.service.InvCountHeaderService;
import com.hand.demo.domain.entity.InvCountHeader;
import com.hand.demo.domain.repository.InvCountHeaderRepository;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * (InvCountHeader)表控制层
 *
 * @author Shafly - 47833
 * @since 2024-12-17 10:42:53
 */

@RestController("invCountHeaderController.v1")
@RequestMapping("/v1/{organizationId}/inv-count-headers")
public class InvCountHeaderController extends BaseController {

    private final InvCountHeaderService invCountHeaderService;

    @Autowired
    public InvCountHeaderController(InvCountHeaderService invCountHeaderService) {
        this.invCountHeaderService = invCountHeaderService;
    }

    // 1. Counting order save (orderSave)
    @ApiOperation(value = "Save Order")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ProcessCacheValue
    @PostMapping("/order-save")
    public ResponseEntity<InvCountInfoDTO> orderSave(@PathVariable Long organizationId,
                                                     @RequestBody List<InvCountHeaderDTO> invCountHeaders) {
        validList(invCountHeaders, InvCountHeader.Save.class);
        SecurityTokenHelper.validTokenIgnoreInsert(invCountHeaders);
        return Results.success(invCountHeaderService.orderSave(invCountHeaders));
    }

    // 2. Counting order remove (orderRemove)
    @ApiOperation(value = "Delete Order")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @DeleteMapping
    public ResponseEntity<?> orderRemove(@PathVariable Long organizationId,
                                         @RequestBody List<InvCountHeaderDTO> invCountHeaders) {
        SecurityTokenHelper.validToken(invCountHeaders);
        return Results.success(invCountHeaderService.orderRemove(invCountHeaders));
    }

    // 3.a. Counting order query (list)
    @ApiOperation(value = "List Order")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ProcessLovValue(targetField = BaseConstants.FIELD_BODY)
    @ProcessCacheValue
    @GetMapping
    public ResponseEntity<Page<InvCountHeaderDTO>> list(InvCountHeader invCountHeader,
                                                        @PathVariable Long organizationId, @ApiIgnore
                                                        @SortDefault(value = InvCountHeader.FIELD_CREATION_DATE,
                                                                direction = Sort.Direction.DESC)
                                                        PageRequest pageRequest) {
        Page<InvCountHeaderDTO> list = invCountHeaderService.selectList(pageRequest, invCountHeader);
        return Results.success(list);
    }

    // 3.b. Counting order query (detail)
    @ApiOperation(value = "Detail Order")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ProcessCacheValue
    @ProcessLovValue(targetField = BaseConstants.FIELD_BODY)
    @GetMapping("/{countHeaderId}/detail")
    public ResponseEntity<InvCountHeaderDTO> detail(@PathVariable Long organizationId,
                                                    @PathVariable Long countHeaderId) {
        InvCountHeaderDTO invCountHeader = invCountHeaderService.selectDetail(countHeaderId);
        return Results.success(invCountHeader);
    }

    // 4. Counting order execution (orderExecution)
    @ApiOperation(value = "Execute Order")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ProcessCacheValue
    @PostMapping("/order-execution")
    public ResponseEntity<InvCountInfoDTO> orderExecution(@PathVariable Long organizationId,
                                                          @RequestBody List<InvCountHeaderDTO> invCountHeaders) {
        validList(invCountHeaders, InvCountHeader.Execute.class);
        // SecurityTokenHelper.validTokenIgnoreInsert(invCountHeaders);
        return Results.success(invCountHeaderService.orderExecution(invCountHeaders));
    }

    // 5. Submit counting results for approval (orderSubmit)
    @ApiOperation(value = "Submit Order")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ProcessCacheValue
    @PostMapping("/order-submit")
    public ResponseEntity<InvCountInfoDTO> orderSubmit(@PathVariable Long organizationId,
                                                       @RequestBody List<InvCountHeaderDTO> invCountHeaders) {
        return Results.success(invCountHeaderService.orderSubmit(invCountHeaders));
    }

    // 6. Counting result synchronous (countResultSync)
    @ApiOperation(value = "Result Sync Order")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ProcessCacheValue
    @PostMapping("/count-result-sync")
    public ResponseEntity<InvCountHeaderDTO> countResultSync(@PathVariable Long organizationId,
                                                             @RequestBody InvCountHeaderDTO invCountHeader) {
        validObject(invCountHeader, InvCountHeader.ResultSync.class);
        return Results.success(invCountHeaderService.countResultSync(invCountHeader));
    }

    // 7. Counting order report dataset method (countingOrderReportDs)
    @ApiOperation(value = "Report Dataset Order")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ProcessCacheValue
    @ProcessLovValue(targetField = BaseConstants.FIELD_BODY)
    @GetMapping("/order-report-dataset")
    public ResponseEntity<List<InvCountHeaderDTO>> countingOrderReportDs(@PathVariable Long organizationId,
                                                                         InvCountHeaderDTO invCountHeader) {
        // validObject(invCountHeader, InvCountHeader.Save.class);
        return Results.success(invCountHeaderService.countingOrderReportDs(invCountHeader));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "Workflow Callback")
    @PostMapping(path = "/workflow-callback")
    public ResponseEntity<InvCountHeaderDTO> workflowCallback(@PathVariable("organizationId") Long tenantId,
                                                   @RequestBody WorkflowEventDTO workflowEventDTO) {
        return Results.success(invCountHeaderService.workflowCallback(tenantId, workflowEventDTO));
    }

}

