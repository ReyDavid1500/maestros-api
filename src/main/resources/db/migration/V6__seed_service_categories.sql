INSERT INTO
    service_categories (
        id,
        name,
        icon_name,
        display_order,
        created_at,
        updated_at
    )
VALUES (
        gen_random_uuid (),
        'Limpieza del hogar',
        'sparkles-outline',
        1,
        NOW(),
        NOW()
    ),
    (
        gen_random_uuid (),
        'Aires acondicionados',
        'thermometer-outline',
        2,
        NOW(),
        NOW()
    ),
    (
        gen_random_uuid (),
        'Reparaciones generales',
        'hammer-outline',
        3,
        NOW(),
        NOW()
    ),
    (
        gen_random_uuid (),
        'Electricidad',
        'flash-outline',
        4,
        NOW(),
        NOW()
    ),
    (
        gen_random_uuid (),
        'Gasfitería / Plomería',
        'water-outline',
        5,
        NOW(),
        NOW()
    ),
    (
        gen_random_uuid (),
        'Pintura',
        'color-palette-outline',
        6,
        NOW(),
        NOW()
    ),
    (
        gen_random_uuid (),
        'Mudanzas',
        'car-outline',
        7,
        NOW(),
        NOW()
    ),
    (
        gen_random_uuid (),
        'Jardinería',
        'leaf-outline',
        8,
        NOW(),
        NOW()
    ),
    (
        gen_random_uuid (),
        'Cerrajería',
        'key-outline',
        9,
        NOW(),
        NOW()
    ),
    (
        gen_random_uuid (),
        'Instalaciones',
        'tv-outline',
        10,
        NOW(),
        NOW()
    );