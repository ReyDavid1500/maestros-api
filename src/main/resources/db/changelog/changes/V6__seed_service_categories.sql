--liquibase formatted sql

--changeset maestros:6 labels:setup
INSERT INTO
    service_categories (
        name,
        icon_name,
        created_at,
        updated_at
    )
VALUES (
        N'Limpieza del hogar',
        'sparkles-outline',
        GETUTCDATE (),
        GETUTCDATE ()
    ),
    (
        N'Aires acondicionados',
        'thermometer-outline',
        GETUTCDATE (),
        GETUTCDATE ()
    ),
    (
        N'Reparaciones generales',
        'hammer-outline',
        GETUTCDATE (),
        GETUTCDATE ()
    ),
    (
        N'Electricidad',
        'flash-outline',
        GETUTCDATE (),
        GETUTCDATE ()
    ),
    (
        N'Gasfitería / Plomería',
        'water-outline',
        GETUTCDATE (),
        GETUTCDATE ()
    ),
    (
        N'Pintura',
        'color-palette-outline',
        GETUTCDATE (),
        GETUTCDATE ()
    ),
    (
        N'Mudanzas',
        'car-outline',
        GETUTCDATE (),
        GETUTCDATE ()
    ),
    (
        N'Jardinería',
        'leaf-outline',
        GETUTCDATE (),
        GETUTCDATE ()
    ),
    (
        N'Cerrajería',
        'key-outline',
        GETUTCDATE (),
        GETUTCDATE ()
    ),
    (
        N'Instalaciones',
        'tv-outline',
        GETUTCDATE (),
        GETUTCDATE ()
    );