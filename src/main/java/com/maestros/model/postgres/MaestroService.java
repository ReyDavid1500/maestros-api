package com.maestros.model.postgres;

import com.maestros.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "maestro_services", uniqueConstraints = @UniqueConstraint(name = "uk_maestro_services_profile_category", columnNames = {
        "maestro_profile_id", "service_category_id" }))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MaestroService extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maestro_profile_id", nullable = false)
    private MaestroProfile maestroProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_category_id", nullable = false)
    private ServiceCategory serviceCategory;

    @Column(name = "price_clp", nullable = false)
    private Long priceClp;

    @Column(name = "estimated_time", nullable = false, length = 50)
    private String estimatedTime;
}
