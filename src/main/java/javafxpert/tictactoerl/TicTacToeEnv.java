/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javafxpert.tictactoerl;

import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.environment.Environment;
import burlap.mdp.singleagent.environment.EnvironmentOutcome;
import burlap.mdp.singleagent.environment.extensions.EnvironmentObserver;
import burlap.mdp.singleagent.environment.extensions.EnvironmentServerInterface;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.XMLFormatter;

/**
 * @author James L. Weaver (Twitter: @JavaFXpert)
 */
public class TicTacToeEnv implements Environment, EnvironmentServerInterface {
  private static int WIN_REWARD = 10;
  private static int LOSE_REWARD = -10;
  private static int MOVE_REWARD = -1;

  /**
   * String representation of cells on the game board.
   * For example: "XOIIXOXIO"
   */
  private StringBuffer gameBoard;

  /**
   * Game status, specifically, whether the game is in-progress, or if X won,
   * or if O won, or if it is cat's game (nobody won).
   */
  private String gameStatus;

  /**
   * Indicates whether the the game is in the terminal state
   */
  private boolean terminated = false;

  /**
   * Reward given for the current action
   */
  private int reward = 0;
  
  /**
   * Policy for the agent embedded in the environment. <br>
   * 0 employs a strategy of playing the first empty cell with an "O"<br>
   *
   * 1 employs a strategy of playing a completely random empty cell with an "O"<br>
   *
   * 2 employ a strategy that randomly places "O" except when there are opportunities to block an "X" three-in-a row<br>
   *
   * 3 employs a strategy that randomly places "O" except when
   * there are opportunities to play third "O" in a row, or to block an "X" three-in-a row<br>
   *
   * 4 employs a strategy that prefers center and random corner placement,
   * except when there are opportunities to play a third "O" in a row, or block an "X" three-in-a row
   *
   */
  private int agentPolicy = 4;

  /**
   * Most recent state, to be returned by currentObservation() method
   */
  TicTacToeState currentObservationState;

  protected List<EnvironmentObserver> observers = new LinkedList<EnvironmentObserver>();

  /**
   * Mark that the player embedded in the environment plays
   */
  private char envPlayerMark = TicTacToeState.O_MARK;
  
  public char getEnvironmentPlayerMark() {
	  return envPlayerMark;
  }

  /**
   * Mark that the opposing player plays
   */
  private char opposingPlayerMark = TicTacToeState.X_MARK;
  
  public char getAgentPlayerMark() {
	  return opposingPlayerMark;
  }

  public TicTacToeEnv() {
    resetEnvironment();
  }
  
  /**
   * Sets the policy for the agent embedded in the environment. 
   * @param policy Policy for the agent. Specified as an int:<br>
   *
   *
   * 0 employs a strategy of playing the first empty cell with an "O"<br>
   *
   * 1 employs a strategy of playing a completely random empty cell with an "O"<br>
   *
   * 2 employ a strategy that randomly places "O" except when there are opportunities to block an "X" three-in-a row<br>
   *
   * 3 employs a strategy that randomly places "O" except when
   * there are opportunities to play third "O" in a row, or to block an "X" three-in-a row<br>
   *
   * 4 employs a strategy that prefers center and random corner placement,
   * except when there are opportunities to play a third "O" in a row, or block an "X" three-in-a row<br>
   *
   */
  public void setAgentPolicy(int policy) {
	  this.agentPolicy = policy;
  }
  
  public int getAgentPolicy() {
	  return this.agentPolicy;
  }
  
  public void setState(TicTacToeState state) {
	  gameBoard = new StringBuffer(state.gameBoard);
	  gameStatus = evalGameStatus();

	  terminated = 
			  gameStatus.equals(TicTacToeState.GAME_STATUS_O_WON) || 
			  gameStatus.equals(TicTacToeState.GAME_STATUS_X_WON) || 
			  gameStatus.equals(TicTacToeState.GAME_STATUS_CATS_GAME);
	  
	  currentObservationState = new TicTacToeState(gameBoard.toString(), gameStatus);
  }

  @Override
  public void resetEnvironment() {
    gameBoard = new StringBuffer(TicTacToeState.EMPTY_BOARD);
    if (envPlayerMark == TicTacToeState.X_MARK) {
      playRandomCell();
    }
    gameStatus = TicTacToeState.GAME_STATUS_IN_PROGRESS;

    currentObservationState = new TicTacToeState(gameBoard.toString(), gameStatus);

    terminated = false;
  }

  @Override
  public void addObservers(EnvironmentObserver... observers) {
    for(EnvironmentObserver o : observers){
      this.observers.add(o);
    }
  }

  @Override
  public void clearAllObservers() {
    this.observers.clear();
  }

  @Override
  public void removeObservers(EnvironmentObserver... observers) {
    for(EnvironmentObserver o : observers){
      this.observers.remove(o);
    }
  }

  @Override
  public List<EnvironmentObserver> observers() {
    return this.observers;
  }

  @Override
  public State currentObservation() {
    return currentObservationState;
  }

  @Override
  public EnvironmentOutcome executeAction(Action action) {
    MoveAction moveAction = (MoveAction)action;

    TicTacToeState priorState = new TicTacToeState(gameBoard.toString(), gameStatus);

    // actionId is the same as the cell number (0 - 8) of the move
    int cellNum = moveAction.getActionId();

    if (cellNum < 0 || cellNum >= TicTacToeState.NUM_CELLS ||
        (gameBoard.charAt(cellNum) != TicTacToeState.EMPTY)) {

      // Illegal move attempted so don't change
      System.out.println("Illegal move attempted to cell " + cellNum);
    }
    else {
      gameBoard.setCharAt(cellNum, opposingPlayerMark);
    }

    gameStatus = evalGameStatus();
    //if (gameStatus.equals(envPlayerMark)) {
    if (gameStatus.toCharArray()[0] == envPlayerMark) {
      reward = LOSE_REWARD;
      terminated = true;
    }
    else if (gameStatus.toCharArray()[0] == opposingPlayerMark) {

      // TODO: Consider removing this condition, as it doen't seem possible to encounter
      reward = WIN_REWARD;
      terminated = true;
    }
    else if (gameStatus.equals(TicTacToeState.GAME_STATUS_CATS_GAME)) {
      reward = MOVE_REWARD;
      terminated = true;
    }
    else {
      reward = MOVE_REWARD;
      terminated = false;

      switch(agentPolicy) {
      case 0:
    	  playFirstEmptyCell();
    	  break;
      case 1:
    	  playRandomCell();
    	  break;
      case 2:
    	  blockOrPlayRandom();
    	  break;
      case 3:
    	  winOrblockOrPlayRandom();
    	  break;
      case 4:
    	  winOrBlockOrCenterOrRandomCornerOrPlayRandom();
    	  break;
      default:
    	  winOrBlockOrCenterOrRandomCornerOrPlayRandom();
      }
      // Uncomment to employ a strategy of playing the first empty cell with an "O"
      //playFirstEmptyCell();

      // Uncomment to employ a strategy of playing a completely random empty cell with an "O"
      //playRandomCell();

      // Uncomment to employ a strategy that randomly places "O" except when there are opportunities to block an "X" three-in-a row
      //blockOrPlayRandom();

      // Uncomment to employ a strategy that randomly places "O" except when
      // there are opportunities to play third "O" in a row, or to block an "X" three-in-a row
      //winOrblockOrPlayRandom();

      // Uncomment to employ a strategy that prefers center and random corner placement,
      // except when there are opportunities to play a third "O" in a row, or block an "X" three-in-a row
      //winOrBlockOrCenterOrRandomCornerOrPlayRandom();

      gameStatus = evalGameStatus();  // Evaluate game status after opposing player has responded, and update terminated state
      if (gameStatus.toCharArray()[0] == envPlayerMark) {
        reward = LOSE_REWARD;
        terminated = true;
      }
      else if (gameStatus.toCharArray()[0] == opposingPlayerMark) {

        // TODO: Consider removing this condition, as it doen't seem possible to encounter
        reward = WIN_REWARD;
        terminated = true;
      }
      else if (gameStatus.equals(TicTacToeState.GAME_STATUS_CATS_GAME)) {
        // TODO: Consider removing this condition, as it doen't seem possible to encounter
        reward = MOVE_REWARD;
        terminated = true;
      }
    }

    TicTacToeState newState = new TicTacToeState(gameBoard.toString(), gameStatus);

    currentObservationState = newState.copy();

    EnvironmentOutcome environmentOutcome =
        new EnvironmentOutcome(priorState, action, newState, reward, terminated);

    return environmentOutcome;
  }

  @Override
  public double lastReward() {
    return reward;
  }

  @Override
  public boolean isInTerminalState() {
    return terminated;
  }
  
  public double rewardForStatus(String status) {
	  double rewardVal = 0;
	  if (status.toCharArray()[0] == envPlayerMark) {
		 rewardVal = LOSE_REWARD;
	 } else if (status.toCharArray()[0] == opposingPlayerMark) {
		 rewardVal = WIN_REWARD;
	 } else {
		 rewardVal = MOVE_REWARD;
	 }

	 return rewardVal;
  }
  
  public boolean isTerminalStatus(String status) {
	  return !status.equals(TicTacToeState.GAME_STATUS_IN_PROGRESS);
  }

  /**
   * Indicate to the environment which mark it will play as (X or O)
   * @param envPlayerMark
   */
  public void setEnvPlayerMark(char envPlayerMark) {
    this.envPlayerMark = envPlayerMark;
    if (envPlayerMark == TicTacToeState.X_MARK) {
      opposingPlayerMark = TicTacToeState.O_MARK;
    }
    else {
      opposingPlayerMark = TicTacToeState.X_MARK;
    }
    resetEnvironment();
  }

  /**
   * Evaluate the status of the game (in-progress, or who won)
   *
   * @return Indicator of in-progress, or who won
   */
  private String evalGameStatus() {
    // Start with the assumption that all cells are occupied but nobody won
    String gameStatus = TicTacToeState.GAME_STATUS_CATS_GAME;

    // Check if this game is still in progress
    for (int idx = 0; idx < TicTacToeState.NUM_CELLS; idx++) {
      if (gameBoard.charAt(idx) ==  TicTacToeState.EMPTY) {
        gameStatus = TicTacToeState.GAME_STATUS_IN_PROGRESS;
        break;
      }
    }

    // Check if X won
    if ((gameBoard.charAt(0) == TicTacToeState.X_MARK && gameBoard.charAt(1) == TicTacToeState.X_MARK && gameBoard.charAt(2) == TicTacToeState.X_MARK) ||
        (gameBoard.charAt(3) == TicTacToeState.X_MARK && gameBoard.charAt(4) == TicTacToeState.X_MARK && gameBoard.charAt(5) == TicTacToeState.X_MARK) ||
        (gameBoard.charAt(6) == TicTacToeState.X_MARK && gameBoard.charAt(7) == TicTacToeState.X_MARK && gameBoard.charAt(8) == TicTacToeState.X_MARK) ||
        (gameBoard.charAt(0) == TicTacToeState.X_MARK && gameBoard.charAt(3) == TicTacToeState.X_MARK && gameBoard.charAt(6) == TicTacToeState.X_MARK) ||
        (gameBoard.charAt(1) == TicTacToeState.X_MARK && gameBoard.charAt(4) == TicTacToeState.X_MARK && gameBoard.charAt(7) == TicTacToeState.X_MARK) ||
        (gameBoard.charAt(2) == TicTacToeState.X_MARK && gameBoard.charAt(5) == TicTacToeState.X_MARK && gameBoard.charAt(8) == TicTacToeState.X_MARK) ||
        (gameBoard.charAt(0) == TicTacToeState.X_MARK && gameBoard.charAt(4) == TicTacToeState.X_MARK && gameBoard.charAt(8) == TicTacToeState.X_MARK) ||
        (gameBoard.charAt(2) == TicTacToeState.X_MARK && gameBoard.charAt(4) == TicTacToeState.X_MARK && gameBoard.charAt(6) == TicTacToeState.X_MARK)) {
      gameStatus = TicTacToeState.GAME_STATUS_X_WON;
      //System.out.println("X won");
      //System.out.print(envPlayerMark == TicTacToeState.O_MARK ? "X" : "x");
    }
    else if ((gameBoard.charAt(0) == TicTacToeState.O_MARK && gameBoard.charAt(1) == TicTacToeState.O_MARK && gameBoard.charAt(2) == TicTacToeState.O_MARK) ||
        (gameBoard.charAt(3) == TicTacToeState.O_MARK && gameBoard.charAt(4) == TicTacToeState.O_MARK && gameBoard.charAt(5) == TicTacToeState.O_MARK) ||
        (gameBoard.charAt(6) == TicTacToeState.O_MARK && gameBoard.charAt(7) == TicTacToeState.O_MARK && gameBoard.charAt(8) == TicTacToeState.O_MARK) ||
        (gameBoard.charAt(0) == TicTacToeState.O_MARK && gameBoard.charAt(3) == TicTacToeState.O_MARK && gameBoard.charAt(6) == TicTacToeState.O_MARK) ||
        (gameBoard.charAt(1) == TicTacToeState.O_MARK && gameBoard.charAt(4) == TicTacToeState.O_MARK && gameBoard.charAt(7) == TicTacToeState.O_MARK) ||
        (gameBoard.charAt(2) == TicTacToeState.O_MARK && gameBoard.charAt(5) == TicTacToeState.O_MARK && gameBoard.charAt(8) == TicTacToeState.O_MARK) ||
        (gameBoard.charAt(0) == TicTacToeState.O_MARK && gameBoard.charAt(4) == TicTacToeState.O_MARK && gameBoard.charAt(8) == TicTacToeState.O_MARK) ||
        (gameBoard.charAt(2) == TicTacToeState.O_MARK && gameBoard.charAt(4) == TicTacToeState.O_MARK && gameBoard.charAt(6) == TicTacToeState.O_MARK)) {
      gameStatus = TicTacToeState.GAME_STATUS_O_WON;
      //System.out.println("O won");
      //System.out.print(envPlayerMark == TicTacToeState.X_MARK ? "O" : "o");
    }

    if (gameStatus.equals(TicTacToeState.GAME_STATUS_CATS_GAME)) {
      //System.out.println("Cat's game");
      //System.out.print(".");
    }
    return gameStatus;
  }

  /**
   * Simple strategy that plays the first empty cell
   */
  private void playFirstEmptyCell() {
    gameBoard.setCharAt(gameBoard.indexOf(Character.toString(TicTacToeState.EMPTY)), envPlayerMark);
  }

  /**
   * Simple strategy that plays a completely random empty cell
   */
  private void playRandomCell() {
	int[] emptyCells = getEmptyGameboardIndices(false);

	int selectedIndex = (int)(Math.random() * emptyCells.length);
	gameBoard.setCharAt(emptyCells[selectedIndex], envPlayerMark);
  }

  /**
   * Strategy that randomly places its mark except when there are opportunities to block a three-in-a row
   */
  private void blockOrPlayRandom() {
    int cellIndexToPlay = evalGameboardForBlock(opposingPlayerMark);
    if (cellIndexToPlay != -1) {
      gameBoard.setCharAt(cellIndexToPlay, envPlayerMark);
    }
    else {
      playRandomCell();
    }
  }

  /**
   * Strategy that randomly places it mark except when there are opportunities to
   * play third mark in a row, or to block a three-in-a row
   */
  private void winOrblockOrPlayRandom() {
    int cellIndexToPlay = evalGameboardForWin(envPlayerMark);
    if (cellIndexToPlay != -1) {
      gameBoard.setCharAt(cellIndexToPlay, envPlayerMark);
      return;
    }
    cellIndexToPlay = evalGameboardForBlock(opposingPlayerMark);
    if (cellIndexToPlay != -1) {
      gameBoard.setCharAt(cellIndexToPlay, envPlayerMark);
    }
    else {
      playRandomCell();
    }
  }

  /**
   * Strategy that prefers center or random corner placement, except when
   * there are opportunities to play a third mark in a row, or block a three-in-a-row
   * TODO: Refactor to remove repeating code
   */
  private void winOrBlockOrCenterOrRandomCornerOrPlayRandom() {
    int cellIndexToPlay = evalGameboardForWin(envPlayerMark);
    if (cellIndexToPlay != -1) {
      gameBoard.setCharAt(cellIndexToPlay, envPlayerMark);
      return;
    }
    cellIndexToPlay = evalGameboardForBlock(opposingPlayerMark);
    if (cellIndexToPlay != -1) {
      gameBoard.setCharAt(cellIndexToPlay, envPlayerMark);
    }
    else {
      playRandomCornerOrCenterOrRandomCell();
    }
  }

  /**
   * Play a random empty corner cell or center cell.
   * Note that a counter is used to attempt that number of random
   * placements, in case none of the corners or center cell is empty.
   */
  private void playRandomCornerOrCenterOrRandomCell() {

    int[] cornersAndCenter = getEmptyGameboardIndices(true);
    if(cornersAndCenter.length > 0) {
    	int chosenIndex = (int)(Math.random() * cornersAndCenter.length);
    	gameBoard.setCharAt(cornersAndCenter[chosenIndex], envPlayerMark);
    } else {
    	playRandomCell();
    }
  }
  
  /**
   * Gets all empty cells in the gameboard. 
   * @param onlyIncludeCenterAndCorners A boolean indicating to return all cells (false) or 
   * just the corner and center cells (indices 0, 2, 4, 6, 8). 
   * @return Returns an int array containing only valid, empty indices in the gameboard. 
   */
  private int[] getEmptyGameboardIndices(boolean onlyIncludeCenterAndCorners) {
	  List<Integer> indicies = new ArrayList<Integer>();
	  for(int i=0; i<TicTacToeState.NUM_CELLS; i++) {
		  if(gameBoard.charAt(i) == TicTacToeState.EMPTY) {
			  if (onlyIncludeCenterAndCorners) {
				  if (i % 2 == 0) {
					  indicies.add(i);
				  }
			  } else {
				  indicies.add(i);
			  }
		  }
	  }

	  int[] retVal = new int[indicies.size()];
	  for(int i=0; i<indicies.size(); i++) {
		  retVal[i] = indicies.get(i);
	  }
	  
	  return retVal;
  }

  /**
   * Evaluate the gameboard for an opportunity to block opposing player three-in-a row
   * TODO: Modify with a less brute-force, and less verbose, approach.  Possibly factor with evalGameboardForWin() method
   *
   * @return Zero-based index of cell that would block, or -1 if no cells apply
   */
  private int evalGameboardForBlock(char markToBlock) {
    int blockingPlay = -1;
    /**
     * XXI
     * ???
     * ???
     */
    if (gameBoard.charAt(0) == markToBlock && gameBoard.charAt(1) == markToBlock && gameBoard.charAt(2) == TicTacToeState.EMPTY) {
      blockingPlay = 2;
    }
    /**
     * XIX
     * ???
     * ???
     */
    else if (gameBoard.charAt(0) == markToBlock && gameBoard.charAt(1) == TicTacToeState.EMPTY && gameBoard.charAt(2) == markToBlock) {
      blockingPlay = 1;
    }
    /**
     * IXX
     * ???
     * ???
     */
    else if (gameBoard.charAt(0) == TicTacToeState.EMPTY && gameBoard.charAt(1) == markToBlock && gameBoard.charAt(2) == markToBlock) {
      blockingPlay = 0;
    }
    /**
     * ???
     * XXI
     * ???
     */
    else if (gameBoard.charAt(3) == markToBlock && gameBoard.charAt(4) == markToBlock && gameBoard.charAt(5) == TicTacToeState.EMPTY) {
      blockingPlay = 5;
    }
    /**
     * ???
     * XIX
     * ???
     */
    else if (gameBoard.charAt(3) == markToBlock && gameBoard.charAt(4) == TicTacToeState.EMPTY && gameBoard.charAt(5) == markToBlock) {
      blockingPlay = 4;
    }
    /**
     * ???
     * IXX
     * ???
     */
    else if (gameBoard.charAt(3) == TicTacToeState.EMPTY && gameBoard.charAt(4) == markToBlock && gameBoard.charAt(5) == markToBlock) {
      blockingPlay = 3;
    }
    /**
     * ???
     * ???
     * XXI
     */
    else if (gameBoard.charAt(6) == markToBlock && gameBoard.charAt(7) == markToBlock && gameBoard.charAt(8) == TicTacToeState.EMPTY) {
      blockingPlay = 8;
    }
    /**
     * ???
     * ???
     * XIX
     */
    else if (gameBoard.charAt(6) == markToBlock && gameBoard.charAt(7) == TicTacToeState.EMPTY && gameBoard.charAt(8) == markToBlock) {
      blockingPlay = 7;
    }
    /**
     * ???
     * ???
     * IXX
     */
    else if (gameBoard.charAt(6) == TicTacToeState.EMPTY && gameBoard.charAt(7) == markToBlock && gameBoard.charAt(8) == markToBlock) {
      blockingPlay = 6;
    }
    /**
     * X??
     * X??
     * I??
     */
    else if (gameBoard.charAt(0) == markToBlock && gameBoard.charAt(3) == markToBlock && gameBoard.charAt(6) == TicTacToeState.EMPTY) {
      blockingPlay = 6;
    }
    /**
     * X??
     * I??
     * X??
     */
    else if (gameBoard.charAt(0) == markToBlock && gameBoard.charAt(3) == TicTacToeState.EMPTY && gameBoard.charAt(6) == markToBlock) {
      blockingPlay = 3;
    }
    /**
     * I??
     * X??
     * X??
     */
    else if (gameBoard.charAt(0) == TicTacToeState.EMPTY && gameBoard.charAt(3) == markToBlock && gameBoard.charAt(6) == markToBlock) {
      blockingPlay = 0;
    }
    /**
     * ?X?
     * ?X?
     * ?I?
     */
    else if (gameBoard.charAt(1) == markToBlock && gameBoard.charAt(4) == markToBlock && gameBoard.charAt(7) == TicTacToeState.EMPTY) {
      blockingPlay = 7;
    }
    /**
     * ?X?
     * ?I?
     * ?X?
     */
    else if (gameBoard.charAt(1) == markToBlock && gameBoard.charAt(4) == TicTacToeState.EMPTY && gameBoard.charAt(7) == markToBlock) {
      blockingPlay = 4;
    }
    /**
     * ?I?
     * ?X?
     * ?X?
     */
    else if (gameBoard.charAt(1) == TicTacToeState.EMPTY && gameBoard.charAt(4) == markToBlock && gameBoard.charAt(7) == markToBlock) {
      blockingPlay = 1;
    }
    /**
     * ??X
     * ??X
     * ??I
     */
    else if (gameBoard.charAt(2) == markToBlock && gameBoard.charAt(5) == markToBlock && gameBoard.charAt(8) == TicTacToeState.EMPTY) {
      blockingPlay = 8;
    }
    /**
     * ??X
     * ??I
     * ??X
     */
    else if (gameBoard.charAt(2) == markToBlock && gameBoard.charAt(5) == TicTacToeState.EMPTY && gameBoard.charAt(8) == markToBlock) {
      blockingPlay = 5;
    }
    /**
     * ??I
     * ??X
     * ??X
     */
    else if (gameBoard.charAt(2) == TicTacToeState.EMPTY && gameBoard.charAt(5) == markToBlock && gameBoard.charAt(8) == markToBlock) {
      blockingPlay = 2;
    }
    /**
     * X??
     * ?X?
     * ??I
     */
    else if (gameBoard.charAt(0) == markToBlock && gameBoard.charAt(4) == markToBlock && gameBoard.charAt(8) == TicTacToeState.EMPTY) {
      blockingPlay = 8;
    }
    /**
     * X??
     * ?I?
     * ??X
     */
    else if (gameBoard.charAt(0) == markToBlock && gameBoard.charAt(4) == TicTacToeState.EMPTY && gameBoard.charAt(8) == markToBlock) {
      blockingPlay = 4;
    }
    /**
     * I??
     * ?X?
     * ??X
     */
    else if (gameBoard.charAt(0) == TicTacToeState.EMPTY && gameBoard.charAt(4) == markToBlock && gameBoard.charAt(8) == markToBlock) {
      blockingPlay = 0;
    }
    /**
     * ??X
     * ?X?
     * I??
     */
    else if (gameBoard.charAt(2) == markToBlock && gameBoard.charAt(4) == markToBlock && gameBoard.charAt(6) == TicTacToeState.EMPTY) {
      blockingPlay = 6;
    }
    /**
     * ??X
     * ?I?
     * X??
     */
    else if (gameBoard.charAt(2) == markToBlock && gameBoard.charAt(4) == TicTacToeState.EMPTY && gameBoard.charAt(6) == markToBlock) {
      blockingPlay = 4;
    }
    /**
     * ??I
     * ?X?
     * X??
     */
    else if (gameBoard.charAt(2) == TicTacToeState.EMPTY && gameBoard.charAt(4) == markToBlock && gameBoard.charAt(6) == markToBlock) {
      blockingPlay = 2;
    }
    return blockingPlay;
  }

  /**
   * Evaluate the gameboard for an opportunity to get three-in-a row
   * TODO: Modify with a less brute-force, and less verbose, approach.  Possibly factor with evalGameboardForBlock() method
   *
   * @return Zero-based index of cell that would win, or -1 if no cells apply
   */
  private int evalGameboardForWin(char playerMark) {
	return evalGameboardForBlock(playerMark);
  }
  
  public int indexForEnemyBlock() {
	  return this.evalGameboardForBlock(opposingPlayerMark);
  }
  
  public int indexForEnemyWin() {
	  return this.evalGameboardForWin(envPlayerMark);
  }
  
  public int[] emptyCellIndices() {
	  return this.getEmptyGameboardIndices(false);
  }
  
  public int[] emptyCornerAndCenterIndicies() {
	  return this.getEmptyGameboardIndices(true);
  }
  
  public String gameStatusForState(TicTacToeState state) {
	  TicTacToeState curState = currentObservationState.copy();
	  setState(state);
	  String status = gameStatus;
	  setState(curState);
	  
	  return status;
  }
}
