//  Copyright (C) 2011 Lucas Catabriga Rocha <catabriga90@gmail.com>
//    
//  This file is part of Graphwar.
//
//  Graphwar is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  Graphwar is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.

//  You should have received a copy of the GNU General Public License
//  along with Graphwar.  If not, see <http://www.gnu.org/licenses/>.
package Graphwar;

import GraphServer.Constants;

public class Function
{
	private String strFunc;
	private PolishNotationFunction polishFunc;
	private double offSet;
	
	private double fireAngle;
	private double[] valuesX;
	private double[] valuesY;
	private double[] valuesDY;
	private int numSteps;
	
	private int[] playersHit;
	private int[] soldiersHit;
	private int[] soldierHitPosition;
	private int numPlayersHit;
	
	private double lastX;
	private double lastY;
	
	private boolean hitObstacle;
	private double impactSpeed;
	
	// Reflection state. valuesX/valuesY hold the raw trajectory (what the function/ODE gives),
	// displayX/displayY hold the path actually shot, after every bounce has mirrored it.
	// display = A*raw + b, where A and b are built up from one mirror transform per bounce.
	private double[] displayX;
	private double[] displayY;
	private double a11, a12, a21, a22;
	private double b1, b2;
	private int numBounces;
	private boolean escapingObstacle;
	private int escapeSteps;
	
	// Vertical wrap-around range in game coordinates: y is wrapped into [-15, +15)
	private static final double PLANE_HEIGHT = 15.0;
	private static final double PLANE_LENGTH = 30.0;
	
	Function(String str) throws MalformedFunction
	{		
		strFunc = str;
		
		polishFunc = new PolishNotationFunction(strFunc);
		
		offSet = 0;
		
		fireAngle = 0;
		valuesX = null;
		valuesY = null;
		valuesDY = null;
		numSteps = 0;
		
		playersHit = null;
		soldiersHit = null;
		numPlayersHit = 0;
		
		lastX = 0;
		lastY = 0;
		
	}
	
	Function(PolishNotationFunction polishNotationFunction)
	{
		strFunc = "";
		
		polishFunc = polishNotationFunction;
		
		offSet = 0;
		
		fireAngle = 0;
		valuesX = null;
		valuesY = null;
		valuesDY = null;
		numSteps = 0;
		
		playersHit = null;
		soldiersHit = null;
		soldierHitPosition = null;
		numPlayersHit = 0;
		
		lastX = 0;
		lastY = 0;		
		
	}
	
	public int getNumPlayersHit()
	{
		return numPlayersHit;
	}
	
	public int getPlayerHit(int index)
	{
		return playersHit[index];
	}
	
	public int getSoldierHit(int index)
	{
		return soldiersHit[index];
	}
	
	public int getSoldierHitPosition(int index)
	{
		return soldierHitPosition[index];
	}
	
	public double getFireAngle()
	{
		return fireAngle;
	}	
	
	public double getX(int index)
	{
		return displayX[index];
	}
	
	public double getY(int index)
	{
		return displayY[index];
	}
	
	public int getNumBounces()
	{
		return numBounces;
	}
	
	public int getNumSteps()
	{
		return numSteps;
	}
	
	public String getStringFunc()
	{
		return strFunc;
	}
	
	private double getStartAngle(double x, double radius)
	{
		double angle = 0;
		
		double startAngleTangent = (polishFunc.evaluateFunction(x+Constants.STEP_SIZE,0,0) - polishFunc.evaluateFunction(x,0,0))/Constants.STEP_SIZE;
		angle = Math.atan(startAngleTangent);
	
		double finalX;
		double error = 10000;
		for(int i=0; error > Constants.ANGLE_ERROR && i<Constants.MAX_ANGLE_LOOPS; i++)
		{
			finalX = x + radius*Math.cos(angle);
			startAngleTangent = (polishFunc.evaluateFunction(finalX+Constants.STEP_SIZE,0,0) - polishFunc.evaluateFunction(finalX,0,0))/Constants.STEP_SIZE;
			
			double newAngle = Math.atan(startAngleTangent);
			
			error = Math.abs(newAngle - angle);
				
			//System.out.println(angle+" "+newAngle);
			//System.out.println(x+" "+finalX+" "+radius+"\n");
			
			angle = newAngle;
			
			
		}		
		
		return angle;
	}
	
	private boolean playerAlreadyHit(int player, int soldier)
	{
		for(int i=0; i<numPlayersHit; i++)
		{
			if(playersHit[i] == player && soldiersHit[i]==soldier)
				return true;
		}
		
		return false;
	}
	
	public void processFunctionRange(Obstacle obstacle, Player players[], int numPlayers, int currentTurn, boolean inverted)
	{		
		playersHit = new int[numPlayers*Constants.MAX_SOLDIERS_PER_PLAYER];
		soldiersHit = new int[numPlayers*Constants.MAX_SOLDIERS_PER_PLAYER];
		soldierHitPosition = new int[numPlayers*Constants.MAX_SOLDIERS_PER_PLAYER];
		numPlayersHit = 0;
		
		valuesX = new double[Constants.FUNC_MAX_STEPS];
		valuesY = new double[Constants.FUNC_MAX_STEPS];
		
		Soldier currentTurnSoldier = players[currentTurn].getCurrentTurnSoldier();
						
		valuesX[0] = currentTurnSoldier.getX();
		valuesY[0] = currentTurnSoldier.getY();
		
		if(inverted)
		{
			valuesX[0] = Constants.PLANE_LENGTH - valuesX[0];
		}
								
		valuesX[0] = (Constants.PLANE_GAME_LENGTH*(valuesX[0] - Constants.PLANE_LENGTH/2))/Constants.PLANE_LENGTH;
		valuesY[0] = (Constants.PLANE_GAME_LENGTH*(-valuesY[0] + Constants.PLANE_HEIGHT/2))/Constants.PLANE_LENGTH;
		
		double gameCoordinateRadius = ((double)(Constants.PLANE_GAME_LENGTH*Constants.SOLDIER_RADIUS))/Constants.PLANE_LENGTH;
				
		fireAngle = getStartAngle(valuesX[0], gameCoordinateRadius);
				
		if(Double.isNaN(fireAngle)==false && Double.isInfinite(fireAngle)==false )
		{
			valuesX[0] = valuesX[0] + gameCoordinateRadius*Math.cos(fireAngle);	
			valuesY[0] = valuesY[0] + gameCoordinateRadius*Math.sin(fireAngle);
		}
				
		offSet = -polishFunc.evaluateFunction(valuesX[0],0,0) + valuesY[0];
			
		double stepSize = Constants.STEP_SIZE;
		double tempStepSize = Constants.STEP_SIZE;
				
		resetReflection();
		applyTransform(0);

		numSteps = Constants.FUNC_MAX_STEPS;		
		for(int i=1; i<Constants.FUNC_MAX_STEPS; i++)
		{	
			tempStepSize = stepSize;
			
			valuesX[i] = valuesX[i-1] + tempStepSize;
			if (GameData.wrapEnabled) {
				valuesY[i] = polishFunc.evaluateFunction(valuesX[i],0,0)+offSet;
				valuesY[i] = (((valuesY[i] - (-PLANE_HEIGHT)) % PLANE_LENGTH) + PLANE_LENGTH) % PLANE_LENGTH + (-PLANE_HEIGHT);
			} else {
				valuesY[i] = polishFunc.evaluateFunction(valuesX[i],0,0)+offSet;
			}

			boolean endFunc = false;
			
//			for(int j=0; Math.pow(valuesX[i]-valuesX[i-1], 2)+Math.pow(valuesY[i]-valuesY[i-1], 2) > Constants.FUNC_MAX_STEP_DISTANCE_SQUARED;j++)
//			{
//				if(valuesX[i]-valuesX[i-1]>Constants.FUNC_MIN_X_STEP_DISTANCE)
//				{
//					tempStepSize = tempStepSize/2;
//
//					valuesX[i] = valuesX[i-1] + tempStepSize;
//					valuesY[i] = polishFunc.evaluateFunction(valuesX[i],0,0)+offSet;
//				}
			for(int j=0; Math.pow(valuesX[i]-valuesX[i-1], 2) + getWrappedDistYSquared(valuesY[i], valuesY[i-1]) > Constants.FUNC_MAX_STEP_DISTANCE_SQUARED;j++)
			{
				if(valuesX[i]-valuesX[i-1]>Constants.FUNC_MIN_X_STEP_DISTANCE)
				{
					tempStepSize = tempStepSize/2;

					valuesX[i] = valuesX[i-1] + tempStepSize;

					// DÖNGÜ İÇİNDEKİ HESAPLAMAYI DA SAR:

					if (GameData.wrapEnabled) {
						valuesY[i] = polishFunc.evaluateFunction(valuesX[i],0,0)+offSet;
						valuesY[i] = (((valuesY[i] - (-PLANE_HEIGHT)) % PLANE_LENGTH) + PLANE_LENGTH) % PLANE_LENGTH + (-PLANE_HEIGHT);
					} else {
						valuesY[i] = polishFunc.evaluateFunction(valuesX[i],0,0)+offSet;
					}
				}
				else
				{
					endFunc = true;
					break;
				}
			}
			
			if(endFunc)
			{
				numSteps = i;
				break;
			}
				
			applyTransform(i);
			
			double x = Constants.PLANE_LENGTH*displayX[i]/Constants.PLANE_GAME_LENGTH + Constants.PLANE_LENGTH/2;
			double y = -Constants.PLANE_LENGTH*displayY[i]/Constants.PLANE_GAME_LENGTH + Constants.PLANE_HEIGHT/2;
		
			
			if(inverted)
			{
				x = Constants.PLANE_LENGTH - x;
			}
			
			for(int j=0; j<numPlayers; j++)
			{
				for(int k=0; k<players[j].getNumSoldiers(); k++)
				{
					if(j==currentTurn)
					{
						if(k == players[j].getCurrentTurnSoldierIndex())
						{
							continue;
						}							
					}
					
					if(players[j].getSoldiers()[k].isAlive())
					{
										
						double distX = players[j].getSoldiers()[k].getX() - x;
						double distY = players[j].getSoldiers()[k].getY() - y;
						
						double distSquared = (Math.pow(distX, 2)+Math.pow(distY, 2));
											
						if(distSquared < Constants.SOLDIER_RADIUS*Constants.SOLDIER_RADIUS)
						{
							if(playerAlreadyHit(j,k)==false)
							{
								playersHit[numPlayersHit] = j;
								soldiersHit[numPlayersHit] = k;
								soldierHitPosition[numPlayersHit] = i;
								numPlayersHit++;
							}
						}
					}
				}
			}	
		
		
			if(checkObstacleCollision(obstacle, i, inverted))
			{
				numSteps = i;
				break;
			}
			
			if(Double.isNaN(y) || Double.isInfinite(y))
			{
				numSteps = i;
				break;
			}
				
		}
		
		lastX = Constants.PLANE_LENGTH*displayX[numSteps-1]/Constants.PLANE_GAME_LENGTH + Constants.PLANE_LENGTH/2;
		lastY = -Constants.PLANE_LENGTH*displayY[numSteps-1]/Constants.PLANE_GAME_LENGTH + Constants.PLANE_HEIGHT/2;
		processImpact(obstacle, inverted);
	
		//for(int i=0; i<valuesX.length; i++)
		//{			
			//valuesX[i] = 80*valuesX[i]+400;
			//valuesY[i] = 232-80*valuesY[i]+offSet;					
		//}
	}
	
	private double getRK4StartAngle(double x, double y, double radius)
	{
		double angle = 0;
		double startAngleTangent;		
		double finalX;
		double finalY;
		double error = 10000;
		for(int i=0; error > Constants.ANGLE_ERROR && i<Constants.MAX_ANGLE_LOOPS; i++)
		{
			finalX = x + radius*Math.cos(angle);
			finalY = y + radius*Math.sin(angle);
			
			double tempStepSize = Constants.STEP_SIZE;
			
			double k1 = polishFunc.evaluateFunction( finalX, finalY, 0);
			double k2 = polishFunc.evaluateFunction( finalX + 0.5*tempStepSize, finalY + 0.5*tempStepSize*k1, 0);
			double k3 = polishFunc.evaluateFunction( finalX + 0.5*tempStepSize, finalY + 0.5*tempStepSize*k2, 0);
			double k4 = polishFunc.evaluateFunction( finalX + tempStepSize, finalY + tempStepSize*k3, 0);
				
			double nextY = finalY + (tempStepSize/6)*(k1 + 2*k2 + 2*k3 + k4);
			double nextX = finalX + tempStepSize;
						
			startAngleTangent = (nextY-finalY)/(nextX-finalX);
			
			double newAngle = Math.atan(startAngleTangent);
			
			error = Math.abs(newAngle - angle);
				
			angle = newAngle;
		}		
		
		return angle;
	}
	
	public void processRK4Range(Obstacle obstacle, Player players[], int numPlayers, int currentTurn, boolean inverted)
	{	
		playersHit = new int[numPlayers*Constants.MAX_SOLDIERS_PER_PLAYER];
		soldiersHit = new int[numPlayers*Constants.MAX_SOLDIERS_PER_PLAYER];
		soldierHitPosition = new int[numPlayers*Constants.MAX_SOLDIERS_PER_PLAYER];
		numPlayersHit = 0;
		
		valuesX = new double[Constants.FUNC_MAX_STEPS];
		valuesY = new double[Constants.FUNC_MAX_STEPS];
		
		double stepSize = Constants.STEP_SIZE;
		
		Soldier currentTurnSoldier = players[currentTurn].getCurrentTurnSoldier();
		
		valuesX[0] = currentTurnSoldier.getX();
		valuesY[0] = currentTurnSoldier.getY();
		
		if(inverted)
		{
			valuesX[0] = Constants.PLANE_LENGTH - valuesX[0];
		}
								
		valuesX[0] = (Constants.PLANE_GAME_LENGTH*(valuesX[0] - Constants.PLANE_LENGTH/2))/Constants.PLANE_LENGTH;
		valuesY[0] = (Constants.PLANE_GAME_LENGTH*(-valuesY[0] + Constants.PLANE_HEIGHT/2))/Constants.PLANE_LENGTH;
		
		double gameCoordinateRadius = ((double)(Constants.PLANE_GAME_LENGTH*Constants.SOLDIER_RADIUS))/Constants.PLANE_LENGTH;
		
		fireAngle = getRK4StartAngle(valuesX[0], valuesY[0], gameCoordinateRadius);
				
		valuesX[0] = valuesX[0] + gameCoordinateRadius*Math.cos(fireAngle);	
		valuesY[0] = valuesY[0] + gameCoordinateRadius*Math.sin(fireAngle);		
		
		
		resetReflection();
		applyTransform(0);

		numSteps = Constants.FUNC_MAX_STEPS;
		for(int i=1; i<Constants.FUNC_MAX_STEPS; i++)
		{
			double tempStepSize = stepSize;
			
			double k1 = polishFunc.evaluateFunction( valuesX[i-1], valuesY[i-1], 0);
			double k2 = polishFunc.evaluateFunction( valuesX[i-1] + 0.5*tempStepSize, valuesY[i-1] + 0.5*tempStepSize*k1, 0);
			double k3 = polishFunc.evaluateFunction( valuesX[i-1] + 0.5*tempStepSize, valuesY[i-1] + 0.5*tempStepSize*k2, 0);
			double k4 = polishFunc.evaluateFunction( valuesX[i-1] + tempStepSize, valuesY[i-1] + tempStepSize*k3, 0);
				
			valuesY[i] = valuesY[i-1] + (tempStepSize/6)*(k1 + 2*k2 + 2*k3 + k4);
			valuesX[i] = valuesX[i-1] + tempStepSize;
			
			boolean endFunc = false;
			
			for(int j=0; Math.pow(valuesX[i]-valuesX[i-1], 2)+Math.pow(valuesY[i]-valuesY[i-1], 2) > Constants.FUNC_MAX_STEP_DISTANCE_SQUARED && valuesX[i]-valuesX[i-1]>Constants.FUNC_MIN_X_STEP_DISTANCE;j++)
			{
				if(valuesX[i]-valuesX[i-1]>Constants.FUNC_MIN_X_STEP_DISTANCE)
				{
					tempStepSize = tempStepSize/2;
					
					k1 = polishFunc.evaluateFunction( valuesX[i-1], valuesY[i-1], 0);
					k2 = polishFunc.evaluateFunction( valuesX[i-1] + 0.5*tempStepSize, valuesY[i-1] + 0.5*tempStepSize*k1, 0);
					k3 = polishFunc.evaluateFunction( valuesX[i-1] + 0.5*tempStepSize, valuesY[i-1] + 0.5*tempStepSize*k2, 0);
					k4 = polishFunc.evaluateFunction( valuesX[i-1] + tempStepSize, valuesY[i-1] + tempStepSize*k3, 0);
						
					valuesY[i] = valuesY[i-1] + (tempStepSize/6)*(k1 + 2*k2 + 2*k3 + k4);
					valuesX[i] = valuesX[i-1] + tempStepSize;	
				}
				else
				{
					endFunc = true;
					break;
				}
			}
			
			if(endFunc)
			{
				numSteps = i;
				break;
			}
			
			applyTransform(i);
			
			double x = Constants.PLANE_LENGTH*displayX[i]/Constants.PLANE_GAME_LENGTH + Constants.PLANE_LENGTH/2;
			double y = -Constants.PLANE_LENGTH*displayY[i]/Constants.PLANE_GAME_LENGTH + Constants.PLANE_HEIGHT/2;
		
			
			if(inverted)
			{
				x = Constants.PLANE_LENGTH - x;
			}
			
			for(int j=0; j<numPlayers; j++)
			{
				for(int k=0; k<players[j].getNumSoldiers(); k++)
				{
					if(j==currentTurn)
					{
						if(k == players[j].getCurrentTurnSoldierIndex())
						{
							continue;
						}							
					}
					
					if(players[j].getSoldiers()[k].isAlive())
					{
										
						double distX = players[j].getSoldiers()[k].getX() - x;
						double distY = players[j].getSoldiers()[k].getY() - y;
						
						double distSquared = (Math.pow(distX, 2)+Math.pow(distY, 2));
											
						if(distSquared < Constants.SOLDIER_RADIUS*Constants.SOLDIER_RADIUS)
						{
							if(playerAlreadyHit(j,k)==false)
							{
								playersHit[numPlayersHit] = j;
								soldiersHit[numPlayersHit] = k;
								soldierHitPosition[numPlayersHit] = i;
								numPlayersHit++;
							}
						}
					}
				}
			}			
			
			if(checkObstacleCollision(obstacle, i, inverted))
			{
				numSteps = i;
				break;
			}
			
			if(Double.isNaN(y) || Double.isInfinite(y))
			{
				numSteps = i;
				break;
			}
						
		}
			
		lastX = Constants.PLANE_LENGTH*displayX[numSteps-1]/Constants.PLANE_GAME_LENGTH + Constants.PLANE_LENGTH/2;;
		lastY = -Constants.PLANE_LENGTH*displayY[numSteps-1]/Constants.PLANE_GAME_LENGTH + Constants.PLANE_HEIGHT/2;
		processImpact(obstacle, inverted);
		
	}
	
	public void processRK42Range(Obstacle obstacle, Player players[], int numPlayers, int currentTurn, double angle ,boolean inverted)
	{	
		playersHit = new int[numPlayers*Constants.MAX_SOLDIERS_PER_PLAYER];
		soldiersHit = new int[numPlayers*Constants.MAX_SOLDIERS_PER_PLAYER];
		soldierHitPosition = new int[numPlayers*Constants.MAX_SOLDIERS_PER_PLAYER];
		numPlayersHit = 0;
		
		
		valuesX = new double[Constants.FUNC_MAX_STEPS];
		valuesY = new double[Constants.FUNC_MAX_STEPS];
		valuesDY = new double[Constants.FUNC_MAX_STEPS];
		
		double stepSize = Constants.STEP_SIZE;
		
		Soldier currentTurnSoldier = players[currentTurn].getCurrentTurnSoldier();
		
		valuesX[0] = currentTurnSoldier.getX();
				
		if(inverted)
		{
			valuesX[0] = Constants.PLANE_LENGTH - valuesX[0];
		}
		
		valuesX[0] = valuesX[0] + Constants.SOLDIER_RADIUS*Math.cos(angle);
		
		
		valuesY[0] = currentTurnSoldier.getY();
		valuesY[0] = valuesY[0] - Constants.SOLDIER_RADIUS*Math.sin(angle);
		
		
		valuesX[0] = (Constants.PLANE_GAME_LENGTH*(valuesX[0] - Constants.PLANE_LENGTH/2))/Constants.PLANE_LENGTH;
		valuesY[0] = (Constants.PLANE_GAME_LENGTH*(-valuesY[0] + Constants.PLANE_HEIGHT/2))/Constants.PLANE_LENGTH;
		valuesDY[0] = Math.tan(angle);
			
		fireAngle = angle;
		
		resetReflection();
		applyTransform(0);

		numSteps = Constants.FUNC_MAX_STEPS;
		for(int i=1; i<Constants.FUNC_MAX_STEPS; i++)
		{
			double tempStepSize = stepSize;
						
			
			double x1 = valuesX[i-1];
			double y1 = valuesY[i-1];
			double y2 = valuesDY[i-1];
			
			double k11 = y2;
			double k12 = polishFunc.evaluateFunction( x1, y1, y2);
			
			x1 = valuesX[i-1] + tempStepSize/2;
			y1 = valuesY[i-1] + (tempStepSize/2)*k11;
			y2 = valuesDY[i-1] + (tempStepSize/2)*k12;
			
			double k21 = y2;
			double k22 = polishFunc.evaluateFunction( x1, y1, y2);
			
			y1 = valuesY[i-1] + (tempStepSize/2)*k21;
			y2 = valuesDY[i-1] + (tempStepSize/2)*k22;
			
			double k31 = y2;
			double k32 = polishFunc.evaluateFunction( x1, y1, y2);
			
			x1 = valuesX[i-1] + tempStepSize;
			y1 = valuesY[i-1] + (tempStepSize)*k31;
			y2 = valuesDY[i-1] + (tempStepSize)*k32;
			
			double k41 = y2;
			double k42 = polishFunc.evaluateFunction( x1, y1, y2);
			
			
			valuesX[i] = valuesX[i-1] + tempStepSize;
			valuesY[i] = valuesY[i-1] + (tempStepSize/6)*(k11 + 2*k21 + 2*k31 + k41);
			valuesDY[i] = valuesDY[i-1] + (tempStepSize/6)*(k12 + 2*k22 + 2*k32 + k42);
			
			boolean endFunc = false;
			
			for(int j=0; Math.pow(valuesX[i]-valuesX[i-1], 2)+Math.pow(valuesY[i]-valuesY[i-1], 2) > Constants.FUNC_MAX_STEP_DISTANCE_SQUARED && valuesX[i]-valuesX[i-1]>Constants.FUNC_MIN_X_STEP_DISTANCE;j++)
			{
				if(valuesX[i]-valuesX[i-1]>Constants.FUNC_MIN_X_STEP_DISTANCE)
				{
			
					tempStepSize = tempStepSize/2;				
					
					x1 = valuesX[i-1];
					y1 = valuesY[i-1];
					y2 = valuesDY[i-1];
					
					k11 = y2;
					k12 = polishFunc.evaluateFunction( x1, y1, y2);
					
					x1 = valuesX[i-1] + tempStepSize/2;
					y1 = valuesY[i-1] + (tempStepSize/2)*k11;
					y2 = valuesDY[i-1] + (tempStepSize/2)*k12;
					
					k21 = y2;
					k22 = polishFunc.evaluateFunction( x1, y1, y2);
					
					y1 = valuesY[i-1] + (tempStepSize/2)*k21;
					y2 = valuesDY[i-1] + (tempStepSize/2)*k22;
					
					k31 = y2;
					k32 = polishFunc.evaluateFunction( x1, y1, y2);
					
					x1 = valuesX[i-1] + tempStepSize;
					y1 = valuesY[i-1] + (tempStepSize)*k31;
					y2 = valuesDY[i-1] + (tempStepSize)*k32;
					
					k41 = y2;
					k42 = polishFunc.evaluateFunction( x1, y1, y2);
					
					
					valuesX[i] = valuesX[i-1] + tempStepSize;
					valuesY[i] = valuesY[i-1] + (tempStepSize/6)*(k11 + 2*k21 + 2*k31 + k41);
					valuesDY[i] = valuesDY[i-1] + (tempStepSize/6)*(k12 + 2*k22 + 2*k32 + k42);
				}
				else
				{
					endFunc = true;
					break;
				}
				
			}
			
			if(endFunc)
			{
				numSteps = i;
				break;
			}
			
			
			applyTransform(i);
			
			double x = Constants.PLANE_LENGTH*displayX[i]/Constants.PLANE_GAME_LENGTH + Constants.PLANE_LENGTH/2;
			double y = -Constants.PLANE_LENGTH*displayY[i]/Constants.PLANE_GAME_LENGTH + Constants.PLANE_HEIGHT/2;
		
			
			if(inverted)
			{
				x = Constants.PLANE_LENGTH - x;
			}
			
			for(int j=0; j<numPlayers; j++)
			{
				for(int k=0; k<players[j].getNumSoldiers(); k++)
				{
					if(j==currentTurn)
					{
						if(k == players[j].getCurrentTurnSoldierIndex())
						{
							continue;
						}							
					}
					
					if(players[j].getSoldiers()[k].isAlive())
					{
										
						double distX = players[j].getSoldiers()[k].getX() - x;
						double distY = players[j].getSoldiers()[k].getY() - y;
						
						double distSquared = (Math.pow(distX, 2)+Math.pow(distY, 2));
											
						if(distSquared < Constants.SOLDIER_RADIUS*Constants.SOLDIER_RADIUS)
						{
							if(playerAlreadyHit(j,k)==false)
							{
								playersHit[numPlayersHit] = j;
								soldiersHit[numPlayersHit] = k;
								soldierHitPosition[numPlayersHit] = i;
								numPlayersHit++;
							}
						}
					}
				}
			}
						
			
			if(checkObstacleCollision(obstacle, i, inverted))
			{
				numSteps = i;
				break;
			}
			
			if(Double.isNaN(y) || Double.isInfinite(y))
			{
				numSteps = i;
				break;
			}
			
			
		}
		
		lastX = Constants.PLANE_LENGTH*displayX[numSteps-1]/Constants.PLANE_GAME_LENGTH + Constants.PLANE_LENGTH/2;;
		lastY = -Constants.PLANE_LENGTH*displayY[numSteps-1]/Constants.PLANE_GAME_LENGTH + Constants.PLANE_HEIGHT/2;
		processImpact(obstacle, inverted);

				
	}
	
	// Measures how fast the function was going when it hit an obstacle.
	// x always advances at the same rate, so speed = path length / x advance, which is
	// sqrt(1 + slope^2) for a plain function. Bounces don't change the length of the path.
	private void processImpact(Obstacle obstacle, boolean inverted)
	{
		impactSpeed = 1;
		
		int end = numSteps;
		
		if(hitObstacle == false || end >= Constants.FUNC_MAX_STEPS)
		{
			return;
		}
		
		int start = end - Constants.EXPLOSION_SPEED_SAMPLE_STEPS;
		if(start < 0)
		{
			start = 0;
		}
		
		double rawDX = valuesX[end] - valuesX[start];
		double length = 0;
		
		for(int i=start+1; i<=end; i++)
		{
			double stepX = displayX[i] - displayX[i-1];
			double stepY = displayY[i] - displayY[i-1];
			double step = Math.sqrt(stepX*stepX + stepY*stepY);
			
			if(GameData.wrapEnabled && step > PLANE_LENGTH/2)
			{
				// A jump across the wrap border is not real movement
				continue;
			}
			
			length += step;
		}
		
		if(rawDX > 0)
		{
			impactSpeed = length/rawDX;
		}
		
		if(Double.isNaN(impactSpeed) || Double.isInfinite(impactSpeed) || impactSpeed < 1)
		{
			impactSpeed = 1;
		}
	}
	
	private void resetReflection()
	{
		displayX = new double[Constants.FUNC_MAX_STEPS];
		displayY = new double[Constants.FUNC_MAX_STEPS];
		
		a11 = 1; a12 = 0;
		a21 = 0; a22 = 1;
		b1 = 0; b2 = 0;
		
		numBounces = 0;
		escapingObstacle = false;
		escapeSteps = 0;
		hitObstacle = false;
	}
	
	private void applyTransform(int i)
	{
		displayX[i] = a11*valuesX[i] + a12*valuesY[i] + b1;
		displayY[i] = a21*valuesX[i] + a22*valuesY[i] + b2;
	}
	
	private int toPixelX(double gameX, boolean inverted)
	{
		double x = Constants.PLANE_LENGTH*gameX/Constants.PLANE_GAME_LENGTH + Constants.PLANE_LENGTH/2;
		
		if(inverted)
		{
			x = Constants.PLANE_LENGTH - x;
		}
		
		return (int)x;
	}
	
	private int toPixelY(double gameY)
	{
		return (int)(-Constants.PLANE_LENGTH*gameY/Constants.PLANE_GAME_LENGTH + Constants.PLANE_HEIGHT/2);
	}
	
	// Checks the point at step i against the terrain. Returns true when the function must end there.
	// With reflection on, a hit on an obstacle mirrors the rest of the path across the obstacle's
	// surface at the collision point instead (angle of incidence = angle of reflection).
	private boolean checkObstacleCollision(Obstacle obstacle, int i, boolean inverted)
	{
		int px = toPixelX(displayX[i], inverted);
		int py = toPixelY(displayY[i]);
		
		if(obstacle.collidePoint(px, py) == false)
		{
			escapingObstacle = false;
			escapeSteps = 0;
			return false;
		}
		
		boolean onObstacle = obstacle.isObstaclePoint(px, py);
		
		// Right after a bounce the path may still be a pixel or so inside the obstacle
		if(escapingObstacle && onObstacle)
		{
			escapeSteps++;
			
			if(escapeSteps > Constants.REFLECTION_MAX_ESCAPE_STEPS)
			{
				hitObstacle = true;
				return true;
			}
			
			return false;
		}
		
		// Borders still end the function; only obstacles reflect
		if(onObstacle == false || GameData.reflectionEnabled == false || numBounces >= Constants.REFLECTION_MAX_BOUNCES || i < 1)
		{
			hitObstacle = onObstacle;
			return true;
		}
		
		double dirX = displayX[i] - displayX[i-1];
		double dirY = displayY[i] - displayY[i-1];
		double dirLength = Math.sqrt(dirX*dirX + dirY*dirY);
		
		if(dirLength < 1e-12 || Double.isNaN(dirLength))
		{
			hitObstacle = true;
			return true;
		}
		
		// Surface normal in terrain pixels, converted to game coordinates (y up, x mirrored if inverted)
		double[] normal = obstacle.getSurfaceNormal(px, py, Constants.REFLECTION_NORMAL_RADIUS);
		double nx;
		double ny;
		
		if(normal != null)
		{
			nx = inverted ? -normal[0] : normal[0];
			ny = -normal[1];
		}
		else
		{
			nx = -dirX/dirLength;
			ny = -dirY/dirLength;
		}
		
		// The normal must face the incoming path; if the estimate doesn't, bounce straight back
		if(dirX*nx + dirY*ny >= 0)
		{
			nx = -dirX/dirLength;
			ny = -dirY/dirLength;
		}
		
		// Mirror across the surface line through the last point outside the obstacle:
		// M(p) = c + R(p - c), with R = I - 2*n*n^T
		double r11 = 1 - 2*nx*nx;
		double r12 = -2*nx*ny;
		double r21 = r12;
		double r22 = 1 - 2*ny*ny;
		
		double cx = displayX[i-1];
		double cy = displayY[i-1];
		
		double newA11 = r11*a11 + r12*a21;
		double newA12 = r11*a12 + r12*a22;
		double newA21 = r21*a11 + r22*a21;
		double newA22 = r21*a12 + r22*a22;
		
		double newB1 = r11*b1 + r12*b2 + cx - (r11*cx + r12*cy);
		double newB2 = r21*b1 + r22*b2 + cy - (r21*cx + r22*cy);
		
		a11 = newA11; a12 = newA12;
		a21 = newA21; a22 = newA22;
		b1 = newB1; b2 = newB2;
		
		applyTransform(i);
		
		numBounces++;
		escapingObstacle = true;
		escapeSteps = 0;
		
		return false;
	}
	
	public boolean hasHitObstacle()
	{
		return hitObstacle;
	}
	
	public double getImpactSpeed()
	{
		return impactSpeed;
	}
	
	public int getExplosionRadius()
	{
		if(Constants.EXPLOSION_SPEED_SCALING == false || hitObstacle == false)
		{
			return Constants.EXPLOSION_RADIUS;
		}
		
		double radius = Constants.EXPLOSION_RADIUS*(1 + Constants.EXPLOSION_SPEED_FACTOR*(impactSpeed - 1));
		
		if(radius > Constants.EXPLOSION_MAX_RADIUS)
		{
			radius = Constants.EXPLOSION_MAX_RADIUS;
		}
		if(radius < Constants.EXPLOSION_RADIUS)
		{
			radius = Constants.EXPLOSION_RADIUS;
		}
		
		return (int)Math.round(radius);
	}
	
	public double getLastX()
	{
		return lastX;
	}
	
	public double getLastY()
	{
		return lastY;
	}

	private double getWrappedDistYSquared(double y1, double y2) {
		double distY = Math.abs(y1 - y2);
		// Y ekseni toplam uzunluğu 30 birimdir (-15 ile +15 arası)
		// Eğer iki nokta arasındaki mesafe 15'ten büyükse, sınırın öbür tarafından birbirlerine daha yakındırlar.
		if (distY > 15.0) {
			distY = 30.0 - distY;
		}
		return distY * distY;
	}
}
