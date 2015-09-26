package com.mygdx.game.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.components.IntentBroadcastComponent;
import com.mygdx.game.components.PathFindingComponent;
import com.mygdx.game.components.SelectableComponent;
import com.mygdx.game.utilities.NavMesh;
import com.mygdx.game.utilities.Triangle;

/**
 * Created by user on 8/25/15.
 */
public class SelectionSystem extends EntitySystem {

	public static final String tag = "SelectionSystem";
	private final Vector3 surfacePoint = new Vector3();
	public final Family systemFamily;
	private final PhysicsSystem phySys;
	private final Viewport viewport;
	private final Vector2 lastClick = new Vector2();
	private ImmutableArray<Entity> entities;
	private final ComponentMapper<IntentBroadcastComponent> intentCmps = ComponentMapper.getFor(IntentBroadcastComponent.class);
	private final ComponentMapper<SelectableComponent> selCmps = ComponentMapper.getFor(SelectableComponent.class);
	private final ComponentMapper<PathFindingComponent> pathCmps = ComponentMapper.getFor(PathFindingComponent.class);
	private final Ray ray = new Ray();

	private NavMesh navMesh;

	public SelectionSystem(PhysicsSystem phySys, Viewport viewport) {
		systemFamily = Family.all(IntentBroadcastComponent.class,
				PathFindingComponent.class,
				SelectableComponent.class).get();
		this.phySys = phySys;
		this.viewport = viewport;
	}

	@Override
	public void addedToEngine(Engine engine) {
		entities = engine.getEntitiesFor(systemFamily);
	}


	@Override
	public void update(float deltaTime) {
		if (entities.size() == 0) {
			return;
		}
		IntentBroadcastComponent intent = intentCmps.get(entities.get(0));
		if (intent.click.equals(lastClick) && !intent.doubleClick) {
			return;
		}
		lastClick.set(intent.click);
		float screenX = intent.click.x;
		float screenY = intent.click.y;

		ray.set(viewport.getPickRay(screenX, screenY));
		float distance = 100;
		Gdx.app.debug(tag, String.format(
				"Mouse: %s, %s, Pick ray: origin: %s, direction: %s.",
				screenX, screenY, ray.origin, ray.direction));

		Entity hitEntity = phySys.rayTest(ray, surfacePoint,
				PhysicsSystem.PC_FLAG,
				PhysicsSystem.ALL_FLAG,
				distance);

		if (hitEntity != null) {
			Gdx.app.debug(tag, "Hit PC: " + hitEntity);
			SelectableComponent hitEntitySelCmp = selCmps.get(hitEntity);
			if (hitEntitySelCmp != null) {
				// If hit entity is selectable, deselect previous, select this one
				for (Entity entity : entities) {
					SelectableComponent selCmp = selCmps.get(entity);
					if (selCmp != null) {
						selCmp.isSelected = false;
					}
				}
				hitEntitySelCmp.isSelected = true;
				return;
			}
		}

		hitEntity = phySys.rayTest(ray, surfacePoint,
				PhysicsSystem.NAVMESH_FLAG,
				PhysicsSystem.NAVMESH_FLAG,
				distance);

		if (hitEntity != null) {
			Triangle hitTriangle = navMesh.rayTest(ray, distance);

			// TODO: Group movement later
			if (hitTriangle != null) {
				for (Entity entity : entities) {
					SelectableComponent selCmp = selCmps.get(entity);
					PathFindingComponent pathCmp = pathCmps.get(entity);
					if (selCmp.isSelected) {
						pathCmp.posGroundRay.origin.set(pathCmp.currentPosition);
						Triangle posTriangle = navMesh.rayTest(pathCmp.posGroundRay, distance);

						navMesh.calculatePath(posTriangle, hitTriangle, pathCmp.currentPosition,
								surfacePoint);
					}
				}
			}

//			// Old
//			Gdx.app.debug(tag, "Hit navmesh: " + hitEntity);
//			for (Entity entity : entities) {
//				SelectableComponent selCmp = selCmps.get(entity);
//				PathFindingComponent pathCmp = pathCmps.get(entity);
//				if (selCmp.isSelected) {
//					pathCmp.run = intent.doubleClick;
//					pathCmp.currentGoal = null;
//					pathCmp.path.clear();
//					pathCmp.path.add(new Vector3(surfacePoint));
//					Gdx.app.debug(tag, String.format("Path target for %s set to %s", entity, surfacePoint));
//				}
//			}

		}

	}

	public void setNavMesh(NavMesh navMesh) {
		this.navMesh = navMesh;
	}
}
