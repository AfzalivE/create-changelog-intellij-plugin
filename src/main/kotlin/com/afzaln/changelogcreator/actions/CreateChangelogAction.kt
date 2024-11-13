package com.afzaln.changelogcreator.actions

import com.afzaln.changelogcreator.settings.ChangelogSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.LocalFilePath
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcs.log.VcsLogProvider
import com.intellij.vcs.log.graph.PermanentGraph
import com.intellij.vcs.log.impl.TimedVcsCommitImpl
import com.intellij.vcs.log.visible.filters.VcsLogFilterObject
import git4idea.GitUtil
import git4idea.GitVcs
import git4idea.history.GitHistoryUtils
import git4idea.log.GitLogProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Paths

class CreateChangelogAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        try {
            val project = event.project ?: return
            runAction(project)
        } catch (e: Exception) {
            print(e.stackTraceToString())
            throw e
        }
    }

    private fun runAction(project: Project) {
        val baseProjectPath = requireNotNull(project.basePath)
        print(baseProjectPath)
        val isVcs = GitUtil.isGitRoot(Paths.get(baseProjectPath))
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
                val lastCommitMessage =
                    getLastCommitMessage(project, LocalFilePath(baseProjectPath, true))
                createChangelogFile(baseProjectPath, filename, lastCommitMessage)
            }
        }

        val editor = FileEditorManager.getInstance(project)
        val virtualFile = requireNotNull(file.toVirtualFile(true))
        editor.openFile(virtualFile, true, true)
    }

    private fun getLastCommitMessage(project: Project, filePath: FilePath): String {
        val gitVcs = VcsLogProvider.LOG_PROVIDER_EP.getExtensions(project).firstOrNull { provider ->
            provider.supportedVcs == GitVcs.getKey()
        } as GitLogProvider? ?: return ""

        val currentBranchLastCommitMessage = getCurrentBranchLastCommitMessage(gitVcs, project, filePath)
        val lastCommitMessage = currentBranchLastCommitMessage.ifEmpty {
            getRepoLastCommitMessage(gitVcs, project, filePath)
        }

        return lastCommitMessage
    }

    private fun getRepoLastCommitMessage(
        gitVcs: GitLogProvider,
        project: Project,
        filePath: FilePath,
    ): String {
        val commitHash = GitHistoryUtils.getLastRevision(project, filePath)?.number.toString()
        return getCommitMessageFromHash(gitVcs, filePath, commitHash)
    }

    private fun getCurrentBranchLastCommitMessage(
        gitVcs: GitLogProvider,
        project: Project,
        filePath: FilePath,
    ): String {
        val repository = GitUtil.getRepositoryForFile(project, filePath)
        val branchName = repository.currentBranch?.name ?: return ""

        val matchingCommits = gitVcs.getCommitsMatchingFilter(
            filePath.virtualFile!!,
            VcsLogFilterObject.collection(VcsLogFilterObject.fromBranch(branchName)),
            // TODO(Afzal): Investigate best option.
            PermanentGraph.Options.Default,
            1
        )
        val commit = matchingCommits.firstOrNull() as TimedVcsCommitImpl? ?: return ""

        return getCommitMessageFromHash(gitVcs, filePath, commit.id.asString())
    }

    private fun getCommitMessageFromHash(
        gitVcs: GitLogProvider,
        filePath: FilePath,
        commitHash: String,
    ): String {
        var lastCommitMessage = ""
        gitVcs.readFullDetails(filePath.virtualFile!!, listOf(commitHash)) {
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
        val template = ChangelogSettings.getInstance().state.changelogTemplate
        if (created) {
            file.setWritable(true)
            file.writeText("$template $lastCommitMessage")
        }
        return file
    }
}

private fun File.toVirtualFile(refresh: Boolean = false): VirtualFile? {
    return VfsUtil.findFileByIoFile(this, refresh)
}
