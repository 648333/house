package com.trae.housing.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "properties")
public class Property {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private String address;

    private Double latitude;
    private Double longitude;
    private Double area;
    private String type; // e.g., "Apartment", "Villa"
    private String decoration; // e.g., "Fine", "Simple"
    private String orientation; // e.g., "South", "North"
    private String floor; // e.g., "Middle/18"
    private String yearBuilt; // e.g., "2015"
    private String tags; // e.g., "Subway,School"
    private String layout; // e.g., "3 Bedroom 2 Living Room"
    
    private String contactName; // Display name for the contact person

    private String imageUrl;
    private String panoramaUrl;
    private String model3dUrl;
    private String floorPlanUrl;

    @Column(columnDefinition = "TEXT")
    private String panoramaImages;

    @Column(columnDefinition = "TEXT")
    private String tourMetadata;

    @Column(columnDefinition = "TEXT")
    private String furnishingPlan;

    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    private LocalDateTime createdAt;

    public Property() {}

    public Property(Long id, String title, String description, BigDecimal price, String address, String imageUrl, Status status, User owner, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.address = address;
        this.imageUrl = imageUrl;
        this.status = status;
        this.owner = owner;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = Status.PENDING;
        }
    }

    public enum Status {
        PENDING, APPROVED, REJECTED, SOLD
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    
    public Double getArea() { return area; }
    public void setArea(Double area) { this.area = area; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDecoration() { return decoration; }
    public void setDecoration(String decoration) { this.decoration = decoration; }

    public String getOrientation() { return orientation; }
    public void setOrientation(String orientation) { this.orientation = orientation; }

    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }

    public String getYearBuilt() { return yearBuilt; }
    public void setYearBuilt(String yearBuilt) { this.yearBuilt = yearBuilt; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getLayout() { return layout; }
    public void setLayout(String layout) { this.layout = layout; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getPanoramaUrl() { return panoramaUrl; }
    public void setPanoramaUrl(String panoramaUrl) { this.panoramaUrl = panoramaUrl; }
    public String getModel3dUrl() { return model3dUrl; }
    public void setModel3dUrl(String model3dUrl) { this.model3dUrl = model3dUrl; }
    public String getFloorPlanUrl() { return floorPlanUrl; }
    public void setFloorPlanUrl(String floorPlanUrl) { this.floorPlanUrl = floorPlanUrl; }
    public String getPanoramaImages() { return panoramaImages; }
    public void setPanoramaImages(String panoramaImages) { this.panoramaImages = panoramaImages; }
    public String getTourMetadata() { return tourMetadata; }
    public void setTourMetadata(String tourMetadata) { this.tourMetadata = tourMetadata; }
    public String getFurnishingPlan() { return furnishingPlan; }
    public void setFurnishingPlan(String furnishingPlan) { this.furnishingPlan = furnishingPlan; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
