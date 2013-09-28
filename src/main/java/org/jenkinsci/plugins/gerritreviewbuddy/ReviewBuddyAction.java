// Copyright 2013 Switch-Gears ApS

package org.jenkinsci.plugins.gerritreviewbuddy;

import com.google.common.collect.ImmutableMultiset;
import hudson.model.InvisibleAction;

public class ReviewBuddyAction extends InvisibleAction {
    private int changedLinesCount;
    private int reviewSessionsCount;
    private int reviewSessionsLength;
    private int reviewersCount;
    private ImmutableMultiset<String> potentialReviewers;

    public ReviewBuddyAction(int changedLinesCount, int reviewSessionsCount, int reviewSessionsLength,
                             int reviewersCount, ImmutableMultiset<String> potentialReviewers) {
        this.changedLinesCount = changedLinesCount;
        this.reviewSessionsCount = reviewSessionsCount;
        this.reviewSessionsLength = reviewSessionsLength;
        this.reviewersCount = reviewersCount;
        this.potentialReviewers = potentialReviewers;
    }

    public int getChangedLinesCount() {
        return changedLinesCount;
    }

    public int getReviewSessionsCount() {
        return reviewSessionsCount;
    }

    public int getReviewSessionsLength() {
        return reviewSessionsLength;
    }

    public int getReviewersCount() {
        return reviewersCount;
    }

    public ImmutableMultiset<String> getPotentialReviewers() {
        return potentialReviewers;
    }
}
