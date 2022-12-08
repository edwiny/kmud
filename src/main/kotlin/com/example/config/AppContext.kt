package com.example.config

import com.example.ctx.SessionCtx
import com.example.ctx.SessionCtxManager
import com.example.ctx.SessionCtxManagerInterface
import com.example.db.DatabaseAccess
import com.example.service.AccountService
import com.example.service.AccountServiceImp
import com.example.service.SessionService
import com.example.service.SessionServiceImpl
import com.example.commands.*

enum class AppProfilesEnum {
    BUILD,
    RUNTIME
}

class AppContext(
    val configuration: Configuration,
    val accountService: AccountService,
    val sessionService: SessionService,
    val sessionCtxManager: SessionCtxManagerInterface,
    val commandRepository: CommandRepository
)

/* Create objects for all the components we'll use */
class AppContextFactory {
    companion object {
        fun getAppContext(profile: AppProfilesEnum, configuration: Configuration): AppContext {
            when (profile) {
                AppProfilesEnum.RUNTIME -> {
                    val dao = DatabaseAccess("jdbc:sqlite:kmud.sqlite")
                    val sessionService = SessionServiceImpl(dao)

                    return AppContext(
                        configuration,
                        accountService = AccountServiceImp(dao),
                        sessionService = sessionService,
                        sessionCtxManager = SessionCtxManager(sessionService),
                        commandRepository = setupCommands(RegexCommandParser())
                    )
                }
                AppProfilesEnum.BUILD -> throw NotImplementedError("This application context has not been implemented yet: $profile")
            }
        }

        fun setupCommands(parser: CommandParser): CommandRepository {
            val mgr = CommandRepository(parser)
            mgr.addCommand(::LoginCommand)
            mgr.addCommand(::CharGenCommand)
            mgr.addCommand(::CharListCommand)
            mgr.addCommand(::CharDeleteCommand)
            mgr.addCommand(::AccountCreateCommand)
            mgr.addCommand(::CharPuppetCommand)
            mgr.addCommand(::LogoutCommand)
            return mgr
        }

    }
}
