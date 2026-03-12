--liquibase formatted sql

--changeset maestros:7 labels:setup
ALTER TABLE maestro_profiles ADD city NVARCHAR (100);