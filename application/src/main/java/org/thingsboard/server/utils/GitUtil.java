package org.thingsboard.server.utils;

import com.google.common.collect.Streams;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class GitUtil {
    private final Git git;

    private UsernamePasswordCredentialsProvider credentialsProvider;

    public static void main(String[] args) throws Exception {
        String workingDirectory = "/home/viacheslav/Desktop/thingsboard-vcs";
        String branch = "main";
        GitUtil gitUtil = new GitUtil("https://github.com/ViacheslavKlimov/thingsboard-vcs.git", branch, workingDirectory,
                "ViacheslavKlimov", "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");
        gitUtil.checkout(branch);

    }

    public GitUtil(String repositoryUri, String defaultBranch, String workingDirectory, String username, String password) throws GitAPIException, IOException {
        File repositoryDirectory = new File(workingDirectory);
        this.credentialsProvider = new UsernamePasswordCredentialsProvider(username, password);
        if (repositoryDirectory.exists()) {
            this.git = Git.open(repositoryDirectory);

            try {
                checkout(defaultBranch);
                pull();
            } catch (RefNotAdvertisedException e) {
                // no remote branch. need to check if there are any commit in the current branch. if none - create branch after commit, else - create branch
                // todo: how to create branch when no commits?
                log.info("No remote branch {} yet", defaultBranch);
            } catch (RefNotFoundException e) {
                // no commits ?
                git.checkout().setName(defaultBranch).setCreateBranch(true).setOrphan(true).call();
                log.warn("No commits");
            }
        } else {
            this.git = cloneRepository(repositoryUri, repositoryDirectory);
            checkout(defaultBranch);
        }
    }


    public void checkout(String branch) throws GitAPIException {
        try {
            git.branchCreate().setName(branch).call();
        } catch (RefAlreadyExistsException ignored) {}
        git.checkout().setName(branch).call();
        log.debug("Checked out branch '{}'", branch);
    }

    public InputStream getFileAtRevision(String commit, String file) throws IOException {
        try (TreeWalk treeWalk = TreeWalk.forPath(git.getRepository(), file, git.getRepository().resolve(commit))) {
            ObjectId blobId = treeWalk.getObjectId(0);
            try (ObjectReader objectReader = git.getRepository().newObjectReader()) {
                ObjectLoader objectLoader = objectReader.open(blobId);
                return objectLoader.openStream();
            }
        }
    }

    public void commit(String message) throws GitAPIException {
        git.add().addFilepattern(".").call();
        git.commit().setMessage(message).call();
        log.debug("Committed '{}'", message);
    }

    public void push() throws GitAPIException {
        prepareCommand(git.push()).call();
        log.debug("Pushed to remote repository");
    }

    public void fetch(String branch) throws GitAPIException, IOException {
        prepareCommand(git.fetch().setInitialBranch(branch)).call();
    }

    public void pull() throws GitAPIException {
        try {
            prepareCommand(git.pull().setRemote("origin")).call();
            log.debug("Pulled from remote branch");
        } catch (RefNotAdvertisedException ignored) {}
    }

    public String getCurrentBranch() throws IOException {
        return git.getRepository().getBranch();
    }

    public List<String> listCommits(String branch, String path) throws GitAPIException, IOException {
        return Streams.stream(git.log().add(git.getRepository().resolve(branch)).addPath(path).call())
                .map(RevCommit::getFullMessage)
                .collect(Collectors.toList());
    }

    private Git cloneRepository(String repositoryUri, File repositoryDirectory) throws GitAPIException {
        return prepareCommand(Git.cloneRepository().setURI(repositoryUri).setDirectory(repositoryDirectory)).call();
    }

    private <C extends GitCommand<T>, T> TransportCommand<C, T> prepareCommand(TransportCommand<C, T> transportCommand) {
        if (credentialsProvider != null) {
            transportCommand.setCredentialsProvider(credentialsProvider);
        } else {
            SshSessionFactory sshSessionFactory = SshSessionFactory.getInstance(); // fixme: test
            transportCommand.setTransportConfigCallback(transport -> {
                ((SshTransport) transport).setSshSessionFactory(sshSessionFactory);
            });
        }
        return transportCommand;
    }

}
