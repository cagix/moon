package cdq;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.Viewport;

public class StageWithState extends Stage {

  public Object applicationState;

  public StageWithState(Viewport viewport, Batch batch){
    super(viewport, batch);
  }

}
