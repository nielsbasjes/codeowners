/*
 * CodeOwners Tools
 * Copyright (C) 2023-2025 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.basjes.codeowners.validator.gitlab

import java.util.regex.Pattern

class GitlabConfiguration @JvmOverloads constructor(
    val serverUrl:ServerUrl = ServerUrl(),
    val projectId:ProjectId = ProjectId(),
    val accessToken:AccessToken = AccessToken(),
    var showAllApprovers: Boolean = false,
    var assumeUncheckableEmailExistsAndCanApprove: Boolean = true,
    var failLevel: FailLevel = FailLevel.ERROR,
    val problemLevels: ProblemLevels = ProblemLevels(),
) {
    fun isAssumeUncheckableEmailExistsAndCanApprove() = assumeUncheckableEmailExistsAndCanApprove

    enum class FailLevel {
        NEVER,
        FATAL,
        ERROR,
        WARNING
    }

    enum class Level {
        INFO,
        WARNING,
        ERROR,
        FATAL
    }

    class ProblemLevels {
        var roleNoUsers: Level = Level.WARNING
        var userUnknownEmail: Level = Level.WARNING
        var userDisabled: Level = Level.WARNING
        var approverDoesNotExist: Level = Level.WARNING
        var userTooLowPermissions: Level = Level.WARNING
        var groupTooLowPermissions: Level = Level.WARNING
        var userNotProjectMember: Level = Level.ERROR
        var groupNotProjectMember: Level = Level.ERROR
        var noValidApprovers: Level = Level.ERROR

        override fun toString(): String {
            return "Configured ProblemLevels: \n" +
                    "- roleNoUsers            = " + roleNoUsers + "\n" +
                    "- approverDoesNotExist   = " + approverDoesNotExist + "\n" +
                    "- userUnknownEmail       = " + userUnknownEmail + "\n" +
                    "- userDisabled           = " + userDisabled + "\n" +
                    "- userTooLowPermissions  = " + userTooLowPermissions + "\n" +
                    "- groupTooLowPermissions = " + groupTooLowPermissions + "\n" +
                    "- userNotProjectMember   = " + userNotProjectMember + "\n" +
                    "- groupNotProjectMember  = " + groupNotProjectMember + "\n" +
                    "- noValidApprovers       = " + noValidApprovers + "\n"
        }
    }

    val isValid: Boolean
        get() = serverUrl.isValid() && projectId.isValid() && accessToken.isValid()

    val isDefaultCIConfigRunningOutsideCI: Boolean
        /**
         * Is this config assuming the default CI settings used for obtaining the serverUrl and projectId settings
         * AND is it missing all of them --> i.e. this is settings specific for running in CI and it is run now outside of CI.
         * @return If this is CI config running OUTSIDE of CI
         */
        get() {
            serverUrl.load()
            projectId.load()
            accessToken.load()
            return serverUrl.isDefaultCIConfig && !serverUrl.isValid() &&
                    projectId.isDefaultCIConfig && !projectId.isValid() && !accessToken.isValid()
        }

    override fun toString(): String {
        return "GitlabConfiguration: {\n" +
                "  " + serverUrl + "\n" +
                "  " + projectId + "\n" +
                "  " + accessToken + "\n" +
                "  assumeUncheckableEmailExistsAndCanApprove = " + assumeUncheckableEmailExistsAndCanApprove + "\n" +
                "  " + problemLevels + "\n" +
                '}'
    }

    abstract class EnvironmentValueLoader {
        abstract fun load()

        private var source: String? = null
        fun getSource(): String? = source
        private var value: String? = null
        private var loaded = false
        private var valid = false

        fun isValid(): Boolean {
            load()
            return valid
        }

        protected open fun checkValidity(value: String?): Boolean {
            return value != null && NON_SPACE_STRING.matcher(value).matches()
        }

        fun getValue(): String? {
            load()
            if (!valid) {
                return null
            }
            return value
        }

        fun toString(name: String): String {
            load()
            return "$name='$value' found via $source" + (if (isValid()) " is valid." else " is NOT valid.")
        }

        /**
         * Load the value
         *
         * @param directValue                    The directly configured value
         * @param propertyId                     A readable form of the property where that is configured.
         * @param environmentVariableName        The name of the configured environment variable
         * @param defaultEnvironmentVariableName A builtin default environment variable name
         */
        protected fun load(
            directValue: String?,
            propertyId: String?,
            environmentVariableName: String?,
            defaultEnvironmentVariableName: String?
        ) {
            if (loaded) {
                return
            }

            // Explicitly specified
            if (!directValue.isNullOrEmpty()) {
                value = directValue.trim { it <= ' ' }
                source = propertyId
                loaded = true
                valid = checkValidity(value!!)
                return
            }
            // Get from environment
            var usedEnvVariableName = defaultEnvironmentVariableName
            if (!environmentVariableName.isNullOrEmpty()) {
                usedEnvVariableName = environmentVariableName
            }
            if (usedEnvVariableName.isNullOrEmpty()) {
                value = null
                source = "invalid environment variable \"$usedEnvVariableName\""
                loaded = true
                valid = false
                return
            }

            value = System.getenv(usedEnvVariableName)
            value = value?.trim { it <= ' ' }
            source = "environment variable $usedEnvVariableName"
            loaded = true
            valid = checkValidity(value)
        }

        companion object {
            private val NON_SPACE_STRING: Pattern = Pattern.compile("^[a-zA-Z0-9:/\\\\+.%_-]+$")
        }
    }


    class ServerUrl(
        private val url: String? = null,
        private val environmentVariableName: String? = null,
        ) : EnvironmentValueLoader() {

        constructor() : this(null, null)

        override fun load() {
            load(url, "gitlab.serverUrl.url", environmentVariableName, CI_SERVER_VARIABLE)
        }

        override fun checkValidity(value: String?): Boolean {
            if (value == null || !super.checkValidity(value)) {
                return false
            }
            return BASEURL_REGEX.matcher(value).matches()
        }

        val isDefaultCIConfig: Boolean
            get() = (CI_SERVER_VARIABLE == environmentVariableName || environmentVariableName == null) && url == null

        override fun toString(): String {
            return toString("ServerUrl")
        }

        companion object {
            private const val CI_SERVER_VARIABLE = "CI_SERVER_URL"
            private val BASEURL_REGEX: Pattern =
                Pattern.compile("^https?://[a-zA-Z0-9](?:(?:[a-zA-Z0-9-]*|(?<!-)\\.(?![-.]))*[a-zA-Z0-9]+)?(?::[0-9]+)?")
        }
    }

    class ProjectId(
        private val id: String? = null,
        private val environmentVariableName: String? = null,
    ) : EnvironmentValueLoader() {

        constructor() : this(null, null)
        override fun load() {
            load(id, "gitlab.projectId.id", environmentVariableName, CI_PROJECT_VARIABLE)
        }

        val isDefaultCIConfig: Boolean
            get() = (CI_PROJECT_VARIABLE == environmentVariableName || environmentVariableName == null) && id == null

        override fun toString(): String {
            return toString("ProjectId")
        }

        companion object {
            private const val CI_PROJECT_VARIABLE = "CI_PROJECT_ID"
        }
    }

    class AccessToken(
        private val environmentVariableName: String? = null
    ) : EnvironmentValueLoader() {

        constructor() : this(null)

        override fun load() {
            load(null, "Not allowed", environmentVariableName, null)
        }

        override fun toString(): String {
            // We are NOT printing the entire token.
            // The Gitlab tokens I have seen are usually "gl pat-" followed by about 20 random characters.

            if (!isValid()) {
                return "AccessToken found via " + getSource() + " is NOT valid."
            }
            val token = getValue()
            var cleanedToken = "***"
            if (token!!.length > 10) {
                cleanedToken =
                    token.substring(0, 6) + "*****" + token.substring(token.length - 2)
            }
            return "AccessToken= '" + cleanedToken + "' found via " + getSource() + " is valid."
        }
    }
}
