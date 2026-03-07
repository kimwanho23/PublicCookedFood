package kwh.PublicCookedFood.account.exception;

import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.account.error.AccountRecoveryErrorCode;

public class PasswordResetCodeDeliveryException extends AppException {

    public PasswordResetCodeDeliveryException(String message, Throwable cause) {
        super(AccountRecoveryErrorCode.PASSWORD_RESET_CODE_DELIVERY_FAILED, message, cause);
    }
}
