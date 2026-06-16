param(
    [string]$BaseUrl = "http://localhost:80",
    [int]$TopK = 5,
    [switch]$SkipRebuild
)

$ErrorActionPreference = "Stop"

function New-TestCase {
    param(
        [string]$Id,
        [string]$Type,
        [Nullable[int]]$CommunityId,
        [string]$Question,
        [bool]$ExpectedCannotAnswer,
        [string[]]$ExpectedAnySource = @(),
        [string[]]$ExpectedAllSources = @(),
        [string[]]$ForbiddenSources = @(),
        [string[]]$ExpectedModes = @()
    )

    [pscustomobject]@{
        Id = $Id
        Type = $Type
        CommunityId = $CommunityId
        Question = $Question
        ExpectedCannotAnswer = $ExpectedCannotAnswer
        ExpectedAnySource = $ExpectedAnySource
        ExpectedAllSources = $ExpectedAllSources
        ForbiddenSources = $ForbiddenSources
        ExpectedModes = $ExpectedModes
    }
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
        [object]$Case
    )

    $body = @{
        communityId = $Case.CommunityId
        question = $Case.Question
        topK = $TopK
    }

    $json = $body | ConvertTo-Json -Depth 5
    Invoke-RestMethod `
        -Uri "$BaseUrl/api/ai/community/customer-service/ask" `
        -Method Post `
        -ContentType "application/json; charset=utf-8" `
        -Body $json
}

$cases = @(
    New-TestCase "RAG-001" "answerable" 1 "3栋本周六会停水吗？" $false @("NOTICE_WATER_001") @() @() @("HYBRID", "KEYWORD")
    New-TestCase "RAG-002" "answerable" 1 "厨房漏水怎么报修？" $false @("FAQ_REPAIR_LEAK_001", "PROCESS_REPAIR_001") @() @() @("HYBRID")
    New-TestCase "RAG-003" "answerable" 1 "物业费欠费怎么缴？" $false @("POLICY_FEE_001") @() @() @("HYBRID", "KEYWORD")
    New-TestCase "RAG-004" "answerable" 1 "亲戚来小区要怎么登记？" $false @("POLICY_VISITOR_001") @() @() @("HYBRID", "KEYWORD")
    New-TestCase "RAG-005" "answerable" 1 "噪音扰民投诉多久受理？" $false @("PROCESS_COMPLAINT_001") @() @() @("HYBRID", "KEYWORD")
    New-TestCase "RAG-006" "answerable" 1 "电动车可以在楼道充电吗？" $false @("manual:*") @() @() @("HYBRID")
    New-TestCase "RAG-007" "paraphrase" 1 "我能把小电驴电池拿回家充一晚上吗？" $false @("manual:*") @() @() @("HYBRID", "VECTOR")
    New-TestCase "RAG-008" "paraphrase" 1 "家里突然没水，是不是周末有维修？" $false @("NOTICE_WATER_001") @() @() @("HYBRID", "VECTOR")
    New-TestCase "RAG-009" "paraphrase" 1 "水管爆了应该先怎么办？" $false @("PROCESS_REPAIR_001", "FAQ_REPAIR_LEAK_001") @() @() @("HYBRID", "VECTOR")
    New-TestCase "RAG-010" "paraphrase" 1 "费用账单看不懂找谁核对？" $false @("POLICY_FEE_001") @() @() @("HYBRID", "VECTOR")
    New-TestCase "RAG-011" "paraphrase" 1 "外地亲戚过来住两天，门岗怎么放行？" $false @("POLICY_VISITOR_001") @() @() @("HYBRID", "VECTOR")
    New-TestCase "RAG-012" "multi" 1 "3栋停水、厨房漏水、物业费欠费分别怎么办？" $false @() @("NOTICE_WATER_001", "POLICY_FEE_001") @() @("HYBRID")
    New-TestCase "RAG-013" "multi" 1 "电梯困人、外来访客、账单疑问分别找谁？" $false @() @("PROCESS_REPAIR_001", "POLICY_VISITOR_001", "POLICY_FEE_001") @() @("HYBRID")
    New-TestCase "RAG-014" "paraphrase" 1 "电动车飞线充电被发现会怎么处理？" $false @("manual:*") @() @() @("HYBRID", "VECTOR")
    New-TestCase "RAG-015" "community-isolation" 999 "3栋本周六停水吗？" $true @() @() @("NOTICE_WATER_001") @()
    New-TestCase "RAG-016" "community-isolation" 2 "3栋本周六停水吗？" $true @() @() @("NOTICE_WATER_001") @()
    New-TestCase "RAG-017" "cannot-answer" 1 "小区游泳池几点开放？" $true @() @() @() @()
    New-TestCase "RAG-018" "cannot-answer" 1 "物业主任手机号是多少？" $true @() @() @() @()
    New-TestCase "RAG-019" "cannot-answer" 1 "快递柜密码是多少？" $true @() @() @() @()
    New-TestCase "RAG-020" "cannot-answer" 1 "车位价格下个月会不会涨？" $true @() @() @() @()
)

Write-Host "RAG regression baseUrl=$BaseUrl topK=$TopK"

if (-not $SkipRebuild) {
    Write-Host "Rebuilding embeddings..."
    try {
        $rebuild = Invoke-RestMethod -Uri "$BaseUrl/api/ai/knowledge/embeddings/rebuild" -Method Post
        Write-Host ("Embedding rebuild: provider={0} model={1} dimension={2} embedded={3} failed={4}" -f `
                $rebuild.provider, $rebuild.model, $rebuild.dimension, $rebuild.embeddedCount, $rebuild.failedCount)
    } catch {
        Write-Host "Embedding rebuild failed: $($_.Exception.Message)" -ForegroundColor Yellow
        Write-Host "Continue running tests because existing embeddings may still be usable." -ForegroundColor Yellow
    }
}

$results = @()

foreach ($case in $cases) {
    $errors = New-Object System.Collections.Generic.List[string]
    $response = $null

    try {
        $response = Invoke-RagAsk -Case $case
    } catch {
        $errors.Add("request failed: $($_.Exception.Message)")
    }

    $sourceIds = @()
    $modes = @()
    $citations = @()

    if ($response) {
        $sourceIds = @($response.sources | ForEach-Object { $_.sourceId } | Where-Object { $_ })
        $modes = @($response.sources | ForEach-Object { $_.retrievalMode } | Where-Object { $_ })
        $citations = @($response.citations | Where-Object { $_ })

        if ([bool]$response.cannotAnswer -ne $case.ExpectedCannotAnswer) {
            $errors.Add("cannotAnswer expected=$($case.ExpectedCannotAnswer) actual=$($response.cannotAnswer)")
        }

        if (-not $case.ExpectedCannotAnswer) {
            if ($case.ExpectedAnySource.Count -gt 0) {
                $matched = $false
                foreach ($source in $case.ExpectedAnySource) {
                    if ((Test-SourceMatch $source $sourceIds) -or (Test-SourceMatch $source $citations)) {
                        $matched = $true
                        break
                    }
                }
                if (-not $matched) {
                    $errors.Add("source mismatch, expected any of [$($case.ExpectedAnySource -join ', ')] actual [$($sourceIds -join ', ')]")
                }
            }

            foreach ($source in $case.ExpectedAllSources) {
                if (-not ((Test-SourceMatch $source $sourceIds) -or (Test-SourceMatch $source $citations))) {
                    $errors.Add("missing source $source, actual [$($sourceIds -join ', ')] citations [$($citations -join ', ')]")
                }
            }

            if ($case.ExpectedModes.Count -gt 0) {
                $modeMatched = [bool]($modes | Where-Object { $case.ExpectedModes -contains $_ } | Select-Object -First 1)
                if (-not $modeMatched) {
                    $errors.Add("retrievalMode expected any of [$($case.ExpectedModes -join ', ')] actual [$($modes -join ', ')]")
                }
            }
        }

        foreach ($source in $case.ForbiddenSources) {
            if ((Test-SourceMatch $source $sourceIds) -or (Test-SourceMatch $source $citations)) {
                $errors.Add("forbidden source matched $source")
            }
        }
    }

    $status = if ($errors.Count -eq 0) { "PASS" } else { "FAIL" }
    $result = [pscustomobject]@{
        Id = $case.Id
        Status = $status
        CannotAnswer = if ($response) { $response.cannotAnswer } else { $null }
        SourceIds = $sourceIds -join ","
        Modes = $modes -join ","
        Citations = $citations -join ","
        Notes = $errors -join "; "
    }
    $results += $result

    $color = if ($status -eq "PASS") { "Green" } else { "Red" }
    Write-Host ("{0} {1} cannotAnswer={2} sources=[{3}] modes=[{4}]" -f `
            $status, $case.Id, $result.CannotAnswer, $result.SourceIds, $result.Modes) -ForegroundColor $color
    if ($errors.Count -gt 0) {
        Write-Host "  $($result.Notes)" -ForegroundColor Red
    }
}

$passed = @($results | Where-Object { $_.Status -eq "PASS" }).Count
$failed = @($results | Where-Object { $_.Status -ne "PASS" }).Count

Write-Host ""
Write-Host ("Summary: passed={0} failed={1} total={2}" -f $passed, $failed, $results.Count)

if ($failed -gt 0) {
    exit 1
}
