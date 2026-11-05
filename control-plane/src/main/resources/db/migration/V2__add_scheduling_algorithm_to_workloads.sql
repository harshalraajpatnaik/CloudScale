-- V2: Add scheduling_algorithm column to workloads table
ALTER TABLE workloads ADD COLUMN IF NOT EXISTS scheduling_algorithm VARCHAR(50) DEFAULT 'RESOURCE_AWARE';
UPDATE workloads SET scheduling_algorithm = 'RESOURCE_AWARE' WHERE scheduling_algorithm IS NULL;
