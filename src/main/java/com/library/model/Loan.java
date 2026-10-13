package com.library.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Represents a checkout transaction for either a PhysicalCopy or DigitalLicence.
 */
public class Loan {
    private Long loanId;
    private Long memberId;
    private LoanType loanType; // PHYSICAL or DIGITAL
    private Long copyId;       // Populated when loanType == PHYSICAL
    private Long licenceId;    // Populated when loanType == DIGITAL
    private Instant checkoutDate;
    private Instant dueDate;
    private Instant returnedDate;
    private LoanStatus status; // ACTIVE, RETURNED, OVERDUE, LOST
    private int renewalCount;
    private int maxRenewals;
    private BigDecimal fineAmount;
    private boolean finePaid;
    private Long issuedByUserId;
    private Instant createdAt;
    private Instant updatedAt;

    // Optional eager associations
    private Member member;
    private PhysicalCopy physicalCopy;
    private DigitalLicence digitalLicence;

    public Loan() {
        this.status = LoanStatus.ACTIVE;
        this.renewalCount = 0;
        this.maxRenewals = 2;
        this.fineAmount = BigDecimal.ZERO;
        this.finePaid = false;
    }

    public static Loan createPhysicalLoan(Long memberId, Long copyId, int durationDays, Long librarianUserId) {
        Loan loan = new Loan();
        loan.setMemberId(memberId);
        loan.setLoanType(LoanType.PHYSICAL);
        loan.setCopyId(copyId);
        loan.setCheckoutDate(Instant.now());
        loan.setDueDate(Instant.now().plus(durationDays, ChronoUnit.DAYS));
        loan.setIssuedByUserId(librarianUserId);
        loan.setCreatedAt(Instant.now());
        loan.setUpdatedAt(Instant.now());
        return loan;
    }

    public static Loan createDigitalLoan(Long memberId, Long licenceId, int durationDays) {
        Loan loan = new Loan();
        loan.setMemberId(memberId);
        loan.setLoanType(LoanType.DIGITAL);
        loan.setLicenceId(licenceId);
        loan.setCheckoutDate(Instant.now());
        loan.setDueDate(Instant.now().plus(durationDays, ChronoUnit.DAYS));
        loan.setCreatedAt(Instant.now());
        loan.setUpdatedAt(Instant.now());
        return loan;
    }

    public boolean canRenew() {
        return status == LoanStatus.ACTIVE && renewalCount < maxRenewals;
    }

    public void renew(int additionalDays) {
        if (!canRenew()) {
            throw new IllegalStateException("Loan cannot be renewed (max renewals reached or overdue)");
        }
        this.dueDate = this.dueDate.plus(additionalDays, ChronoUnit.DAYS);
        this.renewalCount++;
        this.updatedAt = Instant.now();
    }

    public void markReturned() {
        this.returnedDate = Instant.now();
        this.status = LoanStatus.RETURNED;
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public LoanType getLoanType() { return loanType; }
    public void setLoanType(LoanType loanType) { this.loanType = loanType; }

    public Long getCopyId() { return copyId; }
    public void setCopyId(Long copyId) { this.copyId = copyId; }

    public Long getLicenceId() { return licenceId; }
    public void setLicenceId(Long licenceId) { this.licenceId = licenceId; }

    public Instant getCheckoutDate() { return checkoutDate; }
    public void setCheckoutDate(Instant checkoutDate) { this.checkoutDate = checkoutDate; }

    public Instant getDueDate() { return dueDate; }
    public void setDueDate(Instant dueDate) { this.dueDate = dueDate; }

    public Instant getReturnedDate() { return returnedDate; }
    public void setReturnedDate(Instant returnedDate) { this.returnedDate = returnedDate; }

    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }

    public int getRenewalCount() { return renewalCount; }
    public void setRenewalCount(int renewalCount) { this.renewalCount = renewalCount; }

    public int getMaxRenewals() { return maxRenewals; }
    public void setMaxRenewals(int maxRenewals) { this.maxRenewals = maxRenewals; }

    public BigDecimal getFineAmount() { return fineAmount; }
    public void setFineAmount(BigDecimal fineAmount) { this.fineAmount = fineAmount; }

    public boolean isFinePaid() { return finePaid; }
    public void setFinePaid(boolean finePaid) { this.finePaid = finePaid; }

    public Long getIssuedByUserId() { return issuedByUserId; }
    public void setIssuedByUserId(Long issuedByUserId) { this.issuedByUserId = issuedByUserId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }

    public PhysicalCopy getPhysicalCopy() { return physicalCopy; }
    public void setPhysicalCopy(PhysicalCopy physicalCopy) { this.physicalCopy = physicalCopy; }

    public DigitalLicence getDigitalLicence() { return digitalLicence; }
    public void setDigitalLicence(DigitalLicence digitalLicence) { this.digitalLicence = digitalLicence; }
}
