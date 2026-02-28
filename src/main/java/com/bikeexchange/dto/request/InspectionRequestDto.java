package com.bikeexchange.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class InspectionRequestDto {
    private Long bikeId;

    /** Preferred date for the inspection */
    private LocalDate preferredDate;

    /** Preferred time slot, e.g. "MORNING", "AFTERNOON", "09:00-12:00" */
    private String preferredTimeSlot;

    /** Address where the bike can be inspected */
    private String address;

    /** Contact phone for the inspector to reach the seller */
    private String contactPhone;

    /** Additional notes from the seller */
    private String notes;
}
