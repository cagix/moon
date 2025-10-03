(ns com.badlogic.gdx.utils.viewport.fit-viewport
  (:require [com.badlogic.gdx.math.vector2 :as vector2]
            [gdl.graphics.viewport])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.utils.viewport FitViewport)))

(defn create [width height camera]
  (proxy [FitViewport ILookup] [width height camera]
    (valAt [k]
      (let [^FitViewport this this]
        (case k
          :viewport/width             (.getWorldWidth      this)
          :viewport/height            (.getWorldHeight     this)
          :viewport/camera            (.getCamera          this)
          :viewport/left-gutter-width (.getLeftGutterWidth this)
          :viewport/right-gutter-x    (.getRightGutterX    this)
          :viewport/top-gutter-height (.getTopGutterHeight this)
          :viewport/top-gutter-y      (.getTopGutterY      this))))))

(extend-type FitViewport
  gdl.graphics.viewport/Viewport
  (unproject [viewport x y]
    (-> viewport
        (.unproject (vector2/->java x y))
        vector2/->clj))

  (update! [viewport width height {:keys [center?]}]
    (.update viewport width height (boolean center?))))

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
