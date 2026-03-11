package com.maestros.model.postgres;

import com.maestros.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "maestro_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MaestroProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 1000)
    private String description;

    @Column(name = "average_rating", nullable = false)
    @Builder.Default
    private Double averageRating = 0.0;

    @Column(name = "total_jobs", nullable = false)
    @Builder.Default
    private Integer totalJobs = 0;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private boolean isVerified = false;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private boolean isAvailable = true;

    @Column(length = 100)
    private String city;

    @OneToMany(mappedBy = "maestroProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MaestroService> services = new ArrayList<>();
}
