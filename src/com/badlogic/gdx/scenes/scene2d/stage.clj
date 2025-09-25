(ns com.badlogic.gdx.scenes.scene2d.stage
  (:require gdl.scene2d.stage)
  (:import (com.badlogic.gdx.scenes.scene2d StageWithCtx)))

(extend-type StageWithCtx
  gdl.scene2d.stage/Stage
  (set-ctx! [this ctx]
    (set! (.ctx this) ctx))

  (get-ctx [this]
    (.ctx this))

  (act! [this]
    (.act this))

  (draw! [this]
    (.draw this))

  (add! [this actor]
    (.addActor this actor))

  (clear! [this]
    (.clear this))

  (root [this]
    (.getRoot this))

  (hit [this [x y]]
    (.hit this x y true))

  (viewport [this]
    (.getViewport this)))
