package com.petway.pet.entity;

import com.petway.pet.enums.ActivityLevel;
import com.petway.pet.enums.CrowdTolerance;
import com.petway.pet.enums.PlacePreference;
import com.petway.pet.enums.PetSizeType;
import com.petway.pet.enums.SocialityLevel;
import com.petway.pet.enums.DogBreed;
import com.petway.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "pet",
        indexes = {
                @Index(
                        name = "idx_pet_user_id",
                        columnList = "user_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pet_id")
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;


    @Column(
            name = "pet_name",
            nullable = false,
            length = 50
    )
    private String petName;


    @Column(
            name = "breed",
            length = 100
    )
    private String breedDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = "breed_type", length = 50)
    private DogBreed breed;

    @Column(name = "animal_registered")
    private Boolean animalRegistered;


    @Column(name = "birth_date")
    private LocalDate birthDate;


    @Column(
            name = "weight_kg",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal weightKg;


    @Enumerated(EnumType.STRING)
    @Column(
            name = "size_type",
            length = 20
    )
    private PetSizeType sizeType;


    @Enumerated(EnumType.STRING)
    @Column(
            name = "activity_level",
            length = 20
    )
    private ActivityLevel activityLevel;


    @Enumerated(EnumType.STRING)
    @Column(
            name = "crowd_tolerance",
            length = 20
    )
    private CrowdTolerance crowdTolerance;


    @Enumerated(EnumType.STRING)
    @Column(
            name = "place_preference",
            length = 20
    )
    private PlacePreference placePreference;


    @Enumerated(EnumType.STRING)
    @Column(
            name = "sociality_level",
            length = 20
    )
    private SocialityLevel socialityLevel;


    @Column(name = "vaccinated")
    private Boolean vaccinated;


    @Column(name = "neutered")
    private Boolean neutered;


    @Column(
            name = "is_primary",
            nullable = false
    )
    private Boolean isPrimary;


    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;


    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;


    public static Pet create(
            User user,
            String petName,
            String breedDetail,
            DogBreed breed,
            Boolean animalRegistered,
            LocalDate birthDate,
            BigDecimal weightKg,
            PetSizeType sizeType,
            ActivityLevel activityLevel,
            CrowdTolerance crowdTolerance,
            PlacePreference placePreference,
            SocialityLevel socialityLevel,
            Boolean vaccinated,
            Boolean neutered,
            boolean isPrimary
    ) {

        Pet pet = new Pet();

        pet.user = user;
        pet.petName = petName;
        pet.breedDetail = breedDetail;
        pet.breed = breed;
        pet.animalRegistered = animalRegistered;
        pet.birthDate = birthDate;
        pet.weightKg = weightKg;
        pet.sizeType = sizeType;
        pet.activityLevel = activityLevel;
        pet.crowdTolerance = crowdTolerance;
        pet.placePreference = placePreference;
        pet.socialityLevel = socialityLevel;
        pet.vaccinated = vaccinated;
        pet.neutered = neutered;
        pet.isPrimary = isPrimary;

        pet.createdAt = LocalDateTime.now();
        pet.updatedAt = LocalDateTime.now();

        return pet;
    }


    public void update(
            String petName,
            String breedDetail,
            DogBreed breed,
            Boolean animalRegistered,
            LocalDate birthDate,
            BigDecimal weightKg,
            PetSizeType sizeType,
            ActivityLevel activityLevel,
            CrowdTolerance crowdTolerance,
            PlacePreference placePreference,
            SocialityLevel socialityLevel,
            Boolean vaccinated,
            Boolean neutered
    ) {

        this.petName = petName;
        this.breedDetail = breedDetail;
        this.breed = breed;
        this.animalRegistered = animalRegistered;
        this.birthDate = birthDate;
        this.weightKg = weightKg;
        this.sizeType = sizeType;
        this.activityLevel = activityLevel;
        this.crowdTolerance = crowdTolerance;
        this.placePreference = placePreference;
        this.socialityLevel = socialityLevel;
        this.vaccinated = vaccinated;
        this.neutered = neutered;
    }


    public void setPrimary(boolean primary) {

        this.isPrimary = primary;
    }


    @PreUpdate
    public void preUpdate() {

        this.updatedAt = LocalDateTime.now();
    }
}
