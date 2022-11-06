package com.example.config

enum class EnvironmentEnum {
    DEV, PROD;

    override fun toString(): String {
        return this.name.toLowerCase()
    }
    companion object {
        @JvmStatic
        fun fromString(inputString: String?): EnvironmentEnum {
            val returnVal = EnvironmentEnum.values().singleOrNull {
                inputString != null && inputString.toLowerCase().contentEquals(it.toString())
            }
            return returnVal ?: throw IllegalArgumentException("Invalid environment value: $inputString")
        }
    }
}

class Configuration(
    val env: EnvironmentEnum,
    var verbose: Boolean = false
)

class ConfigurationFactory() {
    companion object {
        fun getConfigForEnvironment(environment: EnvironmentEnum): Configuration {
            when (environment) {
                EnvironmentEnum.PROD ->
                    return Configuration(
                        env = EnvironmentEnum.PROD,
                        verbose = false
                    )
                EnvironmentEnum.DEV ->
                    return Configuration(
                        env = EnvironmentEnum.DEV,
                        verbose = true
                    )
            }
        }
    }
}
