package cm.aptoide.pt.view.account;

import android.content.Context;
import android.support.annotation.StringRes;
import cm.aptoide.accountmanager.AccountException;
import cm.aptoide.accountmanager.AccountValidationException;
import cm.aptoide.pt.R;
import cm.aptoide.pt.account.ErrorsMapper;
import cm.aptoide.pt.dataprovider.util.ErrorUtils;
import cm.aptoide.pt.view.ThrowableToStringMapper;

public class AccountErrorMapper implements ThrowableToStringMapper {

  private final Context context;

  public AccountErrorMapper(Context context) {
    this.context = context;
  }

  @Override public String map(Throwable throwable) {
    String message = context.getString(R.string.unknown_error);

    if (throwable instanceof AccountException) {

      if (((AccountException) throwable).hasCode()) {
        message = context.getString(
            ErrorsMapper.getWebServiceErrorMessageFromCode(((AccountException) throwable).getCode(),
                context.getApplicationContext()
                    .getPackageName(), context.getResources()));
      } else {
        @StringRes int errorId = context.getResources()
            .getIdentifier("error_" + ((AccountException) throwable).getError(), "string",
                context.getPackageName());
        message = context.getString(errorId);
      }
    } else if (throwable instanceof AccountValidationException) {
      switch (((AccountValidationException) throwable).getCode()) {
        case AccountValidationException.EMPTY_EMAIL_AND_PASSWORD:
          message = context.getString(R.string.no_email_and_pass_error_message);
          break;
        case AccountValidationException.EMPTY_EMAIL:
          message = context.getString(R.string.no_email_error_message);
          break;
        case AccountValidationException.EMPTY_NAME:
          message = context.getString(R.string.nothing_inserted_user);
          break;
        case AccountValidationException.EMPTY_PASSWORD:
          message = context.getString(R.string.no_pass_error_message);
          break;
        case AccountValidationException.INVALID_PASSWORD:
          message = context.getString(R.string.password_validation_text);
          break;
      }
    } else if (ErrorUtils.isNoNetworkConnection(throwable)) {
      message = context.getString(R.string.connection_error);
    }
    return message;
  }
}
