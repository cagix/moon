(ns clojure.gdx.scene2d.stage
  (:require clojure.scene2d.stage)
  (:import (clojure.gdx.scene2d Stage)))

(defn create [viewport batch state]
  (Stage. viewport batch state))

(extend-type clojure.gdx.scene2d.Stage
  clojure.scene2d.stage/Stage
  (get-ctx [stage]
    @(.ctx stage))

  (act! [stage]
    (.act stage))

  (draw! [stage]
    (.draw stage))

  (add! [stage actor]
    (.addActor stage actor))

  (clear! [stage]
    (.clear stage))

  (root [stage]
    (.getRoot stage))

  (hit [stage [x y]]
    (.hit stage x y true))

  (viewport [stage]
    (.getViewport stage)))
