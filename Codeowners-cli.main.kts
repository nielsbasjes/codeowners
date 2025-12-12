#!/usr/bin/env -S kotlin -howtorun .main.kts

//
//  CodeOwners Tools
//  Copyright (C) 2023-2025 Niels Basjes
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  https://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an AS IS BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

// On Ubuntu 24.04 do this first:
// sudo snap install kotlin --classic

@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:5.0.3")
@file:DependsOn("nl.basjes.codeowners:codeowners-validator:1.14.1")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import nl.basjes.codeowners.validator.CodeOwnersValidator
import nl.basjes.codeowners.validator.CodeOwnersValidator.DirectoryOwners
import nl.basjes.codeowners.validator.gitlab.GitlabConfiguration
import nl.basjes.codeowners.validator.gitlab.GitlabConfiguration.*
import java.io.File
import kotlin.io.path.Path
import kotlin.system.exitProcess

class CodeOwnersChecks : CliktCommand("codeowners") {
    val baseDir:                               File? by option().file(mustExist = true, canBeFile = false, canBeDir = true, canBeSymlink = false, mustBeReadable = true)
    val codeOwnersFile:                        File? by option().file(mustExist = true, canBeFile = true, canBeDir = false, canBeSymlink = false, mustBeReadable = true)
    val allFilesMustHaveCodeOwner:             Boolean by option("--allFilesMustHaveCodeOwner", "-af", help= "Fail if a non ignored file (ether existing or newly created) does not have a mandatory approver").flag( default = false)
    val allExisingFilesMustHaveCodeOwner:      Boolean by option("--allExisingFilesMustHaveCodeOwner", "-aef", help= "Fail if an existing non ignored file does not have a mandatory approver").flag(default = false)
    val allNewlyCreatedFilesMustHaveCodeOwner: Boolean by option("--allNewlyCreatedFilesMustHaveCodeOwner", "-anf", help= "Fail if a newly created non ignored file does not have a mandatory approver").flag(default = false)
    val verbose:                               Boolean by option("--verbose", "-v", help = "Output detailed information about the internal matching process").flag(default = false)
    val showApprovers:                         Boolean by option("--showApprovers", help = "Output the result (either as a table or as json)").flag(default = false)
    val showApproversAsJson:                   Boolean by option("--json", help = "Output the result as json or as a table (table is the default)").flag("--table", default = false)

    val gitlabServerUrl:                       String? by option("--gitlabServerUrl", help="The URL of the Gitlab Server (using the value in the CI_SERVER_URL environment variable if not specified)")
    val gitlabServerUrlEnvironmentVariable:    String? by option("--gitlabServerUrlEnvironmentVariable", help="The name of the environment variable that contains the URL of the Gitlab Server (using CI_SERVER_URL if not specified)")
    val gitlabProjectId:                       String? by option("--gitlabProjectId", help="The id of the Gitlab project (using the value in the CI_PROJECT_ID environment variable if not specified)")
    val gitlabProjectIdEnvironmentVariable:    String? by option("--gitlabProjectIdEnvironmentVariable", help="The name of the environment variable that contains the id of the Gitlab project (using CI_PROJECT_ID if not specified)")
    val gitlabAccessTokenEnvironmentVariable:  String? by option("--gitlabAccessTokenEnvironmentVariable", help="The name of the environment variable that contains the access token of the Gitlab project")

    override fun run() {
        val gitlab =
            if (gitlabAccessTokenEnvironmentVariable == null)
                null // No token no checks
            else
                GitlabConfiguration(
                    ServerUrl(gitlabServerUrl, gitlabServerUrlEnvironmentVariable),
                    ProjectId(gitlabProjectId, gitlabProjectIdEnvironmentVariable),
                    AccessToken(gitlabAccessTokenEnvironmentVariable),
                )

        var directoryOwners: DirectoryOwners
        var pass = true
        try {
            val validator = CodeOwnersValidator(gitlab, null, verbose)

            directoryOwners = validator.analyzeDirectory(
                baseDir ?: Path("").toAbsolutePath().toFile(), // If no basedir specified use the current directory
                codeOwnersFile,
            )
        } catch (ex: Exception) {
            println(ex.message)
            exitProcess(1)
        }

        if (allFilesMustHaveCodeOwner || allExisingFilesMustHaveCodeOwner) {
            if (!directoryOwners.allExistingFilesHaveMandatoryCodeOwner()) {
                println("- Not all existing files have a code-owner. ❌")
                pass = false
            }
        }

        if (allFilesMustHaveCodeOwner || allNewlyCreatedFilesMustHaveCodeOwner) {
            if (!directoryOwners.allNewlyCreatedFilesHaveMandatoryCodeOwner()) {
                println("- Not all possibly newly created files have a code-owner. ❌")
                pass = false
            }
        }

        if (!pass) {
            print ("The failed checks:\n" + directoryOwners.toTable())
            exitProcess(1)
        }

        if (showApprovers) {
            if (showApproversAsJson) {
                print(directoryOwners.toJson())
            } else {
                print("Approvers:\n${directoryOwners.toTable()}")
            }
        }
    }
}

CodeOwnersChecks().main(args)
