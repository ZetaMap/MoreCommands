/**
 * This file is part of MoreCommands. The plugin that adds a bunch of commands to your server.
 * Copyright (c) 2025  ZetaMap
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package fr.zetamap.morecommands.modules.voting;

import arc.struct.ObjectMap;
import arc.util.Time;
import arc.util.Timer;

import fr.zetamap.morecommands.util.Timekeeper;


/** Reusable vote session class that notify about it's state. */
public abstract class VoteSession<P, O> {
  protected ObjectMap<P, VoteType> voted = new ObjectMap<>();
  protected int votes;
  protected Timer.Task task;
  protected float duration;
  protected Timekeeper cooldown;
  protected O objective;
  
  /**
   * @param duration vote session duration, in seconds. Must be greater than {@code 1} second.
   * @param cooldown waiting time between vote sessions, in seconds. Can be {@code 0} for no cooldown.
   */
  public VoteSession(float duration, float cooldown) {
    this.duration = Math.max(duration, 1);
    this.cooldown = cooldown <= 0 ? null : new Timekeeper(cooldown);
  }
  
  /** @return if a new session can be started now. */
  public boolean canStart() {
    return !started() && (cooldown == null || cooldown.exceeded());
  }
  
  /** @return if a new session can be started now by the specified {@code people} with the specified {@code objective}. */
  public boolean canStart(P people, O objective) {
    return true;
  }
  
  /** @return whether a session is in progress. */
  public boolean started() {
    return task != null && task.isScheduled();
  }
  
  /** 
   * Start a new vote session. 
   * @return {@code false} if a session is already running or a new one cannot start now, else {@code true}.
   */
  public boolean start(P by, O objective) {
    if (!canStart(by, objective) || !canStart()) return false;
    if (task != null) Timer.schedule(task, duration);
    else task = Timer.schedule(() -> {if (!canPass()) cancel();}, duration);
    this.objective = objective;
    
    // Vote before notify starting for a proper count
    // Also skips the vote verification as if a people can start a session, he can also vote for
    voted.put(by, VoteType.YES);
    votes += VoteType.YES.sign();
    sessionStarted(by);
    // Check if session can pass immediately
    if (canPass()) force();
    return true;
  }
  
  /** @return whether the current vote session can be stopped. */
  public boolean canStop() {
    return started();
  }
  
  /**
   * Called when a {@code people} tries to {@link #force(P)} finish or {@link #cancel(P)} the current session.
   * @return whether the current vote session can be stopped by this {@code people}
   */
  public boolean canStop(P people) {
    return true;
  }
  
  /** 
   * Stop the current vote session without status notification and without cooldown. <br>
   * {@link #cancel()} should be used instead.
   */
  public void stop() {
    stop(null, false, null, null);
  }

  /** Stops the session. */
  protected void stop(P by, boolean withCooldown, Runnable run, arc.func.Cons<P> runPeople) {
    if ((by != null && !canStop(by)) || !canStop()) return;
    if (task != null) task.cancel();
    if (by == null && run != null) run.run();
    else if (by != null && runPeople != null) runPeople.get(by);
    clear();
    objective = null;
    if (withCooldown && cooldown != null) cooldown.reset();
  }
  
  /** Remove all votes. */
  public void clear() {
    voted.clear();
    votes = 0;
  }
  
  public boolean canVote(P people) {
    return started() && voted(people) == null;
  }
  
  /** 
   * Vote "yes" for the current session. Do nothing if no session was started.
   * <p>
   * The session will be automatically passed if the requirements are met.
   * @return {@code false} if the {@code people} already voted else {@code true}.
   */
  public boolean yes(P people) {
    return vote(people, VoteType.YES);
  }
  
  /** 
   * Vote "no" for the current session. Do nothing if no session was started.
   * <p>
   * The session will be automatically passed if the requirements are met.
   * @return {@code false} if the {@code people} already voted else {@code true}.
   */
  public boolean no(P people) {
    return vote(people, VoteType.NO);
  }
  
  public boolean vote(P people, VoteType type) {
    return vote(people, type, false);
  }
  
  protected boolean vote(P people, VoteType type, boolean silent) {
    if (!canVote(people)) return false;
    voted.put(people, type);
    votes += type.sign();
    if (!silent) sessionVote(people, type);
    if (canPass()) force();
    return true;
  }
  
  /** Remove a {@code people}'s vote. */
  public boolean remove(P people) {
    VoteType vote = voted.remove(people);
    if (vote == null) return false;
    votes -= vote.sign();
    sessionVoteRemoved(people);
    return true;
  }
  
  /** Force finish the current vote session. Do nothing if no session was started. */
  public void force() {
    stop(null, true, this::sessionPassed, null);
  }
  
  /** Force finish the current vote session. Do nothing if no session was started. */
  public void force(P by) {
    stop(by, true, null, this::sessionForced);
  }
  
  /** Cancel the current vote session. Do nothing if no session was started. */
  public void cancel() {
    stop(null, true, this::sessionFailed, null);
  }
  
  /** Cancel the current vote session. Do nothing if no session was started. */
  public void cancel(P by) {
    stop(by, true, null, this::sessionCanceled);
  }
  
  /** @return what the {@code people} voted, or {@code null} if they didn't vote in the current session. */
  public VoteType voted(P people) {
    return voted.get(people);
  }
  
  /** @return the number of player who voted in this session. */
  public int total() {
    return voted.size;
  }
  
  /** @return the votes of the current session, not the number of player who voted. */
  public int votes() {
    return votes;
  }
  
  /** @return the duration of a session. */
  public float duration() {
    return duration;
  }
  
  /** @return the remaining time of the current session, in ms, or {@code 0} if no session was started.. */
  public long sessionRemaining() {
    return Math.max(0, task.getExecuteTimeMillis() - Time.millis());
  }
  
  /** @return the remaining time to wait before restarting a session, in ms, or {@code 0} if no cooldown was defined. */
  public long waitRemaining() {
    return cooldown == null ? 0 : cooldown.remaining();
  }
  
  /** @return whether the current session can pass. */
  public boolean canPass() {
    return votes >= required();
  }
  
  /** @return the required votes to pass a session. */
  public abstract int required();
  
  /** @return the objective of the current vote session or {@code null} if no session was started */
  public O objective() {
    return objective;
  }
  
  
  // Callbacks
  protected abstract void sessionStarted(P by);   
  protected abstract void sessionPassed();
  protected abstract void sessionForced(P by);
  protected abstract void sessionFailed();
  protected abstract void sessionCanceled(P by);
  protected abstract void sessionVote(P who, VoteType type);
  protected abstract void sessionVoteRemoved(P who);
  
  
  public static enum VoteType {
    YES, NO;
    
    public boolean yes() { return this == YES; }
    public boolean no() { return this == NO; }
    /** @return {@code 1} if {@link #YES} or {@code -1} if {@link #NO}. */
    public int sign() { return yes() ? 1 : -1; }
  }
}
