insert into systems (
    system_key,
    name,
    type,
    valuation_mnt,
    description,
    developer_name,
    developer_team,
    start_date,
    end_date,
    in_use,
    environment,
    base_url,
    health_url,
    swagger_url,
    status,
    created_by
)
values
    (
        'banking-transfer-service',
        'Banking Transfer Service',
        'CORE',
        15000000,
        'Харилцах данстай хоёр хэрэглэгчийн хооронд гүйлгээ хийх demo service.',
        'Demo Developer',
        'Core Banking Team',
        '2026-07-06',
        null,
        true,
        'DEV',
        'http://localhost:8084',
        'http://localhost:8084/actuator/health',
        'http://localhost:8084/swagger-ui/index.html',
        'UNKNOWN',
        1
    ),
    (
        'card-service',
        'Card Service',
        'CARD',
        12000000,
        'Картын системийн demo бүртгэл.',
        'Card Developer',
        'Card Team',
        '2026-07-06',
        null,
        true,
        'DEV',
        'http://localhost:8086',
        'http://localhost:8086/actuator/health',
        'http://localhost:8086/swagger-ui/index.html',
        'UNKNOWN',
        1
    ),
    (
        'digital-banking-service',
        'Digital Banking Service',
        'DIGITAL',
        18000000,
        'Mobile/Web banking backend demo бүртгэл.',
        'Digital Developer',
        'Digital Banking Team',
        '2026-07-06',
        null,
        true,
        'DEV',
        'http://localhost:8087',
        'http://localhost:8087/actuator/health',
        'http://localhost:8087/swagger-ui/index.html',
        'UNKNOWN',
        1
    );

insert into system_relations (source_system_id, target_system_id, relation_type, description)
values
    (1, 2, 'INTEGRATES_WITH', 'Transfer service may use card/customer profile data in later demo.'),
    (1, 3, 'CALLS', 'Digital banking channel calls transfer APIs.');
