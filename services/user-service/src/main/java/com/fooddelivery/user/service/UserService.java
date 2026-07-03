package com.fooddelivery.user.service;

import com.fooddelivery.common.lib.exception.BadRequestException;
import com.fooddelivery.common.lib.exception.NotFoundException;
import com.fooddelivery.common.lib.exception.UnauthorizedException;
import com.fooddelivery.user.dto.AuthResponse;
import com.fooddelivery.user.dto.LoginRequest;
import com.fooddelivery.user.dto.RegisterRequest;
import com.fooddelivery.user.dto.UserAddressRequest;
import com.fooddelivery.user.dto.UserAddressResponse;
import com.fooddelivery.user.dto.UserResponse;
import com.fooddelivery.user.entity.User;
import com.fooddelivery.user.entity.UserAddress;
import com.fooddelivery.user.entity.UserRole;
import com.fooddelivery.user.repository.UserAddressRepository;
import com.fooddelivery.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserAddressRepository addressRepository;
    private final KeycloakService keycloakService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(UserRole.CUSTOMER)
                .active(true)
                .build();

        user = userRepository.save(user);
        keycloakService.createKeycloakUser(user, request.getPassword());

        return AuthResponse.builder()
                .token("dummy-token-" + user.getId())
                .user(toUserResponse(user))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(UnauthorizedException::invalidCredentials);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw UnauthorizedException.invalidCredentials();
        }

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        return AuthResponse.builder()
                .token("dummy-token-" + user.getId())
                .user(toUserResponse(user))
                .build();
    }

    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User", id));
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UserResponse request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User", id));

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());

        user = userRepository.save(user);
        return toUserResponse(user);
    }

    public List<UserAddressResponse> getAddresses(UUID userId) {
        return addressRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toAddressResponse)
                .toList();
    }

    @Transactional
    public UserAddressResponse addAddress(UUID userId, UserAddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));

        if (request.isDefault()) {
            unsetDefaultAddresses(userId);
        }

        UserAddress address = UserAddress.builder()
                .user(user)
                .label(request.getLabel())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .isDefault(request.isDefault())
                .build();

        address = addressRepository.save(address);
        return toAddressResponse(address);
    }

    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new NotFoundException("Address", addressId));

        if (!address.getUser().getId().equals(userId)) {
            throw new BadRequestException("Address does not belong to user");
        }

        addressRepository.delete(address);
    }

    private void unsetDefaultAddresses(UUID userId) {
        List<UserAddress> defaults = addressRepository.findByUserIdAndIsDefaultTrue(userId);
        defaults.forEach(addr -> addr.setDefault(false));
        addressRepository.saveAll(defaults);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .role(user.getRole())
                .active(user.isActive())
                .build();
    }

    private UserAddressResponse toAddressResponse(UserAddress address) {
        return UserAddressResponse.builder()
                .id(address.getId())
                .label(address.getLabel())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .pincode(address.getPincode())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .isDefault(address.isDefault())
                .build();
    }
}
