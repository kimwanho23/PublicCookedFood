package kwh.PublicCookedFood.metrics.reco;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecipeScoreEventPublisherUnitTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private RecipeScoreEventPublisher recipeScoreEventPublisher;

    @Test
    void publishRecalculateRequest_publishesEvent() {
        recipeScoreEventPublisher.publishRecalculateRequest(30L, "REVIEW_UPSERT");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        Object published = captor.getValue();
        assertThat(published).isInstanceOf(RecipeScoreRecalculateRequestedEvent.class);
        RecipeScoreRecalculateRequestedEvent event = (RecipeScoreRecalculateRequestedEvent) published;
        assertThat(event.recipeId()).isEqualTo(30L);
        assertThat(event.reason()).isEqualTo("REVIEW_UPSERT");
    }

    @Test
    void publishRecalculateRequest_ignoresNullRecipeId() {
        recipeScoreEventPublisher.publishRecalculateRequest(null, "UNKNOWN");

        verify(applicationEventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }
}
