package Doidao;
import robocode.*;
import java.awt.Color;
import static robocode.util.Utils.normalRelativeAngleDegrees;
import static robocode.util.Utils.normalRelativeAngle;
import static robocode.util.Utils.normalAbsoluteAngle;
import java.awt.geom.*;

public class Doidao2 extends AdvancedRobot
{
	private int state;
	private int fireLevel;
	private int count  = 0;
	private int beatUp = 0;
	
	public void run() {
		setBodyColor(Color.black);
		setState(1); //Inicia rodopiando.

		while(true) {
			if (getState() == 0) {
				turnGunRight(10);		
			} else if (getState() == 1) {
				setTurnRight(200);
				setAhead(200);
				count++;
				
				if (count == 5) {
					count = 0;
					setAhead(30);
				}
			
				execute();
			}
		}
	}

	/**
	 * Propriedades do robô em estado de loop
	 */
	public void loop()
	{
		this.fireLevel = 2;
	}

	/**
	 * Propriedades do robô em estado de tower
	 */
	public void tower()
	{
		
		this.fireLevel = 4;
	}

	public int getState()
	{
		return this.state;
	}

	/**
	 * state = 0; Torre
	 * state = 1; Crazy
	 */
	public void setState(int state)
	{
		this.state = state;
		
		switch(state) {
			case 0:
				tower();
				break;
			case 1:
				loop();
				break;
		}
	}

	/**
	 * Altera o estado do robô de acordo com a distância
	 */
	private void verifyState(double distance)
	{
		if (distance < 150 && getState() == 1) {
			setState(0);
		} else if (distance > 200 && getState() == 0) {
			setState(1);
		}
	}

	private double getBulletSpeed(double power)
	{ 
    	return 20.0 - (5.0 * power); 
	} 

	/**
	 * Iterative Linear Targeting
	 *
	 * @source: http://robowiki.net/wiki/Linear_Targeting
	 */
	public void killWall(ScannedRobotEvent e, double enemyX, double enemyY, double absoluteBearing)
	{
		double bulletPower = Math.min(3.0,getEnergy());
		double myX = getX();
		double myY = getY();

		double enemyHeading = e.getHeadingRadians();
		double enemyVelocity = e.getVelocity();
 
		double deltaTime = 0;
		double battleFieldHeight = getBattleFieldHeight(), battleFieldWidth = getBattleFieldWidth();
		double predictedX = enemyX, predictedY = enemyY;
		
		while((++deltaTime) * (20.0 - 3.0 * bulletPower) < Point2D.Double.distance(myX, myY, predictedX, predictedY)){		
			predictedX += Math.sin(enemyHeading) * enemyVelocity;	
			predictedY += Math.cos(enemyHeading) * enemyVelocity;
	
			if(	predictedX < 18.0 
				|| predictedY < 18.0
				|| predictedX > battleFieldWidth - 18.0
				|| predictedY > battleFieldHeight - 18.0) {
					
				predictedX = Math.min(Math.max(18.0, predictedX), 
           	         battleFieldWidth - 18.0);	
				predictedY = Math.min(Math.max(18.0, predictedY), 
                    battleFieldHeight - 18.0);
				break;
			}
		}

		double theta = normalAbsoluteAngle(Math.atan2(predictedX - getX(), predictedY - getY()));
 
		setTurnRadarRightRadians(normalRelativeAngle(absoluteBearing - getRadarHeadingRadians()));
		setTurnGunRightRadians(normalRelativeAngle(theta - getGunHeadingRadians()));
		
		fire(bulletPower);
	}

	public void onBulletMissed(BulletMissedEvent e)
	{
		beatUp++;
	}

	public void onBulletHit (BulletHitEvent e)
	{
		beatUp = 0;
	}

	/**
	 * onScannedRobot: What to do when you see another robot
	 */
	public void onScannedRobot(ScannedRobotEvent e)
	{	
		verifyState(e.getDistance());
		
		if (getState() == 1) { // Estado Loop
			double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
			double enemyX = getX() + e.getDistance() * Math.sin(absoluteBearing);
			double enemyY = getY() + e.getDistance() * Math.cos(absoluteBearing);
			
			if ((enemyX < 25 || enemyY < 25)) {
				if (beatUp < 10) {
					killWall(e, enemyX, enemyY, absoluteBearing);
				} else {
					fire(this.fireLevel);
					beatUp = 0;
				}
			} else {
				if (count == 4) {
					double bearingFromGun = normalRelativeAngleDegrees(getHeading() + e.getBearing() - getGunHeading()); // Obtém a posição exata do tanque adversário

					turnGunRight(bearingFromGun);
				}
			
				fire(this.fireLevel);
			}
		} else if (getState() == 0) { // Estado Tower
			double bearingFromGun = normalRelativeAngleDegrees(getHeading() + e.getBearing() - getGunHeading()); // Obtém a posição exata do tanque adversário

			turnGunRight(bearingFromGun); // Posiciona o canhão na posição do adversário
			
			if (Math.abs(bearingFromGun) <= 3) {
				if (getGunHeat() == 0) { // Se o calor do canhão estiver 0... FIRE!!!
					fire((this.fireLevel + Math.abs(bearingFromGun)) / (e.getDistance() / 180));
				}
			}

			if (bearingFromGun == 0) { // Canhão apontado pro norte
				scan(); // Escaneia novamente
			}
		}
	}
	
	/**
	 * onHitWall: What to do when you hit a wall
	 */
	public void onHitWall(HitWallEvent e)
	{
		turnRight(90); // Vira o robô em 90º
		ahead(180); // Anda 100 px para frente
	}

	/**
	 * Dispara esse evento quando ele atinge outro robô
	 */
	public void onHitRobot(HitRobotEvent e)
	{
		if (getState() == 1) { // Altera para estado de tower, para combate de curta distância
			setState(0);
		}
	
		// Posição exata do robô adversário
		double moveArma = normalRelativeAngleDegrees(e.getBearing() + getHeading() - getGunHeading());
		// Posiciona o canhão no adversário
		turnGunRight(moveArma);	
		// FIRE!!!
		fire(this.fireLevel);
		/// Escaneia a região
		scan();
	}
}