(ns clojure.gdx.scenes.scene2d.stage
  (:require [clojure.scene2d.stage])
  (:import (com.badlogic.gdx.scenes.scene2d CtxStage)))

(defn create [viewport batch]
  (CtxStage. viewport batch))

(extend-type CtxStage
  clojure.scene2d.stage/Stage
  (set-ctx! [stage ctx]
    (set! (.ctx stage) ctx))

  (get-ctx [stage]
    (.ctx stage))

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
