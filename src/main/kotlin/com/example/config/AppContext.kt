package com.example.config

import com.example.db.DatabaseAccess
import com.example.service.AccountService
import com.example.service.AccountServiceImp
import com.example.service.SessionService
import com.example.service.SessionServiceImpl

enum class AppProfilesEnum {
    BUILD,
    RUNTIME
}

class AppContext(
    val configuration: Configuration,
    val accountService: AccountService,
    val sessionService: SessionService
)

/* Create objects for all the components we'll use */
class AppContextFactory {
    companion object {
        fun getAppContext(profile: AppProfilesEnum, configuration: Configuration): AppContext {
            when (profile) {
                AppProfilesEnum.RUNTIME -> {
                    val dao = DatabaseAccess("jdbc:sqlite:kmud.sqlite")

                    return AppContext(
                        configuration,
                        accountService = AccountServiceImp(dao),
                        sessionService = SessionServiceImpl(dao)
                    )
                }
                AppProfilesEnum.BUILD -> throw NotImplementedError("This application context has not been implemented yet: $profile")
            }
        }
    }
}
