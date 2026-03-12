--liquibase formatted sql

--changeset maestros:6 labels:setup
INSERT INTO
    service_categories (
        id,
        name,
        icon,
        created_at,
        updated_at
    )
VALUES (
        NEWID (),
        N 'Limpieza del hogar',
        'sparkles-outline',
        GETUTCDATE (),
        GETUTCDATE ()
    ),
    (
        NEWID (),
        N 'Aires acondicionados',
        'thermometer-outline',
        GETUTCDATE (),
        GETUTCDATE ()
    ),
    (
        NEWID (),
        N 'Reparaciones generales',
        'hammer-outline',
        GETUTCDATE (),
        GETUTCDATE ()
    ),
    (
        NEWID (),
        N 'Electricidad',
        'flash-outline',
        GETUTCDATE (),
        GETUTCDATE ()
    ),
    (
        NEWID (),
        N 'Gasfitería / Plomería',
        'water-outline',
        GETUTCDATE (),
        GETUTCDATE ()
    ),
    (
        NEWID (),
        N 'Pintura',
        'color-palette-outline',
        GETUTCDATE (),
        GETUTCDATE ()
    ),
    (
        NEWID (),
        N 'Mudanzas',
        'car-outline',
        GETUTCDATE (),
        GETUTCDATE ()
    ),
    (
        NEWID (),
        N 'Jardinería',
        'leaf-outline',
        GETUTCDATE (),
        GETUTCDATE ()
    ),
    (
        NEWID (),
        N 'Cerrajería',
        'key-outline',
        GETUTCDATE (),
        GETUTCDATE ()
    ),
    (
        NEWID (),
        N 'Instalaciones',
        'tv-outline',
        GETUTCDATE (),
        GETUTCDATE ()
    );