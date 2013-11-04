package org.jenkinsci.plugins.gerritreviewbuddy;

// Copyright 2013 Switch-Gears ApS

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.*;
import com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritCause;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.DepthWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@SuppressWarnings("UnusedDeclaration")
public class GerritReviewBuddy extends Builder {

    @DataBoundConstructor
    public GerritReviewBuddy() {
    }

    @Override
    public boolean prebuild(Build build, BuildListener listener) {
        boolean isGerritCause = Iterables.any(build.getCauses(), Predicates.instanceOf(GerritCause.class));
        if (!isGerritCause) {
            listener.getLogger().println("[Gerrit Review Buddy] The build was not triggered by Gerrit!");
            return true;
        }

        // Initialise the Git repo
        Repository fileRepository;
        try {
            fileRepository = new FileRepositoryBuilder()
                    .setGitDir(
                            new File(
                                    build.getWorkspace().getRemote(),
                                    ".git")
                    )
                    .readEnvironment()
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }

        // Check the number of parents
        RevCommit head;
        try {
            ObjectId headObjectId = fileRepository.resolve(Constants.HEAD);
            RevWalk revWalk = new RevWalk(fileRepository);
            head = revWalk.parseCommit(headObjectId);
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }
        listener.getLogger().println("[Gerrit Review Buddy] head: " + head.getId().toString());
        if (head.getParentCount() == 0) {
            listener.getLogger().println("[Gerrit Review Buddy] No parents detected.");
            return true;
        }

        if (head.getParentCount() > 1) {
            listener.getLogger().println("[Gerrit Review Buddy] Merge commit detected.");
            return true;
        }
        RevCommit parent;
        try {
            ObjectId parentObjectId = fileRepository.resolve("HEAD^");
            RevWalk revWalk = new RevWalk(fileRepository);
            parent = revWalk.parseCommit(parentObjectId);
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }
        listener.getLogger().println("[Gerrit Review Buddy] parent: " + parent.getId().toString());

        // Find the changed files
        Set<String> addedFiles = Sets.newHashSet();
        Set<String> nonAddedFiles = Sets.newHashSet();
        List<DiffEntry> diffEntries;
        int changedLinesCount = 0;
        try {
            ObjectId headTree = head.getTree();
            ObjectId parentTree = parent.getTree();
            CanonicalTreeParser headTreeParser = getTreeParser(fileRepository, headTree);
            CanonicalTreeParser parentTreeParser = getTreeParser(fileRepository, parentTree);
            Git git = new Git(fileRepository);
            diffEntries = git.diff().setOldTree(parentTreeParser).setNewTree(headTreeParser).call();

            for (DiffEntry diffEntry : diffEntries) {
                if (diffEntry.getChangeType() == DiffEntry.ChangeType.ADD) {
                    addedFiles.add(diffEntry.getNewPath());
                } else {
                    nonAddedFiles.add(diffEntry.getNewPath());
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DiffFormatter diffFormatter = new DiffFormatter(out);
                diffFormatter.setRepository(fileRepository);
                FileHeader fileHeader = diffFormatter.toFileHeader(diffEntry);
                EditList editList = fileHeader.toEditList();
                for (Edit edit : editList) {
                    switch (edit.getType()) {
                        case DELETE:
                            changedLinesCount += edit.getLengthA();
                            break;
                        case INSERT:
                            changedLinesCount += edit.getLengthB();
                            break;
                        case REPLACE:
                            changedLinesCount += edit.getLengthB();
                            break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        } catch (GitAPIException e) {
            e.printStackTrace();
            return true;
        }

        // Suggest a review time
        int totalReviewTime = Math.max(
                roundUpDivision(changedLinesCount, 5),
                5);
        int roundedReviewTime = roundUpToMultipleOfFive(totalReviewTime);
        int reviewSessionsCount = roundUpDivision(roundedReviewTime, 60);
        int reviewSessionsLength = roundUpToMultipleOfFive(
                roundUpDivision(roundedReviewTime, reviewSessionsCount));
        listener.getLogger().println("[Gerrit Review Buddy] review time: " + reviewSessionsCount + " session(s) of " + reviewSessionsLength + " minutes");

        // Suggest number of reviewers
        int reviewersCount = reviewSessionsCount + 1;
        listener.getLogger().println("[Gerrit Review Buddy] minimum number of reviewers: " + reviewersCount);

        // Find potential reviewers
        final Multiset<String> potentialReviewers = HashMultiset.create();
        DepthWalk.RevWalk revWalk = new DepthWalk.RevWalk(fileRepository, 100);
        try {
            revWalk.markRoot(revWalk.parseCommit(head));
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }
        Multimap<String, RevCommit> result = HashMultimap.create();
        try {
            int commitCount = 4;

            RevCommit revCommit;
            while ((revCommit = revWalk.next()) != null) {
                DepthWalk.Commit commit = (DepthWalk.Commit) revCommit;
                listener.getLogger().println(commit.getId().toString());
                if (revCommit.getParentCount() > 1) {
                    listener.getLogger().println("Skipping merge commit");
                }
                listener.getLogger().println("[Gerrit Review Buddy] Traversing commit: " + revCommit.getId().toString() + " by " + revCommit.getAuthorIdent().getEmailAddress());

                if (revCommit.getAuthorIdent().getEmailAddress().startsWith("jenkins")) {
                    listener.getLogger().println("[Gerrit Review Buddy] + Skipping because the author is blacklisted");
                    continue;
                }

                boolean quickReturn = true;
                for (String file : nonAddedFiles) {
                    if (result.get(file).size() != commitCount) {
                        quickReturn = false;
                    }
                }

                if (quickReturn) {
                    listener.getLogger().println("[Gerrit Review Buddy] Stopping the traversal of the commit tree");
                    break;
                }

                RevWalk walk = new RevWalk(fileRepository);
                if (commit.getParentCount() == 0) {
                    walk.release();
                    break;
                }
                RevCommit commitParent = walk.parseCommit(commit.getParent(0));

                ObjectId headTree = commit.getTree();
                ObjectId parentTree = commitParent.getTree();
                CanonicalTreeParser headTreeParser = getTreeParser(fileRepository, headTree);
                CanonicalTreeParser parentTreeParser = getTreeParser(fileRepository, parentTree);
                Git git = new Git(fileRepository);
                List<DiffEntry> diffs = git.diff().setOldTree(parentTreeParser).setNewTree(headTreeParser).call();
                walk.release();


                for (DiffEntry diffEntry : diffs) {
                    if (nonAddedFiles.contains(diffEntry.getNewPath()) && result.get(diffEntry.getNewPath()).size() < commitCount) {
                        listener.getLogger().println("[Gerrit Review Buddy] + + File " + diffEntry.getNewPath() + " edited");
                        result.put(diffEntry.getNewPath(), revCommit);
                    }
                }

            }
            revWalk.release();
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        } catch (GitAPIException e) {
            e.printStackTrace();
            return true;
        }

        for (String path : result.keySet()) {
            for (RevCommit revCommit : result.get(path)) {
                potentialReviewers.add(revCommit.getAuthorIdent().getEmailAddress());
            }
        }

        // Remove the author of HEAD
        int entriesByAuthor = potentialReviewers.count(head.getAuthorIdent().getEmailAddress());
        potentialReviewers.remove(head.getAuthorIdent().getEmailAddress(), entriesByAuthor);

        ImmutableMultiset<String> sortedReviewers = Multisets.copyHighestCountFirst(potentialReviewers);

        Action action = new ReviewBuddyAction(changedLinesCount, reviewSessionsCount, reviewSessionsLength, reviewersCount, sortedReviewers);
        build.addAction(action);
        try {
            build.save();
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }

        return true;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return true;
    }

    private CanonicalTreeParser getTreeParser(Repository fileRepository, ObjectId tree) throws IOException {
        CanonicalTreeParser treeParser = new CanonicalTreeParser();
        ObjectReader headObjectReader = fileRepository.newObjectReader();
        try {
            treeParser.reset(headObjectReader, tree);
        } finally {
            headObjectReader.release();
        }
        return treeParser;
    }

    private int roundUpDivision(int number, int divisor) {
        Preconditions.checkArgument(number > 0);
        Preconditions.checkArgument(divisor > 0);
        return (number + divisor - 1) / divisor;
    }

    private int roundUpToMultipleOfFive(int number) {
        return (number + 4) / 5 * 5;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Gerrit Review Buddy";
        }
    }
}

