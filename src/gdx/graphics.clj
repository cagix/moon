(ns gdx.graphics
  (:require [com.badlogic.gdx.utils.viewport :as vp]
            [gdl.graphics.viewport]
            [gdl.math :refer [clamp]])
  (:import (com.badlogic.gdx.utils.viewport FitViewport)))

(defn fit-viewport [width height camera]
  (FitViewport. width height camera))

(extend FitViewport
  gdl.graphics.viewport/Viewport
  {:camera vp/camera
   :world-width vp/world-width
   :world-height vp/world-height

   ; TODO this only done @ my update-input, does not belong here ?
   ; so need to save it in the 'ctx/input' record
   :unproject (fn [this [x y]]
                (vp/unproject this
                              (clamp x
                                     (.getLeftGutterWidth this)
                                     (.getRightGutterX    this))
                              (clamp y
                                     (.getTopGutterHeight this)
                                     (.getTopGutterY      this))))

   :update! vp/update!})

(comment

  ; TODO not using width/height params???
	;public void update (int screenWidth, int screenHeight, boolean centerCamera) {
	;	apply(centerCamera);
	;}

	;public void apply (boolean centerCamera) {
	;	HdpiUtils.glViewport(screenX, screenY, screenWidth, screenHeight);
	;	camera.viewportWidth = worldWidth;
	;	camera.viewportHeight = worldHeight;
	;	if (centerCamera) camera.position.set(worldWidth / 2, worldHeight / 2, 0);
	;	camera.update();
	;}


	; /** Calls {@link GL20#glViewport(int, int, int, int)}, expecting the coordinates and sizes given in logical coordinates and
	;  * automatically converts them to backbuffer coordinates, which may be bigger on HDPI screens. */
	; public static void glViewport (int x, int y, int width, int height) {
	; 	if (mode == HdpiMode.Logical && (Gdx.graphics.getWidth() != Gdx.graphics.getBackBufferWidth()
	; 		|| Gdx.graphics.getHeight() != Gdx.graphics.getBackBufferHeight())) {
	; 		Gdx.gl.glViewport(toBackBufferX(x), toBackBufferY(y), toBackBufferX(width), toBackBufferY(height));
	; 	} else {
	; 		Gdx.gl.glViewport(x, y, width, height);
	; 	}
	; }
 )
