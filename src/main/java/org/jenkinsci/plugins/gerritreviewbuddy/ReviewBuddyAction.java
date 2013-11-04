/*
The MIT License (MIT)

Copyright (c) 2013 Switch-Gears ApS

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

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
