package com.library.model;

import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a registered library patron/member.
 * user_id maps to the security user identity in the separate User Management module.
 */
public class Member {
    private Long memberId;
    private Long userId; // Foreign key cross-reference to user_auth_db
    private String membershipNo;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String membershipStatus; // ACTIVE, SUSPENDED, EXPIRED
    private String membershipTier;   // STANDARD, STUDENT, FACULTY, PREMIUM
    private int maxActiveLoans;
    private int currentLoanCount;
    private LocalDate joinedDate;
    private LocalDate expiryDate;
    private Instant createdAt;
    private Instant updatedAt;

    // Associated circulation collections
    private List<Loan> activeLoans = new ArrayList<>();
    private List<Reservation> activeReservations = new ArrayList<>();

    public Member() {
    }

    public Member(Long memberId, Long userId, String membershipNo, String firstName, String lastName, String email) {
        this.memberId = memberId;
        this.userId = userId;
        this.membershipNo = membershipNo;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.membershipStatus = "ACTIVE";
        this.membershipTier = "STANDARD";
        this.maxActiveLoans = 5;
        this.currentLoanCount = 0;
        this.joinedDate = LocalDate.now();
        this.expiryDate = LocalDate.now().plusYears(1);
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean canBorrow() {
        return "ACTIVE".equalsIgnoreCase(membershipStatus) &&
               (expiryDate == null || expiryDate.isAfter(LocalDate.now())) &&
               currentLoanCount < maxActiveLoans;
    }

    // Getters and Setters
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getMembershipNo() { return membershipNo; }
    public void setMembershipNo(String membershipNo) { this.membershipNo = membershipNo; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getMembershipStatus() { return membershipStatus; }
    public void setMembershipStatus(String membershipStatus) { this.membershipStatus = membershipStatus; }

    public String getMembershipTier() { return membershipTier; }
    public void setMembershipTier(String membershipTier) { this.membershipTier = membershipTier; }

    public int getMaxActiveLoans() { return maxActiveLoans; }
    public void setMaxActiveLoans(int maxActiveLoans) { this.maxActiveLoans = maxActiveLoans; }

    public int getCurrentLoanCount() { return currentLoanCount; }
    public void setCurrentLoanCount(int currentLoanCount) { this.currentLoanCount = currentLoanCount; }

    public LocalDate getJoinedDate() { return joinedDate; }
    public void setJoinedDate(LocalDate joinedDate) { this.joinedDate = joinedDate; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<Loan> getActiveLoans() { return activeLoans; }
    public void setActiveLoans(List<Loan> activeLoans) { this.activeLoans = activeLoans; }

    public List<Reservation> getActiveReservations() { return activeReservations; }
    public void setActiveReservations(List<Reservation> activeReservations) { this.activeReservations = activeReservations; }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
