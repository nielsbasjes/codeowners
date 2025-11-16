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

// On Ubuntu 24,04 do this first:
// sudo snap install kotlin --classic

@file:DependsOn("nl.basjes.codeowners:codeowners-validator:1.13.0")

import nl.basjes.codeowners.validator.CodeOwnersValidator
import nl.basjes.codeowners.validator.CodeOwnersValidator.DirectoryOwners
import kotlin.io.path.Path
import kotlin.system.exitProcess

var directoryOwners: DirectoryOwners
try {
    val validator = CodeOwnersValidator(null, null, false)
    directoryOwners = validator.analyzeDirectory(Path("").toAbsolutePath().toFile(), null)
} catch (ex: Exception) {
    println(ex.message)
    exitProcess(1)
}

var pass = true

if (!directoryOwners.allExistingFilesHaveMandatoryCodeOwner()) {
    println("- Not all existing files have a code-owner. ❌")
    pass = false
}

if (!directoryOwners.allNewlyCreatedFilesHaveMandatoryCodeOwner()) {
    println("- Not all possibly newly created files have a code-owner. ❌")
    pass = false
}

if (!pass) {
    print ("The failed checks:\n" + directoryOwners.toTable())
    exitProcess(1)
}
