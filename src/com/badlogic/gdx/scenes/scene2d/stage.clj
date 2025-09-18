(ns com.badlogic.gdx.scenes.scene2d.stage
  (:require [clojure.scene2d.stage])
  (:import (com.badlogic.gdx.scenes.scene2d StageWithCtx)))

(defn create [viewport batch state]
  (StageWithCtx. viewport batch state))

(extend-type StageWithCtx
  clojure.scene2d.stage/Stage
  (get-ctx [this]
    @(.ctx this))

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
