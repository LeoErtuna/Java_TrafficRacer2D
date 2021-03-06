package game.loops;

import game.traffics.FreeroamTraffic;
import engine.GameLoop;
import engine.InputController;
import engine.cars.CarPlayer;
import engine.cars.CarTraffic;
import game.sprites.RoadSprite;
import engine.physics.Collisions;
import gui.panels.gameplay.FreeroamPanel;

/**
 * Created by Leo on 11-Oct-15.
 */
public class FreeroamLoop extends GameLoop
{
    private FreeroamPanel freeroamPanel;

    /* Game objects */
    private RoadSprite roadSprite;
    private CarPlayer carPlayer;
    private FreeroamTraffic freeroamTraffic;

    private boolean raceStarted = false;

    /* Constructor */
    public FreeroamLoop(RoadSprite roadSprite, CarPlayer carPlayer, FreeroamTraffic freeroamTraffic, FreeroamPanel freeroamPanel)
    {
        this.roadSprite = roadSprite;
        this.carPlayer = carPlayer;
        this.freeroamTraffic = freeroamTraffic;
        this.freeroamPanel = freeroamPanel;

        carPlayer.reset(roadSprite.getLinesCoordinates().get(2), roadSprite.getHeight() - 900);
    }

    private void updateInput(double deltaTime)
    {
        /* Respond to throttle */
        if (InputController.THROTTLE_PRESSED)
            carPlayer.setThrottlePosition(1);
        else
            carPlayer.setThrottlePosition(0);

        /* Respond to brake */
        if (InputController.BRAKE_PRESSED)
            carPlayer.setBrakePosition(1);
        else
            carPlayer.setBrakePosition(0);

        /* Respond to shift down */
        if (InputController.SHIFT_DOWN_PRESSED) {
            carPlayer.shiftDown();
            InputController.SHIFT_DOWN_PRESSED = false;
        }

        /* Respond to shift up */
        if (InputController.SHIFT_UP_PRESSED) {
            carPlayer.shiftUp();
            InputController.SHIFT_UP_PRESSED = false;
        }

        /* Respond to turn left */
        if (InputController.TURN_LEFT_PRESSED)
        {
            if (!(InputController.COLLISION_TURN_LEFT_PRESSED || InputController.COLLISION_TURN_RIGHT_PRESSED))
            {
                int turnResult = carPlayer.turnLeft(roadSprite.getLinesCoordinates(), deltaTime, carPlayer.getVelocity());

                if (turnResult == 1)
                    InputController.TURN_LEFT_PRESSED = false;
            }
        }

        /* Respond to turn right */
        if (InputController.TURN_RIGHT_PRESSED)
        {
            if (!(InputController.COLLISION_TURN_LEFT_PRESSED || InputController.COLLISION_TURN_RIGHT_PRESSED))
            {
                int turnResult = carPlayer.turnRight(roadSprite.getLinesCoordinates(), deltaTime, carPlayer.getVelocity());
                if (turnResult == 1)
                    InputController.TURN_RIGHT_PRESSED = false;
            }
        }

        /* Respond to collision turn left */
        if (InputController.COLLISION_TURN_LEFT_PRESSED)
        {
            if (!InputController.COLLISION_TURN_RIGHT_PRESSED)
            {
                InputController.TURN_LEFT_PRESSED = false;
                int turnResult = carPlayer.stopTurnLeft(deltaTime, carPlayer.getVelocity());

                if (turnResult == 1)
                    InputController.COLLISION_TURN_LEFT_PRESSED = false;
            }
        }

        /* Respond to collision turn right */
        if (InputController.COLLISION_TURN_RIGHT_PRESSED)
        {
            if (!InputController.COLLISION_TURN_LEFT_PRESSED)
            {
                InputController.TURN_RIGHT_PRESSED = false;
                int turnResult = carPlayer.stopTurnRight(deltaTime, carPlayer.getVelocity());

                if (turnResult == 1)
                    InputController.COLLISION_TURN_RIGHT_PRESSED = false;
            }
        }
    }

    private void checkCrash()
    {
        for (CarTraffic carTraffic : freeroamTraffic.getCurrentCars())
        {
            if (carPlayer.isOverlappingWith(carTraffic))
            {
                boolean sameLane = carPlayer.getStartX() == carTraffic.getStartX();
                boolean hitWhenAccelerating = carTraffic.getStartY() < carPlayer.getStartY() && carPlayer.getStartY() < carTraffic.getEndY();
                boolean hitWhenTurningLeft = carTraffic.getStartX() < carPlayer.getStartX() && carPlayer.getStartX() < carTraffic.getEndX();
                boolean hitWhenTurningRight = carTraffic.getStartX() < carPlayer.getEndX() && carPlayer.getEndX() < carTraffic.getEndX();

                /* If the cars are in the same line - they overlap only in vertical direction */
                if(sameLane)
                {
                    Collisions.applyCollisionY(carPlayer, carTraffic);
                }

                else
                {
                    /* Collision when turning left */
                    if (hitWhenTurningLeft)
                    {
                        InputController.TURN_LEFT_PRESSED = false;
                        InputController.TURN_RIGHT_PRESSED = false;
                        InputController.COLLISION_TURN_LEFT_PRESSED = true;
                        InputController.COLLISION_TURN_RIGHT_PRESSED = false;

                        if (carPlayer.getStartX() < carTraffic.getEndX())
                        {
                            carPlayer.setStartX(carTraffic.getEndX());
                        }
                    }

                    /* Collision when turning right */
                    if (hitWhenTurningRight)
                    {
                        InputController.TURN_LEFT_PRESSED = false;
                        InputController.TURN_RIGHT_PRESSED = false;
                        InputController.COLLISION_TURN_LEFT_PRESSED = false;
                        InputController.COLLISION_TURN_RIGHT_PRESSED = true;

                        if (carPlayer.getEndX() > carTraffic.getStartX())
                        {
                            carPlayer.setStartX(carTraffic.getStartX() - carPlayer.getWidthPX());
                        }
                    }

                    if (hitWhenAccelerating)
                    {
                        Collisions.applyCollisionY(carPlayer, carTraffic);
                    }
                }


            }
        }
    }

    @Override
    protected void fixedUpdate()
    {
        /* Update input */
        updateInput(getDeltaTimeS());

        checkCrash();

        /* Update game objects */
        carPlayer.update(getDeltaTimeS());
        freeroamTraffic.update(getDeltaTimeS());
        roadSprite.update(carPlayer.getVelocity(), getDeltaTimeS());

        if (!raceStarted)
        {
            this.freeroamTraffic.initialize();
            this.freeroamPanel.updateGameImage();
            this.freeroamPanel.repaint();
            this.freeroamPanel.drawCountdown();
            raceStarted = true;
        }

        if (carPlayer.getHealth() <= 0 && Math.abs(carPlayer.getVelocity() - 0) < 0.01)
        {
            freeroamPanel.windowReference.setToFreeroamFinish();
            this.pauseGameLoop();
        }
    }




    @Override
    protected void freeUpdate()
    {
        if (raceStarted)
        {
            /* Update graphics */
            freeroamPanel.updateGameImage();
        }

        freeroamPanel.repaint();
    }
}
