-- ============================================================
-- AI operations fee unpaid diagnostics
-- Purpose:
--   Diagnose why /api/ai/operations/*/from-db returns feeUnpaidCount
--   different from the expected demo value.
--
-- How to run:
--   mysql -h127.0.0.1 -P3306 -uroot -p1234 smart_community < sql/ai_operations_fee_unpaid_diagnostics.sql
--
-- Java aggregation logic currently counts:
--   sys_fee rows where community_id = 1
--   and UPPER(status) in ('UNPAID', 'PAYING', 'OVERDUE')
--   It does NOT filter by startDate/endDate.
-- ============================================================

SET NAMES utf8mb4;
SET @ops_community_id := 1;
SET @start_time := '2026-06-01 00:00:00';
SET @end_exclusive := '2026-06-08 00:00:00';

SELECT
  '01_community' AS check_name,
  c.id AS community_id,
  c.name AS community_name
FROM sys_community c
WHERE c.id = @ops_community_id;

SELECT
  '02_java_exact_fee_unpaid_count' AS check_name,
  COUNT(*) AS fee_unpaid_count_used_by_java
FROM sys_fee f
WHERE UPPER(COALESCE(f.status, '')) IN ('UNPAID', 'PAYING', 'OVERDUE')
  AND f.community_id = @ops_community_id;

SELECT
  '03_ai_ops_test_fee_rows' AS check_name,
  COUNT(*) AS ai_ops_test_total,
  SUM(CASE WHEN UPPER(COALESCE(status, '')) IN ('UNPAID', 'PAYING', 'OVERDUE') THEN 1 ELSE 0 END) AS counted_by_java,
  SUM(CASE WHEN UPPER(COALESCE(status, '')) NOT IN ('UNPAID', 'PAYING', 'OVERDUE') THEN 1 ELSE 0 END) AS not_counted_by_java,
  MIN(create_time) AS min_create_time,
  MAX(create_time) AS max_create_time,
  MIN(due_date) AS min_due_date,
  MAX(due_date) AS max_due_date
FROM sys_fee
WHERE remark = 'AI_OPS_TEST'
  AND community_id = @ops_community_id;

SELECT
  '04_status_distribution_all_community_1' AS check_name,
  COALESCE(status, '<NULL>') AS status,
  COUNT(*) AS total
FROM sys_fee
WHERE community_id = @ops_community_id
GROUP BY COALESCE(status, '<NULL>')
ORDER BY total DESC, status ASC;

SELECT
  '05_status_distribution_ai_ops_test' AS check_name,
  COALESCE(status, '<NULL>') AS status,
  COUNT(*) AS total
FROM sys_fee
WHERE remark = 'AI_OPS_TEST'
  AND community_id = @ops_community_id
GROUP BY COALESCE(status, '<NULL>')
ORDER BY total DESC, status ASC;

SELECT
  '06_date_range_reference_not_used_by_java' AS check_name,
  SUM(CASE WHEN create_time >= @start_time AND create_time < @end_exclusive THEN 1 ELSE 0 END) AS created_in_demo_week,
  SUM(CASE WHEN due_date >= @start_time AND due_date < @end_exclusive THEN 1 ELSE 0 END) AS due_in_demo_week,
  COUNT(*) AS total_ai_ops_test_rows
FROM sys_fee
WHERE remark = 'AI_OPS_TEST'
  AND community_id = @ops_community_id;

SELECT
  '07_rows_with_unexpected_status' AS check_name,
  id,
  community_id,
  house_id,
  building_no,
  fee_cycle,
  status,
  due_date,
  create_time,
  remark
FROM sys_fee
WHERE remark = 'AI_OPS_TEST'
  AND community_id = @ops_community_id
  AND UPPER(COALESCE(status, '')) NOT IN ('UNPAID', 'PAYING', 'OVERDUE')
ORDER BY id
LIMIT 50;

SELECT
  '08_ai_ops_test_sample_rows' AS check_name,
  id,
  community_id,
  house_id,
  building_no,
  fee_cycle,
  fee_amount,
  status,
  due_date,
  create_time,
  remark
FROM sys_fee
WHERE remark = 'AI_OPS_TEST'
  AND community_id = @ops_community_id
ORDER BY id
LIMIT 20;

SELECT
  '09_expected_vs_actual' AS check_name,
  55 AS expected_ai_ops_test_rows,
  COUNT(*) AS actual_ai_ops_test_rows,
  CASE
    WHEN COUNT(*) = 55 THEN 'OK'
    ELSE 'NOT_OK: rerun sql/ai_operations_weekly_report_test_data.sql or check insert errors'
  END AS diagnosis
FROM sys_fee
WHERE remark = 'AI_OPS_TEST'
  AND community_id = @ops_community_id;
