package com.iotcloudmanager.backend.repository;

import com.iotcloudmanager.backend.entity.MeasurementType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeasurementTypeRepository extends JpaRepository<MeasurementType, String> {

    Optional<MeasurementType> findByTypeAndEnabledTrue(String type);

    List<MeasurementType> findByEnabledTrue();
}