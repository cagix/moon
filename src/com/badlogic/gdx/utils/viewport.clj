(ns com.badlogic.gdx.utils.viewport
  (:require [com.badlogic.gdx.math.vector2 :as vector2])
  (:import (com.badlogic.gdx.utils.viewport Viewport)))

(defn camera [^Viewport this]
  (.getCamera this))

(defn world-width [^Viewport this]
  (.getWorldWidth this))

(defn world-height [^Viewport this]
  (.getWorldHeight this))

(defn unproject [^Viewport this x y]
  (-> this
      (.unproject (vector2/->java x y))
      vector2/->clj))

(defn update! [^Viewport viewport width height {:keys [center?]}]
  (.update viewport width height (boolean center?)))

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
