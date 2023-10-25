package com.github.afzalive.createchangelogintellijplugin.actions

import com.intellij.execution.runToolbar.environment
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.LocalFilePath
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.application
import git4idea.GitUtil
import git4idea.history.GitCommitRequirements
import git4idea.history.GitHistoryUtils
import git4idea.history.GitLogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

class CreateChangelogAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        try {
            val project = event.project ?: return
            runAction(project)
        } catch (e: Exception) {
            print(e.stackTraceToString())
            throw e;
        }
    }

    private fun runAction(project: Project) {
        val baseProjectPath = requireNotNull(project.basePath)
        print(baseProjectPath)
        val isVcs = GitUtil.isGitRoot(baseProjectPath)
        if (!isVcs) {
            print("File not under VCS")
            return
        }

        val file = runBlocking {
            withContext(Dispatchers.IO) {
                val path = LocalFilePath(baseProjectPath, true)
                val repo = GitUtil.getRepositoryForFile(project, path)
                val currentBranch = requireNotNull(repo.currentBranch)
                val filename = currentBranch.name
                val lastCommitMessage = getLastCommitMessage(project, LocalFilePath(baseProjectPath, true))
                createChangelogFile(baseProjectPath, filename, lastCommitMessage)
            }
        }

        val editor = FileEditorManager.getInstance(project)
        val virtualFile = requireNotNull(file.toVirtualFile(true))
        editor.openFile(virtualFile, true, true)
    }

    @Suppress("UnstableApiUsage")
    private fun getLastCommitMessage(project: Project, filePath: FilePath): String {
        val commitHash = GitHistoryUtils.getLastRevision(project, filePath)?.number.toString()
        var lastCommitMessage = ""
        GitLogUtil.readFullDetailsForHashes(
            project,
            filePath.virtualFile!!,
            listOf(commitHash),
            GitCommitRequirements.DEFAULT
        ) {
            lastCommitMessage = it.fullMessage
        }

        return lastCommitMessage
    }

    private fun createChangelogFile(
        baseProjectPath: String,
        filename: String,
        lastCommitMessage: String,
    ): File {
        val sanitizedFilename = filename.replace("/", ".") + ".txt"
        print("Creating changelog file: $filename")
        val changelogFolder = File("$baseProjectPath/changelog")
        if (!changelogFolder.exists()) {
            changelogFolder.mkdirs()
        }
        print("In folder: $changelogFolder")
        val file = File(changelogFolder, sanitizedFilename)
        val created = file.createNewFile()
        print("File created: $created")
        if (created) {
            file.setWritable(true)
            file.writeText("\uD83D\uDC1B\uD83D\uDD76⭐⛳️ $lastCommitMessage \n")
        }
        return file
    }
}

private fun File.toVirtualFile(refresh: Boolean = false): VirtualFile? {
    return VfsUtil.findFileByIoFile(this, refresh)
}
