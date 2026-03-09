package com.maestros.model.postgres;

import com.maestros.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ServiceCategory extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "icon_name", nullable = false)
    private String iconName;

    @Column(name = "display_order")
    private Integer displayOrder;
}
