package com.badlogic.gdx.scenes.scene2d;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.Viewport;

public class CtxStage extends Stage  {

  public Object ctx;

	public CtxStage (Viewport viewport, Batch batch) {
		super(viewport, batch);
    this.ctx = null;
	}

}
