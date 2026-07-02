param(
    [string]$BaseUrl = "http://localhost:80",
    [string]$AiBaseUrl = "",
    [string]$Username = "admin",
    [string]$Password = "",
    [string]$Role = "super_admin",
    [int]$CommunityId = 1,
    [string]$StartDate = "2026-06-01",
    [string]$EndDate = "2026-06-07",
    [int]$TopK = 5,
    [switch]$SkipKnowledgeCreate
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

if (-not $AiBaseUrl) {
    $AiBaseUrl = $BaseUrl
}

$script:StepResults = New-Object System.Collections.Generic.List[object]

function Add-StepResult {
    param(
        [string]$Name,
        [string]$Status,
        [string]$Detail
    )

    $script:StepResults.Add([pscustomobject]@{
        Name = $Name
        Status = $Status
        Detail = $Detail
    })

    $color = if ($Status -eq "PASS") { "Green" } elseif ($Status -eq "SKIP") { "Yellow" } else { "Red" }
    Write-Host ("{0} {1} - {2}" -f $Status, $Name, $Detail) -ForegroundColor $color
}

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Join-NonEmpty {
    param([string[]]$Values)
    return (@($Values) | Where-Object { $_ }) -join ","
}

function Invoke-Json {
    param(
        [string]$Uri,
        [string]$Method = "Get",
        [object]$Body = $null,
        [hashtable]$Headers = @{},
        [int]$TimeoutSec = 45
    )

    $parameters = @{
        Uri = $Uri
        Method = $Method
        Headers = $Headers
        TimeoutSec = $TimeoutSec
    }

    if ($null -ne $Body) {
        $parameters.ContentType = "application/json; charset=utf-8"
        $parameters.Body = ($Body | ConvertTo-Json -Depth 20)
    }

    Invoke-RestMethod @parameters
}

function Test-SourceMatch {
    param(
        [string]$Pattern,
        [string[]]$SourceIds
    )

    if (-not $Pattern) {
        return $true
    }
    if ($Pattern.EndsWith("*")) {
        $prefix = $Pattern.Substring(0, $Pattern.Length - 1)
        return [bool]($SourceIds | Where-Object { $_ -like "$prefix*" } | Select-Object -First 1)
    }
    return $SourceIds -contains $Pattern
}

function Invoke-RagAsk {
    param(
        [string]$Question,
        [string[]]$ExpectedAnySource = @(),
        [bool]$ExpectedCannotAnswer = $false
    )

    $response = Invoke-Json `
        -Uri "$AiBaseUrl/api/ai/community/customer-service/ask" `
        -Method Post `
        -Body @{
            communityId = $CommunityId
            question = $Question
            topK = $TopK
        } `
        -TimeoutSec 60

    Assert-True ([bool]$response -eq $true) "RAG response is empty"
    Assert-True ([bool]$response.cannotAnswer -eq $ExpectedCannotAnswer) `
        "RAG cannotAnswer expected=$ExpectedCannotAnswer actual=$($response.cannotAnswer)"

    $sourceIds = @($response.sources | ForEach-Object { $_.sourceId } | Where-Object { $_ })
    $citations = @($response.citations | Where-Object { $_ })
    $allSourceIds = @($sourceIds + $citations)

    if (-not $ExpectedCannotAnswer -and $ExpectedAnySource.Count -gt 0) {
        $matched = $false
        foreach ($source in $ExpectedAnySource) {
            if (Test-SourceMatch -Pattern $source -SourceIds $allSourceIds) {
                $matched = $true
                break
            }
        }
        Assert-True $matched "RAG source mismatch, expected any of [$($ExpectedAnySource -join ',')] actual [$($allSourceIds -join ',')]"
    }

    return $response
}

Write-Host "AI full smoke baseUrl=$BaseUrl aiBaseUrl=$AiBaseUrl communityId=$CommunityId range=$StartDate..$EndDate"

try {
    $login = Invoke-Json `
        -Uri "$BaseUrl/api/user/login" `
        -Method Post `
        -Body @{
            username = $Username
            password = $Password
            role = $Role
        } `
        -TimeoutSec 20
    Assert-True ($login.code -eq 200) "login code expected=200 actual=$($login.code) msg=$($login.msg)"
    Assert-True ([string]::IsNullOrWhiteSpace($login.data.token) -eq $false) "login token is empty"
    $token = $login.data.token
    Add-StepResult "登录后台" "PASS" "user=$($login.data.username) role=$($login.data.role)"
} catch {
    Add-StepResult "登录后台" "FAIL" $_.Exception.Message
    throw
}

$headers = @{
    Authorization = "Bearer $token"
}

try {
    $rag = Invoke-RagAsk `
        -Question "厨房漏水怎么报修？物业多久响应？" `
        -ExpectedAnySource @("PROCESS_REPAIR_001", "POLICY_REPAIR_001", "FAQ_REPAIR_LEAK_001") `
        -ExpectedCannotAnswer $false
    $sourceIds = @($rag.sources | ForEach-Object { $_.sourceId } | Where-Object { $_ })
    $modes = @($rag.sources | ForEach-Object { $_.retrievalMode } | Where-Object { $_ })
    Add-StepResult "RAG 提问" "PASS" ("cannotAnswer={0} sources=[{1}] modes=[{2}]" -f `
            $rag.cannotAnswer, (Join-NonEmpty $sourceIds), (Join-NonEmpty $modes))
} catch {
    Add-StepResult "RAG 提问" "FAIL" $_.Exception.Message
    throw
}

$sourceId = "manual:SMOKE_EV_CHARGE_RULE"
if ($SkipKnowledgeCreate) {
    Add-StepResult "新增知识" "SKIP" "SkipKnowledgeCreate=true"
} else {
    try {
        $knowledgeBody = @{
            sourceType = "FAQ"
            sourceId = $sourceId
            communityId = $CommunityId
            title = "Smoke Test 电动车集中充电与飞线充电管理规定"
            content = "为保障小区消防安全，电动车应停放在指定非机动车区域，并使用小区集中充电桩充电。严禁在楼道、单元门厅、地下车库疏散通道内停放电动车或私拉电线充电。严禁将电动车电池带入室内、楼道或电梯内充电。发现飞线充电、占用消防通道或电池入户充电的，物业将先提醒整改；拒不整改或存在明显安全隐患的，将联系社区网格员或消防部门协助处理。"
            keywords = @("电动车", "集中充电", "飞线充电", "楼道充电", "电池入户", "消防安全", "SmokeTest")
            status = "ENABLED"
            visibility = "RESIDENT"
        }

        try {
            $created = Invoke-Json `
                -Uri "$AiBaseUrl/api/ai/knowledge/documents" `
                -Method Post `
                -Body $knowledgeBody `
                -Headers $headers `
                -TimeoutSec 30
            $knowledgeId = $created.id
            $message = $created.message
        } catch {
            $existingPage = Invoke-Json `
                -Uri "$AiBaseUrl/api/ai/knowledge/documents?keyword=$sourceId&pageNum=1&pageSize=1" `
                -Method Get `
                -Headers $headers `
                -TimeoutSec 30
            Assert-True ($existingPage.records.Count -gt 0) "create failed and existing smoke knowledge not found: $($_.Exception.Message)"
            $knowledgeId = $existingPage.records[0].id
            $updated = Invoke-Json `
                -Uri "$AiBaseUrl/api/ai/knowledge/documents/$knowledgeId" `
                -Method Put `
                -Body $knowledgeBody `
                -Headers $headers `
                -TimeoutSec 30
            $message = $updated.message
        }

        Assert-True ($knowledgeId -gt 0) "knowledge id is invalid"
        Add-StepResult "新增知识" "PASS" "id=$knowledgeId sourceId=$sourceId action=$message"
    } catch {
        Add-StepResult "新增知识" "FAIL" $_.Exception.Message
        throw
    }
}

try {
    $rebuild = Invoke-Json `
        -Uri "$AiBaseUrl/api/ai/knowledge/embeddings/rebuild" `
        -Method Post `
        -Headers $headers `
        -TimeoutSec 120
    Assert-True ($rebuild.scannedCount -gt 0) "embedding scannedCount should be > 0"
    Assert-True ($rebuild.failedCount -eq 0) "embedding failedCount expected=0 actual=$($rebuild.failedCount)"
    Add-StepResult "重建向量" "PASS" ("provider={0} model={1} scanned={2} embedded={3} failed={4}" -f `
            $rebuild.provider, $rebuild.model, $rebuild.scannedCount, $rebuild.embeddedCount, $rebuild.failedCount)
} catch {
    Add-StepResult "重建向量" "FAIL" $_.Exception.Message
    throw
}

try {
    $ragAfterKnowledge = Invoke-RagAsk `
        -Question "电动车飞线充电或者把电池拿回家充电会怎么处理？" `
        -ExpectedAnySource @($sourceId, "manual:*") `
        -ExpectedCannotAnswer $false
    $sourceIds = @($ragAfterKnowledge.sources | ForEach-Object { $_.sourceId } | Where-Object { $_ })
    Add-StepResult "新增知识命中" "PASS" ("sources=[{0}]" -f (Join-NonEmpty $sourceIds))
} catch {
    Add-StepResult "新增知识命中" "FAIL" $_.Exception.Message
    throw
}

try {
    $weekly = Invoke-Json `
        -Uri "$AiBaseUrl/api/ai/operations/weekly-report/from-db?communityId=$CommunityId&startDate=$StartDate&endDate=$EndDate" `
        -Method Get `
        -Headers $headers `
        -TimeoutSec 90
    Assert-True ($weekly.source -eq "MYSQL") "weekly source expected=MYSQL actual=$($weekly.source)"
    Assert-True ($weekly.sourceData.urgentRepairCount -ge 3) "urgentRepairCount should be high, actual=$($weekly.sourceData.urgentRepairCount)"
    Assert-True ($weekly.sourceData.complaintPending -ge 5) "complaintPending should be high, actual=$($weekly.sourceData.complaintPending)"
    Assert-True ($weekly.report.manualReviewNeeded -eq $true) "weekly manualReviewNeeded expected=true actual=$($weekly.report.manualReviewNeeded)"
    Assert-True ($weekly.report.riskAlerts.Count -gt 0) "weekly riskAlerts should not be empty"
    Add-StepResult "AI 周报" "PASS" ("urgent={0} pendingComplaints={1} unpaidFees={2} riskAlerts={3} provider={4}" -f `
            $weekly.sourceData.urgentRepairCount, $weekly.sourceData.complaintPending, `
            $weekly.sourceData.feeUnpaidCount, $weekly.report.riskAlerts.Count, $weekly.report.provider)
} catch {
    Add-StepResult "AI 周报" "FAIL" $_.Exception.Message
    throw
}

try {
    $insights = Invoke-Json `
        -Uri "$AiBaseUrl/api/ai/operations/insights/from-db?communityId=$CommunityId&startDate=$StartDate&endDate=$EndDate" `
        -Method Get `
        -Headers $headers `
        -TimeoutSec 90
    Assert-True ($insights.source -eq "MYSQL") "insights source expected=MYSQL actual=$($insights.source)"
    Assert-True (@("HIGH", "CRITICAL") -contains $insights.insights.overallRiskLevel) `
        "overallRiskLevel expected HIGH/CRITICAL actual=$($insights.insights.overallRiskLevel)"
    Assert-True ($insights.insights.manualReviewNeeded -eq $true) `
        "insights manualReviewNeeded expected=true actual=$($insights.insights.manualReviewNeeded)"
    Assert-True ($insights.insights.insightCards.Count -gt 0) "insightCards should not be empty"
    Assert-True ($insights.insights.actionItems.Count -gt 0) "actionItems should not be empty"
    $cardTypes = @($insights.insights.insightCards | ForEach-Object { $_.insightType } | Where-Object { $_ })
    Add-StepResult "AI 洞察" "PASS" ("risk={0} cards={1} actions={2} cardTypes=[{3}] provider={4}" -f `
            $insights.insights.overallRiskLevel, $insights.insights.insightCards.Count, `
            $insights.insights.actionItems.Count, (Join-NonEmpty $cardTypes), $insights.insights.provider)
} catch {
    Add-StepResult "AI 洞察" "FAIL" $_.Exception.Message
    throw
}

Write-Host ""
Write-Host "Smoke Summary"
$script:StepResults | Format-Table -AutoSize

$failed = @($script:StepResults | Where-Object { $_.Status -eq "FAIL" }).Count
if ($failed -gt 0) {
    exit 1
}
