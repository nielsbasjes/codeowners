/*
 * CodeOwners Tools
 * Copyright (C) 2023-2026 Niels Basjes
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
        /** A predefined role (like @@owner) is used as codeowner but has no users. */
        var roleNoUsers: Level = Level.WARNING
        /** A codeowner specified as an email is an unknown email address. */
        var userUnknownEmail: Level = Level.WARNING
        /** The user account of codeowner is a disabled account */
        var userDisabled: Level = Level.WARNING
        /** Approver does not exist in Gitlab (not as user and not as group) */
        var approverDoesNotExist: Level = Level.WARNING
        /** User does not have sufficient permissions to approve */
        var userTooLowPermissions: Level = Level.WARNING
        /** Shared group does not have sufficient permissions to approve */
        var groupTooLowPermissions: Level = Level.WARNING
        /** User exists but is not a member of with this project */
        var userNotProjectMember: Level = Level.ERROR
        /** Group exists and is not shared with this project */
        var groupNotProjectMember: Level = Level.ERROR
        /** For a rule none of the available approvers are valid. This causes Gitlab to allow approval by ANYONE! */
        var noValidApprovers: Level = Level.ERROR

        override fun toString(): String {
            return """
                Configured ProblemLevels:
                - Role No Users             = $roleNoUsers
                - Approver Does Not Exist   = $approverDoesNotExist
                - User Unknown Email        = $userUnknownEmail
                - User Disabled             = $userDisabled
                - User Too Low Permissions  = $userTooLowPermissions
                - Group Too Low Permissions = $groupTooLowPermissions
                - User Not Project Member   = $userNotProjectMember
                - Group Not Project Member  = $groupNotProjectMember
                - No Valid Approvers        = $noValidApprovers
            """.trimIndent()

        }
    }

    val valid: Boolean
        get() = serverUrl.valid && projectId.valid && accessToken.valid
    fun isValid() = valid

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
            return serverUrl.isDefaultCIConfig && !serverUrl.valid &&
                   projectId.isDefaultCIConfig && !projectId.valid && !accessToken.valid
        }

    override fun toString(): String {
        return """
GitlabConfiguration: {
  $serverUrl
  $projectId
  $accessToken
  Assume Uncheckable Email Exists And Can Approve = $assumeUncheckableEmailExistsAndCanApprove
  FailLevel: $failLevel
${problemLevels.toString().prependIndent("  ")}
}
"""
    }

    abstract class EnvironmentValueLoader {
        abstract fun load()

        private var sourceOrErrorMessage: String? = null

        protected var internalValue: String? = null
        val value: String?
            get() {
                load()
                if (!valid) {
                    return null
                }
                return internalValue
            }

        private var loaded = false
        private var internalValid = false
        val valid: Boolean
            get() {
                load()
                return internalValid
            }
        fun isValid() = valid

        protected open fun checkValidity(value: String?): Boolean {
            return value != null && NON_SPACE_STRING.matcher(value).matches()
        }

        abstract val sanitizedValue: String

        fun toString(name: String): String {
            load()
            return if (value == null) {
                "$name could not be loaded: $sourceOrErrorMessage."
            } else {
                "$name='$sanitizedValue' $sourceOrErrorMessage."
            }
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
            loaded = true

            // Explicitly specified
            if (!directValue.isNullOrEmpty()) {
                internalValue = directValue.trim()
                internalValid = checkValidity(internalValue)
                sourceOrErrorMessage =
                    if (internalValid) {
                        "(via property \"$propertyId\")"
                    } else {
                        "the value found using property \"$propertyId\" is not valid"
                    }
                return
            }

            // Get from environment
            var usedEnvVariableName = defaultEnvironmentVariableName
            if (!environmentVariableName.isNullOrEmpty()) {
                usedEnvVariableName = environmentVariableName
            }

            if (usedEnvVariableName == null) {
                internalValue = null
                internalValid = false
                sourceOrErrorMessage = "no environment variable was specified"
                return
            }

            if (usedEnvVariableName.isBlank()) {
                internalValue = null
                internalValid = false
                sourceOrErrorMessage = "the environment variable name \"$usedEnvVariableName\" is blank"
                return
            }

            internalValue = System.getenv(usedEnvVariableName) ?.trim()
            internalValid = checkValidity(internalValue)
            if (internalValid) {
                sourceOrErrorMessage = "(via environment variable \"$usedEnvVariableName\")"
            } else {
                if (internalValue == null) {
                    sourceOrErrorMessage = "the environment variable \"$usedEnvVariableName\" does not exist"
                } else {
                    sourceOrErrorMessage = "the value from environment variable \"$usedEnvVariableName\" is NOT valid"
                }
            }
        }

        companion object {
            private val NON_SPACE_STRING: Pattern = Pattern.compile("^[a-zA-Z0-9:/\\\\+.%_-]+$")
        }
    }

    class ServerUrl @JvmOverloads constructor(
        private val url: String? = null,
        private val environmentVariableName: String? = null,
        ) : EnvironmentValueLoader() {

        override fun load() {
            load(url, "gitlab.serverUrl.url", environmentVariableName, CI_SERVER_VARIABLE)
        }

        override fun checkValidity(value: String?): Boolean {
            if (value == null || !super.checkValidity(value)) {
                return false
            }
            return BASEURL_REGEX.matcher(value).matches()
        }

        override val sanitizedValue: String
            get() = value ?: "<<<null>>>"

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

    class ProjectId @JvmOverloads constructor(
        private val id: String? = null,
        private val environmentVariableName: String? = null,
    ) : EnvironmentValueLoader() {

        override fun load() {
            load(id, "gitlab.projectId.id", environmentVariableName, CI_PROJECT_VARIABLE)
        }

        override val sanitizedValue: String
            get() = value ?: "<<<null>>>"

        val isDefaultCIConfig: Boolean
            get() = (CI_PROJECT_VARIABLE == environmentVariableName || environmentVariableName == null) && id == null

        override fun toString(): String {
            return toString("ProjectId")
        }

        companion object {
            private const val CI_PROJECT_VARIABLE = "CI_PROJECT_ID"
        }
    }

    class AccessToken @JvmOverloads constructor(
        private val environmentVariableName: String? = null
    ) : EnvironmentValueLoader() {

        override fun load() {
            load(null, "Not allowed", environmentVariableName, null)
        }

        override val sanitizedValue: String
            get() = value?.let {
                // We are NOT printing the entire token.
                // The Gitlab tokens I have seen are usually "gl pat-" followed by about 20 random characters.
                if (it.length > 10 ) {
                    it.substring(0, 6) + "*****" + it.substring(it.length - 2)
                } else {
                    "***"
                }
            } ?: "<<<null>>>"

        override fun toString(): String {
            return toString("AccessToken")
        }

    }
}
