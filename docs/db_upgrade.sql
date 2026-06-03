-- ==============================================================================
-- DDL Upgrade Script: PostgreSQL 16 + PostGIS 3.4
-- Target Tables: ZDGC (spatial_project) and SUPPLY_MERCHANT (biz_merchant)
-- ==============================================================================

-- Enable PostGIS extension if not already enabled
CREATE EXTENSION IF NOT EXISTS postgis;

-- 1. Upgrade ZDGC (重点工程表 / spatial_project)
-- Add approval_stage: 0-规划许可/即将开工, 1-主体施工, 2-人防/消防验收, 3-综合验收/完工
ALTER TABLE ZDGC ADD COLUMN IF NOT EXISTS approval_stage SMALLINT DEFAULT 1;

-- Add worker_scale_est: 工地常驻工人数预估
ALTER TABLE ZDGC ADD COLUMN IF NOT EXISTS worker_scale_est INTEGER DEFAULT 500;

-- Add catering_saturation: 周边餐饮配套饱和度 (0-极度空缺, 1-基本饱和)
ALTER TABLE ZDGC ADD COLUMN IF NOT EXISTS catering_saturation SMALLINT DEFAULT 0;

-- Add gate_geom: Geometry(Point, 4326) 工地大门的精准空间点坐标
ALTER TABLE ZDGC ADD COLUMN IF NOT EXISTS gate_geom GEOMETRY(Point, 4326);

-- Create spatial index for gate_geom using GIST
CREATE INDEX IF NOT EXISTS idx_zdgc_gate_geom ON ZDGC USING GIST (gate_geom);


-- 2. Upgrade SUPPLY_MERCHANT (商户表 / biz_merchant)
-- Add sub_category: heavy_machinery, hardware, ceylon_bento, minisuper, local_labor
ALTER TABLE SUPPLY_MERCHANT ADD COLUMN IF NOT EXISTS sub_category VARCHAR(50) DEFAULT 'hardware';

-- Add source_region: 雄安本地, 保定仓, 定州基地
ALTER TABLE SUPPLY_MERCHANT ADD COLUMN IF NOT EXISTS source_region VARCHAR(50) DEFAULT '雄安本地';

-- Ensure we have a GIST index on coordinates if represented as geometry, 
-- or we use standard columns (POINT_X, POINT_Y) which can be dynamically casted or indexed.
-- To perfectly support ST_DWithin and GIST calculations, let's add a location geometry column:
ALTER TABLE SUPPLY_MERCHANT ADD COLUMN IF NOT EXISTS location GEOMETRY(Point, 4326);

-- Populate location geom column from POINT_X (longitude) and POINT_Y (latitude) if they exist
UPDATE SUPPLY_MERCHANT 
SET location = ST_SetSRID(ST_Point(CAST(POINT_X AS DOUBLE PRECISION), CAST(POINT_Y AS DOUBLE PRECISION)), 4326)
WHERE POINT_X IS NOT NULL AND POINT_Y IS NOT NULL AND location IS NULL;

-- Create GIST spatial index on merchant location
CREATE INDEX IF NOT EXISTS idx_merchant_location ON SUPPLY_MERCHANT USING GIST (location);

-- ==============================================================================
-- Verification Queries
-- ==============================================================================
-- Verify table extensions:
-- SELECT id, name, approval_stage, worker_scale_est, ST_AsText(gate_geom) FROM ZDGC;
-- SELECT id, merchant_name, sub_category, source_region, ST_AsText(location) FROM SUPPLY_MERCHANT;
