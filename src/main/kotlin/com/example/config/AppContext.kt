package com.example.config

import com.example.commands.Interpreter
import com.example.db.DatabaseAccess
import com.example.repository.DatabaseAccessInterface

enum class AppProfilesEnum {
    BUILD,
    RUNTIME
}

class AppContext(val configuration: Configuration,
                 val dao: DatabaseAccessInterface)


/* Create objects for all the components we'll use */
class AppContextFactory {
    companion object {
        fun getAppContext(profile: AppProfilesEnum, configuration: Configuration): AppContext {
            when (profile) {
                AppProfilesEnum.RUNTIME -> return AppContext(
                    configuration,
                    dao = DatabaseAccess("jdbc:sqlite:kmud.sqlite"),
                )
                AppProfilesEnum.BUILD -> throw NotImplementedError("This application context has not been implemented yet: $profile")
            }
        }
    }
}
