package com.trae.housing.repository;

import com.trae.housing.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByUserId(Long userId);
    List<Appointment> findByPropertyId(Long propertyId);
    // Find appointments for properties owned by a specific user (for agents)
    List<Appointment> findByPropertyOwnerId(Long ownerId);
    boolean existsByPropertyIdAndAppointmentTimeAndStatusIn(Long propertyId, LocalDateTime appointmentTime, List<Appointment.Status> statuses);
}
