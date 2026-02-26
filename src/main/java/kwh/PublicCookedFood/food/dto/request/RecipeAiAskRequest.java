package kwh.PublicCookedFood.food.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RecipeAiAskRequest {

    @NotBlank(message = "질문을 입력해주세요.")
    @Size(max = 500, message = "질문은 500자 이하로 입력해주세요.")
    private String question;

    public String normalizedQuestion() {
        if (question == null) {
            return null;
        }
        String trimmed = question.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
