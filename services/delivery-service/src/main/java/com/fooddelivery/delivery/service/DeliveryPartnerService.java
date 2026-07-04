package com.fooddelivery.delivery.service;

import com.fooddelivery.common.lib.exception.BadRequestException;
import com.fooddelivery.common.lib.exception.NotFoundException;
import com.fooddelivery.delivery.dto.DeliveryPartnerRequest;
import com.fooddelivery.delivery.dto.DeliveryPartnerResponse;
import com.fooddelivery.delivery.entity.DeliveryPartner;
import com.fooddelivery.delivery.repository.DeliveryPartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryPartnerService {

    private final DeliveryPartnerRepository repository;

    @Transactional
    public DeliveryPartnerResponse register(DeliveryPartnerRequest request) {
        repository.findByUserId(request.getUserId()).ifPresent(p -> {
            throw new BadRequestException("Delivery partner already registered for this user");
        });

        DeliveryPartner partner = DeliveryPartner.builder()
                .userId(request.getUserId())
                .vehicleType(request.getVehicleType())
                .licenseNumber(request.getLicenseNumber())
                .build();

        partner = repository.save(partner);
        log.info("Delivery partner registered: {}", partner.getId());
        return toResponse(partner);
    }

    @Transactional
    public DeliveryPartnerResponse verify(UUID partnerId) {
        DeliveryPartner partner = findPartner(partnerId);
        partner.setIsVerified(true);
        partner = repository.save(partner);
        log.info("Delivery partner verified: {}", partnerId);
        return toResponse(partner);
    }

    @Transactional
    public DeliveryPartnerResponse toggleOnline(UUID partnerId) {
        DeliveryPartner partner = findPartner(partnerId);
        if (!partner.getIsVerified()) {
            throw new BadRequestException("Partner must be verified before going online");
        }
        partner.setIsOnline(!partner.getIsOnline());
        partner = repository.save(partner);
        log.info("Delivery partner {} is now {}", partnerId, partner.getIsOnline() ? "online" : "offline");
        return toResponse(partner);
    }

    public DeliveryPartnerResponse getPartner(UUID partnerId) {
        return toResponse(findPartner(partnerId));
    }

    private DeliveryPartner findPartner(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Delivery partner", id));
    }

    private DeliveryPartnerResponse toResponse(DeliveryPartner p) {
        return DeliveryPartnerResponse.builder()
                .id(p.getId()).userId(p.getUserId())
                .vehicleType(p.getVehicleType()).licenseNumber(p.getLicenseNumber())
                .isVerified(p.getIsVerified()).isOnline(p.getIsOnline())
                .currentLatitude(p.getCurrentLatitude()).currentLongitude(p.getCurrentLongitude())
                .averageRating(p.getAverageRating()).createdAt(p.getCreatedAt())
                .build();
    }
}
