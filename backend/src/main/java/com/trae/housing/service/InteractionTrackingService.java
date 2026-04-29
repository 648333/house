package com.trae.housing.service;

import com.trae.housing.model.Property;
import com.trae.housing.model.PropertyInteraction;
import com.trae.housing.model.User;
import com.trae.housing.repository.PropertyInteractionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InteractionTrackingService {
    @Autowired
    private PropertyInteractionRepository propertyInteractionRepository;

    public void track(User user,
                      Property property,
                      ActionType actionType,
                      String source,
                      String metadata,
                      double weight) {
        if (user == null || property == null || actionType == null) {
            return;
        }

        PropertyInteraction interaction = new PropertyInteraction();
        interaction.setUser(user);
        interaction.setProperty(property);
        interaction.setActionType(PropertyInteraction.ActionType.valueOf(actionType.name()));
        interaction.setSource(source);
        interaction.setMetadata(metadata);
        interaction.setWeight(weight);
        propertyInteractionRepository.save(interaction);
    }

    public List<PropertyInteraction> getRecentInteractionsForUser(Long userId) {
        return propertyInteractionRepository.findTop200ByUserIdOrderByCreatedAtDesc(userId);
    }

    public enum ActionType {
        VIEW,
        FAVORITE,
        COMPARE,
        INQUIRY,
        APPOINTMENT,
        REVIEW,
        SHARE,
        TOUR_OPEN,
        TOUR_SCENE_VIEW
    }
}
