import com.orbischallenge.firefly.client.objects.models.EnemyUnit;
import com.orbischallenge.firefly.client.objects.models.FriendlyUnit;
import com.orbischallenge.firefly.client.objects.models.World;
import com.orbischallenge.firefly.client.objects.models.Tile;

import com.orbischallenge.game.engine.Point;
import com.orbischallenge.firefly.objects.enums.Direction;

import com.orbischallenge.logging.Log;

import java.util.*;


/*
 Project Overview
 
 For our AI, we designed it so that the first priority is to always make nests since we want
  
  
  */

public class PlayerAI {
	// Any field declarations go here

	public PlayerAI() {
		// Any instantiation code goes here
	}

	/**
	 * This method will get called every turn.
	 *
	 * @param world The latest state of the world.
	 * @param friendlyUnits An array containing all remaining firefly units in your team
	 * @param enemyUnits An array containing all remaining enemy firefly units
	 *
	 */
	public void doMove(World world, FriendlyUnit[] friendlyUnits, EnemyUnit[] enemyUnits) {
		/* Fly away to freedom, daring fireflies
        Build thou nests
        Grow, become stronger
        Take over the world */

		//CONSTANTS
		int nest_builders = (int)(friendlyUnits.length * .25) ;
		int builders_count = 0;

		//make a limit how many build nests

		for (FriendlyUnit unit: friendlyUnits) {
			//general path to enemy
			List<Point> path = world.getShortestPath(unit.getPosition(),
					world.getClosestCapturableTileFrom(unit.getPosition(), null).getPosition(),
					null);
			List<Point> path_enem = world.getShortestPath(unit.getPosition(),
					world.getClosestEnemyTileFrom(unit.getPosition(), null).getPosition(), null);

			//only move to attack if you have enough health
			int thresh_dist = 5;
			int curr_dist = 1000;
			int select_enemy = -1;
			Point[] your_nests = world.getFriendlyNestPositions();
			int nest_count_thresh = 3;
			if(your_nests.length<nest_count_thresh & (builders_count<nest_builders))
			{

				//call an neutral tiles
				//check all withina sa 10 by 10 block?
				//check which have the most red borders //walls, and then make that the nest target point
				//make your bots move to the surounding spaces, without going on that space.
				int build_nest_thresh = 10;
				int curr_tile = -1;
				int borders = 0;
				Tile [] neut_tiles = world.getNeutralTiles();
				for (int q = 0; q < neut_tiles.length ; q ++)
				{						
					builders_count++;
					int dist = world.getShortestPathDistance(unit.getPosition(), neut_tiles[q].getPosition());

					//check surrounding tiles only if distance is less than the threshold
					if(dist<=build_nest_thresh){
						Map<Direction, Tile> tilesAround = world.getTilesAround(neut_tiles[q].getPosition());
						//check each of these surrounding poitns, and see if any of them are walls, or already taken by you

						if (tilesAround!= null) //null check
						{
							int counter = 0;
							for (Map.Entry<Direction, Tile> entry : tilesAround.entrySet())
							{
								//int counter = 0;
								Tile t = entry.getValue();
								//is it a wall?
								if (t.isFriendly()==true)
								{counter ++;}
								if (world.isWall(t.getPosition()))
								{counter++;}
							}
							if(counter>borders)
							{
								curr_tile = q;//if more of it is already read or a wall, this is a better potential nest site
							}
						}
					}
				}

				//by now, an ideal site should be found
				if (curr_tile!=-1)
				{
					Map<Direction, Tile> tilesAround = world.getTilesAround(neut_tiles[curr_tile].getPosition());
					if(tilesAround!= null){
						//avoid the nest location
						//go to the area around tha nest tile
						List<Point> avoid = new ArrayList<Point>();
						avoid.add(neut_tiles[curr_tile].getPosition());
						List<Point> surroundings = dumpMap(tilesAround);
						List<Point> path_nest = world.getShortestPath(unit.getPosition(), surroundings.get(0), avoid);
						if(path_nest!= null) world.move(unit, path_nest.get(0));
						else{
							if(path!= null) world.move(unit, path.get(0));
							else if(path_enem!=null){
								world.move(unit, path_enem.get(0));	
							}
						}
					}
				}
				else  //if its not found, still move, first to grey land, then enemy land
				{
					if(path!= null) world.move(unit, path.get(0));
					else
					{if(path_enem!= null) world.move(unit, path_enem.get(0));}

				}
			}
			//if you have more health, go after the enemies and nests
			else if ((unit.getHealth()>=2)){
				//evaluate the cost of attacking enemies if there is any enemy within 10 spots
				//iterate over the enemy list to find closest
				for (int i = 0; i < enemyUnits.length; i++)
				{
					int dist = world.getShortestPathDistance(enemyUnits[i].getPosition(), unit.getPosition());
					//if this is the new minimum
					//check as well that you have mor e health, as self preservation is important
					if ((dist<thresh_dist) & Math.min(curr_dist, dist)==dist & (unit.getHealth()>=enemyUnits[i].getHealth()) ) 
					{
						select_enemy = i;
						curr_dist = dist; //update the closet enemy
					}

				}
				//if enemy found, move
				if(select_enemy!=-1){
					//move to the enemy
					List<Point> enemy_path = world.getShortestPath(unit.getPosition(), enemyUnits[select_enemy].getPosition(), null);
					if (enemy_path!= null) world.move(unit, enemy_path.get(0));
					//this is just to enure that this unit will move
					else if (path!= null)
					{world.move(unit, path.get(0));}
					else
					{if (path_enem!=null) world.move(unit, path_enem.get(0));}

				}
				//if no enemy found?
				else
				{
					//ATTACK THE NESTS
					//get the nest locations
					Point[] enemyNestPos = world.getEnemyNestPositions();
					//go after the closest one that is within a distance threshold
					int nest_thresh =8; //be aggresive
					int select_nest = -1;
					int curr_nest_dist = 1000; //pick absurdly large value
					for(int j = 0; j < enemyNestPos.length; j ++)
					{
						int dist = world.getShortestPathDistance(enemyNestPos[j], unit.getPosition());
						if( (dist<nest_thresh) & (dist<=curr_dist))
						{
							select_nest = j;
							curr_nest_dist = dist;
						}
					}
					//if nest found, move
					if(select_nest!=-1){
						//move to the nest
						List<Point> nest_path = world.getShortestPath(unit.getPosition(), enemyNestPos[select_nest], null);
						if (nest_path!= null) world.move(unit, nest_path.get(0));
						//this is only if there is nothing else to do when you have enough hesalth
						else if (path != null) 
						{world.move(unit, path.get(0));}
						else
						{world.move(unit, path_enem.get(0));}

					}

					//this is only if there is nothing else to do when you have enough hesalth
					else if (path != null) 
					{world.move(unit, path.get(0));}
					else
					{world.move(unit, path_enem.get(0));}

				}

			}
			//Move even if you dont have enough health if the enemy had a lot of tiles
			else if(world.getFriendlyTiles().length + 5 < world.getEnemyTiles().length)
			{
				if (path != null) world.move(unit, path.get(0));
				else {
					if (path_enem!=null) world.move(unit, path_enem.get(0));
				}

			}
			else //REST
			{

			}


		}




	}

	public List<Point> dumpMap(Map<Direction, Tile> map)
	{
		List <Point> points= new ArrayList<Point>();
		for (Map.Entry<Direction, Tile> entry : map.entrySet()) {
			Tile t = entry.getValue();
			points.add(t.getPosition());
		}
		return points;
	}

	//cost function
	public int costfunction(World world,Point start, Point end, int factor)
	{
		int dist = world.getShortestPathDistance(start, end);
		int cost = dist * dist * factor;
		return cost;
	}


}