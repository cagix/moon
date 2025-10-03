(ns com.badlogic.gdx.utils.viewport
  (:require [com.badlogic.gdx.math.vector2 :as vector2])
  (:import (com.badlogic.gdx.utils.viewport Viewport)))

(defn unproject [^Viewport viewport x y]
  (-> viewport
      (.unproject (vector2/->java x y))
      vector2/->clj))

(defn update! [^Viewport viewport width height {:keys [center?]}]
  (.update viewport width height (boolean center?)))

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
 )
